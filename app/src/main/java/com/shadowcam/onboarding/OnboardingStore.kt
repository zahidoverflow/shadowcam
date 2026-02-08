package com.shadowcam.onboarding

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface OnboardingStore {
    val hasDismissedGettingStarted: Flow<Boolean>
    suspend fun setDismissedGettingStarted(value: Boolean)
}

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

class DataStoreOnboardingStore(private val context: Context) : OnboardingStore {
    private val dataStore = context.onboardingDataStore

    override val hasDismissedGettingStarted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[KEY_DISMISSED_GETTING_STARTED] ?: false
    }

    override suspend fun setDismissedGettingStarted(value: Boolean) {
        dataStore.edit { prefs -> prefs[KEY_DISMISSED_GETTING_STARTED] = value }
    }

    private companion object {
        val KEY_DISMISSED_GETTING_STARTED = booleanPreferencesKey("dismissed_getting_started")
    }
}

