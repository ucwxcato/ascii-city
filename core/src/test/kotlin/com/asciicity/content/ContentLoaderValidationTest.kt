package com.asciicity.content

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class ContentLoaderValidationTest {
    @TempDir
    lateinit var temporaryDirectory: Path

    @Test
    fun `missing and duplicate spawn plus malformed metadata identify exact json paths`() {
        val root = copyFixture("spawn-errors")
        val metadataPath = root.resolve("maps/city-alpha/city-alpha.meta.json")

        Files.writeString(metadataPath, metadataWithoutSpawn())
        assertEquals(
            "maps/city-alpha/city-alpha.meta.json:$.spawn: required field is missing",
            loadFailure(root).message,
        )

        Files.writeString(metadataPath, metadataWithDuplicateSpawn())
        assertEquals(
            "maps/city-alpha/city-alpha.meta.json:$.spawn: duplicate field",
            loadFailure(root).message,
        )

        Files.writeString(metadataPath, "{")
        assertTrue(loadFailure(root).message.orEmpty().startsWith("maps/city-alpha/city-alpha.meta.json:$: malformed JSON:"))
    }

    @Test
    fun `optional ceiling metadata validates height and material references`() {
        val root = copyFixture("ceiling-errors")
        val metadataPath = root.resolve("maps/city-alpha/city-alpha.meta.json")
        val valid = Files.readString(metadataPath)
        val withCeiling = valid.replaceFirst(
            "  \"objectsFile\":",
            "  \"ceiling\": { \"height\": 7.0, \"material\": \"ceiling.water-park\" },\n  \"objectsFile\":",
        )
        check(withCeiling != valid)

        Files.writeString(metadataPath, withCeiling)
        val ceiling = requireNotNull(ContentLoader().load(root, "city-alpha").metadata.ceiling)
        assertEquals(7.0, ceiling.height)
        assertEquals("ceiling.water-park", ceiling.materialId)

        Files.writeString(metadataPath, withCeiling.replaceFirst("\"height\": 7.0", "\"height\": 0.6"))
        assertEquals(
            "maps/city-alpha/city-alpha.meta.json:$.ceiling.height: must be at least 0.5 units above spawn eye height 0.6",
            loadFailure(root).message,
        )

        Files.writeString(metadataPath, withCeiling.replaceFirst("ceiling.water-park", "ceiling.missing"))
        assertEquals(
            "maps/city-alpha/city-alpha.meta.json:$.ceiling.material: unknown material 'ceiling.missing'",
            loadFailure(root).message,
        )
    }

    @Test
    fun `invalid height material and missing cell fields fail before world construction`() {
        val root = copyFixture("cell-errors")
        val cellsPath = root.resolve("data/cells.json")
        val valid = Files.readString(cellsPath)

        Files.writeString(cellsPath, valid.replaceFirst("\"height\": 6", "\"height\": -1"))
        assertEquals(
            "data/cells.json:$.cells[19].height: must be non-negative",
            loadFailure(root).message,
        )

        Files.writeString(cellsPath, valid.replaceFirst("\"facade.low\"", "\"facade.missing\""))
        assertEquals(
            "data/cells.json:$.cells[25].facadeMaterial: unknown material 'facade.missing'",
            loadFailure(root).message,
        )

        val missingField = valid.replaceFirst(
            Regex("""(?m)^      "facadeMaterial": "facade\.low",\r?\n"""),
            "",
        )
        check(missingField != valid)
        Files.writeString(cellsPath, missingField)
        assertEquals(
            "data/cells.json:$.cells[25].facadeMaterial: required field is missing",
            loadFailure(root).message,
        )
    }

    @Test
    fun `out-of-bounds and duplicate objects fail with object paths`() {
        val root = copyFixture("object-errors")
        val objectsPath = root.resolve("maps/city-alpha/city-alpha.objects.json")
        val valid = Files.readString(objectsPath)

        Files.writeString(objectsPath, valid.replaceFirst("\"x\": 45.5", "\"x\": 145.5"))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0]: object 'npc.rin' is outside map bounds 96x96",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"terminal.archive\"", "\"npc.rin\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[2].id: duplicate object ID 'npc.rin'",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"radius\": 0.24", "\"radius\": -0.24"))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].radius: must be positive for blocking objects and non-negative otherwise",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"radius\": 0.24", "\"radius\": 0.0"))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].radius: must be positive for blocking objects and non-negative otherwise",
            loadFailure(root).message,
        )
    }

    @Test
    fun `facade signs validate layout palette and exterior placement`() {
        val root = copyFixture("facade-sign-errors")
        val objectsPath = root.resolve("maps/city-alpha/city-alpha.objects.json")
        val valid = Files.readString(objectsPath)

        val loaded = ContentLoader().load(root, "city-alpha")
        assertEquals(
            listOf(
                "sign.last-light-casino",
                "sign.signal-lost",
                "sign.lucky-seven-synthetics",
                "prop.vending-archive",
                "prop.noodle-market",
                "prop.security-panel",
                "entrance.last-light-casino",
                "entrance.archive",
            ),
            loaded.facadeSigns.map { it.id },
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"lighting\": \"clubStrobe\"", "\"lighting\": \"sunrise\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.facadeEntrances[0].lighting: unknown facade lighting 'sunrise'",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"face\": \"north\"", "\"face\": \"ceiling\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.facadeSigns[0].face: unknown facade face 'ceiling'",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"borderPalette\": \"signGold\"", "\"borderPalette\": \"missing\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.facadeSigns[0].borderPalette: unknown palette key 'missing'",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst(
                "|                        . . . . . . .                         |",
                "|                       . . . . . . .                         |",
            ),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.facadeSigns[0].glyphRows[4]: ragged row has width 63; expected 64",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"cellZ\": 3", "\"cellZ\": 2"))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.facadeSigns[0].face: facade sign 'sign.last-light-casino' " +
                "must face an open in-bounds exterior cell",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst("\"label\": \"LAST LIGHT CASINO\"", "\"label\": \"   \""),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.facadeSigns[0].label: must be non-blank printable ASCII and at most 32 glyphs",
            loadFailure(root).message,
        )
    }

    @Test
    fun `ascii sprites validate static layout colors transparency and physical size`() {
        val root = copyFixture("sprite-errors")
        val spritesPath = root.resolve("data/sprites.json")
        val valid = Files.readString(spritesPath)

        val loaded = ContentLoader().load(root, "city-alpha")
        assertEquals(
            listOf(
                "sprite.rin",
                "sprite.terminal",
                "sprite.door",
                "sprite.car.taxi",
                "sprite.car.sedan",
                "sprite.car.van",
                "sprite.pedestrian.coat",
                "sprite.pedestrian.umbrella",
                "sprite.pedestrian.courier",
            ),
            loaded.sprites.all().map { it.id },
        )

        Files.writeString(spritesPath, valid.replaceFirst("\"sprite.rin\"", "\"Sprite Rin\""))
        assertEquals(
            "data/sprites.json:$.sprites[0].id: 'Sprite Rin' must contain only lowercase letters, numbers, dots, underscores, or hyphens",
            loadFailure(root).message,
        )

        Files.writeString(spritesPath, valid.replaceFirst("\" nnnnnn  \"", "\" nnnnnn \""))
        assertEquals(
            "data/sprites.json:$.sprites[0].colorRows[9]: ragged row has width 8; expected 9",
            loadFailure(root).message,
        )

        Files.writeString(spritesPath, valid.replaceFirst("\"palette\": \"npc\"", "\"palette\": \"missing\""))
        assertEquals(
            "data/sprites.json:$.sprites[0].colors[2].palette: unknown palette key 'missing'",
            loadFailure(root).message,
        )

        Files.writeString(spritesPath, valid.replaceFirst("\"   cc    \"", "\"   zz    \""))
        assertEquals(
            "data/sprites.json:$.sprites[0].colorRows[0] column 4: unknown color symbol 'z'",
            loadFailure(root).message,
        )

        Files.writeString(spritesPath, valid.replaceFirst("\"physicalWidth\": 0.8", "\"physicalWidth\": 0.0"))
        assertEquals(
            "data/sprites.json:$.sprites[0].physicalWidth: must be positive and at most 16.0",
            loadFailure(root).message,
        )

        Files.writeString(spritesPath, valid.replaceFirst("\"anchor\": { \"x\": 0.5", "\"anchor\": { \"x\": 1.5"))
        assertEquals(
            "data/sprites.json:$.sprites[0].anchor.x: must be between zero and one",
            loadFailure(root).message,
        )

        Files.writeString(spritesPath, valid.replaceFirst("\"   cc    \"", "\"  ccc    \""))
        assertEquals(
            "data/sprites.json:$.sprites[0].colorRows[0] column 3: transparent glyph space must use a transparent color space",
            loadFailure(root).message,
        )
    }

    @Test
    fun `npc and terminal definitions require valid display and dialogue metadata`() {
        val root = copyFixture("npc-errors")
        val objectsPath = root.resolve("maps/city-alpha/city-alpha.objects.json")
        val valid = Files.readString(objectsPath)

        val loaded = ContentLoader().load(root, "city-alpha")
        val rin = loaded.npcs.single()
        assertEquals("npc.rin", rin.entityId.value)
        assertEquals("sprite.rin", rin.spriteId)
        assertEquals(270.0, rin.facingDegrees)
        assertEquals("Rin", rin.displayName)
        assertEquals("dialogue.rin.intro", rin.dialogueId)

        Files.writeString(
            objectsPath,
            valid.replaceFirst(Regex("""(?m)^      "spriteId": "sprite\.rin",\r?\n"""), ""),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].spriteId: required field is missing",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"sprite.rin\"", "\"sprite.missing\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].spriteId: unknown ASCII sprite 'sprite.missing'",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"facingDegrees\": 270.0", "\"facingDegrees\": 360.0"))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].facingDegrees: must be at least zero and less than 360",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst(Regex("""(?m)^      "blocking": true,\r?\n"""), ""),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].blocking: required field is missing",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"displayText\": \"Rin\"", "\"displayText\": \"   \""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].displayText: must not be blank for npc",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst("\"interactionId\": \"dialogue.rin.intro\"", "\"interactionId\": \"dialogue.missing\""),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].interactionId: unknown dialogue 'dialogue.missing'",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"displayText\": \"Rin\"", "\"displayText\": \"Courier\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[0].displayText: must match dialogue " +
                "'dialogue.rin.intro' speaker 'Rin'",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst("\"interactionId\": \"terminal.relay-01\"", "\"interactionId\": \"terminal.missing\""),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[1].interactionId: unknown dialogue 'terminal.missing'",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst("\"displayText\": \"CASCADE RELAY\"", "\"displayText\": \"CASCADE NODE\""),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[1].displayText: must match dialogue " +
                "'terminal.relay-01' speaker 'CASCADE RELAY'",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst(
                Regex("""(?m)^      "type": "terminal",\r?\n      "x": 2\.5,"""),
                "      \"type\": \"terminal\",\n      \"spriteId\": \"sprite.rin\",\n      \"x\": 2.5,",
            ),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[1].spriteId: unknown field",
            loadFailure(root).message,
        )
    }

    @Test
    fun `city life emitters and pedestrian routes fail with exact object paths`() {
        val root = copyFixture("city-life-errors")
        val objectsPath = root.resolve("maps/city-alpha/city-alpha.objects.json")
        val valid = Files.readString(objectsPath)

        Files.writeString(objectsPath, valid.replaceFirst("\"kind\": \"rooftopExhaust\"", "\"kind\": \"missing\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.atmosphericEmitters[3].kind: unknown atmospheric emitter kind",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst("{ \"x\": 44.5, \"z\": 28.5 }", "{ \"x\": 45.5, \"z\": 28.5 }"),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.ambientActors[0].waypoints[0]: " +
                "route segment to the next waypoint must be axis-aligned",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst("\"minimumIntervalSeconds\": 5.0", "\"minimumIntervalSeconds\": 1.0"),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.ambientEventEmitters[0].minimumIntervalSeconds: " +
                "must be between 2.0 and 60.0",
            loadFailure(root).message,
        )
    }

    @Test
    fun `presentation tuning and reduced flicker limits fail with exact paths`() {
        val root = copyFixture("presentation-errors")
        val presentationPath = root.resolve("data/presentation.json")
        val validPresentation = Files.readString(presentationPath)

        Files.writeString(presentationPath, validPresentation.replaceFirst("\"fogEndDistance\": 48.0", "\"fogEndDistance\": 5.0"))
        assertEquals(
            "data/presentation.json:$.fogEndDistance: must be finite and greater than fogStartDistance",
            loadFailure(root).message,
        )

        Files.writeString(
            presentationPath,
            validPresentation.replaceFirst("\"fogEndGlyphDensity\": 0.38", "\"fogEndGlyphDensity\": 1.1"),
        )
        assertEquals(
            "data/presentation.json:$.fogEndGlyphDensity: must be between zero and one",
            loadFailure(root).message,
        )

        Files.writeString(presentationPath, validPresentation)
        val materialsPath = root.resolve("data/materials.json")
        val materials = Files.readString(materialsPath)
        Files.writeString(materialsPath, materials.replaceFirst("\"animationRate\": 3.5", "\"animationRate\": 6.5"))
        assertEquals(
            "data/materials.json:$.materials[2].animationRate: glitchFlicker must not exceed " +
                "presentation maximumGlitchRate 6.0",
            loadFailure(root).message,
        )

        Files.delete(presentationPath)
        assertTrue(loadFailure(root).message.orEmpty().startsWith("data/presentation.json:$: could not read file:"))
    }

    @Test
    fun `door travel destination must name a playable map`() {
        val root = copyFixture("door-destination-errors")
        val objectsPath = root.resolve("maps/city-alpha/city-alpha.objects.json")
        val valid = Files.readString(objectsPath)

        Files.writeString(objectsPath, valid.replaceFirst("\"destinationMapId\": \"casino\"", "\"destinationMapId\": \"missing-map\""))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.objects[3].destinationMapId: unknown playable map 'missing-map'",
            loadFailure(root).message,
        )
    }

    @Test
    fun `traffic cars require known sprites safe road waypoints and restrained speeds`() {
        val root = copyFixture("traffic-errors")
        val objectsPath = root.resolve("maps/city-alpha/city-alpha.objects.json")
        val valid = Files.readString(objectsPath)

        assertEquals(6, ContentLoader().load(root, "city-alpha").trafficCars.size)

        Files.writeString(objectsPath, valid.replaceFirst("sprite.car.taxi", "sprite.car.missing"))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.trafficCars[0].spriteId: unknown ASCII sprite 'sprite.car.missing'",
            loadFailure(root).message,
        )

        Files.writeString(objectsPath, valid.replaceFirst("\"speedCellsPerSecond\": 2.1", "\"speedCellsPerSecond\": 4.1"))
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.trafficCars[0].speedCellsPerSecond: must be between 0.1 and 4.0",
            loadFailure(root).message,
        )

        Files.writeString(
            objectsPath,
            valid.replaceFirst("{ \"x\": 16.5, \"z\": 10.5 }", "{ \"x\": 20.5, \"z\": 11.5 }"),
        )
        assertEquals(
            "maps/city-alpha/city-alpha.objects.json:$.trafficCars[0].waypoints[0]: waypoint must occupy a road cell",
            loadFailure(root).message,
        )
    }

    @Test
    fun `optional life pass arrays may all be omitted without changing base world loading`() {
        val root = copyFixture("life-pass-optional-arrays")
        Files.writeString(root.resolve("maps/city-alpha/city-alpha.objects.json"), """{ "objects": [] }""")

        val loaded = ContentLoader().load(root, "city-alpha")

        assertTrue(loaded.world.allEntities().isEmpty())
        assertTrue(loaded.trafficCars.isEmpty())
        assertTrue(loaded.facadeSigns.isEmpty())
        assertTrue(loaded.atmosphericEmitters.isEmpty())
        assertTrue(loaded.ambientEventEmitters.isEmpty())
        assertTrue(loaded.ambientActors.isEmpty())
    }

    private fun loadFailure(root: Path): ContentValidationException = assertFailsWith {
        ContentLoader().load(root, "city-alpha")
    }

    private fun copyFixture(name: String): Path {
        val root = temporaryDirectory.resolve(name)
        copyTree(Path.of("data"), root.resolve("data"))
        copyTree(Path.of("maps"), root.resolve("maps"))
        return root
    }

    private fun copyTree(source: Path, target: Path) {
        target.createDirectories()
        Files.walk(source).use { paths ->
            paths.forEach { path ->
                val destination = target.resolve(source.relativize(path).toString())
                if (Files.isDirectory(path)) {
                    destination.createDirectories()
                } else {
                    Files.copy(path, destination)
                }
            }
        }
    }

    private fun metadataWithoutSpawn(): String = """
        {
          "id": "city-alpha",
          "version": 1,
          "skyPalette": "sky",
          "fogPalette": "fog",
          "objectsFile": "city-alpha.objects.json"
        }
    """.trimIndent()

    private fun metadataWithDuplicateSpawn(): String = """
        {
          "id": "city-alpha",
          "version": 1,
          "spawn": { "x": 48.5, "z": 4.5, "yawDegrees": 0.0 },
          "spawn": { "x": 48.5, "z": 4.5, "yawDegrees": 0.0 },
          "skyPalette": "sky",
          "fogPalette": "fog",
          "objectsFile": "city-alpha.objects.json"
        }
    """.trimIndent()

}
