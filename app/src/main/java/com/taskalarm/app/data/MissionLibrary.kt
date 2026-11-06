package com.taskalarm.app.data

import kotlin.random.Random

/**
 * Get-out-of-bed missions. Every mission is built only from steps that any home allows,
 * that the phone's sensors can verify, and that can't realistically be done lying in bed:
 *  - turning on the light / opening the shutter (ambient light sensor),
 *  - walking (step detector),
 *  - jumping in place (free-fall followed by landing, phone held upright),
 *  - turning around in place (gyroscope, phone held upright).
 * Pure Kotlin so the whole library is unit tested.
 */
sealed interface MissionStep {
    /** Turn on the light or open the shutter: light must rise to [factor]× the starting level. */
    data class Light(val factor: Float = 3f) : MissionStep
    data class Walk(val steps: Int) : MissionStep
    data class Jumps(val count: Int) : MissionStep
    /** Full 360° turns while standing. */
    data class Spins(val count: Int) : MissionStep

    val short: String
        get() = when (this) {
            is Light -> "אור"
            is Walk -> "$steps צעדים"
            is Jumps -> "$count קפיצות"
            is Spins -> "$count סיבובים"
        }

    val description: String
        get() = when (this) {
            is Light -> "הדלק את האור בחדר או פתח את התריס"
            is Walk -> "לך $steps צעדים עם הטלפון"
            is Jumps -> "קפוץ $count פעמים במקום, עם הטלפון ביד"
            is Spins -> "הסתובב $count סיבובים שלמים במקום, עם הטלפון זקוף ביד"
        }

    /** Rough effort score used to grade difficulty. */
    val effort: Double
        get() = when (this) {
            is Light -> 1.0
            is Walk -> steps / 40.0
            is Jumps -> count / 5.0
            is Spins -> count * 0.8
        }
}

data class Mission(
    val id: String,
    val steps: List<MissionStep>,
) {
    val title: String = steps.joinToString(" · ") { it.short }
    val effort: Double = steps.sumOf { it.effort }
    val difficulty: Int = when {
        effort <= 3.5 -> MissionDifficulty.EASY
        effort <= 7.0 -> MissionDifficulty.MEDIUM
        else -> MissionDifficulty.HARD
    }
}

object MissionDifficulty {
    const val ANY = 0
    const val EASY = 1
    const val MEDIUM = 2
    const val HARD = 3
    fun name(level: Int) = when (level) {
        EASY -> "קל"
        MEDIUM -> "בינוני"
        HARD -> "קשה"
        else -> "הכול"
    }
}

object MissionLibrary {
    private fun light() = MissionStep.Light()
    private fun walk(n: Int) = MissionStep.Walk(n)
    private fun jumps(n: Int) = MissionStep.Jumps(n)
    private fun spins(n: Int) = MissionStep.Spins(n)

    private fun idOf(steps: List<MissionStep>) = steps.joinToString("-") {
        when (it) {
            is MissionStep.Light -> "light"
            is MissionStep.Walk -> "walk${it.steps}"
            is MissionStep.Jumps -> "jump${it.count}"
            is MissionStep.Spins -> "spin${it.count}"
        }
    }

    val all: List<Mission> = buildList {
        fun add(vararg steps: MissionStep) = add(Mission(idOf(steps.toList()), steps.toList()))

        // Single activity
        (30..300 step 10).forEach { add(walk(it)) }
        (5..40 step 5).forEach { add(jumps(it)) }
        (2..8).forEach { add(spins(it)) }
        // Light first, then one activity
        (30..200 step 10).forEach { add(light(), walk(it)) }
        (5..30 step 5).forEach { add(light(), jumps(it)) }
        (2..6).forEach { add(light(), spins(it)) }
        // Two activities
        for (w in listOf(30, 50, 80, 100, 150, 200)) for (j in listOf(5, 10, 15, 20, 25)) add(walk(w), jumps(j))
        for (w in listOf(30, 50, 80, 100, 150, 200)) for (s in listOf(2, 3, 4, 5)) add(walk(w), spins(s))
        for (j in listOf(5, 10, 15, 20, 25)) for (s in listOf(2, 3, 4, 5)) add(jumps(j), spins(s))
        // Light + two activities
        for (w in listOf(40, 60, 80, 100, 120, 150)) for (j in listOf(5, 10, 15, 20, 25)) add(light(), walk(w), jumps(j))
        for (w in listOf(40, 60, 80, 100, 120, 150)) for (s in listOf(2, 3, 4, 5)) add(light(), walk(w), spins(s))
        for (j in listOf(5, 10, 15, 20)) for (s in listOf(2, 3, 4)) add(light(), jumps(j), spins(s))
        // Three activities
        for (w in listOf(50, 100, 150, 200)) for (j in listOf(5, 10, 20)) for (s in listOf(2, 3, 4)) add(walk(w), jumps(j), spins(s))
        // Full morning routine
        for (w in listOf(50, 80, 100, 150, 200)) for (j in listOf(5, 10, 15, 20)) for (s in listOf(2, 3, 4)) {
            add(light(), walk(w), jumps(j), spins(s))
        }
    }

    private val byId = all.associateBy { it.id }

    fun get(id: String?): Mission? = id?.let { byId[it] }

    /** Emergency for a lost fixed barcode: still requires getting up. */
    val emergency = Mission("emergency", listOf(MissionStep.Light(), MissionStep.Walk(150)))

    fun random(
        difficulty: Int = MissionDifficulty.ANY,
        exclude: String? = null,
        random: Random = Random.Default,
    ): Mission {
        val pool = all.filter { (difficulty == MissionDifficulty.ANY || it.difficulty == difficulty) && it.id != exclude }
        return pool.randomOrNull(random) ?: all.random(random)
    }
}
