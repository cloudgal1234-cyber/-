package com.taskalarm.app.data

import kotlin.random.Random

data class MathProblem(val text: String, val answer: Int)

/** Generates arithmetic problems. Pure Kotlin so it can be unit tested. */
object MathProblems {
    fun generate(difficulty: Int, random: Random = Random.Default): MathProblem = when (difficulty) {
        MathDifficulty.EASY -> {
            val a = random.nextInt(10, 50)
            val b = random.nextInt(10, 50)
            if (random.nextBoolean()) MathProblem("$a + $b", a + b)
            else MathProblem("${a + b} − $b", a)
        }
        MathDifficulty.HARD -> {
            val a = random.nextInt(12, 30)
            val b = random.nextInt(12, 30)
            val c = random.nextInt(10, 100)
            MathProblem("$a × $b + $c", a * b + c)
        }
        else -> {
            val a = random.nextInt(11, 40)
            val b = random.nextInt(3, 10)
            val c = random.nextInt(10, 60)
            MathProblem("$a × $b + $c", a * b + c)
        }
    }
}

/** Sentences for the typing task. */
object TypingPhrases {
    val all = listOf(
        "אני ער ומוכן ליום חדש",
        "בוקר טוב לי ולכל העולם",
        "היום אני קם בזמן ולא חוזר למיטה",
        "כוס מים קרים ואני בדרך",
        "כל יום הוא הזדמנות חדשה",
        "אני שולט בבוקר שלי",
        "עוד חמש דקות זה לא תוכנית",
        "קמתי, התלבשתי ויצאתי לדרך",
        "השמש כבר בחוץ וגם אני",
        "הצעד הראשון הוא לקום מהמיטה",
    )

    fun pick(count: Int, random: Random = Random.Default): List<String> =
        all.shuffled(random).take(count.coerceIn(1, all.size))

    /** Typed text matches if it equals the phrase, ignoring surrounding/duplicate whitespace. */
    fun matches(typed: String, phrase: String): Boolean = normalize(typed) == normalize(phrase)

    private fun normalize(s: String) = s.trim().replace(Regex("\\s+"), " ")
}
