package com.lyltc1.lindachu.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lyltc1.lindachu.data.local.entity.StepEntity
import com.lyltc1.lindachu.util.difficultyLabel
import com.lyltc1.lindachu.util.formatDuration
import com.lyltc1.lindachu.util.parseStringList
import kotlinx.coroutines.delay
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    viewModel: DetailViewModel,
    onBack: () -> Unit,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.dish?.title ?: "菜谱",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onEdit) { Text("编辑") }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }

            uiState.dish == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "菜谱不存在或已删除",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> DishDetailContent(
                uiState = uiState,
                resolveImage = viewModel::resolveImage,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun DishDetailContent(
    uiState: DetailUiState,
    resolveImage: (String?) -> File?,
    modifier: Modifier = Modifier
) {
    val dish = uiState.dish ?: return
    val tags = remember(dish.tags) {
        dish.tags.split(",", "，").map { it.trim() }.filter { it.isNotEmpty() }
    }
    val groupedIngredients = remember(uiState.ingredients) {
        uiState.ingredients.groupBy { it.groupName }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            val cover = resolveImage(dish.coverPath)
            if (cover != null && cover.exists()) {
                AsyncImage(
                    model = cover,
                    contentDescription = dish.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(dish.title, style = MaterialTheme.typography.headlineLarge)
                if (tags.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        tags.forEach { tag -> AssistChip(onClick = {}, label = { Text(tag) }) }
                    }
                }
                Text(
                    text = buildList {
                        uiState.categoryName?.let { add(it) }
                        if (dish.durationMin > 0) add("${dish.durationMin} 分钟")
                        add("难度 ${difficultyLabel(dish.difficulty)}")
                        if (dish.servings > 0) add("${dish.servings} 人份")
                        add("做过 ${dish.cookCount} 次")
                    }.joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (uiState.ingredients.isNotEmpty()) {
            item { SectionTitle("食材") }
            groupedIngredients.forEach { (group, items) ->
                item {
                    Text(
                        text = group,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(items, key = { it.id }) { ingredient ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(ingredient.name, style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = listOf(ingredient.amount, ingredient.unit)
                                .filter { it.isNotBlank() }
                                .joinToString(" "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (uiState.steps.isNotEmpty()) {
            item { SectionTitle("做法") }
            items(uiState.steps, key = { it.id }) { step ->
                StepItem(step = step, resolveImage = resolveImage)
            }
        }

        if (dish.notes.isNotBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("小贴士")
                    Text(dish.notes, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

@Composable
private fun StepItem(
    step: StepEntity,
    resolveImage: (String?) -> File?,
    modifier: Modifier = Modifier
) {
    val images = remember(step.imagePaths) {
        parseStringList(step.imagePaths)
            .mapNotNull { relative -> resolveImage(relative) }
            .filter { it.exists() }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${step.orderIndex + 1}",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = step.text,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.weight(1f)
                )
            }

            images.forEach { file ->
                AsyncImage(
                    model = file,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                )
            }

            val timerSeconds = step.timerSec
            if (timerSeconds != null && timerSeconds > 0) {
                StepTimer(seconds = timerSeconds)
            }
        }
    }
}

@Composable
private fun StepTimer(seconds: Int) {
    var remaining by remember(seconds) { mutableIntStateOf(seconds) }
    var running by remember(seconds) { mutableStateOf(false) }

    LaunchedEffect(seconds, running) {
        while (running && remaining > 0) {
            delay(1_000)
            remaining--
        }
        if (remaining <= 0) running = false
    }

    AssistChip(
        onClick = {
            if (remaining <= 0) {
                remaining = seconds
                running = true
            } else {
                running = !running
            }
        },
        label = {
            Text(
                text = when {
                    remaining <= 0 -> "重新计时 ${formatDuration(seconds)}"
                    running -> "暂停 ${formatDuration(remaining)}"
                    else -> "开始 ${formatDuration(remaining)}"
                }
            )
        },
        leadingIcon = {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
        }
    )
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 4.dp)
    )
}
