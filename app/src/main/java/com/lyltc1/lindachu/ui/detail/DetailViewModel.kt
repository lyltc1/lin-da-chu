package com.lyltc1.lindachu.ui.detail

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity
import com.lyltc1.lindachu.data.repository.DishRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.io.File

data class DetailUiState(
    val isLoading: Boolean = true,
    val dish: DishEntity? = null,
    val categoryName: String? = null,
    val ingredients: List<IngredientEntity> = emptyList(),
    val steps: List<StepEntity> = emptyList()
)

class DetailViewModel(
    repository: DishRepository,
    dishId: String,
    private val appContext: Context
) : ViewModel() {

    val uiState: StateFlow<DetailUiState> = combine(
        repository.observeDish(dishId),
        repository.observeCategories()
    ) { details, categories ->
        if (details == null) {
            DetailUiState(isLoading = false)
        } else {
            DetailUiState(
                isLoading = false,
                dish = details.dish,
                categoryName = categories.firstOrNull { it.id == details.dish.categoryId }?.name,
                ingredients = details.ingredients,
                steps = details.steps
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailUiState())

    fun resolveImage(relativePath: String?): File? =
        relativePath?.let { File(appContext.filesDir, it) }
}

class DetailViewModelFactory(
    private val repository: DishRepository,
    private val dishId: String,
    private val appContext: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return DetailViewModel(repository, dishId, appContext) as T
    }
}
