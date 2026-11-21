package ch.bfh.anchor.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.selection.toggleable
import ch.bfh.anchor.model.TherapyNote
import ch.bfh.anchor.platform.formatNoteDateTime
import ch.bfh.anchor.platform.nowMillis
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.ui.adaptive.AdaptiveFilterChip
import ch.bfh.anchor.ui.adaptive.AdaptiveSwitch
import ch.bfh.anchor.ui.adaptive.AnchorButton
import ch.bfh.anchor.ui.adaptive.AnchorIconButton
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import ch.bfh.anchor.ui.components.CloseButton
import ch.bfh.anchor.ui.components.IconBadge
import ch.bfh.anchor.ui.components.calmCardColor
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

/** Joins lists for rememberSaveable; same separator the database uses for tags. */
private const val SEP = '\u001F'
private fun List<String>.pack() = joinToString(SEP.toString())
private fun String.unpack() = if (isEmpty()) emptyList() else split(SEP)

/**
 * Full-screen writing for therapy notes, replacing the bottom sheet whose ten-line field made
 * longer entries painful. The text gets the whole screen; tags, questions and the flag live
 * below it in a "Details" card, and the flag is also one tap away in the bar above the keyboard.
 *
 * Nothing is written to the database until Save. Leaving with unsaved text asks first, and every
 * field is saveable, so rotation or process death does not lose a half-written note.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalComposeUiApi::class)
@Composable
fun NoteEditorScreen(
    vm: AnchorViewModel,
    noteId: String?,
    onClose: () -> Unit
) {
    val notes by vm.notes.collectAsState()
    val existing: TherapyNote? = noteId?.let { id -> notes.firstOrNull { it.id == id } }

    var loaded by rememberSaveable { mutableStateOf(false) }
    var body by rememberSaveable { mutableStateOf("") }
    var flagged by rememberSaveable { mutableStateOf(false) }
    var tagsPacked by rememberSaveable { mutableStateOf("") }
    var questionsPacked by rememberSaveable { mutableStateOf("") }
    var questionDraft by rememberSaveable { mutableStateOf("") }
    var confirmDiscard by rememberSaveable { mutableStateOf(false) }
    val createdAt = rememberSaveable { existing?.date ?: nowMillis() }

    // An edited note is filled in once the list has arrived from the database (it can still be
    // empty for a moment after process restore). Until then the editor shows nothing editable,
    // so Save can never turn an edit into a second, new note.
    LaunchedEffect(existing, noteId) {
        if (loaded) return@LaunchedEffect
        if (noteId == null) {
            loaded = true
        } else if (existing != null) {
            body = existing.body
            flagged = existing.flaggedForNextSession
            tagsPacked = existing.tags.pack()
            loaded = true
        }
    }

    val tags = tagsPacked.unpack()
    val questions = questionsPacked.unpack()
    val dirty = loaded && (
        if (existing == null) body.isNotBlank() || questions.isNotEmpty() || tags.isNotEmpty() || flagged
        else body != existing.body || flagged != existing.flaggedForNextSession ||
            tags != existing.tags || questions.isNotEmpty()
        )

    fun save() {
        val text = body.trim()
        if (text.isEmpty()) return
        if (existing != null) {
            vm.updateNote(existing.copy(body = text, tags = tags, flaggedForNextSession = flagged))
        } else {
            vm.addNote(text, tags, flagged)
        }
        questions.forEach { vm.addSessionQuestion(it) }
        onClose()
    }

    fun requestClose() {
        if (dirty) confirmDiscard = true else onClose()
    }

    NavigationBackHandler(
        state = rememberNavigationEventState(currentInfo = NavigationEventInfo.None),
        isBackEnabled = dirty,
        onBackCompleted = { confirmDiscard = true }
    )

    val availableTags = listOf(
        stringResource(Res.string.journal_tag_insight),
        stringResource(Res.string.journal_tag_homework),
        stringResource(Res.string.journal_tag_coping_skill),
        stringResource(Res.string.journal_tag_discuss_next_time),
        stringResource(Res.string.journal_tag_progress)
    )
    val colors = MaterialTheme.colorScheme
    val bodyFocus = remember { FocusRequester() }
    LaunchedEffect(loaded) {
        // A new note opens ready to type; an existing one opens for reading first.
        if (loaded && noteId == null) runCatching { bodyFocus.requestFocus() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top))
    ) {
        // Top bar: close on the left, Save on the right.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CloseButton(onClick = ::requestClose)
            Spacer(Modifier.weight(1f))
            AnchorButton(
                onClick = ::save,
                enabled = loaded && body.isNotBlank(),
                shape = CircleShape,
                modifier = Modifier.height(40.dp)
            ) {
                Text(stringResource(Res.string.common_save), style = MaterialTheme.typography.labelLarge)
            }
        }

        if (!loaded) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Column
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = formatNoteDateTime(createdAt).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.primary
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (existing != null) stringResource(Res.string.journal_edit_note)
                else stringResource(Res.string.journal_new_note),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                modifier = Modifier.semantics { heading() }
            )
            Spacer(Modifier.height(16.dp))

            val bodyStyle = MaterialTheme.typography.bodyLarge.copy(
                fontSize = 18.sp,
                lineHeight = 29.sp,
                color = colors.onBackground
            )
            BasicTextField(
                value = body,
                onValueChange = { body = it },
                textStyle = bodyStyle,
                cursorBrush = SolidColor(colors.primary),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 260.dp)
                    .focusRequester(bodyFocus),
                decorationBox = { inner ->
                    Box {
                        if (body.isEmpty()) {
                            Text(
                                text = stringResource(Res.string.journal_note_body_label),
                                style = bodyStyle,
                                color = colors.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                        inner()
                    }
                }
            )

            Spacer(Modifier.height(28.dp))

            // Details
            Text(
                text = stringResource(Res.string.journal_editor_details),
                style = MaterialTheme.typography.titleSmall,
                color = colors.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp).semantics { heading() }
            )
            Surface(
                color = calmCardColor(),
                shape = MaterialTheme.shapes.large,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    DetailHeader(AnchorIcons.LocalOffer, stringResource(Res.string.journal_tags_title))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        availableTags.forEach { tag ->
                            AdaptiveFilterChip(
                                selected = tag in tags,
                                onClick = {
                                    tagsPacked = (if (tag in tags) tags - tag else tags + tag).pack()
                                },
                                label = tag
                            )
                        }
                    }

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))

                    DetailHeader(AnchorIcons.HelpOutline, stringResource(Res.string.journal_questions_title))
                    Text(
                        text = stringResource(Res.string.journal_questions_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant
                    )
                    questions.forEachIndexed { index, question ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialTheme.shapes.small)
                                .background(colors.secondary.copy(alpha = 0.10f))
                                .padding(start = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = question,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f).padding(vertical = 10.dp)
                            )
                            AnchorIconButton(onClick = {
                                questionsPacked = questions.filterIndexed { i, _ -> i != index }.pack()
                            }) {
                                Icon(
                                    AnchorIcons.Close,
                                    contentDescription = stringResource(Res.string.journal_question_remove),
                                    tint = colors.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                    QuestionInput(
                        value = questionDraft,
                        onValueChange = { questionDraft = it },
                        onAdd = {
                            val q = questionDraft.trim()
                            if (q.isNotEmpty()) {
                                questionsPacked = (questions + q).pack()
                                questionDraft = ""
                            }
                        }
                    )

                    HorizontalDivider(color = colors.outlineVariant.copy(alpha = 0.6f))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(value = flagged, role = Role.Switch, onValueChange = { flagged = it }),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconBadge(
                            icon = AnchorIcons.Bookmark,
                            containerColor = colors.primary.copy(alpha = 0.12f),
                            contentColor = colors.primary,
                            size = 32.dp,
                            iconSize = 18.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                stringResource(Res.string.journal_next_session_title),
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                stringResource(Res.string.journal_next_session_subtitle),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        AdaptiveSwitch(checked = flagged, onCheckedChange = { flagged = it })
                    }
                }
            }
            Spacer(Modifier.height(32.dp))
        }

        // Writing bar: sits on the keyboard while typing, at the bottom otherwise.
        WritingBar(
            words = remember(body) { body.split(Regex("\\s+")).count { it.isNotBlank() } },
            flagged = flagged,
            onToggleFlag = { flagged = !flagged }
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = {
                Text(
                    if (existing != null) stringResource(Res.string.journal_discard_edit_title)
                    else stringResource(Res.string.journal_discard_new_title)
                )
            },
            text = { Text(stringResource(Res.string.journal_discard_message)) },
            confirmButton = {
                AnchorTextButton(onClick = { confirmDiscard = false }, emphasized = true) {
                    Text(stringResource(Res.string.journal_keep_writing))
                }
            },
            dismissButton = {
                AnchorTextButton(onClick = {
                    confirmDiscard = false
                    onClose()
                }) {
                    Text(stringResource(Res.string.journal_discard_confirm), color = colors.error)
                }
            },
            containerColor = colors.surfaceContainerHigh
        )
    }
}

@Composable
private fun DetailHeader(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, style = MaterialTheme.typography.titleSmall)
    }
}

/** A rounded single-line input with a round "+" inside, so adding a question is one motion. */
@Composable
private fun QuestionInput(value: String, onValueChange: (String) -> Unit, onAdd: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    val style = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(colors.onSurface.copy(alpha = 0.05f))
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = style,
            cursorBrush = SolidColor(colors.primary),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Sentences,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { onAdd() }),
            modifier = Modifier.weight(1f).padding(vertical = 12.dp),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(
                            stringResource(Res.string.journal_question_placeholder),
                            style = style,
                            color = colors.onSurfaceVariant.copy(alpha = 0.6f),
                            maxLines = 1
                        )
                    }
                    inner()
                }
            }
        )
        AnchorIconButton(onClick = onAdd, enabled = value.isNotBlank()) {
            Box(
                Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(if (value.isNotBlank()) colors.primary else colors.outlineVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    AnchorIcons.Add,
                    contentDescription = stringResource(Res.string.journal_question_add),
                    tint = colors.onPrimary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun WritingBar(words: Int, flagged: Boolean, onToggleFlag: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Surface(color = colors.surfaceContainerLow, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = pluralStringResource(Res.plurals.journal_editor_words, words, words),
                style = MaterialTheme.typography.labelMedium,
                color = colors.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            AdaptiveFilterChip(
                selected = flagged,
                onClick = onToggleFlag,
                label = stringResource(Res.string.journal_flag_short)
            )
        }
    }
}
