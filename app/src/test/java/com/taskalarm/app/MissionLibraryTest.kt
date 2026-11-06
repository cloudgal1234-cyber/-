package com.taskalarm.app

import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.MissionDifficulty
import com.taskalarm.app.data.MissionLibrary
import com.taskalarm.app.data.MissionStep
import com.taskalarm.app.data.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MissionLibraryTest {
    private val all = MissionLibrary.all

    @Test fun moreThan300Missions() {
        assertTrue("only ${all.size} missions", all.size > 300)
    }

    @Test fun idsAndTitlesAreUnique() {
        assertEquals(all.size, all.map { it.id }.toSet().size)
        assertEquals(all.size, all.map { it.title }.toSet().size)
    }

    @Test fun everyDifficultyIsWellStocked() {
        for (level in listOf(MissionDifficulty.EASY, MissionDifficulty.MEDIUM, MissionDifficulty.HARD)) {
            assertTrue("few missions at level $level", all.count { it.difficulty == level } >= 30)
        }
    }

    @Test fun everyMissionIsFeasible() {
        all.forEach { m ->
            assertTrue(m.steps.isNotEmpty() && m.steps.size <= 4)
            m.steps.forEach { step ->
                when (step) {
                    is MissionStep.Walk -> assertTrue(step.steps in 30..300)
                    is MissionStep.Jumps -> assertTrue(step.count in 5..40)
                    is MissionStep.Spins -> assertTrue(step.count in 2..8)
                    is MissionStep.Light -> Unit
                }
            }
            // A light step, if present, always comes first (you turn on the light before moving around).
            val lightIndex = m.steps.indexOfFirst { it is MissionStep.Light }
            assertTrue(lightIndex <= 0)
        }
    }

    @Test fun removedBarcodeMissionIdsAreIgnored() {
        assertNull(MissionLibrary.get("scan-end7"))
        assertEquals("משימה אקראית", Alarm(missionId = "scan-end7").taskSummary)
    }

    @Test fun randomRespectsFilters() {
        val random = Random(3)
        repeat(300) {
            val hard = MissionLibrary.random(MissionDifficulty.HARD, random = random)
            assertEquals(MissionDifficulty.HARD, hard.difficulty)
            val other = MissionLibrary.random(MissionDifficulty.HARD, exclude = hard.id, random = random)
            assertNotEquals(hard.id, other.id)
        }
    }

    @Test fun summaries() {
        assertEquals("משימה אקראית", Alarm().taskSummary)
        assertEquals("משימה אקראית (קשה)", Alarm(missionDifficulty = MissionDifficulty.HARD).taskSummary)
        assertEquals("אור · 80 צעדים · 10 קפיצות · 3 סיבובים", Alarm(missionId = "light-walk80-jump10-spin3").taskSummary)
        assertEquals("120 צעדים", Alarm(taskType = TaskType.STEPS, stepCount = 120).taskSummary)
    }
}
