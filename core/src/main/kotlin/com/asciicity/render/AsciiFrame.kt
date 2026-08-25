package com.asciicity.render

class AsciiFrame(
    val width: Int,
    val height: Int,
) {
    private val glyphs: IntArray
    private val foregroundColors: IntArray
    private val backgroundColors: IntArray
    private val depths: FloatArray
    private val emissiveStrengths: FloatArray

    val cellCount: Int
        get() = glyphs.size

    init {
        require(width > 0) { "ASCII frame width must be positive" }
        require(height > 0) { "ASCII frame height must be positive" }
        val size = Math.multiplyExact(width, height)
        glyphs = IntArray(size)
        foregroundColors = IntArray(size)
        backgroundColors = IntArray(size)
        depths = FloatArray(size)
        emissiveStrengths = FloatArray(size)
        clear()
    }

    fun clear(
        glyph: Int = ' '.code,
        foreground: Int = 0xFFFFFFFF.toInt(),
        background: Int = 0x000000FF,
        depth: Float = Float.POSITIVE_INFINITY,
        emissive: Float = 0f,
    ) {
        require(!depth.isNaN()) { "ASCII frame depth must not be NaN" }
        require(emissive.isFinite() && emissive in 0f..1f) { "ASCII frame emissive strength must be between zero and one" }
        glyphs.fill(glyph)
        foregroundColors.fill(foreground)
        backgroundColors.fill(background)
        depths.fill(depth)
        emissiveStrengths.fill(emissive)
    }

    fun set(
        x: Int,
        y: Int,
        glyph: Int,
        foreground: Int,
        background: Int,
        depth: Float,
        emissive: Float = 0f,
    ) {
        require(!depth.isNaN()) { "ASCII frame depth must not be NaN" }
        require(emissive.isFinite() && emissive in 0f..1f) { "ASCII frame emissive strength must be between zero and one" }
        val index = indexOf(x, y)
        setAtIndex(index, glyph, foreground, background, depth, emissive)
    }

    fun writeIfNearer(
        x: Int,
        y: Int,
        glyph: Int,
        foreground: Int,
        background: Int,
        depth: Float,
        emissive: Float = 0f,
    ): Boolean {
        require(!depth.isNaN()) { "ASCII frame depth must not be NaN" }
        require(emissive.isFinite() && emissive in 0f..1f) { "ASCII frame emissive strength must be between zero and one" }
        val index = indexOf(x, y)
        if (depth >= depths[index]) {
            return false
        }
        setAtIndex(index, glyph, foreground, background, depth, emissive)
        return true
    }

    fun glyphAt(x: Int, y: Int): Int = glyphs[indexOf(x, y)]

    fun foregroundAt(x: Int, y: Int): Int = foregroundColors[indexOf(x, y)]

    fun backgroundAt(x: Int, y: Int): Int = backgroundColors[indexOf(x, y)]

    fun depthAt(x: Int, y: Int): Float = depths[indexOf(x, y)]

    fun emissiveAt(x: Int, y: Int): Float = emissiveStrengths[indexOf(x, y)]

    internal fun glyphAtIndex(index: Int): Int = glyphs[index]

    internal fun foregroundAtIndex(index: Int): Int = foregroundColors[index]

    internal fun backgroundAtIndex(index: Int): Int = backgroundColors[index]

    internal fun depthAtIndex(index: Int): Float = depths[index]

    internal fun emissiveAtIndex(index: Int): Float = emissiveStrengths[index]

    /** Presentation-only mutation used by depth-aware local lighting after frame geometry resolves. */
    internal fun setBackgroundAtIndex(index: Int, background: Int) {
        backgroundColors[index] = background
    }

    private fun setAtIndex(
        index: Int,
        glyph: Int,
        foreground: Int,
        background: Int,
        depth: Float,
        emissive: Float,
    ) {
        glyphs[index] = glyph
        foregroundColors[index] = foreground
        backgroundColors[index] = background
        depths[index] = depth
        emissiveStrengths[index] = emissive
    }

    private fun indexOf(x: Int, y: Int): Int {
        require(x in 0 until width) { "ASCII frame x=$x is outside 0 until $width" }
        require(y in 0 until height) { "ASCII frame y=$y is outside 0 until $height" }
        return y * width + x
    }
}
