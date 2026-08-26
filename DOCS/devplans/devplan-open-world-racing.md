# ASCII City Open-World Street Racing — Development Plan

> **Status:** Planned. The current worktree has an authored, test-covered F3 debug sedan with collision-backed WASD driving and a third-person follow view. It is a prototype, not the shipped vehicle/race loop, and has not had a manual in-car gameplay capture.
>
> **Purpose:** Make ASCII City an open-world neon street-racing game where players walk through the city, enter a car, drive to meetups, and run replayable checkpoint races.
>
> **Authority:** This plan owns drivable vehicles, free-roam driving, race-event data, checkpoint/gate presentation, and racing HUD/control behavior. It supersedes the city-life plan's drivable-vehicle exclusion. The canonical plan remains owner of Last Packet dialogue, doors, map travel, and story progress; map/content and projection contracts remain [map-format.md](../map-format.md) and [rendering-contract.md](../rendering-contract.md).

## 0. Outcome

The player can remain on foot, walk to an authored parked sedan, enter it, freely drive city streets in a third-person view, then start a glowing meetup race by driving to its gate. A race clears ordered road checkpoints, records a time, and returns to free roam without a loading screen.

```text
Launch on foot -> walk to sedan -> [E] enter -> third-person car view ->
drive to Neon Run gate -> [E] start -> ordered checkpoints -> finish/time ->
free roam -> [E] exit at a safe roadside position
```

The MVP is one `city-alpha` solo time trial. It is the foundation for future events, not a debug mode.

## 1. Locked decisions

- Walk/drive is the core loop. On-foot exploration remains available and existing story interaction stays on foot.
- The player-facing sedan is an authored `vehicle.player.sedan`, entered/exited with `E`; it is never spawned from F3. The current F3 `V` prototype remains only until the full loop passes verification, then is removed or reduced to a test hook.
- Player cars are detailed, authored pixel-art vehicle assets with directional frames, not enlarged ASCII glyph billboards. They use nearest-neighbor sampling, the existing restrained neon/neutral palette language, and a dedicated cell-depth-clipped presentation path so they feel intentionally premium inside the ASCII world.
- While driving, `W` throttles forward, `S` brakes/reverses, and `A`/`D` steer. Camera is a collision-bounded third-person follow pose; no overhead camera or car interior.
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

### Explicitly out of scope

- AI rivals, traffic combat, damage, police/heat, rubber-banding, drifting physics, gearboxes, interiors, upgrades, garages, money, shops, betting, saves, or online leaderboards.
- Races outside `city-alpha`, cross-map races, procedural roads/events, or multiplayer.
- Replacing the CPU ASCII renderer, fixed maps, lightweight Kotlin model, Scene2D UI, or approved bounded compositor.
- Changes to Last Packet story progress, dialogue, door ownership, or canonical completion rules.

## 3. Player and interaction flow

### On foot

1. Existing WASD walk/strafe and arrow look controls remain unchanged.
2. Targeting a parked sedan shows `[E] ENTER SEDAN`.
3. Enter validates the car-radius footprint while ignoring only the target parked entity; failure leaves both player and parked entity unchanged.

### Driving

1. Enter atomically removes the parked entity, moves the authoritative player state to its pose, applies vehicle radius, and switches the render pose to third person.
2. Footsteps/head bob stop; rain, fog, normal resizing, and audio ownership remain unchanged.
3. HUD reads `W/S DRIVE | A/D STEER | E EXIT`; `E` exits only at a valid roadside position and recreates exactly one parked entity.
4. NPC, terminal, and door actions do not fire from a moving car. Door use shows `EXIT VEHICLE FIRST`.

### Race

1. A nonblocking race gate has an interaction target and stable `raceId`.
2. On foot it explains `VEHICLE REQUIRED`; in the car it offers `[E] START NEON RUN`.
3. Active race HUD shows race name, fixed-step timer, and only the next gate bearing. The next finite-depth marker is the only bright checkpoint marker.
4. Checkpoints trigger when the vehicle's semantic center/radius enters the next declared checkpoint radius. Skipped/out-of-order gates do nothing.
5. Finish shows `NEON RUN COMPLETE — MM:SS.mmm`, returns to driving free roam, and permits immediate replay. Escape aborts and removes all active race markers/timer.

## 4. Architecture and ownership

| Owner | Responsibility | Never owns |
|---|---|---|
| `racing/VehicleDefinition.kt` *(proposed)* | Authored vehicle ID, sprite, parked pose, radius, speed, turn limits. | Mutable runtime state/rendering. |
| `racing/RaceDefinition.kt` *(proposed)* | Race ID/name, starter gate entity, ordered checkpoint positions/radii. | Progress/rewards/render output. |
| `ContentLoader` / `LoadedContent` | Parse vehicle/race records and create linked parked vehicle/race gate entities. | Runtime movement. |
| `GameWorld` / `WorldEntity` | Parked-car/gate entity authority, spatial index, atomic entity mutations. | Driven duplicate, checkpoint state. |
| `VehicleSystem` *(proposed; refactors `DebugVehicleSystem`)* | Fixed-step entry/exit, drive update, collision, follow pose. | Race rules/story/UI. |
| `PlayerSimulation` | Select on-foot/vehicle update, interpolation, one player-world update per step. | Raw keyboard reads/F3 product logic. |
| `RaceSession` / `RaceSystem` *(proposed)* | `IDLE`, `ACTIVE`, `FINISHED`, `ABANDONED`; elapsed time, order, start/abort/finish. | Collision/saving/rendering. |
| `InteractionSystem` / `InteractionOutcome` | Typed vehicle/gate targeting and outcome routing. | Dialogue-only assumptions or direct race mutation. |
| `VehicleVisualRenderer` / `RaceGateRenderer` *(proposed)* | Depth-clipped, nearest-neighbor directional pixel-art car frames and finite-depth ASCII gates in the existing presentation path. | Collision/state, a general effect chain, 3D models. |
| `GameScreen` | Fixed-step order, HUD, outcome routing, audio/head-bob mode, lifecycle cleanup. | Parsing/persistence. |

`TrafficSystem` stays cosmetic. Do not repurpose its time-sampled ASCII traffic paths for the player car or future opponents.

## 5. Data, lifecycle, and failures

### Proposed object-file contract

Document this in `DOCS/map-format.md` before authoring content:

```json
{
  "drivableVehicles": [
    {
      "id": "vehicle.player.sedan",
      "spriteId": "sprite.car.sedan",
      "x": 28.5, "z": 14.5, "yawDegrees": 0,
      "radius": 0.42,
      "speedCellsPerSecond": 7.0,
      "turnSpeedDegreesPerSecond": 115.0,
      "displayText": "SEDAN"
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

Validation must require stable unique IDs/references, existing sprites, finite bounded tuning, in-bounds non-solid vehicle/gate centers, full parked-car clearance, existing `raceGate` starter ID, at least three non-duplicate ordered checkpoints, road-tagged checkpoint cells, and four-direction road connectivity between starter/checkpoints. Authoring must explicitly reject overlap with protected story door, terminal, or arrival footprints.

### Runtime state

```text
Vehicle: ON_FOOT -> ENTERING -> DRIVING -> EXITING -> ON_FOOT
Race:    IDLE -> ACTIVE(nextCheckpoint, elapsed) -> FINISHED -> IDLE
                         \-> ABANDONED -----------------------> IDLE
```

- Transitions run at fixed-step boundaries or `GameScreen` command handling before the next step; renderer/HUD never mutate gameplay.
- `RaceSession` stores active race ID, next index, and fixed-step seconds. A finished time is display-only and session-only.
- Failure (invalid entry/exit/start, duplicate E, skipped checkpoint, resize/pause, F3 map switch, screen disposal, content failure) preserves or cleans state deterministically. Map/screen replacement aborts a race and starts the replacement map on foot.

## 6. UI, rendering, and product constraints

- Add typed vehicle/race interaction outcomes; do not overload dialogue IDs or bypass `InputController`.
- Extend `HudModel` with travel mode and optional race state. Story objective/signal values remain separate from racing state.
- Gates are bounded finite-depth glyphs with restrained existing-neon accents. They do not brighten broad roads, change compositor behavior, or write UI.
- `VehicleVisualRenderer` owns only vehicle art: a small directional-frame asset set (rear, front, left, right, then diagonals only if visual review needs them), fixed logical-world scale, nearest filtering, and explicit clipping against the completed finite ASCII depth field. It must not be a generic textured-world layer, a shader stack, a 3D model pipeline, or a way to draw through walls. Its low-resolution pixel art remains crisp at the existing whole-number glyph scale, uses no anti-aliasing, and is visually tuned against the dark neutral road rather than becoming a bright full-screen focal block.
- Third-person camera shortens toward the car if its preferred 2.6-cell rear offset is blocked; it never leaves map bounds or exposes through-wall geometry.
- No engine/skid/start/finish audio in MVP. Existing music/rain/waterfall/footstep ownership stays intact.
- Collision remains semantic. The car cannot push/damage/remove story entities. It blocks/slides only.
- Fine default performance, 1280x720 logical minimum, Standard fallback, whole-number glyph scaling, and all renderer boundaries remain intact.

## 7. Verification matrix

| Scenario | Expected result | Required proof |
|---|---|---|
| Enter sedan | One parked entity becomes one driven state; radius/camera/directional pixel-art car agree. | System/index tests + 1280x720 capture. |
| Blocked entry/exit | Refusal changes no state or entity count. | Collision trace and runtime message test. |
| Driving trace | Deterministic W/S/A/D behavior, no wall/entity tunneling. | Fixed-step hash across frame chunkings. |
| Follow camera by wall | In bounds; shortens safely; no through-wall render. | Camera test + manual close-wall capture. |
| Start on foot | Vehicle-required feedback; race stays idle. | Interaction/race test. |
| Race happy path | Ordered checkpoints advance once; finish gives stable time/replay. | Race trace + start/checkpoint/finish desktop captures. |
| Skip a checkpoint | Cannot advance/finish; HUD retains required target. | Race-system test. |
| Escape/pause/resize/reload | Clean abort/freeze; no catch-up/duplicate markers/state leak. | Lifecycle tests + smoke route. |
| Invalid content | Exact file/path errors for IDs, sprites, placement, radius, non-road/disconnected route. | Loader fixtures + `:core:validateContent`. |
| Story regression | Existing targets remain reachable; driven map door is refused. | Interaction/walk/vehicle-door test. |
| Vehicle visual quality | Directional pixel-art sedan stays crisp, recognizably detailed, palette-coherent, and depth-clipped rather than reading as an ASCII billboard. | Approved rear/side/turn/occlusion captures at 1280x720 and ultrawide. |
| Presentation | Gates/sedan depth-correct; dark road/HUD readability survive Fine + Standard. | 1280x720/ultrawide review, smoke stats, `clean check`. |

## 8. Phased checklist

### Phase 0 — Contract and route lock

- [ ] Add/update this plan, remove life-pass exclusion, and cross-link the canonical plan.
- [ ] Add `drivableVehicles`, `races`, and `raceGate` format/validation rules.
- [ ] Select one story-safe city-alpha parking bay and a 3+ checkpoint road-only Neon Run after first/third-person sightline review.
- [ ] **Verify:** Route has no required story overlap and schema is approved before loader work.

### Phase 1 — Production free-roam vehicle

- [ ] Refactor F3-only `DebugVehicleSystem` into data-backed `VehicleSystem`; remove hard-coded spawn/sprite ownership.
- [ ] Define/import one redistributable, authored directional pixel-art sedan asset set and validate frame dimensions, palette treatment, logical scale, and nearest filtering. Do not generate the final asset from a generic text glyph layout.
- [ ] Add a narrowly scoped `VehicleVisualRenderer` that composites the car frame into the existing FBO/presentation route only where the resolved ASCII depth permits it; add typed parked vehicle entities, atomic entry/exit, vehicle input mode, door refusal, and third-person camera.
- [ ] Add collision/control/camera/entity/lifecycle tests.
- [ ] **Verify:** Focused tests, content validation, `clean check`, and visually approved 1280x720 on-foot/enter/rear/side-turn/exit/close-wall captures. Verify that the car reads as detailed pixel art rather than an ASCII billboard at both Fine and Standard.

### Phase 2 — Race data and deterministic state

- [ ] Add race definitions, parser/validator/fixtures, race gates, session/state machine, and ordered checkpoint tests.
- [ ] Author `vehicle.player.sedan`, `race-gate.neon-run`, and `race.neon-run` only after the schema validates.
- [ ] **Verify:** Exact-path invalid-content tests, deterministic checkpoint/skip/abort/pause/reload tests, content validation, and `clean check`.

### Phase 3 — Player-facing race integration

- [ ] Add bounded gate/checkpoint rendering and mode-aware HUD/timer/bearing/result feedback.
- [ ] Update README, run commands, map format, controls, and verification notes after evidence; remove/demote F3 V after parity.
- [ ] **Verify:** Visual review at 1280x720 and ultrawide for gate/start/checkpoint/finish/wall scenes; update hashes only after approval; smoke test and `clean check`.

### Phase 4 — Expansion after MVP proof

- [ ] Add one more city-alpha event by reusing the same data/session/renderer pipeline.
- [ ] Choose exactly one next packet: ghost replay or one deterministic AI rival.
- [ ] **Verify:** Repeat Phase 3 evidence for each event.

## 9. Open tuning points

- Speed `7.0`, turn `115` degrees/sec, radius `0.42`, and current follow target (2.6 cells rear, 1.35 height) are prototype starting points; alter only with fixed-step + visual evidence.
- Exact parking/gate/checkpoint coordinates are TBD pending overlap and sightline audit; city-alpha-only, road-connected, three-plus ordered checkpoints are locked.
- Progression (rewards, ownership, garages, upgrades), opponents, heat/police, and persistence each need a separate plan because they introduce economy, adversarial behavior, or durable-state contracts.
