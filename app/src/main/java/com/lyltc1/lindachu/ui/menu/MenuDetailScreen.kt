package com.lyltc1.lindachu.ui.menu

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lyltc1.lindachu.data.local.ImageStorage
import com.lyltc1.lindachu.data.local.entity.DishEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MenuDetailScreen(
    viewModel: MenuDetailViewModel,
    imageStorage: ImageStorage,
    onPickDishes: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val dishes by viewModel.dishes.collectAsStateWithLifecycle()
    val savedId by viewModel.savedId.collectAsStateWithLifecycle()
    val savedMenuId by viewModel.savedMenuId.collectAsStateWithLifecycle()

    // 保存成功后带着菜单 id 去点菜
    LaunchedEffect(savedMenuId) {
        savedMenuId?.let {
            viewModel.consumeSavedMenuId()
            onPickDishes(it)
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(if (draft.title.isBlank()) "新建菜单" else draft.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::saveAndPick,
                        enabled = draft.selected.isNotEmpty() && draft.title.isNotBlank()
                    ) {
                        Text(if (savedId == null) "保存并去点菜" else "去点菜")
                    }
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
            item {
                OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = draft.title,
                            onValueChange = viewModel::onTitleChanged,
                            label = { Text("菜单名") },
                            placeholder = { Text("如：周末家宴") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = draft.greeting,
                            onValueChange = viewModel::onGreetingChanged,
                            label = { Text("招呼语") },
                            placeholder = { Text("如：欢迎来我家吃饭，随便点～") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "选择要上菜单的菜",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "已选 ${draft.selected.size} 道",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (dishes.isEmpty()) {
                item {
                    Text(
                        text = "菜谱库还是空的，先去添加几道菜吧",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(dishes, key = { it.id }) { dish ->
                    DishSelectRow(
                        dish = dish,
                        imageStorage = imageStorage,
                        checked = draft.selected.contains(dish.id),
                        order = draft.selected.indexOf(dish.id).takeIf { it >= 0 }?.plus(1),
                        onToggle = { viewModel.toggleDish(dish.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DishSelectRow(
    dish: DishEntity,
    imageStorage: ImageStorage,
    checked: Boolean,
    order: Int?,
    onToggle: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable(onClick = onToggle)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val coverPath = dish.coverPath
            if (coverPath != null && imageStorage.resolve(coverPath)?.exists() == true) {
                AsyncImage(
                    model = imageStorage.resolve(coverPath),
                    contentDescription = dish.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(dish.title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${dish.durationMin} 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (order != null) {
                Text(
                    text = "#$order",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Checkbox(checked = checked, onCheckedChange = { onToggle() })
        }
    }
}
