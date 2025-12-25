package com.shadowcam.profiles

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shadowcam.core.model.AntiDetectLevel
import com.shadowcam.core.model.AppProfile
import com.shadowcam.core.model.Resolution
import com.shadowcam.logging.LogSink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.profileDataStore by preferencesDataStore(name = "profiles_store")

class DataStoreProfileStore(
    context: Context,
    private val logSink: LogSink
) : ProfileStore {
    private val dataStore = context.profileDataStore

    override val profiles: Flow<List<AppProfile>> = dataStore.data.map { prefs ->
        decodeProfiles(prefs[KEY_PROFILES_JSON])
    }

    override suspend fun upsert(profile: AppProfile) {
        dataStore.edit { prefs ->
            val current = decodeProfiles(prefs[KEY_PROFILES_JSON])
            val updated = current.filterNot { it.packageName == profile.packageName } + profile
            prefs[KEY_PROFILES_JSON] = encodeProfiles(updated)
        }
        logSink.log(
            com.shadowcam.core.model.LogLevel.INFO,
            "Profiles",
            "Profile saved",
            mapOf("package" to profile.packageName)
        )
    }

    override suspend fun remove(packageName: String) {
        dataStore.edit { prefs ->
            val current = decodeProfiles(prefs[KEY_PROFILES_JSON])
            val updated = current.filterNot { it.packageName == packageName }
            prefs[KEY_PROFILES_JSON] = encodeProfiles(updated)
        }
        logSink.log(
            com.shadowcam.core.model.LogLevel.INFO,
            "Profiles",
            "Profile removed",
            mapOf("package" to packageName)
        )
    }

    override suspend fun get(packageName: String): AppProfile? {
        val current = dataStore.data.first()
        return decodeProfiles(current[KEY_PROFILES_JSON]).firstOrNull { it.packageName == packageName }
    }

    private fun encodeProfiles(profiles: List<AppProfile>): String {
        val array = JSONArray()
        profiles.forEach { profile ->
            val obj = JSONObject()
            obj.put("packageName", profile.packageName)
            obj.put("label", profile.label)
            obj.put("width", profile.resolution.width)
            obj.put("height", profile.resolution.height)
            obj.put("fps", profile.fps.toDouble())
            obj.put("antiDetect", profile.antiDetectLevel.name)
            profile.defaultSourceId?.let { obj.put("defaultSourceId", it) }
            obj.put("exifStrategy", profile.exifStrategy)
            array.put(obj)
        }
        return array.toString()
    }

    private fun decodeProfiles(raw: String?): List<AppProfile> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(raw)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    val packageName = obj.optString("packageName")
                    val label = obj.optString("label", packageName)
                    val width = obj.optInt("width", 1280)
                    val height = obj.optInt("height", 720)
                    val fps = obj.optDouble("fps", 30.0).toFloat()
                    val antiDetect = obj.optString("antiDetect", AntiDetectLevel.SAFE.name)
                    val defaultSourceId = obj.optString("defaultSourceId").takeIf { it.isNotBlank() }
                    val exifStrategy = obj.optString("exifStrategy", "balanced")
                    if (packageName.isNotBlank()) {
                        add(
                            AppProfile(
                                packageName = packageName,
                                label = label,
                                resolution = Resolution(width, height),
                                fps = fps,
                                antiDetectLevel = runCatching { AntiDetectLevel.valueOf(antiDetect) }
                                    .getOrDefault(AntiDetectLevel.SAFE),
                                defaultSourceId = defaultSourceId,
                                exifStrategy = exifStrategy
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            logSink.log(
                com.shadowcam.core.model.LogLevel.ERROR,
                "Profiles",
                "Profile decode failed",
                mapOf("error" to (e.message ?: "unknown"))
            )
            emptyList()
        }
    }

    companion object {
        private val KEY_PROFILES_JSON = stringPreferencesKey("profiles_json")
    }
}
