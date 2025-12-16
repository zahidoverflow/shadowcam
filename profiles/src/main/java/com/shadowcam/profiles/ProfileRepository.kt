package com.shadowcam.profiles

import com.shadowcam.core.model.AppProfile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProfileRepository(
    seed: List<AppProfile> = emptyList(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val profiles = MutableStateFlow(seed.associateBy { it.packageName })

    val profilesFlow: Flow<List<AppProfile>> = profiles.map { map ->
        map.values.sortedBy { it.label }
    }

    suspend fun upsert(profile: AppProfile) = withContext(dispatcher) {
        profiles.value = profiles.value.toMutableMap().apply { put(profile.packageName, profile) }
    }

    suspend fun delete(packageName: String) = withContext(dispatcher) {
        profiles.value = profiles.value.toMutableMap().apply { remove(packageName) }
    }

    fun profileFor(packageName: String): AppProfile? = profiles.value[packageName]
}
