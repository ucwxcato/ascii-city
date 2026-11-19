# Authoring maps

This guide explains how to create and edit fixed ASCII City maps. It is a practical companion to [map-format.md](map-format.md), which is the authoritative format reference.

## Map bundle

A map named `my-district` consists of three files in `assets/maps/`:

```text
my-district.txt
my-district.meta.json
my-district.objects.json
```

Start by copying the three `city-alpha` files and renaming each occurrence of `city-alpha` to `my-district`. Map IDs may contain lowercase letters, numbers, periods, underscores, and hyphens.

The game starts `city-alpha`. Playable maps are listed in `MapCatalog.playableMapIds`; while F3 diagnostics are open, `Tab` loads the next entry and keeps diagnostics open so the full rotation can be tested quickly. A valid map bundle does not need to exercise every global material, cell tag, or object type; themed maps may intentionally use only the content they need.

## 1. Draw the footprint

The `.txt` file is a rectangular, UTF-8 grid. One character represents one world cell. Every row must have the same width; do not use tabs or trailing spaces.

```text
nnnnnnn
n,,,,,n
n,..,,n
n....,n
nnnnnnn
```

The first line is the northern edge of the map. In world coordinates, `(0, 0)` is the southwest cell, `x` increases to the east, and `z` increases to the north. For a map with `height` rows, file row `r` becomes `z = height - 1 - r`.

### Current cell palette

| Token | Cell | Walkable | Result |
|---|---|---:|---|
| `.` | road | Yes | Dark neutral street |
| `,` | sidewalk | Yes | Sidewalk |
| `:` | alley | Yes | Alley / glitch zone |
| `_` | plaza | Yes | Plaza |
| `p` | park.grass | Yes | Walkable grass |
| `g` | park.tree | No | 8-unit foliage tree |
| `=` | park.path | Yes | Stone park path |
| `~` | park.pond | No | Animated pond water |
| `b` | park.bench | No | Shaped wooden bench |
| `i` | park.bin | No | Narrow litter bin |
| `f` | park.fountain | No | Fountain pedestal and water |
| `v` | park.pavilion | No | Open-sided pavilion bay |
| `q` | nature.trail | Yes | Dirt nature trail; retains park ambience and natural footsteps |
| `y` | nature.pine | No | 10-unit conical pine silhouette |
| `c` | nature.tent | No | Warm, triangular campsite tent |
| `h` | nature.bridge | Yes | Wooden footbridge across solid water |
| `w` | nature.waterfall | No | Animated falling-water landmark with rock shoulders |
| `r` | nature.rock | No | 7-unit stone cliff / waterfall backing |
| `x` | interior.water-park-wall | No | 7-unit pale white-tile interior wall |
| `l` | building.low | No | 6-unit facade |
| `m` | building.mid | No | 12-unit facade |
| `t` | building.tall | No | 22-unit tower facade |
| `n` | building.neon | No | 16-unit neon facade |

The precise behavior comes from `assets/data/cells.json`. Solid and opaque properties there control collision and visibility; the rendered character or material glyph does not.

For a good first-person street layout, create walkable connected streets and sidewalks, line them with buildings, and use plaza or alley cells to vary intersections. Keep roads and other ground cells visually neutral; neon is meant for building facades and landmarks.

## 2. Set metadata and spawn

In `<map-name>.meta.json`, give the map its matching ID and name its objects file:

```json
{
  "id": "my-district",
  "version": 1,
  "spawn": { "x": 2.5, "z": 1.5, "yawDegrees": 0.0 },
  "skyPalette": "sky",
  "fogPalette": "fog",
  "objectsFile": "my-district.objects.json"
}
```

Use the center of a cell for a spawn or object position: cell `(2, 1)` is normally `x: 2.5, z: 1.5`. Spawn must be in bounds and non-solid. Yaw `0` faces north (`+z`); `90` faces east.

`skyPalette` and `fogPalette` must exist in `assets/data/palette.json`.

### Optional whole-map interior ceiling

For an enclosed single-level venue, add a ceiling object to the map metadata:

```json
"ceiling": {
  "height": 7.0,
  "material": "ceiling.water-park"
}
```

The height must leave at least `0.5` world units above the spawn eye, and the material must exist in `assets/data/materials.json`. The ceiling covers the complete rectangular map footprint and is clipped/depth-tested against walls and fixtures. Use solid semantic wall cells around the venue; the ceiling itself never supplies collision. This form is appropriate for warehouses, enclosed parks, corridors, and other one-floor spaces. It does not create stacked walkable floors, partial roof masks, slopes, or vertical physics.

### Shared NPC/entity sprite art

Static NPC and world-object artwork lives in `assets/data/sprites.json`, not in an individual map. Each sprite has a stable ID, top-to-bottom rectangular glyph rows, matching color-symbol rows, normalized anchor, and physical world size. Spaces are transparent in both layouts; every visible glyph must use a color symbol mapped to an existing `palette.json` key. The common actor anchor is `{ "x": 0.5, "y": 0.0 }`, which places the bottom center of the art at the authored entity position.

NPC map objects reference these shared sprite IDs through the Phase 7 NPC schema. `spriteId` selects presentation art only; the paired object position, explicit `blocking` flag, and `radius` remain the semantic collision authority. NPC-02 composes the already validated definition into the live first-person frame.

## 3. Add objects

Put placed NPCs, terminals, lamps, and doors in `<map-name>.objects.json`:

```json
{
  "objects": [
    {
      "id": "npc.example",
      "type": "npc",
      "x": 2.5,
      "z": 2.5,
      "blocking": true,
      "radius": 0.24,
      "spriteId": "sprite.example",
      "facingDegrees": 180.0,
      "interactionId": "dialogue.example.intro",
      "displayText": "Example"
    }
  ]
}
```

Object IDs must be unique within the map and positions must be in bounds.

- `npc` needs an existing `spriteId`, `facingDegrees` from `0` inclusive to `360` exclusive, an explicit `blocking` flag, a non-blank `displayText`, and a valid dialogue `interactionId` whose speaker matches that display name. It must stand on a non-solid cell and be reachable from spawn. Yaw `0` faces north and `90` faces east; camera-facing sprite projection does not change the authored semantic facing.
- `terminal` needs a valid dialogue `interactionId` and a non-blank `displayText` matching that dialogue's speaker. It must stand on a non-solid cell and be reachable from spawn.
- `lamp` is an optional non-solid fixture with cosmetic glow.
- `door` may sit on a solid doorway cell and is blocking by default.

A blocking object needs a positive `radius`; non-blocking objects may use `0.0`.

Park and nature paths, water, bridges, benches, bins, fountains, pavilions, tents, rocks, waterfalls, and trees are semantic map cells rather than object entities. Their collision comes from `cells.json`; lamps and story relays remain stable authored objects. A bridge is the walkable authored crossing over otherwise-solid water, and tree/fixture silhouette openings never alter collision. Outdoor lamps within one cell of grass or nature terrain automatically seed small green firefly swarms; waterfall-adjacent lamps are prioritized. This is render-only behavior and needs no extra JSON field. Indoor water-park lamps do not seed fireflies.

The same file may contain cosmetic `facadeSigns`. A sign is attached to a straight run of exterior building faces and never changes collision:

```json
{
  "objects": [],
  "facadeSigns": [
    {
      "id": "sign.example-casino",
      "face": "north",
      "cellX": 10,
      "cellZ": 4,
      "widthCells": 6,
      "bottom": 1.5,
      "height": 3.0,
      "glyphRows": [
        "+..........+",
        "| CASINO!  |",
        "+..........+"
      ],
      "distantGlyphRows": [
        "+......+",
        "|CASINO|",
        "+......+"
      ],
      "borderPalette": "signGold",
      "textPalette": "signCream",
      "accentPalette": "signRedOrange",
      "backgroundPalette": "signBackground",
      "animationRate": 3.0
    }
  ]
}
```

For north/south faces, `widthCells` advances east from `cellX`; for east/west faces it advances north from `cellZ`. Every covered cell must be an opaque building, the named face must border an open in-bounds cell, and `bottom + height` must fit below every covered roofline. Both glyph layouts are rectangular printable ASCII: `glyphRows` serves close views and `distantGlyphRows` preserves readable words after projection becomes smaller. Periods act as chasing marquee bulbs; other text remains stable. Palette keys must exist and `animationRate` is limited to 4 Hz.

### Add restrained city life

`atmosphericEmitters`, `ambientEventEmitters`, and `ambientActors` are optional top-level arrays in the same objects file. Use steam only on open ground and rooftop exhaust only on a building cell. Event emitters support `ventHiss`, `electricSpark`, and `distantHorn`; keep their intervals long enough that separate locations do not fire continuously.

Pedestrians reference shared sprite art and follow short axis-aligned `waypoints` on walkable cells. `speedCellsPerSecond`, endpoint `waitSeconds`, and `startDelaySeconds` form their simple authored schedule. Their positions are blocking semantic entities, so keep routes away from story doors, one-cell choke points, and interaction targets. The content validator samples the entire route for solid-cell and radius clearance before constructing the world.

## 4. Validate

Validate an authored map before trying to run it:

```powershell
.\gradlew.bat :core:validateContent -PcontentMap=my-district
```

This checks the map grid, metadata, object references, spawn, collision settings, and NPC/terminal reachability. Error messages identify the file and location, such as `maps/my-district.txt:17:44`.

To include the new map in the in-game test rotation, add its ID to `MapCatalog.playableMapIds`, then run:

```powershell
.\gradlew.bat desktop:run
```

Open F3 and press `Tab` to cycle through `city-alpha`, `water-park`, `nature-reserve`, and any additional catalog entries. `MapCatalog.DEFAULT_MAP_ID` controls the startup map separately.

For a direct development smoke check, launch a catalog entry with `--map=<map-name>`, for example:

```powershell
.\gradlew.bat desktop:run --args="--smoke-test --map=water-park --window-size=1280x720"
```

## Before sharing a map

- Every footprint row has identical width and contains only defined tokens.
- Metadata ID, three filenames, and `objectsFile` agree on the map name.
- Optional ceiling height leaves the required spawn-eye clearance and references an existing material.
- Spawn is on a walkable in-bounds cell.
- NPCs and terminals are reachable from spawn without walking through buildings.
- `interactionId` values exist in `assets/data/dialogue.json`.
- Shared sprite art has rectangular matching glyph/color rows, declared palette symbols, positive bounded physical size, and a normalized anchor.
- Façade signs occupy valid exterior building faces, fit below their roofs, and use rectangular close/distant glyph layouts.
- Atmospheric/event emitters are sparse, in bounds, and use stable unique IDs.
- Ambient actor routes are short, walkable, axis-aligned, and clear of story choke points.
- `:core:validateContent -PcontentMap=<map-name>` passes.
