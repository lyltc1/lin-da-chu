package com.lyltc1.lindachu.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.lyltc1.lindachu.data.local.entity.IngredientEntity
import com.lyltc1.lindachu.data.local.entity.StepEntity
import com.lyltc1.lindachu.util.parseStringList
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
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = group,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items.forEach { ingredient -> IngredientChip(ingredient) }
                        }
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

        if (dish.review.isNotBlank()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionTitle("评价")
                    Text(dish.review, style = MaterialTheme.typography.bodyLarge)
                }
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
                    // 有小结时它当标题，正文另起一行；没有小结就让正文直接顶上
                    text = step.summary.ifBlank { step.text },
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
            }

            if (step.summary.isNotBlank() && step.text.isNotBlank()) {
                val highlightColor = MaterialTheme.colorScheme.primary
                val body = remember(step.text, highlightColor) {
                    highlightAmounts(step.text, highlightColor)
                }
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 36.dp)
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

            }
            }
            }

/**
 * 把步骤正文里「【食用油 15毫升】」这类用量标注渲染成强调样式。
 *
 * 数据层面它就是普通文本，不渲染也照样读得通；渲染后则能一眼扫到用量，
 * 免得在长句里漏看「该放多少」。
 */
private fun highlightAmounts(text: String, color: Color): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("【[^】]*】")
    var cursor = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        pushStyle(SpanStyle(color = color, fontWeight = FontWeight.Bold))
        append(match.value)
        pop()
        cursor = match.range.last + 1
    }
    if (cursor < text.length) append(text.substring(cursor))
}

/** 单个食材小框：名称 + 用量，比列表行更紧凑，扫读也更快 */
@Composable
private fun IngredientChip(ingredient: IngredientEntity) {
    val amountText = remember(ingredient) {
        listOf(ingredient.amount, ingredient.unit)
            .filter { it.isNotBlank() }
            .joinToString(" ")
    }
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
            Text(ingredient.name, style = MaterialTheme.typography.bodyMedium)
            if (amountText.isNotBlank()) {
                Text(
                    text = amountText,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 4.dp)
    )
}
