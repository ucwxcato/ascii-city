# ASCII City — Last Packet Development Plan

> **Status:** World and presentation foundation retained; narrative direction has pivoted to the Kernel-Panic campaign in `storyline-docs/`. The supplied campaign documents now own story, characters, missions, choices, and endings. Existing maps, first-person movement, interactions, dialogue presentation, ambience, and authored traffic remain available as implementation foundations. Player-controlled driving and racing have been removed from the game.
>
> **Purpose:** Implement the supplied Kernel-Panic storyline as a choice-driven first-person ASCII exploration game.
>
> **Authority:** `storyline-docs/` is the narrative source of truth. The focused [walking storyline game plan](devplan-storyline-exploration.md) owns campaign implementation and locks the no-hacking-gameplay direction. This document records the existing first-person engine and its legacy `Last Packet` prototype. Retain the committed city visual-density roadmap in [devplan-city-visual-density.md](devplan-city-visual-density.md). Optional ambient activity, including authored traffic, is documented in the archived [life-pass plan](../ARCHIVE/devplan-life-pass.md). Do not restore player-controlled cars or racing.
>
> **Visual reference:** [A Walkable ASCII Cyberpunk City in One HTML File](https://www.youtube.com/watch?v=3YtygAx_C6A).
>
> **Target:** Kotlin/JVM, LibGDX, Gradle Kotlin DSL, LWJGL3 desktop, Windows-first development with portable desktop packaging.
>

## 0. Legacy `Last Packet` prototype scope (2026-08-24)

The following scope and implementation checklist describe the previous short-story prototype. It is retained as engine history and is not the current campaign design. New story work follows `storyline-docs/CAMPAIGN_WIP/` and related narrative references.

`Last Packet` is the smallest complete game the ASCII City needs to become. Its whole player loop is **walk, notice, interact, and follow the next signal**. The three relays are authored story landmarks, not collectible counters or hard logic puzzles: each gives one short transmission fragment, changes the player's immediate goal, and leads visibly or through coarse HUD guidance to the next place.

The journey uses the existing fixed maps as compact chapters: `city-alpha` is the Rin/archive hub and contains Cascade Relay, `water-park` contains Relay 2, and the `casino` contains Relay 3. The player returns to `city-alpha` after each interior chapter, so the final archive and Rin conclusion land in the familiar starting city. `nature-reserve` remains a complete optional map for this MVP, rather than being padded into the critical path merely because it exists.

The route is self-contained. No shell command, script, virtual filesystem, network scan, credential, automation action, or external application may be required to start, advance, or finish `Last Packet`. `LastPacketSystem` remains a renderer-neutral city story system with no external runtime dependency.

The release target is one coherent 15 to 30 minute run across the authored maps, from Rin's briefing to `TRANSMISSION DELIVERED`. It deliberately needs no balancing spreadsheet: there are no currencies, health, damage, timers, enemy pressure, upgrades, inventory limits, reputation, resource sinks, random drops, alternate builds, or failure states. Difficulty comes only from reading the environment and following gentle, deterministic guidance; an early or mistaken interaction responds safely and never costs progress.

## 1. Assumptions

- The main view is first-person and street-level, not overhead, isometric, or side-scrolling.
- The reference is an aesthetic and camera-behavior target, not a requirement to reproduce its implementation or assets.
- `W/S` walk forward/back relative to camera yaw; `A/D` strafe; left/right arrows turn yaw; up/down arrows change pitch; `E` is reserved for the planned centered interaction system.
- Movement is continuous in world space. The previous one-tile-per-keypress requirement is superseded because discrete jumps would make first-person walking and looking uncomfortable.
- The city remains grid-authored: roads, sidewalks, alleys, building footprints, doors, and collision occupy integer X/Z cells, while the player and NPCs have continuous positions inside those cells.
- The city should feel built from living data rather than static ASCII decoration. Roads carry directional data flow, facades stream and pulse, and restrained glitches vibrate through surfaces using deterministic animation channels.
- Living-data animation is render-only: it samples stable world coordinates, IDs, depth, and time, and never changes the underlying footprint, collision, object placement, or save state.
- The player has no visible `@` in the main first-person view. `@` may identify the player in an F3 debug minimap; this replaces the original overhead player-glyph requirement.
- The renderer uses CPU software raycasting into a character grid, followed by `BitmapFont`/`SpriteBatch` presentation. It does not require 3D models, `ModelBatch`, or a `PerspectiveCamera`. The only approved presentation-shader exception is the bounded cell-aware composite pass required by [devplan-city-visual-density.md](devplan-city-visual-density.md); it is not a general post-processing stack.
- Buildings are vertical columns with data-defined heights and facade materials. A fixed map may optionally define one flat ceiling over its complete rectangular footprint; it is presentation geometry, not collision or a second floor. The MVP does not support arbitrary polygon meshes, sloped surfaces, partial ceiling masks, stacked walkable floors, or looking through windows.
- A bounded multi-hit grid ray allows taller buildings behind shorter foreground structures to remain visible above them. Opaque nearer spans win through a per-character depth buffer.
- The initial horizontal field of view is 75 degrees; yaw wraps continuously and pitch is initially clamped to ±55 degrees. These are tuning values, not save-schema contracts.
- The desktop client area starts at 1280×720 logical pixels, retains a 960×540 minimum, and has no upper resize/maximize limit. Exclusive and borderless fullscreen modes remain outside the MVP; a maximized resizable window is supported.
- The reference ASCII surface is 128×40 cells at 1280×720 using a 10×18 logical-pixel font cell. Larger logical client areas expand the grid and choose the largest whole-number glyph scale near the reference density while preserving at least 96×30 cells; incomplete edge pixels become centered gutters. A 3440×1440 client therefore renders 172×40 at 2× scale, while a typical 3440×1415 Windows work area renders 172×39 at 2×.
- HDPI backing resolution may exceed the logical client size physically, but it must not independently increase ray count or the logical ASCII grid. Grid selection uses logical client dimensions; whole-number presentation scaling keeps glyph edges sharp without non-uniform stretching.
- The player radius is initially 0.22 cell units, eye height 0.60 units, and movement speed 3.0 cells/second. Collision and feel tests may tune these values without redesigning systems.
- The fixed MVP city loads fully into memory. Procedural chunks and save/load are post-MVP.
- The critical route crosses the existing fixed `city-alpha`, `water-park`, and `casino` maps through authored, paired doors with validated arrival poses. Map travel preserves the one in-memory Last Packet story session; a full relaunch resets it.
- Ambient traffic remains a render-only city detail; player-controlled driving and racing are out of scope. The previous `Last Packet` map route is retained here only as a legacy prototype reference.
- Rin assigns the recovery job, three relay terminals are activated in a fixed order, the third relay permanently unseals the archive for that run, the archive reveals the final transmission, and returning to Rin ends the game.
- Relay 1 is permanently located at the Cascade Relay waterfall overlook in a required nature spur connected directly to the west side of the spawn park. Reaching it is part of the Last Packet critical path, not optional exploration or disconnected post-MVP content.
- Story progress is session-only and resets on relaunch. The MVP has no save slots, inventory representation, quest log, branching choice, time limit, combat, death, or fail state.
- Objective guidance uses the current target's eight-way compass bearing plus coarse signal strength. It does not draw a minimap route, breadcrumb trail, or exact distance in the normal HUD.
- Early, repeated, or out-of-order interaction is safe and idempotent: it may show contextual text but cannot skip, duplicate, or reverse story progress.
- Printable ASCII is the authored/rendered baseline. Unicode glyph support requires a later font-coverage and code-point task.
- The bundled font and any optional audio must have redistribution-compatible licenses; system fonts are never assumed.
- `LEARNING_STRATEGY.md` governs implementation sessions: the user drives/types, the assistant navigates, new Kotlin is taught in small Boring-Kotlin lessons, and each session targets at least three concrete micro-wins.
- Checkboxes are completed only after their source plus required test/runtime evidence exists; manual-only verification remains unchecked until observed.

### Status reconciliation (2026-08-20)

- **Verified:** responsive desktop rendering from the 960×540 minimum through ultrawide/maximized client areas, fixed authored map loading, first-person projection, optional depth-tested whole-map ceilings, movement/look/collision, deterministic material animation, downward screen-space rain, street lamps, the spawn-adjacent park, the enclosed waterpark, the connected mandatory pine/campsite/river/waterfall nature spur, validated fog/contrast/reduced-flicker tuning, HUD/F3 diagnostics, center-ray targeting, terminal prompts/responses, modal-safe input, data-driven linear dialogue, the brief Phase 6 presentation route, the validated static ASCII sprite registry, camera-facing sprite projection/depth testing, Rin's typed stationary definition/live rendering/collision agreement, and visual-only authored traffic loops have source and required evidence.
- **Authored but not story-complete:** `npc.rin` now resolves and visibly renders one blocking world entity, `sprite.rin`, west-facing metadata, display name, and introductory dialogue at the authored starting-route position. Rin and terminal interactions resolve validated speaker/line data through the shared modal `DialogueOverlay`; E/Enter advances, Escape closes, and resize retains the active line. The map also contains Cascade Relay `terminal.relay-01`, the archive terminal, and sealed-door placeholders, while Last Packet progression remains absent.
- **Planned:** campaign implementation follows the focused [walking storyline game plan](devplan-storyline-exploration.md). The unstarted Phase 7 `Last Packet` loop is retained as legacy planning history, not the next story task. Optional ambient activity, including traffic, remains available under the [life pass](../ARCHIVE/devplan-life-pass.md); player-controlled vehicles remain removed.

### Player-visible happy path

```text
Launch -> first-person neon street appears -> turn and look up/down with arrows ->
walk/strafe with WASD -> perspective and occlusion update -> walls stop movement ->
rain and facade signals animate -> speak to Rin -> follow the park trailhead through the
pine/campsite nature spur to the waterfall relay -> follow signal guidance through the water-park
and casino chapters -> recover three fragments -> return to the city archive ->
archive seal opens -> read the final transmission ->
return to Rin -> ending appears -> exit cleanly
```

### Last Packet MVP game contract

The story is a deliberately small state machine, not a reusable quest framework:

```text
SEEK_RIN -> SEEK_RELAY_1 -> SEEK_RELAY_2 -> SEEK_RELAY_3 ->
SEEK_ARCHIVE -> RETURN_TO_RIN -> COMPLETE
```

| Stage | Valid progress interaction | Player-visible result | Next objective |
|---|---|---|---|
| `SEEK_RIN` | `npc.rin` | Rin explains the silent-city recovery job. | Find Relay 1. |
| `SEEK_RELAY_1` | `terminal.relay-01` at the Cascade Relay waterfall overlook | Recover fragment 1/3 and show its transmission text. | Return to the city and find Relay 2. |
| `SEEK_RELAY_2` | `terminal.water-control` on `water-park` | Recover fragment 2/3 and show its transmission text. | Return to the city and find Relay 3. |
| `SEEK_RELAY_3` | `terminal.casino-jackpot` on `casino` | Recover fragment 3/3 and permanently remove the archive seal for this run. | Return to the city and enter the archive. |
| `SEEK_ARCHIVE` | `terminal.archive` | Reveal the city's final transmission. | Return to Rin. |
| `RETURN_TO_RIN` | `npc.rin` | Show Rin's conclusion, then a clear completion screen. | Complete. |
| `COMPLETE` | Any story object | Never changes progress; completed objects may replay short contextual text. | `TRANSMISSION DELIVERED`. |

Required authored dialogue IDs are `dialogue.rin.briefing`, `dialogue.relay-01.fragment`, `dialogue.relay-02.fragment`, `dialogue.relay-03.fragment`, `dialogue.archive.final`, `dialogue.rin.complete`, `dialogue.story.locked`, `dialogue.story.out-of-order`, and `dialogue.story.already-recovered`. `LastPacketSystem` owns the small stage-to-object/dialogue mapping in Kotlin, while an `AsciiCityGame`-owned `LastPacketSession` carries that one state machine through each `GameScreen` replacement during map travel. `ContentVerifier` must reject startup if any required object, dialogue ID, map ID, paired travel door, or arrival pose is absent or has the wrong type. This is intentional product-specific code, not a hidden general scripting format.

The normal HUD shows one concise objective line and one navigation line, for example `OBJECTIVE: RELAY 2/3` and `SIGNAL: NW / WEAK`. Signal strength uses three deterministic distance bands—`STRONG` at or below 8 cells, `MEDIUM` at or below 24 cells, and `WEAK` beyond 24 cells—subject to playtest tuning without changing the state contract. The compass bearing is calculated from player position to the current target in world coordinates and does not depend on rendered glyphs.

Story state advances once when the expected interaction is accepted, before its validated modal text is shown. Repeating an already completed interaction or visiting a later relay early returns a contextual `ShowMessage` outcome without changing state. The archive door is a blocking, depth-tested entity over a walkable doorway cell; completing Relay 3 removes that entity exactly once so rendering and collision open together. Relaunching starts at `SEEK_RIN`.

## 2. Recommended technology and architecture

### Locked decisions

| Area | Decision | Reason and boundary |
|---|---|---|
| Project modules | `core` for portable game/domain/render logic and `desktop` for desktop launch/package. | Keeps platform setup outside gameplay while avoiding excess modules. |
| Lifecycle | `AsciiCityGame : Game` installs `GameScreen : Screen`; `GameAssets` owns shared asset lifetime. | Direct LibGDX lifecycle with one gameplay screen is enough for MVP. |
| Window contract | Start at 1280×720, enforce a 960×540 minimum, and leave the upper window size unbounded so native resize/maximize can use large and ultrawide displays; no MVP exclusive fullscreen toggle. | Keeps a stable minimum/reference canvas while allowing the first-person ASCII view to fill the player's desktop. |
| World representation | `WorldGrid` is a rectangular row-major grid of resolved `CellDefinition` indices plus dynamic entities. | Author-friendly, compact, deterministic, and ideal for DDA ray traversal. |
| Player representation | Continuous X/Z position, yaw, pitch, radius, and eye height; no main-view `@`. | First-person movement needs sub-cell positions and camera-relative motion. |
| Perspective | Plain `ViewPose` + `ProjectionMetrics`; custom CPU raycaster performs projection. | Produces real vanishing point, depth, height, and occlusion while keeping the final image genuinely glyph-based. |
| LibGDX cameras | Use an `OrthographicCamera` only to present the final ASCII grid. Do not use `PerspectiveCamera` for MVP world rendering. | Perspective already occurs in software; the GPU receives a flat glyph surface. |
| Glyph rendering | `AsciiFrame` primitive arrays -> `AsciiPresenter` using one `SpriteBatch` and `BitmapFont`. | Avoids one Scene2D Actor per character and avoids per-frame strings. |
| UI | Scene2D only for HUD, dialogue, pause, errors, inventory, and debug panels. | Widgets/input focus benefit from Scene2D; world cells do not. |
| Simulation | Plain Kotlin classes/systems at fixed 60 Hz with render interpolation. | Testable movement/collision/look behavior without a full ECS. |
| Collision | Circle-vs-solid-grid and dynamic-occupant checks use semantic properties, never displayed glyphs. | Visual material changes cannot alter physics accidentally. |
| Content IDs | Stable strings in files/saves, resolved to compact integer indices at load time. | Fast runtime arrays without coupling saves to enum or load order. |
| MVP story | One explicit `LastPacketStage` enum and `LastPacketSystem`; narrative prose stays in `dialogue.json` and object positions stay in `city-alpha.objects.json`. | Makes the one release scenario easy to understand and test without building a scripting language or generic quest engine. |
| Story persistence | `Last Packet` progress exists only for the current run and starts at `SEEK_RIN` on launch. | Save/load remains post-MVP and cannot delay a complete first release. |
| Required nature spur | Replace the west-side block beside the spawn park with a connected pine trail, two bridge crossings, river, campsite tents, cliff waterfall, and `terminal.relay-01` at the overlook. Use tall semantic cells and silhouette masks rather than slopes or vertical physics. | Makes the city's first relay leg a deliberate tonal transition and keeps the landmark inside the fixed-map/rendering architecture. |
| Persistence | Versioned DTOs, stable IDs, candidate-world load, temp-write then replace. | Supports migration and prevents partial/corrupt active state. |
| Kotlin learning | Implementation is user-typed in project-sized lessons; no complete Kotlin file drops unless explicitly requested. | Binding repository learning contract. |

### Component ownership

| Component | Mechanism | Owns | Must not own |
|---|---|---|---|
| `AsciiCityGame` | LibGDX `Game` | Startup, shared assets, screen transition, final disposal. | World rules or ray math. |
| `GameScreen` | LibGDX `Screen` | Dependency composition, fixed-step loop, render ordering, lifecycle routing. | Parsing formats or collision details. |
| `GameWorld` | Plain Kotlin | Grid, player, entities, map ID, simulation time, position index. | Input APIs, fonts, batches. |
| `WorldGrid` | Plain Kotlin + `IntArray` | Bounds, row-major cells, semantic cell lookup/mutation. | Perspective or glyph selection. |
| `PlayerState` | Plain Kotlin | X/Z position, previous position, yaw, pitch, radius, eye height. | Raw keys or rendering. |
| `InputController` | `InputAdapter` | Held WASD/arrows and edge commands (`Interact`, `Cancel`, `ToggleDebug`, `ToggleMute`). | Direct world mutation. |
| `LookSystem` | Plain Kotlin | Fixed-step yaw/pitch integration, wrap/clamp, modal clearing. | Collision or projection. |
| `MovementSystem` | Plain Kotlin | Camera-relative velocity, diagonal normalization, collision-safe position updates. | Keyboard polling. |
| `CollisionSystem` | Plain Kotlin | Circle-vs-grid, bounds, doors, blocking entity occupancy, movement sweep. | Glyph/token comparisons. |
| `ProjectionMetrics` | Immutable math data | FOV, glyph aspect correction, horizon, projection-plane scales. | World mutation. |
| `GridRaycaster` | Plain Kotlin | Ordered DDA cell-boundary hits with distance, side, material coordinate. | Glyph drawing or input. |
| `WorldAsciiRenderer` | Plain Kotlin | Sky/floor/wall/optional-ceiling projection, multi-height occlusion, material sampling, depth writes. | Simulation mutation or ceiling collision. |
| `FacadeSignSampler` | Plain Kotlin | Validated wall-mounted marquee layouts, warm sign colors, distance LOD, restrained bulb animation. | Collision, camera-facing sprites, or world mutation. |
| `AsciiFrame` | Primitive arrays | Glyph code, foreground/background packed color, depth for each output cell. | Assets or world ownership. |
| `AsciiPresenter` | `SpriteBatch` + `BitmapFont` | Draw `AsciiFrame` pixel-aligned through an orthographic projection. | Raycasting. |
| `EntitySpriteRenderer` | Plain projection helper | Project camera-facing NPC/world-object ASCII sprites and depth-test each visible character against world spans. | Sprite identity, entity iteration, or NPC behavior. |
| `InteractionSystem` (proposed) | Plain Kotlin | Center-ray target selection, range/LOS validation, typed outcomes. | Dialogue widgets. |
| `LastPacketSystem` (proposed) | Plain Kotlin | Current `LastPacketStage`, expected stable object IDs, objective target, idempotent progress transitions, archive unsealing, completion outcome. | Rendering, raw input, arbitrary quest graphs, inventory, or save files. |
| `RainRenderer` | Plain Kotlin | Deterministic, allocation-free screen-space rain sampling and sky-only composition. | Map cells or collision. |
| `StreetLampRenderer` | Plain Kotlin | Derived/authored lamp fixtures, depth-tested posts, and restrained local glow. | Collision or light simulation. |
| `TreeSilhouette` / `ParkFixtureSilhouette` | Plain Kotlin | Visual masks for semantic trees, park fixtures, and casino slot-machine billboards. | Collision or map semantics. |
| `Hud` / `DialogueOverlay` | Scene2D | Controls, pose/debug info, prompt, modal dialogue/input focus. | Authoritative state changes. |
| `GameAssets` | `AssetManager` wrapper | Fonts, audio, palettes, data files, fallback/error policy. | Gameplay rules. |
| `AudioService` | LibGDX audio wrapper | Crossfaded music, independent rain/waterfall/SFX levels, no-audio fallback, lifecycle. | Interaction decisions or rendered-glyph proximity. |
| `SaveService` | JSON/file boundary | DTO mapping, migration, validation, atomic slots. | Serializing live renderer/LibGDX objects. |

### In-memory city grid

`WorldGrid(width, depth, cellIndices: IntArray)` uses `index = z * width + x`; `(0,0)` is the southwest map cell. A `CellDefinition` contains stable ID, authoring token, ground material, facade material, solid flag, opaque flag, building height, tags, and optional interaction type. Collision reads `solid`; rendering reads materials/height. The authoring token is never a physics rule.

Dynamic doors, NPCs, terminals, and lamps live outside the base cell array and have stable IDs plus continuous or cell-anchored transforms. A spatial index returns occupants/fixtures for a grid cell without scanning all entities. For the MVP archive seal, its authored footprint cell is walkable and the door entity alone supplies closed-state collision and occlusion; `LastPacketSystem` removes that entity after Relay 3 instead of mutating collision from a glyph or color.

### Map and content files

```text
assets/maps/city-alpha.txt          # rectangular footprint, north row first
assets/maps/city-alpha.meta.json    # map ID/version, spawn, sky/fog, optional ceiling, object path
assets/maps/city-alpha.objects.json # Rin, three relays, archive terminal/seal, lamps
assets/data/cells.json              # tokens -> semantic cell definitions
assets/data/materials.json          # ground/facade glyph ramps, colors, window patterns
assets/data/palette.json            # semantic RGBA colors
assets/data/sprites.json            # static NPC/entity glyph and palette layouts
assets/data/dialogue.json           # Rin, relay fragments, archive, repeat/locked text
assets/data/presentation.json       # validated fog, contrast, shading, pulse, and reduced-flicker tuning
```

The loader normalizes LF/CRLF, rejects tabs/ragged rows/unknown tokens, flips north-first file rows into increasing world Z, validates a collision-free spawn and in-bounds objects, resolves stable IDs, then constructs `GameWorld` only after all files pass. Errors identify file plus line/column or JSON path. A critical failure shows a readable error overlay; it never launches a partial city.

### First-person ASCII rendering contract

1. `AsciiViewportCalculator` divides the logical client area into whole scaled 10×18 glyph cells. It preserves at least 96×30 cells, selects a whole-number scale near the 128×40 reference density (with 5% work-area tolerance for window chrome/taskbars), expands columns for ultrawide clients, and centers incomplete edge pixels as black gutters.
2. `ProjectionMetrics` treats the authored 75° horizontal FOV as the 16:9 reference and derives an aspect-correct effective horizontal FOV while preserving vertical framing. Wider clients reveal more world at the sides instead of stretching glyphs or cropping the top/bottom; extreme aspect ratios clamp to the existing 30°–120° safety range.
3. For each output column, `GridRaycaster` performs bounded DDA from player X/Z along the column ray and emits ordered entry faces for opaque building/door cells up to `maxRayDistance`.
4. `WorldAsciiRenderer` projects each hit cell's base and height. Near spans write depth; traversal continues so uncovered rows may show taller distant buildings above shorter foreground roofs. Traversal stops at distance/step limit or when all relevant rows are occluded.
5. Ground-plane casting fills road, sidewalk, and alley samples below the pitch-adjusted horizon; sky/fog fills remaining cells. Perpendicular camera-space depth prevents fisheye distortion.
6. When metadata defines a whole-map ceiling, an inverse plane cast fills samples above the horizon only where the projected X/Z remains inside the map. It shares the perpendicular character-depth buffer with walls, fixtures, lamps, and sprites, and therefore excludes the later sky-only rain pass without changing collision.
7. Materials choose glyph/color by surface kind, world coordinate, stable phase, face, depth/fog, and time. Data-stream, signal-pulse, and glitch-flicker channels make the city visibly flow and vibrate while remaining deterministic for an identical world/pose/time; they never alter geometry or collision.
8. Depth-tested street lamps render after opaque surfaces; tagged solid park cells use shaped visual masks while preserving semantic collision.
9. Screen-space rain renders after world surfaces/fixtures and before future entity-sprite/UI layers; `AsciiPresenter` draws the completed glyph/color arrays with no per-cell object allocation.
10. `NpcSystem` supplies stationary NPCs in stable entity-ID order from authoritative `GameWorld` positions; `EntitySpriteRenderer` projects them after opaque world surfaces and writes each visible character only where closer than `AsciiFrame.depth`.

### Camera, controls, and collision

- Yaw 0 faces world north (`+Z`); forward is derived from yaw and right is perpendicular.
- Left/right arrows change yaw at an initial 100 degrees/second; up/down change pitch at 75 degrees/second; pitch clamps to ±55 degrees and yaw wraps to `[0, 2π)`.
- `W/S` add/subtract the forward vector; `A/D` add/subtract the right vector. Combined movement is normalized to prevent diagonal speed boosts.
- The player is a circle in the X/Z plane. Movement is split into bounded substeps, resolved separately by axis to slide along walls, and rejected outside map bounds or through closed doors/blocking entities.
- Render interpolation uses previous/current pose; authoritative simulation remains fixed-step. Focus loss, pause, and modal UI clear held keys.
- Planned `E` interaction will cast through the center character cell, depth-test the nearest interactable within the initial 1.75-cell range, reject occluded targets, and return a typed interaction outcome.

### Window sizes and glyph resolution

The launcher requests a 1280×720 logical client area, enforces only the native 960×540 minimum, and passes `-1` upper limits to LWJGL3/GLFW so resize and maximize may use the available desktop work area. Exclusive and borderless fullscreen modes are not exposed in the MVP.

Resize recalculates output columns/rows, whole-number glyph scale, gutters, `AsciiFrame`, presentation transform, Scene2D HUD/dialogue scale, viewport, and projection metrics without rebuilding the world. Logical dimensions select the grid; drawable/backing pixels remain presentation-only so Windows display scaling cannot independently multiply CPU raycasting. The presenter applies one uniform nearest-filtered transform, keeping every glyph aligned and undistorted.

### Rain, story, NPCs, procedural chunks, and saves

- Rain derives sparse column streaks from stable hashes plus render time without mutable particle records. Falling heads move toward smaller `AsciiFrame` rows, draw only into uncovered sky, and never mutate `WorldGrid` or collision.
- `Last Packet` uses the explicit session-only stage sequence documented above. Story outcomes may request modal text or remove the single archive-seal entity; they never infer progress from HUD text, rendered glyphs, dialogue line index, or player position alone.
- MVP NPCs are stationary camera-facing ASCII entity sprites with stable IDs and dialogue IDs. Post-MVP patrols move between cell centers using the same collision system; stable ID order resolves conflicts.
- Procedural chunks use 32×32 footprint cells. Shared roads/alleys derive from a canonical edge key `(worldSeed, generatorVersion, orientation, lowerNeighborCoord)` so both neighbors independently agree. Height/material generation uses partitioned RNG streams so cosmetic changes cannot move roads.
- Saves store schema/game version, fixed-map ID/version or generated seed/version, player X/Z/yaw/pitch, stable entity states, inventory, discovered chunks, and deltas. They never store runtime indices, fonts, buffers, cameras, or batches.

### Implementation and learning workflow

Each checkbox is one focused coding session unless its verification gate explicitly combines prior work. When executing a task: declare Babysteps/Bridge/Shadow mode; anchor the feature in the game; show only a small skeleton with blanks; have the user explain unfamiliar syntax before typing; target three micro-wins (source typed, test/build green, runtime evidence or tattoo note); and add/update `DOCS/kotlin-brain-tattoos/` when a concept lands. Intentional breakage happens only after green, with opt-in, an isolated reversible change, and a final return to green.

## 3. Proposed package and folder structure

```text
ascii-city/
├─ settings.gradle.kts
├─ build.gradle.kts
├─ gradle.properties
├─ gradle/libs.versions.toml
├─ core/
│  ├─ build.gradle.kts
│  └─ src/
│     ├─ main/kotlin/com/asciicity/
│     │  ├─ AsciiCityGame.kt
│     │  ├─ screen/{LoadingScreen,GameScreen}.kt
│     │  ├─ world/{GameWorld,WorldGrid,CellDefinition,WorldPosition}.kt
│     │  ├─ entity/{WorldEntity,PlayerState,Npc,Door,Terminal}.kt
│     │  ├─ input/{InputController,GameCommand}.kt
│     │  ├─ system/{LookSystem,MovementSystem,CollisionSystem,InteractionSystem,NpcSystem}.kt
│     │  ├─ story/{LastPacketStage,LastPacketSystem,StoryOutcome}.kt
│     │  ├─ render/{AsciiFrame,AsciiPresenter,AsciiViewportCalculator,ViewPose,ProjectionMetrics,GridRaycaster,RayHit,WorldAsciiRenderer,RainRenderer,StreetLampRenderer,TreeSilhouette,ParkFixtureSilhouette,EntitySpriteRenderer,RenderPalette}.kt
│     │  ├─ content/{ContentLoader,MapLoader,CellRegistry,MaterialRegistry,ContentValidationException}.kt
│     │  ├─ ui/{Hud,DialogueOverlay,ErrorOverlay}.kt
│     │  ├─ asset/GameAssets.kt
│     │  ├─ audio/AudioService.kt
│     │  ├─ save/{SaveGameDto,SaveService,SaveMigration}.kt       # post-MVP
│     │  ├─ generation/{ChunkCoord,EdgeContract,CityGenerator}.kt # post-MVP
│     │  └─ item/{ItemDefinition,ItemRegistry,Inventory}.kt       # post-MVP
│     └─ test/kotlin/com/asciicity/{content,world,system,render,save,generation}/
├─ desktop/src/main/kotlin/com/asciicity/desktop/DesktopLauncher.kt
├─ assets/{fonts,maps,data,audio,licenses}/
└─ DOCS/
   ├─ devplans/ascii-city-development-plan.md
   ├─ map-format.md
   ├─ rendering-contract.md
   ├─ verification-notes.md
   └─ kotlin-brain-tattoos/
```

Post-MVP packages are created only when their phase begins. The package tree reserves stable boundaries; it does not authorize empty interfaces or speculative framework code.

## 4. Full checkbox development plan

Every task includes scope, dependency, likely files, and an objective completion gate. **[MVP]** is required for the first release, **[Post-MVP]** follows its acceptance, and **[Optional]** may be omitted.

### Phase 0 — Design, learning, and project baseline

- [x] **BASE-01 [MVP] Record the first-person visual contract.** Create original annotated reference plates showing horizon, vanishing point, building scale, road perspective, glyph density, fog, and neon; state that assets are inspiration-only. **Depends:** none. **Files:** `DOCS/rendering-contract.md`. **Done when:** the document distinguishes required first-person behavior from optional visual polish and contains no copyrighted asset extraction.
- [x] **BASE-02 [MVP] Record the lesson execution contract.** Summarize the applicable `LEARNING_STRATEGY.md` workflow, three-micro-win rule, Boring Kotlin default, and tattoo-doc path for this repo. **Depends:** none. **Files:** `DOCS/implementation-workflow.md`, `DOCS/kotlin-brain-tattoos/README.md`. **Done when:** each future task can be run as a user-typed lesson without inventing teaching rules.
- [x] **SETUP-01 [MVP] Pin the build toolchain.** Select Java 21 and mutually compatible stable Kotlin, LibGDX, LWJGL3, and JUnit versions without dynamic selectors. **Depends:** none. **Files:** `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`, wrapper. **Done when:** the wrapper resolves, Gradle daemon/compile/test toolchains require Java 21, and `./gradlew help` resolves cleanly.
- [x] **SETUP-02 [MVP] Create `core` and `desktop` modules.** Configure Kotlin/JVM, UTF-8, assets path, desktop natives, test dependencies, and application main class. **Depends:** SETUP-01. **Files:** module build scripts. **Done when:** `./gradlew projects compileKotlin` succeeds and lists both modules.
- [x] **SETUP-03 [MVP] Launch the responsive desktop window.** Add `AsciiCityGame` and `DesktopLauncher` with a 1280×720 default logical client size, 960×540 minimum, no upper resize/maximize cap, VSync, logical HDPI mode, no exclusive fullscreen option, and clean exit. **Depends:** SETUP-02. **Files:** launcher and game classes. **Done when:** `./gradlew desktop:run` opens at 1280×720, clamps only below 960×540, accepts larger/ultrawide requests and native maximize, and closes without an uncaught error.
- [x] **SETUP-04 [MVP] Establish automated checks.** Configure JUnit 5, Kotlin assertions, and headless LibGDX tests where `Gdx` services are needed. **Depends:** SETUP-02. **Files:** build scripts, one smoke test, `README.md`. **Done when:** `./gradlew clean check` passes on a clean checkout.
- [x] **SETUP-05 [MVP] Add repository hygiene and diagnostics.** Ignore build/IDE/native/local-save outputs, configure LibGDX logging, and install a launcher-level fatal error handler. **Depends:** SETUP-03. **Files:** `.gitignore`, `DesktopLauncher.kt`, proposed `GameLog.kt`. **Done when:** a deliberate failure test produces one readable fatal report and no generated build debris is tracked.
- [x] **VERIFY-00 [MVP] Verify the baseline.** Run clean check, ten launch/exit cycles, resize to the minimum/reference/ultrawide sizes, attempt below-minimum and native maximize, alt-tab, and inspect logs. **Depends:** BASE-01, BASE-02, SETUP-01 through SETUP-05. **Files:** `DOCS/verification-notes.md`. **Done when:** evidence proves the client respects the 960×540 minimum, larger logical areas remain aligned and responsive, and lifecycle/build behavior is green.

### Phase 1 — Window and glyph surface (Milestone 1)

- [x] **ASSET-01 [MVP] Implement `GameAssets`.** Own one `AssetManager`, typed keys, loading state, critical/recoverable failures, and one disposal boundary. **Depends:** SETUP-02. **Files:** `asset/GameAssets.kt`. **Done when:** tests reject access before load and prove one disposal path.
- [x] **ASSET-02 [MVP] Bundle a licensed monospace bitmap font.** Include `.fnt`, texture, printable ASCII coverage, source/version, and license. **Depends:** ASSET-01. **Files:** `assets/fonts/*`, `assets/licenses/*`. **Done when:** characters 32–126 render and packaging inputs include the license.
- [x] **ASSET-03 [MVP] Lock the 10×18 logical font cell.** Define immutable cell width, height, baseline, and HDPI presentation rules; validate `M`, `i`, `@`, `#`, `/`, `\`, and space. **Depends:** ASSET-02. **Files:** proposed `render/FontMetrics.kt`, tests. **Done when:** representative glyphs occupy stable 10×18 logical cells without drift and 128×40 cells exactly fill 1280×720.
- [x] **ASSET-04 [MVP] Load the semantic palette.** Define sky, fog, asphalt, sidewalk, wall faces, windows, neon variants, NPC, player-debug, rain, HUD, and errors. **Depends:** ASSET-01. **Files:** `assets/data/palette.json`, `RenderPalette.kt`. **Done when:** duplicate/malformed/missing keys fail with their logical key.
- [x] **FRAME-01 [MVP] Implement `AsciiFrame`.** Allocate fixed `IntArray` glyph/foreground/background and `FloatArray` depth storage with bounds-safe index/clear/write APIs. **Depends:** ASSET-03. **Files:** `render/AsciiFrame.kt`, tests. **Done when:** tests cover corners, clear values, nearer-depth wins, invalid dimensions, and no object allocation per cell.
- [x] **FRAME-02 [MVP] Implement the glyph presenter.** Draw `AsciiFrame` with `SpriteBatch`/`BitmapFont`, orthographic pixel coordinates, packed colors, and explicit layer/color reset. **Depends:** FRAME-01, ASSET-04. **Files:** `render/AsciiPresenter.kt`. **Done when:** a colored frame appears aligned with one batch begin/end pair.
- [x] **FRAME-03 [MVP] Render the Milestone 1 test card.** Fill a deterministic grid with glyph ramp, color bands, diagonals, boxes, horizon line, and fake near/far building silhouettes. **Depends:** FRAME-02. **Files:** proposed `render/AsciiTestCard.kt`, `GameScreen.kt`. **Done when:** the window displays a colored ASCII perspective test image without world/raycast code.
- [x] **FRAME-04 [MVP] Implement responsive resize-safe ASCII viewport selection.** Divide the logical client area into uniformly scaled 10×18 cells, keep at least 96×30, choose a whole-number glyph scale near the 128×40 reference density, center leftover-pixel gutters, and recreate buffers only when the logical grid changes. **Depends:** ASSET-03, FRAME-02. **Files:** `AsciiViewportCalculator.kt`, tests. **Done when:** 960×540 yields 96×30 at 1×, 1280×720 yields 128×40 at 1×, 3440×1440 yields 172×40 at 2×, common work-area reductions keep the intended scale, non-divisible sizes center gutters, and HDPI backing size alone never changes the grid.
- [x] **ASSET-05 [MVP] Add missing-asset UX.** Use a visible fallback for non-critical glyph/material content and a blocking Scene2D error overlay for font/palette/map failures. **Depends:** ASSET-01, FRAME-02. **Files:** `GameAssets.kt`, `ui/ErrorOverlay.kt`. **Done when:** missing font and corrupt palette tests never produce a blank window or null crash.
- [x] **VERIFY-01 [MVP] Verify Milestone 1.** Capture the test card at required resolutions and exercise critical/recoverable asset failures. **Depends:** ASSET-02 through ASSET-05, FRAME-01 through FRAME-04. **Files:** `DOCS/verification-notes.md`, screenshots. **Done when:** Milestone 1 definition passes and `./gradlew clean check` is green.

### Phase 2 — World data and fixed city

- [x] **WORLD-01 [MVP] Define coordinate/value types.** Add `GridCell(x,z)`, `WorldPosition(x,z)`, `WorldDirection`, and stable `EntityId` using descriptive plain Kotlin values. **Depends:** SETUP-02. **Files:** `world/*.kt`. **Done when:** equality, conversions, floor-to-cell behavior (including negative fixtures), and direction vectors pass tests.
- [x] **MAP-01 [MVP] Define semantic cell/material contracts.** Add `CellDefinition`, `CellRegistry`, `SurfaceMaterial`, and `MaterialRegistry` with stable IDs, token, ground/facade keys, solid/opaque, height, and tags. **Depends:** WORLD-01, ASSET-04. **Files:** world/content classes, `cells.json`, `materials.json`. **Done when:** duplicate IDs/tokens, negative heights, unknown palettes/materials, and missing required fields fail validation.
- [x] **MAP-02 [MVP] Implement `WorldGrid`.** Store resolved cell indices in private row-major `IntArray`; expose dimensions, bounds, read/mutate, and iteration without exposing storage. **Depends:** MAP-01. **Files:** `world/WorldGrid.kt`, tests. **Done when:** every corner/index/order/bounds case passes.
- [x] **MAP-03 [MVP] Specify the map format.** Document UTF-8 printable ASCII, north-row-first layout, metadata/object schemas, coordinate flip, spawn/height/material rules, and diagnostics. **Depends:** MAP-01. **Files:** `DOCS/map-format.md`. **Done when:** an unfamiliar developer can create one valid map and predict each invalid example's error.
- [x] **MAP-04 [MVP] Implement map/content loading.** Parse text/JSON into immutable DTOs, normalize line endings, validate all references/bounds/spawn clearance, then construct the grid. **Depends:** MAP-02, MAP-03. **Files:** `MapLoader.kt`, `ContentLoader.kt`, `ContentValidationException.kt`. **Done when:** failures identify file and line/column or JSON path and no partial world escapes.
- [x] **MAP-05 [MVP] Test map parsing.** Cover empty/ragged/tabbed/unknown-token maps, LF/CRLF parity, missing/duplicate spawn, invalid height/material, out-of-bounds/duplicate objects, malformed JSON, and north-to-Z flip. **Depends:** MAP-04. **Files:** `content/MapLoaderTest.kt`, fixtures. **Done when:** all cases run headlessly with exact diagnostic assertions.
- [x] **WORLD-02 [MVP] Implement `GameWorld` and spatial entity index.** Own grid, player, entities, map identity/time, and consistent add/move/remove queries by cell. **Depends:** WORLD-01, MAP-02. **Files:** `GameWorld.kt`, `WorldEntity.kt`, `PlayerState.kt`. **Done when:** entity lifecycle/index invariants pass tests.
- [x] **MAP-06 [MVP] Author `city-alpha`.** Build at least a 96×96 footprint with connected roads, sidewalks, alleys, varied-height blocks, open sight lines, neon landmarks, player spawn, one terminal, and one NPC location. **Depends:** MAP-04. **Files:** `assets/maps/city-alpha.*`. **Done when:** validation passes, spawn has clearance, required interaction points are reachable, and every MVP material appears.
- [x] **MAP-07 [MVP] Add content validation to `check`.** Run registry/map/object/dialogue validation without launching graphics and print concise per-file results. **Depends:** MAP-06. **Files:** build task or validation entry point. **Done when:** one intentionally bad fixture fails `check` and restoration returns green.
- [x] **VERIFY-02 [MVP] Verify the fixed world data.** Load `city-alpha`, inspect dimensions/spawn/material/height distributions, test invalid fixtures, and record a simple debug footprint. **Depends:** MAP-01 through MAP-07, WORLD-02. **Files:** verification notes. **Done when:** the authored world is deterministic, reachable, and fully validated before rendering.

### Phase 3 — First-person projection and raycasting

- [x] **VIEW-01 [MVP] Define `ViewPose`.** Store interpolatable X/Z, eye height, yaw, pitch, and FOV independently of LibGDX camera classes. **Depends:** WORLD-01. **Files:** `render/ViewPose.kt`, tests. **Done when:** yaw wrap, pitch clamp, forward/right vectors, and interpolation across the wrap boundary pass.
- [x] **PROJ-01 [MVP] Implement `ProjectionMetrics`.** Derive column rays, horizon row, physical aspect correction, vertical scale, and screen-row projection from font/output pixels. **Depends:** VIEW-01, FRAME-04. **Files:** `render/ProjectionMetrics.kt`, tests, `DOCS/rendering-contract.md`. **Done when:** center ray matches forward, symmetric columns mirror, pitch moves horizon correctly, and resize preserves undistorted FOV.
- [x] **RAY-01 [MVP] Implement bounded grid DDA.** Traverse crossed X/Z boundaries in distance order and emit cell, side, entry distance, and face coordinate without rendering. **Depends:** MAP-02, VIEW-01. **Files:** `render/GridRaycaster.kt`, `RayHit.kt`. **Done when:** cardinal, diagonal, corner-tie, outside-bound, empty, and maximum-step cases pass.
- [x] **RAY-02 [MVP] Add DDA property/regression tests.** Assert monotonically increasing distance, no duplicate/infinite hits, stable corner policy, and expected hits across randomized small grids. **Depends:** RAY-01. **Files:** `render/GridRaycasterTest.kt`. **Done when:** fixed seeds pass repeatedly and failures report ray/grid details.
- [x] **RAY-03 [MVP] Project one wall column.** Convert perpendicular hit depth plus cell height into top/bottom rows relative to eye/horizon; shade face and clip to frame. **Depends:** PROJ-01, RAY-01, FRAME-01. **Files:** `WorldAsciiRenderer.kt`, tests. **Done when:** a test wall doubles apparent height at half distance, pitch shifts it correctly, and no fisheye appears across columns.
- [x] **RAY-04 [MVP] Implement multi-height occlusion.** Continue DDA after nearer structures, write per-row depth, reveal only uncovered taller distant spans, and stop when distance/steps/full occlusion permits. **Depends:** RAY-03. **Files:** `WorldAsciiRenderer.kt`, tests. **Done when:** tall-behind-short, short-behind-tall, equal-height, gaps, and max-distance fixtures produce expected frame hashes.
- [x] **RAY-05 [MVP] Implement sky and ground-plane casting.** Fill sky/fog and sample road/sidewalk/alley ground below the pitch-adjusted horizon with distance-correct coordinates. **Depends:** PROJ-01, MAP-01. **Files:** `WorldAsciiRenderer.kt`, tests. **Done when:** road edges converge, turning changes samples continuously, look up/down moves the horizon, and no NaN/divide-by-zero occurs at it.
- [x] **RAY-06 [MVP] Implement living-data material glyph/color sampling.** Select facade/ground glyph ramps, side shading, distance fog, and palette colors; sample data-stream, signal-pulse, and restrained glitch channels from stable world phase plus time. **Depends:** RAY-04, RAY-05, MAP-01. **Files:** `MaterialRegistry.kt`, proposed `MaterialSampler.kt`. **Done when:** identical world/pose/time yields an identical frame, advancing time creates deterministic surface flow/vibration, semantic surfaces remain distinct, and geometry/collision checksums never change.
- [x] **RAY-07 [MVP] Integrate world frame presentation.** Render sky/ground/buildings to `AsciiFrame`, then present it before UI; expose ray count, DDA steps, and filled glyph count. **Depends:** RAY-06, FRAME-02, WORLD-02. **Files:** `GameScreen.kt`, `WorldAsciiRenderer.kt`. **Done when:** `city-alpha` appears from spawn with visible vanishing point, varied skyline, roads, and correct occlusion.
- [x] **RAY-08 [MVP] Remove steady-state renderer allocations.** Reuse hits/scratch arrays/colors, avoid per-cell strings/collections/vectors, and size scratch storage only on resize/config change. **Depends:** RAY-07. **Files:** render package. **Done when:** 60 seconds stationary produces no per-ray/per-character heap allocation in profiling.
- [x] **RAY-09 [MVP] Add deterministic frame fixtures.** Hash glyph/color/depth results for selected poses facing cardinal/diagonal directions, looking up/down, near walls, and layered heights. **Depends:** RAY-07. **Files:** `WorldAsciiRendererTest.kt`, fixtures. **Done when:** headless math/frame tests reproduce hashes and a deliberate projection change fails clearly.
- [x] **INTERIOR-01 [Optional] Add validated whole-map interior ceilings.** Accept an optional finite ceiling height/material in fixed-map metadata, cast the bounded plane above the horizon with shared perpendicular depth, preserve outdoor snapshots when absent, and keep collision independent. Reauthor `water-park` with seven-unit white-tile walls and a pale repeating tiled ceiling; expose `--map` for direct smoke captures. **Depends:** PROJ-01, RAY-05, MAP-04, RAIN-02. **Files:** map metadata/content loader, `ProjectionMetrics.kt`, `WorldAsciiRenderer.kt`, waterpark/palette/material data, format/render docs, tests, verification capture. **Done when:** exact-path metadata failures, opposing floor/ceiling plane math, finite top-row enclosure, rain exclusion, outdoor hash stability, deterministic indoor hash, content validation, full checks, and a visually approved 1280×720 waterpark capture pass. **Evidence:** approved liminal white-tile hash `a5993bbb504879f02f963583a2f1496da955d35aa1ee31047e2e15970ba8136a` and `water-park-interior-1280x720.png` on 2026-08-20.
- [x] **VERIFY-03 [MVP] Verify first-person city projection.** Capture spawn at eight yaw directions and three pitches/resolutions; compare perspective, horizon, height, fog, and occlusion to the rendering contract. **Depends:** VIEW-01, PROJ-01, RAY-01 through RAY-09. **Files:** verification notes/screenshots. **Done when:** the city unmistakably reads as a street-level perspective scene and all projection tests pass.

### Phase 4 — Look, movement, and collision (Milestone 2 core)

- [x] **INPUT-01 [MVP] Implement semantic held input.** Track W/S/A/D and arrow presses/releases plus edge commands E/Enter, Esc, and F3 through `InputController`; do not mutate the world. **Depends:** SETUP-03. **Files:** `InputController.kt`, `GameCommand.kt`, tests. **Done when:** each key has one documented semantic action and focus loss clears held state.
- [x] **LOOK-01 [MVP] Integrate arrow-key yaw/pitch.** Apply fixed-step angular speeds, simultaneous-axis look, yaw wrap, pitch clamp, and modal suppression. **Depends:** INPUT-01, VIEW-01. **Files:** `LookSystem.kt`. **Done when:** holding arrows turns/looks smoothly, perspective rerenders accordingly, and no key changes position.
- [x] **LOOK-02 [MVP] Test camera boundaries.** Cover wrap in both directions, exact pitch clamps, simultaneous arrows, release, pause/focus loss, fixed-step independence, and interpolation near 0/2π. **Depends:** LOOK-01. **Files:** `LookSystemTest.kt`, `ViewPoseTest.kt`. **Done when:** all camera-boundary tests pass without discontinuity or overshoot.
- [x] **COLL-01 [MVP] Implement circle-vs-grid collision.** Query nearby cells by semantic solid flag, reject world bounds, calculate penetration/allowed movement, and ignore glyph/material. **Depends:** MAP-02, WORLD-02. **Files:** `CollisionSystem.kt`, tests. **Done when:** walls, corners, narrow alleys, non-solid same-glyph cells, and map boundaries behave correctly.
- [x] **COLL-02 [MVP] Add swept/substep movement and sliding.** Bound substep distance, resolve X/Z separately, prevent tunneling, and permit natural wall sliding without entering corners. **Depends:** COLL-01. **Files:** `CollisionSystem.kt`. **Done when:** extreme test speed cannot cross one-cell walls and diagonal contact slides deterministically.
- [x] **COLL-03 [MVP] Add dynamic blockers.** Include closed doors and blocking NPC radii while ignoring rain and non-blocking objects; allow checking entity to ignore itself. **Depends:** COLL-02, WORLD-02. **Files:** `CollisionSystem.kt`, entity tests. **Done when:** each blocking/non-blocking case and index update passes.
- [x] **MOVE-01 [MVP] Implement yaw-relative continuous movement.** Build forward/right movement from held WASD, normalize diagonals, multiply by fixed delta/speed, and commit through collision. **Depends:** INPUT-01, LOOK-01, COLL-02. **Files:** `MovementSystem.kt`. **Done when:** W follows view, S reverses, A/D strafe, W+D is not faster, and arrows never translate.
- [x] **MOVE-02 [MVP] Add fixed-step simulation and pose interpolation.** Clamp frame delta, cap catch-up steps, store prior/current pose, and interpolate only for rendering. **Depends:** MOVE-01. **Files:** `GameScreen.kt`, proposed `FixedStepClock.kt`. **Done when:** simulated frame-rate sequences end at equivalent authoritative pose and a pause cannot spiral/burst.
- [x] **MOVE-03 [MVP] Test player movement.** Cover all keys, combined move/look, collision/slide, spawn clearance, bounds, large delta/substeps, diagonal normalization, and repeated deterministic traces. **Depends:** MOVE-02, COLL-03. **Files:** `MovementSystemTest.kt`, `CollisionSystemTest.kt`. **Done when:** expected positions match within documented float tolerance.
- [x] **LIFE-01 [MVP] Complete screen lifecycle.** Route resize/pause/resume/hide/dispose to input, viewports, audio hook, assets, and buffers; clear held state on modal/focus loss. **Depends:** MOVE-02, FRAME-04. **Files:** `GameScreen.kt`. **Done when:** resize/alt-tab/pause creates no stuck motion, jump, duplicate processor, or double dispose.
- [x] **VERIFY-04 [MVP] Verify walking and looking.** Traverse a documented route with all movement/look combinations, collide with representative geometry, resize while moving, and return to spawn coordinates. **Depends:** INPUT-01, LOOK-01/02, COLL-01 through COLL-03, MOVE-01 through MOVE-03, LIFE-01. **Files:** verification notes. **Done when:** controls match the agreed first-person mapping, perspective updates every turn/pitch, and automated suites pass.

### Phase 5 — HUD and interaction targeting

- [x] **HUD-01 [MVP] Build the first-person HUD.** Show `WASD Walk | Arrows Look | E Interact | Esc Back | M Music`, coordinates, yaw compass, a small center reticle, a clickable music control, and reserved one-line objective/signal regions through Scene2D. Accept immutable display values rather than reading story/world state directly. **Depends:** FRAME-04, WORLD-02. **Files:** `ui/Hud.kt`, proposed `ui/HudModel.kt`. **Done when:** HUD is readable, screen-anchored, unclipped at target sizes, and renders cleanly when objective/signal text is absent.
- [x] **HUD-02 [MVP] Add F3 diagnostics and optional footprint map.** Show FPS, frame grid, pose, FOV/horizon, ray/step/glyph counts, target ID/depth, current cell/material, and a small debug map marking player `@`. **Depends:** HUD-01, RAY-07. **Files:** `Hud.kt`. **Done when:** displayed data agrees with tests and toggling has no gameplay effect.
- [x] **INT-01 [MVP] Implement center-ray interaction queries.** Use center view ray plus vertical bounds/depth to select nearest visible interactable within 1.75 cells; reject occluded/out-of-range targets. **Depends:** RAY-01, WORLD-02. **Files:** `InteractionSystem.kt`, tests. **Done when:** centered, off-center, behind-wall, overlapping-depth, up/down-look, and range-boundary cases pass.
- [x] **INT-02 [MVP] Add prompts and one terminal interaction.** Load terminal data, show prompt only for the current valid target, return a typed target/result for `ShowMessage` or `StartDialogue`, and debounce E. Keep story progression outside `InteractionSystem` so Phase 7 can route the same validated interaction through `LastPacketSystem`. **Depends:** INT-01, HUD-01, MAP-06. **Files:** `Terminal.kt`, `InteractionSystem.kt`, object data. **Done when:** the terminal responds exactly once per key edge, never through geometry, and exposes its stable entity/interaction ID without mutating story state.
- [x] **INT-03 [MVP] Test input focus and interaction state.** Cover prompt transitions, turn-away, pitch-away, walk-out-of-range, modal capture, repeated open/close, and invalid target IDs. **Depends:** INT-02. **Files:** `InteractionSystemTest.kt`, UI-state tests. **Done when:** world look/movement cannot leak through a modal and safe errors replace bad data.
- [x] **VERIFY-05 [MVP] Verify Milestone 2 fixed-map play.** Launch fresh, walk/strafe/look through `city-alpha`, demonstrate collision, perspective, resize, F3 `@`, and terminal interaction. **Depends:** VERIFY-03, VERIFY-04, HUD-01/02, INT-01 through INT-03. **Files:** smoke route and capture. **Done when:** Milestone 2 definition passes from a packaged-like run with no error logs.

### Phase 6 — Cyberpunk presentation (Milestone 3)

- [x] **RAIN-01 [MVP] Implement deterministic allocation-free rain.** Derive stable per-column density, length, speed, offset, and glyphs from column hashes plus render time; resize naturally from frame bounds without mutable particle state. **Depends:** MOVE-02, FRAME-01. **Files:** `RainRenderer.kt`, `RainRendererTest.kt`. **Done when:** equal time/frame inputs repeat, time advances the streaks, falling motion uses smaller frame rows, and no particle collection grows.
- [x] **RAIN-02 [MVP] Composite screen-space rain.** Write rain after world geometry/fixtures and before future UI without writing world/collision state or overwriting finite-depth cells. **Depends:** RAIN-01, RAY-07. **Files:** `RainRenderer.kt`, `WorldAsciiRenderer.kt`, tests. **Done when:** rain overlays uncovered sky in desktop smoke checks, nearer geometry remains unchanged, and the world stays semantically immutable.
- [x] **NEON-01 [MVP] Animate facade windows and signals.** Use stable world coordinates plus time to pulse/flicker material colors/glyphs without mutating registry/collision. **Depends:** RAY-06, MAP-06. **Files:** `MaterialSampler.kt`, material data, tests. **Done when:** multiple facade samples vary predictably, identical inputs repeat, neutral ground keeps accent colors out, and world cells do not change.
- [x] **PARK-01 [MVP] Expand the spawn park into a landmark.** Replace the surrounding building block with walkable grass/cross-paths, a collision-aware fountain pond, benches, bins, pavilion bays, trees, and stable authored lamps. **Depends:** MAP-04, MAP-06, COLL-01. **Files:** cells/materials/palette data, `city-alpha.*`, map docs, integration tests. **Done when:** content validation passes, spawn connects directly to the path network, every park token is used, and semantic collision matches the authoring guide.
- [x] **NATURE-01 [MVP] Build the required Last Packet nature spur and Cascade Relay.** Continue the west park path across the street into a compact pine reserve with a main waterfall trail, a campsite loop, two walkable footbridges over a solid river, warm tents/lamps, a rock-backed animated waterfall, and reachable `terminal.relay-01` at its overlook. Keep the waterfall as shaped vertical glyph geometry with no slopes or vertical physics, retain semantic collision for every cell, and relocate any displaced façade landmark validly. This district and relay are mandatory parts of the Last Packet route, never optional or disconnected content. **Depends:** PARK-01, TREE-01, FIXTURE-01, INT-02. **Files:** cells/materials/audio data, `city-alpha.txt`, `city-alpha.objects.json`, tree/fixture renderers, spatial audio field, map docs, integration/render/audio tests, verification notes. **Done when:** all nature tokens are present, the park-to-waterfall route and relay are reachable, pine/tent/waterfall silhouettes pass focused tests, waterfall audio derives from semantic source cells and attenuates outside the district, content/build verification passes, and a 1280×720 runtime capture confirms the connected trailhead and nature landmark. **Evidence:** content validation reports 63 trail cells, 57 pines, 5 tents, 4 bridge cells, 2 waterfall cells, and reachable `terminal.relay-01`; the licensed waterfall master and seamless derivative validate as an independent proximity-mixed environmental layer; focused silhouette/content/audio tests, the core suite, `clean check`, and three visually approved 1280×720 captures pass on 2026-08-19.
- [x] **TREE-01 [MVP] Render recognizable park trees.** Mask tree-tagged wall spans into a narrow trunk plus broad layered canopy with transparent branch space while retaining cell collision. **Depends:** PARK-01, RAY-04. **Files:** `TreeSilhouette.kt`, `WorldAsciiRenderer.kt`, tests. **Done when:** silhouette tests prove trunk/open-side/canopy shape and the 1280×720 runtime view reads as trees.
- [x] **FIXTURE-01 [MVP] Shape semantic fixtures and landmarks.** Render benches, bins, fountain parts, open-sided pavilion bays, and casino slot-machine cabinets through tag-driven camera-facing silhouette masks without deriving collision from their visible glyphs. **Depends:** PARK-01, RAY-04. **Files:** `ParkFixtureSilhouette.kt`, `WorldAsciiRenderer.kt`, tests. **Done when:** each fixture has recognizable solid/open regions and clipping never indexes outside the frame.
- [x] **TRAFFIC-01 [Optional] Add light visual traffic.** Load a few fixed-loop road routes with static ASCII car sprites, render them with first-person depth testing, and keep them entirely outside world entities, collision, interaction, pathfinding, and persistence. **Depends:** SPRITE-02, MAP-06. **Files:** `TrafficCar.kt`, `TrafficSystem.kt`, `TrafficLabelRenderer.kt`, `WorldAsciiRenderer.kt`, `city-alpha.objects.json`, `sprites.json`, map-format docs, focused tests. **Done when:** six cars follow deterministic phase-offset loops on validated road waypoints at a brisk low-density pace, are visible as depth-tested billboards, preserve compact optional wordmarks, and cannot obstruct the player. **Evidence:** traffic route, loader, integration, compact-wordmark, and renderer tests plus content validation and 1280x720 visual capture pass on 2026-08-24.
- [x] **LIGHT-01 [MVP] Render street lamps and restrained glow.** Derive regularly spaced lamps from road/sidewalk topology, accept authored landmark fixtures, and depth-test posts/bulbs/local glow. **Depends:** RAY-07, MAP-06. **Files:** `StreetLampRenderer.kt`, object data, palette, tests. **Done when:** the road network and park receive lamps, authored fixtures do not duplicate derived cells, and nearer geometry occludes glyphs/glow.
- [x] **FOG-01 [MVP] Finish distance-fog and night-readability tuning.** Base distance shading already exists in `MaterialSampler`; define explicit tuning/configuration, minimum road/obstacle contrast, and reduced-flicker values. **Depends:** RAY-06, ASSET-04. **Files:** `presentation.json`, materials/palette, focused tests. **Done when:** near collision geometry remains legible while the distant skyline fades smoothly at required sizes.
- [x] **SIGN-01 [Optional] Add giant authored façade marquees.** Validate straight exterior wall runs, rectangular close/distant ASCII layouts, warm sign-only palette keys, roofline fit, and a restrained chasing-bulb rate; sample each sign at its host wall depth so perspective, fog, and occlusion remain correct. Keep this separate from camera-facing NPC/entity sprites. **Depends:** RAY-04, MAP-06, FOG-01. **Files:** `FacadeSign.kt`, `FacadeSignSampler.kt`, palette/object data, map/render docs, tests. **Done when:** three distinct landmarks render without changing collision, invalid placement/layout/palette data fails with exact paths, distance LOD preserves names, snapshot/allocation tests pass, and the 1280×720 visual capture is approved.
- [x] **AUDIO-01 [Optional] Implement `AudioService`.** Wrap independently leveled music/rain/SFX channels, lifecycle, asset ownership, music-mute state, and a no-op backend for missing devices/headless tests. **Depends:** ASSET-01, LIFE-01. **Files:** `AudioService.kt`. **Done when:** the tested mixer crossfades without restarting, music mute leaves rain/SFX audible, `--no-audio` runs, pause/resume is safe, and assets dispose once.
- [x] **AUDIO-02 [Optional] Add licensed city/rain ambience and feedback.** Loop ambience, add movement/interaction cues, avoid per-frame restart, and record licenses. **Depends:** AUDIO-01, MOVE-02, INT-02. **Files:** `assets/audio/*`, licenses, systems. **Done when:** loops/pause/exit work and each action emits at most one cue.
  - [x] Bundle generated city/park masters and normalized loop-ready OGG derivatives with provenance; crossfade zones semantically and play them at restrained background volume.
  - [x] Bundle the licensed Pixabay `Gentle Rain 07` master and a seamless loop derivative; run it once as a separately leveled environmental layer beneath both music zones.
  - [x] Provide synchronized `[M] MUSIC ON/OFF` HUD and keyboard controls that mute only city/park soundtracks while rain and feedback remain audible, including modal-safe input and the `--no-audio` fallback.
  - [x] Emit quiet alternating footsteps by actual distance traveled so normal walking/sliding has cadence while stationary wall pressure stays silent; select licensed natural recordings for semantic park/grass/path cells and pavement recordings for other walkable ground.
  - [x] Add an interaction one-shot asset and emit it at most once per accepted action.
- [x] **PRESENT-01 [Optional] Add subtle head bob.** Apply a small render-only eye-height offset tied to actual movement, leave authoritative collision/interaction poses untouched, reset across lifecycle transitions, and provide `--no-head-bob` for reduced motion. **Depends:** MOVE-02, PROJ-01. **Files:** `HeadBob.kt`, `GameScreen.kt`, tests. **Done when:** deterministic tests bound the offset, desktop verification remains readable, interaction/collision rays are unchanged, and the effect can be disabled at launch.
- [x] **VERIFY-06 [MVP] Verify cyberpunk presentation.** Run a brief deterministic walking/turning route through rain, neon, fog, HUD, pause, and resize; pair it with focused allocation tests and a semantic-world immutability digest. Include audio only if AUDIO-01/02 were chosen. **Depends:** RAIN-01/02, NEON-01, FOG-01. **Files:** verification/profile evidence. **Done when:** Milestone 3 passes, effects remain readable, and the bounded checks show no presentation-owned state growth.

### Phase 7 — Last Packet narrative game (Milestone 4)

- [x] **SPRITE-01 [MVP] Define static ASCII NPC/entity sprites.** Load one rectangular glyph/color image with an anchor, physical width/height, and transparent-space rules. The image persists across rendered frames; animated sprite sequences are post-MVP. **Depends:** FRAME-01, MAP-04. **Files:** `render/AsciiSprite.kt`, `content/AsciiSpriteRegistry.kt`, `assets/data/sprites.json`, `ContentLoader.kt`, format/authoring docs, tests. **Done when:** valid static sprites load and ragged/unknown-color/invalid-size sprites fail content validation. **Evidence:** `sprite.rin` loads as validated 9×10 static art with a bottom-center anchor; focused sprite/content/integration tests, `:core:validateContent`, and `clean check` pass on 2026-08-19.
- [x] **SPRITE-02 [MVP] Project and depth-test NPC/entity sprites.** Transform each entity relative to the view, make camera-facing sprites face the player, reject sprites behind/outside the FOV, scale them into rows/columns, clip, and compare world depth per character. **Depends:** SPRITE-01, PROJ-01, RAY-07. **Files:** `EntitySpriteRenderer.kt`, `ProjectionMetrics.kt`, rendering contract, tests. **Done when:** near/far/edge/behind-wall/partial-occlusion fixtures produce expected frame hashes. **Evidence:** exact five-fixture hashes plus scale, yaw-facing, behind-camera, out-of-FOV, distance, clipping, and character-level occlusion tests pass; `:core:validateContent` and `clean check` pass on 2026-08-19.
- [x] **NPC-01 [MVP] Complete Rin as the stationary story NPC.** Define stable ID `npc.rin`, position, radius, static ASCII sprite, blocking flag, facing metadata, display name, and dialogue references in object/data content. The prior record was placeholder evidence only; its stable ID remains locked while later story authoring may still refine its prose. **Depends:** WORLD-02, SPRITE-01, MAP-06. **Files:** `Npc.kt`, `LoadedContent.kt`, `ContentLoader.kt`, `city-alpha.objects.json`, sprite/dialogue data, map docs, tests. **Done when:** Rin renders at the authored position near the starting route, participates in collision, and every reference validates. **Evidence:** `npc.rin` resolves at `(45.5,16.5)` with radius `0.24`, explicit blocking, `sprite.rin`, west-facing `270°` metadata, display name `Rin`, and `dialogue.rin.intro`; focused domain/loader/integration tests prove exact-path failures, collision participation, and nonzero projection through the shared entity-sprite renderer. Content validation and the full build pass on 2026-08-19; NPC-02 remains the owner of live GameScreen composition.
- [x] **NPC-02 [MVP] Render stationary NPC behavior safely.** Add NPCs to the position index and camera-facing entity-sprite layer in stable ID order; keep them alive across culling/resize. **Depends:** NPC-01, SPRITE-02, COLL-03. **Files:** `NpcSystem.kt`, `GameWorld.kt`, `WorldAsciiRenderer.kt`, `WorldRenderStats.kt`, `GameScreen.kt`, rendering contract, tests, verification captures. **Done when:** five minutes of approach/turn-away/reapproach never desynchronizes rendering and collision. **Evidence:** Rin composes after opaque geometry/façades and before lamps/rain from her authoritative world position; a revision cache refreshes only after entity mutation and the steady path allocates at most the existing 4 KiB test threshold. Stable-order, wall-occlusion, move/remove invariant, 128×40/96×30 resize, and 18,000-frame five-minute-equivalent approach/turn-away/reapproach tests pass. Updated east/diagonal/close hashes, `clean check`, content validation, and visually approved 1280×720 spawn/close captures pass on 2026-08-19.
- [x] **WINDOW-01 [MVP] Expand the desktop renderer responsively for large and ultrawide displays.** Remove the 1280×720 upper native cap, retain the 960×540 minimum, scale glyph presentation uniformly by whole numbers, expand the logical frame with client size, keep HDPI backing pixels out of grid selection, widen horizontal projection from the 16:9 reference, and scale/re-anchor HUD plus modal dialogue. **Depends:** SETUP-03, FRAME-04, PROJ-01, HUD-01, DIALOGUE placeholder overlay. **Files:** `DesktopWindowSpec.kt`, `DesktopLauncher.kt`, `FontMetrics.kt`, `AsciiViewportCalculator.kt`, `AsciiPresenter.kt`, `ProjectionMetrics.kt`, `Hud.kt`, `MessageOverlay.kt`, `GameScreen.kt`, tests, contracts, captures. **Done when:** minimum/reference/intermediate/3440×1440/4K calculator fixtures pass; a real 3440-wide work-area launch selects 2× glyphs, preserves vertical framing, reveals more world horizontally, keeps the HUD/reticle aligned, and exits without errors; reference rendering remains unchanged. **Evidence:** focused viewport/projection/window/renderer/interaction tests and existing frame snapshots pass; `clean check`, content validation, 1280×720 reference smoke, and visually approved 3440-wide runtime capture pass on 2026-08-19.
- [x] **DIALOGUE-01 [MVP] Implement data-driven linear message dialogue.** Load stable dialogue IDs and show speaker/text through one modal Scene2D overlay for both Rin and terminals; E/Enter advances, Esc closes, and opening/closing clears gameplay input. No choices, conditions, typewriter effect, or dialogue graph are required. **Depends:** NPC-02, INT-03. **Files:** `dialogue.json`, `DialogueOverlay.kt`, loaders. **Done when:** centering Rin or a terminal and pressing E can complete at least two lines without movement/look leakage, including resize while open. **Evidence:** validated Rin and terminal dialogue references route through one responsive line-by-line overlay; focused session/interaction/content/input tests, `:core:validateContent`, and `clean check` pass. The scripted desktop interaction run opened the two-line archive terminal dialogue, retained modal capture through 960×540, 3440×1415, and 1280×720 resizes, advanced and closed with Enter, then reopened and closed with Escape without look/movement leakage on 2026-08-20.
- [ ] **DIALOGUE-02 [MVP] Test targeting and linear messages.** Cover center visibility, occlusion, range, sprite bounds, modal focus, advance/close, repeat, invalid dialogue reference, NPC/terminal speaker text, and resize while open. **Depends:** DIALOGUE-01. **Files:** interaction/dialogue tests. **Done when:** the complete interaction/message suite passes headlessly where possible and through one desktop smoke check.
- [ ] **TRAVEL-01 [MVP] Author the Last Packet map journey.** Add paired, visible, non-blocking travel doors with stable IDs and validated destination spawns for `city-alpha -> water-park -> city-alpha -> casino -> city-alpha`; retain `door.last-light-casino` and `door.casino-exit`, and add the water-park pair without relying on F3 map rotation. Travel may be used early, but later relay terminals show contextual out-of-order dialogue and cannot advance the story. **Depends:** INT-02, MAP-06. **Files:** `city-alpha.objects.json`, `water-park.objects.json`, map metadata, door/content/reachability tests. **Done when:** every direction loads the intended map at a walkable arrival pose, exits remain usable after each relay, no critical route depends on F3, and malformed map/door/spawn references fail content validation with the responsible file/path.
- [ ] **STORY-01 [MVP] Implement the explicit Last Packet state machine.** Add `LastPacketStage`, `LastPacketSystem`, an `AsciiCityGame`-owned `LastPacketSession`, and typed `StoryOutcome`; pass the same session to every replacement `GameScreen` so authored map travel preserves progress. Route validated stable interaction IDs through the seven-stage contract documented above. Accept only the expected progress interaction, return contextual text for early/repeated targets, and make every transition idempotent. **Depends:** INT-03, DIALOGUE-01, TRAVEL-01. **Files:** proposed `story/LastPacketStage.kt`, `LastPacketSystem.kt`, `LastPacketSession.kt`, `StoryOutcome.kt`, `AsciiCityGame.kt`, `GameScreen.kt`, tests. **Done when:** table-driven tests cover the complete route across screen replacements, every out-of-order interaction, every repeat, return trips, and prove no input advances more than one stage.
- [ ] **STORY-02 [MVP] Author the three-relay journey and HUD guidance.** Keep reachable `terminal.relay-01` locked to the NATURE-01 Cascade Relay waterfall overlook on `city-alpha`; repurpose the existing `terminal.water-control` as Relay 2 on `water-park` and `terminal.casino-jackpot` as Relay 3 on `casino`; write the required briefing, fragment, locked/repeat, archive, and return dialogue IDs; make `ContentVerifier` require every Last Packet object/dialogue/type/map/travel reference; feed the current objective's map plus eight-way bearing/three-band signal strength into `HudModel`. **Depends:** STORY-01, NPC-02, NATURE-01, MAP-06, HUD-01. **Files:** `city-alpha.objects.json`, `water-park.objects.json`, `casino.objects.json`, `dialogue.json`, `ContentVerifier.kt`, `LastPacketSystem.kt`, `Hud.kt`, content/integration tests, map authoring docs if the object guidance changes. **Done when:** all required IDs and types validate with file/path diagnostics, every target is reachable from the prior target through authored doors, the opening recovery leg necessarily traverses the nature spur, water-park and casino give visually distinct chapters, guidance updates deterministically while walking or upon arrival, and the prose forms one coherent mystery without requiring inventory or a quest log.
- [ ] **DOOR-01 [MVP] Implement the one-way archive seal.** Reauthor the doorway footprint under `door.archive` as walkable, ensure `terminal.archive` is reachable only beyond that doorway, render the closed door as a depth-tested blocking entity, and remove that entity exactly once when Relay 3 completes. Early archive interaction shows locked feedback; the door cannot be closed again during the run. Do not build general toggleable/locked door machinery. **Depends:** STORY-01, SPRITE-02, COLL-03, RAY-06. **Files:** `city-alpha.txt`, `city-alpha.objects.json`, `LastPacketSystem.kt`, `GameWorld.kt`, entity-sprite/collision/reachability tests. **Done when:** the closed seal is the only route blocker to the archive terminal, it blocks and occludes, Relay 3 opens rendering and collision together, repeated completion is harmless, and the player can enter without clipping or stale spatial-index occupancy.
- [ ] **STORY-03 [MVP] Complete the archive, return, and ending flow.** After Casino Relay 3, require the player to use the existing casino exit back to `city-alpha`; make `terminal.archive` advance only after the seal opens, show the final transmission, change the objective to Rin, then use the existing `DialogueOverlay` to show Rin's conclusion and a non-branching `TRANSMISSION DELIVERED` completion state. Esc closes modal text safely; completion remains stable until exit. **Depends:** STORY-02, DOOR-01, DIALOGUE-02. **Files:** `LastPacketSystem.kt`, `DialogueOverlay.kt`, story/UI tests. **Done when:** a clean session reaches `COMPLETE`, early archive access cannot advance, no further interaction changes progress, and relaunch starts a fresh session at `SEEK_RIN`.
- [ ] **NPC-03 [Post-MVP] Implement deterministic patrols.** Move NPCs between cell-center waypoints at fixed intervals/speeds, share collision, process stable IDs, and wait rather than overlap/swap. **Depends:** NPC-02, MOVE-03. **Files:** `NpcSystem.kt`, NPC data. **Done when:** two patrols run 1,000 ticks without wall clipping, overlap, or update-order variation.
- [ ] **DIALOGUE-03 [Post-MVP] Add dialogue choices and conditions.** Use stable node IDs, explicit next IDs, choice keys, inventory/world predicates, and validator for missing/unescapable nodes. **Depends:** DIALOGUE-02. **Files:** dialogue DTO/loader/UI. **Done when:** one branching conversation completes every branch and invalid graphs fail `check`.
- [ ] **VERIFY-07 [MVP] Verify Last Packet end to end.** From a fresh launch, speak to Rin, deliberately test one early relay, recover Cascade Relay 1, traverse into and out of water-park for Relay 2, traverse into and out of the casino for Relay 3, verify the city archive opens, read the final transmission, return to Rin, reach completion, then relaunch and confirm reset. During the route also test each travel arrival, occlusion/range, repeated interaction, dialogue resize/cancel, and focus recovery. **Depends:** SPRITE-01/02, NPC-01/02, DIALOGUE-01/02, TRAVEL-01, STORY-01 through STORY-03, DOOR-01. **Files:** `DOCS/verification-notes.md`, deterministic route/capture. **Done when:** the entire advertised game loop completes across all required maps with rendering, collision, target, objective, dialogue, travel, and story stage in agreement and no error logs.
- [ ] **VERIFY-07B [Post-MVP] Verify optional interactive expansion.** Exercise patrol conflicts and every branch/condition added after release without changing the verified linear Last Packet path. **Depends:** NPC-03, DIALOGUE-03. **Files:** verification notes/tests. **Done when:** optional interaction features pass deterministically and the original route remains green.

### Phase 8 — Testing, diagnostics, and performance

- [ ] **TEST-01 [MVP] Create reusable headless fixtures.** Build registries, small grids, player poses, entities, input traces, frames, and deterministic clocks without OpenGL. **Depends:** MAP-02, WORLD-02, FRAME-01. **Files:** `TestWorldFactory.kt`, `TestFrameFactory.kt`. **Done when:** parser/collision/movement/raycast/interaction tests share fixtures without production asset coupling.
- [ ] **TEST-02 [MVP] Add asset/content failure regression tests.** Cover missing font texture/descriptor, palette/material/cell/map files, invalid colors/glyphs/heights/sprites/objects/dialogue, missing or mistyped required Last Packet IDs, and friendly error mapping. **Depends:** ASSET-05, MAP-05, SPRITE-01, STORY-02. **Files:** asset/content tests. **Done when:** every failure is critical/recoverable by policy, story errors identify the responsible file/path or stable ID, and no case becomes an untyped null error.
- [ ] **TEST-03 [MVP] Complete the responsive-window matrix.** Record logical/drawable pixels, columns/rows, glyph scale, gutters, effective horizontal/vertical FOV, glyph sharpness, HUD clipping, interaction center, rain coverage, and FPS at 960×540, 1024×576, 1152×648, 1280×720, 1600×900, 1920×1080, 2560×1080, 2560×1440, 3440×1440 or its maximized work area, 3840×2160, representative non-16:9 sizes, and common Windows scaling. **Depends:** FRAME-04, PROJ-01, WINDOW-01, VERIFY-06. **Files:** `DOCS/verification-notes.md`. **Done when:** every required size has dated pass evidence, the 960×540 minimum is enforced, larger/maximized areas expand without distortion, and HDPI backing density alone never changes the logical grid.
- [ ] **TEST-04 [MVP] Define the end-to-end smoke route.** Include launch, eight-direction look, pitch limits, W/S/A/D movement, diagonal normalization, wall slide, skyline, neon/rain, Rin briefing, three relays, archive unlock/transmission, return/ending, F3 `@`, resize, pause, relaunch reset, and exit. **Depends:** VERIFY-07. **Files:** verification notes. **Done when:** a fresh build completes the full Last Packet route with expected stage checkpoints and no error logs.
- [ ] **PERF-01 [Optional] Benchmark a large fixed map only before expanding beyond `city-alpha`.** Load at least 1000×1000 cells and measure memory/load, DDA lookup, collision, and content validation independently of rendering. **Depends:** MAP-04, RAY-02, PACK-04. **Files:** benchmark harness/notes. **Done when:** recorded budgets show constant-time cell/collision queries and acceptable memory/load on development hardware.
- [ ] **PERF-02 [MVP] Benchmark renderer worst cases.** Test the 128×40 reference, required 3440-wide work-area grid, widest supported test display, render distance, dense varied-height skyline, NPC/entity sprites, rain, and debug off/on; capture median/95th frame time, DDA steps, allocations, and heap. **Depends:** RAY-08, SPRITE-02, RAIN-02, WINDOW-01. **Files:** benchmark harness/notes. **Done when:** chosen scale policy sustains the recorded 60-FPS budget with bounded allocations; otherwise tune responsive scale thresholds before release.
- [ ] **TEST-05 [MVP] Add numerical edge-case tests.** Cover rays on grid lines/corners, zero/near-zero direction components, horizon division, maximum pitch/FOV, inside-solid recovery, exact collision tangency, and float non-finite guards. **Depends:** PROJ-01, RAY-04, COLL-02. **Files:** render/system tests. **Done when:** no fixture hangs, emits NaN/infinity, or reads outside arrays.
- [ ] **VERIFY-08 [MVP] Run the quality gate.** Execute clean check/content validation, resolution matrix, full Last Packet smoke route, renderer stress, and ten-minute soak. The optional large-map benchmark is not a release gate. **Depends:** TEST-01 through TEST-05, PERF-02. **Files:** verification notes/reports. **Done when:** every required check has pass evidence or a release-blocking defect ID.

### Phase 9 — Packaging and MVP release

- [ ] **PACK-01 [MVP] Configure desktop distribution.** Produce a runnable fat JAR and/or runtime image with LWJGL natives, assets, main class, title, and JVM options. **Depends:** SETUP-03, ASSET-02. **Files:** `desktop/build.gradle.kts`. **Done when:** artifact launches outside IDE from a path containing spaces.
- [ ] **PACK-02 [MVP] Eliminate working-directory assumptions.** Run from a clean temporary location and verify font/data/map/sprite lookup, optional audio lookup when bundled, and local log paths. **Depends:** PACK-01, ASSET-05. **Files:** release checklist. **Done when:** no source-tree-relative path is required.
- [ ] **PACK-03 [MVP] Package controls, premise, requirements, and licenses.** Document the Last Packet premise without spoiling its ending, first-person controls, rendering expectations, troubleshooting, system requirements, and every bundled third-party asset/library license. **Depends:** STORY-03, PACK-01. **Files:** `README.md`, `THIRD_PARTY_LICENSES.md`, licenses. **Done when:** a new player understands the goal and controls and every bundled external asset is accounted for.
- [ ] **PACK-04 [MVP] Produce the release candidate.** Clean build/test/validate, package, checksum, run the exact Last Packet smoke route from the artifact, and record version/known issues. **Depends:** all MVP tasks and VERIFY-08. **Files:** release checklist/notes. **Done when:** Milestones 1–4, including the complete Rin/relay/archive/return story, pass from the exact artifact.
- [ ] **PACK-05 [Post-MVP] Add native installers.** Use maintained desktop packaging, bundle runtime, set icons/metadata, and test install/upgrade/uninstall without deleting saves. **Depends:** PACK-04. **Files:** packaging config/platform notes. **Done when:** every claimed OS has recorded lifecycle evidence.
- [ ] **VERIFY-09 [MVP] Verify artifact independence.** Install/unpack exact candidate cleanly, run smoke route, resize/HDPI, inspect licenses, and exit. **Depends:** PACK-01 through PACK-04. **Files:** checksum/release evidence. **Done when:** artifact—not IDE—passes with no missing assets or error logs.

### Phase 10 — Save/load and content expansion

- [ ] **SAVE-01 [Post-MVP] Define versioned save DTOs.** Store schema/game version, timestamp, map ID/version or seed/generator version, player X/Z/yaw/pitch, `LastPacketStage`, entity states, inventory, chunks, and deltas by stable IDs. **Depends:** STORY-03, NPC-03. **Files:** `SaveGameDto.kt`, documented JSON example. **Done when:** no DTO contains runtime indices or LibGDX/render objects and story/door state has one consistent representation.
- [ ] **SAVE-02 [Post-MVP] Map live state to/from candidate DTO state.** Validate positions, finite values, collision-free spawn, content IDs, and invariants before constructing a new world. **Depends:** SAVE-01. **Files:** `SaveService.kt`, proposed `WorldStateMapper.kt`. **Done when:** invalid input leaves active world semantically unchanged.
- [ ] **SAVE-03 [Post-MVP] Implement atomic slots.** Sanitize slot names, write/flush/close a sibling temp file, replace destination, and retain one recoverable backup in documented local storage. **Depends:** SAVE-02. **Files:** `SaveService.kt`. **Done when:** injected pre-replace failure preserves previous slot and path traversal is rejected.
- [ ] **SAVE-04 [Post-MVP] Add validation and migrations.** Migrate supported older schema, reject future/corrupt/oversized/non-finite content clearly, and swap world only after total success. **Depends:** SAVE-03. **Files:** `SaveMigration.kt`, fixtures, UI error flow. **Done when:** current/old fixtures load and bad fixtures fail safely.
- [ ] **SAVE-05 [Post-MVP] Test save/load round trips.** Compare player pose, Last Packet stage/archive seal invariant, NPC patrol, inventory, fixed-map deltas, generated seed/chunks, and unsupported schemas. **Depends:** SAVE-04. **Files:** `SaveServiceTest.kt`. **Done when:** semantic comparisons match and corrupt saves never partially load.
- [ ] **SAVE-06 [Post-MVP] Add pause save/load UI.** Provide slots/status/errors, define autosave policy, and capture only between complete simulation steps. **Depends:** SAVE-05. **Files:** proposed `PauseOverlay.kt`, `GameScreen.kt`. **Done when:** packaged save/exit/relaunch/load restores exact gameplay state and focus.
- [ ] **CONTENT-01 [Post-MVP] Consolidate validated content registries.** Move NPC templates, NPC/entity sprites, dialogue, doors, interactables, items, materials, palette, and tuning behind one validation entry point and stable namespace. **Depends:** DIALOGUE-02, DOOR-01. **Files:** `assets/data/*.json`, `ContentLoader.kt`. **Done when:** content-only changes add an NPC/dialogue/door without Kotlin changes and bad references fail `check`.
- [ ] **VERIFY-10 [Post-MVP] Verify persistence and content.** Change world/player pose, save, exit/relaunch/load, interrupt a save, recover previous slot, and add one content-only object. **Depends:** SAVE-01 through SAVE-06, CONTENT-01. **Files:** verification notes/fixtures. **Done when:** state matches, recovery succeeds, and invalid content cannot replace active world.

### Phase 11 — Seamless procedural city (Milestone 5)

- [ ] **PROC-01 [Post-MVP] Lock generation contracts.** Define 32×32 chunks, global/local conversion, seed/version, stages, RNG mixing, shared edge key, reserved road cells, building IDs/heights, and validation. **Depends:** MAP-03, SAVE-01. **Files:** `DOCS/procedural-generation.md`, `ChunkCoord.kt`. **Done when:** exact formulas make two neighbors independently compute identical sample connectors.
- [ ] **PROC-02 [Post-MVP] Partition deterministic RNG streams.** Derive separate roads, lots, heights, facades, fixtures, and entities streams from seed/chunk/stage. **Depends:** PROC-01. **Files:** proposed `SeedMixer.kt`, tests. **Done when:** extra cosmetic RNG calls cannot change road/lot hashes.
- [ ] **PROC-03 [Post-MVP] Generate shared road/edge contracts.** Reserve matching north/south/east/west openings and compatible ground/collision before interiors. **Depends:** PROC-02. **Files:** `EdgeContract.kt`. **Done when:** a 10×10 chunk region across 100 seeds has zero edge mismatch.
- [ ] **PROC-04 [Post-MVP] Generate connected interiors and heights.** Lay roads, subdivide lots, create building footprints/doors/alleys, assign bounded heights/materials, flood-fill required walking space, and use bounded retry/fallback. **Depends:** PROC-03. **Files:** `CityGenerator.kt`, stage classes. **Done when:** every chunk honors connectors, has reachable streets, valid spawnable ground, registered materials, and bounded runtime.
- [ ] **PROC-05 [Post-MVP] Add deterministic props, NPCs, and fixtures.** Use stage RNG, stable generated IDs, collision/visibility checks, and chunk signatures. **Depends:** PROC-04, NPC-03. **Files:** generator/entity factories. **Done when:** repeated generation produces identical content and never blocks mandatory connectors.
- [ ] **PROC-06 [Post-MVP] Test determinism and perspective seams.** Generate in different neighbor orders, compare full signatures/edges, and render poses looking across each boundary for ground/facade discontinuity. **Depends:** PROC-05, RAY-09. **Files:** `CityGeneratorTest.kt`, frame fixtures. **Done when:** 100 seeds yield identical hashes, zero connector mismatch, and no visible/collidable boundary seam.
- [ ] **PROC-07 [Post-MVP] Implement bounded chunk access/cache.** Generate on demand, resolve global queries, retain dirty deltas, and evict by radius without exposing unloaded gaps to movement/rays. **Depends:** PROC-06, SAVE-04. **Files:** proposed `ChunkWorld.kt`, `GameWorld.kt`. **Done when:** crossing/revisiting preserves mutations and memory follows cache bounds.
- [ ] **PROC-08 [Post-MVP] Integrate raycasting/collision/interaction through `WorldGridView`.** Put fixed and chunk worlds behind one query boundary without changing their semantics. **Depends:** PROC-07, COLL-03, RAY-07. **Files:** proposed `WorldGridView.kt`, affected systems. **Done when:** common tests pass against both backends and a ray/movement step crosses a chunk with no missing frame.
- [ ] **PROC-09 [Post-MVP] Stress the expandable world.** Walk/render hundreds of chunks, monitor cache, generation latency, DDA work, save/reload deltas, and deterministic hashes. **Depends:** PROC-08. **Files:** performance harness/notes. **Done when:** memory is bounded, travel remains playable, deltas survive, and hashes survive restart.
- [ ] **PROC-10 [Optional] Add background chunk generation only if profiling requires it.** Use bounded executor, immutable validated candidates, game-thread publication, priority/cancellation, and clean shutdown. **Depends:** PROC-09 plus measured stalls. **Files:** proposed `ChunkGenerationQueue.kt`. **Done when:** frame spikes improve, determinism remains, and no worker survives exit.
- [ ] **VERIFY-11 [Post-MVP] Verify Milestone 5.** Traverse at least 25 chunks and every edge orientation, inspect first-person seams, revisit changes, save/reload, and run seed/cache suites. **Depends:** PROC-01 through PROC-09. **Files:** verification evidence. **Done when:** full Milestone 5 definition passes.

### Phase 12 — Inventory and optional game expansion

- [ ] **ITEM-01 [Post-MVP] Define item registry and inventory.** Add stable item ID, glyph/color/name/description/tags/stack limit and ordered capacity-limited stacks with atomic add/remove. **Depends:** CONTENT-01. **Files:** item package, `items.json`. **Done when:** stacking, capacity, exact removal, unknown IDs, and no-partial-failure tests pass.
- [ ] **ITEM-02 [Post-MVP] Build a cyberdeck inventory overlay.** Use existing font/palette, keyboard navigation, glyph/name/count/description, empty state, and modal focus. **Depends:** ITEM-01, HUD-01, DIALOGUE-01. **Files:** proposed `InventoryOverlay.kt`, input mappings. **Done when:** open/navigate/close never moves/looks and remains readable at target sizes.
- [ ] **ITEM-03 [Post-MVP] Connect pickups, terminals, and dialogue to inventory.** Add typed give/take/use/check results; one `InventorySystem` applies atomic mutations while UI/scripts request outcomes. **Depends:** ITEM-02, DIALOGUE-03. **Files:** `InventorySystem.kt`, interactions/dialogue/content. **Done when:** one access shard is granted once, detected in dialogue, and consumed/checked by one locked target without duplication.
- [ ] **ITEM-04 [Post-MVP] Persist inventory.** Save stable IDs/quantities, validate capacity/limits, define obsolete-item migration, and round-trip. **Depends:** ITEM-03, SAVE-04. **Files:** save DTO/service tests. **Done when:** inventory restores exactly and invalid item state cannot partially load.
- [ ] **FUTURE-01 [Optional] Add mouse look and configurable controls.** Define sensitivity, capture/release, inversion, bindings/conflicts, reduced motion, and persistence. **Depends:** PACK-04, SAVE-04. **Files:** settings/input/UI. **Done when:** keyboard behavior remains valid and mouse focus never traps the user without an escape path.
- [ ] **FUTURE-02 [Optional] Replace the single Last Packet state machine with a general quest system only from a separate focused plan.** Define stable quest/objective IDs, event inputs, item/dialogue conditions, atomic rewards, UI, save migration, and validation while preserving the original route. **Depends:** ITEM-04, Milestone 5. **Files:** future devplan and proposed `quest/*`. **Done when:** focused plan is approved; this item does not authorize ad hoc quest abstractions during MVP work.
- [ ] **FUTURE-03 [Optional] Evaluate shader/3D alternatives only from evidence.** Compare CPU raycasting limits against an offscreen 3D+ASCII shader prototype without replacing the working renderer. **Depends:** measured unmet visual/performance need after Milestone 3. **Files:** architecture decision/isolated benchmark. **Done when:** a decision records image quality, performance, portability, complexity, and migration cost.
- [ ] **FUTURE-04 [Optional] Evaluate ECS only from entity-complexity evidence.** Measure plain-system pain, prototype one narrow path, and document migration. **Depends:** substantially expanded behaviors. **Files:** architecture decision/prototype. **Done when:** evidence accepts/rejects ECS; none is added speculatively.

## 5. Milestone definitions of done

### Milestone 1: Window and glyph rendering

- A clean Gradle build launches a resizable LWJGL3 desktop window at 1280×720 with an enforced 960×540 minimum and no upper resize/maximize cap.
- A bundled licensed monospace bitmap font presents a colored, aligned ASCII perspective test card.
- The reference surface is exactly 128×40 at 1280×720; larger clients expand the grid and select uniform whole-number glyph scaling, while HDPI backing pixels alone never change ray count.
- Critical missing assets show a readable controlled error.
- `./gradlew clean check` and VERIFY-01 pass.

### Milestone 2: Playable fixed map

- `city-alpha` loads and validates from text/JSON content.
- The main view is unmistakably first-person: road edges converge, building scale/occlusion changes with distance, pitch moves the horizon, and yaw turns the city.
- WASD provides continuous camera-relative walking/strafe; arrows provide yaw/pitch only; collision blocks semantic solid cells, doors, and NPCs.
- The renderer handles varied building heights, fog/materials, resize, and bounded output without fisheye or numerical failures.
- HUD shows controls/pose/reticle; F3 may show `@` on a debug footprint.
- One terminal interaction works through center-ray range/visibility targeting.
- VERIFY-02 through VERIFY-05 pass.

### Milestone 3: Cyberpunk presentation

- Screen-space ASCII rain, flowing/vibrating data-material animation, neon facade signals, distance fog, and HUD work together; ambience and feedback may be added later without blocking release.
- Effects never mutate grid/collision and remain readable at required resolutions.
- A deterministic presentation route and worst-case renderer profile show bounded memory/allocations and meet the recorded frame budget.
- VERIFY-06 and performance gates pass.

### Milestone 4: Interactive city

- A fresh session begins at `SEEK_RIN` and completes the exact `city-alpha` Rin/Cascade Relay -> `water-park` Relay 2 -> `casino` Relay 3 -> `city-alpha` archive/Rin route.
- Three geographically separated relays reveal distinct fragments in order; objective bearing/signal guidance is sufficient to find them without a normal-view minimap route.
- Relay 3 removes the archive seal once, changing visible occlusion and semantic collision together; the archive cannot advance early.
- The final transmission and Rin's conclusion produce an unmistakable `TRANSMISSION DELIVERED` completion state, while relaunch resets cleanly.
- Early, repeated, canceled, occluded, and out-of-range interactions never skip or duplicate progress.
- Rendering depth, collision, interaction target, prompt, objective, dialogue, story state, and audio (when enabled) always agree.
- STORY-01 through STORY-03, DOOR-01, DIALOGUE-02, and VERIFY-07 pass. Patrols, branching dialogue, and general reusable quests remain post-MVP.

### Milestone 5: Expandable world

- Versioned atomic saves preserve continuous player pose and semantic world/entity/inventory state without partial-load failure.
- Procedural chunks are deterministic by seed/version and share identical road/ground/collision edge contracts.
- First-person rays, movement, and interaction cross chunk boundaries without visible or collidable seams.
- Cache memory is bounded, dirty deltas survive eviction/reload, and 100-seed plus 25-chunk verification passes.
- VERIFY-10 and VERIFY-11 pass from a packaged build.

## 6. Risks and likely technical problems

| Risk | Symptom | Mitigation and proof |
|---|---|---|
| CPU raycaster scope is underestimated. | MVP stalls on skyline rendering. | Build vertical slices in order: DDA -> one wall -> multi-height -> ground -> material -> sprite; each has headless fixtures. |
| Character-cell aspect is ignored. | Buildings look stretched and FOV changes by resolution. | Centralize physical pixel/cell projection in `ProjectionMetrics`; test multiple sizes. |
| Euclidean ray distance causes fisheye. | Walls curve across the screen. | Project perpendicular camera-space depth; RAY-03 cross-column test. |
| DDA corner ties are unstable. | Rays flicker/leak at exact grid corners. | Lock deterministic tie policy and randomized property tests. |
| First opaque hit hides skyline. | Taller distant buildings vanish behind short ones. | Multi-hit traversal plus per-character depth/vertical coverage; RAY-04 fixtures. |
| Ground math fails at horizon/pitch limit. | NaN, flashing rows, or inverted road. | Epsilon/finite guards, row classification, TEST-05 numerical cases. |
| Ceiling plane disagrees with wall depth or leaks sky rain. | Roof draws through nearby walls, leaves outdoor holes, or admits rain indoors. | Reuse perpendicular plane depth, bound samples to map X/Z, compose before opaque walls/rain, and lock top-row/rain/hash fixtures; INTERIOR-01. |
| Continuous movement tunnels or sticks. | Player crosses thin walls or snags corners. | Bounded substeps, circle collision, axis sliding, extreme-speed tests. |
| Movement speed changes diagonally/frame rate. | W+D or high-refresh runs faster. | Normalize vector and fixed-step integration; MOVE-03 traces. |
| Render interpolation crosses yaw the long way. | Full spin near 0/360 degrees. | Shortest-angle interpolation; LOOK-02 wrap tests. |
| Large/maximized or HDPI windows distort glyphs or explode renderer work. | Glyph aspect drifts, HUD/reticle misalign, or backing-pixel density multiplies ray count. | Uniform integer presentation scale, minimum-surface guard, work-area-aware scale threshold, logical-only grid selection, Hor+ projection, and responsive matrix/performance gates; WINDOW-01, FRAME-04, TEST-03, PERF-02. |
| Per-ray/per-glyph allocation creates GC spikes. | Rhythmic hitching during turns/rain. | Primitive arrays/reused scratch data and allocation profile gate. |
| Entity-sprite and wall depth disagree. | NPC draws through buildings or disappears. | Shared perpendicular depth convention and SPRITE-02 occlusion fixtures. |
| Center interaction disagrees with what player sees. | Prompt targets hidden/off-center entity. | Use same view/projection/depth semantics and INT-01 tests. |
| Material visuals leak into collision. | Reskin changes walkability. | Semantic `solid` only; same-glyph/different-collision tests. |
| Font lacks glyph or inconsistent advance. | Missing boxes/column drift. | Printable ASCII validator and ASSET-03 representative grid. |
| Pitch/rapid glyph motion causes discomfort. | Eye strain or disorientation. | Clamp pitch, configurable speeds/FOV, reduced flicker/rain, head bob optional/disabled. |
| Map is valid but streets are unreadable first-person. | Player gets lost in repetitive canyons. | Author landmarks, color districts, sight lines, compass, and smoke-route review. |
| Objective guidance is too vague or too exact. | The player wanders without progress or follows numbers instead of reading the city. | Use eight-way bearing plus three coarse signal bands, place each relay by a distinct landmark, and tune thresholds through the full-route playtest. |
| Story input advances twice or out of order. | One key press skips a fragment or an early relay opens the archive. | Route edge-triggered stable IDs through one idempotent `LastPacketSystem`; table-test every stage/target pair and repeats. |
| Archive rendering and collision disagree. | The seal looks open but still blocks, or can be walked through while visible. | Put the seal entity over a walkable authored cell and remove it once from `GameWorld`/spatial index; verify both frame depth and collision before/after Relay 3. |
| Malformed content partially constructs world. | Index mismatch or startup crash. | Validate immutable DTOs fully before world creation. |
| Saves contain non-finite/out-of-wall pose. | Load crash/stuck player. | Validate finite position/yaw/pitch and collision clearance before swap. |
| Procedural edges match roads but not perspective materials. | Visible ground/facade seam at boundary. | Edge contract includes ground/material continuity and rendered seam fixtures. |
| Background generation adds races. | Half-built chunks/nondeterminism/shutdown hang. | Stay synchronous until profiling; publish only immutable validated candidates if PROC-10 is justified. |
| Packaging depends on IDE paths. | Assets work only via `run`. | LibGDX internal assets and clean-path artifact verification. |
| Learning sessions become oversized. | Long stall with no green feedback. | One concept/session, three micro-wins, cut after stalled 25–60 minutes per `LEARNING_STRATEGY.md`. |

## 7. Current next five tasks

1. **DIALOGUE-02:** Test targeting, modal focus, linear messages, and resize behavior.
2. **TRAVEL-01:** Author and validate the city -> water-park -> city -> casino -> city door journey.
3. **STORY-01:** Implement the one idempotent story session across map-screen replacement.
4. **STORY-02:** Author the three-relay route, dialogue content, validation, and cross-map HUD guidance.
5. **DOOR-01 / STORY-03:** Open the archive after Casino Relay 3, then complete the return-to-Rin ending.

The required Phase 6 scope, mandatory nature-spur/Cascade Relay route, optional ambience/footsteps/head bob/façade marquees, responsive `WINDOW-01` revision, and linear `DIALOGUE-01` presentation are verified complete. SPRITE-01/02 provide the shared validated static-art registry/projector; NPC-01/02 pair Rin's typed metadata and authoritative blocking world entity with stable, depth-tested live composition. DIALOGUE-02 is next. TRAVEL-01 then turns the existing water-park and casino into compact story chapters without F3 map rotation; STORY-02 must preserve Cascade Relay as the fixed first recovery landmark while using those two existing terminals for the remaining relays. Continue Phase 7 in listed dependency order through `VERIFY-07`. Only then may the optional [life pass](devplan-life-pass.md) begin, reusing the proven entity-sprite and NPC occupancy path; it does not block Phase 8, packaging, or release. An optional interaction cue remains non-blocking.

## 8. Features that should not be included in the MVP

- Overhead/isometric gameplay as a second main view; only an F3 debug footprint may use `@`.
- `PerspectiveCamera`/`ModelBatch`, imported 3D models, arbitrary meshes, skeletal animation, or any shader-based ASCII post-process beyond the one bounded cell-aware composite pass required by `devplan-city-visual-density.md`.
- Arbitrary polygon buildings, diagonal walls, curves, slopes, stairs, elevators, rooftops, stacked walkable floors, jumping, crouching, or vertical physics.
- Mouse look, controllers, touch, key-rebinding UI, or broad accessibility settings beyond readable defaults and reduced-flicker-safe tuning points.
- Procedural generation, chunk streaming, background generation, save/load, autosave, or cloud saves.
- Inventory, equipment, crafting, loot, shops, economy, reputation, branching dialogue, or a general quest framework. The single explicit Last Packet state machine is the complete MVP story.
- Patrol/pathfinding/chase/combat AI; the MVP NPC is stationary.
- Combat, weapons, health, death, stealth, wanted/police systems, vehicles, or fail states.
- General reusable, toggleable, or relockable doors. The single one-way `door.archive` seal is allowed and opens permanently after Relay 3 for the current run.
- Multiplayer, networking, accounts, achievements, leaderboards, analytics, or telemetry.
- Mod discovery, external scripts, hot reload, user map editor, or arbitrary file access.
- Unicode/emoji content beyond validated printable ASCII.
- Dynamic shadows, general real-time lighting, arbitrary bloom shaders, volumetric fog, reflections, or general particle engine. The required cell-aware contact-shade/local-light/emissive-halo composite remains the sole limited exception.
- Pixel-perfect reproduction of the reference video, its city, colors, layout, audio, or assets.
- A full ECS, dependency-injection framework, event-bus framework, database, or premature multithreading.
- Exclusive or borderless fullscreen modes; large and maximized resizable windows are supported without a dedicated fullscreen toggle.
- Native installers for every OS; one independently runnable responsive desktop artifact is enough for MVP.

The MVP is complete only when the packaged first-person fixed-city build satisfies the walking/look/perspective requirements, passes all MVP automated tests and verification gates, and completes the documented Last Packet route from Rin's briefing through `TRANSMISSION DELIVERED`. Deferred systems must not delay it unless they reveal a correctness failure in an MVP-owned foundation.
