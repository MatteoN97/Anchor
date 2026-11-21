package ch.bfh.anchor.ui.mood

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ch.bfh.anchor.model.MoodScale

/** The same five faces, labels and order everywhere Anchor asks for a mood. */
@Composable
fun MoodChoices(
    selectedScore: Int?,
    onScoreSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MoodScale.displayScores.forEach { score ->
            val selected = selectedScore == score
            Surface(
                onClick = { onScoreSelected(score) },
                modifier = Modifier.weight(1f),
                color = if (selected) MaterialTheme.colorScheme.primary
                        else ch.bfh.anchor.ui.components.calmCardColor(),
                shape = MaterialTheme.shapes.medium,
                border = if (selected) null
                         else androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(MoodScale.emoji(score), fontSize = 24.sp)
                    Text(
                        MoodScale.label(score),
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        maxLines = 2,
                        textAlign = TextAlign.Center,
                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
