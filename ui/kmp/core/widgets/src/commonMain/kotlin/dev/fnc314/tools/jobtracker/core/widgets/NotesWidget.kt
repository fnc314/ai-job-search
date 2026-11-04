package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import kotlinx.coroutines.delay
import kotlin.random.Random

private var nextId = 0L
private fun generateNoteId(): String = "note_${++nextId}_${Random.nextLong()}"

private data class NoteItem(
    val id: String = generateNoteId(),
    val text: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotesWidget(
    onNotesChanged: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    initialNotes: List<String> = emptyList(),
    label: String = "Notes",
) {
    val items = remember {
        mutableStateListOf<NoteItem>().apply {
            initialNotes.forEach { add(NoteItem(text = it)) }
            add(NoteItem(text = ""))
        }
    }

    var focusNewItemId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(initialNotes) {
        val currentFilled = items.map { it.text.trim() }.filter { it.isNotEmpty() }
        if (currentFilled != initialNotes) {
            items.clear()
            initialNotes.forEach { items.add(NoteItem(text = it)) }
            items.add(NoteItem(text = ""))
        }
    }

    fun notifyNotesChanged() {
        val filled = items.map { it.text.trim() }.filter { it.isNotEmpty() }
        onNotesChanged(filled)
    }

    fun ensureTrailingEmpty() {
        if (items.isEmpty() || items.last().text.isNotEmpty()) {
            items.add(NoteItem(text = ""))
        }
    }

    fun commitDrafting(index: Int) {
        if (items[index].text.isNotBlank()) {
            if (index == items.lastIndex) {
                val newItem = NoteItem(text = "")
                items.add(newItem)
                focusNewItemId = newItem.id
            }
            notifyNotesChanged()
        }
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        if (label.isNotEmpty()) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        items.forEachIndexed { index, item ->
            key(item.id) {
                val isDrafting = index == items.lastIndex
                val requestFocusOnMount = item.id == focusNewItemId

                NoteRow(
                    item = item,
                    isDrafting = isDrafting,
                    requestFocus = requestFocusOnMount,
                    onFocusRequested = {
                        if (focusNewItemId == item.id) {
                            focusNewItemId = null
                        }
                    },
                    canReorder = index < items.lastIndex,
                    onTextChange = { newText ->
                        items[index] = items[index].copy(text = newText)
                        notifyNotesChanged()
                    },
                    onCommit = {
                        commitDrafting(index)
                    },
                    onDelete = {
                        items.removeAt(index)
                        ensureTrailingEmpty()
                        notifyNotesChanged()
                    },
                    onMoveUp = {
                        if (index > 0) {
                            val temp = items[index]
                            items[index] = items[index - 1]
                            items[index - 1] = temp
                            notifyNotesChanged()
                        }
                    },
                    onMoveDown = {
                        if (index < items.lastIndex - 1) {
                            val temp = items[index]
                            items[index] = items[index + 1]
                            items[index + 1] = temp
                            notifyNotesChanged()
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteRow(
    item: NoteItem,
    isDrafting: Boolean,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onTextChange: (String) -> Unit,
    onCommit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            delay(50)
            try {
                focusRequester.requestFocus()
            } catch (_: Exception) {}
            onFocusRequested()
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue != SwipeToDismissBoxValue.Settled) {
                onDelete()
                true
            } else {
                false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color = when (dismissState.targetValue) {
                SwipeToDismissBoxValue.Settled -> Color.Transparent
                else -> MaterialTheme.colorScheme.errorContainer
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(color, shape = MaterialTheme.shapes.small)
                    .padding(horizontal = MaterialTheme.spacing.medium),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Note",
                    tint = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        },
        content = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = item.text,
                        onValueChange = onTextChange,
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(focusRequester)
                            .onFocusChanged { focusState ->
                                isFocused = focusState.isFocused
                            }
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Enter && !keyEvent.isShiftPressed) {
                                    onCommit()
                                    true
                                } else {
                                    false
                                }
                            },
                        label = {
                            Text(if (isDrafting && item.text.isEmpty()) "Add a note..." else "Note")
                        },
                        singleLine = !isFocused,
                        maxLines = if (isFocused) Int.MAX_VALUE else 1,
                        trailingIcon = {
                            if (isDrafting || isFocused) {
                                IconButton(onClick = onCommit) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Commit Note"
                                    )
                                }
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { onCommit() }
                        )
                    )

                    if (canMoveUp || canMoveDown) {
                        Column(
                            modifier = Modifier.padding(start = 4.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (canMoveUp) {
                                IconButton(
                                    onClick = onMoveUp,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        text = "▲",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            if (canMoveDown) {
                                IconButton(
                                    onClick = onMoveDown,
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Text(
                                        text = "▼",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    )
}
