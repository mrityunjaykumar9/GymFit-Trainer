package com.freegymtrainer.app.presentation.nutrition

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.freegymtrainer.app.domain.nutrition.Fasting
import com.freegymtrainer.app.domain.nutrition.FastingPhase
import com.freegymtrainer.app.domain.nutrition.FastingProtocol
import com.freegymtrainer.app.domain.nutrition.FoodSuggestion
import com.freegymtrainer.app.domain.nutrition.Tip
import com.freegymtrainer.app.domain.nutrition.TipKind
import com.freegymtrainer.app.presentation.components.FoodIcon
import com.freegymtrainer.app.presentation.components.foodIconFile
import kotlinx.coroutines.delay
import java.time.LocalTime

private val GoodGreen = Color(0xFF4CAF50)
private val WarnAmber = Color(0xFFFFA726)

/** A few plain-language tips from today's numbers. */
@Composable
fun CoachCard(tips: List<Tip>) {
    if (tips.isEmpty()) return
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text("Today's coach", style = MaterialTheme.typography.titleMedium)
            tips.forEach { tip ->
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Top) {
                    val (icon, tint) = when (tip.kind) {
                        TipKind.GOOD -> Icons.Filled.CheckCircle to GoodGreen
                        TipKind.WARN -> Icons.Filled.Warning to WarnAmber
                        TipKind.INFO -> Icons.Filled.Lightbulb to MaterialTheme.colorScheme.primary
                    }
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.padding(top = 2.dp).size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(tip.title, style = MaterialTheme.typography.titleSmall)
                        Text(tip.body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** "What to eat next": foods that fit what is left of today's calories and protein. Tapping one opens it ready to log. */
@Composable
fun SuggestionsRow(suggestions: List<FoodSuggestion>, remainingKcal: Int, remainingProtein: Int, onPick: (FoodSuggestion) -> Unit) {
    if (suggestions.isEmpty()) return
    Text("What to eat next", style = MaterialTheme.typography.titleMedium)
    Text(
        "Fits your remaining $remainingKcal kcal" + if (remainingProtein > 0) " and ${remainingProtein} g protein" else "",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(Modifier.height(8.dp))
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        suggestions.forEach { s ->
            ElevatedCard(
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.width(150.dp).clickable { onPick(s) }
            ) {
                Column(Modifier.padding(12.dp)) {
                    FoodIcon(foodIconFile(s.food.name, s.food.category), size = 48.dp)
                    Spacer(Modifier.height(8.dp))
                    Text(s.food.name, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                    Spacer(Modifier.height(2.dp))
                    Text("${s.protein} g protein", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                    Text("${s.grams.toInt()} g · ${s.calories} kcal", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

/** Intermittent-fasting timer: pick an eating window and see how long until it opens or closes. */
@Composable
fun FastingCard(
    protocol: FastingProtocol,
    startMinute: Int,
    onProtocol: (FastingProtocol) -> Unit,
    onShiftStart: (Int) -> Unit
) {
    // Re-evaluate every 30 seconds so the countdown stays current while the screen is open.
    val now by produceState(initialValue = LocalTime.now().let { it.hour * 60 + it.minute }) {
        while (true) { delay(30_000); value = LocalTime.now().let { it.hour * 60 + it.minute } }
    }
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Fasting timer", style = MaterialTheme.typography.titleMedium)
            }
            Text(
                "Intermittent fasting is one way to manage calories - not required, and not for everyone.",
                style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FastingProtocol.values().forEach { p ->
                    FilterChip(selected = protocol == p, onClick = { onProtocol(p) }, label = { Text(p.label) })
                }
            }
            val status = Fasting.status(protocol, startMinute, now)
            if (status != null) {
                Spacer(Modifier.height(12.dp))
                val eating = status.phase == FastingPhase.EATING
                Text(
                    if (eating) "Eating window open" else "Fasting",
                    style = MaterialTheme.typography.titleLarge, color = if (eating) GoodGreen else MaterialTheme.colorScheme.primary
                )
                Text(
                    (if (eating) "Closes in " else "Eating window opens in ") + Fasting.formatDuration(status.minutesLeft) +
                        "  ·  ${Fasting.formatClock(status.windowStartMinute)} - ${Fasting.formatClock(status.windowEndMinute)}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { status.progress.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
                    color = if (eating) GoodGreen else MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Window starts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    OutlinedButton(onClick = { onShiftStart(-30) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("-30 min") }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = { onShiftStart(30) }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) { Text("+30 min") }
                }
            }
        }
    }
}

/** A tappable card that opens the diet guide. */
@Composable
fun GuideLinkCard(onClick: () -> Unit) {
    ElevatedCard(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().clickable(onClick = onClick)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) { Icon(Icons.Filled.AutoStories, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Diet guide", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Fat loss, muscle gain, the balanced plate and eating out - explained simply",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
