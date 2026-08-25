package com.asciicity.content

object MapCatalog {
    const val DEFAULT_MAP_ID = "city-alpha"

    val playableMapIds: List<String> = listOf(
        DEFAULT_MAP_ID,
        "water-park",
        "nature-reserve",
        "casino",
    )

    fun nextAfter(currentMapId: String): String? {
        val currentIndex = playableMapIds.indexOf(currentMapId)
        require(currentIndex >= 0) { "Unknown playable map '$currentMapId'" }
        if (playableMapIds.size < 2) return null
        return playableMapIds[(currentIndex + 1) % playableMapIds.size]
    }
}
