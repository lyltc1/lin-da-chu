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
 * 令牌仅存于本机 DataStore，不随备份 zip 导出，避免换机时泄露。
 */
class SyncPreferences(private val context: Context) {

    private object Keys {
        val TOKEN = stringPreferencesKey("github_token")
        val OWNER = stringPreferencesKey("repo_owner")
        val REPO = stringPreferencesKey("repo_name")
        val LAST_SYNC_AT = longPreferencesKey("last_sync_at")
    }

    suspend fun getToken(): String = context.syncDataStore.data.first()[Keys.TOKEN].orEmpty()

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
    }
}
