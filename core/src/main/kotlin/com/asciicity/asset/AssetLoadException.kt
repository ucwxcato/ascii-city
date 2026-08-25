package com.asciicity.asset

class AssetLoadException(
    val userMessage: String,
    cause: Throwable,
) : RuntimeException(userMessage, cause)
