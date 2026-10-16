package com.lyltc1.lindachu.ui.order

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lyltc1.lindachu.data.local.entity.DishEntity
import kotlinx.coroutines.launch

/**
 * 点菜页：大家轮流在这台手机上加减想吃的菜。
 *
 * 菜按分类分成家常菜、汤等几组，左侧是分类索引，
 * 点一下右侧就滚到那一组——人多的时候不用翻半天找菜。
 *
 * 全部操作都在本机，点完生成一份点单，随后跳到汇总页截图即可。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickDishesScreen(
    viewModel: PickDishesViewModel,
    onGenerated: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sections by viewModel.sections.collectAsStateWithLifecycle()
    val savedOrderId by viewModel.savedOrderId.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(savedOrderId) {
        savedOrderId?.let {
            viewModel.consumeSavedOrderId()
            onGenerated(it)
        }
    }

    LaunchedEffect(uiState.message) {
        uiState.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    // 每个分类在右侧列表里的起始位置：第 0 项是提示语，之后是「组标题 + N 道菜」
    val sectionOffsets = remember(sections) {
        val offsets = linkedMapOf<String, Int>()
        var index = 1
        sections.forEach { section ->
            offsets[section.id] = index
            index += 1 + section.dishes.size
        }
        offsets
    }

    // 右侧滚到哪一组，左侧就高亮哪个分类
    val currentSectionId by remember(sections) {
        derivedStateOf {
            val index = listState.firstVisibleItemIndex
            sectionOffsets.entries.lastOrNull { it.value <= index }?.key
                ?: sections.firstOrNull()?.id
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(uiState.menuTitle.ifBlank { "点菜" }) },
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
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (uiState.totalCount == 0) {
                                "还没点菜"
                            } else {
                                "已点 ${uiState.dishCount} 道 · ${uiState.totalCount} 份"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    Button(
                        onClick = viewModel::submit,
                        enabled = uiState.totalCount > 0
                    ) {
                        Text("生成菜单")
                    }
                }
            }
        }
    ) { padding ->
        Row(modifier = Modifier.fillMaxSize().padding(padding)) {
            // 只有一个分类时索引没有意义，直接省掉，把宽度留给菜
            if (sections.size > 1) {
                CategoryRail(
                    sections = sections,
                    counts = uiState.counts,
                    currentId = currentSectionId,
                    onSelect = { id ->
                        val target = sectionOffsets[id] ?: return@CategoryRail
                        scope.launch { listState.animateScrollToItem(target) }
                    }
                )
            }

            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Text(
                        text = "大家轮流点，点完点「生成菜单」",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                sections.forEach { section ->
                    stickyHeader(key = "header-${section.id}") {
                        CategoryHeader(section.name)
                    }
                    items(section.dishes, key = { it.id }) { dish ->
                        PickRow(
                            dish = dish,
                            count = uiState.counts[dish.id] ?: 0,
                            onIncrement = { viewModel.increment(dish.id) },
                            onDecrement = { viewModel.decrement(dish.id) }
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = uiState.note,
                        onValueChange = viewModel::onNoteChanged,
                        label = { Text("备注（可选）") },
                        placeholder = { Text("如：小王不吃辣") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

/** 左侧分类索引 */
@Composable
private fun CategoryRail(
    sections: List<CategorySection>,
    counts: Map<String, Int>,
    currentId: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .width(96.dp)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        items(sections, key = { it.id }) { section ->
            val selected = section.id == currentId
            val picked = section.pickedCount(counts)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (selected) MaterialTheme.colorScheme.surface else Color.Transparent
                    )
                    .clickable { onSelect(section.id) }
                    .padding(horizontal = 10.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = section.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.weight(1f)
                )
                if (picked > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.primary) {
                        Text(picked.toString())
                    }
                }
            }
        }
    }
}

/** 右侧每组的吸顶标题 */
@Composable
private fun CategoryHeader(name: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun PickRow(
    dish: DishEntity,
    count: Int,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(dish.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${dish.durationMin} 分钟",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            FilledTonalIconButton(
                onClick = onDecrement,
                enabled = count > 0,
                modifier = Modifier.size(36.dp)
            ) {
                Text("−", style = MaterialTheme.typography.titleLarge)
            }
            Text(
                text = count.toString(),
                modifier = Modifier.widthIn(min = 36.dp),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium
            )
            FilledTonalIconButton(
                onClick = onIncrement,
                modifier = Modifier.size(36.dp)
            ) {
                Text("+", style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}
