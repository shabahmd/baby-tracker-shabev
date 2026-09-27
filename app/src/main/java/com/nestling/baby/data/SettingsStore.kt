package com.nestling.baby.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nestling.baby.domain.AppSettings
import com.nestling.baby.domain.Side
import com.nestling.baby.domain.VolumeUnit
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

interface SettingsStore {
    val settings: Flow<AppSettings>

    suspend fun update(transform: (AppSettings) -> AppSettings)
}

private val Context.nestlingPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "nestling_settings",
)

class DataStoreSettings(context: Context) : SettingsStore {

    private val store = context.applicationContext.nestlingPreferences

    override val settings: Flow<AppSettings> = store.data
        .catch { cause ->
            // A corrupt preferences file must never take the app down — the log is what matters.
            if (cause is IOException) emit(emptyPreferences()) else throw cause
        }
        .map { it.toSettings() }

    override suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { prefs ->
            val next = transform(prefs.toSettings())
            prefs[Keys.BABY_NAME] = next.babyName
            prefs[Keys.NIGHT_MODE] = next.nightMode
            prefs[Keys.DYNAMIC_COLOR] = next.dynamicColor
            prefs[Keys.UNIT] = next.unit.name
            prefs[Keys.LAST_AMOUNT] = next.lastAmountMl
            prefs[Keys.LAST_SIDE] = next.lastSide?.name.orEmpty()
            prefs[Keys.NIGHT_PROMPT_SUPPRESSED] = next.nightPromptSuppressedOn
        }
    }

    private object Keys {
        val BABY_NAME = stringPreferencesKey("baby_name")
        val NIGHT_MODE = booleanPreferencesKey("night_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val UNIT = stringPreferencesKey("unit")
        val LAST_AMOUNT = intPreferencesKey("last_amount_ml")
        val LAST_SIDE = stringPreferencesKey("last_side")
        val NIGHT_PROMPT_SUPPRESSED = stringPreferencesKey("night_prompt_suppressed_on")
    }

    private fun Preferences.toSettings(): AppSettings {
        val defaults = AppSettings()
        return AppSettings(
            babyName = this[Keys.BABY_NAME]?.takeIf { it.isNotBlank() } ?: defaults.babyName,
            nightMode = this[Keys.NIGHT_MODE] ?: defaults.nightMode,
            dynamicColor = this[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            unit = VolumeUnit.fromStorage(this[Keys.UNIT]),
            lastAmountMl = this[Keys.LAST_AMOUNT] ?: defaults.lastAmountMl,
            lastSide = Side.fromStorage(this[Keys.LAST_SIDE]),
            nightPromptSuppressedOn = this[Keys.NIGHT_PROMPT_SUPPRESSED] ?: "",
        )
    }
}
