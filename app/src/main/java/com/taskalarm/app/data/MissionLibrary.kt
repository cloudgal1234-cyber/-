package com.taskalarm.app.data

import kotlin.random.Random

/**
 * Get-out-of-bed missions. Every mission is built only from steps the phone can verify
 * and that cannot realistically be done lying in bed:
 *  - scanning product barcodes that satisfy a rule (you have to go and look through products),
 *  - scanning several different products,
 *  - turning on the light / opening the curtains (ambient light sensor),
 *  - walking (step detector).
 * Pure Kotlin so the whole library is unit tested.
 */

/** A rule a scanned retail barcode (EAN/UPC digits) must satisfy. */
class ScanRule(
    val id: String,
    /** Short name used in mission titles, e.g. "סיומת 7". */
    val short: String,
    /** Completes the sentence "מצא מוצר …". */
    val text: String,
    /** Rough share of household products that satisfy the rule. */
    val probability: Double,
    private val predicate: (String) -> Boolean,
) {
    fun matches(digits: String): Boolean = digits.length >= 8 && predicate(digits)
}

sealed interface MissionStep {
    /** Turn on the light or open the curtains: light must rise to [factor]× the starting level. */
    data class Light(val factor: Float = 3f) : MissionStep
    data class Walk(val steps: Int) : MissionStep
    data class Scan(val rule: ScanRule) : MissionStep
    /** Scan [count] different products. */
    data class ScanDistinct(val count: Int) : MissionStep

    val needsCamera: Boolean get() = this is Scan || this is ScanDistinct

    val description: String
        get() = when (this) {
            is Light -> "הדלק את האור בחדר או פתח את התריס/הווילון"
            is Walk -> "לך $steps צעדים עם הטלפון"
            is Scan -> "מצא וסרוק מוצר ${rule.text}"
            is ScanDistinct -> "סרוק $count מוצרים שונים"
        }

    /** Rough effort score used to grade difficulty. */
    val effort: Double
        get() = when (this) {
            is Light -> 1.0
            is Walk -> steps / 40.0
            is Scan -> 1.0 + 0.3 / rule.probability
            is ScanDistinct -> count * 1.2
        }
}

data class Mission(
    val id: String,
    val title: String,
    val steps: List<MissionStep>,
) {
    val effort: Double = steps.sumOf { it.effort }
    val difficulty: Int = when {
        effort <= 3.0 -> MissionDifficulty.EASY
        effort <= 6.5 -> MissionDifficulty.MEDIUM
        else -> MissionDifficulty.HARD
    }
    val needsCamera: Boolean = steps.any { it.needsCamera }
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
    private fun d(digits: String, fromEnd: Int) = digits[digits.length - 1 - fromEnd].digitToInt()

    val endsWith: List<ScanRule> = (0..9).map { n ->
        ScanRule("end$n", "סיומת $n", "שהברקוד שלו מסתיים בספרה $n", 0.1) { d(it, 0) == n }
    }
    val secondToLast: List<ScanRule> = (0..9).map { n ->
        ScanRule("pen$n", "ספרה לפני אחרונה $n", "שהספרה הלפני־אחרונה בברקוד שלו היא $n", 0.1) { d(it, 1) == n }
    }
    val otherRules: List<ScanRule> = listOf(
        ScanRule("even", "סיומת זוגית", "שהברקוד שלו מסתיים בספרה זוגית", 0.5) { d(it, 0) % 2 == 0 },
        ScanRule("odd", "סיומת אי־זוגית", "שהברקוד שלו מסתיים בספרה אי־זוגית", 0.5) { d(it, 0) % 2 == 1 },
        ScanRule("high", "סיומת 7–9", "שהברקוד שלו מסתיים ב־7, 8 או 9", 0.3) { d(it, 0) >= 7 },
        ScanRule("low", "סיומת 0–2", "שהברקוד שלו מסתיים ב־0, 1 או 2", 0.3) { d(it, 0) <= 2 },
        ScanRule("sumeven", "סכום ספרות זוגי", "שסכום כל ספרות הברקוד שלו זוגי", 0.5) { s -> s.sumOf { it.digitToInt() } % 2 == 0 },
        ScanRule("sumodd", "סכום ספרות אי־זוגי", "שסכום כל ספרות הברקוד שלו אי־זוגי", 0.5) { s -> s.sumOf { it.digitToInt() } % 2 == 1 },
        ScanRule("twin", "סיומת כפולה", "ששתי הספרות האחרונות בברקוד שלו זהות", 0.1) { d(it, 0) == d(it, 1) },
        ScanRule("il", "מוצר ישראלי", "ישראלי (ברקוד שמתחיל ב־729)", 0.5) { it.length == 13 && it.startsWith("729") },
        ScanRule("import", "מוצר מיובא", "מיובא (ברקוד שלא מתחיל ב־729)", 0.4) { !it.startsWith("729") },
        ScanRule("short", "ברקוד קצר", "עם ברקוד קצר של 8 ספרות", 0.05) { it.length == 8 },
    )
    val allRules: List<ScanRule> = endsWith + secondToLast + otherRules

    val all: List<Mission> = buildList {
        // Barcode hunts
        allRules.forEach { add(Mission("scan-${it.id}", "ציד ברקוד: ${it.short}", listOf(MissionStep.Scan(it)))) }
        // Several different products
        (2..6).forEach { add(Mission("distinct-$it", "$it מוצרים שונים", listOf(MissionStep.ScanDistinct(it)))) }
        // Light, then hunt
        allRules.forEach {
            add(Mission("light-scan-${it.id}", "אור ואז ציד: ${it.short}", listOf(MissionStep.Light(), MissionStep.Scan(it))))
        }
        // Walk, then hunt
        listOf(30, 60, 100).forEach { steps ->
            allRules.forEach {
                add(
                    Mission(
                        "walk$steps-scan-${it.id}", "$steps צעדים ואז ציד: ${it.short}",
                        listOf(MissionStep.Walk(steps), MissionStep.Scan(it))
                    )
                )
            }
        }
        // Two endings, two different products
        for (a in 0..9) for (b in a + 1..9) {
            add(
                Mission(
                    "pair-end$a-end$b", "ציד כפול: סיומות $a ו־$b",
                    listOf(MissionStep.Scan(endsWith[a]), MissionStep.Scan(endsWith[b]))
                )
            )
        }
        // Ending + second-to-last digit, two different products
        for (a in 0..9) for (b in 0..9) if (a != b) {
            add(
                Mission(
                    "pair-end$a-pen$b", "ציד כפול: סיומת $a וספרה לפני אחרונה $b",
                    listOf(MissionStep.Scan(endsWith[a]), MissionStep.Scan(secondToLast[b]))
                )
            )
        }
        // Light + walk + several products
        listOf(40, 80, 120).forEach { steps ->
            (2..4).forEach { n ->
                add(
                    Mission(
                        "light-walk$steps-distinct$n", "אור, $steps צעדים ו־$n מוצרים",
                        listOf(MissionStep.Light(), MissionStep.Walk(steps), MissionStep.ScanDistinct(n))
                    )
                )
            }
        }
        // No camera needed
        listOf(100, 150, 200, 250, 300).forEach {
            add(Mission("walk$it", "הליכת בוקר: $it צעדים", listOf(MissionStep.Walk(it))))
        }
        listOf(60, 100, 150, 200, 250).forEach {
            add(Mission("light-walk$it", "אור ו־$it צעדים", listOf(MissionStep.Light(), MissionStep.Walk(it))))
        }
        // Full morning routine
        allRules.forEach {
            add(
                Mission(
                    "routine-${it.id}", "שגרת בוקר: אור, 50 צעדים וציד ${it.short}",
                    listOf(MissionStep.Light(), MissionStep.Walk(50), MissionStep.Scan(it))
                )
            )
        }
    }

    private val byId = all.associateBy { it.id }

    fun get(id: String?): Mission? = id?.let { byId[it] }

    /** Emergency for a lost registered barcode: still requires going to the kitchen. */
    val emergency = Mission("emergency", "משימת חירום: 3 מוצרים שונים", listOf(MissionStep.ScanDistinct(3)))

    /** Emergency when the camera can't be used. */
    val emergencyNoCamera = Mission("emergency-walk", "משימת חירום: אור ו־150 צעדים", listOf(MissionStep.Light(), MissionStep.Walk(150)))

    fun random(
        difficulty: Int = MissionDifficulty.ANY,
        cameraAvailable: Boolean = true,
        exclude: String? = null,
        random: Random = Random.Default,
    ): Mission {
        val pool = all.filter {
            (difficulty == MissionDifficulty.ANY || it.difficulty == difficulty) &&
                (cameraAvailable || !it.needsCamera) && it.id != exclude
        }
        return pool.randomOrNull(random) ?: all.filter { cameraAvailable || !it.needsCamera }.random(random)
    }
}
