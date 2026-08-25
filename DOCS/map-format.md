# ASCII City fixed-map format

Phase 2 fixed cities are a validated bundle of UTF-8 text and JSON files. The loader builds the complete candidate world only after every registry, coordinate, reference, and reachability check passes.

## Coordinate contract

- World space uses horizontal `x` and depth `z`; `y` is reserved for projected height.
- `(0,0)` is the southwest footprint cell.
- Positive `x` points east and positive `z` points north.
- The first row of a `.txt` map is the northernmost row. Loading flips file row `r` to `worldZ = depth - 1 - r`.
- Continuous positions floor into cells, including negative values: `(-0.01,-1.01)` maps to `(-1,-2)`.
- Yaw `0` faces north (`+z`) and metadata stores yaw in degrees. Runtime state converts it to radians.

## File bundle

```text
assets/data/palette.json
assets/data/materials.json
assets/data/cells.json
assets/data/sprites.json
assets/data/dialogue.json
assets/maps/<map-name>/<map-name>.txt
assets/maps/<map-name>/<map-name>.meta.json
assets/maps/<map-name>/<map-name>.objects.json
```

Stable IDs use lowercase letters, numbers, dots, underscores, and hyphens. Runtime indices are resolved only after validation and must never be authored or saved.

## Footprint text

The footprint is a rectangular grid of one-character tokens declared by `cells.json`.

- Encoding: UTF-8 printable ASCII.
- Line endings: LF and CRLF are equivalent; one final newline is allowed.
- Tabs, empty maps, empty rows, ragged widths, unknown tokens, and non-printable characters are errors.
- Spaces are legal only if a cell definition explicitly owns the space token; trailing whitespace is never trimmed.
- Diagnostics use `file:line:column`, with all three values one-based.

Example file:

```text
tlln
,..,
,..,
mmmm
```

The first `t` occupies `(0,3)`, while the first `m` on the last row occupies `(0,0)`.

## Semantic material definitions

`materials.json` contains a `materials` array. Every entry requires:

| Field | Type | Rule |
|---|---|---|
| `id` | string | Unique stable ID. |
| `glyphRamp` | string | One or more printable ASCII characters. |
| `paletteKey` | string | Must exist in `palette.json`. |
| `accentPaletteKeys` | string array | Optional unique palette keys sampled as sparse animated color accents. |
| `animation` | string | `static`, `dataStream`, `signalPulse`, or `glitchFlicker`. |
| `animationRate` | number | Finite and non-negative; exactly `0` for `static`. |
| `phaseScale` | number | Finite, non-negative world-coordinate phase multiplier. |
| `tags` | string array | Optional unique semantic tags. |

Animation is cosmetic. The renderer samples glyph channels using world position, height, depth, time, and deterministic phase; it must not mutate cells or collision. Accent placement stays spatially stable while glyphs animate, with denser multicolor accents reserved for dedicated neon facades. Ground materials stay neutral to preserve contrast and depth. This is how buildings read as moving data without turning the street plane into visual noise.

The renderer recognizes the paired `white-tile` plus `wall-tile-grid` or `ceiling-tile-grid` tags for sterile interior surfaces. These produce deterministic world-space ASCII grout lines and a brighter material background while leaving ground shading unchanged. A facade material tagged `solid-interior-wall` receives a filled, orientation-shaded background so its glyph ramp reads as surface detail instead of a thin character curtain. `interior` materials use a sparser accent distribution than neon facades.

## Semantic cell definitions

`cells.json` contains a `cells` array. Every entry requires:

| Field | Type | Rule |
|---|---|---|
| `id` | string | Unique stable ID. |
| `token` | one-character string | Unique printable ASCII authoring token. |
| `groundMaterial` | string | Existing material ID. |
| `facadeMaterial` | string | Existing material ID. |
| `solid` | boolean | Authoritative collision property. |
| `opaque` | boolean | Authoritative world-occlusion property. |
| `height` | integer | Non-negative; positive when solid or opaque. |
| `tags` | string array | Optional unique semantic tags. |
| `interactionType` | string | Optional future static-cell interaction category. |

Displayed tokens and glyph ramps never determine collision. Changing a facade from `0` to `@` cannot make it solid; only `solid` can.

Cells tagged `fixture` render as camera-facing silhouettes while retaining their full semantic cell collision. Casino slot machines combine `fixture` with `slot-machine` to draw cabinet feet, a lower body and button, animated reel windows, a `$` marquee, and a top light. Their billboard artwork never changes occupancy or pathfinding.

## Static ASCII NPC/entity sprites

`sprites.json` contains the shared static artwork used by NPCs and other world entities. SPRITE-01 loads and validates this registry; NPC object records now attach stable sprite IDs to typed NPC metadata. `EntitySpriteRenderer` owns projection, while NPC-02 owns runtime composition into the first-person frame.

```json
{
  "sprites": [
    {
      "id": "sprite.example",
      "physicalWidth": 0.8,
      "physicalHeight": 1.8,
      "anchor": { "x": 0.5, "y": 0.0 },
      "glyphRows": [" /\\ ", "/__\\"],
      "colorRows": [" cc ", "nnnn"],
      "colors": [
        { "symbol": "c", "palette": "neonCyan" },
        { "symbol": "n", "palette": "npc" }
      ]
    }
  ]
}
```

| Field | Type | Rule |
|---|---|---|
| `id` | string | Unique stable sprite ID. |
| `physicalWidth`, `physicalHeight` | number | World-space size, greater than `0` and at most `16`. |
| `anchor.x`, `anchor.y` | number | Normalized attachment point from the sprite's left and bottom edges, each from `0` to `1`; `{0.5, 0.0}` places an actor's bottom center at its world position. |
| `glyphRows` | string array | Top-to-bottom rectangular printable ASCII artwork, 1–64 rows and 1–64 columns. |
| `colorRows` | string array | Same dimensions as `glyphRows`; each visible glyph uses a declared color symbol. |
| `colors[].symbol` | one-character string | Unique non-space printable ASCII symbol local to this sprite. |
| `colors[].palette` | string | Existing key from `palette.json`. |

A space in `glyphRows` is always transparent and must have a space in the matching `colorRows` cell. A visible glyph must have a declared non-space color symbol. Sprite art is presentation data only: it never defines entity collision, interaction range, or story identity. MVP sprites contain one persistent static image; animation sequences are post-MVP.

## Metadata schema

```json
{
  "id": "city-alpha",
  "version": 1,
  "spawn": { "x": 48.5, "z": 4.5, "yawDegrees": 0.0 },
  "skyPalette": "sky",
  "fogPalette": "fog",
  "objectsFile": "city-alpha.objects.json"
}
```

- `id` must equal the requested map name and `version` must be positive.
- Exactly one `spawn` object is required. Its continuous position must be in bounds and on a non-solid cell.
- Sky/fog keys must exist in the palette.
- `objectsFile` names one JSON file inside `assets/maps/`; paths and traversal are rejected.
- Unknown and duplicate JSON fields are errors.

An enclosed, single-level map may additionally define one ceiling across its complete rectangular footprint:

```json
"ceiling": {
  "height": 7.0,
  "material": "ceiling.water-park"
}
```

`height` is a finite world-space Y coordinate at least `0.5` units above the spawn eye height. `material` must name an existing entry in `materials.json`. The renderer casts this bounded horizontal plane above the pitch-adjusted horizon using the same perpendicular depth convention as ground, walls, fixtures, and sprites. Walls nearer than the plane occlude it, the plane hides taller exterior columns where a ray reaches the roof first, and its finite depth prevents sky-only rain from drawing through it. A metadata ceiling is presentation geometry only: it does not alter footprint collision, create an upper walkable floor, or support stacked levels.

## Object schema

`<map-name>.objects.json` contains an `objects` array. Common fields are `id`, `type`, `x`, `z`, optional `blocking`, optional non-negative `radius`, optional `interactionId`, and optional `displayText`. Blocking objects require a positive collision radius; defaults are type-specific and validated at load time. NPCs make `blocking`, `displayText`, `spriteId`, `facingDegrees`, and `interactionId` explicit required fields; terminals require both `displayText` and `interactionId`.

| Type | Requirements |
|---|---|
| `npc` | Non-solid in-bounds cell; explicit `blocking`; positive radius when blocking; non-blank `displayText`; existing `spriteId`; `facingDegrees` in `[0,360)` using yaw `0` north/`90` east; existing dialogue `interactionId` whose speaker matches `displayText`. NPC-only fields are rejected on other object types. |
| `terminal` | Non-solid in-bounds cell; non-blank `displayText`; existing dialogue `interactionId` whose speaker matches `displayText`. |
| `lamp` | Optional non-solid in-bounds landmark fixture with a local cosmetic glow. Roadside lamps are also derived automatically from sidewalk/road topology. |
| `door` | In bounds; may anchor to a solid doorway footprint and defaults to blocking. An optional `destinationMapId` must name a playable map. Its optional `destinationSpawn` object supplies `x`, `z`, and optional `yawDegrees` for an explicit arrival point; without it, travel uses the destination map's authored spawn. Set `renderBillboard` only when the door needs freestanding sprite art; façade doors remain part of the wall by default. A non-blocking door's `radius` is also its center-ray interaction width. |

Object IDs are globally unique within the map. NPC and terminal cells must be reachable from spawn through four-directional non-solid footprint cells. NPC sprite artwork remains presentation-only: the paired `WorldEntity` position, `blocking`, and `radius` fields are the collision authority.

### Visual traffic schema

An object file may include a `trafficCars` array. Each entry is a cosmetic, camera-facing car sprite that follows its closed waypoint loop at a fixed speed. Traffic is not a world entity: it has no collision radius, interaction, pathfinding, avoidance, parking, persistence, or gameplay state.

| Field | Type | Rule |
|---|---|---|
| `id` | string | Stable and unique across the map's objects, signs, entrances, and traffic cars. |
| `spriteId` | string | Existing static ASCII sprite ID. |
| `label` | string | Optional non-blank printable ASCII wordmark of at most 12 glyphs. It stays compact over the perspective-scaled car body. |
| `speedCellsPerSecond` | number | Finite and between `0.1` and `4.0`; keep ordinary city traffic restrained. |
| `phaseOffsetSeconds` | number | Optional finite non-negative loop-time offset used to spread cars along similar routes. |
| `waypoints` | array | At least two `{ "x": number, "z": number }` points, each in bounds on a `road` cell. The last point reconnects to the first automatically; adjacent points, including that closing segment, must differ. |

Routes are authored as straight road-lane segments. The traffic renderer samples them from simulation time and depth-tests the resulting sprite against the first-person frame. It never writes to `GameWorld`, the spatial entity index, collision, interaction targeting, or save state. Similar routes should use different `phaseOffsetSeconds` values to keep the city active without becoming traffic-heavy.

### Façade sign schema

The object file may also contain a `facadeSigns` array. These are cosmetic wall-mounted marquees, not world entities or collision geometry.

| Field | Type | Rule |
|---|---|---|
| `id` | string | Stable and unique across objects and façade signs. |
| `face` | string | `north`, `south`, `east`, or `west`; the named side must border an open in-bounds cell. |
| `cellX`, `cellZ` | integer | Non-negative anchor cell. |
| `widthCells` | integer | Positive run length; advances in +x on north/south faces and +z on east/west faces. |
| `bottom`, `height` | number | Finite non-negative bottom and positive height; the top must not exceed any covered building cell. |
| `glyphRows` | string array | Rectangular printable-ASCII close layout, 2–12 rows and 3–64 columns. |
| `distantGlyphRows` | string array | Rectangular printable-ASCII distance layout with the same bounds. |
| `borderPalette`, `textPalette`, `accentPalette`, `backgroundPalette` | string | Existing palette keys. |
| `animationRate` | number | Finite 0–4 Hz rate for animated façade lighting. |
| `label` | string | Optional non-blank printable ASCII label, at most 32 glyphs. It is projected once per screen glyph for readable close-up text rather than magnified as façade texture. |

Every covered footprint cell must be tagged `building` and opaque. Façade overlays on the same face may not overlap vertically. The renderer samples their frame/light artwork at the wall's existing perpendicular depth, so perspective, fog, nearer-wall occlusion, and collision semantics remain those of the host façade. A `label` is a readability overlay: it writes one terminal glyph per character at the sign's projected center only from a sufficiently front-on authored side, preventing nearby words from becoming repeated stretched glyph blocks or reading through the façade at a shallow angle. Overlays never alter `WorldGrid`.

### Façade entrance schema

The object file may contain `facadeEntrances` alongside `facadeSigns`. An entrance is visible ground-level façade art for a door, never collision geometry or an interaction target. A separate `objects` entry of type `door` still owns those semantics.

| Field | Type | Rule |
|---|---|---|
| `id`, `face`, `cellX`, `cellZ`, `widthCells` | sign equivalents | Same stable ID, exterior-face, and building-cell constraints as a sign. |
| `height` | number | Positive; entrances always have `bottom = 0`. |
| `glyphRows`, `distantGlyphRows`, palette fields, `animationRate` | sign equivalents | Same rectangular ASCII, palette, and 0–4 Hz constraints as a sign. |
| `lighting` | string | Required: `amberPulse`, `clubStrobe`, or `marquee`. Only `@` and `~` glyphs are lit panels. |

`amberPulse` alternates marked glass/panel glyphs between `accentPalette` and `backgroundPalette`; ordinary doors should use an amber/yellow accent. `clubStrobe` cycles those panels through the accent, text, and border palettes. The enclosing door frame stays stable, and lighting never changes map collision, world lighting, or the eternal-night sky.

### Atmospheric emitter schema

An optional `atmosphericEmitters` array places stateless visual steam or rooftop exhaust. At most eight emitters are allowed. Their three wisps are derived from stable ID, authored phase, world position, and simulation time; they do not create entities or collision.

| Field | Type | Rule |
|---|---|---|
| `id` | string | Stable and unique across every object-file array. |
| `kind` | string | `steam` on a non-solid cell or `rooftopExhaust` on an opaque building cell. Rooftop base height is derived from the host building. |
| `x`, `z` | number | Finite in-bounds world position. |
| `riseHeight` | number | Finite `0.5..3.0` world units. |
| `drift` | number | Finite `0..0.75` horizontal world units. |
| `periodSeconds` | number | Finite `1.5..8.0`. |
| `phaseOffset` | number | Optional finite value in `[0,1)`. |

Steam sources render a persistent cosmetic grate and use bright neutral wisp cores that attenuate through the normal fog calculation. Extra plume detail is distance-limited, and every fixture/wisp write has finite frame depth. Nearer walls, fixtures, and actors win; the renderer never mutates `WorldGrid`.

### Derived park fireflies

Green fireflies require no object array. `ParkFireflyRenderer` selects at most four authored `lamp` objects per map when a grass or nature-tagged cell is within one cell of the lamp. Waterfall-adjacent lamps rank first, then ordinary park lamps, then other nature lamps; indoor water-park lamps do not qualify because their nearby cells lack this outdoor habitat. Each selected lamp emits three deterministic, stateless finite-depth glyphs. Fireflies are visual only: they create no collision, entities, audio, interaction targets, or authored state, and nearer geometry always occludes them.

### Ambient event emitter schema

An optional `ambientEventEmitters` array places up to twelve intermittent micro-events. Event intervals are deterministic pseudo-random samples derived from stable IDs, so equal fixed-step traces reproduce exactly. Each trigger drives one short visual glyph plus a distance-attenuated, stereo-panned one-shot cue.

| Field | Type | Rule |
|---|---|---|
| `id` | string | Stable and unique across every object-file array. |
| `kind` | string | `ventHiss`, `electricSpark`, or `distantHorn`. |
| `x`, `z` | number | Finite in-bounds event position. |
| `height` | number | Finite `0..12` projected world height. |
| `minimumIntervalSeconds`, `maximumIntervalSeconds` | number | Finite `2..60`; maximum must be at least minimum. |
| `durationSeconds` | number | Finite `0.1..2.0`. |
| `audibleRange` | number | Finite `2..32` cells. Outside this radius the trigger is silent. |

Music mute does not mute these environmental cues. Pause/hide stops fixed-step scheduling, and resume does not accumulate a catch-up burst.

### Ambient actor schema

An optional `ambientActors` array defines up to ten noninteractive pedestrians. Each actor is one blocking `AMBIENT_ACTOR` world entity rendered through the shared ASCII sprite path. Its semantic entity position and collision radius remain authoritative while on-screen, culled, paused, or resized.

| Field | Type | Rule |
|---|---|---|
| `id` | string | Stable unique entity ID. |
| `spriteId` | string | Existing static ASCII sprite ID. |
| `radius` | number | Optional finite `0.15..0.35`; defaults to `0.22`. |
| `speedCellsPerSecond` | number | Finite `0.2..0.8`. |
| `waitSeconds` | number | Optional finite endpoint dwell in `0..12`. |
| `startDelaySeconds` | number | Optional finite initial schedule offset in `0..30`. |
| `waypoints` | array | Two to sixteen in-bounds points on walkable cells. Consecutive segments must be axis-aligned, at most 12 cells, and retain radius clearance along the full segment. |

Routes ping-pong between their endpoints. Actors move in stable ID order at the existing fixed step. A blocked actor waits without accumulating movement; it never pushes the player, tunnels through a solid cell, or creates an interaction prompt.

## Dialogue schema

`dialogue.json` contains `dialogues`, each with a unique stable `id`, non-blank `speaker`, and one or more non-blank `lines`. NPC and terminal `interactionId` values resolve these definitions. The modal dialogue UI presents one line at a time; E/Enter advances and closes after the final line, while Escape closes immediately.

## Diagnostic examples

```text
maps/city-alpha.txt:17:44: unknown map token '?'
maps/city-alpha.meta.json:$.spawn: duplicate field
data/cells.json:$.cells[4].height: must be non-negative
maps/city-alpha.objects.json:$.objects[1].id: duplicate object ID 'npc.rin'
```

The `check` lifecycle runs `core:validateContent`. To prove its failure path without modifying the canonical city:

```powershell
.\gradlew.bat check -PcontentMap=invalid-phase2
.\gradlew.bat check
```

The first command must fail on the intentional unknown-token fixture. The second must return the canonical content and all tests to green.
