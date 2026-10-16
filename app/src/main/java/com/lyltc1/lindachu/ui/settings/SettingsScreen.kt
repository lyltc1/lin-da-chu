package com.lyltc1.lindachu.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) viewModel.exportBackup(uri)
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) viewModel.importBackup(uri)
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("设置与同步") },
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
            if (uiState.message != null) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    ) {
                        Text(
                            text = uiState.message!!,
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            item {
                RepositorySection(
                    owner = uiState.owner,
                    repo = uiState.repo,
                    token = uiState.token,
                    onOwnerChanged = viewModel::onOwnerChanged,
                    onRepoChanged = viewModel::onRepoChanged,
                    onTokenChanged = viewModel::onTokenChanged,
                    onSave = viewModel::saveCredentials
                )
            }

            item {
                SyncSection(
                    lastSyncAt = uiState.lastSyncAt,
                    isSyncing = uiState.isSyncing,
                    enabled = uiState.hasCredentials,
                    onSync = viewModel::sync
                )
            }

            item {
                BackupSection(
                    isBusy = uiState.isBusy,
                    onExport = { exportLauncher.launch("lindachu-backup.zip") },
                    onImport = { importLauncher.launch(arrayOf("application/zip")) }
                )
            }


        }
    }
}

@Composable
private fun RepositorySection(
    owner: String,
    repo: String,
    token: String,
    onOwnerChanged: (String) -> Unit,
    onRepoChanged: (String) -> Unit,
    onTokenChanged: (String) -> Unit,
    onSave: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("GitHub 数据仓库", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = owner,
                onValueChange = onOwnerChanged,
                label = { Text("用户名 / 组织") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = repo,
                onValueChange = onRepoChanged,
                label = { Text("仓库名") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = token,
                onValueChange = onTokenChanged,
                label = { Text("访问令牌") },
                placeholder = { Text("ghp_xxx 或 github_pat_xxx") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = "需要具有 repo 权限的 Personal Access Token，仅保存在本机，不会写入备份文件。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSave, modifier = Modifier.fillMaxWidth()) {
                Text("保存凭据")
            }
        }
    }
}

@Composable
private fun SyncSection(
    lastSyncAt: Long,
    isSyncing: Boolean,
    enabled: Boolean,
    onSync: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("同步", style = MaterialTheme.typography.titleLarge)
            Text(
                text = if (lastSyncAt > 0L) "上次同步：${formatDateTime(lastSyncAt)}" else "尚未同步过",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onSync, enabled = enabled && !isSyncing, modifier = Modifier.fillMaxWidth()) {
                Text(if (isSyncing) "同步中…" else "立即同步")
            }
        }
    }
}

@Composable
private fun BackupSection(
    isBusy: Boolean,
    onExport: () -> Unit,
    onImport: () -> Unit
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("备份", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "导出为 zip（菜谱 JSON + 图片），可用于换机迁移或留存。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onExport, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Text("导出备份")
            }
            OutlinedButton(onClick = onImport, enabled = !isBusy, modifier = Modifier.fillMaxWidth()) {
                Text("导入备份")
            }
        }
    }
}

private fun formatDateTime(millis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
