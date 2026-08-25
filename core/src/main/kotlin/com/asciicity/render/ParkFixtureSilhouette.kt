package com.asciicity.render

internal object ParkFixtureSilhouette {
    enum class Part(val glyph: Int?, val paletteKey: String? = null, val emissiveStrength: Float = 0f) {
        BENCH_SLAT('='.code),
        BENCH_LEG('|'.code),
        BIN('#'.code),
        FOUNTAIN_STONE('O'.code),
        FOUNTAIN_WATER('~'.code),
        PAVILION_POST('|'.code),
        PAVILION_ROOF('^'.code),
        TENT_CANVAS('/'.code),
        TENT_RIDGE('^'.code),
        WATERFALL_FLOW(null),
        WATERFALL_ROCK('#'.code, PaletteKeys.PARK_STONE),
        WATERFALL_SPRAY('~'.code, PaletteKeys.PARK_WATER),
        SLOT_CABINET('#'.code, "casinoCabinet"),
        SLOT_FRAME('|'.code, "casinoCabinet"),
        SLOT_REEL(null),
        SLOT_BUTTON('o'.code, "neonRed", 0.7f),
        SLOT_MARQUEE('$'.code, "casinoLight", 0.85f),
        SLOT_TOP_LIGHT('*'.code, "neonCyan", 1.0f),
    }

    fun partAt(tags: Set<String>, faceCoordinate: Double, vertical: Double, height: Double): Part? {
        if (vertical < 0.0 || vertical > height) return null
        return when {
            BENCH_TAG in tags -> benchPart(faceCoordinate, vertical)
            BIN_TAG in tags -> binPart(faceCoordinate, vertical)
            FOUNTAIN_TAG in tags -> fountainPart(faceCoordinate, vertical)
            PAVILION_TAG in tags -> pavilionPart(faceCoordinate, vertical, height)
            TENT_TAG in tags -> tentPart(faceCoordinate, vertical, height)
            WATERFALL_TAG in tags -> waterfallPart(faceCoordinate, vertical, height)
            SLOT_MACHINE_TAG in tags -> slotMachinePart(faceCoordinate, vertical, height)
            else -> null
        }
    }

    private fun benchPart(horizontal: Double, vertical: Double): Part? = when {
        vertical in 0.58..0.92 && horizontal in 0.05..0.95 -> Part.BENCH_SLAT
        vertical in 0.38..0.56 && horizontal in 0.05..0.95 -> Part.BENCH_SLAT
        vertical <= 0.38 && (horizontal in 0.16..0.29 || horizontal in 0.71..0.84) -> Part.BENCH_LEG
        else -> null
    }

    private fun binPart(horizontal: Double, vertical: Double): Part? = when {
        vertical >= 0.80 && horizontal in 0.18..0.82 -> Part.BIN
        vertical >= 0.08 && horizontal in 0.25..0.75 -> Part.BIN
        else -> null
    }

    private fun fountainPart(horizontal: Double, vertical: Double): Part? = when {
        vertical >= 1.28 && horizontal in 0.46..0.54 -> Part.FOUNTAIN_WATER
        vertical in 1.00..1.28 && horizontal in 0.16..0.84 -> Part.FOUNTAIN_WATER
        vertical in 0.30..1.00 && horizontal in 0.39..0.61 -> Part.FOUNTAIN_STONE
        vertical <= 0.30 && horizontal in 0.08..0.92 -> Part.FOUNTAIN_STONE
        else -> null
    }

    private fun pavilionPart(horizontal: Double, vertical: Double, height: Double): Part? = when {
        vertical >= height * 0.72 -> Part.PAVILION_ROOF
        horizontal <= 0.13 || horizontal >= 0.87 -> Part.PAVILION_POST
        else -> null
    }

    private fun tentPart(horizontal: Double, vertical: Double, height: Double): Part? {
        val normalized = vertical / height
        val distanceFromCenter = kotlin.math.abs(horizontal - 0.5)
        if (normalized >= 0.90 && distanceFromCenter <= 0.06) return Part.TENT_RIDGE
        val canvasHalfWidth = (1.0 - normalized) * 0.48
        if (distanceFromCenter > canvasHalfWidth) return null
        if (normalized <= 0.55 && distanceFromCenter <= 0.11) return null
        return Part.TENT_CANVAS
    }

    private fun waterfallPart(horizontal: Double, vertical: Double, height: Double): Part? = when {
        horizontal <= 0.14 || horizontal >= 0.86 -> Part.WATERFALL_ROCK
        vertical <= height * 0.12 -> Part.WATERFALL_SPRAY
        else -> Part.WATERFALL_FLOW
    }

    private fun slotMachinePart(horizontal: Double, vertical: Double, height: Double): Part? {
        val normalized = vertical / height
        if (horizontal !in 0.08..0.92) return null
        return when {
            normalized <= 0.10 && (horizontal in 0.18..0.34 || horizontal in 0.66..0.82) -> Part.SLOT_CABINET
            normalized <= 0.10 -> null
            normalized <= 0.43 && normalized in 0.31..0.38 && horizontal in 0.61..0.75 -> Part.SLOT_BUTTON
            normalized <= 0.43 && horizontal in 0.14..0.86 -> Part.SLOT_CABINET
            normalized <= 0.76 && (horizontal <= 0.18 || horizontal >= 0.82) -> Part.SLOT_FRAME
            normalized <= 0.76 && horizontal in 0.18..0.82 -> Part.SLOT_REEL
            normalized <= 0.94 && horizontal in 0.16..0.84 -> Part.SLOT_MARQUEE
            horizontal in 0.40..0.60 -> Part.SLOT_TOP_LIGHT
            else -> null
        }
    }

    private const val BENCH_TAG = "bench"
    private const val BIN_TAG = "bin"
    private const val FOUNTAIN_TAG = "fountain"
    private const val PAVILION_TAG = "pavilion"
    private const val TENT_TAG = "tent"
    private const val WATERFALL_TAG = "waterfall"
    private const val SLOT_MACHINE_TAG = "slot-machine"
}
