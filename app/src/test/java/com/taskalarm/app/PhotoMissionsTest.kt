package com.taskalarm.app

import com.taskalarm.app.data.Alarm
import com.taskalarm.app.data.ImageFeatures
import com.taskalarm.app.data.MissionDifficulty
import com.taskalarm.app.data.PhotoCatalog
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PhotoMissionsTest {

    @Test fun moreThan300Missions() {
        assertTrue(PhotoCatalog.missions.size > 300)
        assertEquals(PhotoCatalog.missions.size, PhotoCatalog.missions.map { it.id }.toSet().size)
        assertEquals(PhotoCatalog.objects.size, PhotoCatalog.objects.map { it.key }.toSet().size)
    }

    @Test fun includesTheObviousOnes() {
        assertNotNull(PhotoCatalog.get("fridge"))
        assertNotNull(PhotoCatalog.get("light_switch"))
        assertEquals("צלם את המקרר ואת מתג התאורה", PhotoCatalog.mission("photo:fridge+light_switch")?.title)
    }

    @Test fun difficulty() {
        assertEquals(MissionDifficulty.EASY, PhotoCatalog.mission("photo:fridge")!!.difficulty)
        assertEquals(MissionDifficulty.MEDIUM, PhotoCatalog.mission("photo:fridge+kettle")!!.difficulty)
        assertEquals(MissionDifficulty.HARD, PhotoCatalog.mission("photo:fridge+toilet")!!.difficulty)
    }

    @Test fun onlyRegisteredPlacesAreUsed() {
        val registered = setOf("fridge", "light_switch", "kitchen_sink")
        val eligible = PhotoCatalog.eligible(registered)
        assertEquals(3 + 3, eligible.size) // 3 singles + 3 pairs
        val random = Random(1)
        repeat(100) {
            val m = PhotoCatalog.random(registered, random = random)!!
            assertTrue(m.objects.all { it.key in registered })
        }
        assertNull(PhotoCatalog.random(emptySet()))
    }

    @Test fun swapNeverReturnsTheSameMission() {
        val registered = setOf("fridge", "light_switch")
        repeat(50) {
            val first = PhotoCatalog.random(registered)!!
            val second = PhotoCatalog.random(registered, first.difficulty, exclude = first.id)
            if (second != null) assertNotEquals(first.id, second.id)
        }
        // A single place: nothing else to swap to.
        assertNull(PhotoCatalog.random(setOf("fridge"), exclude = "photo:fridge"))
    }

    private fun solid(argb: Int) = IntArray(48 * 48) { argb }

    @Test fun identicalImagesMatchAndDifferentOnesDoNot() {
        val white = ImageFeatures(mapOf("Refrigerator" to 0.8f), ImageFeatures.histogram(solid(0xFFF0F0F0.toInt()), 48, 48))
        val white2 = ImageFeatures(mapOf("Refrigerator" to 0.7f), ImageFeatures.histogram(solid(0xFFEAEAEA.toInt()), 48, 48))
        val green = ImageFeatures(mapOf("Plant" to 0.9f), ImageFeatures.histogram(solid(0xFF2E8B2E.toInt()), 48, 48))
        assertTrue(ImageFeatures.similarity(white, white) > 0.99)
        assertTrue(ImageFeatures.similarity(white, white2) > 0.9)
        assertTrue(ImageFeatures.similarity(white, green) < 0.1)
    }

    @Test fun featuresSurviveSerialization() {
        val f = ImageFeatures(mapOf("Light switch" to 0.6f, "Wall" to 0.4f), ImageFeatures.histogram(solid(0xFF336699.toInt()), 48, 48))
        val back = ImageFeatures.parseAll(ImageFeatures.serializeAll(listOf(f, f)))
        assertEquals(2, back.size)
        assertEquals(f.labels, back[0].labels)
        assertTrue(ImageFeatures.similarity(f, back[1]) > 0.999)
    }

    @Test fun thresholdIsBounded() {
        assertEquals(0.45f, ImageFeatures.threshold(0.2), 0.001f)
        assertEquals(0.85f, ImageFeatures.threshold(1.0), 0.001f)
    }

    @Test fun summaries() {
        assertEquals("צילום אקראי", Alarm().taskSummary)
        assertEquals("צילום אקראי (קשה)", Alarm(missionDifficulty = MissionDifficulty.HARD).taskSummary)
        assertEquals("צלם את המקרר", Alarm(missionId = "photo:fridge").taskSummary)
    }
}
