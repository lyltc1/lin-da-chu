package com.lyltc1.lindachu.ui.edit

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lyltc1.lindachu.data.local.entity.CategoryEntity
import com.lyltc1.lindachu.util.formatDuration
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditScreen(
    viewModel: EditViewModel,
    onSaved: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // 系统相册一次只能回调 Uri，用下标记住当前在为哪个步骤配图
    var pendingStepIndex by remember { mutableIntStateOf(-1) }
    val pickImage = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null && pendingStepIndex >= 0) {
            viewModel.onStepImagePicked(pendingStepIndex, uri)
        }
        pendingStepIndex = -1
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNew) "新建菜谱" else "编辑菜谱") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = uiState.canSave
                    ) { Text("保存") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { BasicInfoSection(uiState, viewModel) }
            item { IngredientsSection(uiState, viewModel) }
            item {
                StepsSection(
                    uiState = uiState,
                    viewModel = viewModel,
                    onPickImage = { index ->
                        pendingStepIndex = index
                        pickImage.launch(
                            PickVisualMediaRequest(
                                ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun BasicInfoSection(uiState: EditUiState, viewModel: EditViewModel) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = uiState.title,
                onValueChange = viewModel::onTitleChanged,
                label = { Text("菜名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("分类", style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(uiState.categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = uiState.categoryId == category.id,
                        onClick = { viewModel.onCategoryChanged(category.id) },
                        label = { Text(category.name) }
                    )
                }
            }

            OutlinedTextField(
                value = uiState.tags,
                onValueChange = viewModel::onTagsChanged,
                label = { Text("标签（逗号分隔）") },
                placeholder = { Text("如：快手,家常") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.aliases,
                onValueChange = viewModel::onAliasesChanged,
                label = { Text("别名（逗号分隔）") },
                placeholder = { Text("如：手撕包菜,炒包菜") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.durationMin,
                onValueChange = viewModel::onDurationChanged,
                label = { Text("耗时/分钟") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.review,
                onValueChange = viewModel::onReviewChanged,
                label = { Text("评价") },
                placeholder = { Text("如：简单清爽，色泽翠绿") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = uiState.notes,
                onValueChange = viewModel::onNotesChanged,
                label = { Text("小贴士") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun IngredientsSection(uiState: EditUiState, viewModel: EditViewModel) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("食材", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = viewModel::addIngredient) { Text("添加") }
            }

            uiState.ingredients.forEachIndexed { index, ingredient ->
                IngredientEditorRow(
                    ingredient = ingredient,
                    onChange = { viewModel.updateIngredient(index, it) },
                    onRemove = { viewModel.removeIngredient(index) },
                    canRemove = uiState.ingredients.size > 1
                )
            }
        }
    }
}

@Composable
private fun IngredientEditorRow(
    ingredient: EditableIngredient,
    onChange: (EditableIngredient) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    val groups = listOf("主料", "辅料", "调料")

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = ingredient.name,
                onValueChange = { onChange(ingredient.copy(name = it)) },
                label = { Text("名称") },
                singleLine = true,
                modifier = Modifier.weight(2f)
            )
            Spacer8()
            OutlinedTextField(
                value = ingredient.amount,
                onValueChange = { onChange(ingredient.copy(amount = it)) },
                label = { Text("用量") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            Spacer8()
            OutlinedTextField(
                value = ingredient.unit,
                onValueChange = { onChange(ingredient.copy(unit = it)) },
                label = { Text("单位") },
                singleLine = true,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onRemove, enabled = canRemove) {
                Icon(Icons.Default.Delete, contentDescription = "删除食材")
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            groups.forEach { group ->
                FilterChip(
                    selected = ingredient.group == group,
                    onClick = { onChange(ingredient.copy(group = group)) },
                    label = { Text(group) }
                )
            }
        }
    }
}

@Composable
private fun StepsSection(
    uiState: EditUiState,
    viewModel: EditViewModel,
    onPickImage: (Int) -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("做法", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = viewModel::addStep) { Text("添加步骤") }
            }

            uiState.steps.forEachIndexed { index, step ->
                StepEditorCard(
                    step = step,
                    index = index,
                    stepCount = uiState.steps.size,
                    resolveImage = viewModel::resolveImage,
                    onChange = { viewModel.updateStep(index, it) },
                    onRemove = { viewModel.removeStep(index) },
                    onMoveUp = { viewModel.moveStep(index, index - 1) },
                    onMoveDown = { viewModel.moveStep(index, index + 1) },
                    onPickImage = { onPickImage(index) },
                    onRemoveImage = { imageIndex -> viewModel.removeStepImage(index, imageIndex) }
                )
            }
        }
    }
}

@Composable
private fun StepEditorCard(
    step: EditableStep,
    index: Int,
    stepCount: Int,
    resolveImage: (String?) -> File?,
    onChange: (EditableStep) -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onPickImage: () -> Unit,
    onRemoveImage: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "第 ${index + 1} 步",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onMoveUp, enabled = index > 0) { Text("上移") }
                TextButton(onClick = onMoveDown, enabled = index < stepCount - 1) { Text("下移") }
                IconButton(onClick = onRemove, enabled = stepCount > 1) {
                    Icon(Icons.Default.Delete, contentDescription = "删除步骤")
                }
            }

            OutlinedTextField(
                value = step.summary,
                onValueChange = { onChange(step.copy(summary = it)) },
                label = { Text("小结（可选）") },
                placeholder = { Text("如：择菜、洗菜、热油") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = step.text,
                onValueChange = { onChange(step.copy(text = it)) },
                label = { Text("这一步做什么") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )

            if (step.imagePaths.isNotEmpty()) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    itemsIndexed(step.imagePaths) { imageIndex, relative ->
                        Box(modifier = Modifier.size(88.dp)) {
                            val file = resolveImage(relative)
                            if (file != null && file.exists()) {
                                AsyncImage(
                                    model = file,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            IconButton(
                                onClick = { onRemoveImage(imageIndex) },
                                modifier = Modifier.align(Alignment.TopEnd)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "移除图片")
                            }
                        }
                    }
                }
            }

            AssistChip(
                onClick = onPickImage,
                label = { Text("添加图片") },
                leadingIcon = {
                    Icon(Icons.Default.Add, contentDescription = null)
                }
            )
        }
    }
}

@Composable
private fun Spacer8() = Spacer(modifier = Modifier.size(8.dp))
