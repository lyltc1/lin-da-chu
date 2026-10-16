package com.lyltc1.lindachu.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.prefs.SyncPreferences
import com.lyltc1.lindachu.data.repository.DishRepository
import com.lyltc1.lindachu.data.sync.SyncManager
import com.lyltc1.lindachu.data.sync.SyncResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

/** 首页同步相关的状态，聚成一个对象以便和其它流一起 combine */
private data class SyncStatus(
    val hasCredentials: Boolean = false,
    val isSyncing: Boolean = false,
    val message: String? = null
)

data class HomeUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val dishes: List<DishEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val query: String = "",
    /** 是否已填过 GitHub 凭据，决定空状态给的是「同步」还是「去设置」 */
    val hasCredentials: Boolean = false,
    val isSyncing: Boolean = false,
    val syncMessage: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val repository: DishRepository,
    private val syncManager: SyncManager,
    private val preferences: SyncPreferences,
    private val appContext: Context
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val rawQuery = MutableStateFlow("")
    private val syncStatus = MutableStateFlow(SyncStatus())

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class)
    private val debouncedQuery = rawQuery.debounce(200)

    val dishes: StateFlow<List<DishEntity>> =
        combine(selectedCategoryId, debouncedQuery) { categoryId, query -> categoryId to query }
            .flatMapLatest { (categoryId, query) -> repository.observeDishes(categoryId, query) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        categories, dishes, selectedCategoryId, rawQuery, syncStatus
    ) { cats, list, categoryId, q, sync ->
        HomeUiState(cats, list, categoryId, q, sync.hasCredentials, sync.isSyncing, sync.message)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { repository.ensureSeeded() }
        viewModelScope.launch {
            syncStatus.update {
                it.copy(
                    hasCredentials = preferences.getToken().isNotBlank() &&
                        preferences.getOwner().isNotBlank() &&
                        preferences.getRepo().isNotBlank()
                )
            }
        }
    }

    fun onCategorySelected(id: String?) {
        selectedCategoryId.value = if (selectedCategoryId.value == id) null else id
    }

    fun onQueryChanged(query: String) {
        rawQuery.value = query
    }

    /**
     * 空状态里的「立即同步」：把 GitHub 仓库中的菜谱拉到本机。
     *
     * 新装设备上本地是空的，同步只会下载、不会上传，
     * 所以这一步可以放心交给用户点，不必担心覆盖云端数据。
     */
    fun sync() {
        if (syncStatus.value.isSyncing) return
        viewModelScope.launch {
            syncStatus.update { it.copy(isSyncing = true, message = "正在同步…") }
            when (val result = syncManager.sync()) {
                is SyncResult.Success -> syncStatus.update {
                    it.copy(
                        isSyncing = false,
                        message = if (result.pulled > 0) "同步完成，下载 ${result.pulled} 道菜"
                        else "已是最新，没有需要下载的菜谱"
                    )
                }

                is SyncResult.Failure -> syncStatus.update {
                    it.copy(isSyncing = false, message = "同步失败：${result.message}")
                }
            }
        }
    }

    /** 把相对路径解析为私有目录下的实际文件，供图片加载使用 */
    fun resolveImage(relativePath: String?): File? =
        relativePath?.let { File(appContext.filesDir, it) }
}

class HomeViewModelFactory(
    private val repository: DishRepository,
    private val syncManager: SyncManager,
    private val preferences: SyncPreferences,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository, syncManager, preferences, appContext) as T
    }
}
