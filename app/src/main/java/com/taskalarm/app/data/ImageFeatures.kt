package com.taskalarm.app.data

import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Compact on-device description of a photo, used to check that the live camera shows the
 * same place as the registered reference:
 *  - [labels]: ML Kit image-labeling confidences ("Refrigerator" → 0.8 …),
 *  - [hist]: colour histograms of the whole frame and of its four quadrants (layout + colour).
 * Pure Kotlin (no Android types) so the matching maths is unit tested.
 */
class ImageFeatures(val labels: Map<String, Float>, val hist: FloatArray) {

    fun serialize(): String =
        "L:" + labels.entries.joinToString("|") { "${it.key.replace("|", " ").replace("=", " ")}=${it.value}" } +
            "\nH:" + hist.joinToString(",")

    companion object {
        const val BINS = 28 // 12 hues × 2 saturations + 4 greys
        const val REGIONS = 5 // whole frame + 4 quadrants
        private const val SEPARATOR = "\n#\n"

        fun parse(text: String): ImageFeatures? = runCatching {
            val lines = text.lines()
            val l = lines.first { it.startsWith("L:") }.removePrefix("L:")
            val h = lines.first { it.startsWith("H:") }.removePrefix("H:")
            val labels = if (l.isBlank()) emptyMap() else l.split("|").associate {
                val (k, v) = it.split("=")
                k to v.toFloat()
            }
            ImageFeatures(labels, h.split(",").map { it.toFloat() }.toFloatArray())
        }.getOrNull()

        fun serializeAll(list: List<ImageFeatures>) = list.joinToString(SEPARATOR) { it.serialize() }
        fun parseAll(text: String): List<ImageFeatures> = text.split(SEPARATOR).mapNotNull { parse(it) }

        /** Histograms for an ARGB pixel array of size [w]×[h]. */
        fun histogram(pixels: IntArray, w: Int, h: Int): FloatArray {
            val out = FloatArray(BINS * REGIONS)
            for (y in 0 until h) for (x in 0 until w) {
                val bin = colorBin(pixels[y * w + x])
                out[bin] += 1f
                val quadrant = 1 + (if (y >= h / 2) 2 else 0) + (if (x >= w / 2) 1 else 0)
                out[quadrant * BINS + bin] += 1f
            }
            for (r in 0 until REGIONS) {
                var sum = 0f
                for (b in 0 until BINS) sum += out[r * BINS + b]
                if (sum > 0) for (b in 0 until BINS) out[r * BINS + b] /= sum
            }
            return out
        }

        private fun colorBin(argb: Int): Int {
            val r = (argb shr 16 and 0xFF) / 255f
            val g = (argb shr 8 and 0xFF) / 255f
            val b = (argb and 0xFF) / 255f
            val mx = max(r, max(g, b))
            val mn = min(r, min(g, b))
            val sat = if (mx == 0f) 0f else (mx - mn) / mx
            if (sat < 0.18f || mx < 0.12f) {
                return 24 + min(3, (mx * 4).toInt()) // grey levels
            }
            val d = mx - mn
            var hue = when (mx) {
                r -> ((g - b) / d) % 6f
                g -> (b - r) / d + 2f
                else -> (r - g) / d + 4f
            } * 60f
            if (hue < 0) hue += 360f
            val hueBin = min(11, (hue / 30f).toInt())
            return hueBin * 2 + if (sat < 0.5f) 0 else 1
        }

        /** 0..1, how likely [a] and [b] show the same place. */
        fun similarity(a: ImageFeatures, b: ImageFeatures): Double {
            var histSim = 0.0
            val regions = min(a.hist.size, b.hist.size) / BINS
            for (r in 0 until regions) {
                var inter = 0.0
                for (i in 0 until BINS) inter += min(a.hist[r * BINS + i], b.hist[r * BINS + i])
                histSim += inter
            }
            if (regions > 0) histSim /= regions

            if (a.labels.isEmpty() || b.labels.isEmpty()) return histSim * 0.9
            var dot = 0.0
            a.labels.forEach { (k, v) -> b.labels[k]?.let { dot += v * it } }
            val na = sqrt(a.labels.values.sumOf { (it * it).toDouble() })
            val nb = sqrt(b.labels.values.sumOf { (it * it).toDouble() })
            val labelSim = if (na == 0.0 || nb == 0.0) 0.0 else dot / (na * nb)
            return 0.55 * labelSim + 0.45 * histSim
        }

        /** Best similarity of [live] to any of the [references]. */
        fun bestMatch(live: ImageFeatures, references: List<ImageFeatures>): Double =
            references.maxOfOrNull { similarity(live, it) } ?: 0.0

        /**
         * Per-place pass mark from two registration shots of the same place: a bit below how
         * similar they were to each other, within sane bounds.
         */
        fun threshold(selfSimilarity: Double): Float = (selfSimilarity * 0.85).coerceIn(0.45, 0.85).toFloat()
    }
}
