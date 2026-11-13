package dev.fnc314.tools.jobtracker.features.artifactpreviewer.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography
import dev.fnc314.tools.jobtracker.core.artifacts.model.ArtifactsRepository
import dev.fnc314.tools.jobtracker.core.artifacts.model.TrackerArtifact
import dev.fnc314.tools.jobtracker.core.data.platform.PlatformFileSaver
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import dev.fnc314.tools.jobtracker.core.navigation.LocalTopBarActionsSlot
import dev.fnc314.tools.jobtracker.features.artifactpreviewer.domain.usecase.LoadArtifactContentUseCase
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.Res
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_action_save
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_error_fetch
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_error_no_content
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_error_not_found
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_loading
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_save_error
import fnc314jobsearchtracker.features_artifact_previewer_ui.generated.resources.artifact_previewer_save_success
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

/**
 * Full-page immersive Markdown preview screen.
 *
 * Loads artifact metadata and content, and displays the Markdown content
 * with font-scaled typography mapped to [MaterialTheme.typography].
 *
 * The TopAppBar (back button, title, save icon) is rendered by the app-level
 * [AppScaffold]; this screen contributes the save action via [LocalTopBarActionsSlot].
 *
 * @param applicationId Application identifier for loading the artifact.
 * @param artifactId Artifact identifier for loading the content.
 * @param modifier Composable layout modifier.
 */
@Composable
public fun ArtifactPreviewScreen(
    applicationId: String,
    artifactId: String,
    modifier: Modifier = Modifier,
) {
    val loadContent = koinInject<LoadArtifactContentUseCase>()
    val artifactsRepo = koinInject<ArtifactsRepository>()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var artifact by remember { mutableStateOf<TrackerArtifact?>(null) }
    var content by remember { mutableStateOf<String?>(null) }
    var errorRes by remember { mutableStateOf<StringResource?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(applicationId, artifactId) {
        try {
            artifact = artifactsRepo.getArtifact(applicationId, artifactId)
            if (artifact == null) {
                errorRes = Res.string.artifact_previewer_error_not_found
            } else {
                content = loadContent.invoke(
                    LoadArtifactContentUseCase.Params(applicationId, artifactId)
                )
            }
        } catch (e: Exception) {
            errorRes = when {
                e.message?.contains("not found", ignoreCase = true) == true ->
                    Res.string.artifact_previewer_error_not_found
                e.message?.contains("No content", ignoreCase = true) == true ->
                    Res.string.artifact_previewer_error_no_content
                else -> Res.string.artifact_previewer_error_fetch
            }
        } finally {
            isLoading = false
        }
    }

    val filename = artifact?.filename ?: ""
    val saveCd = stringResource(Res.string.artifact_previewer_action_save)

    // Contribute save action to the shared TopAppBar
    LocalTopBarActionsSlot.current.render.value = @Composable {
        if (content != null) {
            val saveSuccessMsg = stringResource(Res.string.artifact_previewer_save_success)
            val saveErrorMsg = stringResource(Res.string.artifact_previewer_save_error)
            IconButton(
                onClick = {
                    scope.launch {
                        val result = PlatformFileSaver.saveText(
                            content = content!!,
                            filename = filename,
                        )
                        val message = if (result.isSuccess) saveSuccessMsg else saveErrorMsg
                        snackbarHostState.showSnackbar(message)
                    }
                },
                modifier = Modifier.semantics { contentDescription = saveCd },
            ) {
                Icon(
                    imageVector = Icons.Filled.Save,
                    contentDescription = saveCd,
                )
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
    ) {
        when {
            isLoading -> {
                val loadingCd = stringResource(Res.string.artifact_previewer_loading)
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .semantics { contentDescription = loadingCd },
                )
            }
            errorRes != null -> {
                Text(
                    text = stringResource(errorRes!!),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(MaterialTheme.spacing.medium),
                )
            }
            content != null -> {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    Markdown(
                        content = content!!,
                        colors = markdownColor(),
                        typography = markdownTypography(
                            h1 = MaterialTheme.typography.headlineSmall,
                            h2 = MaterialTheme.typography.titleLarge,
                            h3 = MaterialTheme.typography.titleMedium,
                            h4 = MaterialTheme.typography.titleSmall,
                            h5 = MaterialTheme.typography.bodyLarge,
                            h6 = MaterialTheme.typography.bodyMedium,
                            paragraph = MaterialTheme.typography.bodyLarge,
                            code = MaterialTheme.typography.bodyMedium,
                            inlineCode = MaterialTheme.typography.bodyMedium,
                            quote = MaterialTheme.typography.bodyMedium,
                            ordered = MaterialTheme.typography.bodyLarge,
                            bullet = MaterialTheme.typography.bodyLarge,
                            list = MaterialTheme.typography.bodyLarge,
                            textLink = TextLinkStyles(
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    textDecoration = TextDecoration.Underline,
                                ).toSpanStyle(),
                            ),
                            table = MaterialTheme.typography.bodyMedium,
                            alertTitle = MaterialTheme.typography.bodyLarge,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(MaterialTheme.spacing.medium),
                    )
                }
            }
        }
    }
}
