package com.asciicity.render

import com.asciicity.content.MaterialRegistry
import com.asciicity.content.MapCeiling
import com.asciicity.system.NpcSystem
import com.asciicity.system.TrafficSystem
import com.asciicity.system.AmbientActorSystem
import com.asciicity.ambient.AmbientEventSystem
import com.asciicity.world.GameWorld
import kotlin.math.ceil
import kotlin.math.floor

class WorldAsciiRenderer(
    private val world: GameWorld,
    private val materials: MaterialRegistry,
    private val palette: RenderPalette,
    presentation: PresentationTuning = PresentationTuning.DEFAULT,
    facadeSigns: Collection<FacadeSign> = emptyList(),
    private val trafficSystem: TrafficSystem? = null,
    private val npcSystem: NpcSystem? = null,
    private val ceiling: MapCeiling? = null,
    private val maxRayDistance: Double = 48.0,
    maxRaySteps: Int = 192,
    private val terminalBillboardRenderer: TerminalBillboardRenderer? = null,
    private val doorBillboardRenderer: DoorBillboardRenderer? = null,
    atmosphericEmitters: Collection<AtmosphericEmitter> = emptyList(),
    private val ambientEventSystem: AmbientEventSystem? = null,
    private val ambientActorSystem: AmbientActorSystem? = null,
    private val fontProfile: AsciiFontProfile = AsciiFontProfile.STANDARD,
) {
    private val materialSampler = MaterialSampler(palette, presentation)
    private val facadeSignSampler = FacadeSignSampler(
        facadeSigns,
        materialSampler,
        world.grid.width,
        world.grid.depth,
    )
    private val facadeLabelRenderer = FacadeLabelRenderer(facadeSigns, materialSampler)
    private val localizedLightRenderer = LocalizedLightRenderer()
    private val streetLampRenderer = StreetLampRenderer(world.allEntities(), world.grid, palette)
    private val parkFireflyRenderer = ParkFireflyRenderer(world.allEntities(), world.grid, materialSampler)
    private val rainRenderer = RainRenderer(palette)
    private val atmosphericRenderer = AtmosphericRenderer(atmosphericEmitters, materialSampler)
    private val parkFixtureBillboardRenderer = ParkFixtureBillboardRenderer(
        world.grid,
        materials,
        materialSampler,
        maxRayDistance,
    )
    private val maxRaySteps = maxRaySteps
    private val maxWorldHeight: Double
    private var coveredRows = BooleanArray(0)
    private val wallVisitor = WallRayVisitor()
    private val facadeSignSample = FacadeSignSample()
    private var cachedMetrics: ProjectionMetrics? = null
    private var cachedWidth = -1
    private var cachedHeight = -1
    private var cachedPitch = Double.NaN
    private var cachedFov = Double.NaN

    val stats = WorldRenderStats()

    init {
        var height = 0
        world.grid.forEachCell { _, definition -> height = maxOf(height, definition.height) }
        maxWorldHeight = height.toDouble()
    }

    fun render(
        frame: AsciiFrame,
        pose: ViewPose,
        timeSeconds: Double,
        interpolationAlpha: Double = 1.0,
    ) {
        require(timeSeconds.isFinite() && timeSeconds >= 0.0) { "Render time must be finite and non-negative" }
        require(interpolationAlpha.isFinite() && interpolationAlpha in 0.0..1.0) {
            "Render interpolation alpha must be finite and between zero and one"
        }
        val metrics = metrics(frame, pose)
        stats.reset()
        frame.clear(
            glyph = ' '.code,
            foreground = palette[PaletteKeys.FOG],
            background = palette[PaletteKeys.SKY],
        )
        renderGround(frame, pose, metrics, timeSeconds)
        renderCeiling(frame, pose, metrics, timeSeconds)
        renderWalls(frame, pose, metrics, timeSeconds)
        facadeLabelRenderer.render(frame, pose, metrics)
        parkFixtureBillboardRenderer.render(frame, pose, metrics, timeSeconds)
        stats.localizedLightWrites = localizedLightRenderer.render(frame)
        stats.atmosphericWrites = atmosphericRenderer.render(frame, pose, metrics, timeSeconds)
        stats.ambientEventWrites = ambientEventSystem?.render(frame, pose, metrics, materialSampler) ?: 0
        stats.trafficSpriteWrites = trafficSystem?.render(frame, pose, metrics, timeSeconds) ?: 0
        doorBillboardRenderer?.render(frame, pose, metrics)
        stats.terminalSpriteWrites = terminalBillboardRenderer?.render(frame, pose, metrics) ?: 0
        stats.entitySpriteWrites = npcSystem?.render(frame, pose, metrics) ?: 0
        stats.ambientActorSpriteWrites = ambientActorSystem?.render(frame, pose, metrics, interpolationAlpha) ?: 0
        streetLampRenderer.render(frame, pose, metrics)
        stats.fireflyWrites = parkFireflyRenderer.render(frame, pose, metrics, timeSeconds)
        rainRenderer.render(frame, timeSeconds)
        countFilledGlyphs(frame)
    }

    private fun renderGround(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, timeSeconds: Double) {
        for (column in 0 until frame.width) {
            val directionX = metrics.rayDirectionX(pose, column)
            val directionZ = metrics.rayDirectionZ(pose, column)
            for (row in 0 until frame.height) {
                val depth = metrics.groundDepthAtRow(row, pose.eyeHeight)
                if (!depth.isFinite() || depth > maxRayDistance) continue
                val worldX = pose.position.x + directionX * depth
                val worldZ = pose.position.z + directionZ * depth
                val cellX = floor(worldX).toInt()
                val cellZ = floor(worldZ).toInt()
                if (!world.grid.contains(cellX, cellZ)) continue
                val definition = world.grid.cellAt(cellX, cellZ)
                val material = materials.material(definition.groundMaterialId)
                frame.set(
                    x = column,
                    y = row,
                    glyph = materialSampler.glyph(material, worldX, worldZ, 0.0, timeSeconds, depth),
                    foreground = materialSampler.foreground(
                        material,
                        side = null,
                        depth = depth,
                        worldX = worldX,
                        worldZ = worldZ,
                        vertical = 0.0,
                        timeSeconds = timeSeconds,
                    ),
                    background = materialSampler.background(material, side = null),
                    depth = depth.toFloat(),
                )
            }
        }
    }

    private fun renderCeiling(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, timeSeconds: Double) {
        val definition = ceiling ?: return
        val material = materials.material(definition.materialId)
        for (column in 0 until frame.width) {
            val directionX = metrics.rayDirectionX(pose, column)
            val directionZ = metrics.rayDirectionZ(pose, column)
            for (row in 0 until frame.height) {
                val depth = metrics.ceilingDepthAtRow(row, pose.eyeHeight, definition.height)
                if (!depth.isFinite() || depth > maxRayDistance) continue
                val worldX = pose.position.x + directionX * depth
                val worldZ = pose.position.z + directionZ * depth
                val cellX = floor(worldX).toInt()
                val cellZ = floor(worldZ).toInt()
                if (!world.grid.contains(cellX, cellZ)) continue
                frame.set(
                    x = column,
                    y = row,
                    glyph = materialSampler.glyph(
                        material,
                        worldX,
                        worldZ,
                        definition.height,
                        timeSeconds,
                        depth,
                    ),
                    foreground = materialSampler.foreground(
                        material,
                        side = null,
                        depth = depth,
                        worldX = worldX,
                        worldZ = worldZ,
                        vertical = definition.height,
                        timeSeconds = timeSeconds,
                    ),
                    background = materialSampler.background(material, side = null),
                    depth = depth.toFloat(),
                )
                stats.ceilingWrites += 1
            }
        }
    }

    private fun renderWalls(frame: AsciiFrame, pose: ViewPose, metrics: ProjectionMetrics, timeSeconds: Double) {
        if (coveredRows.size != frame.height) coveredRows = BooleanArray(frame.height)
        for (column in 0 until frame.width) {
            coveredRows.fill(false)
            stats.rayCount += 1
            val directionX = metrics.rayDirectionX(pose, column)
            val directionZ = metrics.rayDirectionZ(pose, column)
            wallVisitor.prepare(frame, pose, metrics, timeSeconds, column, directionX, directionZ)
            GridRaycaster.traverse(
                originX = pose.position.x,
                originZ = pose.position.z,
                directionX = directionX,
                directionZ = directionZ,
                gridWidth = world.grid.width,
                gridDepth = world.grid.depth,
                maxDistance = maxRayDistance,
                maxSteps = maxRaySteps,
                visitor = wallVisitor,
            )
        }
    }

    private inner class WallRayVisitor : RayVisitor {
        private lateinit var frame: AsciiFrame
        private lateinit var pose: ViewPose
        private lateinit var metrics: ProjectionMetrics
        private var timeSeconds = 0.0
        private var column = 0
        private var directionX = 0.0
        private var directionZ = 0.0

        fun prepare(
            frame: AsciiFrame,
            pose: ViewPose,
            metrics: ProjectionMetrics,
            timeSeconds: Double,
            column: Int,
            directionX: Double,
            directionZ: Double,
        ) {
            this.frame = frame
            this.pose = pose
            this.metrics = metrics
            this.timeSeconds = timeSeconds
            this.column = column
            this.directionX = directionX
            this.directionZ = directionZ
        }

        override fun visit(
            cellX: Int,
            cellZ: Int,
            side: RayHitSide,
            distance: Double,
            faceCoordinate: Double,
        ): Boolean {
            stats.ddaSteps += 1
            if (distance <= MINIMUM_DEPTH) return true
            val definition = world.grid.cellAt(cellX, cellZ)
            if (!definition.opaque || definition.height <= 0) return true
            if (PARK_FIXTURE_TAG in definition.tags) return true
            stats.wallHits += 1
            val bottom = metrics.projectWorldHeight(0.0, pose.eyeHeight, distance)
            val top = metrics.projectWorldHeight(definition.height.toDouble(), pose.eyeHeight, distance)
            val firstRow = ceil(bottom).toInt().coerceAtLeast(0)
            val lastRow = floor(top).toInt().coerceAtMost(frame.height - 1)
            if (firstRow <= lastRow) {
                val material = materials.material(definition.facadeMaterialId)
                val isTree = TREE_TAG in definition.tags
                val isPine = PINE_TAG in definition.tags
                val hitWorldX = pose.position.x + directionX * distance
                val hitWorldZ = pose.position.z + directionZ * distance
                val facadeFace = FacadeFace.fromRayHit(side, directionX, directionZ)
                for (row in firstRow..lastRow) {
                    val vertical = pose.eyeHeight +
                        (row + 0.5 - metrics.horizonRow) * distance / metrics.focalRows
                    val treePart = if (isTree) {
                        TreeSilhouette.partAt(
                            faceCoordinate,
                            vertical,
                            definition.height.toDouble(),
                            pine = isPine,
                        ) ?: continue
                    } else null
                    val hasFacadeSign = facadeFace != null && facadeSignSampler.sample(
                        cellX = cellX,
                        cellZ = cellZ,
                        face = facadeFace,
                        side = side,
                        faceCoordinate = faceCoordinate,
                        vertical = vertical,
                        depth = distance,
                        timeSeconds = timeSeconds,
                        output = facadeSignSample,
                    )
                    val glyph = when {
                        hasFacadeSign -> facadeSignSample.glyph
                        treePart == TreeSilhouette.Part.TRUNK -> '|'.code
                        else -> materialSampler.glyph(
                            material,
                            hitWorldX,
                            hitWorldZ,
                            vertical,
                            timeSeconds,
                            distance,
                        )
                    }
                    val foreground = if (treePart == TreeSilhouette.Part.TRUNK) {
                        materialSampler.foreground(PaletteKeys.PARK_TRUNK, side, distance)
                    } else if (hasFacadeSign) {
                        facadeSignSample.foreground
                    } else {
                        materialSampler.foreground(
                            material,
                            side = side,
                            depth = distance,
                            worldX = hitWorldX,
                            worldZ = hitWorldZ,
                            vertical = vertical,
                            timeSeconds = timeSeconds,
                        )
                    }
                    val background = if (treePart == TreeSilhouette.Part.TRUNK) {
                        materialSampler.background(PaletteKeys.PARK_TRUNK, side)
                    } else if (hasFacadeSign) {
                        facadeSignSample.background
                    } else {
                        materialSampler.background(material, side, distance)
                    }
                    if (frame.writeIfNearer(
                            x = column,
                            y = row,
                            glyph = glyph,
                            foreground = foreground,
                            background = background,
                            depth = distance.toFloat(),
                            emissive = if (hasFacadeSign) FACADE_SIGN_EMISSIVE_STRENGTH else 0f,
                        )
                    ) {
                        coveredRows[row] = true
                    }
                }
            }
            return !allPossibleWallRowsCovered(distance)
        }

        private fun allPossibleWallRowsCovered(distance: Double): Boolean {
            val possibleBottom = ceil(metrics.projectWorldHeight(0.0, pose.eyeHeight, distance))
                .toInt().coerceAtLeast(0)
            val possibleTop = floor(metrics.projectWorldHeight(maxWorldHeight, pose.eyeHeight, distance))
                .toInt().coerceAtMost(frame.height - 1)
            if (possibleBottom > possibleTop) return true
            for (row in possibleBottom..possibleTop) if (!coveredRows[row]) return false
            return true
        }
    }

    private fun countFilledGlyphs(frame: AsciiFrame) {
        var count = 0
        for (row in 0 until frame.height) {
            for (column in 0 until frame.width) {
                if (frame.glyphAt(column, row) != ' '.code) count += 1
            }
        }
        stats.filledGlyphCount = count
    }

    private fun metrics(frame: AsciiFrame, pose: ViewPose): ProjectionMetrics {
        val current = cachedMetrics
        if (
            current == null || cachedWidth != frame.width || cachedHeight != frame.height ||
            cachedPitch != pose.pitchRadians || cachedFov != pose.horizontalFovRadians
        ) {
            cachedWidth = frame.width
            cachedHeight = frame.height
            cachedPitch = pose.pitchRadians
            cachedFov = pose.horizontalFovRadians
            cachedMetrics = ProjectionMetrics(
                columns = frame.width,
                rows = frame.height,
                pixelWidth = frame.width * fontProfile.cellWidth,
                pixelHeight = frame.height * fontProfile.cellHeight,
                horizontalFovRadians = pose.horizontalFovRadians,
                pitchRadians = pose.pitchRadians,
            )
        }
        return cachedMetrics!!
    }

    companion object {
        private const val MINIMUM_DEPTH = 1e-6
        private const val TREE_TAG = "tree"
        private const val PINE_TAG = "pine"
        private const val PARK_FIXTURE_TAG = "fixture"
        private const val FACADE_SIGN_EMISSIVE_STRENGTH = 0.58f
    }
}
