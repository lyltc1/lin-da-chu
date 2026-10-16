package com.lyltc1.lindachu.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync")

/**
 * 同步相关偏好：GitHub 令牌、数据仓库坐标、上次同步时间。
 *
 * 令牌只存在本机 DataStore，不随备份 zip 导出，避免换机时泄露；
 * 没填就是空，同步时会提示去设置页填——不内置任何令牌。
 */
class SyncPreferences(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("github_token")
        val OWNER = stringPreferencesKey("repo_owner")
        val REPO = stringPreferencesKey("repo_name")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    /** 用户在设置里填过的令牌；没填返回空串，由调用方提示去设置页填写 */
    suspend fun getToken(): String =
        context.syncDataStore.data.first()[Keys.TOKEN]?.takeIf { it.isNotBlank() } ?: BUILT_IN_TOKEN

    suspend fun setToken(value: String) {
        context.syncDataStore.edit { it[Keys.TOKEN] = value.trim() }
    }

    suspend fun getOwner(): String =
        context.syncDataStore.data.first()[Keys.OWNER]?.takeIf { it.isNotBlank() } ?: DEFAULT_OWNER

    suspend fun setOwner(value: String) {
        context.syncDataStore.edit { it[Keys.OWNER] = value.trim() }
    }

    suspend fun getRepo(): String =
        context.syncDataStore.data.first()[Keys.REPO]?.takeIf { it.isNotBlank() } ?: DEFAULT_REPO

    suspend fun setRepo(value: String) {
        context.syncDataStore.edit { it[Keys.REPO] = value.trim() }
    }

    suspend fun getLastSyncAt(): Long = context.syncDataStore.data.first()[Keys.LAST_SYNC_AT] ?: 0L

    fun observeLastSyncAt(): Flow<Long> =
        context.syncDataStore.data.map { it[Keys.LAST_SYNC_AT] ?: 0L }

    suspend fun setLastSyncAt(value: Long) {
        context.syncDataStore.edit { it[Keys.LAST_SYNC_AT] = value }
    }

    companion object {
        const val DEFAULT_OWNER = "lyltc1"
        const val DEFAULT_REPO = "lin-da-chu-data"

        /**
         * 这里曾经内置过一枚令牌，好让新设备开箱就能同步。
         *
         * 留空的原因：写在源码里就等于编译进 APK——安装包发出去，
         * 拿到的人反编译就能取出；源码推到公开仓库时，
         * GitHub 的密钥扫描还会当场把它吊销。
         *
         * 现在令牌一律在「设置与同步」页填写，只存在本机 DataStore。
         */
        private const val BUILT_IN_TOKEN = ""
    }
}
