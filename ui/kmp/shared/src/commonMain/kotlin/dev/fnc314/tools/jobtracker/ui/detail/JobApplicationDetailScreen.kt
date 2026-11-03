package dev.fnc314.tools.jobtracker.ui.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.data.model.*
import dev.fnc314.tools.jobtracker.ui.component.StatusBadge
import dev.fnc314.tools.jobtracker.ui.detail.component.ArtifactsList
import dev.fnc314.tools.jobtracker.ui.detail.component.EventsTimeline
import dev.fnc314.tools.jobtracker.util.displayName
import org.koin.compose.koinInject

@Composable
fun JobApplicationDetailScreen(
    applicationId: String,
    viewModel: JobApplicationDetailViewModel = koinInject(),
    onNavigateBack: () -> Unit,
    onArtifactClick: (TrackerArtifact) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()

    LaunchedEffect(applicationId) {
        viewModel.loadApplication(applicationId)
    }

    var showStatusDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showFitDialog by remember { mutableStateOf(false) }

    if (showStatusDialog) {
        UpdateStatusDialog(
            currentStatus = state.application?.status,
            onConfirm = { status, date, note ->
                viewModel.updateStatus(status, date, note)
                showStatusDialog = false
            },
            onDismiss = { showStatusDialog = false },
        )
    }

    if (showEventDialog) {
        RecordEventDialog(
            onConfirm = { type, note ->
                viewModel.recordEvent(type, note)
                showEventDialog = false
            },
            onDismiss = { showEventDialog = false },
        )
    }

    if (showFitDialog) {
        UpdateFitScoreDialog(
            onConfirm = { score ->
                viewModel.updateFitScore(score)
                showFitDialog = false
            },
            onDismiss = { showFitDialog = false },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Application Details") },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.semantics { contentDescription = "Go back" },
                    ) {
                        Text("←")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showStatusDialog = true },
                        modifier = Modifier.semantics { contentDescription = "Update status" },
                    ) {
                        Text("📋")
                    }
                    IconButton(
                        onClick = { showEventDialog = true },
                        modifier = Modifier.semantics { contentDescription = "Record event" },
                    ) {
                        Text("📝")
                    }
                    IconButton(
                        onClick = { showFitDialog = true },
                        modifier = Modifier.semantics { contentDescription = "Update fit score" },
                    ) {
                        Text("⭐")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Error: ${state.error}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = { viewModel.loadApplication(applicationId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                val app = state.application
                val company = state.company

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Application Summary
                    ApplicationSummarySection(application = app)

                    // Company Profile
                    company?.let {
                        CompanyProfileSection(company = it)
                    }

                    // Artifacts
                    SectionHeader("Artifacts")
                    ArtifactsList(
                        artifacts = state.artifacts,
                        onArtifactClick = onArtifactClick,
                    )

                    // Events Timeline
                    SectionHeader("Events Timeline")
                    EventsTimeline(events = state.events)

                    // Notes
                    app?.notesRendered?.let { notes ->
                        SectionHeader("Notes")
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                        ) {
                            Text(
                                text = notes,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ApplicationSummarySection(
    application: JobApplication?,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = application?.company ?: "",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                application?.status?.let { status ->
                    StatusBadge(status = status)
                }
            }

            application?.role?.let { role ->
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                application?.location?.let { location ->
                    Text(
                        text = "📍 $location",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                application?.channel?.let { channel ->
                    Text(
                        text = "📡 ${channel.displayName()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            application?.fitEvaluation?.let { fit ->
                Text(
                    text = "Fit Rating: ${fit.score ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            application?.jobUrl?.let { url ->
                Text(
                    text = "🔗 $url",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            application?.createdAt?.let { createdAt ->
                Text(
                    text = "Created: ${createdAt.toString().take(19)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
fun CompanyProfileSection(
    company: Company,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier.padding(horizontal = 16.dp),
        onClick = { expanded = !expanded },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = company.name,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = if (expanded) "▲" else "▼",
                style = MaterialTheme.typography.labelSmall,
            )
        }
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                company.website?.let { website ->
                    Text(
                        text = "🌐 $website",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                company.industry?.let { industry ->
                    Text(
                        text = "Industry: $industry",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                if (company.aliases.isNotEmpty()) {
                    Text(
                        text = "Also known as: ${company.aliases.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.primary,
    )
}
