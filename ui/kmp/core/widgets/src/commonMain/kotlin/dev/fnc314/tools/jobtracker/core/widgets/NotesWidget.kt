package dev.fnc314.tools.jobtracker.core.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentList
import androidx.compose.ui.input.key.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import dev.fnc314.tools.jobtracker.core.designsystem.componentSizes
import dev.fnc314.tools.jobtracker.core.designsystem.spacing
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.random.Random

private var nextId = 0L
private fun generateNoteId(): String = "note_${++nextId}_${Random.nextLong()}"

private data class NoteItem(
    val id: String = generateNoteId(),
    val text: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun NotesWidget(
    onNotesChanged: (ImmutableList<String>) -> Unit,
    modifier: Modifier = Modifier,
    initialNotes: ImmutableList<String> = persistentListOf(),
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
        val filled = items.map { it.text.trim() }.filter { it.isNotEmpty() }.toImmutableList()
        onNotesChanged(filled)
    }

    fun ensureTrailingEmpty(): NoteItem {
        return if (items.isEmpty() || items.last().text.isNotEmpty()) {
            val emptyItem = NoteItem(text = "")
            items.add(emptyItem)
            emptyItem
        } else {
            items.last()
        }
    }

    fun addNewNoteAndFocus() {
        val target = ensureTrailingEmpty()
        focusNewItemId = target.id
    }

    fun commitDrafting(index: Int) {
        if (items[index].text.isNotBlank()) {
            if (index == items.lastIndex) {
                val newItem = NoteItem(text = "")
                items.add(newItem)
                focusNewItemId = newItem.id
            } else {
                addNewNoteAndFocus()
            }
            notifyNotesChanged()
        }
    }

    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var draggingItemYOffset by remember { mutableStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small)
    ) {
        if (label.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                IconButton(
                    onClick = { addNewNoteAndFocus() },
                    modifier = Modifier.semantics { contentDescription = "Add new note" }
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add new note",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        items.forEachIndexed { index, item ->
            key(item.id) {
                val isDrafting = index == items.lastIndex
                val requestFocusOnMount = item.id == focusNewItemId
                val isDragging = draggingIndex == index

                val elevation by animateDpAsState(
                    targetValue = if (isDragging) MaterialTheme.componentSizes.elevationDraggingCard else MaterialTheme.spacing.none,
                    label = "dragElevation"
                )
                val scale by animateFloatAsState(
                    targetValue = if (isDragging) 1.03f else 1f,
                    label = "dragScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            itemHeights[index] = coordinates.size.height
                        }
                        .zIndex(if (isDragging) 10f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragging) draggingItemYOffset else 0f
                            scaleX = scale
                            scaleY = scale
                            shadowElevation = elevation.toPx()
                        }
                ) {
                    NoteRow(
                        item = item,
                        isDrafting = isDrafting,
                        isDragging = isDragging,
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
                        onDragStart = {
                            draggingIndex = index
                            draggingItemYOffset = 0f
                        },
                        onDrag = { dragDelta ->
                            draggingItemYOffset += dragDelta
                            val currentDragging = draggingIndex ?: return@NoteRow
                            val currentHeight = (itemHeights[currentDragging] ?: 0).toFloat()
                            val stepThreshold = if (currentHeight > 0) currentHeight * 0.7f else 50f

                            if (draggingItemYOffset > stepThreshold && currentDragging < items.lastIndex - 1) {
                                // Swap down
                                val target = currentDragging + 1
                                val temp = items[currentDragging]
                                items[currentDragging] = items[target]
                                items[target] = temp
                                draggingIndex = target
                                draggingItemYOffset -= (itemHeights[target] ?: currentHeight.roundToInt()).toFloat()
                                notifyNotesChanged()
                            } else if (draggingItemYOffset < -stepThreshold && currentDragging > 0) {
                                // Swap up
                                val target = currentDragging - 1
                                val temp = items[currentDragging]
                                items[currentDragging] = items[target]
                                items[target] = temp
                                draggingIndex = target
                                draggingItemYOffset += (itemHeights[target] ?: currentHeight.roundToInt()).toFloat()
                                notifyNotesChanged()
                            }
                        },
                        onDragEnd = {
                            draggingIndex = null
                            draggingItemYOffset = 0f
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteRow(
    item: NoteItem,
    isDrafting: Boolean,
    isDragging: Boolean,
    requestFocus: Boolean,
    onFocusRequested: () -> Unit,
    canReorder: Boolean,
    onTextChange: (String) -> Unit,
    onCommit: () -> Unit,
    onDelete: () -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragEnd: () -> Unit,
) {
    var isFocused by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(true) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(requestFocus) {
        if (requestFocus) {
            var attempts = 0
            while (attempts < 10) {
                try {
                    focusRequester.requestFocus()
                    break
                } catch (_: Exception) {
                    delay(30)
                    attempts++
                }
            }
            onFocusRequested()
        }
    }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { dismissValue ->
            if (dismissValue != SwipeToDismissBoxValue.Settled) {
                isVisible = false
                true
            } else {
                false
            }
        }
    )

    AnimatedVisibility(
        visible = isVisible,
        exit = shrinkVertically(
            animationSpec = tween(durationMillis = 250),
            shrinkTowards = Alignment.Top
        ) + fadeOut(animationSpec = tween(durationMillis = 200))
    ) {
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = !isDrafting && !isDragging,
            enableDismissFromEndToStart = !isDrafting && !isDragging,
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
                    tonalElevation = if (isDragging) MaterialTheme.componentSizes.elevationDraggingTonal else MaterialTheme.spacing.none,
                    shadowElevation = if (isDragging) MaterialTheme.componentSizes.elevationDraggingShadow else MaterialTheme.spacing.none,
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

                        if (canReorder && !isDrafting) {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = MaterialTheme.spacing.subSmall)
                                    .size(MaterialTheme.componentSizes.iconButtonSize)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = { onDragStart() },
                                            onDragEnd = { onDragEnd() },
                                            onDragCancel = { onDragEnd() },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                onDrag(dragAmount.y)
                                            }
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "⠿",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        )
    }

    LaunchedEffect(isVisible) {
        if (!isVisible) {
            delay(260)
            onDelete()
        }
    }
}
