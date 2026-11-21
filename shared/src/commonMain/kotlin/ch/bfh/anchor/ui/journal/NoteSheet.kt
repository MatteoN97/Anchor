package ch.bfh.anchor.ui.journal

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import ch.bfh.anchor.ui.icons.AnchorIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.model.TherapyNote

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteSheet(
    onDismiss: () -> Unit,
    onSave: (body: String, tags: List<String>, flagged: Boolean, questions: List<String>) -> Unit,
    existingNote: TherapyNote? = null
) {
    var body by remember { mutableStateOf(existingNote?.body ?: "") }
    var flagged by remember { mutableStateOf(existingNote?.flaggedForNextSession ?: false) }
    val selectedTags = remember {
        mutableStateListOf<String>().apply {
            existingNote?.tags?.let { addAll(it) }
        }
    }

    // Questions added in this sheet (to be saved to the global list on Save)
    val newQuestions = remember { mutableStateListOf<String>() }
    var questionDraft by remember { mutableStateOf("") }
    val questionFocusRequester = remember { FocusRequester() }

    val availableTags = listOf(
        stringResource(Res.string.journal_tag_insight),
        stringResource(Res.string.journal_tag_homework),
        stringResource(Res.string.journal_tag_coping_skill),
        stringResource(Res.string.journal_tag_discuss_next_time),
        stringResource(Res.string.journal_tag_progress)
    )

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(Res.string.common_cancel))
                }
                Text(
                    text = if (existingNote != null) stringResource(Res.string.journal_edit_note)
                    else stringResource(Res.string.journal_new_note),
                    style = MaterialTheme.typography.titleLarge
                )
                TextButton(
                    onClick = { onSave(body.trim(), selectedTags.toList(), flagged, newQuestions.toList()) },
                    enabled = body.isNotBlank()
                ) {
                    Text(
                        text = stringResource(Res.string.common_save),
                        color = if (body.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Note body
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text(stringResource(Res.string.journal_note_body_label)) },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                maxLines = 10
            )

            // Tags
            Text(
                text = stringResource(Res.string.journal_tags_title),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableTags.chunked(3).forEach { rowTags ->
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rowTags.forEach { tag ->
                            FilterChip(
                                selected = tag in selectedTags,
                                onClick = {
                                    if (tag in selectedTags) selectedTags.remove(tag)
                                    else selectedTags.add(tag)
                                },
                                label = { Text(tag, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider()

            // Questions for next session
            Text(
                text = stringResource(Res.string.journal_questions_title),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = stringResource(Res.string.journal_questions_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Already-queued questions for this note
            if (newQuestions.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    newQuestions.forEachIndexed { index, question ->
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = question,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { newQuestions.removeAt(index) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = AnchorIcons.Close,
                                        contentDescription = stringResource(Res.string.journal_question_remove),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Input row to add a new question
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = questionDraft,
                    onValueChange = { questionDraft = it },
                    placeholder = { Text(stringResource(Res.string.journal_question_placeholder)) },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(questionFocusRequester),
                    singleLine = true
                )
                FilledIconButton(
                    onClick = {
                        val q = questionDraft.trim()
                        if (q.isNotEmpty()) {
                            newQuestions.add(q)
                            questionDraft = ""
                        }
                    },
                    enabled = questionDraft.isNotBlank()
                ) {
                    Icon(
                        imageVector = AnchorIcons.Add,
                        contentDescription = stringResource(Res.string.journal_question_add)
                    )
                }
            }

            HorizontalDivider()

            // Flag for next session
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(Res.string.journal_next_session_title),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = stringResource(Res.string.journal_next_session_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Switch(checked = flagged, onCheckedChange = { flagged = it })
            }
        }
    }
}
