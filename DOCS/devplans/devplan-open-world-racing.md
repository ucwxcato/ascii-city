# ASCII City Open-World Street Racing — Development Plan

> **Status:** Planned. The current worktree has an authored, test-covered F3 debug sedan with collision-backed WASD driving and a third-person follow view. It is a prototype, not the shipped vehicle/race loop, and has not had a manual in-car gameplay capture.
>
> **Purpose:** Make ASCII City an open-world neon street-racing game where players walk through the city, enter a car, drive to meetups, and run replayable checkpoint races.
>
> **Authority:** This plan owns drivable vehicles, free-roam driving, race-event data, checkpoint/gate presentation, and racing HUD/control behavior. It supersedes the city-life plan's drivable-vehicle exclusion. The canonical plan remains owner of Last Packet dialogue, doors, map travel, and story progress; map/content and projection contracts remain [map-format.md](../map-format.md) and [rendering-contract.md](../rendering-contract.md).

## 0. Outcome

The player can remain on foot, walk to an authored parked compact coupe, enter it, freely drive city streets in a third-person view, then start a glowing meetup race by driving to its gate. A race clears ordered road checkpoints, records a time, and returns to free roam without a loading screen.

```text
Launch on foot -> walk to compact coupe -> [E] enter -> third-person car view ->
drive to Neon Run gate -> [E] start -> ordered checkpoints -> finish/time ->
free roam -> [E] exit at a safe roadside position
```

The MVP is one `city-alpha` solo time trial. It is the foundation for future events, not a debug mode.

## 1. Locked decisions

- Walk/drive is the core loop. On-foot exploration remains available and existing story interaction stays on foot.
- The first player-facing/test car is the authored `vehicle.player.compact-coupe`, entered/exited with `E`; it is never spawned from F3. Its visual brief is a legally distinct, non-branded early-2000s compact coupe/hatch with the user-requested Focus-coupe energy: short overhangs, an upright cabin, broad practical stance, tapered lamps, and small wheels. It must not copy a Ford badge, name, grille, exact panels, or production-car silhouette. The current F3 `V` sedan prototype remains only until the full loop passes verification, then is removed or reduced to a test hook.
- Player cars are detailed, authored pixel-art vehicle assets with directional frames, not enlarged ASCII glyph billboards. They use nearest-neighbor sampling, the existing restrained neon/neutral palette language, and a dedicated cell-depth-clipped presentation path so they feel intentionally premium inside the ASCII world.
- Every player vehicle is a believable road-worn civilian "beater," not a supercar: modest top speed, visible wear/mismatched panels, practical proportions, and a weighty street-handling character. Cars may be recent rather than vintage, but must never read as luxury, exotic, pristine, or race-built machinery.
- The first compact coupe has a front-wheel-drive handling character: its normal acceleration pulls from the front, while handbrake drifting rotates the rear at modest speed. It must feel agile and scrappy rather than high-power or rear-drive exotic.
- While driving, `W` throttles forward, `S` brakes/reverses, and `A`/`D` steer. Camera is a collision-bounded third-person follow pose; no overhead camera or car interior.
- `Space` is the handbrake. At sufficient speed while steering, it intentionally reduces rear lateral grip and enables a controllable drift; releasing it restores normal grip over bounded fixed-step recovery. A drift is a physical velocity/heading relationship, never an animation toggle.
- Vehicle state advances only in the existing fixed step and collides via `CollisionSystem` against semantic cells and blocking world entities. Rendered glyphs/gates/colors never define collision.
- A parked vehicle is a blocking `WorldEntity`. A driven vehicle occupies the authoritative player simulation position/radius and is rendered from that same state, never as a duplicate colliding entity.
- Exiting requires a deterministic safe on-foot candidate beside/behind the car. If none is clear, refuse without changing state.
- First event is a solo, replayable, no-load-screen `city-alpha` road time trial. Ordered checkpoints are mandatory. No damage, police, economy, upgrades, cars, opponents, or persistence in MVP.
- Race gates start only while driving. Checkpoint detection is automatic and semantic; Escape abandons an active race only. Pause freezes fixed-step race time.
- Vehicles/races are map-authored and fail validation before world construction. No generic scripting language, ECS, or new renderer stack.
- Racing is not optional. It may coexist with incomplete Last Packet work but cannot block story paths, mutate story state, or make story targets inaccessible.
- In MVP a driven car cannot use map-travel doors: show `EXIT VEHICLE FIRST`. Cars do not cross maps. Session state resets on relaunch.

## 2. Goals and non-goals

### Goals

- Smooth walk -> enter -> drive -> exit free roam.
- Deterministic vehicle collision, third-person view, input routing, and lifecycle behavior.
- One readable race gate, ordered checkpoint route, finish/time result, and replay loop.
- Strict data validation for vehicle placement and road-only routes.
- Clear extension seams for multiple events and later rivals, without implementing them now.

### MVP boundary

The first release packet deliberately proves one car, handling/drift/smoke, and one city-alpha time trial before it adds a career. The following are **planned later scope**, not rejected features: AI rivals, active road traffic, collisions/damage, police/heat, rubber-banding policy, gearboxes, interiors, upgrades, garages, currency, shops, betting, saved career state, leaderboards, additional-map/cross-map events, procedural event selection, and multiplayer.

### Explicitly not part of the product direction

- Replacing the CPU ASCII world, fixed authored maps, lightweight Kotlin model, Scene2D UI, or approved bounded compositor with a generic 3D/engine stack.
- Letting racing bypass or silently mutate Last Packet story progress, dialogue, door ownership, or canonical completion rules. A future story/racing crossover must be an explicit planned integration packet.

### Long-term racing roadmap

| Expansion | Player-facing outcome | Dependency boundary |
|---|---|---|
| Street events and rivals | Multiple event types, rival beaters, traffic-aware racing, and a deliberate rubber-banding policy. | Proven solo race state, safe vehicle/traffic collision, and visual budget. |
| Career and garage | Earn money from events, buy/keep beater cars, tune performance/cosmetics, use garages/shops, and retain a career save. | Authoritative progression/economy/save contract; no client-trust shortcuts. |
| Heat and consequences | Police attention, pursuits, damage/repair, and meaningful event risk/reward. | Robust AI, damage, failure/recovery, and safe player-state persistence. |
| World expansion | Events on additional maps, cross-map travel rules for vehicles, curated procedural event rotation, and richer interiors. | Map-travel vehicle persistence plus route/content validation. |
| Social competition | Local/online leaderboards, ghosts, then multiplayer if the deterministic/network contract is explicitly designed. | Replay integrity, anti-cheat, account/service, and network authority plan. |

These are all in scope for ASCII City's racing direction; phase ordering is a quality and safety boundary, not a product veto.

## 3. Player and interaction flow

### On foot

1. Existing WASD walk/strafe and arrow look controls remain unchanged.
2. Targeting the parked compact coupe shows `[E] ENTER COUPE`.
3. Enter validates the car-radius footprint while ignoring only the target parked entity; failure leaves both player and parked entity unchanged.

### Driving

1. Enter atomically removes the parked entity, moves the authoritative player state to its pose, applies vehicle radius, and switches the render pose to third person.
2. Footsteps/head bob stop; rain, fog, normal resizing, and audio ownership remain unchanged.
3. HUD reads `W/S DRIVE | A/D STEER | SPACE DRIFT | E EXIT`; `E` exits only at a valid roadside position and recreates exactly one parked entity.
4. NPC, terminal, and door actions do not fire from a moving car. Door use shows `EXIT VEHICLE FIRST`.
5. A drift starts only when rear traction is deliberately broken by speed, steering, and held handbrake. A short neutral tire-smoke trail appears from the rear wheels only while the car is actually sliding; ordinary turns do not emit smoke.

### Race

1. A nonblocking race gate has an interaction target and stable `raceId`.
2. On foot it explains `VEHICLE REQUIRED`; in the car it offers `[E] START NEON RUN`.
3. Active race HUD shows race name, fixed-step timer, and only the next gate bearing. The next finite-depth marker is the only bright checkpoint marker.
4. Checkpoints trigger when the vehicle's semantic center/radius enters the next declared checkpoint radius. Skipped/out-of-order gates do nothing.
5. Finish shows `NEON RUN COMPLETE — MM:SS.mmm`, returns to driving free roam, and permits immediate replay. Escape aborts and removes all active race markers/timer.

## 4. Architecture and ownership

| Owner | Responsibility | Never owns |
|---|---|---|
| `racing/VehicleDefinition.kt` *(proposed)* | Authored beater identity, art set, parked pose, radius, and bounded handling values. | Mutable runtime state/rendering. |
| `racing/VehicleDynamicsState.kt` *(proposed)* | Fixed-step velocity vector, heading, drift/slip state, and last safe collision response. | Content data, UI, particle ownership. |
| `racing/RaceDefinition.kt` *(proposed)* | Race ID/name, starter gate entity, ordered checkpoint positions/radii. | Progress/rewards/render output. |
| `ContentLoader` / `LoadedContent` | Parse vehicle/race records and create linked parked vehicle/race gate entities. | Runtime movement. |
| `GameWorld` / `WorldEntity` | Parked-car/gate entity authority, spatial index, atomic entity mutations. | Driven duplicate, checkpoint state. |
| `VehicleSystem` *(proposed; refactors `DebugVehicleSystem`)* | Fixed-step entry/exit, acceleration/braking, steering, handbrake drift, collision response, and follow pose. | Race rules/story/UI. |
| `PlayerSimulation` | Select on-foot/vehicle update, interpolation, one player-world update per step. | Raw keyboard reads/F3 product logic. |
| `RaceSession` / `RaceSystem` *(proposed)* | `IDLE`, `ACTIVE`, `FINISHED`, `ABANDONED`; elapsed time, order, start/abort/finish. | Collision/saving/rendering. |
| `InteractionSystem` / `InteractionOutcome` | Typed vehicle/gate targeting and outcome routing. | Dialogue-only assumptions or direct race mutation. |
| `VehicleVisualRenderer` / `RaceGateRenderer` *(proposed)* | Depth-clipped, nearest-neighbor directional pixel-art car frames and finite-depth ASCII gates in the existing presentation path. | Collision/state, a general effect chain, 3D models. |
| `TireSmokeRenderer` *(proposed)* | Render-only bounded neutral smoke sampled from fixed-step skid emissions. | Vehicle physics, collision, a general particle system. |
| `GameScreen` | Fixed-step order, HUD, outcome routing, audio/head-bob mode, lifecycle cleanup. | Parsing/persistence. |

`TrafficSystem` stays cosmetic. Do not repurpose its time-sampled ASCII traffic paths for the player car or future opponents.

### Handling model

`VehicleSystem` uses a local-space velocity model, not direct position rotation. Each fixed step resolves throttle/brake acceleration, rolling drag, speed-limited steering, then lateral grip before asking collision to sweep the proposed movement. The resulting world velocity is the authoritative source for car motion, slip, and race checkpoint crossing.

- Longitudinal velocity is limited by the authored beater top speed; reverse is substantially slower. Releasing `W`/`S` coasts down through rolling resistance instead of stopping instantly.
- Steering is strongest at city speed and deliberately reduces at the top of the beater's range. The car turns from its current velocity/heading; it cannot pivot in place or gain speed from turning.
- Normal lateral grip rapidly settles a small slide. Holding `Space` applies handbrake braking and lower rear lateral grip only when moving and steering; the visual/race state becomes `DRIFTING` only after a minimum signed slip angle is reached.
- On wall/entity contact, the collision result removes the blocked velocity component, retains only a small tangent component, and applies a bounded speed loss. There is no damage, spinout lottery, push-through, or collision-derived boost.
- The first handling profile must feel lively at normal city-road speeds but cannot exceed the map's readable first/third-person pace. Exact acceleration, grip, turn, and top-speed values remain data tuning, bounded by validation and deterministic tests.

## 5. Data, lifecycle, and failures

### Proposed object-file contract

Document this in `DOCS/map-format.md` before authoring content:

```json
{
  "drivableVehicles": [
    {
      "id": "vehicle.player.compact-coupe",
      "spriteId": "vehicle.art.compact-coupe",
      "x": 28.5, "z": 14.5, "yawDegrees": 0,
      "radius": 0.42,
      "class": "beater",
      "driveLayout": "frontWheelDrive",
      "handling": {
        "topSpeedCellsPerSecond": 8.5,
        "reverseSpeedCellsPerSecond": 2.75,
        "accelerationCellsPerSecondSquared": 5.0,
        "brakeCellsPerSecondSquared": 8.0,
        "turnDegreesPerSecond": 105.0,
        "normalLateralGrip": 8.0,
        "handbrakeLateralGrip": 1.8,
        "driftMinimumSpeed": 3.5
      },
      "displayText": "COMPACT COUPE"
    }
  ],
  "races": [
    {
      "id": "race.neon-run",
      "displayName": "NEON RUN",
      "startGateEntityId": "race-gate.neon-run",
      "checkpointRadius": 1.35,
      "checkpoints": [{ "x": 31.5, "z": 14.5 }, { "x": 42.5, "z": 14.5 }, { "x": 42.5, "z": 28.5 }]
    }
  ],
  "objects": [{
    "id": "race-gate.neon-run", "type": "raceGate",
    "x": 28.5, "z": 14.5, "raceId": "race.neon-run", "displayText": "NEON RUN"
  }]
}
```

Validation must require stable unique IDs/references, existing directional pixel-art asset sets, `class: "beater"`, a supported explicit drive layout (the first is `frontWheelDrive`), finite bounded handling values, in-bounds non-solid vehicle/gate centers, full parked-car clearance, existing `raceGate` starter ID, at least three non-duplicate ordered checkpoints, road-tagged checkpoint cells, and four-direction road connectivity between starter/checkpoints. Authoring must explicitly reject overlap with protected story door, terminal, or arrival footprints.

### Runtime state

```text
Vehicle: ON_FOOT -> ENTERING -> DRIVING -> EXITING -> ON_FOOT
Race:    IDLE -> ACTIVE(nextCheckpoint, elapsed) -> FINISHED -> IDLE
                         \-> ABANDONED -----------------------> IDLE
Handling: GRIP -> HANDBRAKE -> DRIFTING -> GRIP
```

- Transitions run at fixed-step boundaries or `GameScreen` command handling before the next step; renderer/HUD never mutate gameplay.
- `RaceSession` stores active race ID, next index, and fixed-step seconds. A finished time is display-only and session-only.
- `VehicleDynamicsState` is reset on vehicle entry, screen replacement, and safe exit. Its velocity, heading, drift state, and emission sequence are interpolated/read-only for presentation; a renderer may never invent a drift or modify traction.
- Tire smoke uses a small fixed-capacity emission ring generated only by confirmed drifting fixed steps. Each emission stores position, heading, age seed, and intensity; the renderer samples it deterministically for less than one second, writes only finite-depth smoke glyphs, and never creates collision or a general particle list.
- Failure (invalid entry/exit/start, duplicate E, skipped checkpoint, resize/pause, F3 map switch, screen disposal, content failure) preserves or cleans state deterministically. Map/screen replacement aborts a race and starts the replacement map on foot.

## 6. UI, rendering, and product constraints

- Add typed vehicle/race interaction outcomes; do not overload dialogue IDs or bypass `InputController`.
- Extend `HudModel` with travel mode and optional race state. Story objective/signal values remain separate from racing state.
- Gates are bounded finite-depth glyphs with restrained existing-neon accents. They do not brighten broad roads, change compositor behavior, or write UI.
- `VehicleVisualRenderer` owns only vehicle art: a small directional-frame asset set (rear, front, left, right, then diagonals only if visual review needs them), fixed logical-world scale, nearest filtering, and explicit clipping against the completed finite ASCII depth field. It must not be a generic textured-world layer, a shader stack, a 3D model pipeline, or a way to draw through walls. Its low-resolution pixel art remains crisp at the existing whole-number glyph scale, uses no anti-aliasing, and is visually tuned against the dark neutral road rather than becoming a bright full-screen focal block.
- Beater art uses original/re-distributable pixel assets and restrained physical detail: dented/misaligned body panels, faded paint, grime, taped/cracked lamps, worn tires, and small practical aftermarket touches. The first compact coupe must emphasize its distinct short-overhang/upright-cabin/broad-stance brief rather than reproduce a particular Ford Focus coupe. No real manufacturer marks, copied vehicle designs, glossy showroom finish, or excessive underglow. The car remains readable against the dark road without becoming a saturated neon billboard.
- `TireSmokeRenderer` is a dedicated, bounded effect rather than a general particle engine: smoke is desaturated grey/fog-tone, sparse, short-lived, rear-wheel anchored, depth-clipped, and emitted only by actual drift state. It never covers the reticle/HUD, recolors the road, or flashes the frame.
- Third-person camera shortens toward the car if its preferred 2.6-cell rear offset is blocked; it never leaves map bounds or exposes through-wall geometry.
- No engine/skid/start/finish audio in MVP. Existing music/rain/waterfall/footstep ownership stays intact.
- Collision remains semantic. The car cannot push/damage/remove story entities. It blocks/slides only.
- Fine default performance, 1280x720 logical minimum, Standard fallback, whole-number glyph scaling, and all renderer boundaries remain intact.

## 7. Verification matrix

| Scenario | Expected result | Required proof |
|---|---|---|
| Enter compact coupe | One parked entity becomes one driven state; radius/camera/directional pixel-art car agree. | System/index tests + 1280x720 capture. |
| Blocked entry/exit | Refusal changes no state or entity count. | Collision trace and runtime message test. |
| Driving trace | Deterministic W/S/A/D behavior, no wall/entity tunneling. | Fixed-step hash across frame chunkings. |
| Beater limits | Every authored player vehicle validates as `beater`; top speed/reverse/acceleration remain inside established modest ranges. | Content validation fixtures. |
| Grip and handbrake | Normal steering settles lateral velocity; Space plus speed/steering produces a bounded drift; release recovers deterministically. | Vehicle-dynamics traces and frame-chunking hash tests. |
| Collision while sliding | Contact removes blocked velocity and costs speed without tunnelling, damage, or a random spin. | Swept-collision velocity tests. |
| Tire smoke | Only confirmed rear-wheel drift emits capped, finite-depth neutral smoke; ordinary turns/collisions emit none. | Emission/renderer tests and rear/side/occlusion capture. |
| Follow camera by wall | In bounds; shortens safely; no through-wall render. | Camera test + manual close-wall capture. |
| Start on foot | Vehicle-required feedback; race stays idle. | Interaction/race test. |
| Race happy path | Ordered checkpoints advance once; finish gives stable time/replay. | Race trace + start/checkpoint/finish desktop captures. |
| Skip a checkpoint | Cannot advance/finish; HUD retains required target. | Race-system test. |
| Escape/pause/resize/reload | Clean abort/freeze; no catch-up/duplicate markers/state leak. | Lifecycle tests + smoke route. |
| Invalid content | Exact file/path errors for IDs, sprites, placement, radius, non-road/disconnected route. | Loader fixtures + `:core:validateContent`. |
| Story regression | Existing targets remain reachable; driven map door is refused. | Interaction/walk/vehicle-door test. |
| Vehicle visual quality | Directional pixel-art compact coupe stays crisp, recognizably detailed, palette-coherent, legally distinct from its Focus-coupe reference, and depth-clipped rather than reading as an ASCII billboard. | Approved rear/side/turn/occlusion captures at 1280x720 and ultrawide. |
| Presentation | Gates/compact coupe depth-correct; dark road/HUD readability survive Fine + Standard. | 1280x720/ultrawide review, smoke stats, `clean check`. |

## 8. Phased checklist

### Phase 0 — Contract and route lock

- [x] Add/update this plan, remove life-pass exclusion, and cross-link the canonical plan.
- [ ] Add `drivableVehicles`, `races`, and `raceGate` format/validation rules.
- [ ] Select one story-safe city-alpha parking bay and a 3+ checkpoint road-only Neon Run after first/third-person sightline review.
- [ ] **Verify:** Route has no required story overlap and schema is approved before loader work.

### Phase 1 — Production free-roam vehicle

- [ ] Refactor F3-only `DebugVehicleSystem` into data-backed `VehicleSystem`; add fixed-step local velocity, beater acceleration/brake/drag/steering, handbrake drift, collision velocity response, and no hard-coded spawn/sprite ownership.
- [ ] Define/import `vehicle.art.compact-coupe`: one redistributable, authored directional pixel-art front-wheel-drive beater compact-coupe asset set. Validate frame dimensions, legal distinctness from its Focus-coupe inspiration, worn-detail/palette treatment, logical scale, nearest filtering, `class: "beater"`, and `driveLayout: "frontWheelDrive"`. Do not generate the final asset from a generic text glyph layout.
- [ ] Add a narrowly scoped `VehicleVisualRenderer` that composites the car frame into the existing FBO/presentation route only where the resolved ASCII depth permits it; add typed parked vehicle entities, atomic entry/exit, vehicle input mode, door refusal, and third-person camera.
- [ ] Add a fixed-capacity, drift-only `TireSmokeRenderer` and smoke emission tests; do not introduce a generic particle system.
- [ ] Add collision/control/drift/smoke/camera/entity/lifecycle tests.
- [ ] **Verify:** Focused tests, content validation, `clean check`, and visually approved 1280x720 on-foot/enter/rear/side-turn/controlled-drift/smoke/exit/close-wall captures. Verify that the car reads as detailed pixel art rather than an ASCII billboard at both Fine and Standard.

### Phase 2 — Race data and deterministic state

- [ ] Add race definitions, parser/validator/fixtures, race gates, session/state machine, and ordered checkpoint tests.
- [ ] Author `vehicle.player.compact-coupe`, `race-gate.neon-run`, and `race.neon-run` only after the schema validates.
- [ ] **Verify:** Exact-path invalid-content tests, deterministic checkpoint/skip/abort/pause/reload tests, content validation, and `clean check`.

### Phase 3 — Player-facing race integration

- [ ] Add bounded gate/checkpoint rendering and mode-aware HUD/timer/bearing/result feedback.
- [ ] Update README, run commands, map format, controls, and verification notes after evidence; remove/demote F3 V after parity.
- [ ] **Verify:** Visual review at 1280x720 and ultrawide for gate/start/checkpoint/finish/wall scenes; update hashes only after approval; smoke test and `clean check`.

### Phase 4 — Street-racing field expansion

- [ ] Add multiple city-alpha event types and one deterministic rival beater using the proven race pipeline; define and test its rubber-banding policy rather than hiding it in tuning.
- [ ] Add bounded semantic traffic/vehicle collision behavior for races, with no player/entity tunnelling or renderer-owned occupancy.
- [ ] **Verify:** Rival/traffic deterministic traces, collision/failure recovery tests, story-route regression, and repeated desktop race captures.

### Phase 5 — Career, garage, and beater progression

- [ ] Create a separate authoritative career/economy/save contract covering event rewards, money, owned beaters, garages, shops, upgrades, cosmetic wear, repair, and safe save/load migrations.
- [ ] Add additional beater classes only after the garage contract validates every asset and handling profile against the no-supercar rule.
- [ ] **Verify:** Economy transaction/integrity tests, save/reload/migration coverage, garage/shop runtime flows, and complete regression of free-roam and races.

### Phase 6 — Heat, damage, and wider city competition

- [ ] Add damage/repair, police heat/pursuits, event betting/risk, gearbox/manual-driving support, and vehicle interiors through focused subplans sharing one player-vehicle authority.
- [ ] Add vehicle-safe cross-map travel, additional-map races, curated procedural event rotation, and explicit story/racing crossover content only after the travel/session contract is extended.
- [ ] **Verify:** Failure/recovery, pursuit, damage, door/map-transfer, event-generation, and full story/career smoke evidence.

### Phase 7 — Competitive services

- [ ] Design replay/ghost integrity, leaderboards, and multiplayer as server-authoritative/networked features; do not ship any claimed competitive result from untrusted local state.
- [ ] **Verify:** Determinism/replay validation, anti-cheat/service failure cases, disconnected-session recovery, and multiplayer soak evidence.

## 9. Open tuning points

- The first compact coupe's front-wheel-drive layout is locked; its example 8.5-cell/sec top speed, 2.75-cell/sec reverse, 5.0 acceleration, 8.0 braking, 105-degree steering, grip values, 3.5 drift threshold, radius `0.42`, and current follow target (2.6 cells rear, 1.35 height) are starting points. Tune only through fixed-step handling traces plus controlled-drift, smoke, and collision capture evidence; remain inside validated beater limits.
- Exact parking/gate/checkpoint coordinates are TBD pending overlap and sightline audit; city-alpha-only, road-connected, three-plus ordered checkpoints are locked.
- Career, rivals, heat, damage, wider-map events, and competitive services are all planned in Phases 4–7. Each still needs its focused implementation contract before work begins because it introduces AI, economy, adversarial behavior, persistence, or network authority.
