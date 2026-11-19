package com.asciicity.screen

import com.asciicity.audio.AmbienceZone
import com.asciicity.audio.AudioService
import com.asciicity.audio.FootstepCadence
import com.asciicity.audio.FootstepSurface
import com.asciicity.audio.WaterfallAudioField
import com.asciicity.ambient.AmbientEventSystem
import com.asciicity.asset.GameAssets
import com.asciicity.content.LoadedContent
import com.asciicity.content.MapCatalog
import com.asciicity.input.GameCommand
import com.asciicity.input.InputController
import com.asciicity.input.InteractionVerificationScript
import com.asciicity.input.CasinoEntryVerificationScript
import com.asciicity.input.CasinoReturnVerificationScript
import com.asciicity.input.PresentationVerificationScript
import com.asciicity.input.WalkVerificationScript
import com.asciicity.render.AsciiFrame
import com.asciicity.render.AsciiFontProfile
import com.asciicity.render.AsciiPresenter
import com.asciicity.render.AsciiViewport
import com.asciicity.render.AsciiViewportCalculator
import com.asciicity.render.DoorBillboardRenderer
import com.asciicity.render.HeadBob
import com.asciicity.render.ProjectionMetrics
import com.asciicity.render.TerminalBillboardRenderer
import com.asciicity.render.ViewPose
import com.asciicity.render.WorldAsciiRenderer
import com.asciicity.system.FixedStepClock
import com.asciicity.system.InteractionOutcome
import com.asciicity.system.InteractionSystem
import com.asciicity.system.InteractionTarget
import com.asciicity.system.NpcSystem
import com.asciicity.system.PlayerSimulation
import com.asciicity.system.TrafficSystem
import com.asciicity.system.AmbientActorSystem
import com.asciicity.ui.DebugFootprint
import com.asciicity.ui.Hud
import com.asciicity.ui.HudModel
import com.asciicity.ui.HudText
import com.asciicity.ui.DialogueOverlay
import com.asciicity.util.GameLog
import com.asciicity.world.WorldPosition
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.OrthographicCamera
import java.security.MessageDigest
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot

class GameScreen(
    private val assets: GameAssets,
    private val content: LoadedContent,
    initialYawDegrees: Double? = null,
    initialPitchDegrees: Double? = null,
    verifyWalk: Boolean = false,
    verifyInteraction: Boolean = false,
    verifyCasinoEntry: Boolean = false,
    verifyCasinoReturn: Boolean = false,
    verifyPresentation: Boolean = false,
    audioEnabled: Boolean = true,
    private val headBobEnabled: Boolean = true,
    private val nextMapId: String? = null,
    private val onMapSwitchRequested: (String) -> Unit = {},
    private val onMapTravelRequested: (InteractionOutcome.EnterMap) -> Unit = {},
    initialDebugVisible: Boolean = false,
    initialPosition: WorldPosition? = null,
    private val worldFontProfile: AsciiFontProfile = AsciiFontProfile.DEFAULT,
) : ScreenAdapter() {
    private val camera = OrthographicCamera()
    private val presenter = AsciiPresenter(assets.worldFont(worldFontProfile), worldFontProfile)
    private val npcSystem = NpcSystem(content.world, content.npcs, content.sprites)
    private val trafficSystem = TrafficSystem(content.trafficCars, content.sprites)
    private val ambientActorSystem = AmbientActorSystem(content.world, content.ambientActors, content.sprites)
    private val ambientEventSystem = AmbientEventSystem(content.ambientEventEmitters)
    private val terminalBillboardRenderer = TerminalBillboardRenderer(content.world, content.sprites)
    private val doorBillboardRenderer = DoorBillboardRenderer(content.world, content.sprites)
    private val worldRenderer = WorldAsciiRenderer(
        content.world,
        content.materials,
        assets.palette,
        content.presentation,
        content.facadeSigns,
        trafficSystem = trafficSystem,
        npcSystem = npcSystem,
        ceiling = content.metadata.ceiling,
        terminalBillboardRenderer = terminalBillboardRenderer,
        doorBillboardRenderer = doorBillboardRenderer,
        atmosphericEmitters = content.atmosphericEmitters,
        ambientEventSystem = ambientEventSystem,
        ambientActorSystem = ambientActorSystem,
        fontProfile = worldFontProfile,
    )
    private val input = InputController()
    private val audio = AudioService.create(audioEnabled)
    private val waterfallAudioField = WaterfallAudioField.fromGrid(content.world.grid)
    private val footstepCadence = FootstepCadence()
    private val headBob = HeadBob()
    private val interactionSystem = InteractionSystem(content.world)
    private val hud = Hud(assets.font, assets.palette, audio::toggleMusicMuted)
    private val inputMultiplexer: InputMultiplexer
    private val dialogueOverlay = DialogueOverlay(assets.font, assets.palette)
    private val fixedStepClock = FixedStepClock()
    private val initialPlayer = content.world.player.copy(
        position = when {
            verifyInteraction -> InteractionVerificationScript.INITIAL_POSITION
            verifyCasinoEntry && content.world.mapId == MapCatalog.DEFAULT_MAP_ID -> CasinoEntryVerificationScript.INITIAL_POSITION
            verifyCasinoReturn && content.world.mapId == "casino" -> CasinoReturnVerificationScript.INITIAL_POSITION
            initialPosition != null -> initialPosition
            else -> content.world.player.position
        },
        yawRadians = ViewPose.normalizeYaw(
            initialYawDegrees?.let(Math::toRadians)
                ?: if (verifyInteraction) 0.0
                else if (verifyCasinoEntry && content.world.mapId == MapCatalog.DEFAULT_MAP_ID) Math.PI
                else if (verifyCasinoReturn && content.world.mapId == "casino") 0.0
                else content.world.player.yawRadians,
        ),
        pitchRadians = (
            initialPitchDegrees?.let(Math::toRadians)
                ?: if (verifyInteraction) 0.0 else content.world.player.pitchRadians
            )
            .coerceIn(-ViewPose.MAX_PITCH_RADIANS, ViewPose.MAX_PITCH_RADIANS),
    )
    private val simulation = PlayerSimulation(content.world, input, initialState = initialPlayer)
    private val walkVerification = WalkVerificationScript().takeIf { verifyWalk }
    private val interactionVerification = InteractionVerificationScript().takeIf { verifyInteraction }
    private val casinoEntryVerification = CasinoEntryVerificationScript().takeIf {
        verifyCasinoEntry && content.world.mapId == MapCatalog.DEFAULT_MAP_ID
    }
    private val casinoReturnVerification = CasinoReturnVerificationScript().takeIf {
        verifyCasinoReturn && content.world.mapId == "casino"
    }
    private val presentationVerification = PresentationVerificationScript().takeIf { verifyPresentation }
    private val initialSemanticWorldHash = semanticWorldHash().takeIf { verifyPresentation }
    private var presentationHeapStartBytes: Long? = null
    private var simulationStep = 0
    private var viewport: AsciiViewport? = null
    private var frame: AsciiFrame? = null
    private var currentTarget: InteractionTarget? = null
    private var previousTargetId: String? = null
    private var debugVisible = initialDebugVisible
    private var logRenderStats = true
    private var disposed = false

    init {
        inputMultiplexer = InputMultiplexer(input, hud.inputProcessor)
    }

    override fun render(delta: Float) {
        val activeViewport = viewport ?: return
        val activeFrame = frame ?: return
        val steps = fixedStepClock.addFrame(delta.toDouble().coerceAtLeast(0.0))
        repeat(steps) {
            walkVerification?.apply(simulationStep, input)?.let(::handleWalkVerificationEvent)
            interactionVerification?.apply(simulationStep, input)?.let(::handleInteractionVerificationEvent)
            casinoEntryVerification?.apply(simulationStep, input)
            casinoReturnVerification?.apply(simulationStep, input)
            presentationVerification?.apply(simulationStep, input)?.let(::handlePresentationVerificationEvent)
            val positionBeforeStep = simulation.currentState.position
            simulation.step(fixedStepClock.fixedDeltaSeconds)
            ambientActorSystem.step(fixedStepClock.fixedDeltaSeconds)
            ambientEventSystem.step(
                fixedStepClock.fixedDeltaSeconds,
                simulation.interpolatedPose(1.0),
            ) { event ->
                audio.playAmbient(event.cue, event.volume, event.pan, event.pitch)
            }
            val positionAfterStep = simulation.currentState.position
            val distanceMoved = hypot(
                positionAfterStep.x - positionBeforeStep.x,
                positionAfterStep.z - positionBeforeStep.z,
            )
            val surface = FootstepSurface.fromCellTags(
                content.world.grid.cellAt(positionAfterStep.toGridCell()).tags,
            )
            footstepCadence.update(distanceMoved, surface)?.let(audio::play)
            if (headBobEnabled) headBob.update(distanceMoved, fixedStepClock.fixedDeltaSeconds)
            simulationStep += 1
        }
        val playerPose = simulation.interpolatedPose(fixedStepClock.interpolationAlpha)
        val interactionPose = playerPose
        val renderPose = if (headBobEnabled) headBob.apply(interactionPose) else interactionPose
        handleCommands(interactionPose, activeViewport)
        audio.update(
            ambienceZone(),
            delta.coerceAtLeast(0f).coerceAtMost(0.25f),
            waterfallAudioField.mixAt(playerPose.position),
        )
        val renderTime = content.world.simulationTimeSeconds +
            fixedStepClock.interpolationAlpha * fixedStepClock.fixedDeltaSeconds
        worldRenderer.render(
            activeFrame,
            renderPose,
            renderTime,
            interpolationAlpha = fixedStepClock.interpolationAlpha,
        )
        hud.update(hudModel(playerPose, activeViewport))
        presenter.present(activeFrame, activeViewport, camera.combined)
        hud.render(delta)
        dialogueOverlay.render(delta)
        if (logRenderStats) {
            val stats = worldRenderer.stats
            GameLog.info(
                "GameScreen",
                "World frame rays=${stats.rayCount} steps=${stats.ddaSteps} " +
                    "walls=${stats.wallHits} ceilingWrites=${stats.ceilingWrites} " +
                    "trafficWrites=${stats.trafficSpriteWrites} terminalWrites=${stats.terminalSpriteWrites} " +
                    "npcWrites=${stats.entitySpriteWrites} " +
                    "ambientActorWrites=${stats.ambientActorSpriteWrites} " +
                    "atmosphereWrites=${stats.atmosphericWrites} eventWrites=${stats.ambientEventWrites} " +
                    "fireflyWrites=${stats.fireflyWrites} " +
                    "localLightWrites=${stats.localizedLightWrites} " +
                    "compositeDepthCells=${presenter.compositeFiniteDepthCellCount} " +
                    "emissiveCells=${presenter.compositeEmissiveCellCount} " +
                    "glyphs=${stats.filledGlyphCount}",
            )
            logRenderStats = false
        }
    }

    override fun resize(width: Int, height: Int) {
        val nextViewport = AsciiViewportCalculator.calculate(
            logicalWidth = width,
            logicalHeight = height,
            drawableWidth = Gdx.graphics.backBufferWidth,
            drawableHeight = Gdx.graphics.backBufferHeight,
            profile = worldFontProfile,
        )
        camera.setToOrtho(false, width.toFloat(), height.toFloat())
        camera.update()

        val currentFrame = frame
        if (currentFrame == null || currentFrame.width != nextViewport.columns || currentFrame.height != nextViewport.rows) {
            frame = AsciiFrame(nextViewport.columns, nextViewport.rows)
            GameLog.info(
                "GameScreen",
                "ASCII ${nextViewport.profile.id} surface is ${nextViewport.columns}x${nextViewport.rows} " +
                    "at ${nextViewport.glyphScale}x glyph scale",
            )
            logRenderStats = true
        }
        viewport = nextViewport
        hud.resize(width, height, nextViewport)
        dialogueOverlay.resize(width, height, nextViewport.glyphScale)
    }

    override fun show() {
        input.clear()
        fixedStepClock.reset()
        simulation.synchronize()
        ambientActorSystem.synchronize()
        footstepCadence.reset()
        headBob.reset()
        audio.start(ambienceZone())
        Gdx.input.inputProcessor = inputMultiplexer
    }

    override fun pause() {
        audio.pause()
        clearTransientState()
    }

    override fun resume() {
        clearTransientState()
        audio.resume()
    }

    override fun hide() {
        audio.pause()
        clearTransientState()
        if (Gdx.input.inputProcessor === inputMultiplexer) Gdx.input.inputProcessor = null
    }

    override fun dispose() {
        if (disposed) return
        disposed = true
        hide()
        dialogueOverlay.dispose()
        hud.dispose()
        presenter.dispose()
        audio.dispose()
    }

    private fun clearTransientState() {
        input.clear()
        fixedStepClock.reset()
        simulation.synchronize()
        ambientActorSystem.synchronize()
        footstepCadence.reset()
        headBob.reset()
    }

    private fun handleCommands(viewPose: ViewPose, activeViewport: AsciiViewport) {
        val mapSwitchRequested = input.consume(GameCommand.CYCLE_DEBUG_MAP)
        val crtToggleRequested = input.consume(GameCommand.TOGGLE_CRT)
        if (input.consume(GameCommand.TOGGLE_MUSIC)) {
            val muted = audio.toggleMusicMuted()
            GameLog.info("AudioService", "Music ${if (muted) "muted" else "unmuted"}")
        }
        if (dialogueOverlay.isVisible) {
            val advance = input.consume(GameCommand.INTERACT)
            val cancel = input.consume(GameCommand.CANCEL)
            if (cancel) {
                closeDialogue()
            } else if (advance && !dialogueOverlay.advance()) {
                finishDialogue()
            }
        }

        if (!dialogueOverlay.isVisible) {
            if (input.consume(GameCommand.TOGGLE_DEBUG)) {
                debugVisible = !debugVisible
                GameLog.info("GameScreen", "F3 diagnostics ${if (debugVisible) "shown" else "hidden"}")
            }
            if (debugVisible && crtToggleRequested) {
                presenter.toggleCrtEffect()
                GameLog.info("GameScreen", "CRT effect ${if (presenter.crtEffectEnabled) "enabled" else "disabled"}")
            }
            if (debugVisible && mapSwitchRequested && nextMapId != null) {
                GameLog.info("GameScreen", "F3 map switch requested: ${content.world.mapId} -> $nextMapId")
                onMapSwitchRequested(nextMapId)
            }
            currentTarget = interactionSystem.findTarget(
                viewPose,
                activeViewport.columns,
                activeViewport.rows,
                activeViewport.profile,
            )
            logTargetTransition(currentTarget)
            if (input.consume(GameCommand.INTERACT)) {
                val target = currentTarget
                if (target != null) showOutcome(target, interactionSystem.interact(target))
            }
            input.consume(GameCommand.CANCEL)
        } else {
            currentTarget = null
            logTargetTransition(null)
        }
    }

    private fun showOutcome(target: InteractionTarget, outcome: InteractionOutcome) {
        when (outcome) {
            is InteractionOutcome.ShowMessage -> dialogueOverlay.show(outcome.title, listOf(outcome.message))
            is InteractionOutcome.StartDialogue -> dialogueOverlay.show(content.dialogues.dialogue(outcome.dialogueId))
            is InteractionOutcome.EnterMap -> {
                GameLog.info("Interaction", "Entering map '${outcome.mapId}' via ${target.entityId}")
                onMapTravelRequested(outcome)
                return
            }
            is InteractionOutcome.Error -> dialogueOverlay.show("INTERACTION ERROR", listOf(outcome.message))
        }
        input.modalActive = true
        GameLog.info("Interaction", "Opened target=${target.entityId} outcome=${outcome::class.simpleName}")
    }

    private fun closeDialogue() {
        dialogueOverlay.hide()
        finishDialogue()
    }

    private fun finishDialogue() {
        input.modalActive = false
        GameLog.info("Interaction", "Closed modal response")
    }

    private fun logTargetTransition(target: InteractionTarget?) {
        val nextId = target?.entityId?.value
        if (nextId == previousTargetId) return
        previousTargetId = nextId
        GameLog.info(
            "Interaction",
            if (target == null) "Center target cleared"
            else "Center target=${target.entityId} depth=${"%.3f".format(Locale.ROOT, target.depth)}",
        )
    }

    private fun hudModel(viewPose: ViewPose, activeViewport: AsciiViewport): HudModel {
        val metrics = ProjectionMetrics(
            columns = activeViewport.columns,
            rows = activeViewport.rows,
            pixelWidth = activeViewport.columns * activeViewport.profile.cellWidth,
            pixelHeight = activeViewport.rows * activeViewport.profile.cellHeight,
            horizontalFovRadians = viewPose.horizontalFovRadians,
            pitchRadians = viewPose.pitchRadians,
        )
        val currentCell = viewPose.position.toGridCell()
        val definition = content.world.grid.cellAt(currentCell)
        val target = currentTarget
        return HudModel(
            locationLine = HudText.location(viewPose),
            promptLine = if (dialogueOverlay.isVisible || target == null) "" else HudText.prompt(target.label),
            debugVisible = debugVisible,
            debugText = if (!debugVisible) "" else HudText.debug(
                mapId = content.world.mapId,
                nextMapId = nextMapId,
                fps = Gdx.graphics.framesPerSecond,
                columns = activeViewport.columns,
                rows = activeViewport.rows,
                glyphScale = activeViewport.glyphScale,
                profileId = activeViewport.profile.id,
                pose = viewPose,
                effectiveHorizontalFovRadians = metrics.effectiveHorizontalFovRadians,
                horizonRow = metrics.horizonRow,
                rayCount = worldRenderer.stats.rayCount,
                ddaSteps = worldRenderer.stats.ddaSteps,
                filledGlyphCount = worldRenderer.stats.filledGlyphCount,
                targetId = target?.entityId?.value,
                targetDepth = target?.depth,
                cellId = definition.id,
                groundMaterialId = definition.groundMaterialId,
                facadeMaterialId = definition.facadeMaterialId,
                crtEffectEnabled = presenter.crtEffectEnabled,
            ),
            debugMap = if (!debugVisible) "" else DebugFootprint.render(content.world.grid, currentCell),
            musicAvailable = audio.musicAvailable,
            musicMuted = audio.musicMuted,
        )
    }

    private fun ambienceZone(): AmbienceZone {
        val cell = simulation.currentState.position.toGridCell()
        return if ("park" in content.world.grid.cellAt(cell).tags) AmbienceZone.PARK else AmbienceZone.CITY
    }

    private fun handleWalkVerificationEvent(event: WalkVerificationScript.Event) {
        val player = simulation.currentState
        GameLog.info(
            "WalkVerification",
            "$event step=$simulationStep x=${"%.3f".format(Locale.ROOT, player.position.x)} " +
                "z=${"%.3f".format(Locale.ROOT, player.position.z)} " +
                "yaw=${"%.2f".format(Locale.ROOT, Math.toDegrees(player.yawRadians))} " +
                "pitch=${"%.2f".format(Locale.ROOT, Math.toDegrees(player.pitchRadians))}",
        )
        when (event) {
            WalkVerificationScript.Event.LIFECYCLE_RESET -> {
                pause()
                resume()
                input.keyDown(com.badlogic.gdx.Input.Keys.W)
            }
            WalkVerificationScript.Event.RESIZE_MINIMUM -> Gdx.graphics.setWindowedMode(1280, 720)
            WalkVerificationScript.Event.RESIZE_REFERENCE -> Gdx.graphics.setWindowedMode(1280, 720)
            else -> Unit
        }
    }

    private fun handleInteractionVerificationEvent(event: InteractionVerificationScript.Event) {
        val player = simulation.currentState
        GameLog.info(
            "InteractionVerification",
            "$event step=$simulationStep x=${"%.3f".format(Locale.ROOT, player.position.x)} " +
                "z=${"%.3f".format(Locale.ROOT, player.position.z)} " +
                "yaw=${"%.2f".format(Locale.ROOT, Math.toDegrees(player.yawRadians))} " +
                "modal=${dialogueOverlay.isVisible} target=${currentTarget?.entityId ?: "--"}",
        )
        when (event) {
            InteractionVerificationScript.Event.RESIZE_MINIMUM -> Gdx.graphics.setWindowedMode(1280, 720)
            InteractionVerificationScript.Event.RESIZE_ULTRAWIDE -> Gdx.graphics.setWindowedMode(3440, 1440)
            InteractionVerificationScript.Event.RESIZE_REFERENCE -> Gdx.graphics.setWindowedMode(1280, 720)
            else -> Unit
        }
    }

    private fun handlePresentationVerificationEvent(event: PresentationVerificationScript.Event) {
        val player = simulation.currentState
        GameLog.info(
            "PresentationVerification",
            "$event step=$simulationStep x=${"%.3f".format(Locale.ROOT, player.position.x)} " +
                "z=${"%.3f".format(Locale.ROOT, player.position.z)} " +
                "yaw=${"%.2f".format(Locale.ROOT, Math.toDegrees(player.yawRadians))} " +
                "pitch=${"%.2f".format(Locale.ROOT, Math.toDegrees(player.pitchRadians))}",
        )
        when (event) {
            PresentationVerificationScript.Event.START -> {
                System.gc()
                presentationHeapStartBytes = usedHeapBytes()
                GameLog.info(
                    "PresentationVerification",
                    "Baseline semanticHash=$initialSemanticWorldHash heapBytes=$presentationHeapStartBytes",
                )
            }
            PresentationVerificationScript.Event.PAUSE_RESUME -> {
                pause()
                resume()
            }
            PresentationVerificationScript.Event.RESIZE_MINIMUM -> Gdx.graphics.setWindowedMode(1280, 720)
            PresentationVerificationScript.Event.RESIZE_ULTRAWIDE -> Gdx.graphics.setWindowedMode(3440, 1440)
            PresentationVerificationScript.Event.RESIZE_REFERENCE -> Gdx.graphics.setWindowedMode(1280, 720)
            PresentationVerificationScript.Event.COMPLETE -> completePresentationVerification()
            PresentationVerificationScript.Event.MUSIC_OFF,
            PresentationVerificationScript.Event.MUSIC_ON -> Unit
        }
    }

    private fun completePresentationVerification() {
        System.gc()
        val finalHash = semanticWorldHash()
        check(finalHash == initialSemanticWorldHash) {
            "Presentation verification mutated semantic world state: $initialSemanticWorldHash -> $finalHash"
        }
        val finalHeapBytes = usedHeapBytes()
        val startHeapBytes = checkNotNull(presentationHeapStartBytes) {
            "Presentation verification completed without a heap baseline"
        }
        GameLog.info(
            "PresentationVerification",
            "COMPLETE semanticStable=true semanticHash=$finalHash " +
                "heapStartBytes=$startHeapBytes heapEndBytes=$finalHeapBytes " +
                "heapDeltaBytes=${finalHeapBytes - startHeapBytes}",
        )
    }

    private fun semanticWorldHash(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        fun add(value: Any?) {
            digest.update(value.toString().toByteArray(Charsets.UTF_8))
            digest.update(0)
        }
        add(content.world.mapId)
        add(content.world.mapVersion)
        add(content.world.grid.width)
        add(content.world.grid.depth)
        content.world.grid.forEachCell { cell, definition ->
            add(cell.x)
            add(cell.z)
            add(definition)
        }
        content.world.allEntities().sortedBy { it.id.value }.forEach { entity ->
            add(
                if (entity.type == com.asciicity.entity.WorldEntityType.AMBIENT_ACTOR) {
                    entity.copy(position = WorldPosition(0.0, 0.0))
                } else {
                    entity
                },
            )
        }
        return digest.digest().joinToString("") { "%02x".format(Locale.ROOT, it) }
    }

    private fun usedHeapBytes(): Long {
        val runtime = Runtime.getRuntime()
        return runtime.totalMemory() - runtime.freeMemory()
    }
}
