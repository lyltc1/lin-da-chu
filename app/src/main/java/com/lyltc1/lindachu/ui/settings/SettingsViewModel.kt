package com.lyltc1.lindachu.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.backup.BackupManager
import com.lyltc1.lindachu.data.prefs.SyncPreferences
import com.lyltc1.lindachu.data.repository.DishRepository
import com.lyltc1.lindachu.data.sync.SyncManager
import com.lyltc1.lindachu.data.sync.SyncResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val owner: String = SyncPreferences.DEFAULT_OWNER,
    val repo: String = SyncPreferences.DEFAULT_REPO,
    val token: String = "",
    val isSyncing: Boolean = false,
    val isBusy: Boolean = false,
    val lastSyncAt: Long = 0L,
    val message: String? = null
) {
    val hasCredentials: Boolean get() = token.isNotBlank() && owner.isNotBlank() && repo.isNotBlank()
}

class SettingsViewModel(
    private val repository: DishRepository,
    private val syncManager: SyncManager,
    private val backupManager: BackupManager,
    private val preferences: SyncPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    owner = preferences.getOwner(),
                    repo = preferences.getRepo(),
                    token = preferences.getToken(),
                    lastSyncAt = preferences.getLastSyncAt()
                )
            }
        }
    }

    fun onOwnerChanged(value: String) = _uiState.update { it.copy(owner = value, message = null) }
    fun onRepoChanged(value: String) = _uiState.update { it.copy(repo = value, message = null) }
    fun onTokenChanged(value: String) = _uiState.update { it.copy(token = value, message = null) }

    fun saveCredentials() {
        viewModelScope.launch {
            val state = _uiState.value
            preferences.setToken(state.token)
            preferences.setOwner(state.owner)
            preferences.setRepo(state.repo)
            _uiState.update { it.copy(message = "凭据已保存到本机") }
        }
    }

    fun sync() {
        if (_uiState.value.isSyncing) return
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, message = "正在同步…") }
            when (val result = syncManager.sync()) {
                is SyncResult.Success -> _uiState.update {
                    it.copy(
                        isSyncing = false,
                        lastSyncAt = result.syncedAt,
                        message = "同步完成：下载 ${result.pulled} 条，上传 ${result.pushed} 条"
                    )
                }

                is SyncResult.Failure -> _uiState.update {
                    it.copy(isSyncing = false, message = "同步失败：${result.message}")
                }
            }
        }
    }

    fun exportBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = "正在导出备份…") }
            val result = runCatching {
                backupManager.export(uri, repository.exportPayload()).getOrThrow()
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = if (result.isSuccess) "备份已导出" else "导出失败：${result.exceptionOrNull()?.message}"
                )
            }
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = "正在导入备份…") }
            val result = runCatching {
                repository.restore(backupManager.import(uri).getOrThrow())
            }
            _uiState.update {
                it.copy(
                    isBusy = false,
                    message = if (result.isSuccess) "备份已导入" else "导入失败：${result.exceptionOrNull()?.message}"
                )
            }
        }
    }
}

class SettingsViewModelFactory(
    private val repository: DishRepository,
    private val syncManager: SyncManager,
    private val backupManager: BackupManager,
    private val preferences: SyncPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return SettingsViewModel(repository, syncManager, backupManager, preferences) as T
    }
}
