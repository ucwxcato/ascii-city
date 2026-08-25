package com.asciicity.desktop

data class RequestedWindowSize(
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0) { "Window width must be positive" }
        require(height > 0) { "Window height must be positive" }
    }

    companion object {
        private const val ARGUMENT_PREFIX = "--window-size="

        fun from(arguments: Array<String>): RequestedWindowSize? {
            val value = arguments
                .firstOrNull { it.startsWith(ARGUMENT_PREFIX) }
                ?.removePrefix(ARGUMENT_PREFIX)
                ?: return null
            val dimensions = value.lowercase().split('x')
            require(dimensions.size == 2) {
                "Window size must use --window-size=WIDTHxHEIGHT"
            }

            return RequestedWindowSize(
                width = dimensions[0].toInt(),
                height = dimensions[1].toInt(),
            )
        }
    }
}
