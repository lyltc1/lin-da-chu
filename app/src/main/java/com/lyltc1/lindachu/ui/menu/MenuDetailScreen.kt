package com.lyltc1.lindachu.ui.menu

import android.graphics.Bitmap
import androidx.compose.foundation.Image
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
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
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val dishes by viewModel.dishes.collectAsStateWithLifecycle()
    val publishState by viewModel.publishState.collectAsStateWithLifecycle()

    val pageUrl = (publishState as? PublishState.Done)?.url.orEmpty()
    val qrBitmap: Bitmap? = remember(pageUrl) {
        if (pageUrl.isNotBlank()) com.lyltc1.lindachu.util.generateQrCode(pageUrl) else null
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
                Text("选择要上菜单的菜", style = MaterialTheme.typography.titleLarge)
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

            item {
                Button(
                    onClick = viewModel::publish,
                    enabled = publishState !is PublishState.Publishing &&
                        draft.selected.isNotEmpty() &&
                        draft.title.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when (publishState) {
                            is PublishState.Publishing -> "发布中…"
                            is PublishState.Done -> "重新发布"
                            else -> "发布到 GitHub Pages"
                        }
                    )
                }
            }

            if (publishState is PublishState.Publishing) {
                item {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }

            if (publishState is PublishState.Failed) {
                item {
                    val message = (publishState as PublishState.Failed).message
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            text = message,
                            modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }

            if (qrBitmap != null) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("让朋友扫这个码看菜单", style = MaterialTheme.typography.titleMedium)
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "菜单二维码",
                                modifier = Modifier.size(220.dp),
                                contentScale = ContentScale.Fit
                            )
                            Text(
                                text = pageUrl,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
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
                    text = "难度 ${dish.difficulty} · ${dish.durationMin} 分钟 · ${dish.servings} 人份",
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
