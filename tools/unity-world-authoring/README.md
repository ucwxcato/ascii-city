# ASCII City World Authoring for Unity

This Unity package is an editor-side map painter and 3D prefab placement workspace for the existing ASCII City maps. Its Scene view includes generated 3D previews for authored trees, ponds, fountains, waterfalls, benches, lamps, facade signs, and entrances, plus actual Unity prefab instances. The grid painter edits the same north-to-south `.txt` footprint files the Kotlin game reads. Unity scenes are authoring workspaces; this does not port the game's runtime or renderer.

## Install

Copy this folder into a Unity project's `Packages/com.asciicity.world-authoring` directory, or add it as a local package in Package Manager. The package targets Unity 2022.3 LTS or newer.

## Open the map painter

1. Open **Tools > ASCII City > World Authoring**.
2. Select the ASCII City repository folder containing `assets/maps` and `assets/data/cells.json`.
3. Choose a map, select a named brush category such as **Ground & Streets**, **Buildings**, **Park**, or **Nature**, and drag across cells to paint. Use **Save map** to write its `.txt` file.
4. Use **Create / sync scene** to create or refresh its 3D cell preview and generated map details. Orbit and frame the Scene view normally.
5. Select a cell in the painter, assign a prefab and stable asset ID, then choose **Place at selected cell**. Move and rotate instances in the Scene view as needed.
6. Save the Unity scene. **Export scene placements** writes `<map-id>.unity-assets.json` beside the map data.

Map coordinates follow the game: `x` increases east, `z` increases north, yaw `0` faces north, and the first text row is the northern edge. The generated tree, water, fountain, waterfall, bench, lamp, sign, and entrance geometry is an editable Unity preview built from the map's authored data. It approximates the game's ASCII silhouettes and materials; it does not reproduce the first-person glyph renderer pixel-for-pixel.

## Current boundary

Terrain edits are consumed by the existing game after saving the map `.txt`. Unity prefab placements are saved in a separate sidecar so they do not invalidate the current strict object schema. The Kotlin runtime does not load that sidecar yet. A later runtime feature can add validated asset definitions and render those placements with matching collision semantics.
