package dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.features.jobapplicationdetails.ui.MarkdownPreviewDialog
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.Res
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_action_open
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_action_preview
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_empty
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_item_cd
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_item_subtitle
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_size_bytes
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_size_kb
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_size_mb
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_source_cloud
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_source_doc
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_source_local
import fnc314jobsearchtracker.features_job_application_details_ui.generated.resources.artifacts_source_remote
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ArtifactsList(
    artifacts: List<TrackerArtifact>,
    onArtifactClick: (TrackerArtifact) -> Unit,
    modifier: Modifier = Modifier,
    settings: AppSettings = AppSettings(),
    fileOpener: PlatformFileOpener = remember { PlatformFileOpener() },
    fetchContent: suspend (String) -> String? = { null },
): Unit {
    var previewMarkdownArtifact by remember { mutableStateOf<TrackerArtifact?>(null) }

    if (previewMarkdownArtifact != null) {
        MarkdownPreviewDialog(
            artifact = previewMarkdownArtifact!!,
            onDismiss = { previewMarkdownArtifact = null },
            fetchContent = fetchContent,
        )
    }

    if (artifacts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stringResource(Res.string.artifacts_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
    ) {
        artifacts.forEach { artifact ->
            ArtifactItem(
                artifact = artifact,
                onClick = {
                    val isPdf = artifact.filename.endsWith(".pdf", ignoreCase = true) ||
                            artifact.contentType?.contains("pdf", ignoreCase = true) == true
                    val isMd = artifact.filename.endsWith(".md", ignoreCase = true) ||
                            artifact.contentType?.contains("text", ignoreCase = true) == true

                    when {
                        isPdf -> {
                            // PDFs always go out to device default handler
                            val target = artifact.localPath ?: artifact.externalUrl ?: ""
                            if (target.isNotEmpty()) {
                                fileOpener.openPdf(target)
                            }
                            onArtifactClick(artifact)
                        }
                        isMd -> {
                            if (settings.renderMarkdownInternally) {
                                previewMarkdownArtifact = artifact
                            } else {
                                val target = artifact.localPath ?: artifact.externalUrl ?: ""
                                if (target.isNotEmpty()) {
                                    fileOpener.openFile(target)
                                }
                                onArtifactClick(artifact)
                            }
                        }
                        else -> {
                            val target = artifact.externalUrl ?: artifact.localPath ?: ""
                            if (target.isNotEmpty()) {
                                fileOpener.openUrl(target)
                            }
                            onArtifactClick(artifact)
                        }
                    }
                },
                settings = settings,
                onViewInternal = {
                    previewMarkdownArtifact = artifact
                },
                onOpenExternal = {
                    val target = artifact.localPath ?: artifact.externalUrl ?: ""
                    if (target.isNotEmpty()) {
                        fileOpener.openFile(target)
                    }
                }
            )
        }
    }
}

@Composable
internal fun ArtifactItem(
    artifact: TrackerArtifact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    settings: AppSettings = AppSettings(),
    onViewInternal: () -> Unit = {},
    onOpenExternal: () -> Unit = {},
): Unit {
    val kindLabel = artifact.kind.displayName()
    val isPdf = artifact.filename.endsWith(".pdf", ignoreCase = true) ||
            artifact.contentType?.contains("pdf", ignoreCase = true) == true
    val isMd = artifact.filename.endsWith(".md", ignoreCase = true)

    val sourceLabel = when {
        artifact.storage != null -> stringResource(Res.string.artifacts_source_cloud)
        artifact.localPath != null -> stringResource(Res.string.artifacts_source_local)
        artifact.externalUrl != null -> stringResource(Res.string.artifacts_source_remote)
        else -> stringResource(Res.string.artifacts_source_doc)
    }
    val artifactCd: String = stringResource(Res.string.artifacts_item_cd, kindLabel, artifact.filename, sourceLabel)

    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics {
                contentDescription = artifactCd
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.spacing.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Icon
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when {
                    isPdf -> MaterialTheme.colorScheme.errorContainer
                    isMd -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.secondaryContainer
                },
                modifier = Modifier.size(MaterialTheme.componentSizes.actionTargetSize),
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = when {
                            isPdf -> Icons.Default.Info
                            isMd -> Icons.Default.Edit
                            else -> Icons.Default.Info
                        },
                        contentDescription = null,
                        tint = when {
                            isPdf -> MaterialTheme.colorScheme.onErrorContainer
                            isMd -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier.size(MaterialTheme.iconSizes.default)
                    )
                }
            }

            // File Information
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.micro),
            ) {
                Text(
                    text = artifact.filename,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = stringResource(Res.string.artifacts_item_subtitle, kindLabel, sourceLabel),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                artifact.size?.let { size ->
                    Text(
                        text = formatSize(size),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            // Action Buttons / Direct links
            if (isMd && settings.renderMarkdownInternally) {
                FilledTonalButton(
                    onClick = onViewInternal,
                    contentPadding = PaddingValues(
                        horizontal = MaterialTheme.spacing.small,
                        vertical = MaterialTheme.spacing.extraSmall
                    ),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        modifier = Modifier.size(MaterialTheme.iconSizes.small),
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacing.extraSmall))
                    Text(stringResource(Res.string.artifacts_action_preview), style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = stringResource(Res.string.artifacts_action_open),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                    )
                }
            }
        }
    }
}

@Composable
private fun formatSize(size: Long): String {
    return when {
        size >= 1_000_000 -> stringResource(Res.string.artifacts_size_mb, size / 1_000_000)
        size >= 1_000 -> stringResource(Res.string.artifacts_size_kb, size / 1_000)
        else -> stringResource(Res.string.artifacts_size_bytes, size)
    }
}
