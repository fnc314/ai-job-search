package dev.fnc314.tools.jobtracker.ui.detail.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.data.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.util.displayName

@Composable
fun ArtifactsList(
    artifacts: List<TrackerArtifact>,
    onArtifactClick: (TrackerArtifact) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (artifacts.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
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
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        artifacts.forEach { artifact ->
            ArtifactItem(
                artifact = artifact,
                onClick = { onArtifactClick(artifact) },
            )
        }
    }
}

@Composable
fun ArtifactItem(
    artifact: TrackerArtifact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val kindLabel = artifact.kind.displayName()
    val hasStorage = artifact.storage != null
    val hasLocalPath = artifact.localPath != null
    val hasExternalUrl = artifact.externalUrl != null
    val sourceLabel = when {
        hasStorage -> "GCS"
        hasLocalPath -> "Local"
        hasExternalUrl -> "URL"
        else -> "Unknown"
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                onClickLabel = "Open ${artifact.filename}",
            )
            .semantics { contentDescription = "$kindLabel: ${artifact.filename} ($sourceLabel)" },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // File icon indicator
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = getFileIcon(artifact),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
            }

            // File info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = artifact.filename,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
                Text(
                    text = "$kindLabel · $sourceLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                artifact.size?.let { size ->
                    Text(
                        text = formatSize(size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // Open indicator
            Text(
                text = "→",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { contentDescription = "Open file" },
            )
        }
    }
}

fun getFileIcon(artifact: TrackerArtifact): String {
    return when {
        artifact.contentType?.contains("pdf", ignoreCase = true) == true -> "📄"
        artifact.contentType?.contains("image", ignoreCase = true) == true -> "🖼"
        artifact.contentType?.contains("text", ignoreCase = true) == true -> "📝"
        artifact.filename.endsWith(".md", ignoreCase = true) -> "📝"
        artifact.filename.endsWith(".pdf", ignoreCase = true) -> "📄"
        artifact.filename.endsWith(".png", ignoreCase = true) || artifact.filename.endsWith(".jpg", ignoreCase = true) -> "🖼"
        else -> "📎"
    }
}

fun formatSize(size: Long): String {
    return when {
        size >= 1_000_000 -> "${size / 1_000_000} MB"
        size >= 1_000 -> "${size / 1_000} KB"
        else -> "$size B"
    }
}
