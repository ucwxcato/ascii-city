# ASCII City Life Pass — Development Plan

> **Status:** First-pass subset implemented and under verification on 2026-08-25: five stateless steam/exhaust emitters, three animated façade props, four deterministic positional micro-event emitters, three blocking pedestrians with authored fixed-step routes/schedules, and derived green fireflies around park/nature lamps. Phase 0's data-lock/shared-actor prerequisites and Phase 1's steam/exhaust work were reconciled on 2026-08-25; the required canonical `VERIFY-07` story-route gate remains open. The broader storefront, traffic-signal, cat, semantic-traffic, density, soak, and end-to-end checklist below remains planned.
>
> **Scope update:** The user explicitly added positional ambient emitters to this subset. The implemented vent, spark, and horn one-shots are therefore an approved exception to the original audio non-goal below; no new music, voice, or control was added.
>
> **Purpose:** Make `city-alpha` feel inhabited and mechanically coherent through steam/exhaust, ambient citizens, bounded road activity, living storefronts, and park wildlife.
>
> **Authority:** This document owns the optional city-life implementation and verification contract after the canonical `Last Packet` route passes `VERIFY-07`. Player-controlled free-roam vehicles and racing are owned by the core [devplan-open-world-racing.md](devplan-open-world-racing.md), not this life pass. The canonical game, story, renderer, and release plan remains [ascii-city-development-plan.md](ascii-city-development-plan.md); the map and projection contracts remain [map-format.md](../map-format.md) and [rendering-contract.md](../rendering-contract.md).
>
> **Target:** The existing Kotlin/LibGDX desktop game, fixed `city-alpha` map, responsive CPU-rendered first-person ASCII surface with a 96×30 minimum and a 128×40 reference canvas, and current lightweight domain model.

## 0. Outcome

The player can cross the city and repeatedly notice small, restrained signs of activity: steam lifting from grates and rooftops, occupied sidewalks, safe traffic motion, animated storefront machinery, green fireflies near park lamps, and one rare cat encounter. Every visible solid actor agrees with collision, every cosmetic effect respects depth, and none of the life pass changes story progression.

Complete when:

```text
Launch city-alpha -> walk from the park through the market and archive district ->
see depth-correct steam, citizens, traffic, storefront activity, fireflies, and the cat ->
approach and pass each feature safely -> complete the Last Packet route unchanged ->
pause, resize, resume, and exit with deterministic state and bounded performance
```

## 1. Verified baseline and dependency boundary

### Verified now

- `WorldAsciiRenderer` renders ground and walls, then depth-tested lamps, then sky-only rain into one `AsciiFrame`.
- `AsciiFrame.writeIfNearer` provides the shared depth rule required by new world-space effects.
- `GameWorld`, `SpatialEntityIndex`, and `CollisionSystem` already support adding, moving, removing, and colliding with blocking `WorldEntity` instances.
- `ContentLoader` strictly validates `city-alpha.objects.json`; unknown fields and invalid references fail startup with file/JSON-path diagnostics.
- Canonical `SPRITE-01/02` and `NPC-01/02` are verified: `AsciiSpriteRegistry` loads validated static sprite art, `EntitySpriteRenderer` projects camera-facing entity sprites with per-character depth testing, and `NpcSystem` composes Rin from one authoritative blocking world entity in stable ID order without culling/resize desynchronization.
- Park lamps, semantic park tags, exterior building faces, roads, sidewalks, alleys, animated road materials, and the connected Cascade Relay nature spur already exist and can anchor life-pass content.
- Spatial waterfall ambience already derives from semantic waterfall cells and is independently proximity-mixed. This plan must preserve it but does not own or expand its audio contract.

### Dependency rule

This is a focused post-MVP polish plan, not a replacement for Phase 7. No life-pass implementation packet begins until the canonical `Last Packet` route passes `VERIFY-07`; this lets final story targets, prompts, doors, and sightlines define safe ambient placements. After that gate, implement the restrained render-only work before semantic actors and implement cosmetic traffic vehicles last. Citizens, cosmetic traffic vehicles, and the cat must reuse `AsciiSpriteRegistry`, `EntitySpriteRenderer`, and the canonical `NPC-01/02` world-entity integration; they must not create a second sprite projection or actor-occupancy system. Player-controlled vehicles are separately owned by [devplan-open-world-racing.md](devplan-open-world-racing.md). The life pass remains optional and is not a release gate.

## 2. Locked decisions

- Preserve first-person street-level ASCII rendering. Do not introduce models, shaders, a perspective camera, an ECS, a general particle engine, or an external UI framework.
- Divide the pass into three ownership classes:
  - **Render-only attachments/effects:** steam, exhaust, storefront frames, traffic signals, and fireflies. They never define collision or mutate authored content.
  - **Semantic static actors:** vendors, sitters, umbrella pedestrians, and a visible resting cat. They exist as blocking world entities and use the shared entity-sprite layer.
  - **Semantic moving actors:** pacing citizens, vehicles, and a fleeing cat. Fixed-step systems move their authoritative world entities; rendering interpolates the same state.
- Anything that looks like a person, animal, or vehicle body is semantic while visible. The player must never walk through a rendered body, and invisible/cull state must never remove its collision.
- Small steam glyphs, signal lamps, fireflies, and vehicle-light accents are visibly cosmetic and non-collidable.
- Moving actors use explicit authored lanes only. There is no navigation mesh, free-roaming AI, crowd simulation, traffic network solver, chase behavior, or general pathfinding.
- Citizens and vehicles process in stable entity-ID order. A blocked actor waits; it never pushes the player, teleports through a blocker, swaps through another actor, or deals damage.
- Actor simulation runs through the existing fixed-step loop. Frame rate, resizing, culling, pausing, and render interpolation cannot change authoritative outcomes.
- Steam and firefly motion is stateless and deterministic: stable ID, authored phase, world position, and simulation time produce every sample. No per-frame random generator or mutable particle list is allowed.
- Storefront animation changes local glyphs only. Whole facades, panels, or streets may not flash; animation rates remain at or below 2 Hz except continuous mechanical glyph motion.
- Reuse the current restrained palette. Steam uses fog/neutral tones; storefronts and traffic favor warm gold, red-orange, cream, acid green, amber, and existing neutral colors rather than adding more dominant cyan/magenta.
- `city-alpha.objects.json` remains the map-specific placement authority. Shared entity-sprite art remains in the Phase 7 sprite registry/file selected by `SPRITE-01`; this plan references stable sprite IDs instead of embedding duplicate actor art.
- All new top-level object arrays are optional. Omitting them produces the current verified city. If an array is present but invalid, loading fails closed before constructing the world.
- Life-pass actors cannot own dialogue, objectives, quest state, rewards, doors, or Last Packet transitions. Story NPCs remain owned by the canonical plan.
- Session behavior resets on relaunch. This plan adds no save schema, permanent population state, economy, schedules, or day/night cycle.
- Maintain the existing logical window bounds and ASCII-grid limits. New passes must be clipped, depth-tested, allocation-bounded, and safe at 960×540 and 1280×720.

### Initial `city-alpha` density caps

These are hard first-pass caps, not targets to fill indiscriminately.

| Feature | Initial cap | Motion cap | Readability rule |
|---|---:|---:|---|
| Steam/exhaust emitters | 8 | At most 3 live wisps each | Never cover prompts/reticle or overwrite nearer depth |
| Ambient citizens | 10 | At most 2 pacing citizens | Never occupy a one-cell story doorway or mandatory interaction cell |
| Traffic actors | 2 | Both may move on separate authored loops | Stop for the player and blockers; no damage or pushing |
| Traffic signals | 8 intersections | Derived deterministic phases | Only one small signal cluster per qualifying corner |
| Storefronts | 8 | At most 4 frames each | One restrained animated region per host wall run |
| Firefly swarms | 4 outdoor park/nature lamps | At most 3 visible green glyphs each | Small local glow only; no full-screen pulsing |
| Cat | 1 | One authored flee lane | Hides for the session after reaching its safe endpoint |

## 3. Goals and non-goals

### Goals

- Add recognizable motion and occupation to alleys, rooftops, sidewalks, roads, storefront walls, and the park.
- Make every placement content-authored, strictly validated, and reproducible.
- Reuse existing depth, fog, entity-sprite, collision, fixed-step, and lifecycle behavior.
- Keep the player’s route, prompts, landmark readability, and dark neutral ground visually dominant.
- Provide headless regression coverage plus required desktop visual/runtime evidence.

### Explicitly out of scope

- Large crowds, schedules, jobs, homes, dialogue for background citizens, reputation, police, combat, or crowd reactions.
- General NPC patrol/pathfinding. This pass permits only validated short pacing/flee lanes; canonical `NPC-03` remains the owner of general patrol behavior.
- Traffic laws, intersections with physical right-of-way simulation, collisions that injure/push the player, or vehicle interiors. Player-controlled free-roam vehicles and racing are owned by [devplan-open-world-racing.md](devplan-open-world-racing.md).
- Arbitrary particle graphs, GPU particles, smoke physics, volumetric fog, dynamic shadows, bloom, reflections, or weather simulation.
- Enterable shops, purchasing, inventory, minigames, opening hours, or interactive vending transactions.
- Wildlife ecology, random spawning, breeding, feeding, collectible animals, or persistence.
- New music, voice, controls, or environmental audio beyond the existing rain and semantic waterfall layers. Those require a separate audio scope.
- Procedural placement or support for maps other than validated fixed-map content during this pass.

## 4. Player experience

### 4.1 Steam and rooftop exhaust

- Alley/grate emitters release narrow `.'o` wisps that rise, drift slightly, thin with age, and disappear.
- Rooftop exhaust begins at the validated roof height of an opaque building cell and remains visible above the skyline only when not occluded by nearer geometry.
- Stable phase offsets prevent every emitter from pulsing together.
- Steam stays neutral and sparse; it must not resemble a solid wall or obscure the skyline continuously.

### 4.2 Street population

- Static roles provide the first density layer: an umbrella pedestrian under a lamp, a vendor beside a storefront, a seated figure at a park bench, and a person using a machine.
- Up to two pedestrians pace along short authored sidewalk/plaza lanes after static collision/render agreement is proven.
- Background citizens show no interaction prompt. They wait when the player or another blocking entity occupies their next safe step.
- Distance LOD may reduce a citizen to a compact silhouette, but rendering and collision keep the same stable entity ID.

### 4.3 Road activity

- Derived traffic signals change slowly at qualifying road intersections using stable, offset phases.
- One or two compact ASCII vehicles follow explicit road-only waypoint loops. Their bodies and lights use the shared entity-sprite layer and semantic collision.
- A vehicle approaches no closer than its configured stop clearance to the player or a blocker. It waits and resumes; it cannot shove, damage, trap, or pass through the player.
- Existing ground `traffic-stream` animation remains data flow. New vehicles and signals add readable physical context without recoloring the whole road.

### 4.4 Living storefronts

- Wall-mounted storefront frames provide vending displays, noodle/menu panels, arcade cabinets, security shutters, and service kiosks.
- Each storefront uses 1–4 rectangular printable-ASCII frames with a close/distant layout or a tested small projection. Motion is local: a cursor moves, a shutter ticks, or one indicator changes.
- Storefronts share the host wall’s perpendicular depth, fog, side shading, and occlusion exactly as facade marquees do.
- Storefronts are presentation-only in this pass. They show no `[E]` prompt and do not modify story or inventory.

### 4.5 Park wildlife

- Outdoor park/nature lamps derive small green firefly swarms. Each firefly follows a deterministic orbit/bob pattern and is depth-tested against trees, fixtures, actors, and walls. Waterfall-adjacent lamps are prioritized; indoor water-park fixtures are excluded.
- One authored cat rests near the park edge. When the player enters a short trigger radius, it follows a validated flee lane to an alley/cover endpoint and then becomes hidden for the rest of the session.
- While visible, the cat has a small semantic collision radius. On hiding, its rendered instance and entity occupancy are removed together exactly once.

## 5. Architecture and ownership

### Existing owners to reuse

| Existing component | Life-pass use |
|---|---|
| `ContentLoader` / `ContentVerifier` | Parse and cross-validate all optional life-pass arrays before world construction. |
| `LoadedContent` | Carry immutable render definitions and actor behavior definitions beside the constructed `GameWorld`. |
| `GameWorld` / `SpatialEntityIndex` | Own visible solid citizens, cosmetic traffic vehicles, and the cat; update positions/removal atomically. |
| `CollisionSystem` | Validate actor steps and player clearance; ignore the actor currently moving by stable ID. |
| `FixedStepClock` / `GameScreen` | Advance ambient/traffic systems at the same fixed delta as player simulation. |
| `AsciiSpriteRegistry` / `EntitySpriteRenderer` | Render all citizen, cosmetic traffic-vehicle, and cat bodies with the verified shared projection/depth rules. |
| Canonical `NPC-01/02` entity integration | Establish the shared blocking actor, spatial-index, culling, and resize-safe rendering path before ambient actors are added. |
| `WorldAsciiRenderer` / `AsciiFrame` | Compose storefronts/effects and enforce finite-depth writes/clipping. |
| `StreetLampRenderer` | Anchor derived firefly swarms to authored lamp fixtures without duplicating lamp placement. |

### Proposed focused components

| Component | Owns | Must not own |
|---|---|---|
| `LifePassContent` | Immutable lists of emitters, storefronts, citizen definitions, and traffic definitions. | Runtime mutation or LibGDX assets. |
| `AtmosphericEmitter` | Stable ID, anchor, kind, drift direction, and phase. | Particle instances or collision. |
| `AtmosphericRenderer` | Stateless steam/exhaust sampling, projection, fog/depth writes, glyph budget. | World mutation or general particle APIs. |
| `Storefront` / `StorefrontSampler` | Validated exterior wall placement, frames, palette references, LOD, local animation. | Interaction, inventory, or collision. |
| `AmbientActorDefinition` | Sprite/role/facing, collision radius, optional short motion contract. | Dialogue or generic behavior trees. |
| `AmbientActorSystem` | Stable-order static/pace/flee state, collision-safe fixed-step movement, previous/current render pose, cat hiding. | Input, pathfinding, or story state. |
| `TrafficActorDefinition` | Sprite, blocking radius, road-only waypoint loop, speed, phase. | Route generation or traffic AI. |
| `TrafficSystem` | Swept collision-safe lane motion, stop/wait/resume, previous/current pose. | Damage, pushing, traffic-law simulation, or player movement. |
| `TrafficSignalRenderer` | Derived intersection fixtures and deterministic signal phase. | Road collision or vehicle authority. |
| `ParkFireflyRenderer` | Derived green firefly swarms around eligible outdoor park/nature lamps. | Cat state or semantic collision. |

### Render and simulation order

```text
Fixed step:
  scripted verification/input -> player simulation (commits player and advances world time once) ->
  ambient actors -> traffic actors

Render:
  ground + ceilings + walls + facade signs + park fixtures -> atmospheric wisps + micro-events ->
  vehicles + shared entity sprites (story NPCs, ambient actors, cat) + lamps + fireflies ->
  rain -> HUD/modals
```

Every finite world-space pass uses the existing perpendicular-depth convention and `writeIfNearer`. Rain remains last and sky-only. Equal-depth ties use a documented stable pass/ID order so repeated frames hash identically.

`PlayerSimulation` remains the only owner that advances `GameWorld.simulationTimeSeconds`; life-pass systems consume the same fixed delta but never advance the world clock themselves.

### Proposed source locations

```text
core/src/main/kotlin/com/asciicity/
  content/LifePassContent.kt
  entity/AmbientActorDefinition.kt
  entity/TrafficActorDefinition.kt
  render/AtmosphericRenderer.kt
  render/Storefront.kt
  render/StorefrontSampler.kt
  render/TrafficSignalRenderer.kt
  render/ParkFireflyRenderer.kt
  system/AmbientActorSystem.kt
  system/TrafficSystem.kt

core/src/test/kotlin/com/asciicity/{content,render,system}/
assets/maps/city-alpha.objects.json
DOCS/map-format.md
DOCS/map-authoring-guide.md
DOCS/rendering-contract.md
DOCS/verification-notes.md
```

Life-pass component names remain proposed. Entity art and projection must use the established `AsciiSpriteRegistry` and `EntitySpriteRenderer`; if canonical `NPC-01/02` establishes a more specific sprite-instance or actor type, this plan must use it rather than introducing a parallel abstraction.

## 6. Content contracts

`city-alpha.objects.json` gains four optional top-level arrays. All IDs share the existing map-wide stable-ID namespace with objects and facade signs.

### 6.1 Atmospheric emitters

```json
"atmosphericEmitters": [
  {
    "id": "steam.alley-market-01",
    "kind": "steam-grate",
    "cellX": 44,
    "cellZ": 18,
    "drift": "east",
    "phaseOffset": 0.25
  },
  {
    "id": "exhaust.archive-roof-01",
    "kind": "rooftop-exhaust",
    "cellX": 54,
    "cellZ": 22,
    "drift": "north",
    "phaseOffset": 0.70
  }
]
```

- `kind` is `steam-grate` or `rooftop-exhaust`.
- `cellX/cellZ` must be in bounds. A grate requires non-solid alley, sidewalk, plaza, or road ground; rooftop exhaust requires an opaque `building` cell and derives its base height from that cell.
- `drift` is cardinal; `phaseOffset` is finite in `[0,1)`.
- Per-kind speed, lifetime, spread, and maximum three-wisp count are engine tuning constants, not arbitrary authored particle graphs.

### 6.2 Ambient actors

```json
"ambientActors": [
  {
    "id": "ambient.vendor-market-01",
    "role": "vendor",
    "spriteId": "sprite.ambient.vendor",
    "x": 46.5,
    "z": 24.5,
    "facingDegrees": 90.0,
    "radius": 0.22
  },
  {
    "id": "ambient.walker-market-01",
    "role": "pedestrian",
    "spriteId": "sprite.ambient.umbrella",
    "x": 45.5,
    "z": 30.5,
    "facingDegrees": 0.0,
    "radius": 0.22,
    "motion": {
      "mode": "pace",
      "endX": 45.5,
      "endZ": 34.5,
      "speed": 0.45,
      "phaseOffset": 0.0
    }
  }
]
```

- `role` is `pedestrian`, `vendor`, `sitter`, `machine-user`, or `cat`; it selects validation/behavior only, not arbitrary AI.
- Positions and every swept motion sample require in-bounds non-solid ground plus radius clearance.
- `motion` is absent/static, `pace`, or `flee`. Pace/flee lanes are axis-aligned, no longer than 6 cells, and cannot cross water, buildings, fixtures, doors, story objects, spawn, or another authored lane.
- Citizen speeds are `0.25..0.65` cells/second. The cat uses the same bounded range and one `flee` lane.
- `flee` is valid only for the single `cat` role and additionally requires `triggerDistance` in `1.5..4.0` cells; other roles reject that field.
- Every `spriteId` must resolve through the Phase 7 sprite registry. Ambient actors have no `interactionId`.

### 6.3 Traffic actors

```json
"trafficActors": [
  {
    "id": "traffic.market-loop-01",
    "spriteId": "sprite.vehicle.compact",
    "radius": 0.34,
    "speed": 1.25,
    "phaseOffset": 0.0,
    "waypoints": [
      { "x": 48.5, "z": 8.5 },
      { "x": 48.5, "z": 40.5 },
      { "x": 50.5, "z": 40.5 },
      { "x": 50.5, "z": 8.5 }
    ]
  }
]
```

- Waypoint loops contain 2–16 axis-aligned segments and close from the final point to the first.
- Every segment must remain on `road` cells with radius clearance and must not include spawn, a story-object cell, or an archive-door approach.
- Speed is finite in `0.75..1.75` cells/second; radius is finite in `0.20..0.40`; phase is `[0,1)` along the total loop length.
- A traffic actor is blocking while visible. Its system uses swept substeps, stops before player/entity overlap, and never advances merely because it was culled.

### 6.4 Storefronts

```json
"storefronts": [
  {
    "id": "storefront.noodle-market-01",
    "kind": "menu-panel",
    "face": "west",
    "cellX": 52,
    "cellZ": 24,
    "widthCells": 1,
    "bottom": 0.15,
    "height": 2.2,
    "frames": [
      ["+NOODLE+", "| 24H  |", "+......+"],
      ["+NOODLE+", "| OPEN |", "+..o...+"]
    ],
    "distantGlyphRows": ["[NOODLE]", "[ OPEN ]"],
    "primaryPalette": "signCream",
    "accentPalette": "signRedOrange",
    "backgroundPalette": "signBackground",
    "animationRate": 1.0,
    "phaseOffset": 0.5
  }
]
```

- `kind` is `menu-panel`, `vending`, `arcade`, `shutter`, or `service-kiosk`.
- Placement follows facade-sign exterior face/run/roofline validation and cannot share any covered cell face with another storefront or facade sign.
- `frames` contains 1–4 rectangular printable-ASCII layouts, each 2–8 rows and 2–32 columns with identical dimensions. `distantGlyphRows` is rectangular and within the same bounds.
- Palette keys must exist. Animation rate is finite in `0..2` Hz; phase is `[0,1)`.
- Storefront frames are cosmetic wall samples and never create openings or collision.

### 6.5 Derived content

- `TrafficSignalRenderer` derives eligible signals from intersections where two road axes meet and a nearby sidewalk corner exists. Stable cell coordinates select phase; no new object is created.
- `ParkFireflyRenderer` derives a swarm only from authored lamps within outdoor grass/nature/waterfall habitat, subject to the four-swarm cap. Waterfall-adjacent lamps rank first, then ordinary park lamps, then other nature lamps. Each swarm contains three stateless finite-depth green glyphs; water-park fixtures do not qualify.

## 7. Lifecycle, collision, and failure handling

- Parse every life-pass array into immutable candidates, validate all cross-references/placements, then construct `GameWorld`. A single invalid entry aborts startup with its exact file and JSON path.
- Optional arrays default to empty; missing sprite prerequisites fail clearly when an actor references them rather than silently dropping the actor.
- Static blocking actors participate in the initial `SpatialEntityIndex`. Moving systems update index position only after collision accepts the authoritative step.
- Actor collision tests include footprint cells, other blocking entities, and the player. The player never becomes a regular world entity merely to support traffic.
- If an actor cannot move, it retains its previous position and waits. Traffic/citizens do not accumulate catch-up distance while blocked or paused.
- Cat state is `RESTING -> FLEEING -> HIDDEN`. The trigger is one-way per session; hiding removes entity-sprite visibility and spatial occupancy atomically and is idempotent.
- Pause/hide stops fixed-step actor advancement. Resume continues without a burst. Resize/culling changes only projection.
- Disposal owns no new native resource beyond Phase 7 sprite assets already held by `GameAssets`; renderers/systems retain primitive buffers and immutable content only.
- Content validation builds a conservative blocked-cell mask from static life-pass actor radii, then proves spawn and every canonical story target retains a four-directional reachable approach cell. The complete scripted story route remains the final authority for moving-lane cases.
- Runtime systems log one concise load summary and exceptional invariant failures only; they do not log every particle, actor wait, or frame.

## 8. Verification matrix

| Scenario | Expected result | Evidence required |
|---|---|---|
| Empty optional arrays | Current city loads/renders exactly as before | Existing snapshot hashes plus loader regression |
| Invalid emitter/storefront/actor/traffic entry | Startup fails before world construction with file/JSON path | Table-driven `ContentLoaderValidationTest` fixtures |
| Steam behind/in front of walls | Nearer wall wins; visible wisp uses finite depth/fog | `AtmosphericRendererTest` frame/depth fixtures |
| Storefront on exterior wall | Correct face orientation, LOD, fog, and occlusion | `StorefrontSamplerTest` samples and frame hash |
| Static citizen approach | Entity sprite and blocking radius share one entity position | Entity-sprite/collision integration fixture |
| Pacing citizen for 1,000 ticks | No wall clipping, overlap, swap, or frame-rate divergence | Deterministic trace/hash test |
| Player blocks a vehicle | Vehicle stops before overlap, waits, then resumes | `TrafficSystemTest` swept-collision trace |
| Vehicle off-screen | It follows the same authoritative trace as on-screen | Culling-independent system test |
| Traffic signal cycle | Stable cells have stable offset phases and bounded rate | `TrafficSignalRendererTest` time samples |
| Fireflies around park/nature lamps | At most four outdoor lamp swarms emit; waterfall lamps are included and trees/walls occlude | `ParkFireflyRendererTest` fixture |
| Cat encounter | Resting collision, one-way flee, atomic hide/removal | Actor-state and spatial-index integration test |
| Pause/resume/resize | No actor jump, duplicate state, stale collision, or projection error | Desktop verification route and focused lifecycle tests |
| Full Last Packet route | Story stages, doors, prompts, dialogue, and objective remain unchanged | Canonical end-to-end route with life pass enabled |
| Maximum density across responsive sizes | Readable landmarks/HUD, bounded writes/allocations, target frame budget | 960×540, 1280×720, and required 3440-wide work-area captures plus renderer profile |
| Ten-minute soak | No unbounded heap growth, drift, deadlock, or error logs | Recorded soak evidence in `verification-notes.md` |

## 9. Phased checklist

### Phase 0 — Prerequisites and data lock

- [ ] **LPASS-01:** After canonical `VERIFY-07` passes, record baseline hashes, screenshots, renderer statistics, story-route checkpoints, and Last Packet verification evidence before adding life-pass content. **Status:** Baseline capture/reconciliation is recorded in `DOCS/verification-notes.md`, but this gate cannot complete: `TRAVEL-01`, `STORY-01` through `STORY-03`, `DOOR-01`, and canonical `VERIFY-07` are still open in the canonical plan. The original “before adding life-pass content” condition is superseded by the already implemented first-pass subset above; do not present its life features as a verified story-route baseline. **Files:** `DOCS/verification-notes.md`.
- [x] **LPASS-02:** Reconcile the implemented optional object arrays and strict parsing/validation contracts. **Files:** `LoadedContent.kt`, `ContentLoader.kt`, `DOCS/map-format.md`, `DOCS/map-authoring-guide.md`, tests. **Evidence:** optional `facadeSigns`, `facadeEntrances`, `trafficCars`, `atmosphericEmitters`, `ambientEventEmitters`, and `ambientActors` load as empty or populated content; unknown fields, ranges, duplicate IDs, invalid placements, missing sprites, illegal lanes, and unknown palette keys fail with exact paths.
- [x] **LPASS-03:** Reuse verified canonical `SPRITE-01/02` and `NPC-01/02`, and document the shared sprite-instance/blocking-actor API consumed by ambient actors. **Files:** `AsciiSpriteRegistry`, `EntitySpriteRenderer`, `GameWorld`, `CollisionSystem`, `NpcSystem`, `AmbientActorSystem`, tests. **Evidence:** `AmbientActorSystem` resolves its authoritatively positioned `AMBIENT_ACTOR` from `GameWorld`, uses `CollisionSystem` for movement clearance, and passes the validated registry sprite to the existing `EntitySpriteRenderer`; no life-pass projector or occupancy store exists. The canonical Rin checks retain near/far/edge/behind-wall, culling, resize, and collision coverage.

### Phase 1 — Steam and living storefronts

- [x] **STEAM-01:** Implement immutable emitter definitions and stateless bounded wisp sampling for grate steam and rooftop exhaust. **Files:** `AtmosphericEmitter.kt`, `AtmosphericRenderer.kt`, loader/tests. **Evidence:** immutable `AtmosphericEmitter` definitions validate bounds/ranges; `AtmosphericRenderer` samples stable-ID/time phases with exactly three wisps per emitter and never mutates `WorldGrid`; `ContentLoader` derives rooftop base height from the opaque building host; deterministic/depth tests pass.
- [x] **STEAM-02:** Integrate finite-depth projection/fog/occlusion and author up to eight well-spaced `city-alpha` emitters. **Files:** `WorldAsciiRenderer.kt`, object data, render/content tests. **Evidence:** five authored emitters validate under the cap; finite `writeIfNearer` writes let nearer geometry win, while later entity/HUD/rain passes retain their own precedence. Reviewed 1280×720 and 3440×1440 captures, focused renderer tests, and `clean check` pass.
- [ ] **STORE-01:** Implement strict storefront frames, placement validation, wall-face indexing, LOD, and local animation. **Files:** `Storefront.kt`, `StorefrontSampler.kt`, `ContentLoader.kt`, tests. **Done when:** valid frames sample predictably and ragged/non-ASCII/overlapping/off-wall/too-fast content fails precisely.
- [ ] **STORE-02:** Author 5–8 distinct storefronts across the market, alley, and archive approaches using restrained warm/acid accents. **Files:** `city-alpha.objects.json`, palette only if an approved non-cyan/magenta key is genuinely missing. **Done when:** each storefront reads at close range, at least three remain identifiable at distance, and no facade sign, obstacle, or story landmark loses readability.
- [ ] **VERIFY-LP1:** Run focused tests, content validation, clean check, two-resolution screenshots, and visual review for steam/storefront density. **Done when:** the intended motion is visible but the dark neutral street plane and silhouette contrast remain dominant.

### Phase 2 — Low-risk derived activity

- [ ] **ROADLIFE-01:** Derive bounded traffic signals from road topology with stable offset phases and depth-tested glyphs. **Files:** `TrafficSignalRenderer.kt`, renderer integration, tests. **Done when:** only eligible intersections receive signals, signal rate stays restrained, and nearer world geometry occludes them.
- [x] **WILDLIFE-01:** Derive up to four deterministic three-firefly swarms from outdoor park/nature lamps and render them with shared depth/fog rules. **Files:** `ParkFireflyRenderer.kt`, `WorldAsciiRenderer.kt`, `WorldRenderStats.kt`, tests. **Evidence:** stable lamp-ID ranking caps each map at four swarms, prioritizes the city and reserve cascade lamps, and requires nearby grass/nature habitat so water-park lamps never qualify. Three green stateless glyphs per swarm use finite `writeIfNearer` depth and yield to nearer geometry; focused city/nature/determinism/occlusion tests pass.
- [ ] **VERIFY-LP2:** Observe every derived signal cluster and eligible firefly swarm from multiple sides at both window sizes. **Done when:** the effects are deterministic and depth-correct, remain visually restrained, and introduce no collision or world mutation.

### Phase 3 — Static street population

- [ ] **CROWD-01:** Author validated one-frame sprites for umbrella pedestrian, vendor, sitter, machine user, and generic pedestrian through the Phase 7 sprite registry. **Files:** sprite data/assets, sprite tests. **Done when:** every silhouette has transparent space, close/distant readability, approved palette use, and no ragged/unknown glyph/color data.
- [ ] **CROWD-02:** Add noninteractive ambient world entities and render them through the shared entity-sprite layer in stable ID order. **Depends:** LPASS-03. **Files:** `WorldEntityType`, `AmbientActorDefinition.kt`, loader, `GameWorld`, entity-sprite integration, collision tests. **Done when:** 6–8 static citizens render/collide at the same position, remain stable while culled/resized, and never show interaction prompts.
- [ ] **VERIFY-LP3:** Walk past every static citizen, pressure collision, pause/resume, turn away/reapproach, resize, and complete the canonical interaction route. **Done when:** render/collision/index state always agrees, no placement obstructs a story target, and the population feels occupied rather than crowded.

### Phase 4 — Bounded moving actors and traffic

- [ ] **CROWD-03:** Implement at most two short deterministic pacing lanes with fixed-step movement and wait-on-block behavior. **Depends:** CROWD-02. **Files:** `AmbientActorSystem.kt`, `GameScreen.kt`, tests. **Done when:** 1,000-tick traces are repeatable across frame sequences and actors never enter solids, overlap/swap, or block a mandatory story choke point.
- [ ] **WILDLIFE-02:** Author one cat sprite and one safe park-edge flee lane; implement `RESTING -> FLEEING -> HIDDEN` through `AmbientActorSystem`. **Depends:** WILDLIFE-01, CROWD-03. **Files:** sprite/object data, actor system, spatial-index tests. **Done when:** the cat blocks only while visible, flees once per session, hides atomically, cannot enter water/fixtures, and resets on relaunch.
- [ ] **TRAFFIC-01:** Author up to two compact vehicle sprites and validated nonintersecting road-only waypoint loops. **Depends:** ROADLIFE-01, CROWD-02. **Files:** sprite data, `TrafficActorDefinition.kt`, object data, content tests. **Done when:** every lane segment is axis-aligned/road-only/clear of protected story cells and invalid loops fail before world construction.
- [ ] **TRAFFIC-02:** Implement stable fixed-step traffic motion, interpolation, swept entity/player clearance, and wait/resume behavior as the final life-pass behavior packet. **Depends:** TRAFFIC-01, CROWD-03, WILDLIFE-02. **Files:** `TrafficSystem.kt`, `GameScreen.kt`, collision/index tests. **Done when:** vehicles never push/damage/overlap the player, never tunnel, run identically off-screen, and cannot deadlock the required story route.
- [ ] **VERIFY-LP4:** Block each pacer and vehicle, trigger and obstruct the cat flee lane, cross each vehicle loop, leave/revisit, pause/resume, and relaunch at both window sizes. **Done when:** moving citizens, wildlife, and traffic remain safe, rare, deterministic, and mechanically consistent while the road material remains visually neutral.

### Phase 5 — Integration, performance, and approval

- [ ] **LPASS-VERIFY-01:** Add one deterministic life-pass route covering park, market, alley, road crossing, archive approach, pause, music toggle, resize, and completion checkpoints. **Files:** proposed `LifePassVerificationScript.kt`, script tests, desktop launcher routing. **Done when:** the route exits automatically with expected actor-state hashes and no errors.
- [ ] **LPASS-VERIFY-02:** Extend render statistics for life-pass candidate/write counts without per-frame allocations and profile maximum authored density for at least 3,600 warmed frames. **Files:** `WorldRenderStats.kt`, renderer tests, verification notes. **Done when:** logical-grid bounds remain unchanged, allocations are bounded, and the recorded 60-FPS budget is met or density is reduced.
- [ ] **LPASS-VERIFY-03:** Run `clean check`, `:core:validateContent`, full Last Packet verification, both resolution captures, and a ten-minute soak. **Files:** `DOCS/verification-notes.md`, retained approved captures. **Done when:** story/render/collision/lifecycle checks pass and visual review approves separation, motion density, palette restraint, and landmark clarity.
- [ ] **LPASS-DOC-01:** Reconcile this plan, the canonical plan, map/render contracts, README, controls if changed, and verification evidence. **Done when:** only implemented-and-proven items are checked and no document advertises unverified behavior.

## 10. Open tuning points

- Exact emitter, actor, storefront, traffic-loop, and wildlife coordinates remain an authoring decision after baseline first-person captures identify readable sightlines and safe route clearance.
- Citizen/vehicle sprite dimensions and distance LOD thresholds inherit the proven Phase 7 entity-sprite contract; tune only against the verified `SPRITE-02` evidence and canonical `NPC-02` integration.
- Vehicle speed/stop clearance, citizen pacing speed, cat trigger distance, steam drift, and firefly radius stay within the locked bounds but require desktop playtest approval.
- If maximum-density captures become noisy, reduce authored counts before adding configuration, new palette keys, or renderer complexity.
