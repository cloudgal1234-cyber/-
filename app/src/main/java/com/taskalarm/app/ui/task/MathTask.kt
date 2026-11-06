package com.taskalarm.app.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskalarm.app.data.MathProblems
import kotlinx.coroutines.delay

/** Solve [count] arithmetic problems. A wrong answer replaces the problem with a new one. */
@Composable
fun MathTask(count: Int, difficulty: Int, onComplete: () -> Unit) {
    val total = count.coerceAtLeast(1)
    var solvedCount by remember { mutableIntStateOf(0) }
    var problem by remember { mutableStateOf(MathProblems.generate(difficulty)) }
    var input by remember { mutableStateOf("") }
    var wrongCount by remember { mutableIntStateOf(0) }
    var showWrong by remember { mutableStateOf(false) }
    val done by rememberUpdatedState(onComplete)

    LaunchedEffect(wrongCount) {
        if (wrongCount > 0) {
            showWrong = true; delay(1500); showWrong = false
        }
    }

    fun submit() {
        val value = input.toIntOrNull() ?: return
        if (value == problem.answer) {
            solvedCount++
            if (solvedCount >= total) done()
        } else {
            wrongCount++
        }
        problem = MathProblems.generate(difficulty)
        input = ""
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("תרגיל ${(solvedCount + 1).coerceAtMost(total)} מתוך $total", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { solvedCount.toFloat() / total }, modifier = Modifier.fillMaxWidth())
        // Math reads left-to-right.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("${problem.text} = ?", fontSize = 38.sp, fontWeight = FontWeight.Bold)
                Text(
                    input.ifEmpty { " " },
                    fontSize = 34.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                NumberPad(
                    onDigit = { if (input.length < 6) input += it },
                    onBackspace = { input = input.dropLast(1) },
                    onSubmit = { submit() },
                    submitEnabled = input.isNotEmpty()
                )
            }
        }
        if (showWrong) {
            Text("תשובה שגויה — הנה תרגיל חדש", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun NumberPad(onDigit: (String) -> Unit, onBackspace: () -> Unit, onSubmit: () -> Unit, submitEnabled: Boolean) {
    val rows = listOf(listOf("1", "2", "3"), listOf("4", "5", "6"), listOf("7", "8", "9"))
    val keyMod = Modifier.height(60.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { d ->
                    FilledTonalButton(onClick = { onDigit(d) }, modifier = keyMod.weight(1f), shape = RoundedCornerShape(16.dp)) {
                        Text(d, fontSize = 24.sp)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilledTonalButton(onClick = onBackspace, modifier = keyMod.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "מחק ספרה")
            }
            FilledTonalButton(onClick = { onDigit("0") }, modifier = keyMod.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Text("0", fontSize = 24.sp)
            }
            Button(onClick = onSubmit, enabled = submitEnabled, modifier = keyMod.weight(1f), shape = RoundedCornerShape(16.dp)) {
                Text("✓", fontSize = 24.sp)
            }
        }
    }
}
