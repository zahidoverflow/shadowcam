package com.shadowcam.profiles

import com.shadowcam.core.model.AppProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

interface ProfileStore {
    val profiles: Flow<List<AppProfile>>
    suspend fun upsert(profile: AppProfile)
    suspend fun remove(packageName: String)
    suspend fun get(packageName: String): AppProfile?
}

class InMemoryProfileStore : ProfileStore {
    private val backing = MutableStateFlow<List<AppProfile>>(emptyList())
    override val profiles: Flow<List<AppProfile>> = backing

    override suspend fun upsert(profile: AppProfile) {
        val without = backing.value.filterNot { it.packageName == profile.packageName }
        backing.value = without + profile
    }

    override suspend fun remove(packageName: String) {
        backing.value = backing.value.filterNot { it.packageName == packageName }
    }

    override suspend fun get(packageName: String): AppProfile? =
        backing.value.firstOrNull { it.packageName == packageName }
}
