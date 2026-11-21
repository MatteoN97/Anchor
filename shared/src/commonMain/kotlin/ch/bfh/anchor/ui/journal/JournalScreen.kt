package ch.bfh.anchor.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import ch.bfh.anchor.ui.icons.AnchorIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.platform.formatNoteDate
import ch.bfh.anchor.model.SessionQuestion
import ch.bfh.anchor.model.TherapyNote
import ch.bfh.anchor.viewmodel.AnchorViewModel

private enum class JournalTab { Notes, Questions }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(viewModel: AnchorViewModel) {
    val notes by viewModel.notes.collectAsState()
    val questions by viewModel.sessionQuestions.collectAsState()
    var selectedTab by remember { mutableStateOf(JournalTab.Notes) }
    var showAddSheet by remember { mutableStateOf(false) }
    var noteToEdit by remember { mutableStateOf<TherapyNote?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.journal_title)) })
        },
        floatingActionButton = {
            if (selectedTab == JournalTab.Notes) {
                FloatingActionButton(onClick = { showAddSheet = true }) {
                    Icon(AnchorIcons.Add, contentDescription = stringResource(Res.string.journal_add_note))
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab bar
            TabRow(selectedTabIndex = selectedTab.ordinal) {
                Tab(
                    selected = selectedTab == JournalTab.Notes,
                    onClick = { selectedTab = JournalTab.Notes },
                    text = { Text("Notes") }
                )
                Tab(
                    selected = selectedTab == JournalTab.Questions,
                    onClick = { selectedTab = JournalTab.Questions },
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text("Questions")
                            val pending = questions.count { !it.answered }
                            if (pending > 0) {
                                Badge { Text("$pending") }
                            }
                        }
                    }
                )
            }

            when (selectedTab) {
                JournalTab.Notes -> NotesTab(
                    notes = notes,
                    onToggleFlag = { note ->
                        viewModel.updateNote(note.copy(flaggedForNextSession = !note.flaggedForNextSession))
                    },
                    onEdit = { noteToEdit = it },
                    onDelete = { viewModel.deleteNote(it) }
                )

                JournalTab.Questions -> QuestionsTab(
                    questions = questions,
                    onToggle = { viewModel.toggleSessionQuestion(it) },
                    onDelete = { viewModel.deleteSessionQuestion(it) }
                )
            }
        }
    }

    // Add note sheet
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

    // Edit note sheet
    noteToEdit?.let { note ->
        NoteSheet(
            existingNote = note,
            onDismiss = { noteToEdit = null },
            onSave = { body, tags, flagged, newQuestions ->
                viewModel.updateNote(note.copy(body = body, tags = tags.joinToString(","), flaggedForNextSession = flagged))
                newQuestions.forEach { viewModel.addSessionQuestion(it) }
                noteToEdit = null
            }
        )
    }
}

// ── Notes tab ────────────────────────────────────────────────────────────────

@Composable
private fun NotesTab(
    notes: List<TherapyNote>,
    onToggleFlag: (TherapyNote) -> Unit,
    onEdit: (TherapyNote) -> Unit,
    onDelete: (TherapyNote) -> Unit
) {
    if (notes.isEmpty()) {
        EmptyState(
            title = stringResource(Res.string.journal_empty_title),
            subtitle = stringResource(Res.string.journal_empty_subtitle)
        )
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(notes, key = { it.id }) { note ->
                ListItem(
                    headlineContent = {
                        Text(
                            text = note.body,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    supportingContent = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = formatNoteDate(note.date),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (note.tags.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    note.tags.split(",").filter { it.isNotBlank() }.forEach { tag ->
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }
                            }
                        }
                    },
                    leadingContent = {
                        IconButton(onClick = { onToggleFlag(note) }) {
                            Icon(
                                imageVector = if (note.flaggedForNextSession) AnchorIcons.Bookmark
                                else AnchorIcons.BookmarkBorder,
                                contentDescription = if (note.flaggedForNextSession)
                                    stringResource(Res.string.journal_flagged)
                                else stringResource(Res.string.journal_not_flagged),
                                tint = if (note.flaggedForNextSession)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                    },
                    trailingContent = {
                        Row {
                            IconButton(onClick = { onEdit(note) }) {
                                Icon(AnchorIcons.Edit, contentDescription = stringResource(Res.string.common_edit),
                                    tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { onDelete(note) }) {
                                Icon(AnchorIcons.Delete, contentDescription = stringResource(Res.string.common_delete),
                                    tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                )
                HorizontalDivider()
            }
        }
    }
}

// ── Questions tab ─────────────────────────────────────────────────────────────

@Composable
private fun QuestionsTab(
    questions: List<SessionQuestion>,
    onToggle: (SessionQuestion) -> Unit,
    onDelete: (SessionQuestion) -> Unit
) {
    if (questions.isEmpty()) {
        EmptyState(
            title = "No questions yet",
            subtitle = "Add questions while writing a note — they'll appear here."
        )
    } else {
        val pending = questions.filter { !it.answered }
        val answered = questions.filter { it.answered }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            if (pending.isNotEmpty()) {
                item {
                    Text(
                        text = "To discuss",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(pending, key = { it.id }) { question ->
                    QuestionRow(question = question, onToggle = onToggle, onDelete = onDelete)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }

            if (answered.isNotEmpty()) {
                item {
                    Text(
                        text = "Discussed",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
                items(answered, key = { it.id }) { question ->
                    QuestionRow(question = question, onToggle = onToggle, onDelete = onDelete)
                    HorizontalDivider(modifier = Modifier.padding(start = 56.dp))
                }
            }
        }
    }
}

@Composable
private fun QuestionRow(
    question: SessionQuestion,
    onToggle: (SessionQuestion) -> Unit,
    onDelete: (SessionQuestion) -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = question.text,
                style = if (question.answered)
                    MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough)
                else MaterialTheme.typography.bodyLarge,
                color = if (question.answered) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface
            )
        },
        leadingContent = {
            Checkbox(
                checked = question.answered,
                onCheckedChange = { onToggle(question) }
            )
        },
        trailingContent = {
            IconButton(onClick = { onDelete(question) }) {
                Icon(
                    AnchorIcons.Delete,
                    contentDescription = stringResource(Res.string.common_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    )
}

// ── Shared ────────────────────────────────────────────────────────────────────

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
