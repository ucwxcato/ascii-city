# ASCII City Agent Guide

## Project intent

ASCII City is a desktop-first Kotlin/LibGDX cyberpunk exploration game. The player walks through **first-person ASCII environments**: it is not a top-down tile game. The current implementation uses a catalog of fixed, data-authored maps (`city-alpha`, `water-park`, `nature-reserve`, and `casino`) and renders them as perspective glyph geometry with fog, multilevel occlusion, animated data glyphs, and a restrained Brighter Berries neon palette.

Preserve these visual rules:

- WASD moves relative to the current view; arrow keys turn and look up/down without moving.
- Keep the street plane dark and neutral to create depth.
- Put colorful, stable neon accents on facades, windows, and landmarks; animate glyphs rather than flashing entire surfaces.
- Collision comes from semantic cells/entities, never from a rendered glyph or color.
- The desktop window defaults to and keeps a 1280x720 logical minimum and has no upper resize/maximize cap. Larger logical client areas expand the ASCII surface and select a whole-number glyph scale; they must not non-uniformly stretch glyphs or derive logical ray count from HDPI backing pixels.
- Fine 6x12 is the default world profile. Standard 10x18 and Dense 8x15 remain selectable fallback world profiles; HUD, dialogue, and errors stay on Standard. Fine may use emitter-only CPU phosphor copies plus the bounded cell-aware compositor to keep the default surface responsive.

## Layout

- `core/`: portable gameplay, content, rendering, input, and UI code.
- `desktop/`: LWJGL3 launcher, desktop window rules, and desktop-only failure handling.
- `assets/data/`: palette, materials, semantic cell definitions, and dialogue data.
- `assets/maps/`: fixed map footprints, map metadata, and map objects.
- `tools/unity-world-authoring/`: Unity Editor package for painting existing map footprints and placing 3D prefab assets; see its `README.md`.
- `tools/unity-authoring-project/`: Unity 6 host project that loads the local world-authoring package.
- `DOCS/map-authoring-guide.md`: practical map creation and editing workflow, including the current token legend.
- `DOCS/map-format.md`: authoritative map/content format rules.
- `DOCS/rendering-contract.md`: first-person visual and projection contract.
- `DOCS/verification-notes.md`: recorded runtime verification evidence.
- `DOCS/devplans/`: implementation plan and completed work tracking. `devplan-life-pass.md` owns optional ambient actors/traffic/wildlife; `devplan-city-visual-density.md` owns the committed contrast, road/sidewalk detail, façade/street-detail, localized-light, required cell-aware composite, and dense-ASCII roadmap.

## Important architecture boundaries

- `GameScreen` owns the running LibGDX screen lifecycle and presentation.
- `PlayerSimulation`, `MovementSystem`, `LookSystem`, and `CollisionSystem` own fixed-step player state. Keep simulation deterministic and independent of rendering frame rate.
- `WorldGrid`, `GameWorld`, and `WorldEntity` own gameplay geometry and semantic state.
- `ContentLoader` and the registries validate data before constructing the world. Reject invalid content with useful file-and-path diagnostics.
- `WorldAsciiRenderer`, `GridRaycaster`, `ProjectionMetrics`, and `MaterialSampler` own first-person ASCII projection and surfaces. `RainRenderer`, `TreeSilhouette`, `ParkFixtureSilhouette`, `ParkFireflyRenderer`, and `LocalizedLightRenderer` own their specialized visual passes. `LocalizedLightRenderer` may tint only final finite frame cells at the source depth or farther, from explicit resolved emissive glyphs; it owns no world state or collision. Do not replace this with a top-down renderer or a full ECS.
- `AsciiPresenter` turns `AsciiFrame` cells into LibGDX draw calls. Maintain glyph-cell alignment and resize-safe viewport behavior.
- `InputController` translates desktop input into `GameAction`/`GameCommand`; avoid reading keyboard state directly in gameplay systems.
- Unity is the target source of truth for world authoring: paint semantic terrain/building cells, sculpt terrain elevation, and place complete 3D prefab instances in the Unity scene. `Save map` exports the footprint to `<map-id>.txt` and elevation to `<map-id>.terrain.json`; `Export scene placements` exports placed models to `<map-id>.unity-assets.json`. These files are validated game inputs, not files users should need to hand-edit. The Kotlin runtime consumes the heightfield and turns exported model placements into ASCII sprites with authored collision. Unity mesh geometry remains editor-side and does not replace the first-person ASCII renderer.
- In Unity, each tree, bench, fountain, waterfall, lamp, or building prefab must appear as one named scene object that can be selected and moved as a whole. Keep reusable prefabs in `Assets/ASCII City/Placeable Assets` so the authoring shelf can list them; create missing built-in prefabs without overwriting user-edited ones. Reuse stable mesh and material assets by object type; scene sync must not delete a shared asset while other instances still reference it or generate materials per placement ID. Generated Unity mesh and material main object names must match their asset filenames.
- Imported model packs are kept under `tools/unity-authoring-project/Assets/Third Party/Imported Packs/`: Kenney City Kit Commercial 2.1, Kenney City Kit Industrial 2.0, Quaternius Modular SciFi MegaKit Standard, Quaternius Downtown City MegaKit Standard, and the 63-file nature FBX set from `FBX-20261011T180602Z-1-001.zip`. Preserve each included license file; the nature FBX archive supplied no license or source note. Wrapper prefabs for all 483 models are organized under `Assets/ASCII City/Placeable Assets/Imported Packs/<pack-name>/`, preserving source category subfolders where available. Each wrapper has one named root, a nested source-model prefab, and an `AsciiCityAssetPlacement` component so the authoring shelf can list and place it.

Use plain Kotlin classes and the current lightweight domain model. Do not introduce a general ECS, 3D model pipeline, shader stack, or external UI framework without an explicit request. The approved exception is the one depth-aware neon compositor required by `DOCS/devplans/devplan-city-visual-density.md`: it may use the existing presentation FBO path's low-resolution nearest-filtered ASCII color/depth/emissive inputs for bounded contact shade/local light/emissive halo, but it must not become a general effect chain or affect world/UI semantics.

## Content and rendering rules

- Keep `assets/data/palette.json`, `materials.json`, and `cells.json` in sync with their Kotlin validation contracts.
- Palette keys referenced by material data must be declared in `PaletteKeys` when they are required engine colors.
- `accentPaletteKeys` are cosmetic only. Keep ground materials free of accent colors unless a task explicitly changes the visual-depth rule.
- Road/sidewalk seams, lane marks, drains, patches, curbs, and slab joints are render-only neutral ground detail: they must preserve the dark road hierarchy, must not alter semantic walkability/collision, and may receive color only from a bounded visible local-light pass.
- Material animation must sample time/world position without mutating `WorldGrid`, collision, or authored content.
- Park grass (`p`) and paths (`=`) are walkable. Water (`~`), trees (`g`), benches (`b`), bins (`i`), fountains (`f`), and pavilion bays (`v`) are solid semantic cells even when their silhouettes contain visual openings.
- `TreeSilhouette` shapes trees; `ParkFixtureSilhouette` shapes park furniture, landmarks, and casino slot-machine billboards. Neither may define collision.
- `AsciiFrame` rows increase upward when presented by LibGDX. Downward screen motion, including falling rain, moves toward smaller row values.
- Preserve the coordinate contract: `x` is east/west, `z` is north/south, `y` is projected height, and yaw `0` faces north.
- Map rows are authored north-to-south; `MapLoader` converts them to world coordinates. Read `DOCS/map-format.md` before changing map parsing or data schemas.
- Keep the Unity exports in sync after authoring: `Save map` for painted cells/elevation and `Export scene placements` for placed models. The footprint supplies road, sidewalk, path, water, and building semantics; its Unity preview is an editable mesh, while the game still renders those cells through ASCII. Keep Unity placements in their validated sidecar instead of folding them into strict `.objects.json`.

## Build, test, and runtime checks

Use the checked-in Gradle wrapper. On Windows PowerShell:

```powershell
./gradlew clean check
./gradlew :core:validateContent
./gradlew gameRun
./gradlew desktop:run
./gradlew desktop:run --args=--smoke-test
./gradlew desktop:run --args="--verify-walk"
```

Useful rendering checks:

```powershell
./gradlew desktop:run --args="--smoke-test --window-size=1280x720 --view-yaw=0 --view-pitch=0"
./gradlew desktop:run --args="--smoke-test --window-size=1280x720 --view-yaw=90 --view-pitch=0"
./gradlew desktop:run --args="--smoke-test --window-size=3440x1440 --view-yaw=0 --view-pitch=0"
```

- Run at least focused tests for the changed system, then `clean check` for completed features.
- Content changes must pass `:core:validateContent`.
- Renderer/palette changes require a smoke-test screenshot at 1280x720 and snapshot-hash updates only after visually confirming the intended result. Responsive viewport changes additionally require a representative ultrawide capture and verification of the actual logical client area reported by the platform.
- If Windows locks a build artifact after launching the desktop app, run `./gradlew --stop` before retrying `clean check`.

## Change and Git discipline

- Inspect `git status` before editing and preserve unrelated user changes. In particular, do not stage, revert, or overwrite `.obsidian/workspace.json` unless the user explicitly asks.
- Use `apply_patch` for source, data, documentation, and configuration edits.
- Keep changes scoped; update docs and tests when altering public data formats, movement/collision rules, controls, or rendering behavior.
- Do not commit or push unless the user asks. When asked, stage explicit intended files rather than `git add .`.
- Before committing, run `git diff --check` and include a detailed commit message describing behavior and verification coverage.

## Current user-facing controls

- `W` / `S`: forward / back.
- `A` / `D`: strafe left / right.
- Arrow keys: yaw left/right and pitch up/down.
- `E` or Enter: interact (reserved/implemented according to current screen state).
- Escape: cancel.
- F3: toggle diagnostics and the debug footprint.
- `Tab` while F3 is open: load the next playable map.
- C while F3 is open: toggle the CRT world presentation effect.
- `M`: mute/unmute city and park music only; rain and footsteps remain audible, and the HUD music button performs the same command.

City and park music starts muted by default; rain, waterfall ambience, and footsteps retain their independent ownership.
