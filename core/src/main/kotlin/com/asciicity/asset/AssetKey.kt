package com.asciicity.asset

data class AssetKey<T : Any>(
    val path: String,
    val type: Class<T>,
) {
    init {
        require(path.isNotBlank()) { "Asset path must not be blank" }
    }
}
