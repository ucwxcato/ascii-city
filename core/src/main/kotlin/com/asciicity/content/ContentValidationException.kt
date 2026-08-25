package com.asciicity.content

class ContentValidationException(
    message: String,
    cause: Throwable? = null,
) : IllegalArgumentException(message, cause)
