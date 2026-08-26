# ASCII City Visual Density — Development Plan

> **Status:** Committed visual-enrichment plan, authored 2026-08-25. Existing close-façade contrast, slot-machine distance fade, steam/ambient actors, and park/nature fireflies are its verified baseline; no item below is complete unless it has its stated implementation and verification evidence. The cell-aware composite lighting packet is required work in this plan, not an optional experiment.
>
> **Purpose:** Make the first-person city read as a layered, inhabited cyberpunk place: strong architectural masses at close range, recognizable depth landmarks in the middle distance, sparse bright activity against dark streets, and eventually a denser ASCII surface that makes the world feel more granular without sacrificing legibility or performance.
>
> **Authority:** This committed visual-enrichment plan complements the canonical [Last Packet plan](ascii-city-development-plan.md) and the [Life Pass](devplan-life-pass.md). It does not change story progression, controls, map format rules, or the first-person rendering contract until a checked implementation packet explicitly updates those contracts. It never bypasses the canonical story gate, but once this plan is undertaken its cell-aware composite lighting packet is mandatory.
>
> **Reference boundary:** [A Walkable ASCII Cyberpunk City in One HTML File](https://www.youtube.com/watch?v=3YtygAx_C6A) is used only as a visual-behavior reference: dense text geometry, large value masses, local emissive points, street clutter, and restrained movement. Do not copy its code, frames, layouts, assets, typography, palette, or city content.

## 0. Intended result

The player can turn into a street and immediately parse a dark foreground silhouette, a few chunky near façade planes, layered signs/fixtures/windows, a readable road and sidewalk corridor, and a limited number of moving or glowing focal points. Approaching a block reveals awnings, cables, vents, shutters, kiosks, parked vehicles, road wear, curb/drain detail, and people without turning every screen cell into noise. Park/nature retain their quieter green/cyan identity.

```text
dark street and foreground silhouettes
  -> partly filled near building massing
    -> windows, signs, and façade fixtures at several depths
      -> sparse local light pools, steam, fireflies, people, and traffic
        -> fogged skyline with a few stable landmark colors
```

This is a hierarchy, not a request to make every surface brighter or busier.

## 1. Locked decisions

- Preserve the desktop-first, first-person CPU-raycast ASCII renderer. No 3D model pipeline, general ECS, general-purpose shader stack, dynamic shadow system, or free-roaming crowd simulation is introduced. The one approved graphics-pipeline addition is the mandatory, tightly bounded cell-aware composite pass specified in §3.5 and Phase 3.5.
- The road/ground plane remains dark and neutral. Color and contrast belong primarily to facades, windows, fixtures, signs, and localized light sources.
- Any body that looks solid—a pedestrian, parked car, or moving vehicle—has a semantic entity and collision that agrees with its rendered position. Cosmetic light, steam, sparks, rain, fireflies, cable lines, decals, and tiny distant traffic lights are non-colliding.
- All animation remains deterministic from stable identity, world position, and time. No per-frame random source or mutable particle population is allowed.
- Nearer finite-depth geometry wins through `AsciiFrame.writeIfNearer`. Decorative passes never punch through walls, fixtures, people, or vehicles.
- Keep whole-facade flashing, global light pulses, full-screen blur/bloom, and uniform fog recoloring out of scope. The required compositor may use a tiny thresholded halo around locally emissive glyphs, but it must remain cell-aware and cannot become a broad conventional post effect. A fixture may animate locally at a restrained rate; a district may have a palette identity without becoming a flat color wash.
- Keep authored maps fixed and validate any new placement data strictly. Derived render-only detail must not mutate `WorldGrid` or create hidden collision.
- The desktop client keeps its current 1280×720 logical minimum. The standard 10×18 font profile and 128×40 reference surface remain supported throughout this plan. The dense profile is a separate, benchmarked migration/pilot, not an untested replacement.
- Logical client size—not HDPI backing pixels—continues to determine the ASCII surface. World glyphs always use uniform integer presentation scale and never stretch independently in X/Y.

## 2. Existing baseline to preserve

- Near façades already receive a partial distance-faded background fill; the street plane stays neutral.
- Casino slot-machine billboards use close-only cabinet fill and return to clean glyph silhouettes at distance.
- Atmospheric emitters, ambient events, three authored pedestrians, lamps, façade props, and deterministic fireflies already compose through the shared depth buffer.
- `AsciiSpriteRegistry`, `EntitySpriteRenderer`, `GameWorld`, `SpatialEntityIndex`, and `CollisionSystem` provide the existing authoritative path for visible solid actors.
- The window/render contract presently selects a 10×18 font cell and 128×40 world surface at the 1280×720 logical minimum, with whole-number presentation scale and responsive columns at wider logical client sizes. The 96×30 calculator fixture remains useful defensive coverage but is not a supported desktop client size.

Each packet below must retain these properties unless its own acceptance criteria explicitly supersede one.

## 3. Visual language

### 3.1 Value hierarchy and façade contrast

Every exterior building should resolve into four deliberately different levels:

| Layer | Visual job | Typical treatment | Must avoid |
|---|---|---|---|
| Street/sky | Leave room for depth and landmarks | Near-black neutral backgrounds, sparse rain/data | Colored ground wash or bright fog |
| Structural mass | Make close buildings feel solid | Partial dark fill, broad `#`, `=`, `|`, or `:` fields, darker side faces | Full opaque slabs with no glyph texture |
| Surface structure | Explain scale and orientation | Window grids, seams, shutters, rails, recessed bays | Equal-density noise across every face |
| Hot accents | Direct the eye | Small warm/cyan/magenta/acid-green signs, lamps, machine displays | Repeated bright strips or whole-plane flashes |

Fog and glyph-density reduction must still make far architecture recede. A near building may gain contrast and local mass, but it must not erase the silhouette of a taller distant building above it.

### 3.2 Architectural and street detail

Use compact, repeated vocabulary rather than bespoke decoration everywhere:

- Facades: awnings, shutter ribs, balcony/railing bands, cable drops, service ladders, AC boxes, recessed door bays, menu boards, and utility panels.
- Streets: bollards, bins, delivery crates, utility cabinets, signal heads, bus-shelter frames, parked vehicle silhouettes, and one or two landmark kiosks.
- Roof/skyline: vents, water tanks, antenna stubs, exhausts, offset roof ledges, and rare animated sign crowns.
- Nature: retain tree/fixture silhouettes, waterfall movement, lamp/firefly pockets, rocks/tents, and trail markers; do not import dense city clutter into the reserve.

Large silhouette and depth placement come before micro-glyph texture. A fixture must still read when its projected width falls to a few cells, otherwise it should select a tested distant layout or cull visually while preserving any required semantic entity.

### 3.3 Road and sidewalk detail

Roads and sidewalks need a little more authored character, but remain the quiet negative space that gives facades their contrast:

- **Roads:** preserve the current dark directional data flow, then add sparse neutral lane-edge dashes, patched seams, drainage/grate strips, curb shadows, and occasional faded crosswalk/turn markings. Any warm/cyan reflection comes only from an already visible local emitter and stays finite-depth/local; it is not a second colorful ground material.
- **Sidewalks:** add restrained slab seams, curb edges, step/threshold marks, inset utility covers, tactile/gutter bands, and isolated darker wear patches. Fixture-adjacent detail should point toward entries, kiosks, lamps, or street corners rather than repeating uniformly.
- **Depth:** close road/sidewalk samples may use a little more structural glyph variation; middle distance simplifies to large clean bands; far ground keeps enough continuity to guide the eye toward the vanishing point.
- **Palette:** base ground materials remain free of `accentPaletteKeys`. Neutral fog/road/sidewalk colors own the surface; an existing nearby sign/lamp/terminal/vehicle can apply only the bounded local-light tint described below.
- **Semantics:** markings, patches, drains, seams, and curb shading do not change walkability or collision. A real bollard, parked vehicle, bin, or crate is a semantic fixture/entity according to its explicit ownership.

### 3.4 Localized lighting

Local lights make details cohere without a global lighting engine:

- A lamp, marquee, kiosk, vending screen, vehicle light, or terminal may tint only nearby *visible* frame cells that are behind its finite depth.
- The tint radius and strength fall with projected distance, are clipped to the ASCII frame, and never overwrite nearer geometry.
- Light may soften a local facade/ground background and support an already-authored glyph; it does not add a second shadow, modify material/collision data, or brighten an entire street.
- Warm gold/red-orange belongs to market/casino activity; cyan/magenta remains sparse electronic accent; acid green marks limited data/nature cues.

### 3.5 Required cell-aware composite lighting

The visual-density pass includes one small graphics pipeline alongside the ASCII renderer. It is a required composition stage, not a replacement renderer and not a general post-processing framework:

```text
CPU raycast -> AsciiFrame glyph/color/background/depth
  -> cell-lighting pass: local emitter tint + finite-depth contact information
    -> low-resolution ASCII color target + depth target + emissive target
      -> one depth-aware neon compositor: contact shade + restrained emissive halo + CRT treatment
        -> HUD/dialogue (unaffected)
```

- **Inputs:** the compositor receives only the low-resolution world color target, normalized finite-depth target, and explicit/thresholded world-emissive target. UI is composed afterward, so it cannot glow, distort, or feed depth data into the world pass. Invalid depth carries a validity bit and is never treated as a nearby wall.
- **Cell sampling:** color/depth/emissive inputs use nearest filtering and are sampled at resolved ASCII-cell centers. The shader may inspect immediate cell neighbors for contact and halo decisions, but it never applies smooth full-resolution filtering to the base ASCII image.
- **Contact shade:** derive a small darkening only from adjacent finite depth discontinuities in the resolved ASCII frame. It gives curbs, building bases, fixture feet, and near corners extra weight without attempting physically correct cast shadows.
- **Local illumination:** consume validated visible emitters and resolved depth so a lamp/sign/terminal/vehicle/firefly can affect only nearby, farther frame cells. It must not brighten an occluded wall, write through a foreground object, or create new world geometry.
- **Emissive composite:** use an explicit small brightness threshold and at most a one-cell-equivalent halo around approved bright glyphs. The halo supports signs and lights; it cannot blur the complete image, change glyph alignment, or make unlit material glow.
- **Pipeline limit:** one CPU cell-lighting stage and one presentation composite shader/FBO path are the complete permitted pipeline. It runs at ASCII-source resolution before uniform presentation scaling. No arbitrary effect chains, user-authored shader graph, shadow map, screen-space reflection, full-resolution blur stack, or global illumination follows from this approval.
- **Ownership:** `AsciiFrame` remains the authoritative ASCII/depth result. The compositor receives presentation data only and owns neither collision, interaction, simulation time, content mutation, nor HUD/dialogue rendering.

### 3.6 Living city rhythm

Movement is the exception that makes static density believable:

- Keep existing steam, events, pedestrians, and fireflies sparse and phase-staggered.
- Add static street roles and parked vehicles before adding more moving bodies.
- When the canonical route dependency permits it, add no more than two pacing pedestrian lanes and two vehicle loops. They reuse the shared semantic actor path, wait for blockers, and never push the player.
- Traffic signals, vehicle lamps, scrolling menus, and sign cursors are tiny secondary motion. The player should notice them after the street composition, not before it.

## 4. Dense ASCII surface strategy

The reference feeling partly comes from more visible text cells. Raising density is worthwhile, but it changes ray count, frame storage, presentation, UI scaling, and visual readability together; it is not a material-data tweak.

### 4.1 Profiles and rollout

| Profile | World bitmap cell | 1280×720 desktop-minimum surface | 960×540 defensive calculator surface | Role |
|---|---:|---:|---:|---|
| Standard | 10×18 | 128×40 | 96×30 | Readable fallback profile and UI font |
| Dense (target) | 8×15 | 160×48 | 120×36 | +50% world cells for finer façade/fixture detail |
| Fine (default) | 6×12 | 213×60 with 1 px side gutters | 160×45 | +150% cells over Standard; default world surface |

The dense 8×15 profile preserves a similar glyph aspect to the current 10×18 profile and exactly fits the 1280×720 desktop minimum at 1×. Its 960×540 result is retained only as defensive calculator coverage. It is intentionally more conservative than blindly matching a much larger video grid: it must prove readability, per-frame allocation bounds, and target frame time on the project’s CPU renderer first.

Fine is now the default desktop world profile because its denser close/middle-distance architecture is the approved visual direction. `--ascii-density=standard` and `--ascii-density=dense` remain supported fallbacks. To keep Fine responsive, its CPU phosphor copies are limited to explicit emissive glyphs while the existing full-resolution shader preserves the compact phosphor halo; its cell-aware haze samples the emitter cell plus its four cardinal neighbors, omitting diagonal samples that fall at the very edge of the 1.42-cell radius. The HUD, dialogue, and error overlay remain independently readable on the proven UI font rather than inheriting a smaller world glyph blindly.

### 4.2 Required technical shape

- Replace global `FontMetrics` world-cell constants with an immutable, validated `AsciiFontProfile`/`WorldSurfaceProfile`: font path, cell width/height, baseline, reference/minimum columns/rows, and printable ASCII coverage.
- Keep UI font metrics separate from world presentation metrics. `AsciiViewport`, `AsciiViewportCalculator`, `ProjectionMetrics`, `WorldAsciiRenderer`, `AsciiPresenter`, interaction center-ray construction, HUD/dialogue layout, and smoke diagnostics must consume the selected profile explicitly.
- Load both licensed bitmap fonts through `GameAssets`; validate every printable ASCII glyph, uniform advance, texture filtering, exact line height, and integer baseline. A failed selected profile produces the existing readable asset failure path, never a mixed-metric frame.
- Allocate/reuse `AsciiFrame` and CRT buffer at the selected profile dimensions. No drawable/backing-pixel value may silently raise columns, rows, rays, or buffer size.
- Preserve uniform whole-number scaling, gutters, FOV/vertical-framing rules, reticle alignment, and world-space depth behavior. Higher density improves sampling; it must not alter world collision, map scale, player speed, yaw/pitch limits, or entity coordinates.

### 4.3 Fine default-profile gates

Fine is the approved desktop default. It remains subject to these ongoing release gates:

- Standard snapshots and behavior remain unchanged when Standard is selected.
- Fine viewport fixtures prove the desktop minimum 1280×720 → 213×60 @1× and retain 960×540 → 160×45 @1× as defensive calculator coverage; representative 16:9, ultrawide, 4K, non-divisible, and HDPI cases retain uniform integer scale and logical-size ownership.
- Projection/interaction fixtures prove equivalent world-space center-ray targeting, collision, vertical framing, and occlusion at each profile.
- A warmed 3,600-frame reference-view allocation probe remains within the existing 4 KiB steady-state threshold, or a documented replacement threshold is justified before approving the profile.
- The recorded target frame-time budget survives a Fine worst case: varied-height facade view, all approved fixture/light passes, actors, rain, debug off/on, and the widest supported logical surface.
- Visual review confirms the Fine font improves close/middle-distance readability rather than creating shimmer, CRT blur, tiny unreadable HUD text, or a field of uniform noise.

## 5. Architecture and data ownership

| Component | New/extended responsibility | Must not own |
|---|---|---|
| `MaterialSampler` / presentation tuning | Value tiers, distance-safe glyph-density/contrast rules, and tested palette restraint | Collision, map mutation, or global lighting |
| `FacadeDetailRenderer` (proposed) | Depth-tested awnings, rails, panels, service fixtures, close/distant glyph layouts | Door interaction, building collision, or free-form particles |
| `LocalizedLightRenderer` (proposed) | Bounded visible-cell tint around validated emitters after geometry resolves | Shadow maps, global illumination, material mutation, or collision |
| `AsciiCompositeRenderer` (proposed) | Cell-depth contact shade plus the one final emissive/CRT composite path | A general effect chain, world mutation, UI composition, or collision |
| `ParkFixtureBillboardRenderer` / `FacadeLabelRenderer` | Reuse/extend compact fixture and sign vocabulary where their ownership already fits | Generic semantic actor projection |
| `AsciiSpriteRegistry` / `EntitySpriteRenderer` | Static pedestrian, parked-car, and vehicle sprite projection with shared depth | Actor scheduling or collision inference |
| `AmbientActorSystem` / `TrafficSystem` | Fixed-step authoritative pacing/vehicle motion when the relevant Life Pass packets are approved | Rendering-only props or story progression |
| `AsciiFontProfile` (proposed) | Validated world-font metrics and standard/dense profile identity | UI text layout, world simulation, or raw desktop dimensions |
| `AsciiViewportCalculator` / `AsciiPresenter` | Selected profile’s grid, uniform scale/gutters, frame/CRT presentation | Map geometry or HDPI-driven density changes |
| `WorldRenderStats` | Candidate/write counts for new passes and profile dimensions for smoke evidence | Gameplay state |
| `ContentLoader` / registries | Strict optional definitions for any authored façade/street fixtures | Deriving collision from glyphs |

### Proposed content contract

Do not introduce a general scene graph. If authoring needs more than existing facade signs/objects, add one optional map-scoped array for compact validated fixture definitions:

```json
"visualFixtures": [
  {
    "id": "fixture.market-awning-01",
    "kind": "awning",
    "cellX": 42,
    "cellZ": 18,
    "face": "south",
    "paletteKey": "warmGold"
  }
]
```

- `kind` is a closed enum such as `awning`, `rail`, `ac-unit`, `cable-drop`, `kiosk`, `utility-box`, `bollard`, `crate-stack`, or `parked-car`.
- A facade-attached kind validates an opaque exterior host face; a free-standing kind validates a clear placement and declares whether it is semantic/blocking.
- `paletteKey`, kind/face, dimensions, host cell, overlap, bounds, prohibited story cells, and required path clearance validate before world construction.
- `parked-car` is an authoritative blocking entity/sprite if it has a body. Decorative cables, panels, rails, light tints, and signs remain render-only.
- The final schema is designed only in the corresponding implementation packet after deciding whether current facade sign/object data can represent the need cleanly. The JSON above is illustrative, not an accepted schema today.

## 6. Implementation sequence

### Phase 0 — Measurement and visual rules

Capture a named city route with near/middle/far façade views, market, archive, park, Cascade waterfall, water-park, nature reserve, and casino. Record frame hashes, write counts, DDA steps, allocations, logical surface/profile, and 1280×720 plus representative ultrawide captures.

Establish a visual review sheet for every packet:

- Is the street darker and quieter than the architecture?
- Can the player distinguish near front/side faces, middle fixtures, and far skyline at a glance?
- Does each bright point identify a place or object rather than merely add noise?
- Does visible solidity agree with collision and authored placement?
- Does the route retain story doors, terminals, Rin, prompts, and navigation readability?

### Phase 1 — Value hierarchy and material readability

First improve the existing renderer without adding new world bodies:

1. Audit facade materials by depth band and side face; give each a readable structural/mid/hot-accent ramp.
2. Tune close/middle/far background fill and glyph density per facade family, preserving the dark street and fogged skyline.
3. Add tested facade-specific window/seam rhythms so towers do not collapse into repeated uniform glyph fields.
4. Add sparse neutral road/sidewalk seam, curb, lane-edge, drainage, and wear sampling that keeps the ground darker than the built environment.
5. Recheck casino, water-park, park, and nature to ensure city contrast rules do not flatten their intentionally different identities.

### Phase 2 — Dense surface pilot and adoption decision

Raise the sampling density before adding more visual layers, so fixture, lighting, and compositor work is tuned for the surface the city will actually use:

1. Add and validate the licensed 8×15 dense bitmap world font plus `AsciiFontProfile` selection while retaining the 10×18 Standard profile.
2. Make world-surface consumers profile-aware; keep UI readability independent.
3. Add profile-specific viewport, projection, interaction, presenter, render-hash, allocation, and worst-case performance tests.
4. Review paired Standard/Dense screenshots for the same route at the 1280×720 desktop minimum and ultrawide. Retune the Phase 1 material ramps and CRT presentation for Dense before adding fixtures or lighting.
5. The current policy is Fine as the desktop default with Standard/Dense launch-selectable fallbacks. Continue to measure the default profile at reference and widest surfaces; update `rendering-contract.md`, user-facing verification notes, and launcher/help text if that policy needs to change.

### Phase 3 — Architectural fixtures and local lights

Build the compact fixture vocabulary and use it only at high-value sightlines:

1. Validate render-only façade fixtures with host-face/depth/LOD behavior.
2. Add awnings, shutters, rail bands, AC/service boxes, cable drops, and recessed entries across market and archive approaches.
3. Add bounded local light tint sourced from lamps, marquees, kiosks, terminals, and approved fixtures.
4. Add a small street kit: bollards, utility boxes, crates, kiosk frame, and a few non-blocking decorative signs.

### Phase 3.5 — Required cell-aware composite lighting

Implement this before expanding semantic city population, so the new fixtures and actors land in a stable visual language:

1. Add an allocation-bounded cell-lighting pass that derives finite-depth contact information and validated local-emitter tint from `AsciiFrame` data.
2. Extend the existing presentation FBO path with low-resolution nearest-filtered color/depth/emissive targets and exactly one depth-aware neon compositor shader. It preserves aligned glyph cells while adding contact shade plus a thresholded, one-cell-equivalent emissive halo to approved bright output.
3. Instrument cell-lighting candidates/writes, composite resolution, and active profile in smoke diagnostics.
4. Review paired composite-off/composite-on captures from the same 1280×720 and ultrawide views; tune by reducing radius/strength before adding emitters or effects.

### Phase 4 — Static occupation and district identity

Increase perceived life before simulation complexity:

1. Author static vendor, sitter, umbrella, machine-user, and generic pedestrian sprites through the shared entity-sprite registry.
2. Place only safe, semantic actors beside landmarks and wide sidewalk/plaza cells; validate story-route clearance.
3. Add semantic parked vehicles only where the map supports a safe route around them; add their local lamp/display treatment through the light pass.
4. Apply district authoring budgets: warm market density, sparse clinical archive, green/cyan park and reserve, gold/red casino.

### Phase 5 — Bounded moving life

Implement only after static density and route clearance are approved:

1. Complete the Life Pass’s traffic signals, pacing pedestrians, and up-to-two vehicle loops through its shared actor rules.
2. Add compact vehicle headlights/tail lights and slower stable signal phases as render-only accents on top of authoritative bodies.
3. Run fixed-step, blocker, culling, resize, pause/resume, and long-trace checks; reduce density instead of adding avoidance AI if a route becomes noisy or blocked.

## 7. Verification matrix

| Area | Automated evidence | Desktop evidence |
|---|---|---|
| Contrast | Material sampler near/middle/far and side-face ordering; roads remain neutral | 1280×720 near façade, cross-street, and skyline captures |
| Roads/sidewalks | Ground sampler close/middle/far continuity; neutral base palette and no collision change | Cross-street, lane approach, and sidewalk-entry captures at 1280×720 |
| Fixtures | Content validation for face/bounds/overlap/clearance; finite-depth/LOD/occlusion tests | Market and archive approach from both sides |
| Local lights | Radius/falloff/clipping/near-depth tests; no frame allocation | Night-view captures prove local tint without global wash |
| Composite lighting | Depth-neighbor/contact-shade, emitter occlusion, threshold, glyph alignment, and bounded-allocation tests | Paired composite-off/on 1280×720 and ultrawide captures show stronger depth without image blur |
| Static bodies | Sprite projection + collision/index agreement; no story-cell obstruction | Walk around every actor/parked car, then turn away/reapproach |
| Moving life | Fixed-step traces, waiting, collision, stable IDs, interpolation | Player blocks/clears every pacing and vehicle lane; pause/resize/relaunch |
| Dense profile | Profile/font/viewport/projection/interaction/presenter fixtures; Standard snapshots unchanged | Paired Standard/Dense 1280×720, ultrawide, and long-session review |
| Performance | 3,600 warmed frames, allocations, DDA/write counters, frame-time benchmark | Dense worst-case route with debug off/on; no error logs or UI clipping |
| Story safety | Last Packet route regression if/when canonical route is complete | Doors, terminals, Rin, objective, dialogue, and map travel remain clear |

All renderer or palette work requires visually reviewed 1280×720 captures before accepting changed frame hashes. Responsive/dense-profile work additionally requires a representative ultrawide capture and actual logical-client-area evidence. Run focused tests, `:core:validateContent` for content changes, `./gradlew clean check`, and `git diff --check` for every completed packet.

## 8. Checklist

### Phase 0 — Baseline and design lock

- [ ] **VD-01:** Capture a repeatable visual-density route and record current hashes, stats, allocations, logical surface, and reviewed baseline screenshots. **Files:** verification script/notes/captures. **Done when:** city, park, Cascade, reserve, water-park, and casino evidence identify the exact views that later packets compare against.
- [ ] **VD-02:** Publish a material/fixture district sheet covering value tiers, allowed accent density, landmark colors, and forbidden global effects. **Files:** this plan and, after implementation, palette/presentation documentation. **Done when:** a reviewer can reject a proposed change as too bright, too uniform, or in the wrong district without subjective guesswork.

### Phase 1 — Contrast and façade readability

- [x] **VD-03:** Add façade-family depth/side/LOD contrast tuning with structural glyph ramps. **Files:** presentation/material sampling, focused tests, rendering contract. **Evidence:** existing close-fill/side-face rules remain intact while `facade-ribbed` low/mid façades add sparse vertical ribs/belt seams and `facade-window-grid` garden/residential/tall façades add stable window divisions inside 22 world units; the fogged skyline falls back to the existing animated material ramps.
- [x] **VD-03A:** Add sparse neutral road and sidewalk detail: lane-edge/crossing marks, patched seams, drainage strips, curb edges, slab joints, and utility-cover/wear sampling. **Files:** materials/presentation sampler/tests. **Evidence:** `road-detail` uses four-cell neutral seam blocks only inside 18 world units and `sidewalk-detail` uses two-cell slab joints only inside 16; both retain their animated base ramp between marks, remain accent-free, and do not affect collision/walkability. Road asphalt now owns a restrained charcoal background above sky black, so the close/middle street reads as pavement rather than void without becoming a bright ground wash.
- [x] **VD-04:** Add stable window/seam pattern variation for key exterior facade families. **Files:** materials/presentation sampler/tests. **Evidence:** deterministic material tags select stable `+`/`-`/`|` detail only on structural boundaries; test sweeps cover low, residential, and tall façades plus road/sidewalk close/far behavior while the existing data-stream sample continues to animate away from a seam.
- [x] **VERIFY-VD1:** Review 1280×720 and ultrawide material captures, update only affected snapshots, and run the full check. **Evidence:** reviewed north/cross-street 1280×720 plus north ultrawide captures retain a dark neutral ground plane while improving close façade mass/structure; all seven affected city frame hashes were updated after review.

### Phase 2 — Higher-density ASCII surface

- [x] **DENSE-01:** Add the licensed 8×15 bitmap world font and profile validation while keeping the 10×18 Standard profile. **Files:** `assets/fonts/vt323-8x15.*`, generator, `AsciiFontProfile.kt`, `GameAssets.kt`, profile/font tests. **Evidence:** both bundled profiles cover printable ASCII 32–126 with exact uniform cell/advance metrics, nearest filtering, and integer placement; a failed font contract reports both its asset path and profile identity.
- [x] **DENSE-02:** Make viewport, frame, presenter, projection, interaction, diagnostics, and world renderer profile-aware. **Files:** `AsciiFontProfile.kt`, viewport/presenter/screen/renderer/UI boundary tests. **Evidence:** profile-specific snapshot fixtures remain explicit; Dense produces 160×48 at 1280×720 and defensive 120×36 at 960×540, ignores HDPI backing size, maintains uniform integer scaling, preserves centered interaction targeting and vertical FOV, and reports its active profile in F3 diagnostics. Fine is now selected by the desktop default.
- [x] **DENSE-03:** Tune dense material ramps, fixture LOD, CRT presentation, and HUD separation from paired visual evidence. **Depends:** DENSE-02. **Evidence:** paired Standard/Dense 1280×720 review retained the existing material and CRT values: Dense adds sampling detail without shimmer or disproportionately broad phosphor halos, while HUD/dialogue continue to use the 10×18 UI font. The dense presentation route retains readable controls, reticle, diagnostics, and map/interaction behavior across resize, pause/resume, and music toggles.
- [x] **DENSE-03A:** Add the Fine 6×12 default world profile for visibly higher sampling density. **Evidence:** Fine produces 213×60 at 1280×720 and defensive 160×45 at 960×540, retains a separately validated licensed VT323 atlas and the 10×18 UI boundary, and completed the moving presentation route plus actual-work-area ultrawide smoke check. It is now the default after the Fine presenter cut generic CPU glow copies and diagonal emissive taps while retaining explicit-emitter haze; Standard and Dense remain launch-selectable fallbacks.
- [ ] **DENSE-04:** Continue benchmarking and tune the default profile budget. **Depends:** DENSE-03. **Done when:** 3,600-frame allocations, target frame-time, widest supported surface, full renderer stats, presentation route, 1280×720 and ultrawide captures all pass; the recorded budget confirms Fine remains the default or documents a fallback policy.

### Phase 3 — Detail and local lighting

- [x] **VD-05:** Audit existing sign/object data for Phase 3. **Evidence:** the existing validated `facadeSigns`/`facadeEntrances` contract already provides stable IDs, exterior-face and roof/bounds checks, non-overlap validation, palette validation, close/distant layouts, labels, and collision-free ownership. A parallel `visualFixtures` schema was unnecessary.
- [x] **VD-06:** Use the existing depth-tested façade/street fixture layouts with tested close/distant variants. **Evidence:** `FacadeSignSampler` attaches wall-mounted signs, props, and entrances to their host faces at wall depth; `FacadeLabelRenderer` supplies compact front-on readable labels; `ParkFixtureBillboardRenderer` retains semantic fixture silhouettes. Existing focused sampler/label/fixture tests cover LOD, occlusion, and no collision inference.
- [x] **VD-07:** Implement bounded localized light tint from approved visible emitters. **Files:** `LocalizedLightRenderer.kt`, `AsciiFrame.kt`, world stats, focused tests, rendering contract. **Evidence:** post-geometry tint reads only finite resolved emissive cells at `0.85+`, affects a one/two-cell clipped radius, rejects nearer/hidden and sky cells, allocates no per-cell objects after warmup, and reports `localLightWrites` in smoke diagnostics.
- [x] **VD-08:** Author restrained market/archive fixtures and local-light support. **Files:** existing city façade-sign data, approved captures. **Evidence:** the validated `prop.vending-archive` and `prop.noodle-market` fixtures now have compact `ARCHIVE 24H` and `OPEN NOODLES` labels that feed the bounded local-light pass without changing routes, signs, doors, terminals, or collision.
- [x] **VERIFY-VD2:** Complete fixture/light visual and performance review. **Evidence:** reviewed Fine 1280×720 archive/market captures show compact local facade color without a global road wash; focused depth, threshold, and 3,600-frame allocation tests cover the tint pass; full verification recorded below.

### Phase 3.5 — Required cell-aware composite lighting

- [x] **COMP-01:** Produce low-resolution nearest-filtered world color, finite-depth, and emissive inputs after the world frame resolves. **Files:** `AsciiFrame.kt`, `AsciiCompositeData.kt`, `AsciiPresenter.kt`, source renderers, focused tests/stats. **Evidence:** finite depth is normalized with an explicit validity alpha; lamps, façade labels/signs, fireflies, and casino light parts write explicit presentation-only emissive strength through the winning depth cell; reusable targets exactly match the selected ASCII surface and exclude HUD/dialogue. Transparent mask pixels use overwrite blending so moving emitters and camera turns cannot retain stale haze.
- [ ] **COMP-02:** Implement validated local-emitter tint through the same cell-lighting pass. **Depends:** COMP-01, VD-07. **Files:** emitter query/light pass/tests. **Done when:** tint is local, depth-occluded, deterministic, allocation-bounded, and does not recolor an entire road or hidden geometry.
- [x] **COMP-03:** Extend the existing ASCII presentation buffer with one depth-aware neon compositor shader. **Depends:** COMP-01. **Files:** scoped `AsciiPresenter` extension, shader/input tests. **Evidence:** the existing single CRT shader now samples nearest-filtered cell depth/emissive targets at cell centers, darkens only the farther side of immediate finite-depth discontinuities, and adds a soft emitter-colored haze that reaches at most one neighboring cell. Base glyphs stay aligned; UI renders afterward; no broad full-image blur or effect chain was added.
- [ ] **VERIFY-COMP:** Approve the mandatory depth-aware neon compositor from paired composite-off/on reference and ultrawide captures plus focused allocation/performance tests. **Depends:** COMP-01 through COMP-03. **Done when:** the on state clearly improves building/curb/fixture depth and neon presence without conventional blur, global bloom, washed roads, or a frame-time regression beyond the recorded budget.

### Phase 4 — Static life and district composition

- [ ] **VD-09:** Author and validate static street-role sprites through the shared sprite registry. **Depends:** canonical `SPRITE-01/02` foundation. **Done when:** each role has close/distant readability and transparent glyph areas preserve frame depth/background.
- [ ] **VD-10:** Place safe semantic static actors and parked vehicles with story-route clearance validation. **Depends:** VD-09. **Done when:** every solid body agrees with `GameWorld`/spatial-index/collision state and never blocks a mandatory interaction route.
- [ ] **VD-11:** Tune district-specific density budgets and landmark palette ownership. **Depends:** VD-08, VD-10. **Done when:** market, archive, park/reserve, and casino are distinguishable without globally increasing saturation or object count.
- [ ] **VERIFY-VD3:** Walk the static-density route at both reference sizes and ultrawide. **Done when:** no actor/vehicle loses collision agreement during culling/resize and scene readability survives close and distant views.

### Phase 5 — Moving life

- [ ] **VD-12:** Complete the dependent Life Pass traffic/pacing packets and integrate their visual accents. **Depends:** `ROADLIFE-01`, `CROWD-03`, `TRAFFIC-01/02` in `devplan-life-pass.md`. **Done when:** movement is sparse, stable, collision-safe, and strengthens rather than dominates the street composition.
- [ ] **VERIFY-VD4:** Run fixed-step, blocker, pause, resize, map travel, and soak evidence for the combined density pass. **Done when:** actors/traffic remain deterministic and the canonical story route, once available, stays completable.

## 9. Out of scope

- Copying the reference implementation, assets, city layout, or exact visual output.
- A general lighting engine, global illumination, shadow maps, arbitrary effect chains, reflections, volumetric effects, or dynamic day/night cycle. The single mandatory cell-aware composite pass is the only graphics-pipeline exception.
- Unbounded crowds, schedules, pathfinding, vehicle combat, driving, or traffic simulation beyond authored lanes.
- Changing collision because a glyph looks filled, or rendering a solid person/vehicle without a semantic owner.
- Making every district equally neon, increasing palette saturation to compensate for weak value design, or filling all open space with props.
- Replacing fixed maps with procedural city generation or changing the Last Packet story scope.

## 10. Open decisions to resolve during implementation

- Which current façade material families gain dedicated structural patterns versus sharing a common sampler rule.
- Whether a compact `visualFixtures` array is actually necessary after auditing the existing façade sign/object contracts.
- The exact city locations and collision footprints for parked vehicles, kiosks, and static population, after route captures identify safe sightlines.
- Fine default-profile fallback policy for lower-spec devices, if the verified widest-surface budget requires one.
- Performance target measurements for the Fine worst case, recorded on the developer’s real desktop alongside allocation and frame-time evidence.
