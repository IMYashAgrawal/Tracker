package com.yash.tracker.ui.screen2_history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yash.tracker.data.model.HabitRecord
import com.yash.tracker.ui.components.HabitFormContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel = viewModel()
) {
    val records by viewModel.records.collectAsState()
    var selectedRecord by remember { mutableStateOf<HabitRecord?>(null) }
    val scope = rememberCoroutineScope()

    if (selectedRecord != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Edit Session") },
                    navigationIcon = {
                        IconButton(onClick = { selectedRecord = null }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    }
                )
            }
        ) { padding ->
            HabitFormContent(
                initialRecord = selectedRecord,
                onSubmit = { updatedRecord ->
                    scope.launch {
                        viewModel.updateRecord(updatedRecord)
                        selectedRecord = null
                    }
                },
                modifier = Modifier.padding(padding)
            )
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(title = { Text("Session History") })
            }
        ) { padding ->
            if (records.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("No sessions logged yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(records) { record ->
                        HistoryItem(
                            record = record,
                            onClick = { selectedRecord = record },
                            onDelete = {
                                scope.launch {
                                    viewModel.deleteRecord(record)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryItem(
    record: HabitRecord,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Session") },
            text = { Text("Are you sure you want to delete this session? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete()
                    showDeleteConfirm = false
                }) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = record.session_date,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    Text(
                        text = record.session_time,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { showDeleteConfirm = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Location: ${record.location}")
            Text("Triggers: ${record.triggers.joinToString(", ")}")
            Text("Urge Intensity: ${record.urge_intensity}/10")
            Text("Satisfaction: ${record.experience_satisfaction}/10")
        }
    }
}
