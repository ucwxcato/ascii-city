# Verification Notes

## Visual-density Phase 1 — 2026-08-26

- [x] Low/mid exterior façades now receive stable rib/belt structure, while garden/residential/tall families receive sparse window-grid divisions through material tags. These marks apply only within 22 world units; existing animated material glyphs occupy all non-structural cells and the distant fog density remains unchanged.
- [x] Roads and sidewalks gain neutral close-range structure without any palette or semantic change: roads use four-cell seam blocks inside 18 world units and sidewalks use two-cell slab joints inside 16. They preserve the existing animated glyph ramps between marks, stay dark, and never change walkability or collision.
- [x] Road asphalt now uses a neutral charcoal palette with a dedicated restrained background value rather than the generic near-black material background. The reviewed [Fine road capture](verification/road-asphalt-1280x720.png) shows a readable pavement plane and seams against sky black while preserving dark, accent-free depth. SHA-256: `9a47b5c4a4231f60fb14227d6a6d5ce1e4fd85389d03aa08bf8e5bed42f926fd`.
- [x] Focused sampler/presentation/world-renderer tests cover deterministic structural glyphs, road/sidewalk accent exclusion and detail falloff, living-data animation away from seams, facade fill depth ordering, fog density, and all affected city frame hashes. The seven city pose hashes were updated only after visual review.
- [x] Reviewed desktop smoke captures: [north 1280×720](verification/visual-density-phase1-north-1280x720.png), [cross-street 1280×720](verification/visual-density-phase1-east-1280x720.png), and [north ultrawide](verification/visual-density-phase1-north-3440x1440.png). The city retains dark street/sidewalk negative space while close façades gain clear structural scale; the ultrawide view preserves the same hierarchy at the expanded surface. SHA-256: `b0616212c562169276b615dec9eff549f9f492039721453540d163a96ae9ea99`, `3b6793131550ed6d139ad73807c3a38d39ddc698292fd8708f386a80a19d9e30`, and `83250798ccf281eba35a381524eaaa009e25ffc42ab6b465af94fead717620a3` respectively.

## Dense ASCII surface pilot — 2026-08-26

- [x] The selectable `--ascii-density=standard|dense|fine` world profile keeps HUD/dialogue/error UI on the readable 10×18 font. Fine 6×12 is now the default desktop world profile; Dense and Standard remain explicitly selectable fallbacks. Every bundled profile validates printable ASCII metrics, preserves nearest filtering and integer placement, and names a failing profile/asset in the controlled asset-failure path.
- [x] Focused profile tests prove Standard fixtures remain unchanged; Dense selects 160×48 at the 1280×720 desktop minimum and 120×36 at defensive 960×540; it ignores HDPI backing-pixel size, keeps whole-number scale, preserves world-space vertical framing and center-ray interaction targeting, and allocates no per-ray/per-glyph objects across a warmed 3,600-frame 160×48 renderer probe.
- [x] Reviewed paired desktop captures: [Standard 1280×720](verification/visual-density-phase2/city-standard-1280x720.png), [Dense 1280×720](verification/visual-density-phase2/city-dense-1280x720.png), and [Dense presentation route 1280×720](verification/visual-density-phase2/city-dense-presentation-route-1280x720.png). Dense makes close/middle façade structure finer while the 10×18 controls/HUD remain crisp; the retained CRT treatment stays neon without broadening the smaller glyphs into blur. SHA-256: `eaf93e5199954488377b41762aeaa1586b8b5cd6cf4b78f97a1ffe614d39cf4f`, `4657e0cea8048e5b497291cb5d1c8ef300051e2cc084e86615592a34588edfc9`, and `af1ae337a42186a7f32a6128d6442d25200127e931cbbe70d0de51d33d438150`.
- [x] A requested 3440×1440 dense smoke run first calculated the expected 215×48 @2× surface, then the actual Linux work area reported by the platform settled at 3440×1344 and selected 430×89 @1×. The retained [actual-work-area capture](verification/visual-density-phase2/city-dense-ultrawide-requested-3440x1440-actual-3440x1344.png) and runtime logs confirm logical client size—not backing pixels—owns the ray count. SHA-256: `95866f045e1b5a429584c5448587d150bb681acc3693139c9bcd518551e65574`.
- [ ] Fine is the approved default while **DENSE-04** continues to record its real-device widest-surface target-frame-time budget. The implemented cell-aware compositor accepts every selected world profile while HUD/dialogue stay outside the effect; broader local-emitter tint remains open.

## Cell-aware neon compositor and Fine profile — 2026-08-26

- [x] `AsciiFrame` now carries a presentation-only emissive strength beside glyph/color/background/depth. Depth-winning lamp bulbs/arms, façade signs and labels, park fireflies, and casino button/marquee/top-light parts opt in explicitly; nearer non-emissive geometry replaces the value through the same cell write and therefore blocks its haze. Collision, interaction, authored content, simulation time, and UI do not consume this channel.
- [x] `AsciiCompositeData` reuses cell-sized depth and emissive arrays. Finite depth is normalized to 48 world units with alpha as the explicit validity bit; emissive RGB comes from the resolved foreground and alpha from its authored strength. The corresponding nearest-filtered textures exactly match Standard, Dense, or Fine frame dimensions. Their Pixmaps use overwrite blending, fixing the initially observed stale-haze trail when the camera or an emitter moved.
- [x] The existing single CRT shader samples the immediate cell neighborhood only. It applies an 18% maximum contact darkening to the farther side of a finite-depth edge and a soft colored haze ending within 1.42 cell-center units; it does not blur the base ASCII image or affect HUD/dialogue. The Fine optimization keeps the emitter cell plus four cardinal samples, omitting diagonals at the edge of that radius, and limits legacy CPU phosphor copies to explicit emitters. Smoke diagnostics now report finite-depth and emissive cell counts.
- [x] Exterior façade background fill now falls away across 28 rather than 14 world units, preserving restrained mass through the middle distance without changing ground/interior fill, collision, fog color, maximum ray distance, or the far skyline density floor. The reviewed [Fine extended-façade capture](verification/visual-density-compositor/city-fine-extended-facades-1280x720.png) keeps distinct side faces, windows, street darkness, and porous far silhouettes. SHA-256: `c3d112425bcc5c8d86f132d2c6bab288c8edfdf7f8621ba1a8111caf56f25880`.
- [x] Reviewed captures: [Dense composite 1280×720](verification/visual-density-compositor/city-dense-composite-1280x720.png), [Fine composite 1280×720](verification/visual-density-compositor/city-fine-composite-1280x720.png), and [Fine moving-camera/F3 route](verification/visual-density-compositor/city-fine-moving-composite-1280x720.png). Fine raises the world surface to 213×60 (12,780 cells, 2.5× Standard) while the UI stays 10×18; moving-camera evidence shows compact lamps/sign haze without retained trails. SHA-256: `c3f96c3d8c18a8e980a9aa5032e26c57004f62356550e146a8de776041d135ca`, `d0be51426252f298afdca948d3d8662d5287985a67c8a43de57afa767aa5c260`, and `1c69409f0c6bd44da7ba835ad8011360f6bc3d7138016bf1f4c68daf6ff1be93`.
- [x] A requested 3440×1440 Fine smoke run first selected 286×60 @2×, then the actual 3440×1344 Linux work area selected 573×112 @1× and rendered 55,525 glyphs from 573 rays. The reviewed [actual-work-area capture](verification/visual-density-compositor/city-fine-composite-requested-3440x1440-actual-3440x1344.png) retains a dark neutral road and aligned UI. SHA-256: `c545b463fbadc4b3357f3ce3d9d68ed5fa80c3b25207eea77f5f095e17343620`.
- [x] The reviewed [Fine casino capture](verification/visual-density-compositor/casino-fine-composite-1280x720.png) resolves individual cabinet feet, frames, reels, buttons, marquees, and cyan caps across multiple depths; 384 explicit emissive cells remain attached to their depth-tested machine parts. SHA-256: `375330b731606c402782bd094c2c02d1696f29913628b6c2795fd56bf9f9f1fe`.
- [x] Fine is the no-flag desktop default; `--ascii-density=standard|dense|fine` remains available for comparison or fallback. The optimized no-flag Fine 1280×720 smoke capture retains crisp glyph fields, compact lamp/sign glow, and the Standard UI font: [default Fine capture](verification/visual-density-compositor/city-fine-default-optimized-1280x720.png), SHA-256 `e25c36b58b4bac612174d901f37e481c355e6a2e2c2e939c5c88300fdaf9669b`. The 540-frame no-audio/no-head-bob presentation route completed with the existing stable semantic hash across resize and map behavior. Fine’s widest-surface frame-time budget, compositor-side local-emitter tint (**COMP-02**), and final composite approval remain open.

## Visual-density Phase 3 — 2026-08-26

- [x] Existing `facadeSigns` and `facadeEntrances` were sufficient for the required validated façade fixture contract: they already enforce exterior host faces, footprint bounds, roof fit, non-overlap, palette keys, close/distant ASCII layouts, and non-collision semantics. No second visual-fixture schema was added.
- [x] `LocalizedLightRenderer` reads only the resolved presentation emissive channel after façade labels and fixture silhouettes. Sources need finite depth and at least `0.85` authored strength; their one/two-cell background tint is clipped, deterministic, and applied only to finite cells at the source depth or farther. Focused tests prove nearer geometry and sky stay untouched and a warmed 3,600-frame Fine update stays within the 4 KiB allocation threshold.
- [x] The existing archive vending prop and noodle-market prop now use compact readable labels, `ARCHIVE 24H` and `OPEN NOODLES`, which supply visible local sources without authoring new collision objects. Reviewed Fine captures: [archive façade](verification/visual-density-phase3-archive-1280x720.png) (`localLightWrites=40`) and [market façade](verification/visual-density-phase3-market-1280x720.png) (`localLightWrites=44`). Both retain a dark road and depth-separated colored facade pools. SHA-256: `26efe529b00a1c771a7eea2d67f046c11844cf8969a78cc6ba9fc014dbfcb37c` and `99234dc3066c49cccd279627e48fb7d0e11694e07fd3380d8ed9fde2e2e48022`.

## Derived park fireflies — 2026-08-25

- [x] `ParkFireflyRenderer` derives at most four swarms from authored lamps near grass or nature terrain. Each selected lamp yields exactly three deterministic orbiting/bobbing green points; waterfall-adjacent lamps are ranked first, while indoor water-park lamps lack the required habitat and never qualify. The effect adds no content schema, entity, collision, audio, interaction, or mutable world state.
- [x] Every point writes finite perpendicular depth through `AsciiFrame.writeIfNearer`, so nearer terrain, fixtures, walls, and actors occlude it. The pulse changes only the compact `o`/`*` glyph and its local background tint; it does not add a broad bloom pass or recolor the ground.
- [x] Focused `ParkFireflyRendererTest` and `WorldAsciiRendererTest` pass, including capped city/nature selection, city and reserve cascade inclusion, time determinism, animation, and full occlusion. The city renderer snapshots affected by the stronger firefly glyphs were updated only after visual review.
- [x] Reviewed 1280×720 desktop smoke captures: [city park](verification/fireflies-city-park-1280x720.png) (`fireflyWrites=9`), [city Cascade waterfall](verification/fireflies-city-cascade-1280x720.png) (`fireflyWrites=3`), and [nature reserve](verification/fireflies-nature-reserve-1280x720.png) (`fireflyWrites=3`). They retain discrete visible green points without a full-screen glow. SHA-256: `3f21d8eb01ee45c36ad81495e06dbf4ed7d46eb603b83d432a173f839890c1dc`, `09825adbad94cd3cd55ba05cbe2d6c4802a426071de1f762086f3c2affe323cf`, and `c4b0f562f2efa0d44b69af6d4f5c898b5fed26993792c58a8eb7574dc5b50629` respectively.

## Life-pass Phase 0 reconciliation — 2026-08-25

- [x] Refreshed presentation baseline evidence with audio and head bob disabled: a requested 960×540 launch first selected a 96×30 surface, then correctly enforced the desktop 1280×720 logical minimum and captured at 128×40; the direct 1280×720 run also captured at 128×40; and the 3440×1440 run settled at 344×74 after its initial whole-number-scaled surface. The retained [requested-960/minimum-enforced](verification/life-pass-phase0-requested-960x540-enforced-1280x720.png), [1280×720](verification/life-pass-phase0-baseline-1280x720.png), and [3440×1440](verification/life-pass-phase0-baseline-3440x1440.png) captures were visually reviewed. Their SHA-256 digests are `472a898184979174d5cc4e66c0f1b5c9408d7ad6e0b6332dc3a98e1f03c991e2`, `6376a1f163278894de12d5238308609cb7c0c093dd5711ca1318ecf54a9f8618`, and `fa2879cd657cdc5f790a0e6659935885d7c72624d586e925c1f6440d6caa8eb6` respectively.
- [x] The 540-frame no-audio/no-head-bob presentation route captured [its 1280×720 baseline frame](verification/life-pass-phase0-presentation-route-1280x720.png), toggled the default-off soundtrack on and back off, paused/resumed, resized through the minimum, 3440-wide, and reference work areas, and completed with stable semantic hash `b28ac29600d065f77e654e4c84a4e436ce26d2bd591edda35655d75a3429b808`. Its capture SHA-256 is `1e7fd09597f7417d92728380dcf20b9c6ccb6bb07a1b293ad0a34dffec89be01`.
- [x] `LPASS-02`/`LPASS-03` are reconciled against the current implementation: strict optional-object parsing and the shared `AsciiSpriteRegistry` → `GameWorld`/`CollisionSystem` → `EntitySpriteRenderer` actor path are documented in the life-pass plan and covered by current loader/actor/NPC tests.
- [ ] Canonical Last Packet verification is not claimed here. The canonical plan still leaves `TRAVEL-01`, `STORY-01` through `STORY-03`, `DOOR-01`, and `VERIFY-07` open, so no complete story-route checkpoints or final Phase 0 gate assertion exists yet.

## Close-façade contrast retune — 2026-08-25

- [x] Exterior façade backgrounds now gain a stronger but still partial fill inside fourteen world units. At point-blank range the front/side factors approach `0.40`/`0.27`; they remain below half of the base wall color and fall continuously to the original `0.12`/`0.08`, preserving ASCII foreground detail and face-orientation shading.
- [x] Ground materials, interior-wall special cases, glyph animation, palette accents, collision, and authored world data are unchanged. A focused sampler contract proves the near/middle/far ordering, prevents the fill from becoming either imperceptible or fully solid, and confirms road backgrounds remain distance-invariant.
- [x] The retained [1280×720 close-façade capture](verification/close-facade-contrast-1280x720.png) was visually reviewed before accepting all seven affected city snapshot hashes. Near building masses read continuously behind their glyph fields, the darker side face remains distinct, and the dark street plane, windows, signs, and steam keep their prior hierarchy.
- [x] Casino slot-machine billboards now apply the same partial background to nearby silhouette cells instead of inheriting the floor/wall behind them. The cabinet fill remains full through six world units, fades smoothly, and is completely absent by ten units so compact distant sprites do not merge into noisy blocks. Focused tests cover the close/middle/far ordering, sub-half-strength cabinet color, and transparent internal/between-machine cutouts. The retained [close](verification/casino-solid-machines-1280x720.png) and [far](verification/casino-machines-far-1280x720.png) 1280×720 captures confirm that nearby reel windows, controls, feet, and separate cabinet silhouettes remain readable while distant machines return to clean foreground-only glyphs.

## City life first pass — 2026-08-25

- [x] `city-alpha.objects.json` now validates five stateless steam/rooftop-exhaust emitters, three animated one-cell façade props, four deterministic positional micro-event emitters, and three blocking ambient pedestrians with short axis-aligned routes, endpoint dwell times, and staggered starts.
- [x] Ambient pedestrians reuse `GameWorld`, `SpatialEntityIndex`, `CollisionSystem`, and `EntitySpriteRenderer`. Focused fixed-step tests prove authoritative entity movement, endpoint scheduling, player wait/resume behavior, and render interpolation without collision/render disagreement.
- [x] Vent, spark, and horn triggers use reproducible stable-ID interval/pitch sampling, finite-depth visual glyphs, squared distance attenuation, and listener-relative stereo pan. Three mono 48 kHz cues were synthesized from noise/sine sources with no recorded samples; pause and music mute leave environmental ownership explicit.
- [x] City/park music now starts muted by default. A real 1280×720 audio-enabled smoke run loaded every loop and cue, presented `[M] MUSIC OFF`, retained rain/environmental channels, captured normally, and exited after 24 frames. `M` and the HUD button still restore the current soundtrack crossfade.
- [x] The retained [market façade/steam capture](readme/city-life-pass-1280x720.png) and [pedestrian/steam capture](readme/city-life-pedestrian-1280x720.png) were visually reviewed at 1280×720. After the initial wisps proved too subtle in play, nearby steam was retuned to use a persistent `[=#=]` grate, brighter `o`/`O`/`@` cores, and depth-limited side/crest detail. The west grate and matching hiss source moved from `(44.5, 24.5)` to `(45.5, 24.5)` so the courier no longer hides them. The street plane remains dark, wall animation stays local, and both pedestrians and plumes remain grounded, finite-depth, and occluded by nearer geometry.
- [x] The 540-frame `--verify-presentation --no-audio --no-head-bob` route moved through the city, paused/resumed, toggled the default-off music state on and back off, resized 1280×720 → 3440×1440 → 1280×720, and completed with normalized semantic hash `b28ac29600d065f77e654e4c84a4e436ce26d2bd591edda35655d75a3429b808`. Dynamic ambient-actor positions are normalized in this presentation-only immutability check and covered separately by fixed-step actor traces.
- [x] Four affected façade-view snapshot hashes were updated only after that visual review; unchanged south/west/down views retained their previous hashes. `./gradlew clean check` passes all 188 tests, strict content validation reports 25 semantic entities with three `ambient_actor` entries, and `git diff --check` is clean.

## Neon casino interior and map rotation — 2026-08-22

- [x] `casino` validates as a 40×32 enclosed interior with 962 mutually reachable walkable cells, 140 perimeter-wall cells, 156 solid two-unit slot-machine cells, 16 solid one-unit gaming-table cells, six light columns, six lamps, and one reachable jackpot-control terminal.
- [x] The casino uses semantic `u` carpet, `j` gold-aisle, `X` wall, `s` slot-machine, `k` gaming-table, and `o` light-column cells. Slot cells retain full-cell collision but render through the camera-facing fixture billboard pass as feet, cabinet, red button, animated reel window, `$` marquee, and cyan top light. Billboard artwork and animated glyph/color sampling do not mutate occupancy or world state.
- [x] `MapCatalog` now rotates `city-alpha -> water-park -> nature-reserve -> casino -> city-alpha`. The live F3 panel on `casino` reports `[TAB] LOAD city-alpha`, proving the existing debug map-swap path includes the new map and wraps correctly.
- [x] The Last Light Casino façade now has a data-authored, non-blocking exterior `door.last-light-casino` target with `destinationMapId: "casino"`. Invalid destinations fail content validation with the exact object JSON path. The 1280×720 `--verify-casino-entry` route acquired the door, pressed E, loaded `casino`, and captured its authored `(19.5, 2.5)` spawn.
- [x] `casino` now starts beside a visible full-width `door.casino-exit` billboard. Its 1.5-cell interaction radius spans the three-cell doorway and its authored `destinationSpawn` returns to `(26.5, 5.8)` at 180° outside Last Light. The 1280×720 `--verify-casino-return` route acquired the exit, pressed E, loaded City Alpha at that exact pose, and reacquired `[E] LAST LIGHT CASINO`.
- [x] Automated discovery coverage proves the spawn sees only guest Wi-Fi, the authored coverage overlap sees both signals, and the fixture edge sees only Bluetooth. The 1280×720 F3 capture visibly reports `NETWORKS LAST_LIGHT_GUEST 4/4` near spawn.
- [x] Focused catalog/material tests and `:cityworld-core:validateContent -PcontentMap=casino` pass. A dedicated `solid-interior-wall` material treatment gives only the casino shell a continuous gold background while preserving subtle animated neon glyph detail and darker side-face depth.
- [x] Focused slot silhouette and billboard composition tests prove the cabinet parts render instead of an opaque block. The 540-frame 1280×720 presentation route completed while walking, looking, toggling F3, pausing, muting, and resizing; the semantic map hash remained stable. Visual inspection confirmed filled warm-gold walls, purple carpet, gold paths, multicolor ceiling lights, and rows of individually readable `777`, `$`, and `BAR` machines.

## Phase 0 — Project baseline

> Status: Complete. Automated and real-window checks passed on 2026-08-18; the user confirmed the remaining hands-on window/lifecycle checks. The original 1280×720 upper-bound evidence below was superseded by the verified responsive `WINDOW-01` contract on 2026-08-19; the 960×540 minimum remains current.

### Pinned versions

| Component | Version |
|---|---:|
| Java toolchain | 21 |
| Gradle wrapper | 9.6.1 |
| Kotlin | 2.4.10 |
| LibGDX | 1.14.2 |
| JUnit Jupiter | 5.14.4 |

### Automated checks

- [x] `gradlew --version` resolves; the daemon criteria and compile/test toolchains require Temurin Java 21.
- [x] `gradlew help` resolves the build.
- [x] `gradlew projects compileKotlin` succeeds and lists `core` plus `desktop`.
- [x] `gradlew clean check` succeeds.
- [x] Headless LibGDX smoke test passes.
- [x] Desktop window-limit, launch-argument, and fatal-handler tests pass.

Observed evidence:

| Check | Result |
|---|---|
| Gradle | Wrapper 9.6.1; Java 21 daemon criteria in `gradle/gradle-daemon-jvm.properties` |
| Toolchains | Auto-provisioned Eclipse Temurin 21.0.11 plus developer-machine Temurin 25.0.3 detected |
| Clean build | `BUILD SUCCESSFUL`; `core:test` and `desktop:test` pass |
| Default desktop smoke | Started at 1280×720 logical pixels and exited after ten frames |
| Minimum request | 960×540 started at 960×540 |
| Below-minimum request | 800×450 was clamped to 960×540 |
| Original above-maximum request | 1600×900 was clamped to 1280×720 under the original contract; `WINDOW-01` later removed this upper cap |
| Lifecycle repetition | Ten consecutive real-window start/render/exit cycles passed |

### Original manual desktop checks

- [x] Launch at 1280×720 logical client size.
- [x] Resize to 960×540.
- [x] Attempt below-minimum and above-maximum sizes through the diagnostic launch argument.
- [x] Attempt maximize and confirm the original client area did not exceed 1280×720; this obsolete upper-bound behavior was replaced by `WINDOW-01`.
- [x] Alt-tab/focus repeatedly without a crash.
- [x] Close and relaunch ten times without duplicate or error-level lifecycle logs.
- [x] Trigger a disposable test startup failure and confirm one readable fatal report, then restore green state.

### Environment

- Date: 2026-08-18
- OS: Windows desktop
- Developer machine JVM initially observed: Temurin 25.0.3
- Project daemon and compilation target: provisioned Temurin Java 21 toolchain

## Phase 1 — Window and glyph surface

> Status: Complete. Automated checks, real-window rendering, both original target resolutions, and controlled critical-asset failures passed on 2026-08-18. The later `WINDOW-01` section extends this verified baseline to responsive large and ultrawide surfaces.

### Automated checks

- [x] All 22 tests pass with zero failures, errors, or skips.
- [x] Printable ASCII code points 32–126 exist in the bundled bitmap-font definition.
- [x] Every glyph uses a 10×18 region and a 10-pixel advance.
- [x] Font source, SHA-256, upstream metadata, and OFL license are bundled.
- [x] Palette parsing covers required, duplicate, malformed, invalid, missing, and fallback cases.
- [x] `AsciiFrame` covers corners, clear values, depth wins, invalid dimensions, and invalid coordinates.
- [x] Original viewport selection proves 96×30, 128×40, centered gutters, minimum clamping, and HDPI independence; `WINDOW-01` adds uncapped upper sizing and integer presentation scale.
- [x] The test card fills both supported surfaces deterministically.
- [x] Missing-font and corrupt-palette paths become controlled `AssetLoadException` failures.
- [x] `gradlew clean check` succeeds.

### Real-window evidence

| Capture | Observed result |
|---|---|
| [1280×720 test card](verification/milestone-1-1280x720.png) | Exactly 128×40 cells; aligned border, glyph ramp, palette bands, skyline, horizon, and converging road guides |
| [960×540 test card](verification/milestone-1-960x540.png) | Exactly 96×30 cells with the same stable composition and whole-cell alignment |
| [Missing font](verification/milestone-1-missing-font.png) | Readable blocking Scene2D error screen; no blank window or null crash |
| [Corrupt palette](verification/milestone-1-corrupt-palette.png) | Readable validation path naming the file, key, expected formats, and invalid value |

The runtime logged the selected ASCII surface for each successful launch, captured after four rendered frames, and exited cleanly after ten frames. Visual inspection confirmed nearest-filtered glyphs, stable 10×18 spacing, distinct semantic colors, centered error text, and correctly oriented screenshots.

## Phase 2 — World data and fixed city

> Status: Complete. Strict content loading, deterministic world construction, reachability, diagnostics, and the intentional failure/recovery path passed on 2026-08-18.

### Automated checks

- [x] All 37 tests pass with zero failures, errors, or skips.
- [x] `gradlew clean check` loads and validates the canonical content bundle without launching graphics.
- [x] Coordinate conversion covers negative flooring and north-row-first map orientation.
- [x] Registries reject duplicate IDs/tokens, invalid heights, missing fields, invalid animation parameters, and unknown palettes/materials.
- [x] Map parsing rejects empty, ragged, tabbed, unknown-token, and malformed content while treating LF and CRLF identically.
- [x] Spawn, object bounds, duplicate objects, interaction references, and interactable reachability are validated before a `GameWorld` escapes.
- [x] `WorldGrid` storage and `GameWorld` entity add/move/remove spatial-index invariants pass.
- [x] `gradlew check -PcontentMap=invalid-phase2` fails as intended with `maps/invalid-phase2.txt:1:1: unknown map token '?'`.
- [x] Restoring the default invocation returns `gradlew clean check` to green.

### Authored-world evidence

| Property | Observed result |
|---|---|
| Map identity | `city-alpha`, schema version 1 |
| Footprint | 96×96; 9,216 cells |
| Spawn | `(48.5, 4.5)`, yaw 0°; non-solid road cell |
| Reachability | 5,648 non-solid cells reachable from spawn |
| Cell distribution | alley 194; low 820; mid 899; neon 893; tall 914; park grass 226; paths 77; pond 16; trees 16; benches 4; bins 2; fountain 1; pavilion 3; plaza 75; road 3,060; sidewalk 2,016 |
| Height distribution | 0: 5,648; 1: 22; 2: 1; 4: 3; 6: 820; 8: 16; 12: 899; 16: 893; 22: 914 |
| Objects | one door, one NPC, one terminal, and fourteen authored lamps |
| Material coverage | all seventeen authored ground/facade materials used |
| Living-data channels | static, data stream, signal pulse, and restrained glitch flicker validate independently of collision |
| Generator repeatability | two runs produced SHA-256 `85BF5DA43A2BBF3D2E17159DC52866EDD5CBD7DAB42D24E5AD54C3DF4EDA4659` |

### Debug footprint

North is at the top; one character samples each 2×2-cell area. This is validation evidence, not the player-facing renderer.

```text
nnnnnn,...,lmmmm:mtttt,...,nnnll:lllmm,...,tt:tt
nnnnnn,...,lmmmm:mtttt,...,nnnll:lllmm,...,___tt
nnnnnn,...,lmmmm:mtttt,...,nnnll:lllmm,...,tt:tt
,,,,,,,...,,,,,,,,,,,,,...,,,,,,,,,,,,,...,,,,,,
................................................
tttttt,...,nllll:lmmmm,...,tttnn:nnnll,...,mm:mm
mmmmmm,...,tnnnn:nllll,...,mmmtt:tttnn,...,ll:ll
mm___m,...,tnnnn:nllll,...,mmmtt:tttnn,...,ll:ll
mmmmmm,...,tnnnn:nllll,...,mmmtt:tttnn,...,ll:ll
,,,,,,,...,,,,,,,,,,,,,...,,,,,,,,,,,,,...,,,,,,
................................................
llllll,...,mtttt:tnnnn,...,lllmm:mmmtt,...,nn:nn
nnnnnn,...,lmmmm:mtttt,...,nnnll:lllmm,...,tt:tt
nnnnnn,...,lmmmm:mtttt,...,nnn___lllmm,...,tt:tt
nnnnnn,...,lmmmm:mtttt,...,nnnll:lllmm,...,tt:tt
,,,,,,,...,,,,,,,,,,,,,...,,,,,,,,,,,,,...,,,,,,
................................................
tttttt,...,nllll:lmmmm,...,tttnn:nnnll,...,mm:mm
mmmmmm,...,tnnnn:nllll,...,mmmtt:tttnn,...,ll:ll
mmmmmm,...,tnn___nllll,...,nnnnt:tttnn,...,ll:ll
mmmmmm,...,tnnnn:nllll,...,mmmtt:tttnn,...,ll:ll
,,,,,,,...,,,,,,,,,,,,,...,,,,,,,,,,,,,...,,,,,,
................................................
llllll,...,mttttttnnnn,...,lllmmmmmmtt,...,nnnnn
```

## Phase 3 — First-person projection and raycasting

> Status: Complete. Projection math, bounded DDA, multi-height occlusion, ground casting, living-data materials, deterministic fixtures, allocation checks, and real-window captures passed on 2026-08-18.

### Automated checks

- [x] All 50 tests pass with zero failures, errors, or skips.
- [x] View-pose tests cover yaw wrap, ±55° pitch clamp, orthogonal forward/right vectors, and shortest-path interpolation across 0/2π.
- [x] Projection tests cover center/symmetric rays, physical glyph aspect, horizon motion, inverse-distance height, and 128×40 versus 96×30 parity.
- [x] DDA regression/property tests cover cardinal, diagonal, exact-corner, outside-map, maximum-step, finite/monotonic distance, unique cells, and 1,000 fixed random rays.
- [x] Multi-height fixtures prove a taller distant wall remains visible above a shorter near wall while a short distant wall remains occluded behind a taller near wall.
- [x] Identical world/pose/time produces identical glyph/color/depth SHA-256 fixtures across cardinal, diagonal, up, and down views.
- [x] Advancing render time changes living-data glyph/color output while the complete depth buffer and world-grid snapshot remain unchanged.
- [x] A warmed 3,600-frame allocation probe—equivalent to 60 seconds at 60 FPS—stays below the 4 KiB test threshold, proving no per-ray or per-glyph steady-state object creation.
- [x] `gradlew clean check --warning-mode all` succeeds with content validation and the full 50-test suite.

### Real-window evidence

The verification sweep captured all eight 45° yaw directions at level, +25°, and −25° pitch at 1280×720 (24 views), then repeated north-facing pitch checks at 960×540. Every launch completed ten frames and exited without an error-level log.

| Capture | Observed result |
|---|---|
| [North, level, 1280×720](verification/phase3-north-level-1280x720.png) | 128×40 street canyon with a stable horizon, converging data road, stepped facade depth, varied towers, and neon signal bands |
| [North, +25°, 1280×720](verification/phase3-north-up-1280x720.png) | Horizon moves down and upper facade/tower spans expand without changing position |
| [North, −25°, 1280×720](verification/phase3-north-down-1280x720.png) | Horizon moves up and ground-plane data flow occupies the view without numerical artifacts |
| [Southwest, level, 1280×720](verification/phase3-southwest-level-1280x720.png) | Rotation exposes a different neon facade and depth ordering rather than sliding a flat image |
| [North, level, 960×540](verification/phase3-north-level-960x540.png) | The same composition survives at the 96×30 minimum surface with undistorted FOV |
| [North, +25°, 960×540](verification/phase3-north-up-960x540.png) | Upper spans remain readable and whole-cell aligned at minimum size |
| [North, −25°, 960×540](verification/phase3-north-down-960x540.png) | Ground convergence and horizon behavior remain stable at minimum size |

At the reference north view, early-occlusion termination reduced the observed traversal from 8,500 candidate steps to 3,133 while preserving the frame image; a southwest view required 856 steps. The retained presentation intentionally reads as a city being decoded from moving data rather than static ASCII-painted walls.

## Phase 4 — Look, movement, and collision

> Status: Complete. Semantic input, fixed-step look/movement, semantic collision, sliding, dynamic blockers, lifecycle clearing, interpolation, and the real-window walk route passed on 2026-08-18.

### Automated checks

- [x] All 65 tests pass with zero failures, errors, or skips.
- [x] W/S move forward/back, A/D strafe, and arrows affect yaw/pitch only; E/Enter, Escape, and F3 remain edge-triggered commands.
- [x] Input release, clear, gameplay suppression, pause/focus reset, and command consumption prevent stuck or repeated actions.
- [x] Yaw wraps both directions, simultaneous look axes work, pitch clamps exactly at ±55°, aggregate/fixed look steps agree, and interpolation remains continuous at 0/2π.
- [x] Circle collision covers walls, corners, one-cell alleys, map bounds, and visually identical solid/non-solid materials using semantic `solid` only.
- [x] 0.10-cell substeps prevent a six-cell movement delta from tunneling through a one-cell wall; axis separation slides deterministically along it.
- [x] Closed doors and blocking NPC radii collide; non-blocking objects do not; ignored-self and post-index-move queries agree.
- [x] WASD is yaw-relative, diagonal input is normalized, combined move/look updates coherently, and repeated 120-tick traces produce identical poses.
- [x] 30 FPS and 60 FPS frame sequences produce equivalent authoritative poses; a ten-second frame spike is clamped to eight catch-up steps and reset without a burst.
- [x] The authored spawn is collision-free and the scripted 686-step city route enters the expanded park, contacts a semantic bin cell, returns within 0.01 cells of `(28.5,13.5)`, and restores yaw/pitch to zero.
- [x] Negative/zero blocking-object radii fail content validation with an exact JSON path.
- [x] `gradlew clean check` succeeds with content validation and the full 79-test suite.

### Real-window route

`desktop:run --args=--verify-walk` executed 750 rendered frames through the actual `GameScreen`:

1. Walked north from `(48.500,4.500)` to `(48.500,13.500)` using W.
2. Performed a lifecycle clear/resume while W was held without a jump or stuck key.
3. Strafed east with D until semantic collision stopped X at `52.750` against the facade.
4. Resized live from 128×40 to 96×30 while moving, then restored 128×40 on the return leg.
5. Used A and S to return exactly to `(48.500,4.500)`.
6. Held right+up arrows to yaw `100°` and clamp pitch at `55°`, then left+down to restore both axes to `0°` without translating.
7. Exited normally with no error-level lifecycle log or duplicate input processor.

The retained [walk verification capture](verification/phase4-walk-verification.png) shows the interpolated city view during the arrow-look segment after the movement/collision/resize route.

## Phase 5 — HUD and interaction targeting

> Status: Complete. Scene2D HUD/debug presentation, center-ray interaction selection, terminal response routing, modal-safe input, live resize, and the deterministic desktop interaction route passed on 2026-08-18.

### Automated checks

- [x] All 91 tests pass with zero failures, errors, or skips.
- [x] HUD formatting covers wrapped eight-way compass headings, stable locale-independent pose/debug text, absent objective/signal values, and a bounded north-up footprint with one player `@`.
- [x] Center-ray targeting covers centered, off-center, behind-player, pitch-away, inclusive 1.75-cell range, out-of-range, opaque-wall occlusion, and overlapping-depth selection.
- [x] Terminal, NPC, stale-target, and mismatched-interaction data return typed `ShowMessage`, `StartDialogue`, or safe `Error` outcomes without mutating future story state.
- [x] Modal input clears held movement, ignores F3/gameplay actions, accepts E/Enter and Escape as close commands, and returns to gameplay without a stored key press.
- [x] The scripted Phase 5 event sequence is deterministic and emits each resize, turn, open, close, and completion checkpoint once.
- [x] `gradlew :core:validateContent`, `gradlew clean check`, and the ordinary ten-frame `desktop:run --args=--smoke-test` finish successfully. The first clean attempt encountered the documented Windows JAR lock; `gradlew --stop` released it and the required clean rerun passed.

### Real-window interaction route

`desktop:run --args="--verify-interaction --screenshot=<absolute-output>"` executed 210 rendered frames through the real `GameScreen`:

1. Started on a validated open cell 1.30 cells south of `terminal.archive`; the centered terminal produced `[E] ARCHIVE NODE` and F3 exposed live frame/world data plus the north-up `@` footprint.
2. Opened the typed terminal response once with E; the modal cleared the target/prompt and suppressed movement/look input.
3. Resized live from 128×40 at 1280×720 to 96×30 at 960×540 while the modal remained open and readable.
4. Closed with Enter, re-acquired the target, turned 50° away until targeting cleared, then turned back to 0° and re-acquired it.
5. Reopened with E, closed with Escape, restored 1280×720, and completed with no error-level log, stuck input, or duplicate interaction.
6. Ran the ordinary non-verification smoke path afterward to prove normal launch/render/exit remained green.

The retained [Phase 5 interaction capture](verification/phase5-interaction-1280x720.png) shows the aligned controls, live F3 diagnostics/footprint, dark modal panel, and readable archive-terminal response over the first-person frame.

## Phase 6 — Cyberpunk presentation

> Status: Complete for required scope. Strict fog/contrast/reduced-flicker tuning, focused regression coverage, visual review, and the brief deterministic desktop presentation route passed on 2026-08-18. Required Cascade waterfall ambience plus optional low-volume music, licensed rain ambience, footsteps, and render-only head bob were added on 2026-08-19; an interaction cue remains pending.

### Tuning and automated checks

- [x] `data/presentation.json` strictly validates fog start/end/density, near-contrast range/floor, side-face shading, signal-pulse floor, and reduced-flicker rate/chance/brightness limits with exact file-and-JSON-path errors.
- [x] Fog starts at 10 cells, converges monotonically, and reaches the configured fog color and 0.38 glyph density at 48 cells.
- [x] Road and solid-facade samples within 8 cells retain at least 0.05 luminance contrast against their material backgrounds, including shaded side faces.
- [x] Glitch animation is capped at 6 Hz with sparse 1-in-17 glyph substitutions, 1-in-29 color flashes, and at most a 1.15 brightness multiplier.
- [x] Updated cardinal/diagonal/up/down renderer SHA-256 snapshots lock the intended presentation output after visual review.
- [x] The existing warmed 3,600-frame allocation probe remains below its 4 KiB steady-state threshold; rain remains derived from frame/time with no growing particle collection.
- [x] `gradlew :core:validateContent` succeeds and explicitly includes `data/presentation.json`.
- [x] `gradlew clean check` succeeds with 96 tests, zero failures, errors, or skips.

### Brief real-window presentation route

`desktop:run --args="--verify-presentation --screenshot=<absolute-output>"` executed 540 rendered frames through the real `GameScreen`:

1. Enabled F3 and continuously alternated walk/strafe with yaw/pitch input while rain, living-data facades, neon signals, fog, street lamps, park silhouettes, and the HUD rendered together.
2. Exercised pause/resume at fixed step 120 without stuck input or a simulation burst.
3. Resized live from 128×40 at 1280×720 to 96×30 at 960×540 at step 240, then restored 128×40 at step 360.
4. Recomputed a SHA-256 digest across the map identity, every semantic grid cell, and every static entity at step 480; it exactly matched the startup digest `8dc444126da92aeb47856dd8b6c664cf1801631b009943c9d595bc248c10458e`.
5. Reported heap use after explicit collection at both bounds (`4,409,072` to `7,260,648` bytes; `+2,851,576` bytes during startup/warmup) and exited normally. The long-run allocation guard remains the renderer's warmed 3,600-frame probe rather than this startup-inclusive smoke measurement.

The retained [fog-tuning capture](verification/phase6-fog-1280x720.png) confirms readable neutral near geometry and a smoothly fading skyline. The retained [presentation-route capture](verification/phase6-presentation-smoke-1280x720.png) shows the live F3/HUD overlay, rain, stable neon accents, park paths/fixtures, lamps, and depth-separated facades at 1280×720.

### Ambience refinement — 2026-08-19

- [x] Two owner-supplied ACE-Step 1.5 instrumental masters are retained under `assets/audio/source/`; normalized stereo 48 kHz Vorbis derivatives use four-second tail-to-intro crossfades for runtime looping.
- [x] Pixabay `Gentle Rain 07` by DRAGON-STUDIO is retained with source URL, Pixabay Content License links, hashes, and modification details; its stereo 48 kHz Vorbis derivative uses a four-second tail-to-intro transition for an 86.024-second runtime loop.
- [x] Pixabay `Waterfall In The Mountains_Nature Sound` by NickyPe is retained with source URL, Pixabay Content License links, hashes, and modification details; its stereo 48 kHz Vorbis derivative uses a two-second tail-to-intro transition for a seamless 23.584-second runtime loop.
- [x] Pixabay `Footsteps in thin snow` and `Concrete Footsteps` masters are retained with source/license links and hashes; four quiet 0.38-second mono Vorbis derivatives provide alternating natural and pavement steps without overlapping the full source recordings.
- [x] `AudioService` starts city, park, rain, and waterfall loops once; city/park music crossfades from semantic park tags at a restrained `0.32` level while rain remains independent at a softened `0.30`. The waterfall layer peaks at `0.50`, follows a smoothed proximity field derived from semantic waterfall cells, is silent at the canonical spawn, and remains independent of music mute. Distance-based footsteps use natural cues on semantic `park`/`grass`/`path` ground and pavement cues elsewhere, and all channels share pause/resume/disposal while the music control affects only soundtracks.
- [x] `M` and the clickable `[M] MUSIC ON/OFF` HUD button share one soundtrack-mute state and remain usable while a modal suppresses gameplay input; rain and footsteps continue while music is off.
- [x] Focused audio/input/HUD/script tests cover crossfade levels, independent rain level, single-track fallback, music-mute restoration with live rain/SFX, effects-channel behavior, distance-driven footstep alternation, semantic natural/pavement selection, surface transitions, silent wall pressure, invalid delta rejection, lifecycle idempotence, modal input, and deterministic verification events.
- [x] `desktop:run --args=--verify-presentation` loaded the city, park, rain, and four footstep assets, paused/resumed all audio, turned music off at step 180 and back on at step 210 without muting rain/SFX, resized through both window limits, preserved semantic hash `8dc444126da92aeb47856dd8b6c664cf1801631b009943c9d595bc248c10458e`, and exited normally after 540 frames.
- [x] `desktop:run --args=--verify-walk` loaded all four natural/pavement footstep derivatives, exercised distance-based park-path walking plus lifecycle/collision/resize behavior, and exited normally after 750 frames; `clean check` and `:core:validateContent` passed afterward.
- [x] `desktop:run --args="--smoke-test --no-audio --no-head-bob"` completed ten frames with the no-op audio backend and reduced-motion presentation, and `clean check` passed afterward.

The retained [audio HUD capture](verification/phase6-audio-hud-1280x720.png) confirms the bottom-right control's alignment at 1280×720; its current label is `[M] MUSIC ON/OFF` to match soundtrack-only behavior. PRESENT-01 tests bound head bob to ±0.015 world units and prove it changes only render eye height; `--no-head-bob` disables it for reduced motion. AUDIO-02 remains open only for an interaction one-shot and its once-per-action emission rule.

### Contrast-separation refinement — 2026-08-19

The neutral palette no longer aliases wall fronts with fog or wall sides with sidewalks. Side-face brightness is 0.66, and deterministic glyph thinning now falls from full density at the 10-cell fog start to 0.38 at the 48-cell fog end without changing depth, collision, or authored world state. Focused presentation/content/renderer tests, `:core:validateContent`, and `clean check` passed. Visual inspection of the retained [1280×720 contrast-separation capture](verification/contrast-separation-1280x720.png) confirmed clearer face orientation and a less uniformly dense mid/far skyline while preserving silhouettes, neon landmarks, and the dark ground plane.

### Vegas façade marquees — 2026-08-19

- [x] `city-alpha.objects.json` authors `LAST LIGHT CASINO`, `SIGNAL LOST`, and `LUCKY//7 SYNTHETICS` on three distinct exterior building faces without adding entities or collision.
- [x] Strict loading validates stable IDs, cardinal faces, opaque building coverage, open exterior neighbors, roofline fit, non-overlap, rectangular printable close/distant glyph layouts, palette references, and a maximum 4 Hz bulb rate with exact JSON paths.
- [x] The wall sampler reverses north/west texture orientation for readable outward-facing text, uses a simpler distance layout after 14 cells, animates only authored period bulbs, applies existing side shading/fog, and writes at the host wall depth.
- [x] Focused content, palette, sampler, snapshot, determinism, and warmed allocation tests pass; `:core:validateContent` and `clean check` pass with the canonical map.
- [x] A 1280×720 desktop smoke view at yaw 180° and pitch 10° rendered the close marquee with no error logs; additional oblique views confirmed partial building occlusion and distance LOD behavior.

The retained [façade-sign capture](verification/facade-signs-1280x720.png) confirms the approved giant warm marquee treatment against the existing cyan/magenta building accents. The signs remain flush with their buildings and visually distinct from Rin's camera-facing entity-sprite art.

### Required Last Packet nature spur — 2026-08-19

- [x] The west side of the spawn park now continues through a two-cell trailhead crossing into a required nature district with 63 trail cells, 57 pines, 5 tents, 4 footbridge cells, 23 river/pool cells inside the new district, a 2-cell animated waterfall, and its stone cliff backing.
- [x] `terminal.relay-01` is authored as `CASCADE RELAY` on the waterfall overlook at cell `(2,25)`, validates as a terminal, and belongs to the same 5,718-cell reachable component as the canonical spawn. Three authored nature lamps mark the trailhead, campsite, and cascade approach.
- [x] Trail/bridge/grass cells remain walkable and select natural footsteps/park ambience through semantic tags. River, pine, tent, waterfall, and rock cells remain solid independently of their shaped glyph openings.
- [x] Focused tests prove the conical pine crown/trunk, triangular tent canvas/open entrance, waterfall flow/rock/spray regions, exact landmark cells, required token use, relay identity, and relay reachability. The complete 125-test core suite, `:core:validateContent`, and `clean check` pass.
- [x] Visual review at 1280×720 approved the park-to-nature transition, forest/trail depth, warm campsite canvas, and cyan waterfall flow against neutral stone. The canonical spawn metadata was restored before final validation; the close views were capture-only verification viewpoints.
- [x] The displaced `LUCKY//7 SYNTHETICS` marquee was re-anchored to a valid east-facing exterior wall and continues to pass façade placement validation.
- [x] The Cascade waterfall now owns a licensed looped environmental layer. Two semantic waterfall cells form its allocation-free distance field: full level within 2.5 cells, smooth fade through the reserve, and silence at 14 cells and beyond. Focused tests cover source derivation, midpoint attenuation, city-spawn silence, music-mute independence, no-restart mixing, pause/resume, disposal, invalid levels, and empty-source safety.

The retained [park trailhead capture](verification/nature-spur-1280x720.png) shows the connected city/park transition and pine silhouettes from the canonical spawn. The retained [cascade capture](verification/nature-cascade-1280x720.png) shows the animated twin-width waterfall feeding the river between stone shoulders, while the [campsite capture](verification/nature-camp-1280x720.png) confirms warm triangular tents among the trail, pines, and lamps.

## Phase 7 — Stationary Rin definition (`NPC-01`)

> Status: Complete for authored NPC metadata, reference validation, semantic collision, and isolated sprite projection on 2026-08-19. At this packet's completion, `NPC-02` still owned the live GameScreen composition evidence recorded in the following section.

- [x] `city-alpha.objects.json` authors `npc.rin` at `(45.5,16.5)` on a non-solid starting-route sidewalk cell with explicit blocking, radius `0.24`, `sprite.rin`, west-facing `270°` metadata, display name `Rin`, and `dialogue.rin.intro`.
- [x] `Npc` carries immutable stable entity/sprite/dialogue identity plus facing/display metadata while the paired `WorldEntity` remains authoritative for position and collision.
- [x] Strict loading rejects missing/unknown sprite references, missing blocking metadata, facing outside `[0,360)`, blank display names, missing dialogue references, and dialogue-speaker/display-name disagreement with exact file and JSON paths.
- [x] The focused integration fixture resolves one typed NPC to one world entity, proves Rin blocks the player-radius collision query, and produces nonzero output when her resolved sprite and exact authored position are passed through `EntitySpriteRenderer` from the canonical spawn.
- [x] All 129 core tests pass with zero failures, errors, or skips. `:core:validateContent` reports one NPC among 21 entities and preserves 5,718 reachable cells; `clean check` passes.
- [x] The NPC-01 `desktop:run --args="--smoke-test --no-audio --no-head-bob"` loaded the updated content at 1280×720, rendered ten frames, and exited normally. That packet deliberately deferred any player-visible claim to the NPC-02 evidence below.

## Phase 7 — Live stationary Rin composition (`NPC-02`)

> Status: Complete. Stable-order live composition, authoritative entity refresh, depth occlusion, culling/resize safety, bounded allocation, and visual verification passed on 2026-08-19. Dialogue presentation remains owned by `DIALOGUE-01`.

- [x] `NpcSystem` sorts definitions once by stable entity ID, resolves their paired `GameWorld` entities, and sends the current authoritative positions plus validated sprite IDs through `EntitySpriteRenderer` without owning collision or duplicating mutable position state.
- [x] `GameWorld` increments an internal entity revision on add/move/remove. The NPC layer refreshes cached immutable entity records only after that revision changes, so moved entities render at their new authoritative position, removed/mistyped entities fail loudly, and unchanged frames allocate no per-NPC lookup objects.
- [x] `WorldAsciiRenderer` composes NPC sprites after ground, walls, and façade signs, before depth-tested lamps and final sky-only rain. `WorldRenderStats.entitySpriteWrites` exposes the pass without changing the logical grid or semantic world.
- [x] Focused tests cover stable ID order, nearer-wall occlusion, collision persistence while culled, authoritative move refresh, removal failure, nonzero full-renderer composition, 128×40/96×30 resizing, and an accelerated 18,000-frame trace representing five minutes of approach, turn-away culling, and reapproach. Both NPC-only and full-renderer warmed allocation probes remain at or below 4 KiB.
- [x] All 135 core tests pass with zero failures, errors, or skips. `:core:validateContent` preserves 21 entities and 5,718 reachable cells; `clean check` passes. Updated east, diagonal-up, and close-approach hashes lock the intended live sprite output.
- [x] A 1280×720 spawn-distance smoke view at yaw `80°` writes 10 Rin cells and exits after ten frames. A closer verification view writes 42 cells, turns Rin outside the FOV, renders at 96×30, turns back, restores 128×40, and exits after 210 frames without losing entity/collision state or logging an error.

The retained [spawn-distance Rin capture](verification/phase7-rin-1280x720.png) confirms she is discoverable from the park approach without dominating the skyline. The retained [close Rin capture](verification/phase7-rin-close-1280x720.png) confirms readable cyan hood/magenta coat/neutral body colors, transparent sprite cells, grounded feet, and correct depth alongside lamps, trees, façades, and the dark street plane.

## Playable map rotation and themed maps - 2026-08-19

- [x] The original three-map `MapCatalog` rotation and F3/Tab behavior passed here; the current four-map rotation including `casino` is verified in the newer casino section above.
- [x] Each swap creates fresh map-bound simulation, collision, interaction, rendering, NPC, waterfall-field, and ambience state at the destination spawn. The old screen is disposed after replacement, and F3 remains open for repeated testing.
- [x] `water-park` validates as a 40x32 enclosed venue with 482 solid pond cells, 124 walkable bridge cells, 236 park-path cells, four fountains, pavilion bays, benches, bins, lamps, and a reachable flow-control terminal.
- [x] `nature-reserve` validates as a 40x32 standalone nature map with 61 pines, 216 trail cells, 96 pond cells, 32 bridge cells, eight waterfall cells, five tents, a reachable reserve relay, and Rin on the central trail.
- [x] Focused catalog/input/HUD tests, independent validation of both themed maps, `clean check`, and the ten-frame 1280x720 no-audio/no-head-bob smoke test pass.
- [x] A live desktop input pass opened F3 and cycled through water park, nature reserve, and back to the city. Each destination rendered a 128x40 frame and logged its expected map ID without content or rendering errors.

## Whole-map interior ceiling and liminal waterpark (`INTERIOR-01`) — 2026-08-20

> Status: Complete. Optional ceiling metadata, inverse plane casting, shared depth composition, enclosed waterpark content, deterministic tile grids, direct-map smoke tooling, and 1280×720 visual review pass.

- [x] Fixed-map metadata optionally accepts a `ceiling` object with a finite height at least 0.5 units above spawn eye height and an existing material ID. Missing/unknown fields, insufficient clearance, and unknown material references fail with exact file-and-JSON-path diagnostics.
- [x] `ProjectionMetrics.ceilingDepthAtRow` mirrors ground-plane math above the pitch-adjusted horizon. `WorldAsciiRenderer` bounds projected ceiling samples to map X/Z and writes their perpendicular depth before walls, sprites, lamps, and sky-only rain; it never mutates `WorldGrid` or collision.
- [x] Outdoor maps omit the ceiling and retain their approved city frame hashes. The indoor fixture proves every top-row character is finite-depth, no rain color overwrites the roof, and the approved 128×40 waterpark frame hash is `a5993bbb504879f02f963583a2f1496da955d35aa1ee31047e2e15970ba8136a`.
- [x] `water-park` v2 validates as a 40×32 single-level interior with a seven-unit ceiling, 140 solid seven-unit white-tile perimeter cells, 642 unchanged reachable cells, 482 solid water cells, 124 walkable bridge cells, and five unchanged authored entities.
- [x] World-space wall and ceiling grout grids use pale off-white fields, darker orientation-aware backgrounds, and sparse sickly fluorescent accents. Ground materials remain untouched and dark; the ceiling supplies no collision, partial roof mask, upper floor, slope, or vertical physics.
- [x] The direct `--map=water-park` smoke route opened a 1280×720 logical client, selected a 128×40 surface, rendered 128 rays with 2,297 ceiling writes, captured the frame, and exited normally after ten frames with audio disabled.
- [x] Focused metadata, catalog, projection, material, and renderer tests pass. Final `clean check` passes all 148 core and 7 desktop tests, and a separate `:core:validateContent -PcontentMap=water-park` confirms the v2 bundle and reachable component.

The retained [liminal waterpark capture](verification/water-park-interior-1280x720.png) confirms a converging pale tiled ceiling, repetitive white-tile wall fields, dark pool/floor bands, fixture occlusion, and no visible outdoor sky or rain. The deliberately repetitive, washed-out shell establishes the requested liminal horror-space treatment without replacing the first-person ASCII renderer or semantic collision model.

## Responsive large and ultrawide window (`WINDOW-01`)

> Status: Complete. Unbounded upper resizing/maximize, responsive whole-number glyph scaling, Hor+ projection, scaled HUD/dialogue, live resize safety, reference-image stability, and ultrawide visual verification passed on 2026-08-19. Exclusive/borderless fullscreen remains intentionally unexposed.

- [x] LWJGL3 keeps the 1280×720 default and 960×540 native minimum but uses GLFW's `-1` upper size limits. Launch arguments accept sizes above the former cap, and native window management may maximize to the available work area.
- [x] `AsciiViewportCalculator` uses logical client pixels only. It keeps at least 96×30 cells, chooses a uniform whole-number glyph scale near the 128×40 reference density, tolerates up to 5% ordinary desktop work-area loss at scale thresholds, and centers incomplete-cell gutters. Drawable HDPI backing pixels never alter columns, rows, or scale.
- [x] Calculator fixtures prove 960×540 → 96×30 @1×, 1280×720 → 128×40 @1×, 1600×900 → 160×50 @1×, 2560×1080 → 256×60 @1×, 3440×1440 → 172×40 @2×, the observed 3440×1415 work area → 172×39 @2× with a five-pixel vertical gutter, and 3840×2160 → 128×40 @3×. Non-divisible scaled sizes and the minimum-surface guard also pass.
- [x] `AsciiPresenter` uniformly translates/scales the entire cell surface with nearest filtering; it never stretches X and Y independently. HUD labels/buttons, the reticle, debug panel, and the modal message panel share the selected integer scale and scaled content anchors.
- [x] `ProjectionMetrics` derives effective horizontal FOV from the 75° 16:9 reference while preserving vertical framing. Ultrawide tests prove wider side rays with unchanged vertical FOV/focal-row ratio; extreme aspect ratios clamp safely to 30°–120°. F3 shows the effective value and glyph scale.
- [x] Existing 960×540/1280×720 renderer snapshots remain unchanged. The final 1280×720 reference smoke selected 128×40 @1×, rendered 128 rays, and exited normally.
- [x] The aspect-sensitive standalone Rin fixtures intentionally changed under Hor+ projection. Visual preview confirms readable near/far scaling, clean edge clipping, unchanged full-wall rejection, and correct per-character partial occlusion; the approved near/far/edge/partial hashes are `dbb82650974ddc86e0c8b153a4bc237fff0686d26427553a6a1edadc72a8b912`, `5bc71e663083e13db1d5d67e86ef89deb16533143d76293367436df9a4a6c67e`, `27cc6de2898f9dd038a254cc8b9ef09bc20cf4b964ea2b337a5fe5a38be8c47d`, and `58e2b74c7888316ff6d196d435af6dd8c00ae22e8664d378ca2e343b1737a51d`; the fully occluded hash remains unchanged.
- [x] A requested 3440×1440 window produced the platform's 3440×1415 logical work area, selected 172×39 @2×, rendered 172 rays, and exited normally. Visual inspection confirms crisp doubled glyphs, preserved vertical composition, additional city on both sides, centered perspective, and correctly anchored HUD controls.
- [x] The interaction route opened a modal at 3440×1415, then resized it live through 960×540 → 3440×1415 → 1280×720 while modal input remained captured. Closing restored the same terminal target; turn-away/reacquire and reopen/escape completed without an error. The ultrawide modal capture shows `GRID 172x39 @2x`, effective `FOV 93.2`, readable debug/HUD text, and a centered scaled panel.
- [x] The 540-frame presentation route resized live from 128×40 @1× to 96×30 @1×, 172×39 @2×, and back to 128×40 @1× while moving/looking. It completed with semantic world hash `95b07e9985309efb2c145cd56e3b40ca8e152dc4c7dea5f099ad73fe87ef0c5f` unchanged and no error logs.
- [x] Final `clean check` passes 142 core tests and 7 desktop tests with zero failures, errors, or skips. `:core:validateContent` passes with 21 entities and 5,718 reachable cells; `git diff --check` is clean.

Retained visual evidence:

| Capture | Observed result |
|---|---|
| [1280×720 responsive reference](verification/ultrawide-reference-1280x720.png) | Original 128×40 composition at 1× remains aligned and undistorted |
| [3440-wide ultrawide world](verification/ultrawide-3440x1440.png) | Actual 3440×1415 client renders 172×39 at 2×, fills the work area, and reveals more horizontal city geometry |
| [3440-wide ultrawide modal and F3](verification/ultrawide-dialogue-3440x1440.png) | Scaled modal, debug panel, controls, reticle-centered composition, and effective 93.2° horizontal FOV remain readable |

## Data-driven linear dialogue (`DIALOGUE-01`) — 2026-08-20

> Status: Complete. Rin and terminal dialogue data, shared modal line presentation, input capture, advance/cancel behavior, live resize safety, and automated/runtime verification pass.

- [x] `DialogueOverlay` replaces the temporary message placeholder with one Scene2D panel that shows the validated speaker, one line at a time, line progress, and context-sensitive advance/close guidance. It preserves the existing whole-number glyph scaling and centered resize behavior.
- [x] Rin and every authored terminal resolve stable `interactionId` values through `dialogue.json`. Terminal loading now rejects unknown dialogue IDs, blank/missing display text, and speaker/display disagreement with exact object JSON paths.
- [x] E/Enter advances exactly one line and closes after the final line; Escape closes immediately. Modal activation and release both clear pending/held gameplay input, so movement and look commands cannot leak into or out of dialogue.
- [x] Focused dialogue-session, interaction-system, content-loader, and verification-script tests pass. `:core:validateContent`, `clean check`, and `git diff --check` pass.
- [x] The scripted `desktop:run --args="--verify-interaction"` route opened the archive terminal's two-line authored dialogue, held the same modal line through 960×540, an actual 3440×1415 ultrawide client at 2× glyph scale, and 1280×720, advanced once, closed on the final Enter, reacquired the target after turning away/back, reopened, and closed with Escape. The player position remained `(51.500,19.200)` throughout and the run exited normally after 210 frames without errors.

## Visible terminal billboards — 2026-08-20

- [x] Every semantic terminal now projects the validated compact `sprite.terminal`: a cyan screen in a neutral metal screen-and-stand frame. The visual pass reads only terminal positions from `GameWorld`; it does not change collision, targeting range, dialogue IDs, or authored map geometry.
- [x] `TerminalBillboardRenderer` writes through `EntitySpriteRenderer` after opaque world geometry and before lamps/rain, so walls can occlude the visible terminal just as they occlude NPC sprites. Its revision cache refreshes only when the entity world changes.
- [x] Focused sprite/billboard composition tests and `:core:validateContent` pass. A 1280×720 desktop interaction run at `(51.500,19.200)` facing the archive node wrote 914 terminal glyph cells at depth `1.3` and exited without errors; visual inspection approved the compact billboard and centered interaction prompt.
## Unity-authored terrain and placed assets (2026-10-11)

> Status: The Unity scene export path, validated Kotlin sidecars, elevated terrain projection and movement, ASCII model sprites, and blocking placement collision are connected.

- [x] Unity scene rows are the painter's edit source once a scene exists. `Save map` exports the semantic `.txt` footprint and south-to-north corner-height `.terrain.json`; `Export scene placements` writes version 1 `.unity-assets.json` with per-prefab ASCII sprite profiles and placed transforms.
- [x] Kotlin validates both optional sidecars. Terrain heights drive ground projection and actor/wall anchors, player eye height follows terrain, and collision follows walkable slopes while rejecting rises over 0.35 units per movement substep.
- [x] Unity prefab placements render through the first-person ASCII sprite path and use their authored blocking flag and scaled collision radius. Unity mesh geometry stays inside the authoring project.
- [x] The city map still has 3,050 road cells. The Unity terrain mesh's road material submesh contains 18,300 indices (3,050 quads), and a 1280x720 runtime capture shows the road layer: [Unity authoring road runtime](verification/unity-world-road-runtime-1280x720.png).
- [x] The authoring package compiles in Unity 6 batch mode. Final `clean check` passes 216 core/desktop tests with zero failures, errors, or skips; content validation and `git diff --check` pass.
