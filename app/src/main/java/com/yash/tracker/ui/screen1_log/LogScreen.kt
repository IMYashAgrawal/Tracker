package com.yash.tracker.ui.screen1_log

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yash.tracker.ui.components.HabitFormContent

@Composable
fun LogScreen(
    viewModel: LogViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is LogUiState.Success) {
            snackbarHostState.showSnackbar("Entry saved successfully")
            viewModel.resetState()
        } else if (uiState is LogUiState.Error) {
            snackbarHostState.showSnackbar((uiState as LogUiState.Error).message)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HabitFormContent(
            onSubmit = { record ->
                viewModel.submitRecord(record)
            }
        )

        if (uiState is LogUiState.Submitting) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}
