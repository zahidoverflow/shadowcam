package com.shadowcam.root

import android.content.Context
import android.net.Uri
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class RootSettings(
    val videoUri: String?,
    val videoName: String?,
    val imageUri: String?,
    val imageName: String?
)

private val Context.rootSettingsStore by preferencesDataStore(name = "root_settings")

class RootSettingsStore(private val context: Context) {
    private val dataStore = context.rootSettingsStore

    val settings: Flow<RootSettings> = dataStore.data.map { prefs ->
        RootSettings(
            videoUri = prefs[KEY_VIDEO_URI],
            videoName = prefs[KEY_VIDEO_NAME],
            imageUri = prefs[KEY_IMAGE_URI],
            imageName = prefs[KEY_IMAGE_NAME]
        )
    }

    suspend fun saveVideo(uri: Uri, name: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_VIDEO_URI] = uri.toString()
            if (name.isNullOrBlank()) {
                prefs.remove(KEY_VIDEO_NAME)
            } else {
                prefs[KEY_VIDEO_NAME] = name
            }
        }
    }

    suspend fun saveImage(uri: Uri, name: String?) {
        dataStore.edit { prefs ->
            prefs[KEY_IMAGE_URI] = uri.toString()
            if (name.isNullOrBlank()) {
                prefs.remove(KEY_IMAGE_NAME)
            } else {
                prefs[KEY_IMAGE_NAME] = name
            }
        }
    }

    companion object {
        private val KEY_VIDEO_URI = stringPreferencesKey("video_uri")
        private val KEY_VIDEO_NAME = stringPreferencesKey("video_name")
        private val KEY_IMAGE_URI = stringPreferencesKey("image_uri")
        private val KEY_IMAGE_NAME = stringPreferencesKey("image_name")
    }
}
