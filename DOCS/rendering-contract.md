# First-Person ASCII Rendering Contract

> Status: Phase 4 implementation and visual verification complete; first-person projection follows interpolated WASD/arrow-controlled simulation from the 960×540 minimum through responsive large and ultrawide client areas. The Phase 7 camera-facing NPC/entity sprite projector and stationary Rin composition are verified at the reference sizes, and the responsive window revision is verified at 1280×720 plus a 3440-wide Windows work area.

## Reference boundary

The visual inspiration is [A Walkable ASCII Cyberpunk City in One HTML File](https://www.youtube.com/watch?v=3YtygAx_C6A). This project uses the reference only to identify visual behaviors. It does not copy video frames, code, city layouts, audio, typography, colors, or other assets.

## Original annotated reference plates

These are project-authored diagrams of the required composition, not extracted video frames.

```text
TALL BACKGROUND MASSING             NEAR FACADE
        +---------+                      ||||||
   +----+ windows +----+                 ||||||  larger glyph spans
---+-------------------+---- HORIZON ----||||||----
              > VANISHING POINT <        ||||||
             /                  \          ||||||
     ROAD EDGE                    ROAD EDGE
```

- The horizon and vanishing point anchor the street-level perspective.
- Road edges and facade bands converge toward the vanishing point.
- Near facades cover more rows and columns; distant towers may remain visible above them.

```text
LOOK UP                              LOOK DOWN
more sky / taller upper spans        horizon rises / more ground samples
          ^ pitch                                v pitch
---------- horizon                 ---------------------- horizon
  converging road                         wider road foreground
```

- Pitch changes projection and visible sky/ground; it never changes player position.
- Yaw changes which facade faces are visible and moves the vanishing point with the view.

## Required first-person behaviors

- The view is at street level with a stable horizon and a forward vanishing point.
- Road and sidewalk edges converge with distance.
- Nearby facades occupy more character cells than distant facades.
- Turning changes the visible building faces and their horizontal positions continuously.
- Looking up/down moves the horizon and projected vertical spans without moving the player.
- Near opaque surfaces occlude farther surfaces at the same screen cells.
- Taller distant buildings can remain visible above shorter nearer buildings.
- An optional whole-map ceiling projects above the horizon as a bounded horizontal plane, uses the shared perpendicular depth buffer, and leaves outdoor-map output unchanged when absent.
- Ceiling depth stops sky-only rain and correctly meets nearer walls without defining collision or an upper walkable floor.
- The waterpark shell uses deterministic world-space grout grids, pale off-white tile fields, darker side/ceiling shading, and sparse sickly fluorescent accents. Its ground and water remain darker so the enclosure does not flatten navigation depth.
- Distance fog reduces contrast and glyph density without hiding nearby collision geometry. Exterior façade backgrounds gain a restrained dark fill inside 12 cells, so close buildings read as solid masses while the farther skyline stays lighter and more abstract.
- City-facing low/mid facades add sparse deterministic rib/belt seams, while garden/residential/tall facades add deterministic window-grid divisions inside the close/middle detail band. Structural marks yield to the normal animated material ramp between seams and drop away before the fogged skyline, so they describe building scale without becoming uniform noise.
- Road and sidewalk ground remains neutral and non-semantic: nearby roads add restrained four-cell seam blocks and sidewalks add two-cell slab joints, but their existing animated base ramps remain visible between marks. The detail has no accent palette, collision, walkability, or world-state effect and fades before distant ground can lose its clean vanishing corridor.
- Fog tuning is strictly data-authored in `assets/data/presentation.json`: samples begin converging after 10 cells, reach the fog color and 0.38 glyph density at 48 cells, and remain deterministic at equal world position/depth, while road and solid-facade samples within 8 cells retain at least 0.05 luminance contrast against their material background.
- Colored windows and neon facade patterns provide landmarks against a nearly black night sky.
- Authored Vegas-style façade marquees remain flush to cardinal building faces, use warm gold/red-orange/cream/acid accents distinct from the cyan/magenta buildings, switch to a simpler distance layout when projected small, and share the host wall depth for perspective, fog, and occlusion. Their optional readable labels place each letter once in projected terminal cells, so close-up names such as `LAST LIGHT CASINO` remain legible instead of stretching the source letters into blocks.
- Street lamps are derived at regular intervals from sidewalk/road topology, with optional authored fixtures for landmarks; their local glow is depth-tested so nearby walls occlude it.
- Tree cells render as narrow trunks beneath style-specific foliage silhouettes: park trees use layered broad canopies, while nature pines use low conical boughs and pointed crowns. Benches, bins, fountains, pavilion bays, triangular tents, and rock-shouldered waterfall flow render as camera-facing billboard silhouettes centered in their semantic cells; all open silhouette space remains visually transparent while collision stays semantic.
- The city is constructed from living data: facade streams, signal bands, road data flow, sparse sky rain, and restrained deterministic glitches animate across its surfaces.
- Reduced-flicker tuning caps glitch channels at 6 Hz, makes glyph substitutions sparse, and limits color flashes to a 1.15 brightness multiplier; whole surfaces never flash.
- Material animation is sampled from world position, depth, stable identity, and time; it changes presentation without mutating geometry or collision.
- The final image is composed of aligned text glyphs and semantic colors.
- The presenter gives the completed ASCII surface a single cell-aware CRT composite: curved glass, RGB phosphor separation, horizontal scanlines, a dark rounded vignette, finite-depth contact shade, and an explicit emitter-colored haze bounded to one neighboring cell. Cell depth uses an explicit validity bit and nearest sampling; only depth-winning lamp, sign/label, firefly, and casino-light glyphs currently enter the emissive target. HUD/dialogue render afterward and never glow. These are presentation-only operations; world geometry, interaction, semantic colors, and collision remain unchanged.

Façade marquees are sampled during opaque wall projection before lamps and rain. They are distinct from camera-facing NPC/entity sprites used for free-standing people and objects.

## Camera-facing NPC/entity sprite projection

- `EntitySpriteRenderer` transforms an entity position into camera-forward depth and camera-right lateral offset. The sprite plane faces the current view and uses the same perpendicular depth convention as walls, preventing fisheye disagreement.
- Authored physical width/height and normalized anchor determine the world-space plane. A bottom-center `{0.5, 0.0}` anchor keeps an actor's feet at its world position.
- Destination character centers map back to the static sprite's top-authored glyph/color rows. Transparent spaces do not write any glyph, color, background, or depth.
- Every visible destination character calls `AsciiFrame.writeIfNearer`; nearer walls may therefore hide all or only part of a sprite. Visible sprite characters preserve the existing frame background.
- Sprites entirely behind the camera, beyond the bounded draw distance, outside the horizontal FOV, or outside the vertical frame are rejected before rasterization. Partially visible sprites clip to frame bounds.
- `NpcSystem` supplies NPCs in stable entity-ID order, resolves positions from authoritative `GameWorld` entities, and uses a revision cache so unchanged stationary actors allocate nothing per frame. Culling, turning, and resizing never remove semantic state or collision.
- `TerminalBillboardRenderer` uses the same depth-tested projector with one validated compact screen-and-stand sprite for every semantic terminal entity. It makes terminal interaction prompts correspond to a visible physical target without changing terminal collision, target range, or dialogue identity.
- The entity-sprite pass runs after ground, opaque walls, and façade signs, then lamps compose by the same depth rule and sky-only rain runs last. SPRITE-02 provides the reusable projector and NPC-02 supplies the stable typed instances; neither infers collision or sprite identity from rendered glyphs.

## Reference canvas

- Default/reference logical client area: 1280×720.
- Minimum desktop logical client area: 1280×720. The 960×540 calculator fixtures remain defensive internal coverage only.
- Maximum logical client area: none; native resize and maximize may use the available desktop work area.
- Default world glyph cell: Fine 6×12 logical pixels.
- Default world ASCII surface: 213 columns × 60 rows at 1× glyph scale, with one-pixel side gutters at 1280×720.
- Standard fallback ASCII surface: 128 columns × 40 rows; its 96×30 surface remains defensive calculator coverage.
- Selectable world profiles: Fine 6×12 (default), Dense 8×15 (`160×48` at 1280×720), and Standard 10×18 (`128×40`). HUD, dialogue, and errors remain on the Standard UI font for every world profile.
- Exterior façade background mass falls off through 28 world units, keeping middle-distance buildings readable while far architecture still returns to the sparse fogged skyline treatment. Ground backgrounds do not inherit this fill.
- Road asphalt uses a restrained neutral charcoal background distinct from sky black, so its close/middle-distance surface reads as pavement. It remains accent-free and materially darker than façade fields and neon landmarks.
- `LocalizedLightRenderer` runs after wall labels and fixture silhouettes resolve. It derives compact one/two-cell background tints only from resolved finite emissive sources at strength `0.85+`, clips to the frame, and changes only cells at the same or farther depth. It cannot tint sky, hidden/nearer geometry, `WorldGrid`, collision, UI, or a whole road.
- Responsive scale: the largest whole-number glyph scale near the reference density that still preserves the minimum surface. A 5% reference-threshold tolerance absorbs ordinary taskbar/window-chrome loss.
- Ultrawide examples: 3440×1440 selects 172×40 at 2×; a 3440×1415 work area selects 172×39 at 2×; 3840×2160 selects 128×40 at 3×.
- Initial field of view: 75 horizontal degrees at the 16:9 reference aspect. Wider output derives a larger effective horizontal FOV while preserving vertical framing, clamped to the existing 30°–120° safety range.
- Initial pitch range: −55 to +55 degrees.

`AsciiPresenter` applies one uniform translation/scale transform to both cell backgrounds and glyphs with nearest texture filtering. It never scales X and Y independently. Its reusable finite-depth and emissive targets match the selected cell grid exactly and are replaced completely every frame. At Fine density, only explicitly emissive glyphs receive the legacy CPU phosphor copies; the shader retains the compact base-phosphor treatment and samples only the emitter cell plus its cardinal neighbors for the bounded colored haze. `Hud` and the message overlay retain the readable Standard UI font and anchor against the scaled content bounds; the reticle remains on the center interaction cell. `AsciiViewportCalculator` ignores drawable/backing resolution when selecting columns, rows, and scale so HDPI density improves presentation without multiplying CPU rays.

## MVP visual proof

Phase 3 satisfies the original visual baseline: screenshots across eight yaw directions, three pitches, and the minimum/reference sizes show a first-person street, converging road geometry, changing building faces, depth-correct occlusion, varied skyline height, fog, and neon/data landmarks. WINDOW-01 adds responsive proof that the reference image remains stable while ultrawide output reveals additional side geometry with crisp, uniformly scaled glyphs and aligned HUD/UI.

## Deferred polish

- Mouse look, general/full-screen bloom, dynamic shadows, reflections, arbitrary 3D models, sloped geometry, partial ceiling masks, stacked walkable floors, and exact imitation of the reference remain outside the MVP. The bounded cell-aware emissive haze above is the only approved bloom-like treatment.
