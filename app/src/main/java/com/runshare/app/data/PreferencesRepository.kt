package com.runshare.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.runshare.app.model.MapProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 用户偏好设置存储
 */
class PreferencesRepository(private val context: Context) {

    companion object {
        private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

        private val MAP_PROVIDER_KEY = stringPreferencesKey("map_provider")
        private val SHARE_DURATION_KEY = intPreferencesKey("share_duration_minutes")
        private val KEEP_SCREEN_ON_KEY = booleanPreferencesKey("keep_screen_on")
        private val VOICE_PROMPT_KEY = booleanPreferencesKey("voice_prompt")
    }

    /**
     * 获取地图提供商
     */
    val mapProvider: Flow<MapProvider> = context.dataStore.data.map { preferences ->
        val name = preferences[MAP_PROVIDER_KEY] ?: MapProvider.OSM.name
        MapProvider.fromName(name)
    }

    /**
     * 设置地图提供商
     */
    suspend fun setMapProvider(provider: MapProvider) {
        context.dataStore.edit { preferences ->
            preferences[MAP_PROVIDER_KEY] = provider.name
        }
    }

    /**
     * 获取分享时长（分钟）
     */
    val shareDurationMinutes: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[SHARE_DURATION_KEY] ?: 30
    }

    /**
     * 设置分享时长
     */
    suspend fun setShareDuration(minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[SHARE_DURATION_KEY] = minutes
        }
    }

    /**
     * 获取是否保持屏幕常亮
     */
    val keepScreenOn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEEP_SCREEN_ON_KEY] ?: true
    }

    /**
     * 设置保持屏幕常亮
     */
    suspend fun setKeepScreenOn(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEEP_SCREEN_ON_KEY] = enabled
        }
    }

    /**
     * 获取是否开启语音提示
     */
    val voicePromptEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[VOICE_PROMPT_KEY] ?: false
    }

    /**
     * 设置语音提示
     */
    suspend fun setVoicePrompt(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[VOICE_PROMPT_KEY] = enabled
        }
    }
}
