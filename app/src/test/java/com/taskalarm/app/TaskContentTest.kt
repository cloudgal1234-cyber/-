package com.taskalarm.app

import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.MathDifficulty
import com.taskalarm.app.data.MathProblems
import com.taskalarm.app.data.TaskType
import com.taskalarm.app.data.TypingPhrases
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class TaskContentTest {

    /** Evaluates the generated text independently of the generator's own answer. */
    private fun evaluate(text: String): Int {
        val t = text.replace(" ", "")
        Regex("""^(\d+)\+(\d+)$""").find(t)?.let { return it.groupValues[1].toInt() + it.groupValues[2].toInt() }
        Regex("""^(\d+)−(\d+)$""").find(t)?.let { return it.groupValues[1].toInt() - it.groupValues[2].toInt() }
        Regex("""^(\d+)×(\d+)\+(\d+)$""").find(t)?.let {
            return it.groupValues[1].toInt() * it.groupValues[2].toInt() + it.groupValues[3].toInt()
        }
        error("Unexpected problem format: $text")
    }

    @Test fun mathAnswersAreCorrectForAllDifficulties() {
        val random = Random(42)
        for (difficulty in listOf(MathDifficulty.EASY, MathDifficulty.MEDIUM, MathDifficulty.HARD)) {
            repeat(200) {
                val p = MathProblems.generate(difficulty, random)
                assertEquals(p.text, evaluate(p.text), p.answer)
                assertTrue(p.answer > 0)
            }
        }
    }

    @Test fun typingIgnoresExtraWhitespaceOnly() {
        val phrase = "בוקר טוב לי ולכל העולם"
        assertTrue(TypingPhrases.matches("  בוקר  טוב לי ולכל העולם ", phrase))
        assertFalse(TypingPhrases.matches("בוקר טוב לי ולכל העולמ", phrase))
        assertFalse(TypingPhrases.matches("", phrase))
    }

    @Test fun typingPicksDistinctPhrases() {
        val picked = TypingPhrases.pick(5, Random(1))
        assertEquals(5, picked.size)
        assertEquals(5, picked.toSet().size)
    }

    @Test fun summaryDescribesEachTask() {
        val base = Alarm()
        assertEquals("50 צעדים", base.copy(taskType = TaskType.STEPS).taskSummary)
        assertEquals("3 תרגילים (בינוני)", base.copy(taskType = TaskType.MATH).taskSummary)
        assertEquals("30 ניעורים", base.copy(taskType = TaskType.SHAKE).taskSummary)
    }
}
