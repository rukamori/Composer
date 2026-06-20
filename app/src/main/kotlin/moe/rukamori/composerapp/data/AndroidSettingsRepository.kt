package moe.rukamori.composerapp.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import moe.rukamori.composerapp.domain.repository.ComposerSettings
import moe.rukamori.composerapp.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

private val Context.composerDataStore by preferencesDataStore(name = "composer_settings")

@Singleton
class AndroidSettingsRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : SettingsRepository {
        override val settings: Flow<ComposerSettings> =
            context.composerDataStore.data.map { preferences ->
                ComposerSettings(
                    snapEnabled = preferences[SnapEnabledKey] ?: true,
                    useDynamicColor = preferences[DynamicColorKey] ?: true,
                )
            }

        override suspend fun setSnapEnabled(enabled: Boolean) {
            context.composerDataStore.edit { it[SnapEnabledKey] = enabled }
        }

        override suspend fun setUseDynamicColor(enabled: Boolean) {
            context.composerDataStore.edit { it[DynamicColorKey] = enabled }
        }

        private companion object {
            val SnapEnabledKey = booleanPreferencesKey("snap_enabled")
            val DynamicColorKey = booleanPreferencesKey("dynamic_color")
        }
    }
