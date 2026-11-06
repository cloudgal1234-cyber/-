package com.taskalarm.app.ui.task

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.taskalarm.app.data.TypingPhrases

/** Type [count] sentences exactly. The sentence is plain (non-selectable) text, so it can't be copied. */
@Composable
fun TypingTask(count: Int, onComplete: () -> Unit) {
    val phrases = remember { TypingPhrases.pick(count) }
    var index by remember { mutableIntStateOf(0) }
    var typed by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    val done by rememberUpdatedState(onComplete)
    val phrase = phrases[index.coerceAtMost(phrases.lastIndex)]

    fun submit() {
        if (TypingPhrases.matches(typed, phrase)) {
            error = false
            typed = ""
            if (index + 1 >= phrases.size) done() else index++
        } else {
            error = true
        }
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("משפט ${index + 1} מתוך ${phrases.size}", style = MaterialTheme.typography.titleMedium)
        LinearProgressIndicator(progress = { index.toFloat() / phrases.size }, modifier = Modifier.fillMaxWidth())
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                phrase,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(16.dp)
            )
        }
        OutlinedTextField(
            value = typed,
            onValueChange = { typed = it; error = false },
            label = { Text("הקלד את המשפט בדיוק") },
            isError = error,
            supportingText = { if (error) Text("יש הבדל — בדוק כל אות") },
            keyboardOptions = KeyboardOptions(
                autoCorrectEnabled = false,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { submit() }),
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { submit() }, enabled = typed.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
            Text("אישור")
        }
    }
}

