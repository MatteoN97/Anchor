package ch.bfh.anchor.ui.journal

import androidx.compose.ui.text.intl.Locale
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.SessionQuestion
import ch.bfh.anchor.model.TherapyNote
import ch.bfh.anchor.platform.formatNoteDate
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.adaptive.AdaptiveCheckbox
import ch.bfh.anchor.ui.adaptive.AdaptiveListRow
import ch.bfh.anchor.ui.adaptive.AdaptivePageScaffold
import ch.bfh.anchor.ui.adaptive.AdaptiveSegmentedControl
import ch.bfh.anchor.ui.adaptive.AdaptiveTag
import ch.bfh.anchor.ui.adaptive.AnchorIconButton
import ch.bfh.anchor.ui.adaptive.PageAction
import ch.bfh.anchor.ui.adaptive.RowEditActions
import ch.bfh.anchor.ui.adaptive.SwipeToDelete
import ch.bfh.anchor.ui.adaptive.adaptiveSection
import ch.bfh.anchor.ui.adaptive.emptyState
import ch.bfh.anchor.ui.adaptive.largeTitle
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.LocalIsIos
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.stringResource

private enum class JournalTab { Notes, Questions }

@Composable
fun JournalScreen(viewModel: AnchorViewModel) {
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.sessionQuestions.collectAsState()
    var selectedTab by remember { mutableStateOf(JournalTab.Notes) }
    var showAddSheet by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<TherapyNote?>(null) }
    val isIos = LocalIsIos.current
    // One scroll position per tab, so switching back does not jump.
    val notesListState = rememberLazyListState()
    val questionsListState = rememberLazyListState()
    val listState = if (selectedTab == JournalTab.Notes) notesListState else questionsListState

    val title = stringResource(Res.string.journal_title)
    val strings = JournalStrings(
        notesEmptyTitle = stringResource(Res.string.journal_empty_title),
        notesEmptySubtitle = stringResource(Res.string.journal_empty_subtitle),
        questionsEmptyTitle = stringResource(Res.string.journal_questions_empty_title),
        questionsEmptySubtitle = stringResource(Res.string.journal_questions_empty_subtitle),
        toDiscuss = stringResource(Res.string.journal_questions_to_discuss),
        discussed = stringResource(Res.string.journal_questions_discussed)
    )
    val pendingCount = questions.count { !it.answered }
    val segments: @Composable (Modifier) -> Unit = { modifier ->
        AdaptiveSegmentedControl(
            options = listOf(
                stringResource(Res.string.journal_tab_notes),
                stringResource(Res.string.journal_tab_questions)
            ),
            selectedIndex = selectedTab.ordinal,
            onSelect = { selectedTab = JournalTab.entries[it] },
            counts = listOf(null, pendingCount),
            modifier = modifier
        )
    }

    AdaptivePageScaffold(
        title = title,
        listState = listState,
        action = if (selectedTab == JournalTab.Notes) {
            PageAction(
                icon = AnchorIcons.Add,
                label = stringResource(Res.string.journal_add_note),
                onClick = { showAddSheet = true }
            )
        } else null
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            // Android pins the tabs under the app bar; iOS scrolls the segmented control with the title.
            if (!isIos) segments(Modifier)

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(bottom = padding.calculateBottomPadding() + 88.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                largeTitle(title)
                if (isIos) {
                    item(key = "segments") {
                        segments(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 20.dp))
                    }
                }

                when (selectedTab) {
                    JournalTab.Notes -> notesTab(
                        notes = notes,
                        strings = strings,
                        onToggleFlag = { note ->
                            viewModel.updateNote(note.copy(flaggedForNextSession = !note.flaggedForNextSession))
                        },
                        onEdit = { noteToEdit = it },
                        onDelete = { viewModel.deleteNote(it) }
                    )

                    JournalTab.Questions -> questionsTab(
                        questions = questions,
                        strings = strings,
                        onToggle = { viewModel.toggleSessionQuestion(it) },
                        onDelete = { viewModel.deleteSessionQuestion(it) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        NoteSheet(
            onDismiss = { showAddSheet = false },
            onSave = { body, tags, flagged, newQuestions ->
                viewModel.addNote(body, tags, flagged)
                newQuestions.forEach { viewModel.addSessionQuestion(it) }
                showAddSheet = false
            }
        )
    }

    noteToEdit?.let { note ->
        NoteSheet(
            existingNote = note,
            onDismiss = { noteToEdit = null },
            onSave = { body, tags, flagged, newQuestions ->
                viewModel.updateNote(note.copy(body = body, tags = tags, flaggedForNextSession = flagged))
                newQuestions.forEach { viewModel.addSessionQuestion(it) }
                noteToEdit = null
            }
        )
    }
}

/** Text the list builders need; resolved in composition because LazyListScope is not composable. */
private class JournalStrings(
    val notesEmptyTitle: String,
    val notesEmptySubtitle: String,
    val questionsEmptyTitle: String,
    val questionsEmptySubtitle: String,
    val toDiscuss: String,
    val discussed: String
)

// ── Notes tab ────────────────────────────────────────────────────────────────

private fun LazyListScope.notesTab(
    notes: List<TherapyNote>,
    strings: JournalStrings,
    onToggleFlag: (TherapyNote) -> Unit,
    onEdit: (TherapyNote) -> Unit,
    onDelete: (TherapyNote) -> Unit
) {
    if (notes.isEmpty()) {
        emptyState(strings.notesEmptyTitle, strings.notesEmptySubtitle)
        return
    }
    adaptiveSection(items = notes, key = { it.id }, separatorInset = 60.dp) { note ->
        NoteRow(note, onToggleFlag, onEdit, onDelete)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NoteRow(
    note: TherapyNote,
    onToggleFlag: (TherapyNote) -> Unit,
    onEdit: (TherapyNote) -> Unit,
    onDelete: (TherapyNote) -> Unit
) {
    val isIos = LocalIsIos.current
    val deleteLabel = stringResource(Res.string.common_delete)
    SwipeToDelete(onDelete = { onDelete(note) }, deleteLabel = deleteLabel) {
        AdaptiveListRow(
            headline = {
                Text(
                    text = note.body,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            },
            supporting = {
                Column(verticalArrangement = Arrangement.spacedBy(if (isIos) 6.dp else 4.dp)) {
                    Text(
                        text = formatNoteDate(note.date, Locale.current.language),
                        style = if (isIos) MaterialTheme.typography.bodySmall else MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (note.tags.isNotEmpty()) {
                        // Wraps: three of the five tags already overflow a phone-width row.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(if (isIos) 6.dp else 4.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            note.tags.forEach { tag -> AdaptiveTag(tag) }
                        }
                    }
                }
            },
            leading = {
                AnchorIconButton(onClick = { onToggleFlag(note) }) {
                    Icon(
                        imageVector = if (note.flaggedForNextSession) AnchorIcons.Bookmark
                        else AnchorIcons.BookmarkBorder,
                        contentDescription = if (note.flaggedForNextSession)
                            stringResource(Res.string.journal_flagged)
                        else stringResource(Res.string.journal_not_flagged),
                        tint = if (note.flaggedForNextSession) MaterialTheme.colorScheme.primary
                        else if (isIos) MaterialTheme.colorScheme.outline
                        else MaterialTheme.colorScheme.outlineVariant
                    )
                }
            },
            trailing = {
                RowEditActions(
                    onEdit = { onEdit(note) },
                    onDelete = { onDelete(note) },
                    editLabel = stringResource(Res.string.common_edit),
                    deleteLabel = deleteLabel
                )
            },
            onClick = if (isIos) ({ onEdit(note) }) else null
        )
    }
}

// ── Questions tab ─────────────────────────────────────────────────────────────

private fun LazyListScope.questionsTab(
    questions: List<SessionQuestion>,
    strings: JournalStrings,
    onToggle: (SessionQuestion) -> Unit,
    onDelete: (SessionQuestion) -> Unit
) {
    if (questions.isEmpty()) {
        emptyState(strings.questionsEmptyTitle, strings.questionsEmptySubtitle)
        return
    }
    val pending = questions.filter { !it.answered }
    val answered = questions.filter { it.answered }

    if (pending.isNotEmpty()) {
        adaptiveSection(items = pending, key = { it.id }, header = strings.toDiscuss, separatorInset = 60.dp) { question ->
            QuestionRow(question = question, onToggle = onToggle, onDelete = onDelete)
        }
    }
    if (answered.isNotEmpty()) {
        adaptiveSection(items = answered, key = { it.id }, header = strings.discussed, separatorInset = 60.dp) { question ->
            QuestionRow(question = question, onToggle = onToggle, onDelete = onDelete)
        }
    }
}

@Composable
private fun QuestionRow(
    question: SessionQuestion,
    onToggle: (SessionQuestion) -> Unit,
    onDelete: (SessionQuestion) -> Unit
) {
    val deleteLabel = stringResource(Res.string.common_delete)
    SwipeToDelete(onDelete = { onDelete(question) }, deleteLabel = deleteLabel) {
        AdaptiveListRow(
            headline = {
                Text(
                    text = question.text,
                    style = if (question.answered)
                        MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
                    else MaterialTheme.typography.bodyLarge,
                    color = if (question.answered) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface
                )
            },
            leading = {
                AdaptiveCheckbox(checked = question.answered, onCheckedChange = { onToggle(question) })
            },
            trailing = {
                RowEditActions(
                    onEdit = null,
                    onDelete = { onDelete(question) },
                    editLabel = "",
                    deleteLabel = deleteLabel
                )
            }
        )
    }
}
