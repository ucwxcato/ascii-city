package com.asciicity.world

data class WorldAsset(
    val id: String,
    val assetId: String,
    val spriteId: String,
    val position: WorldPosition,
    val anchorHeight: Double,
    val yawRadians: Double,
    val scaleX: Double,
    val scaleY: Double,
    val scaleZ: Double,
    val blocking: Boolean,
    val collisionRadius: Double,
) {
    init {
        require(id.isNotBlank() && assetId.isNotBlank() && spriteId.isNotBlank()) {
            "World asset IDs must not be blank"
        }
        require(anchorHeight.isFinite() && yawRadians.isFinite()) { "World asset transform must be finite" }
        require(listOf(scaleX, scaleY, scaleZ).all { it > 0.0 && it <= 16.0 && it.isFinite() }) {
            "World asset scale must be positive, finite, and at most 16"
        }
        require(collisionRadius >= 0.0 && collisionRadius.isFinite()) {
            "World asset collision radius must be finite and non-negative"
        }
        require(!blocking || collisionRadius > 0.0) {
            "Blocking world assets must have a positive collision radius"
        }
    }
}
