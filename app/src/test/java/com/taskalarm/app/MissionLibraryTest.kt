package com.taskalarm.app

import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.MissionDifficulty
import com.taskalarm.app.data.MissionLibrary
import com.taskalarm.app.data.MissionStep
import com.taskalarm.app.data.TaskType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
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

    @Test fun everyMissionHasStepsAndEveryDifficultyIsUsed() {
        assertTrue(all.all { it.steps.isNotEmpty() })
        for (level in listOf(MissionDifficulty.EASY, MissionDifficulty.MEDIUM, MissionDifficulty.HARD)) {
            assertTrue("no missions at level $level", all.count { it.difficulty == level } >= 20)
        }
    }

    @Test fun everyRuleIsSatisfiableByRealBarcodes() {
        // Deterministic pseudo-random EAN-13 codes; each rule must accept at least one.
        val random = Random(7)
        val samples = List(5000) { i ->
            if (i % 3 == 0) "729" + List(10) { random.nextInt(10) }.joinToString("")
            else if (i % 50 == 1) List(8) { random.nextInt(10) }.joinToString("")
            else List(13) { random.nextInt(10) }.joinToString("")
        }
        MissionLibrary.allRules.forEach { rule ->
            assertTrue("rule ${rule.id} never matches", samples.any { rule.matches(it) })
            assertFalse("rule ${rule.id} always matches", samples.all { rule.matches(it) })
        }
    }

    @Test fun ruleExamples() {
        val milk = "7290004131074" // ends with 4, second-to-last 7, Israeli
        assertTrue(MissionLibrary.endsWith[4].matches(milk))
        assertFalse(MissionLibrary.endsWith[3].matches(milk))
        assertTrue(MissionLibrary.secondToLast[7].matches(milk))
        assertTrue(MissionLibrary.otherRules.first { it.id == "il" }.matches(milk))
        assertFalse(MissionLibrary.otherRules.first { it.id == "import" }.matches(milk))
        assertTrue(MissionLibrary.otherRules.first { it.id == "short" }.matches("96385074"))
        assertFalse("too short to be a product barcode", MissionLibrary.endsWith[4].matches("1234"))
    }

    @Test fun pairMissionsNeedTwoDifferentRules() {
        all.filter { it.id.startsWith("pair-") }.forEach { m ->
            val rules = m.steps.filterIsInstance<MissionStep.Scan>().map { it.rule.id }
            assertEquals(2, rules.size)
            assertNotEquals(rules[0], rules[1])
        }
    }

    @Test fun randomRespectsFilters() {
        val random = Random(3)
        repeat(300) {
            val noCam = MissionLibrary.random(cameraAvailable = false, random = random)
            assertFalse(noCam.needsCamera)
            val hard = MissionLibrary.random(MissionDifficulty.HARD, random = random)
            assertEquals(MissionDifficulty.HARD, hard.difficulty)
            val other = MissionLibrary.random(exclude = hard.id, difficulty = MissionDifficulty.HARD, random = random)
            assertNotEquals(hard.id, other.id)
        }
    }

    @Test fun summaries() {
        assertEquals("משימה אקראית", Alarm().taskSummary)
        assertEquals("משימה אקראית (קשה)", Alarm(missionDifficulty = MissionDifficulty.HARD).taskSummary)
        assertEquals("ציד ברקוד: סיומת 7", Alarm(missionId = "scan-end7").taskSummary)
        assertEquals("120 צעדים", Alarm(taskType = TaskType.STEPS, stepCount = 120).taskSummary)
    }
}
