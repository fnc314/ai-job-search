package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.sharp.Edit
import androidx.compose.material.icons.sharp.Notifications
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import coil3.compose.AsyncImage
import dev.fnc314.tools.jobtracker.core.data.model.Company
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.designsystem.LocalWindowSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.WindowWidthSizeClass
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.widgets.ErrorBannerWidget
import dev.fnc314.tools.jobtracker.core.widgets.FitBadge
import dev.fnc314.tools.jobtracker.core.widgets.LoadingWidget
import dev.fnc314.tools.jobtracker.core.widgets.StatusBadge
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.component.ArtifactsList
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.component.EventsTimeline
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_action_adjust_fit_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_action_record_event
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_action_update_fit
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_action_update_status
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_company_aliases
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_company_industry
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_company_logo_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_company_profile_title
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_company_website
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_error_prefix
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_fab_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_fit_rating_label
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_label_gaps
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_last_updated
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_prefix_app_url
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_prefix_channel
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_prefix_compensation
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_prefix_job_url
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_prefix_location
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_section_artifacts
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_section_notes
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.detail_section_timeline
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Primary detail screen displaying comprehensive information about a single job application.
 *
 * Renders company profile, role metadata, compensation ranges, historical timeline events,
 * generated artifacts, and interactive actions (status transition, fit-score calibration, event creation).
 *
 * @param applicationId Unique identifier of the application record to display.
 * @param viewModel [JobApplicationDetailViewModel] managing state and business operations.
 * @param onNavigateBack Callback invoked when the user selects back navigation.
 * @param onArtifactClick Callback invoked when an artifact item is clicked.
 * @param settings Application configuration controlling formatting and density.
 * @param modifier Composable layout modifier.
 */
@Composable
public fun JobApplicationDetailsScreen(
    applicationId: String,
    viewModel: JobApplicationDetailViewModel = koinInject(),
    onNavigateBack: () -> Unit,
    onArtifactClick: (TrackerArtifact) -> Unit = {},
    settings: AppSettings = AppSettings(),
    modifier: Modifier = Modifier,
): Unit {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(applicationId) {
        viewModel.onIntent(JobApplicationDetailUiIntent.Load(applicationId))
    }

    when (val state = uiState) {
        is JobApplicationDetailUiState.NavigateBack -> {
            LaunchedEffect(state) {
                onNavigateBack()
            }
        }
        is JobApplicationDetailUiState.Loading -> {
            LoadingWidget(modifier = modifier)
        }
        is JobApplicationDetailUiState.Error -> {
            ErrorBannerWidget(
                message = stringResource(Res.string.detail_error_prefix, state.message),
                onRetry = {
                    viewModel.onIntent(JobApplicationDetailUiIntent.Load(applicationId))
                },
                modifier = modifier.padding(MaterialTheme.spacing.medium),
            )
        }
        is JobApplicationDetailUiState.Content -> {
            JobApplicationDetailContent(
                state = state,
                onIntent = viewModel::onIntent,
                onArtifactClick = onArtifactClick,
                settings = settings,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun JobApplicationDetailContent(
    state: JobApplicationDetailUiState.Content,
    onIntent: (JobApplicationDetailUiIntent) -> Unit,
    onArtifactClick: (TrackerArtifact) -> Unit,
    settings: AppSettings,
    modifier: Modifier = Modifier,
): Unit {
    val model = state.model
    var showStatusDialog by remember { mutableStateOf(false) }
    var showEventDialog by remember { mutableStateOf(false) }
    var showFitDialog by remember { mutableStateOf(false) }
    var fabMenuExpanded by remember { mutableStateOf(false) }

    if (showStatusDialog) {
        UpdateStatusDialog(
            currentStatus = model.status,
            onConfirm = { status, date, note ->
                onIntent(JobApplicationDetailUiIntent.UpdateStatus(newStatus = status, date = date, note = note))
                showStatusDialog = false
            },
            onDismiss = { showStatusDialog = false },
        )
    }

    if (showEventDialog) {
        RecordEventDialog(
            onConfirm = { type, note ->
                onIntent(JobApplicationDetailUiIntent.RecordEvent(type = type, notes = note.orEmpty()))
                showEventDialog = false
            },
            onDismiss = { showEventDialog = false },
        )
    }

    if (showFitDialog) {
        UpdateFitScoreDialog(
            currentScore = model.fitScore,
            onConfirm = { score ->
                onIntent(JobApplicationDetailUiIntent.UpdateFitScore(newScore = score))
                showFitDialog = false
            },
            onDismiss = { showFitDialog = false },
        )
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val isWide = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            if (state.isUpdating) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.TopCenter,
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
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                        ) {
                            ApplicationSummarySection(
                                model = model,
                                onAdjustFitClick = { showFitDialog = true },
                            )
                            model.company?.let { CompanyProfileSection(company = it) }
                            model.notesRendered?.let { notes ->
                                SectionHeader(stringResource(Res.string.detail_section_notes))
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
                            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                        ) {
                            SectionHeader(stringResource(Res.string.detail_section_artifacts))
                            ArtifactsList(
                                artifacts = model.artifacts,
                                onArtifactClick = { artifact ->
                                    onIntent(JobApplicationDetailUiIntent.OpenArtifact(artifact.id))
                                    onArtifactClick(artifact)
                                },
                                settings = settings,
                            )

                            ApplicationTimelineSection(model = model)
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
                            model = model,
                            onAdjustFitClick = { showFitDialog = true },
                        )
                        model.company?.let { CompanyProfileSection(company = it) }

                        SectionHeader(stringResource(Res.string.detail_section_artifacts))
                        ArtifactsList(
                            artifacts = model.artifacts,
                            onArtifactClick = { artifact ->
                                onIntent(JobApplicationDetailUiIntent.OpenArtifact(artifact.id))
                                onArtifactClick(artifact)
                            },
                            settings = settings,
                        )

                        ApplicationTimelineSection(model = model)

                        model.notesRendered?.let { notes ->
                            SectionHeader(stringResource(Res.string.detail_section_notes))
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

        // Details Menu FAB
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(MaterialTheme.spacing.medium),
        ) {
            FloatingActionButton(
                onClick = { fabMenuExpanded = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(Res.string.detail_fab_cd),
                )
            }
            DropdownMenu(
                expanded = fabMenuExpanded,
                onDismissRequest = { fabMenuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.detail_action_update_status)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Sharp.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                        )
                    },
                    onClick = {
                        fabMenuExpanded = false
                        showStatusDialog = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.detail_action_record_event)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Sharp.Edit,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                        )
                    },
                    onClick = {
                        fabMenuExpanded = false
                        showEventDialog = true
                    },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(Res.string.detail_action_update_fit)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Build,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                        )
                    },
                    onClick = {
                        fabMenuExpanded = false
                        showFitDialog = true
                    },
                )
            }
        }
    }
}

@Composable
internal fun ApplicationSummarySection(
    model: JobApplicationDetailUiModel,
    modifier: Modifier = Modifier,
    onAdjustFitClick: (() -> Unit)? = null,
): Unit {
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
                    text = model.companyName,
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(status = model.status, textAlign = TextAlign.End)
            }

            model.role?.let { role ->
                Text(
                    text = role,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                model.formattedLocation?.let { location ->
                    Text(
                        text = stringResource(Res.string.detail_prefix_location, location),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    text = stringResource(Res.string.detail_prefix_channel, model.channelDisplayText),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            model.formattedCompensation?.let { compensation ->
                Text(
                    text = stringResource(Res.string.detail_prefix_compensation, compensation),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            val fitScore = model.fitScore
            val adjustFitLabel: String = stringResource(Res.string.detail_action_adjust_fit_cd)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
                modifier = if (onAdjustFitClick != null) {
                    Modifier.clickable(
                        onClick = onAdjustFitClick,
                        onClickLabel = adjustFitLabel,
                    )
                } else {
                    Modifier
                },
            ) {
                Text(
                    text = stringResource(Res.string.detail_fit_rating_label, model.fitScoreDisplayText),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (fitScore != null) {
                    FitBadge(score = fitScore)
                }
                if (onAdjustFitClick != null) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = adjustFitLabel,
                        modifier = Modifier.size(MaterialTheme.iconSizes.small),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (model.fitGaps.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.detail_label_gaps, model.fitGaps.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            model.jobUrl?.let { url ->
                Text(
                    text = stringResource(Res.string.detail_prefix_job_url, url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            model.jobApplicationUrl?.let { url ->
                Text(
                    text = stringResource(Res.string.detail_prefix_app_url, url),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Text(
                text = stringResource(Res.string.detail_last_updated, model.lastUpdatedDisplay()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
internal fun ApplicationTimelineSection(
    model: JobApplicationDetailUiModel,
    modifier: Modifier = Modifier,
): Unit {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.none),
    ) {
        SectionHeader(stringResource(Res.string.detail_section_timeline))
        EventsTimeline(events = model.timelineEvents)
    }
}

@Composable
internal fun CompanyProfileSection(
    company: Company,
    modifier: Modifier = Modifier,
): Unit {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
            ) {
                company.logoUrl?.let { url ->
                    AsyncImage(
                        model = url,
                        contentDescription = stringResource(Res.string.detail_company_logo_cd, company.name),
                        modifier = Modifier
                            .size(MaterialTheme.iconSizes.extraLarge)
                            .clip(MaterialTheme.shapes.small),
                    )
                }
                Text(
                    text = stringResource(Res.string.detail_company_profile_title, company.name),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            company.website?.let { website ->
                Text(
                    text = stringResource(Res.string.detail_company_website, website),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            company.industry?.let { industry ->
                Text(
                    text = stringResource(Res.string.detail_company_industry, industry),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (company.aliases.isNotEmpty()) {
                Text(
                    text = stringResource(Res.string.detail_company_aliases, company.aliases.joinToString(", ")),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
): Unit {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = MaterialTheme.spacing.medium,
                vertical = MaterialTheme.spacing.small,
            ),
        color = MaterialTheme.colorScheme.primary,
    )
}
