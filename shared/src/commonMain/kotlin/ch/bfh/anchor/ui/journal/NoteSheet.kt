package ch.bfh.anchor.ui.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.ui.draw.clip
import androidx.compose.material3.*
import ch.bfh.anchor.ui.icons.AnchorIcons
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import org.jetbrains.compose.resources.stringResource
import ch.bfh.anchor.model.TherapyNote
import ch.bfh.anchor.ui.adaptive.AdaptiveFilterChip
import ch.bfh.anchor.ui.adaptive.AdaptiveSheet
import ch.bfh.anchor.ui.adaptive.AdaptiveSwitch
import ch.bfh.anchor.ui.adaptive.AdaptiveTextField
import ch.bfh.anchor.ui.adaptive.AnchorIconButton
import ch.bfh.anchor.ui.theme.LocalIsIos

@OptIn(ExperimentalLayoutApi::class)
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

    val availableTags = listOf(
        stringResource(Res.string.journal_tag_insight),
        stringResource(Res.string.journal_tag_homework),
        stringResource(Res.string.journal_tag_coping_skill),
        stringResource(Res.string.journal_tag_discuss_next_time),
        stringResource(Res.string.journal_tag_progress)
    )

    val isIos = LocalIsIos.current

    AdaptiveSheet(
        title = if (existingNote != null) stringResource(Res.string.journal_edit_note)
        else stringResource(Res.string.journal_new_note),
        cancelLabel = stringResource(Res.string.common_cancel),
        confirmLabel = stringResource(Res.string.common_save),
        confirmEnabled = body.isNotBlank(),
        onDismiss = onDismiss,
        onConfirm = { onSave(body.trim(), selectedTags.toList(), flagged, newQuestions.toList()) }
    ) {
        AdaptiveTextField(
            value = body,
            onValueChange = { body = it },
            label = stringResource(Res.string.journal_note_body_label),
            minLines = 4,
            maxLines = 10,
            // iOS shows the field's purpose as its placeholder, the sheet title already names it.
            showLabel = !isIos
        )

        // Tags
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetSectionTitle(stringResource(Res.string.journal_tags_title))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                availableTags.forEach { tag ->
                    AdaptiveFilterChip(
                        selected = tag in selectedTags,
                        onClick = {
                            if (tag in selectedTags) selectedTags.remove(tag)
                            else selectedTags.add(tag)
                        },
                        label = tag
                    )
                }
            }
        }

        if (!isIos) HorizontalDivider()

        // Questions for next session
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetSectionTitle(stringResource(Res.string.journal_questions_title))
            Text(
                text = stringResource(Res.string.journal_questions_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = if (isIos) Modifier.padding(horizontal = 16.dp) else Modifier
            )

            newQuestions.forEachIndexed { index, question ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = question,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        AnchorIconButton(onClick = { newQuestions.removeAt(index) }) {
                            Icon(
                                imageVector = AnchorIcons.Close,
                                contentDescription = stringResource(Res.string.journal_question_remove),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                AdaptiveTextField(
                    value = questionDraft,
                    onValueChange = { questionDraft = it },
                    label = stringResource(Res.string.journal_question_placeholder),
                    showLabel = false,
                    singleLine = true,
                    modifier = Modifier.weight(1f)
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
        }

        if (!isIos) HorizontalDivider()

        // Flag for next session: a switch row, on iOS in its own white cell like a Form toggle.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (isIos) Modifier
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                    else Modifier
                )
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.journal_next_session_title),
                    style = if (isIos) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.titleSmall
                )
                Text(
                    text = stringResource(Res.string.journal_next_session_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            AdaptiveSwitch(checked = flagged, onCheckedChange = { flagged = it })
        }
    }
}

@Composable
private fun SheetSectionTitle(text: String) {
    if (LocalIsIos.current) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    } else {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
