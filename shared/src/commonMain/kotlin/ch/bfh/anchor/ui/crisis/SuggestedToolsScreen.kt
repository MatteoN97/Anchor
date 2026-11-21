package ch.bfh.anchor.ui.crisis

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ch.bfh.anchor.model.BuiltInTools
import ch.bfh.anchor.model.CopingTool
import ch.bfh.anchor.model.Feeling
import ch.bfh.anchor.resources.Res
import ch.bfh.anchor.resources.common_continue
import ch.bfh.anchor.resources.crisis_suggestions_add
import ch.bfh.anchor.resources.crisis_suggestions_added
import ch.bfh.anchor.resources.crisis_suggestions_badge
import ch.bfh.anchor.resources.crisis_suggestions_subtitle_feeling
import ch.bfh.anchor.resources.crisis_suggestions_title
import androidx.compose.foundation.shape.CircleShape
import ch.bfh.anchor.ui.adaptive.AnchorButton
import ch.bfh.anchor.ui.components.calmCardColor
import ch.bfh.anchor.ui.adaptive.AnchorTextButton
import ch.bfh.anchor.viewmodel.AnchorViewModel
import org.jetbrains.compose.resources.stringResource

@Composable
fun SuggestedToolsScreen(
    feeling: Feeling,
    viewModel: AnchorViewModel,
    onFinished: () -> Unit,
    onToolAdded: (CopingTool) -> Unit = {}
) {
    val tools by viewModel.tools.collectAsState()

    // The suggestions are picked once and then held for as long as this step is on screen.
    // Deriving them from the live tool list reshuffled the rows under the finger that had just
    // tapped "add to my toolkit": the new tool changed what suggestionsFor returns, so the row
    // being tapped could move or disappear. The only recompute allowed is the first list to
    // arrive from the database, in case this screen opened before that happened.
    // Built-in techniques in the current language; resolved here because the picking below
    // happens outside composition.
    val builtInNames = BuiltInTools.all.associate { it.id to stringResource(it.nameRes) }
    val builtInDescriptions = BuiltInTools.all.associate { it.id to stringResource(it.descriptionRes) }
    val resolve: (BuiltInTools.Definition) -> CopingTool = { definition ->
        definition.toTool(builtInNames.getValue(definition.id), builtInDescriptions.getValue(definition.id))
    }

    var suggestions by remember(feeling) {
        mutableStateOf(BuiltInTools.suggestionsFor(feeling, tools, resolve))
    }
    var pickedFromStoredTools by remember(feeling) { mutableStateOf(tools.isNotEmpty()) }
    LaunchedEffect(feeling, tools) {
        if (!pickedFromStoredTools && tools.isNotEmpty()) {
            suggestions = BuiltInTools.suggestionsFor(feeling, tools, resolve)
            pickedFromStoredTools = true
        }
    }

    // Reacts to the live list on purpose: the suggestion rows stay put, but a built-in tool that
    // has just been added says so, instead of offering to add it a second time.
    val toolkitNames = remember(tools) { tools.mapTo(mutableSetOf()) { it.name.trim().lowercase() } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(Res.string.crisis_suggestions_title),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = stringResource(
                    Res.string.crisis_suggestions_subtitle_feeling,
                    stringResource(feeling.phraseRes)
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            suggestions.tools.forEach { tool ->
                val isBuiltIn = suggestions.builtIn.any { it.id == tool.id }
                SuggestedToolCard(
                    tool = tool,
                    isBuiltIn = isBuiltIn,
                    alreadyInToolkit = tool.name.trim().lowercase() in toolkitNames,
                    onAdd = { onToolAdded(tool) }
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnchorButton(
            onClick = onFinished,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = CircleShape
        ) {
            Text(
                text = stringResource(Res.string.common_continue),
                style = MaterialTheme.typography.titleMedium
            )
        }

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
private fun SuggestedToolCard(
    tool: CopingTool,
    isBuiltIn: Boolean,
    alreadyInToolkit: Boolean,
    onAdd: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = calmCardColor(),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = tool.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                if (isBuiltIn) {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = MaterialTheme.shapes.extraLarge
                    ) {
                        Text(
                            text = stringResource(Res.string.crisis_suggestions_badge),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
            if (tool.description.isNotEmpty()) {
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isBuiltIn) {
                if (alreadyInToolkit) {
                    Text(
                        text = stringResource(Res.string.crisis_suggestions_added),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    AnchorTextButton(onClick = onAdd, contentPadding = PaddingValues(0.dp)) {
                        Text(
                            text = stringResource(Res.string.crisis_suggestions_add),
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        }
    }
}
