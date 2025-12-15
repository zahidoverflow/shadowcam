package com.shadowcam.profiles

import com.shadowcam.core.model.CameraProfile
import com.shadowcam.core.model.DemoData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class ProfileRepository(
    seed: List<CameraProfile> = DemoData.demoProfiles(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val profiles = MutableStateFlow(seed.associateBy { it.appId })

    val profilesFlow: Flow<List<CameraProfile>> = profiles.map { map ->
        map.values.sortedBy { it.label }
    }

    suspend fun upsert(profile: CameraProfile) = withContext(dispatcher) {
        profiles.value = profiles.value.toMutableMap().apply { put(profile.appId, profile) }
    }

    suspend fun delete(appId: String) = withContext(dispatcher) {
        profiles.value = profiles.value.toMutableMap().apply { remove(appId) }
    }

    fun profileFor(appId: String): CameraProfile? = profiles.value[appId]

    fun toggleFavorite(appId: String) {
        profiles.value[appId]?.let { current ->
            profiles.value = profiles.value.toMutableMap().apply {
                put(appId, current.copy(favorite = !current.favorite))
            }
        }
    }
}
