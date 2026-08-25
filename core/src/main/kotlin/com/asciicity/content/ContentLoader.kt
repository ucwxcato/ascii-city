package com.asciicity.content

import com.asciicity.entity.Npc
import com.asciicity.entity.AmbientActor
import com.asciicity.entity.PlayerState
import com.asciicity.entity.WorldEntity
import com.asciicity.entity.WorldEntityType
import com.asciicity.render.AsciiSprite
import com.asciicity.render.AtmosphericEmitter
import com.asciicity.render.AtmosphericEmitterKind
import com.asciicity.render.FacadeFace
import com.asciicity.render.FacadeSign
import com.asciicity.render.PaletteValidationException
import com.asciicity.render.PresentationTuning
import com.asciicity.render.RenderPalette
import com.asciicity.traffic.TrafficCar
import com.asciicity.ambient.AmbientEventEmitter
import com.asciicity.ambient.AmbientEventKind
import com.asciicity.world.CellDefinition
import com.asciicity.world.EntityId
import com.asciicity.world.GameWorld
import com.asciicity.world.SurfaceAnimation
import com.asciicity.world.SurfaceMaterial
import com.asciicity.world.WorldPosition
import com.asciicity.world.WorldGrid
import java.nio.file.Files
import java.nio.file.Path
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

private const val MINIMUM_CEILING_CLEARANCE = 0.5

class ContentLoader {
    fun load(assetRoot: Path, mapName: String = "city-alpha"): LoadedContent {
        ContentIds.requireStable(mapName, "command line", "$.mapName")
        val normalizedRoot = assetRoot.toAbsolutePath().normalize()
        val paletteSource = "data/palette.json"
        val presentationSource = "data/presentation.json"
        val materialSource = "data/materials.json"
        val cellSource = "data/cells.json"
        val spriteSource = "data/sprites.json"
        val dialogueSource = "data/dialogue.json"
        val mapSource = "maps/$mapName/$mapName.txt"
        val metadataSource = "maps/$mapName/$mapName.meta.json"

        val palette = parsePalette(read(normalizedRoot, paletteSource), paletteSource)
        val presentation = parsePresentation(read(normalizedRoot, presentationSource), presentationSource)
        val materials = parseMaterials(read(normalizedRoot, materialSource), materialSource, palette, presentation)
        val cells = parseCells(read(normalizedRoot, cellSource), cellSource, materials)
        val sprites = parseSprites(read(normalizedRoot, spriteSource), spriteSource, palette)
        val grid = MapLoader().parse(read(normalizedRoot, mapSource), mapSource, cells)
        val metadata = parseMetadata(read(normalizedRoot, metadataSource), metadataSource, palette, materials)
        if (metadata.id != mapName) {
            throw ContentValidationException(
                "$metadataSource:$.id: map ID '${metadata.id}' must match requested map '$mapName'",
            )
        }
        validateSpawn(metadata, grid.width, grid.depth) { x, z -> grid.cellAt(WorldPosition(x, z).toGridCell()).solid }

        val dialogues = parseDialogues(read(normalizedRoot, dialogueSource), dialogueSource)
        val objectSource = resolveObjectSource(metadata.objectsFile, metadataSource)
        val mapObjects = parseObjects(
            text = read(normalizedRoot, objectSource),
            source = objectSource,
            grid = grid,
            dialogues = dialogues,
            sprites = sprites,
            palette = palette,
        )
        validateInteractionReachability(grid, metadata.playerSpawn, mapObjects.entities, objectSource)

        val world = GameWorld(
            mapId = metadata.id,
            mapVersion = metadata.version,
            grid = grid,
            player = metadata.playerSpawn,
            entities = mapObjects.entities,
        )
        return LoadedContent(
            palette = palette,
            presentation = presentation,
            materials = materials,
            cells = cells,
            sprites = sprites,
            dialogues = dialogues,
            metadata = metadata,
            world = world,
            npcs = mapObjects.npcs,
            trafficCars = mapObjects.trafficCars,
            facadeSigns = mapObjects.facadeSigns,
            atmosphericEmitters = mapObjects.atmosphericEmitters,
            ambientEventEmitters = mapObjects.ambientEventEmitters,
            ambientActors = mapObjects.ambientActors,
        )
    }

    private fun parsePalette(text: String, source: String): RenderPalette = try {
        RenderPalette.parse(text, source)
    } catch (error: PaletteValidationException) {
        throw ContentValidationException("$source:$: ${error.message}", error)
    }

    private fun parsePresentation(text: String, source: String): PresentationTuning {
        val root = JsonContentReader.parseObject(text, source)
        root.rejectUnknown(
            setOf(
                "fogStartDistance",
                "fogEndDistance",
                "fogEndGlyphDensity",
                "nearContrastDistance",
                "minimumNearLuminanceContrast",
                "sideFaceBrightness",
                "signalPulseMinimumBrightness",
                "glitchBrightnessBoost",
                "maximumGlitchRate",
                "glitchGlyphChanceDivisor",
                "glitchFlashChanceDivisor",
            ),
        )
        val fogStart = root.requiredDouble("fogStartDistance")
        val fogEnd = root.requiredDouble("fogEndDistance")
        val fogEndGlyphDensity = root.requiredDouble("fogEndGlyphDensity")
        val nearContrastDistance = root.requiredDouble("nearContrastDistance")
        val minimumNearContrast = root.requiredDouble("minimumNearLuminanceContrast")
        val sideBrightness = root.requiredDouble("sideFaceBrightness")
        val pulseMinimum = root.requiredDouble("signalPulseMinimumBrightness")
        val glitchBoost = root.requiredDouble("glitchBrightnessBoost")
        val maximumGlitchRate = root.requiredDouble("maximumGlitchRate")
        val glyphDivisor = root.requiredInt("glitchGlyphChanceDivisor")
        val flashDivisor = root.requiredInt("glitchFlashChanceDivisor")

        if (!fogStart.isFinite() || fogStart < 0.0) {
            throw ContentValidationException("$source:$.fogStartDistance: must be finite and non-negative")
        }
        if (!fogEnd.isFinite() || fogEnd <= fogStart) {
            throw ContentValidationException("$source:$.fogEndDistance: must be finite and greater than fogStartDistance")
        }
        if (!fogEndGlyphDensity.isFinite() || fogEndGlyphDensity !in 0.0..1.0) {
            throw ContentValidationException("$source:$.fogEndGlyphDensity: must be between zero and one")
        }
        if (!nearContrastDistance.isFinite() || nearContrastDistance !in 0.0..fogStart) {
            throw ContentValidationException(
                "$source:$.nearContrastDistance: must be finite and between zero and fogStartDistance",
            )
        }
        if (!minimumNearContrast.isFinite() || minimumNearContrast !in 0.0..1.0) {
            throw ContentValidationException("$source:$.minimumNearLuminanceContrast: must be between zero and one")
        }
        if (!sideBrightness.isFinite() || sideBrightness !in 0.0..1.0) {
            throw ContentValidationException("$source:$.sideFaceBrightness: must be between zero and one")
        }
        if (!pulseMinimum.isFinite() || pulseMinimum !in 0.0..1.0) {
            throw ContentValidationException("$source:$.signalPulseMinimumBrightness: must be between zero and one")
        }
        if (!glitchBoost.isFinite() || glitchBoost !in 1.0..2.0) {
            throw ContentValidationException("$source:$.glitchBrightnessBoost: must be between one and two")
        }
        if (!maximumGlitchRate.isFinite() || maximumGlitchRate <= 0.0) {
            throw ContentValidationException("$source:$.maximumGlitchRate: must be positive and finite")
        }
        if (glyphDivisor < 2) {
            throw ContentValidationException("$source:$.glitchGlyphChanceDivisor: must be at least two")
        }
        if (flashDivisor < 2) {
            throw ContentValidationException("$source:$.glitchFlashChanceDivisor: must be at least two")
        }
        return PresentationTuning(
            fogStartDistance = fogStart,
            fogEndDistance = fogEnd,
            fogEndGlyphDensity = fogEndGlyphDensity,
            nearContrastDistance = nearContrastDistance,
            minimumNearLuminanceContrast = minimumNearContrast,
            sideFaceBrightness = sideBrightness,
            signalPulseMinimumBrightness = pulseMinimum,
            glitchBrightnessBoost = glitchBoost,
            maximumGlitchRate = maximumGlitchRate,
            glitchGlyphChanceDivisor = glyphDivisor,
            glitchFlashChanceDivisor = flashDivisor,
        )
    }

    private fun parseMaterials(
        text: String,
        source: String,
        palette: RenderPalette,
        presentation: PresentationTuning,
    ): MaterialRegistry {
        val root = JsonContentReader.parseObject(text, source)
        root.rejectUnknown(setOf("materials"))
        val materials = root.requiredArray("materials").map { value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf("id", "glyphRamp", "paletteKey", "accentPaletteKeys", "animation", "animationRate", "phaseScale", "tags"),
            )
            val material = SurfaceMaterial(
                id = item.requiredString("id"),
                glyphRamp = item.requiredString("glyphRamp"),
                paletteKey = item.requiredString("paletteKey"),
                animation = parseAnimation(item.requiredString("animation"), item.location),
                animationRate = item.requiredDouble("animationRate"),
                phaseScale = item.requiredDouble("phaseScale"),
                tags = item.optionalStringArray("tags"),
                accentPaletteKeys = item.optionalStringList("accentPaletteKeys"),
            )
            if (
                material.animation == SurfaceAnimation.GLITCH_FLICKER &&
                material.animationRate > presentation.maximumGlitchRate
            ) {
                throw ContentValidationException(
                    "${item.location}.animationRate: glitchFlicker must not exceed " +
                        "presentation maximumGlitchRate ${presentation.maximumGlitchRate}",
                )
            }
            material
        }
        return MaterialRegistry.create(materials, palette, source)
    }

    private fun parseCells(text: String, source: String, materials: MaterialRegistry): CellRegistry {
        val root = JsonContentReader.parseObject(text, source)
        root.rejectUnknown(setOf("cells"))
        val cells = root.requiredArray("cells").map { value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf(
                    "id",
                    "token",
                    "groundMaterial",
                    "facadeMaterial",
                    "solid",
                    "opaque",
                    "height",
                    "tags",
                    "interactionType",
                ),
            )
            val token = item.requiredString("token")
            if (token.length != 1) {
                throw ContentValidationException("${item.location}.token: must contain exactly one character")
            }
            CellDefinition(
                id = item.requiredString("id"),
                token = token.single(),
                groundMaterialId = item.requiredString("groundMaterial"),
                facadeMaterialId = item.requiredString("facadeMaterial"),
                solid = item.requiredBoolean("solid"),
                opaque = item.requiredBoolean("opaque"),
                height = item.requiredInt("height"),
                tags = item.optionalStringArray("tags"),
                interactionType = item.optionalString("interactionType"),
            )
        }
        return CellRegistry.create(cells, materials, source)
    }

    private fun parseSprites(text: String, source: String, palette: RenderPalette): AsciiSpriteRegistry {
        val root = JsonContentReader.parseObject(text, source)
        root.rejectUnknown(setOf("sprites"))
        val sprites = root.requiredArray("sprites").mapIndexed { index, value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf(
                    "id",
                    "physicalWidth",
                    "physicalHeight",
                    "anchor",
                    "glyphRows",
                    "colorRows",
                    "colors",
                ),
            )
            val id = item.requiredString("id")
            ContentIds.requireStable(id, source, "$.sprites[$index].id")
            val anchor = item.requiredObject("anchor")
            anchor.rejectUnknown(setOf("x", "y"))
            val physicalWidth = item.requiredDouble("physicalWidth")
            val physicalHeight = item.requiredDouble("physicalHeight")
            val anchorX = anchor.requiredDouble("x")
            val anchorY = anchor.requiredDouble("y")
            validateSpritePhysicalSize(physicalWidth, "${item.location}.physicalWidth")
            validateSpritePhysicalSize(physicalHeight, "${item.location}.physicalHeight")
            validateSpriteAnchor(anchorX, "${anchor.location}.x")
            validateSpriteAnchor(anchorY, "${anchor.location}.y")

            val glyphRows = item.requiredArray("glyphRows").map(JsonValueView::asString)
            val colorRows = item.requiredArray("colorRows").map(JsonValueView::asString)
            val columns = validateSpriteRows(glyphRows, "${item.location}.glyphRows")
            validateSpriteColorRows(colorRows, glyphRows.size, columns, "${item.location}.colorRows")

            val colors = linkedMapOf<Char, Int>()
            val colorDefinitions = item.requiredArray("colors")
            if (colorDefinitions.isEmpty()) {
                throw ContentValidationException("${item.location}.colors: at least one color definition is required")
            }
            for (colorValue in colorDefinitions) {
                val colorItem = colorValue.asObject()
                colorItem.rejectUnknown(setOf("symbol", "palette"))
                val symbolText = colorItem.requiredString("symbol")
                if (symbolText.length != 1 || symbolText.single() == ' ' || symbolText.single().code !in 33..126) {
                    throw ContentValidationException(
                        "${colorItem.location}.symbol: must be one non-space printable ASCII character",
                    )
                }
                val symbol = symbolText.single()
                if (symbol in colors) {
                    throw ContentValidationException("${colorItem.location}.symbol: duplicate color symbol '$symbol'")
                }
                val paletteKey = colorItem.requiredString("palette")
                colors[symbol] = try {
                    palette[paletteKey]
                } catch (_: PaletteValidationException) {
                    throw ContentValidationException(
                        "${colorItem.location}.palette: unknown palette key '$paletteKey'",
                    )
                }
            }

            val cellCount = Math.multiplyExact(columns, glyphRows.size)
            val glyphs = IntArray(cellCount)
            val foregroundColors = IntArray(cellCount)
            var visibleGlyphCount = 0
            for (rowIndex in glyphRows.indices) {
                val glyphRow = glyphRows[rowIndex]
                val colorRow = colorRows[rowIndex]
                for (columnIndex in 0 until columns) {
                    val glyph = glyphRow[columnIndex]
                    val colorSymbol = colorRow[columnIndex]
                    val cellPath = "${item.location}.colorRows[$rowIndex] column ${columnIndex + 1}"
                    when {
                        glyph == ' ' && colorSymbol != ' ' -> throw ContentValidationException(
                            "$cellPath: transparent glyph space must use a transparent color space",
                        )
                        glyph != ' ' && colorSymbol == ' ' -> throw ContentValidationException(
                            "$cellPath: visible glyph '$glyph' requires a color symbol",
                        )
                        colorSymbol != ' ' && colorSymbol !in colors -> throw ContentValidationException(
                            "$cellPath: unknown color symbol '$colorSymbol'",
                        )
                    }
                    val cellIndex = rowIndex * columns + columnIndex
                    glyphs[cellIndex] = glyph.code
                    if (glyph != ' ') {
                        foregroundColors[cellIndex] = colors.getValue(colorSymbol)
                        visibleGlyphCount += 1
                    }
                }
            }
            if (visibleGlyphCount == 0) {
                throw ContentValidationException("${item.location}.glyphRows: must contain at least one visible glyph")
            }
            AsciiSprite(
                id = id,
                columns = columns,
                rows = glyphRows.size,
                anchorX = anchorX,
                anchorY = anchorY,
                physicalWidth = physicalWidth,
                physicalHeight = physicalHeight,
                glyphs = glyphs,
                foregroundColors = foregroundColors,
            )
        }
        return AsciiSpriteRegistry.create(sprites, source)
    }

    private fun validateSpriteRows(rows: List<String>, location: String): Int {
        if (rows.size !in 1..AsciiSprite.MAXIMUM_ROWS) {
            throw ContentValidationException("$location: must contain between 1 and ${AsciiSprite.MAXIMUM_ROWS} rows")
        }
        val columns = rows.first().length
        if (columns !in 1..AsciiSprite.MAXIMUM_COLUMNS) {
            throw ContentValidationException(
                "$location[0]: must contain between 1 and ${AsciiSprite.MAXIMUM_COLUMNS} glyphs",
            )
        }
        for ((rowIndex, row) in rows.withIndex()) {
            if (row.length != columns) {
                throw ContentValidationException(
                    "$location[$rowIndex]: ragged row has width ${row.length}; expected $columns",
                )
            }
            if (row.any { it.code !in 32..126 }) {
                throw ContentValidationException("$location[$rowIndex]: must contain printable ASCII only")
            }
        }
        return columns
    }

    private fun validateSpriteColorRows(rows: List<String>, expectedRows: Int, columns: Int, location: String) {
        if (rows.size != expectedRows) {
            throw ContentValidationException("$location: has ${rows.size} rows; expected $expectedRows")
        }
        for ((rowIndex, row) in rows.withIndex()) {
            if (row.length != columns) {
                throw ContentValidationException(
                    "$location[$rowIndex]: ragged row has width ${row.length}; expected $columns",
                )
            }
            if (row.any { it.code !in 32..126 }) {
                throw ContentValidationException("$location[$rowIndex]: must contain printable ASCII only")
            }
        }
    }

    private fun validateSpritePhysicalSize(value: Double, location: String) {
        if (value <= 0.0 || value > AsciiSprite.MAXIMUM_PHYSICAL_SIZE) {
            throw ContentValidationException(
                "$location: must be positive and at most ${AsciiSprite.MAXIMUM_PHYSICAL_SIZE}",
            )
        }
    }

    private fun validateSpriteAnchor(value: Double, location: String) {
        if (value !in 0.0..1.0) {
            throw ContentValidationException("$location: must be between zero and one")
        }
    }

    private fun parseMetadata(
        text: String,
        source: String,
        palette: RenderPalette,
        materials: MaterialRegistry,
    ): MapMetadata {
        val root = JsonContentReader.parseObject(text, source)
        root.rejectUnknown(setOf("id", "version", "spawn", "skyPalette", "fogPalette", "objectsFile", "ceiling"))
        val spawn = root.requiredObject("spawn")
        spawn.rejectUnknown(setOf("x", "z", "yawDegrees"))
        val sky = root.requiredString("skyPalette")
        val fog = root.requiredString("fogPalette")
        requirePaletteKey(palette, sky, source, "$.skyPalette")
        requirePaletteKey(palette, fog, source, "$.fogPalette")
        val id = root.requiredString("id")
        ContentIds.requireStable(id, source, "$.id")
        val version = root.requiredInt("version")
        if (version <= 0) {
            throw ContentValidationException("$source:$.version: must be positive")
        }
        val playerSpawn = PlayerState(
            position = WorldPosition(spawn.requiredDouble("x"), spawn.requiredDouble("z")),
            yawRadians = spawn.requiredDouble("yawDegrees") * PI / 180.0,
        )
        val ceiling = root.optionalObject("ceiling")?.let { ceilingObject ->
            ceilingObject.rejectUnknown(setOf("height", "material"))
            val height = ceilingObject.requiredDouble("height")
            if (height - playerSpawn.eyeHeight < MINIMUM_CEILING_CLEARANCE) {
                throw ContentValidationException(
                    "${ceilingObject.location}.height: must be at least $MINIMUM_CEILING_CLEARANCE units " +
                        "above spawn eye height ${playerSpawn.eyeHeight}",
                )
            }
            val materialId = ceilingObject.requiredString("material")
            if (!materials.contains(materialId)) {
                throw ContentValidationException(
                    "${ceilingObject.location}.material: unknown material '$materialId'",
                )
            }
            MapCeiling(height = height, materialId = materialId)
        }
        return MapMetadata(
            id = id,
            version = version,
            playerSpawn = playerSpawn,
            skyPaletteKey = sky,
            fogPaletteKey = fog,
            objectsFile = root.requiredString("objectsFile"),
            ceiling = ceiling,
        )
    }

    private fun parseDialogues(text: String, source: String): DialogueRegistry {
        val root = JsonContentReader.parseObject(text, source)
        root.rejectUnknown(setOf("dialogues"))
        val dialogues = root.requiredArray("dialogues").map { value ->
            val item = value.asObject()
            item.rejectUnknown(setOf("id", "speaker", "lines"))
            DialogueDefinition(
                id = item.requiredString("id"),
                speaker = item.requiredString("speaker"),
                lines = item.requiredArray("lines").map(JsonValueView::asString),
            )
        }
        return DialogueRegistry.create(dialogues, source)
    }

    private fun parseObjects(
        text: String,
        source: String,
        grid: WorldGrid,
        dialogues: DialogueRegistry,
        sprites: AsciiSpriteRegistry,
        palette: RenderPalette,
    ): ParsedMapObjects {
        val root = JsonContentReader.parseObject(text, source)
        root.rejectUnknown(
            setOf(
                "objects",
                "facadeSigns",
                "facadeEntrances",
                "trafficCars",
                "atmosphericEmitters",
                "ambientEventEmitters",
                "ambientActors",
            ),
        )
        val ids = mutableSetOf<String>()
        val npcs = mutableListOf<Npc>()
        val entities = root.requiredArray("objects").map { value ->
            val item = value.asObject()
            val type = parseEntityType(item.requiredString("type"), item.location)
            val commonFields = setOf(
                "id",
                "type",
                "x",
                "z",
                "blocking",
                "radius",
                "interactionId",
                "displayText",
            )
            val doorFields = setOf("destinationMapId", "destinationSpawn", "renderBillboard")
            val npcFields = setOf("spriteId", "facingDegrees")
            item.rejectUnknown(
                commonFields +
                    (if (type == WorldEntityType.DOOR) doorFields else emptySet()) +
                    (if (type == WorldEntityType.NPC) npcFields else emptySet()),
            )
            val idText = item.requiredString("id")
            ContentIds.requireStable(idText, source, "${item.location}.id")
            if (!ids.add(idText)) {
                throw ContentValidationException("${item.location}.id: duplicate object ID '$idText'")
            }
            val position = WorldPosition(item.requiredDouble("x"), item.requiredDouble("z"))
            if (position.x < 0.0 || position.z < 0.0 || position.x >= grid.width || position.z >= grid.depth) {
                throw ContentValidationException(
                    "${item.location}: object '$idText' is outside map bounds ${grid.width}x${grid.depth}",
                )
            }
            if (
                type in setOf(WorldEntityType.NPC, WorldEntityType.TERMINAL, WorldEntityType.LAMP) &&
                grid.cellAt(position.toGridCell()).solid
            ) {
                throw ContentValidationException("${item.location}: object '$idText' must occupy a non-solid cell")
            }
            val interactionId = item.optionalString("interactionId")
            val destinationMapId = item.optionalString("destinationMapId")
            if (destinationMapId != null && destinationMapId !in MapCatalog.playableMapIds) {
                throw ContentValidationException(
                    "${item.location}.destinationMapId: unknown playable map '$destinationMapId'",
                )
            }
            val destinationSpawn = item.optionalObject("destinationSpawn")?.let { destination ->
                if (destinationMapId == null) {
                    throw ContentValidationException("${item.location}.destinationSpawn: requires destinationMapId")
                }
                destination.rejectUnknown(setOf("x", "z", "yawDegrees"))
                val position = WorldPosition(destination.requiredDouble("x"), destination.requiredDouble("z"))
                val yawDegrees = destination.optionalDouble("yawDegrees", Double.NaN)
                    .takeUnless(Double::isNaN)
                if (yawDegrees != null && (yawDegrees < 0.0 || yawDegrees >= 360.0)) {
                    throw ContentValidationException("${destination.location}.yawDegrees: must be at least zero and less than 360")
                }
                DestinationSpawn(position, yawDegrees)
            }
            if (type in setOf(WorldEntityType.NPC, WorldEntityType.TERMINAL) && interactionId == null) {
                throw ContentValidationException("${item.location}.interactionId: required for ${type.name.lowercase()}")
            }
            if (
                type in setOf(WorldEntityType.NPC, WorldEntityType.TERMINAL) &&
                interactionId != null &&
                !dialogues.contains(interactionId)
            ) {
                throw ContentValidationException("${item.location}.interactionId: unknown dialogue '$interactionId'")
            }
            val displayText = if (type in setOf(WorldEntityType.NPC, WorldEntityType.TERMINAL)) {
                item.requiredString("displayText").also { displayName ->
                    if (displayName.isBlank()) {
                        throw ContentValidationException(
                            "${item.location}.displayText: must not be blank for ${type.name.lowercase()}",
                        )
                    }
                }
            } else {
                item.optionalString("displayText")
            }
            val blocking = if (type == WorldEntityType.NPC) {
                item.requiredBoolean("blocking")
            } else {
                item.optionalBoolean("blocking", type == WorldEntityType.DOOR)
            }
            val collisionRadius = item.optionalDouble("radius", defaultCollisionRadius(type))
            if (collisionRadius < 0.0 || blocking && collisionRadius == 0.0) {
                throw ContentValidationException(
                    "${item.location}.radius: must be positive for blocking objects and non-negative otherwise",
                )
            }
            val entity = WorldEntity(
                id = EntityId(idText),
                type = type,
                position = position,
                blocking = blocking,
                collisionRadius = collisionRadius,
                interactionId = interactionId,
                displayText = displayText,
                destinationMapId = destinationMapId,
                destinationPosition = destinationSpawn?.position,
                destinationYawDegrees = destinationSpawn?.yawDegrees,
                renderBillboard = if (type == WorldEntityType.DOOR) {
                    item.optionalBoolean("renderBillboard", false)
                } else {
                    false
                },
            )
            if (type == WorldEntityType.TERMINAL) {
                val dialogueId = checkNotNull(interactionId)
                val speaker = dialogues.dialogue(dialogueId).speaker
                if (displayText != speaker) {
                    throw ContentValidationException(
                        "${item.location}.displayText: must match dialogue '$dialogueId' speaker '$speaker'",
                    )
                }
            }
            if (type == WorldEntityType.NPC) {
                val spriteId = item.requiredString("spriteId")
                ContentIds.requireStable(spriteId, source, "${item.location}.spriteId")
                if (!sprites.contains(spriteId)) {
                    throw ContentValidationException("${item.location}.spriteId: unknown ASCII sprite '$spriteId'")
                }
                val facingDegrees = item.requiredDouble("facingDegrees")
                if (facingDegrees < 0.0 || facingDegrees >= 360.0) {
                    throw ContentValidationException(
                        "${item.location}.facingDegrees: must be at least zero and less than 360",
                    )
                }
                val dialogueId = checkNotNull(interactionId)
                val displayName = checkNotNull(displayText)
                if (dialogues.dialogue(dialogueId).speaker != displayName) {
                    throw ContentValidationException(
                        "${item.location}.displayText: must match dialogue '$dialogueId' speaker " +
                            "'${dialogues.dialogue(dialogueId).speaker}'",
                    )
                }
                npcs += Npc(
                    entityId = entity.id,
                    spriteId = spriteId,
                    facingDegrees = facingDegrees,
                    displayName = displayName,
                    dialogueId = dialogueId,
                )
            }
            entity
        }.toMutableList()
        val occupiedFaces = mutableMapOf<String, MutableList<FacadeOverlaySpan>>()
        val facadeSigns = root.optionalArray("facadeSigns").mapIndexed { index, value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf(
                    "id",
                    "face",
                    "cellX",
                    "cellZ",
                    "widthCells",
                    "bottom",
                    "height",
                    "glyphRows",
                    "distantGlyphRows",
                    "borderPalette",
                    "textPalette",
                    "accentPalette",
                    "backgroundPalette",
                    "animationRate",
                    "label",
                ),
            )
            val idText = item.requiredString("id")
            ContentIds.requireStable(idText, source, "$.facadeSigns[$index].id")
            if (!ids.add(idText)) {
                throw ContentValidationException("${item.location}.id: duplicate object ID '$idText'")
            }
            val face = parseFacadeFace(item.requiredString("face"), item.location)
            val cellX = item.requiredInt("cellX")
            val cellZ = item.requiredInt("cellZ")
            val widthCells = item.requiredInt("widthCells")
            val bottom = item.requiredDouble("bottom")
            val height = item.requiredDouble("height")
            val glyphRows = item.requiredArray("glyphRows").map(JsonValueView::asString)
            val distantGlyphRows = item.requiredArray("distantGlyphRows").map(JsonValueView::asString)
            val borderPalette = item.requiredString("borderPalette")
            val textPalette = item.requiredString("textPalette")
            val accentPalette = item.requiredString("accentPalette")
            val backgroundPalette = item.requiredString("backgroundPalette")
            val animationRate = item.requiredDouble("animationRate")
            val label = item.optionalString("label")

            if (cellX < 0 || cellZ < 0) {
                throw ContentValidationException("${item.location}: facade sign anchor must be non-negative")
            }
            if (widthCells <= 0) {
                throw ContentValidationException("${item.location}.widthCells: must be positive")
            }
            if (bottom < 0.0) {
                throw ContentValidationException("${item.location}.bottom: must be non-negative")
            }
            if (height <= 0.0) {
                throw ContentValidationException("${item.location}.height: must be positive")
            }
            if (label != null && (label.isBlank() || label.length > FacadeSign.MAXIMUM_LABEL_LENGTH || label.any { it.code !in 32..126 })) {
                throw ContentValidationException(
                    "${item.location}.label: must be non-blank printable ASCII and at most ${FacadeSign.MAXIMUM_LABEL_LENGTH} glyphs",
                )
            }
            validateFacadeSignGlyphRows(glyphRows, "${item.location}.glyphRows")
            validateFacadeSignGlyphRows(distantGlyphRows, "${item.location}.distantGlyphRows")
            if (animationRate !in 0.0..FacadeSign.MAXIMUM_ANIMATION_RATE) {
                throw ContentValidationException(
                    "${item.location}.animationRate: must be between zero and ${FacadeSign.MAXIMUM_ANIMATION_RATE}",
                )
            }
            requirePaletteKey(palette, borderPalette, source, "$.facadeSigns[$index].borderPalette")
            requirePaletteKey(palette, textPalette, source, "$.facadeSigns[$index].textPalette")
            requirePaletteKey(palette, accentPalette, source, "$.facadeSigns[$index].accentPalette")
            requirePaletteKey(palette, backgroundPalette, source, "$.facadeSigns[$index].backgroundPalette")
            validateFacadeSignPlacement(
                grid = grid,
                sourceLocation = item.location,
                id = idText,
                face = face,
                cellX = cellX,
                cellZ = cellZ,
                widthCells = widthCells,
                bottom = bottom,
                top = bottom + height,
                occupiedFaces = occupiedFaces,
            )
            FacadeSign(
                id = idText,
                face = face,
                cellX = cellX,
                cellZ = cellZ,
                widthCells = widthCells,
                bottom = bottom,
                height = height,
                glyphRows = glyphRows,
                distantGlyphRows = distantGlyphRows,
                borderPaletteKey = borderPalette,
                textPaletteKey = textPalette,
                accentPaletteKey = accentPalette,
                backgroundPaletteKey = backgroundPalette,
                animationRate = animationRate,
                label = label,
            )
        }
        val facadeEntrances = root.optionalArray("facadeEntrances").mapIndexed { index, value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf(
                    "id",
                    "face",
                    "cellX",
                    "cellZ",
                    "widthCells",
                    "height",
                    "glyphRows",
                    "distantGlyphRows",
                    "borderPalette",
                    "textPalette",
                    "accentPalette",
                    "backgroundPalette",
                    "animationRate",
                    "lighting",
                ),
            )
            val idText = item.requiredString("id")
            ContentIds.requireStable(idText, source, "$.facadeEntrances[$index].id")
            if (!ids.add(idText)) {
                throw ContentValidationException("${item.location}.id: duplicate object ID '$idText'")
            }
            val face = parseFacadeFace(item.requiredString("face"), item.location)
            val cellX = item.requiredInt("cellX")
            val cellZ = item.requiredInt("cellZ")
            val widthCells = item.requiredInt("widthCells")
            val height = item.requiredDouble("height")
            val glyphRows = item.requiredArray("glyphRows").map(JsonValueView::asString)
            val distantGlyphRows = item.requiredArray("distantGlyphRows").map(JsonValueView::asString)
            val borderPalette = item.requiredString("borderPalette")
            val textPalette = item.requiredString("textPalette")
            val accentPalette = item.requiredString("accentPalette")
            val backgroundPalette = item.requiredString("backgroundPalette")
            val animationRate = item.requiredDouble("animationRate")
            val lighting = parseFacadeLighting(item.requiredString("lighting"), item.location)

            if (cellX < 0 || cellZ < 0) {
                throw ContentValidationException("${item.location}: facade entrance anchor must be non-negative")
            }
            if (widthCells <= 0) {
                throw ContentValidationException("${item.location}.widthCells: must be positive")
            }
            if (height <= 0.0) {
                throw ContentValidationException("${item.location}.height: must be positive")
            }
            validateFacadeSignGlyphRows(glyphRows, "${item.location}.glyphRows")
            validateFacadeSignGlyphRows(distantGlyphRows, "${item.location}.distantGlyphRows")
            if (animationRate !in 0.0..FacadeSign.MAXIMUM_ANIMATION_RATE) {
                throw ContentValidationException(
                    "${item.location}.animationRate: must be between zero and ${FacadeSign.MAXIMUM_ANIMATION_RATE}",
                )
            }
            requirePaletteKey(palette, borderPalette, source, "$.facadeEntrances[$index].borderPalette")
            requirePaletteKey(palette, textPalette, source, "$.facadeEntrances[$index].textPalette")
            requirePaletteKey(palette, accentPalette, source, "$.facadeEntrances[$index].accentPalette")
            requirePaletteKey(palette, backgroundPalette, source, "$.facadeEntrances[$index].backgroundPalette")
            validateFacadeSignPlacement(
                grid = grid,
                sourceLocation = item.location,
                id = idText,
                face = face,
                cellX = cellX,
                cellZ = cellZ,
                widthCells = widthCells,
                bottom = 0.0,
                top = height,
                occupiedFaces = occupiedFaces,
                overlayKind = "entrance",
            )
            FacadeSign(
                id = idText,
                face = face,
                cellX = cellX,
                cellZ = cellZ,
                widthCells = widthCells,
                bottom = 0.0,
                height = height,
                glyphRows = glyphRows,
                distantGlyphRows = distantGlyphRows,
                borderPaletteKey = borderPalette,
                textPaletteKey = textPalette,
                accentPaletteKey = accentPalette,
                backgroundPaletteKey = backgroundPalette,
                animationRate = animationRate,
                lighting = lighting,
            )
        }
        val trafficCars = root.optionalArray("trafficCars").mapIndexed { index, value ->
            val item = value.asObject()
            item.rejectUnknown(setOf("id", "spriteId", "speedCellsPerSecond", "phaseOffsetSeconds", "waypoints", "label"))
            val idText = item.requiredString("id")
            ContentIds.requireStable(idText, source, "$.trafficCars[$index].id")
            if (!ids.add(idText)) {
                throw ContentValidationException("${item.location}.id: duplicate object ID '$idText'")
            }
            val spriteId = item.requiredString("spriteId")
            ContentIds.requireStable(spriteId, source, "${item.location}.spriteId")
            if (!sprites.contains(spriteId)) {
                throw ContentValidationException("${item.location}.spriteId: unknown ASCII sprite '$spriteId'")
            }
            val speed = item.requiredDouble("speedCellsPerSecond")
            if (speed !in TrafficCar.MINIMUM_SPEED..TrafficCar.MAXIMUM_SPEED) {
                throw ContentValidationException(
                    "${item.location}.speedCellsPerSecond: must be between " +
                        "${TrafficCar.MINIMUM_SPEED} and ${TrafficCar.MAXIMUM_SPEED}",
                )
            }
            val phase = item.optionalDouble("phaseOffsetSeconds", 0.0)
            val label = item.optionalString("label")
            if (phase < 0.0) {
                throw ContentValidationException("${item.location}.phaseOffsetSeconds: must be non-negative")
            }
            if (label != null && (label.isBlank() || label.length > TrafficCar.MAXIMUM_LABEL_LENGTH || label.any { it.code !in 32..126 })) {
                throw ContentValidationException(
                    "${item.location}.label: must be non-blank printable ASCII and at most ${TrafficCar.MAXIMUM_LABEL_LENGTH} glyphs",
                )
            }
            val waypoints = item.requiredArray("waypoints").map { waypointValue ->
                val waypoint = waypointValue.asObject()
                waypoint.rejectUnknown(setOf("x", "z"))
                val position = WorldPosition(waypoint.requiredDouble("x"), waypoint.requiredDouble("z"))
                if (!grid.contains(position.toGridCell())) {
                    throw ContentValidationException("${waypoint.location}: waypoint is outside map bounds ${grid.width}x${grid.depth}")
                }
                if ("road" !in grid.cellAt(position.toGridCell()).tags) {
                    throw ContentValidationException("${waypoint.location}: waypoint must occupy a road cell")
                }
                position
            }
            if (waypoints.size < TrafficCar.MINIMUM_WAYPOINTS) {
                throw ContentValidationException(
                    "${item.location}.waypoints: must contain at least ${TrafficCar.MINIMUM_WAYPOINTS} waypoints",
                )
            }
            for (waypointIndex in waypoints.indices) {
                val nextIndex = (waypointIndex + 1) % waypoints.size
                if (waypoints[waypointIndex] == waypoints[nextIndex]) {
                    throw ContentValidationException("${item.location}.waypoints[$waypointIndex]: must not repeat the next waypoint")
                }
            }
            TrafficCar(idText, spriteId, speed, phase, waypoints, label)
        }
        val atmosphericEmitters = root.optionalArray("atmosphericEmitters").mapIndexed { index, value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf("id", "kind", "x", "z", "riseHeight", "drift", "periodSeconds", "phaseOffset"),
            )
            val idText = item.requiredString("id")
            ContentIds.requireStable(idText, source, "$.atmosphericEmitters[$index].id")
            if (!ids.add(idText)) {
                throw ContentValidationException("${item.location}.id: duplicate object ID '$idText'")
            }
            val kind = when (item.requiredString("kind")) {
                "steam" -> AtmosphericEmitterKind.STEAM
                "rooftopExhaust" -> AtmosphericEmitterKind.ROOFTOP_EXHAUST
                else -> throw ContentValidationException("${item.location}.kind: unknown atmospheric emitter kind")
            }
            val position = WorldPosition(item.requiredDouble("x"), item.requiredDouble("z"))
            if (!grid.contains(position.toGridCell())) {
                throw ContentValidationException("${item.location}: atmospheric emitter is outside map bounds ${grid.width}x${grid.depth}")
            }
            val hostCell = grid.cellAt(position.toGridCell())
            val baseHeight = when (kind) {
                AtmosphericEmitterKind.STEAM -> {
                    if (hostCell.solid) {
                        throw ContentValidationException("${item.location}: steam emitter must occupy a non-solid cell")
                    }
                    0.05
                }
                AtmosphericEmitterKind.ROOFTOP_EXHAUST -> {
                    if (!hostCell.opaque || "building" !in hostCell.tags) {
                        throw ContentValidationException("${item.location}: rooftop exhaust must occupy an opaque building cell")
                    }
                    hostCell.height + 0.05
                }
            }
            val riseHeight = item.requiredDouble("riseHeight")
            val drift = item.requiredDouble("drift")
            val period = item.requiredDouble("periodSeconds")
            val phase = item.optionalDouble("phaseOffset", 0.0)
            if (riseHeight !in AtmosphericEmitter.MINIMUM_RISE..AtmosphericEmitter.MAXIMUM_RISE) {
                throw ContentValidationException(
                    "${item.location}.riseHeight: must be between ${AtmosphericEmitter.MINIMUM_RISE} and ${AtmosphericEmitter.MAXIMUM_RISE}",
                )
            }
            if (drift !in 0.0..AtmosphericEmitter.MAXIMUM_DRIFT) {
                throw ContentValidationException(
                    "${item.location}.drift: must be between zero and ${AtmosphericEmitter.MAXIMUM_DRIFT}",
                )
            }
            if (period !in AtmosphericEmitter.MINIMUM_PERIOD..AtmosphericEmitter.MAXIMUM_PERIOD) {
                throw ContentValidationException(
                    "${item.location}.periodSeconds: must be between ${AtmosphericEmitter.MINIMUM_PERIOD} and ${AtmosphericEmitter.MAXIMUM_PERIOD}",
                )
            }
            if (phase < 0.0 || phase >= 1.0) {
                throw ContentValidationException("${item.location}.phaseOffset: must be at least zero and less than one")
            }
            AtmosphericEmitter(idText, kind, position, baseHeight, riseHeight, drift, period, phase)
        }
        if (atmosphericEmitters.size > AtmosphericEmitter.MAXIMUM_EMITTERS) {
            throw ContentValidationException(
                "$source:$.atmosphericEmitters: must contain at most ${AtmosphericEmitter.MAXIMUM_EMITTERS} emitters",
            )
        }

        val ambientEventEmitters = root.optionalArray("ambientEventEmitters").mapIndexed { index, value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf(
                    "id",
                    "kind",
                    "x",
                    "z",
                    "height",
                    "minimumIntervalSeconds",
                    "maximumIntervalSeconds",
                    "durationSeconds",
                    "audibleRange",
                ),
            )
            val idText = item.requiredString("id")
            ContentIds.requireStable(idText, source, "$.ambientEventEmitters[$index].id")
            if (!ids.add(idText)) {
                throw ContentValidationException("${item.location}.id: duplicate object ID '$idText'")
            }
            val kind = when (item.requiredString("kind")) {
                "ventHiss" -> AmbientEventKind.VENT_HISS
                "electricSpark" -> AmbientEventKind.ELECTRIC_SPARK
                "distantHorn" -> AmbientEventKind.DISTANT_HORN
                else -> throw ContentValidationException("${item.location}.kind: unknown ambient event kind")
            }
            val position = WorldPosition(item.requiredDouble("x"), item.requiredDouble("z"))
            if (!grid.contains(position.toGridCell())) {
                throw ContentValidationException("${item.location}: ambient event emitter is outside map bounds ${grid.width}x${grid.depth}")
            }
            val height = item.requiredDouble("height")
            val minimumInterval = item.requiredDouble("minimumIntervalSeconds")
            val maximumInterval = item.requiredDouble("maximumIntervalSeconds")
            val duration = item.requiredDouble("durationSeconds")
            val audibleRange = item.requiredDouble("audibleRange")
            if (height !in 0.0..AmbientEventEmitter.MAXIMUM_HEIGHT) {
                throw ContentValidationException(
                    "${item.location}.height: must be between zero and ${AmbientEventEmitter.MAXIMUM_HEIGHT}",
                )
            }
            if (minimumInterval !in AmbientEventEmitter.MINIMUM_INTERVAL..AmbientEventEmitter.MAXIMUM_INTERVAL) {
                throw ContentValidationException(
                    "${item.location}.minimumIntervalSeconds: must be between " +
                        "${AmbientEventEmitter.MINIMUM_INTERVAL} and ${AmbientEventEmitter.MAXIMUM_INTERVAL}",
                )
            }
            if (maximumInterval !in minimumInterval..AmbientEventEmitter.MAXIMUM_INTERVAL) {
                throw ContentValidationException(
                    "${item.location}.maximumIntervalSeconds: must be at least minimumIntervalSeconds and at most " +
                        AmbientEventEmitter.MAXIMUM_INTERVAL,
                )
            }
            if (duration !in AmbientEventEmitter.MINIMUM_DURATION..AmbientEventEmitter.MAXIMUM_DURATION) {
                throw ContentValidationException(
                    "${item.location}.durationSeconds: must be between " +
                        "${AmbientEventEmitter.MINIMUM_DURATION} and ${AmbientEventEmitter.MAXIMUM_DURATION}",
                )
            }
            if (audibleRange !in AmbientEventEmitter.MINIMUM_RANGE..AmbientEventEmitter.MAXIMUM_RANGE) {
                throw ContentValidationException(
                    "${item.location}.audibleRange: must be between " +
                        "${AmbientEventEmitter.MINIMUM_RANGE} and ${AmbientEventEmitter.MAXIMUM_RANGE}",
                )
            }
            AmbientEventEmitter(
                idText,
                kind,
                position,
                height,
                minimumInterval,
                maximumInterval,
                duration,
                audibleRange,
            )
        }
        if (ambientEventEmitters.size > AmbientEventEmitter.MAXIMUM_EMITTERS) {
            throw ContentValidationException(
                "$source:$.ambientEventEmitters: must contain at most ${AmbientEventEmitter.MAXIMUM_EMITTERS} emitters",
            )
        }

        val ambientActors = root.optionalArray("ambientActors").mapIndexed { index, value ->
            val item = value.asObject()
            item.rejectUnknown(
                setOf("id", "spriteId", "radius", "speedCellsPerSecond", "waitSeconds", "startDelaySeconds", "waypoints"),
            )
            val idText = item.requiredString("id")
            ContentIds.requireStable(idText, source, "$.ambientActors[$index].id")
            if (!ids.add(idText)) {
                throw ContentValidationException("${item.location}.id: duplicate object ID '$idText'")
            }
            val spriteId = item.requiredString("spriteId")
            ContentIds.requireStable(spriteId, source, "${item.location}.spriteId")
            if (!sprites.contains(spriteId)) {
                throw ContentValidationException("${item.location}.spriteId: unknown ASCII sprite '$spriteId'")
            }
            val radius = item.optionalDouble("radius", 0.22)
            if (radius !in 0.15..0.35) {
                throw ContentValidationException("${item.location}.radius: must be between 0.15 and 0.35")
            }
            val speed = item.requiredDouble("speedCellsPerSecond")
            val wait = item.optionalDouble("waitSeconds", 0.0)
            val startDelay = item.optionalDouble("startDelaySeconds", 0.0)
            if (speed !in AmbientActor.MINIMUM_SPEED..AmbientActor.MAXIMUM_SPEED) {
                throw ContentValidationException(
                    "${item.location}.speedCellsPerSecond: must be between ${AmbientActor.MINIMUM_SPEED} and ${AmbientActor.MAXIMUM_SPEED}",
                )
            }
            if (wait !in 0.0..AmbientActor.MAXIMUM_WAIT) {
                throw ContentValidationException(
                    "${item.location}.waitSeconds: must be between zero and ${AmbientActor.MAXIMUM_WAIT}",
                )
            }
            if (startDelay !in 0.0..AmbientActor.MAXIMUM_START_DELAY) {
                throw ContentValidationException(
                    "${item.location}.startDelaySeconds: must be between zero and ${AmbientActor.MAXIMUM_START_DELAY}",
                )
            }
            val waypoints = item.requiredArray("waypoints").map { waypointValue ->
                val waypoint = waypointValue.asObject()
                waypoint.rejectUnknown(setOf("x", "z"))
                WorldPosition(waypoint.requiredDouble("x"), waypoint.requiredDouble("z")).also { position ->
                    if (!grid.contains(position.toGridCell())) {
                        throw ContentValidationException("${waypoint.location}: waypoint is outside map bounds ${grid.width}x${grid.depth}")
                    }
                    if ("walkable" !in grid.cellAt(position.toGridCell()).tags) {
                        throw ContentValidationException("${waypoint.location}: waypoint must occupy a walkable cell")
                    }
                    if (!isGridPositionFree(grid, position, radius)) {
                        throw ContentValidationException("${waypoint.location}: waypoint does not have radius clearance from solid cells")
                    }
                }
            }
            if (waypoints.size !in AmbientActor.MINIMUM_WAYPOINTS..AmbientActor.MAXIMUM_WAYPOINTS) {
                throw ContentValidationException(
                    "${item.location}.waypoints: must contain between ${AmbientActor.MINIMUM_WAYPOINTS} and ${AmbientActor.MAXIMUM_WAYPOINTS} waypoints",
                )
            }
            for (waypointIndex in 0 until waypoints.lastIndex) {
                validateAmbientRouteSegment(
                    grid,
                    waypoints[waypointIndex],
                    waypoints[waypointIndex + 1],
                    radius,
                    "${item.location}.waypoints[$waypointIndex]",
                )
            }
            val start = waypoints.first()
            val overlappingEntity = entities.firstOrNull { entity ->
                entity.blocking && hypot(entity.position.x - start.x, entity.position.z - start.z) <
                    entity.collisionRadius + radius
            }
            if (overlappingEntity != null) {
                throw ContentValidationException(
                    "${item.location}.waypoints[0]: start overlaps blocking object '${overlappingEntity.id}'",
                )
            }
            val entityId = EntityId(idText)
            entities += WorldEntity(
                id = entityId,
                type = WorldEntityType.AMBIENT_ACTOR,
                position = start,
                blocking = true,
                collisionRadius = radius,
                interactionId = null,
                displayText = null,
            )
            AmbientActor(entityId, spriteId, speed, wait, startDelay, waypoints)
        }
        if (ambientActors.size > AmbientActor.MAXIMUM_ACTORS) {
            throw ContentValidationException(
                "$source:$.ambientActors: must contain at most ${AmbientActor.MAXIMUM_ACTORS} actors",
            )
        }
        return ParsedMapObjects(
            entities,
            npcs.toList(),
            trafficCars,
            facadeSigns + facadeEntrances,
            atmosphericEmitters,
            ambientEventEmitters,
            ambientActors,
        )
    }

    private fun validateAmbientRouteSegment(
        grid: WorldGrid,
        start: WorldPosition,
        end: WorldPosition,
        radius: Double,
        location: String,
    ) {
        val deltaX = end.x - start.x
        val deltaZ = end.z - start.z
        val distance = hypot(deltaX, deltaZ)
        if (distance <= 0.0) {
            throw ContentValidationException("$location: must not repeat the next waypoint")
        }
        if (abs(deltaX) > 1e-9 && abs(deltaZ) > 1e-9) {
            throw ContentValidationException("$location: route segment to the next waypoint must be axis-aligned")
        }
        if (distance > AmbientActor.MAXIMUM_SEGMENT_LENGTH) {
            throw ContentValidationException(
                "$location: route segment must be at most ${AmbientActor.MAXIMUM_SEGMENT_LENGTH} cells",
            )
        }
        val samples = ceil(distance / 0.1).toInt().coerceAtLeast(1)
        for (sample in 0..samples) {
            val progress = sample.toDouble() / samples
            val position = WorldPosition(start.x + deltaX * progress, start.z + deltaZ * progress)
            val cell = grid.cellAt(position.toGridCell())
            if ("walkable" !in cell.tags || !isGridPositionFree(grid, position, radius)) {
                throw ContentValidationException("$location: route segment crosses a non-walkable or obstructed cell")
            }
        }
    }

    private fun isGridPositionFree(grid: WorldGrid, position: WorldPosition, radius: Double): Boolean {
        if (
            position.x - radius < 0.0 || position.z - radius < 0.0 ||
            position.x + radius > grid.width || position.z + radius > grid.depth
        ) return false
        val minimumX = floor(position.x - radius).toInt()
        val maximumX = floor(position.x + radius).toInt()
        val minimumZ = floor(position.z - radius).toInt()
        val maximumZ = floor(position.z + radius).toInt()
        val radiusSquared = radius * radius
        for (z in minimumZ..maximumZ) for (x in minimumX..maximumX) {
            if (!grid.cellAt(x, z).solid) continue
            val closestX = position.x.coerceIn(x.toDouble(), x + 1.0)
            val closestZ = position.z.coerceIn(z.toDouble(), z + 1.0)
            val deltaX = position.x - closestX
            val deltaZ = position.z - closestZ
            if (deltaX * deltaX + deltaZ * deltaZ < radiusSquared - 1e-12) return false
        }
        return true
    }

    private fun validateFacadeSignGlyphRows(rows: List<String>, location: String) {
        if (rows.size !in 2..12) {
            throw ContentValidationException("$location: must contain between 2 and 12 rows")
        }
        val columns = rows.first().length
        if (columns !in 3..64) {
            throw ContentValidationException("$location[0]: must contain between 3 and 64 glyphs")
        }
        for ((rowIndex, row) in rows.withIndex()) {
            if (row.length != columns) {
                throw ContentValidationException(
                    "$location[$rowIndex]: ragged row has width ${row.length}; expected $columns",
                )
            }
            if (row.any { it.code !in 32..126 }) {
                throw ContentValidationException("$location[$rowIndex]: must contain printable ASCII only")
            }
        }
    }

    private fun validateFacadeSignPlacement(
        grid: WorldGrid,
        sourceLocation: String,
        id: String,
        face: FacadeFace,
        cellX: Int,
        cellZ: Int,
        widthCells: Int,
        bottom: Double,
        top: Double,
        occupiedFaces: MutableMap<String, MutableList<FacadeOverlaySpan>>,
        overlayKind: String = "sign",
    ) {
        var offset = 0
        while (offset < widthCells) {
            val x = if (face == FacadeFace.NORTH || face == FacadeFace.SOUTH) cellX + offset else cellX
            val z = if (face == FacadeFace.EAST || face == FacadeFace.WEST) cellZ + offset else cellZ
            if (!grid.contains(x, z)) {
                throw ContentValidationException(
                    "$sourceLocation: facade $overlayKind '$id' extends outside map bounds ${grid.width}x${grid.depth}",
                )
            }
            val definition = grid.cellAt(x, z)
            if (!definition.opaque || "building" !in definition.tags) {
                throw ContentValidationException(
                    "$sourceLocation: facade $overlayKind '$id' must attach only to opaque building cells",
                )
            }
            if (top > definition.height) {
                throw ContentValidationException(
                    "$sourceLocation.height: facade $overlayKind '$id' top $top exceeds building height ${definition.height}",
                )
            }
            val neighborX = when (face) {
                FacadeFace.EAST -> x + 1
                FacadeFace.WEST -> x - 1
                else -> x
            }
            val neighborZ = when (face) {
                FacadeFace.NORTH -> z + 1
                FacadeFace.SOUTH -> z - 1
                else -> z
            }
            if (!grid.contains(neighborX, neighborZ) || grid.cellAt(neighborX, neighborZ).opaque) {
                throw ContentValidationException(
                    "$sourceLocation.face: facade $overlayKind '$id' must face an open in-bounds exterior cell",
                )
            }
            val faceKey = "$x:$z:${face.name}"
            val overlays = occupiedFaces.getOrPut(faceKey) { mutableListOf() }
            if (overlays.any { existing -> bottom < existing.top && top > existing.bottom }) {
                throw ContentValidationException(
                    "$sourceLocation: facade $overlayKind '$id' overlaps another facade overlay on the same wall face",
                )
            }
            overlays += FacadeOverlaySpan(bottom, top)
            offset += 1
        }
    }

    private data class FacadeOverlaySpan(val bottom: Double, val top: Double)

    private fun validateSpawn(
        metadata: MapMetadata,
        width: Int,
        depth: Int,
        isSolid: (Double, Double) -> Boolean,
    ) {
        val position = metadata.playerSpawn.position
        if (position.x < 0.0 || position.z < 0.0 || position.x >= width || position.z >= depth) {
            throw ContentValidationException("maps/${metadata.id}/${metadata.id}.meta.json:$.spawn: position is outside map bounds ${width}x$depth")
        }
        if (isSolid(position.x, position.z)) {
            throw ContentValidationException("maps/${metadata.id}/${metadata.id}.meta.json:$.spawn: position occupies a solid cell")
        }
    }

    private fun validateInteractionReachability(
        grid: com.asciicity.world.WorldGrid,
        player: PlayerState,
        entities: List<WorldEntity>,
        source: String,
    ) {
        val reachable = WorldReachability.reachableCells(grid, player.position.toGridCell())
        for (entity in entities) {
            if (entity.type in setOf(WorldEntityType.NPC, WorldEntityType.TERMINAL) && entity.position.toGridCell() !in reachable) {
                throw ContentValidationException("$source:$: interactable '${entity.id}' is unreachable from the player spawn")
            }
        }
    }

    private fun parseAnimation(value: String, location: String): SurfaceAnimation = when (value) {
        "static" -> SurfaceAnimation.STATIC
        "dataStream" -> SurfaceAnimation.DATA_STREAM
        "signalPulse" -> SurfaceAnimation.SIGNAL_PULSE
        "glitchFlicker" -> SurfaceAnimation.GLITCH_FLICKER
        else -> throw ContentValidationException("$location.animation: unknown animation '$value'")
    }

    private data class DestinationSpawn(val position: WorldPosition, val yawDegrees: Double?)

    private fun parseEntityType(value: String, location: String): WorldEntityType = when (value) {
        "npc" -> WorldEntityType.NPC
        "door" -> WorldEntityType.DOOR
        "terminal" -> WorldEntityType.TERMINAL
        "lamp" -> WorldEntityType.LAMP
        else -> throw ContentValidationException("$location.type: unknown object type '$value'")
    }

    private fun parseFacadeFace(value: String, location: String): FacadeFace = when (value) {
        "north" -> FacadeFace.NORTH
        "south" -> FacadeFace.SOUTH
        "east" -> FacadeFace.EAST
        "west" -> FacadeFace.WEST
        else -> throw ContentValidationException("$location.face: unknown facade face '$value'")
    }

    private fun parseFacadeLighting(value: String, location: String): com.asciicity.render.FacadeLighting = when (value) {
        "marquee" -> com.asciicity.render.FacadeLighting.MARQUEE
        "amberPulse" -> com.asciicity.render.FacadeLighting.AMBER_PULSE
        "clubStrobe" -> com.asciicity.render.FacadeLighting.CLUB_STROBE
        else -> throw ContentValidationException("$location.lighting: unknown facade lighting '$value'")
    }

    private fun defaultCollisionRadius(type: WorldEntityType): Double = when (type) {
        WorldEntityType.NPC -> 0.24
        WorldEntityType.AMBIENT_ACTOR -> 0.22
        WorldEntityType.DOOR -> 0.45
        WorldEntityType.TERMINAL -> 0.24
        WorldEntityType.LAMP -> 0.0
    }

    private fun requirePaletteKey(palette: RenderPalette, key: String, source: String, path: String) {
        try {
            palette[key]
        } catch (_: PaletteValidationException) {
            throw ContentValidationException("$source:$path: unknown palette key '$key'")
        }
    }

    private fun resolveObjectSource(value: String, metadataSource: String): String {
        if (!value.matches(Regex("[a-z0-9][a-z0-9._-]*\\.json"))) {
            throw ContentValidationException("$metadataSource:$.objectsFile: must name one JSON file inside maps/")
        }
        return "maps/${metadataSource.substringAfter("maps/").substringBefore('/')}/$value"
    }

    private fun read(root: Path, relativePath: String): String {
        val path = root.resolve(relativePath).normalize()
        if (!path.startsWith(root)) {
            throw ContentValidationException("$relativePath:$: path escapes the asset root")
        }
        return try {
            Files.readString(path)
        } catch (error: Exception) {
            throw ContentValidationException("$relativePath:$: could not read file: ${error.message}", error)
        }
    }

    private data class ParsedMapObjects(
        val entities: List<WorldEntity>,
        val npcs: List<Npc>,
        val trafficCars: List<TrafficCar>,
        val facadeSigns: List<FacadeSign>,
        val atmosphericEmitters: List<AtmosphericEmitter>,
        val ambientEventEmitters: List<AmbientEventEmitter>,
        val ambientActors: List<AmbientActor>,
    )
}
