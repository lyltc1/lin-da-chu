package com.lyltc1.lindachu.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.repository.DishRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class HomeUiState(
    val categories: List<CategoryEntity> = emptyList(),
    val dishes: List<DishEntity> = emptyList(),
    val selectedCategoryId: String? = null,
    val query: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class HomeViewModel(
    private val repository: DishRepository,
    private val appContext: Context
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<String?>(null)
    private val rawQuery = MutableStateFlow("")

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    @OptIn(FlowPreview::class)
    private val debouncedQuery = rawQuery.debounce(200)

    val dishes: StateFlow<List<DishEntity>> =
        combine(selectedCategoryId, debouncedQuery) { categoryId, query -> categoryId to query }
            .flatMapLatest { (categoryId, query) -> repository.observeDishes(categoryId, query) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val uiState: StateFlow<HomeUiState> = combine(
        categories, dishes, selectedCategoryId, rawQuery
    ) { cats, list, categoryId, q ->
        HomeUiState(cats, list, categoryId, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    init {
        viewModelScope.launch { repository.ensureSeeded() }
    }

    fun onCategorySelected(id: String?) {
        selectedCategoryId.value = if (selectedCategoryId.value == id) null else id
    }

    fun onQueryChanged(query: String) {
        rawQuery.value = query
    }

    /** 把相对路径解析为私有目录下的实际文件，供图片加载使用 */
    fun resolveImage(relativePath: String?): File? =
        relativePath?.let { File(appContext.filesDir, it) }
}

class HomeViewModelFactory(
    private val repository: DishRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return HomeViewModel(repository, appContext) as T
    }
}
