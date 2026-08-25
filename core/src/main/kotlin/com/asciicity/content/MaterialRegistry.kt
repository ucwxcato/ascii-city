package com.asciicity.content

import com.asciicity.render.PaletteValidationException
import com.asciicity.render.RenderPalette
import com.asciicity.world.SurfaceAnimation
import com.asciicity.world.SurfaceMaterial

class MaterialRegistry private constructor(
    materials: List<SurfaceMaterial>,
) {
    private val byId = materials.associateBy(SurfaceMaterial::id)

    val size: Int
        get() = byId.size

    fun contains(id: String): Boolean = id in byId

    fun material(id: String): SurfaceMaterial = byId[id]
        ?: throw IllegalArgumentException("Unknown material '$id'")

    fun all(): List<SurfaceMaterial> = byId.values.toList()

    companion object {
        fun create(
            materials: List<SurfaceMaterial>,
            palette: RenderPalette,
            source: String = "materials",
        ): MaterialRegistry {
            if (materials.isEmpty()) {
                throw ContentValidationException("$source:$.materials: at least one material is required")
            }
            val seen = mutableSetOf<String>()
            for ((index, material) in materials.withIndex()) {
                val path = "$.materials[$index]"
                ContentIds.requireStable(material.id, source, "$path.id")
                if (!seen.add(material.id)) {
                    throw ContentValidationException("$source:$path.id: duplicate material ID '${material.id}'")
                }
                if (material.glyphRamp.isEmpty() || material.glyphRamp.any { it.code !in 32..126 }) {
                    throw ContentValidationException("$source:$path.glyphRamp: must contain printable ASCII characters")
                }
                try {
                    palette[material.paletteKey]
                } catch (_: PaletteValidationException) {
                    throw ContentValidationException(
                        "$source:$path.paletteKey: unknown palette key '${material.paletteKey}'",
                    )
                }
                for ((accentIndex, accentKey) in material.accentPaletteKeys.withIndex()) {
                    try {
                        palette[accentKey]
                    } catch (_: PaletteValidationException) {
                        throw ContentValidationException(
                            "$source:$path.accentPaletteKeys[$accentIndex]: unknown palette key '$accentKey'",
                        )
                    }
                }
                if (!material.animationRate.isFinite() || material.animationRate < 0.0) {
                    throw ContentValidationException("$source:$path.animationRate: must be finite and non-negative")
                }
                if (!material.phaseScale.isFinite() || material.phaseScale < 0.0) {
                    throw ContentValidationException("$source:$path.phaseScale: must be finite and non-negative")
                }
                if (material.animation == SurfaceAnimation.STATIC && material.animationRate != 0.0) {
                    throw ContentValidationException("$source:$path.animationRate: static materials must use 0")
                }
            }
            return MaterialRegistry(materials)
        }
    }
}
