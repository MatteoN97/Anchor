package ch.bfh.anchor.ui.crisis

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.HelpContact
import ch.bfh.anchor.platform.rememberPhoneDialer
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.*
import ch.bfh.anchor.ui.adaptive.AnchorButton
import ch.bfh.anchor.ui.adaptive.AnchorCardButton
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import ch.bfh.anchor.ui.icons.AnchorIcons
import ch.bfh.anchor.ui.theme.LocalIsIos
import org.jetbrains.compose.resources.stringResource

/**
 * "Call for help", pinned to the bottom of every step of the crisis flow, so reaching a person
 * never depends on getting through the steps first.
 *
 * With only the crisis line known it dials straight away: one tap, no choice to make. The
 * system still asks before the call is placed (Android opens the dialer, iOS confirms), so a
 * stray tap costs nothing. With a trusted person set up as well, a sheet offers both.
 */
@Composable
fun CallForHelpButton(
    crisisLine: HelpContact,
    trustedContact: HelpContact?,
    modifier: Modifier = Modifier
) {
    val dial = rememberPhoneDialer()
    var choosing by remember { mutableStateOf(false) }

    AnchorButton(
        onClick = {
            if (trustedContact == null) dial(crisisLine.number) else choosing = true
        },
        modifier = modifier.height(48.dp),
        containerColor = MaterialTheme.colorScheme.errorContainer,
        contentColor = MaterialTheme.colorScheme.onErrorContainer,
        shape = CircleShape
    ) {
        Icon(AnchorIcons.Call, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(Res.string.crisis_call_button), style = MaterialTheme.typography.labelLarge)
    }

    if (choosing && trustedContact != null) {
        CallChoiceSheet(
            crisisLine = crisisLine,
            trustedContact = trustedContact,
            onCall = { number ->
                choosing = false
                dial(number)
            },
            onDismiss = { choosing = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CallChoiceSheet(
    crisisLine: HelpContact,
    trustedContact: HelpContact,
    onCall: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val isIos = LocalIsIos.current
    val colors = MaterialTheme.colorScheme
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = if (isIos) colors.background else BottomSheetDefaults.ContainerColor,
        dragHandle = if (isIos) {
            {
                Box(
                    Modifier
                        .padding(top = 6.dp)
                        .size(width = 36.dp, height = 5.dp)
                        .clip(CircleShape)
                        .background(colors.outline)
                )
            }
        } else {
            { BottomSheetDefaults.DragHandle() }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = if (isIos) 16.dp else 24.dp)
                .padding(top = 12.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(Res.string.crisis_call_sheet_title),
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            )
            TrustedContactCallCard(trustedContact, onClick = { onCall(trustedContact.number) })
            CrisisLineCallCard(crisisLine, onClick = { onCall(crisisLine.number) })
            AnchorTextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(Res.string.common_cancel))
            }
        }
    }
}

@Composable
internal fun TrustedContactCallCard(contact: HelpContact, onClick: () -> Unit) {
    CallCard(
        title = if (contact.name.isNotBlank()) {
            stringResource(Res.string.crisis_call_trusted_title, contact.name)
        } else {
            stringResource(Res.string.crisis_call_trusted_fallback)
        },
        subtitle = contact.number,
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
        onClick = onClick
    )
}

@Composable
internal fun CrisisLineCallCard(line: HelpContact, onClick: () -> Unit) {
    CallCard(
        title = stringResource(Res.string.crisis_call_line_title),
        subtitle = if (line.name.isNotBlank()) "${line.name} — ${line.number}" else line.number,
        containerColor = MaterialTheme.colorScheme.error,
        contentColor = MaterialTheme.colorScheme.onError,
        onClick = onClick
    )
}

@Composable
private fun CallCard(
    title: String,
    subtitle: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit
) {
    AnchorCardButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        contentColor = contentColor,
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}
