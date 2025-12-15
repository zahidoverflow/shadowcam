package com.shadowcam.sources

import com.shadowcam.core.model.SourceDescriptor
import com.shadowcam.core.model.SourceId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface SourceRepository {
    val sources: Flow<List<SourceDescriptor>>
    suspend fun add(source: SourceDescriptor)
    suspend fun remove(id: SourceId)
    suspend fun find(id: SourceId): SourceDescriptor?
    suspend fun setDefault(sourceId: SourceId)
    val defaultSourceId: Flow<SourceId?>
}

class InMemorySourceRepository : SourceRepository {
    private val backing = MutableStateFlow<List<SourceDescriptor>>(emptyList())
    private val defaultId = MutableStateFlow<SourceId?>(null)

    override val sources: Flow<List<SourceDescriptor>> = backing
    override val defaultSourceId: Flow<SourceId?> = defaultId

    override suspend fun add(source: SourceDescriptor) {
        backing.value = backing.value + source
        if (defaultId.value == null) defaultId.value = source.id
    }

    override suspend fun remove(id: SourceId) {
        backing.value = backing.value.filterNot { it.id == id }
        if (defaultId.value == id) defaultId.value = backing.value.firstOrNull()?.id
    }

    override suspend fun find(id: SourceId): SourceDescriptor? =
        backing.value.firstOrNull { it.id == id }

    override suspend fun setDefault(sourceId: SourceId) {
        defaultId.value = sourceId
    }
}
