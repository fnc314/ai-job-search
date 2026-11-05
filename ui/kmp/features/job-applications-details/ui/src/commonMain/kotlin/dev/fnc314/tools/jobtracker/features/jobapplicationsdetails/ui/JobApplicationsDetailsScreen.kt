package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.sharp.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.TooltipIconButton
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.data.model.*
import dev.fnc314.tools.jobtracker.core.widgets.FitBadge
import dev.fnc314.tools.jobtracker.core.widgets.StatusBadge
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.component.ArtifactsList
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.component.EventsTimeline
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.model.displayName
import dev.fnc314.tools.jobtracker.core.data.util.formatSemanticTimestamp
import org.koin.compose.koinInject

@Composable
fun JobApplicationsDetailsScreen(
    applicationId: String,
    viewModel: JobApplicationDetailViewModel = koinInject(),
    onNavigateBack: () -> Unit,
    onArtifactClick: (TrackerArtifact) -> Unit = {},
    settings: AppSettings = AppSettings(),
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
            currentScore = state.application?.fitScore,
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
                    TooltipIconButton(
                        tooltip = "Go back",
                        icon = Icons.AutoMirrored.Sharp.ArrowBack,
                        onClick = onNavigateBack,
                    )
                },
                actions = {
                    TooltipIconButton(
                        tooltip = "Update status",
                        icon = Icons.Sharp.Notifications,
                        onClick = { showStatusDialog = true },
                    )
                    TooltipIconButton(
                        tooltip = "Record event",
                        icon = Icons.Sharp.Edit,
                        onClick = { showEventDialog = true },
                    )
                    TooltipIconButton(
                        tooltip = "Update fit score",
                        icon = Icons.Default.Build,
                        onClick = { showFitDialog = true },
                    )
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
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                        Button(onClick = { viewModel.dismissError() }) {
                            Text("Dismiss")
                        }
                        Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraSmall))
                        TextButton(onClick = { viewModel.loadApplication(applicationId) }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                val app = state.application
                val company = state.company
                val windowSizeClass = LocalWindowSizeClass.current
                val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.TopCenter
                ) {
                    if (isWide) {
                        // 2-column layout for Medium and Expanded viewports
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = MaterialTheme.componentSizes.detailMaxWidth)
                                .padding(MaterialTheme.spacing.medium)
                                .verticalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                            ) {
                                ApplicationSummarySection(
                                    application = app,
                                    onAdjustFitClick = { showFitDialog = true },
                                )
                                company?.let { CompanyProfileSection(company = it) }
                                app?.notesRendered?.let { notes ->
                                    SectionHeader("Notes")
                                    OutlinedCard(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = MaterialTheme.shapes.medium,
                                    ) {
                                        Text(
                                            text = notes,
                                            modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium)
                            ) {
                                SectionHeader("Artifacts")
                                ArtifactsList(
                                    artifacts = state.artifacts,
                                    onArtifactClick = onArtifactClick,
                                    settings = settings,
                                )

                                SectionHeader("Events Timeline")
                                EventsTimeline(
                                    events = state.events,
                                    application = state.application,
                                )
                            }
                        }
                    } else {
                        // 1-column layout for Compact viewports
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                        ) {
                            ApplicationSummarySection(
                                application = app,
                                onAdjustFitClick = { showFitDialog = true },
                            )
                            company?.let { CompanyProfileSection(company = it) }

                            SectionHeader("Artifacts")
                            ArtifactsList(
                                artifacts = state.artifacts,
                                onArtifactClick = onArtifactClick,
                                settings = settings,
                            )

                            SectionHeader("Events Timeline")
                            EventsTimeline(
                                events = state.events,
                                application = state.application,
                            )

                            app?.notesRendered?.let { notes ->
                                SectionHeader("Notes")
                                OutlinedCard(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = MaterialTheme.spacing.medium),
                                    shape = MaterialTheme.shapes.medium,
                                ) {
                                    Text(
                                        text = notes,
                                        modifier = Modifier.padding(MaterialTheme.spacing.medium),
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
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
    onAdjustFitClick: (() -> Unit)? = null,
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
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
                    StatusBadge(status = status, textAlign = TextAlign.End)
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
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
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

            val fitScore = application?.fitScore
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = if (onAdjustFitClick != null) {
                    Modifier.clickable(
                        onClick = onAdjustFitClick,
                        onClickLabel = "Adjust fit score",
                    )
                } else {
                    Modifier
                },
            ) {
                Text(
                    text = "Fit Rating: ${fitScore ?: "N/A"}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (fitScore != null) {
                    FitBadge(score = fitScore)
                }
                if (onAdjustFitClick != null) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Adjust fit score",
                        modifier = Modifier.size(MaterialTheme.iconSizes.small),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            application?.fitEvaluation?.gaps?.let { gaps ->
                if (gaps.isNotEmpty()) {
                    Text(
                        text = "Gaps: ${gaps.joinToString(", ")}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            application?.jobUrl?.let { url ->
                Text(
                    text = "🔗 Job URL: $url",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            application?.jobApplicationUrl?.let { url ->
                Text(
                    text = "📋 Application URL: $url",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            application?.let { app ->
                val timeLabel = formatSemanticTimestamp(app.updatedAt, app.createdAt)
                if (timeLabel != null) {
                    Text(
                        text = timeLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun CompanyProfileSection(
    company: Company,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
        ) {
            Text(
                text = "Company Profile: ${company.name}",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
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
                    text = "Aliases: ${company.aliases.joinToString(", ")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
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
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small
            ),
        color = MaterialTheme.colorScheme.primary,
    )
}
