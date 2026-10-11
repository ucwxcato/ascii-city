package com.asciicity.render

import com.asciicity.content.AsciiSpriteRegistry
import com.asciicity.world.WorldAsset

class WorldAssetRenderer(
    assets: Collection<WorldAsset>,
    sprites: AsciiSpriteRegistry,
    private val spriteRenderer: EntitySpriteRenderer = EntitySpriteRenderer(),
) {
    private val resolvedAssets = assets.sortedBy(WorldAsset::id).map { asset ->
        require(sprites.contains(asset.spriteId)) {
            "World asset '${asset.id}' references unknown ASCII sprite '${asset.spriteId}'"
        }
        asset to sprites.sprite(asset.spriteId)
    }

    fun render(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics): Int {
        var written = 0
        for ((asset, sprite) in resolvedAssets) {
            written += spriteRenderer.render(
                frame = frame,
                pose = pose,
                metrics = metrics,
                sprite = sprite,
                position = asset.position,
                anchorHeight = asset.anchorHeight,
                scaleX = asset.scaleX,
                scaleY = asset.scaleY,
                scaleZ = asset.scaleZ,
                orientationYawRadians = asset.yawRadians,
            )
        }
        return written
    }
}
