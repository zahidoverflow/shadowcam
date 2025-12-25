package com.shadowcam.apps

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.shadowcam.core.model.LogLevel
import com.shadowcam.logging.LogSink
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class TargetApp(
    val packageName: String,
    val label: String
)

interface TargetAppStore {
    val selected: Flow<TargetApp?>
    suspend fun setTarget(app: TargetApp?)
}

private val Context.targetAppDataStore by preferencesDataStore(name = "target_app")

class DataStoreTargetAppStore(
    private val context: Context,
    private val logSink: LogSink
) : TargetAppStore {
    private val dataStore = context.targetAppDataStore

    override val selected: Flow<TargetApp?> = dataStore.data.map { prefs ->
        val packageName = prefs[KEY_PACKAGE] ?: return@map null
        val label = prefs[KEY_LABEL] ?: packageName
        TargetApp(packageName, label)
    }

    override suspend fun setTarget(app: TargetApp?) {
        dataStore.edit { prefs ->
            if (app == null) {
                prefs.remove(KEY_PACKAGE)
                prefs.remove(KEY_LABEL)
            } else {
                prefs[KEY_PACKAGE] = app.packageName
                prefs[KEY_LABEL] = app.label
            }
        }
        logSink.log(
            LogLevel.INFO,
            "Target",
            if (app == null) "Target cleared" else "Target set",
            if (app == null) emptyMap() else mapOf("package" to app.packageName, "label" to app.label)
        )
    }

    companion object {
        private val KEY_PACKAGE = stringPreferencesKey("target_package")
        private val KEY_LABEL = stringPreferencesKey("target_label")
    }
}
