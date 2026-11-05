package dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.iconSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileOpener
import dev.fnc314.tools.jobtracker.features.jobapplicationsdetails.ui.MarkdownPreviewDialog
import dev.fnc314.tools.jobtracker.core.data.settings.AppSettings
import dev.fnc314.tools.jobtracker.core.data.model.displayName

@Composable
fun ArtifactsList(
    artifacts: List<TrackerArtifact>,
    onArtifactClick: (TrackerArtifact) -> Unit,
    modifier: Modifier = Modifier,
    settings: AppSettings = AppSettings(),
    fileOpener: PlatformFileOpener = remember { PlatformFileOpener() },
) {
    var previewMarkdownArtifact by remember { mutableStateOf<TrackerArtifact?>(null) }

    if (previewMarkdownArtifact != null) {
        MarkdownPreviewDialog(
            artifact = previewMarkdownArtifact!!,
            onDismiss = { previewMarkdownArtifact = null },
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
                text = "No artifacts attached",
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
fun ArtifactItem(
    artifact: TrackerArtifact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    settings: AppSettings = AppSettings(),
    onViewInternal: () -> Unit = {},
    onOpenExternal: () -> Unit = {},
) {
    val kindLabel = artifact.kind.displayName()
    val isPdf = artifact.filename.endsWith(".pdf", ignoreCase = true) ||
            artifact.contentType?.contains("pdf", ignoreCase = true) == true
    val isMd = artifact.filename.endsWith(".md", ignoreCase = true)

    val sourceLabel = when {
        artifact.storage != null -> "Cloud Storage"
        artifact.localPath != null -> "Local File"
        artifact.externalUrl != null -> "Remote URL"
        else -> "Document"
    }

    OutlinedCard(
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$kindLabel: ${artifact.filename} ($sourceLabel)" },
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
                    text = "$kindLabel · $sourceLabel",
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
                    Text("Preview", style = MaterialTheme.typography.labelSmall)
                }
            } else {
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Open file",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(MaterialTheme.iconSizes.standard),
                    )
                }
            }
        }
    }
}

fun formatSize(size: Long): String {
    return when {
        size >= 1_000_000 -> "${size / 1_000_000} MB"
        size >= 1_000 -> "${size / 1_000} KB"
        else -> "$size B"
    }
}
