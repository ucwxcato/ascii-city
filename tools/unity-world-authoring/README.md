# ASCII City World Authoring for Unity

This Unity package is the world-authoring workspace for ASCII City. Paint semantic cells, shape terrain elevation, and place complete 3D prefab instances in the Unity scene. The game remains first-person ASCII: Unity exports the authored scene data, and the Kotlin runtime renders placed models as validated ASCII sprites with collision.

## Install

Copy this folder into a Unity project's `Packages/com.asciicity.world-authoring` directory, or add it as a local package in Package Manager. The package targets Unity 2022.3 LTS or newer.

## Open the map painter

1. Open **Tools > ASCII City > World Authoring**.
2. Select the ASCII City repository folder containing `assets/maps` and `assets/data/cells.json`.
3. Choose a map and use named brushes such as **Ground & Streets**, **Buildings**, **Park**, or **Nature** to paint semantic cells. **Create / sync scene** makes or refreshes the Unity authoring scene. After that, **Load map** reads the footprint from the scene.
4. Use the elevation brush to raise, lower, or smooth terrain. Painted roads, sidewalks, and paths remain semantic cells while the scene shows their editable surface mesh.
5. Select a model from the **Placeable model shelf**, then use **Place at selected cell** or the prefab brush. Move and rotate each complete model in the Scene view. Buildings can be painted as footprint cells or placed from the shelf as a whole prefab.
6. Choose **Save map** to export the scene footprint to `<map-id>.txt` and elevation to `<map-id>.terrain.json`. Save the Unity scene, then choose **Export scene placements** to write `<map-id>.unity-assets.json`. The running Kotlin game reads these exports; export again after scene edits to update the game.

The package creates named prefabs for built-in 3D props and a Cyberpunk Shop building. Each placed instance is a single selectable object; its internal mesh parts stay grouped under that prefab. Add imported Unity prefabs to `Assets/ASCII City/Placeable Assets`, then press **Refresh shelf** to make them available in the same picker. **Create missing built-in prefabs** restores missing built-ins without replacing edited prefabs.

Map coordinates follow the game: `x` increases east, `z` increases north, yaw `0` faces north, and the first text row is the northern edge. Generated terrain, water, sign, and entrance meshes are Unity previews built from the authored data. They help with placement and elevation but do not replace the ASCII game renderer. Unity model placements use each prefab's authored ASCII sprite profile in game; Kotlin does not import their 3D meshes.

## Export files

- `<map-id>.txt` stores the north-to-south semantic cell footprint used by collision and ASCII ground/wall rendering.
- `<map-id>.terrain.json` stores the height of each footprint corner. The Kotlin renderer projects sloped ground and follows elevation during movement.
- `<map-id>.unity-assets.json` stores ASCII sprite profiles and whole-scene prefab placements, including transforms and blocking collision settings.

Use `Save map` and `Export scene placements` after editing. The runtime loads these exported files; it does not read Unity scene files directly. Terrain and prefab meshes remain Unity authoring assets, while the game displays the footprint and placement sprites as ASCII.
