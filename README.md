<p align="center">
  <img src="DOCS/verification/visual-density-compositor/city-fine-default-optimized-1280x720.png" alt="Last Packet: ASCII City — neon first-person street" width="100%" />
</p>

<h1 align="center">Last Packet: ASCII City</h1>

<p align="center">
  <strong>FIRST-PERSON ASCII CYBERPUNK EXPLORATION</strong><br />
  Walk rain-soaked neon streets. Follow failing signals. Find the last message still trying to get through.
</p>

<p align="center">
  Kotlin &middot; LibGDX &middot; LWJGL3 &middot; CPU-rendered perspective ASCII
</p>

> The exploration and presentation foundation is playable today. The complete Last Packet relay/archive/ending route is still in development.

`Last Packet: ASCII City` is a desktop-first cyberpunk exploration game built from data-authored maps, semantic collision, and a living first-person ASCII renderer. It is not a top-down tile game: façades, roads, terminals, lights, weather, and characters resolve into one depth-tested perspective frame.

## A city made of text

| Fine city streets | Archive district | Last Light Casino |
|---|---|---|
| ![Charcoal road and neon façades](DOCS/verification/road-asphalt-1280x720.png) | ![Archive façade](DOCS/verification/visual-density-phase3-archive-1280x720.png) | ![Casino machines](DOCS/verification/visual-density-compositor/casino-fine-composite-1280x720.png) |

## Current state

The first-person city foundation, cyberpunk presentation, stationary Rin, visible terminal billboards, and data-driven linear dialogue are playable. The explicit Last Packet progression loop is the next major milestone.

Implemented:

- High-detail Fine 6×12 ASCII is the default world surface: 213×60 cells at 1280×720, with crisp glyph alignment and Standard-font UI.
- Street-level first-person exploration with camera-relative WASD movement.
- Independent arrow-key turning and vertical look.
- Semantic collision against buildings, park fixtures, water, doors, and entities.
- Four validated playable maps: the `city-alpha` streets, an enclosed `water-park`, a standalone `nature-reserve`, and the gold-walled neon `casino` interior with camera-facing slot-machine cabinets.
- Perspective ASCII raycasting with multilevel occlusion, optional depth-tested interior ceilings, distance fog, rain, street lamps, trees, park silhouettes, animated data facades, charcoal road surfaces, readable façade labels, localized light pools, and giant authored Vegas-style marquees.
- A restrained neon palette with validated contrast and reduced-flicker tuning.
- HUD, center-ray interaction prompts, terminal responses, and F3 diagnostics.
- Rin's depth-tested ASCII sprite, two-line introductory dialogue, and semantic blocking position near the city route.
- Physical in-world terminal billboards: compact cyan screens on metal stands that depth-test against walls while retaining semantic interaction/collision rules.
- Six visual-only cars loop authored city-road routes at a brisk but low-density pace; they are atmosphere only and never block, collide with, or react to the player.
- Sparse steam and rooftop exhaust, animated wall machines, positional vent/spark/horn events, and three scheduled pedestrians add deterministic street life; pedestrian sprites and blocking entities share one authoritative position.
- Validated, data-driven linear dialogue for Rin and terminals; E/Enter advances one line, while Escape closes safely without movement/look input leaking through.
- City/park music starts muted and can be enabled with `M`; smooth zone crossfades, independent gentle-rain ambience, semantic natural/pavement footsteps, positional city cues, lifecycle-safe playback, and synchronized HUD/keyboard controls remain available.
- Subtle movement-driven head bob that affects presentation only and can be disabled for reduced motion.
- Deterministic simulation, content validation, resize-safe rendering, and automated verification routes.

Coming next:

- Complete the remaining targeting/modal dialogue coverage.
- Add the explicit ordered relay state machine, signal guidance, and contextual repeat/early interaction text.
- Open the one-way archive seal after Relay 3, reveal the final transmission, and return to Rin for the ending.

![Visible Archive Node terminal](DOCS/readme/archive-terminal-1280x720.png)

![City-life pedestrian and steam](DOCS/readme/city-life-pedestrian-1280x720.png)

The canonical implementation roadmap is in [the development plan](DOCS/devplans/ascii-city-development-plan.md).

## Run the game

Requirements:

- The checked-in Gradle wrapper.
- Internet access on the first build so Gradle can resolve dependencies and provision the pinned Java 21 toolchain.

```bash
./gradlew gameRun
```

`gameRun` is the standalone game entry point. It launches Fine ASCII by default. The desktop window starts at and keeps a 1280×720 logical minimum and has no upper resize/maximize cap. Larger and ultrawide windows expand the first-person view with crisp whole-number glyph scaling instead of stretching the ASCII image.

Compare the available world-density profiles:

```bash
./gradlew desktop:run --args="--ascii-density=fine"
./gradlew desktop:run --args="--ascii-density=dense"
./gradlew desktop:run --args="--ascii-density=standard"
```

For all launch, capture, map, camera, asset-override, and verification commands, see [DOCS/run-cmd.md](DOCS/run-cmd.md).

## Controls

| Input | Action |
|---|---|
| `W` / `S` | Walk forward / backward |
| `A` / `D` | Strafe left / right |
| Left / Right arrows | Turn |
| Up / Down arrows | Look up / down |
| `E` or Enter | Interact / advance |
| Escape | Cancel / close |
| `F3` | Toggle diagnostics and the debug footprint |
| `Tab` while F3 is open | Load the next playable map |
| `M` or the music HUD button | Mute / unmute the soundtrack; rain and footsteps stay audible |

## Build and verify

Run the complete automated build:

```powershell
./gradlew clean check
./gradlew :core:validateContent
```

Useful desktop checks:

```powershell
# Ten-frame launch/render smoke check
./gradlew desktop:run --args=--smoke-test

# Movement, collision, lifecycle, and resize route
./gradlew desktop:run --args=--verify-walk

# HUD, targeting, modal input, and terminal route
./gradlew desktop:run --args=--verify-interaction

# Brief rain, neon, fog, pause, resize, and immutability route
./gradlew desktop:run --args=--verify-presentation
```

Diagnostic arguments can be combined with the smoke check:

- `--window-size=WIDTHxHEIGHT`
- `--ascii-density=fine|dense|standard`
- `--map=city-alpha|water-park|nature-reserve|casino`
- `--view-yaw=DEGREES`
- `--view-pitch=DEGREES`
- `--view-x=NUMBER` and `--view-z=NUMBER` together to start at a valid authored world position (useful for focused screenshots and renderer debugging)
- `--screenshot=ABSOLUTE_PATH`
- `--no-audio`
- `--no-head-bob`

Recorded test and runtime evidence lives in [verification notes](DOCS/verification-notes.md).

## Project layout

- `core/`: portable gameplay, simulation, content loading, rendering, input, and UI.
- `desktop/`: LWJGL3 launcher, desktop window rules, and desktop failure handling.
- `assets/data/`: validated palette, presentation, material, cell, and dialogue data.
- `assets/maps/`: fixed footprints, metadata, and objects for each playable map.
- `DOCS/`: format contracts, authoring guides, verification evidence, and development plans.

Start with the [map authoring guide](DOCS/map-authoring-guide.md), [map format](DOCS/map-format.md), or [rendering contract](DOCS/rendering-contract.md) when changing content or presentation.

## Developer orientation

- `GameScreen` owns the running LibGDX lifecycle and combines the simulation, first-person frame, HUD, and modal dialogue.
- `PlayerSimulation`, movement/look/collision systems, and `GameWorld` own deterministic gameplay state; renderers never define collision.
- `ContentLoader` validates palette, material, cell, sprite, dialogue, map, and object data before a world is constructed. Error messages identify the source file and JSON path.
- `WorldAsciiRenderer` owns the CPU first-person projection. `EntitySpriteRenderer` is reused by Rin and the visible terminal billboards, with per-character depth testing against the world frame.
- Static map content lives under `assets/`; keep rendering/material changes separate from semantic cells and entities.

The detailed project constraints and verification expectations are in [AGENTS.md](AGENTS.md). The roadmap is [the development plan](DOCS/devplans/ascii-city-development-plan.md).

## Technology

- Kotlin/JVM
- LibGDX
- LWJGL3 desktop backend
- Gradle Kotlin DSL
- CPU-rendered ASCII perspective geometry

The project intentionally uses a lightweight domain model rather than a general ECS, 3D model pipeline, shader stack, or external UI framework.
