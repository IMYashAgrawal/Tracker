package com.yash.tracker.ui.screen3_analysis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.yash.tracker.analytics.model.DateRangeType
import com.yash.tracker.analytics.model.Insight
import com.yash.tracker.analytics.model.InsightConfidence
import com.yash.tracker.analytics.model.OverviewMetrics

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalysisScreen(
    viewModel: AnalysisViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Analysis") },
                actions = {
                    IconButton(onClick = { viewModel.analyze() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        when {
            uiState.isLoading -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(uiState.error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { viewModel.analyze() }) { Text("Retry") }
                    }
                }
            }
            uiState.overview == null -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Text("No data to analyze yet. Log some sessions first.")
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Date Range Selector
                    DateRangeSelector(
                        selected = uiState.selectedRange,
                        onSelected = { viewModel.setDateRange(it) }
                    )

                    // Overview Cards
                    uiState.overview?.let { overview ->
                        OverviewSection(overview)
                    }

                    // Insights
                    if (uiState.insights.isNotEmpty()) {
                        InsightsSection(uiState.insights)
                    }

                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DateRangeSelector(
    selected: DateRangeType,
    onSelected: (DateRangeType) -> Unit
) {
    val options = listOf(
        DateRangeType.LAST_7_DAYS to "7D",
        DateRangeType.LAST_30_DAYS to "30D",
        DateRangeType.LAST_90_DAYS to "90D",
        DateRangeType.ALL_TIME to "All"
    )
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (type, label) ->
            FilterChip(
                selected = selected == type,
                onClick = { onSelected(type) },
                label = { Text(label) }
            )
        }
    }
}

@Composable
fun OverviewSection(overview: OverviewMetrics) {
    Text("Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    // Row 1: Counts
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard("Total", overview.totalEntries.value.toString(), Modifier.weight(1f))
        StatCard("This Week", overview.entriesThisWeek.value.toString(), Modifier.weight(1f))
        StatCard("This Month", overview.entriesThisMonth.value.toString(), Modifier.weight(1f))
    }

    // Row 2: Averages
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            "Avg Urge",
            String.format("%.1f", overview.avgUrgeIntensity.value) + "/10",
            Modifier.weight(1f)
        )
        StatCard(
            "Avg Satisfaction",
            String.format("%.1f", overview.avgSatisfaction.value) + "/10",
            Modifier.weight(1f)
        )
    }

    // Row 3: Patterns
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        StatCard(
            "Spontaneous",
            String.format("%.0f%%", overview.spontaneousPercent.value),
            Modifier.weight(1f)
        )
        StatCard(
            "Full Control",
            String.format("%.0f%%", overview.fullControlPercent.value),
            Modifier.weight(1f)
        )
        StatCard(
            "Media Used",
            String.format("%.0f%%", overview.mediaConsumptionPercent.value),
            Modifier.weight(1f)
        )
    }

    // Row 4: Gaps
    if (overview.avgGapMinutes.value > 0) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val avgGapHours = overview.avgGapMinutes.value / 60.0
            StatCard(
                "Avg Gap",
                if (avgGapHours >= 24) String.format("%.1f days", avgGapHours / 24)
                else String.format("%.1f hrs", avgGapHours),
                Modifier.weight(1f)
            )
            StatCard(
                "Activity Affected",
                String.format("%.0f%%", overview.activityAffectedPercent.value),
                Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun InsightsSection(insights: List<Insight>) {
    Text("Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

    insights.forEach { insight ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (insight.confidence) {
                    InsightConfidence.HIGH -> MaterialTheme.colorScheme.primaryContainer
                    InsightConfidence.MODERATE -> MaterialTheme.colorScheme.secondaryContainer
                    InsightConfidence.LOW -> MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        insight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${insight.confidence.name} (n=${insight.n})",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(insight.description, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
