package com.lyltc1.lindachu.ui.edit

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.lyltc1.lindachu.data.local.ImageStorage
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.data.local.entity.DishEntity
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity
import com.lyltc1.lindachu.data.repository.DishRepository
import com.lyltc1.lindachu.util.encodeStringList
import com.lyltc1.lindachu.util.parseStringList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

/** 编辑页使用的食材草稿 */
data class EditableIngredient(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val amount: String = "",
    val unit: String = "",
    val group: String = "主料"
)

/** 编辑页使用的步骤草稿 */
data class EditableStep(
    val id: String = UUID.randomUUID().toString(),
    val text: String = "",
    val imagePaths: List<String> = emptyList(),
    val timerSec: Int? = null
)

data class EditUiState(
    val isNew: Boolean = true,
    val dishId: String = "",
    val title: String = "",
    val categoryId: String? = null,
    val categories: List<CategoryEntity> = emptyList(),
    val tags: String = "",
    val difficulty: Int = 2,
    val durationMin: String = "",
    val servings: String = "2",
    val notes: String = "",
    val ingredients: List<EditableIngredient> = listOf(EditableIngredient()),
    val steps: List<EditableStep> = listOf(EditableStep()),
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val canSave: Boolean get() = title.isNotBlank() && !isSaving
}

class EditViewModel(
    private val repository: DishRepository,
    private val imageStorage: ImageStorage,
    dishId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EditUiState(
            isNew = dishId == null,
            dishId = dishId ?: UUID.randomUUID().toString()
        )
    )
    val uiState: StateFlow<EditUiState> = _uiState

    /** 编辑已有菜谱时保留原始创建时间 */
    private var originalCreatedAt: Long? = null

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                _uiState.update { state ->
                    state.copy(
                        categories = categories,
                        categoryId = state.categoryId ?: categories.firstOrNull()?.id
                    )
                }
            }
        }
        dishId?.let(::load)
    }

    private fun load(id: String) {
        viewModelScope.launch {
            repository.observeDish(id).filterNotNull().take(1).collect { details ->
                originalCreatedAt = details.dish.createdAt
                _uiState.update { state ->
                    state.copy(
                        title = details.dish.title,
                        categoryId = details.dish.categoryId,
                        tags = details.dish.tags,
                        difficulty = details.dish.difficulty,
                        durationMin = details.dish.durationMin.takeIf { it > 0 }?.toString().orEmpty(),
                        servings = details.dish.servings.takeIf { it > 0 }?.toString().orEmpty(),
                        notes = details.dish.notes,
                        ingredients = details.ingredients
                            .map { EditableIngredient(it.id, it.name, it.amount, it.unit, it.groupName) }
                            .ifEmpty { listOf(EditableIngredient()) },
                        steps = details.steps
                            .map { EditableStep(it.id, it.text, parseStringList(it.imagePaths), it.timerSec) }
                            .ifEmpty { listOf(EditableStep()) }
                    )
                }
            }
        }
    }

    fun onTitleChanged(value: String) = _uiState.update { it.copy(title = value, errorMessage = null) }
    fun onTagsChanged(value: String) = _uiState.update { it.copy(tags = value) }
    fun onNotesChanged(value: String) = _uiState.update { it.copy(notes = value) }
    fun onDurationChanged(value: String) = _uiState.update { it.copy(durationMin = value.filter { it.isDigit() }) }
    fun onServingsChanged(value: String) = _uiState.update { it.copy(servings = value.filter { it.isDigit() }) }
    fun onDifficultyChanged(value: Int) = _uiState.update { it.copy(difficulty = value) }
    fun onCategoryChanged(value: String?) = _uiState.update { it.copy(categoryId = value) }

    fun addIngredient() =
        _uiState.update { it.copy(ingredients = it.ingredients + EditableIngredient(group = "主料")) }

    fun updateIngredient(index: Int, value: EditableIngredient) =
        _uiState.update { state ->
            state.copy(ingredients = state.ingredients.toMutableList().apply { set(index, value) })
        }

    fun removeIngredient(index: Int) =
        _uiState.update { state ->
            state.copy(ingredients = state.ingredients.toMutableList().apply { removeAt(index) })
        }

    fun addStep() = _uiState.update { it.copy(steps = it.steps + EditableStep()) }

    fun updateStep(index: Int, value: EditableStep) =
        _uiState.update { state ->
            state.copy(steps = state.steps.toMutableList().apply { set(index, value) })
        }

    fun removeStep(index: Int) =
        _uiState.update { state ->
            state.copy(steps = state.steps.toMutableList().apply { removeAt(index) })
        }

    fun moveStep(from: Int, to: Int) {
        if (to !in _uiState.value.steps.indices) return
        _uiState.update { state ->
            val list = state.steps.toMutableList()
            val item = list.removeAt(from)
            list.add(to, item)
            state.copy(steps = list)
        }
    }

    fun onStepImagePicked(stepIndex: Int, uri: Uri) {
        viewModelScope.launch {
            val relative = imageStorage.save(uri, _uiState.value.dishId)
            if (relative == null) {
                _uiState.update { it.copy(errorMessage = "图片保存失败，请重试") }
                return@launch
            }
            _uiState.update { state ->
                state.copy(
                    steps = state.steps.mapIndexed { index, step ->
                        if (index == stepIndex) step.copy(imagePaths = step.imagePaths + relative)
                        else step
                    },
                    errorMessage = null
                )
            }
        }
    }

    fun removeStepImage(stepIndex: Int, imageIndex: Int) =
        _uiState.update { state ->
            state.copy(
                steps = state.steps.mapIndexed { index, step ->
                    if (index == stepIndex) {
                        step.copy(
                            imagePaths = step.imagePaths.toMutableList().apply { removeAt(imageIndex) }
                        )
                    } else step
                }
            )
        }

    fun save(onDone: () -> Unit) {
        val state = _uiState.value
        val title = state.title.trim()
        if (title.isBlank()) {
            _uiState.update { it.copy(errorMessage = "请先填写菜名") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val now = System.currentTimeMillis()

            val keptIngredients = state.ingredients
                .filter { it.name.isNotBlank() }
                .mapIndexed { index, item ->
                    IngredientEntity(
                        id = item.id,
                        dishId = state.dishId,
                        name = item.name.trim(),
                        amount = item.amount.trim(),
                        unit = item.unit.trim(),
                        groupName = item.group.ifBlank { "主料" },
                        orderIndex = index
                    )
                }

            val keptSteps = state.steps
                .filter { it.text.isNotBlank() || it.imagePaths.isNotEmpty() }
                .mapIndexed { index, item ->
                    StepEntity(
                        id = item.id,
                        dishId = state.dishId,
                        orderIndex = index,
                        text = item.text.trim(),
                        imagePaths = encodeStringList(item.imagePaths),
                        timerSec = item.timerSec?.takeIf { it > 0 }
                    )
                }

            val dish = DishEntity(
                id = state.dishId,
                title = title,
                coverPath = keptSteps.firstOrNull { it.imagePaths != "[]" }
                    ?.let { parseStringList(it.imagePaths).firstOrNull() },
                categoryId = state.categoryId,
                tags = state.tags.trim(),
                difficulty = state.difficulty,
                durationMin = state.durationMin.toIntOrNull() ?: 0,
                servings = state.servings.toIntOrNull() ?: 0,
                notes = state.notes.trim(),
                createdAt = originalCreatedAt ?: now,
                updatedAt = now
            )

            repository.saveDish(dish, keptIngredients, keptSteps)
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }

    fun resolveImage(relativePath: String?): File? = imageStorage.resolve(relativePath)
}

class EditViewModelFactory(
    private val repository: DishRepository,
    private val imageStorage: ImageStorage,
    private val dishId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return EditViewModel(repository, imageStorage, dishId) as T
    }
}
