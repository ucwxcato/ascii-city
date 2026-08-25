package com.asciicity.content

internal object ContentIds {
    private val pattern = Regex("[a-z0-9][a-z0-9._-]*")

    fun requireStable(value: String, source: String, path: String): String {
        if (!value.matches(pattern)) {
            throw ContentValidationException(
                "$source:$path: '$value' must contain only lowercase letters, numbers, dots, underscores, or hyphens",
            )
        }
        return value
    }
}
