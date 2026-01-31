package com.runshare.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.runshare.app.model.MapProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

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
        
        // 新增：用户和共享设置
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val USER_ID_KEY = stringPreferencesKey("user_id")
        private val SERVER_URL_KEY = stringPreferencesKey("server_url")
        private val SHARING_ENABLED_KEY = booleanPreferencesKey("sharing_enabled")
        private val LOGGED_IN_KEY = booleanPreferencesKey("logged_in")
    }

    /**
     * 获取地图提供商
     */
    val mapProvider: Flow<MapProvider> = context.dataStore.data.map { preferences ->
        val name = preferences[MAP_PROVIDER_KEY] ?: MapProvider.AMAP.name
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

    // ========== 用户和共享设置 ==========

    /**
     * 获取用户名
     */
    val username: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USERNAME_KEY] ?: "跑步者"
    }

    /**
     * 设置用户名
     */
    suspend fun setUsername(name: String) {
        context.dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = name
        }
    }

    /**
     * 获取用户唯一ID（自动生成）
     */
    val userId: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[USER_ID_KEY] ?: ""
    }

    /**
     * 获取或生成用户ID
     */
    suspend fun getOrCreateUserId(): String {
        val existing = context.dataStore.data.first()[USER_ID_KEY]
        if (!existing.isNullOrEmpty()) return existing
        
        val newId = UUID.randomUUID().toString().replace("-", "").take(16)
        context.dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = newId
        }
        return newId
    }

    /**
     * 获取服务器地址
     */
    val serverUrl: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[SERVER_URL_KEY] ?: ""
    }

    /**
     * 设置服务器地址
     */
    suspend fun setServerUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[SERVER_URL_KEY] = url
        }
    }

    /**
     * 获取是否正在共享位置
     */
    val sharingEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[SHARING_ENABLED_KEY] ?: false
    }

    /**
     * 设置位置共享开关
     */
    suspend fun setSharingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[SHARING_ENABLED_KEY] = enabled
        }
    }

    /**
     * 获取是否已登录
     */
    val isLoggedIn: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LOGGED_IN_KEY] ?: false
    }

    /**
     * 设置登录状态
     */
    suspend fun setLoggedIn(loggedIn: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LOGGED_IN_KEY] = loggedIn
        }
    }
}

