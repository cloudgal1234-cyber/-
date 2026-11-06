package com.taskalarm.app.ui.task

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlin.random.Random

private const val GRID = 3
private const val START_LENGTH = 3

private enum class Phase { SHOWING, INPUT, WRONG }

/**
 * Simon-style memory game: tiles flash in sequence and must be tapped back in order.
 * Each round the sequence grows by one; a mistake replays the round with a new sequence.
 */
@Composable
fun MemoryTask(rounds: Int, onComplete: () -> Unit) {
    val total = rounds.coerceAtLeast(1)
    var round by remember { mutableIntStateOf(0) }
    var attempt by remember { mutableIntStateOf(0) }
    var sequence by remember { mutableStateOf(emptyList<Int>()) }
    var phase by remember { mutableStateOf(Phase.SHOWING) }
    var lit by remember { mutableStateOf<Int?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var flash by remember { mutableStateOf<Pair<Int, Int>?>(null) } // tile to tap-flash, unique key
    var tapCounter by remember { mutableIntStateOf(0) }
    val done by rememberUpdatedState(onComplete)

    LaunchedEffect(round, attempt) {
        val length = START_LENGTH + round
        sequence = List(length) { Random.nextInt(GRID * GRID) }
        progress = 0
        phase = Phase.SHOWING
        delay(700)
        for (tile in sequence) {
            lit = tile; delay(550)
            lit = null; delay(200)
        }
        phase = Phase.INPUT
    }
    LaunchedEffect(flash) {
        if (flash != null) {
            delay(180); flash = null
        }
    }
    LaunchedEffect(phase) {
        if (phase == Phase.WRONG) {
            delay(1200)
            attempt++
        }
    }

    fun tap(tile: Int) {
        if (phase != Phase.INPUT) return
        flash = tile to ++tapCounter
        if (sequence.getOrNull(progress) == tile) {
            progress++
            if (progress == sequence.size) {
                if (round + 1 >= total) done() else round++
            }
        } else {
            phase = Phase.WRONG
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("סבב ${round + 1} מתוך $total", style = MaterialTheme.typography.titleMedium)
        Text(
            when (phase) {
                Phase.SHOWING -> "שים לב לרצף…"
                Phase.INPUT -> "חזור על הרצף (${progress}/${sequence.size})"
                Phase.WRONG -> "טעות! רצף חדש בדרך"
            },
            textAlign = TextAlign.Center,
            color = if (phase == Phase.WRONG) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Column(Modifier.fillMaxWidth(0.85f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            for (r in 0 until GRID) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    for (c in 0 until GRID) {
                        val tile = r * GRID + c
                        val color by animateColorAsState(
                            if (lit == tile || flash?.first == tile) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh,
                            label = "tile"
                        )
                        Box(
                            Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(color)
                                .clickable(enabled = phase == Phase.INPUT) {
                                    tap(tile)
                                }
                        )
                    }
                }
            }
        }
    }
}
