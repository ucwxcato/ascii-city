using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using System.Text;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace AsciiCity.WorldAuthoring
{
    public sealed class AsciiCityWorldWindow : EditorWindow
    {
        [Serializable] private sealed class CellCatalog { public CellDefinition[] cells; }
        [Serializable] private sealed class CellDefinition
        {
            public string id;
            public string token;
            public string[] tags;
            public bool solid;
            public int height;
        }
        [Serializable] private sealed class PlacementFile
        {
            public int version = 1;
            public AssetSpriteProfile[] assets;
            public Placement[] placements;
        }
        [Serializable] private sealed class AssetSpriteProfile
        {
            public string assetId;
            public string spriteId;
            public float physicalWidth;
            public float physicalHeight;
            public float anchorX;
            public float anchorY;
            public string[] glyphRows;
            public string[] colorRows;
            public AsciiCitySpriteColor[] colors;
        }
        [Serializable] private sealed class TerrainHeightfieldFile
        {
            public int width;
            public int depth;
            public float[] heightsSouthToNorth;
        }
        [Serializable] private sealed class MapObjectFile
        {
            public MapObject[] objects;
            public FacadeArt[] facadeSigns;
            public FacadeArt[] facadeEntrances;
        }
        [Serializable] private sealed class MapObject
        {
            public string id;
            public string type;
            public float x;
            public float z;
            public string displayText;
        }
        [Serializable] private sealed class FacadeArt
        {
            public string id;
            public string face;
            public int cellX;
            public int cellZ;
            public int widthCells;
            public float bottom;
            public float height;
            public string[] glyphRows;
            public string label;
            public string borderPalette;
            public string textPalette;
            public string accentPalette;
            public string backgroundPalette;
        }
        [Serializable] private sealed class Placement
        {
            public string id;
            public string assetId;
            public float x;
            public float y;
            public float z;
            public float yawDegrees;
            public float scaleX;
            public float scaleY;
            public float scaleZ;
            public bool blocksMovement;
            public float collisionRadius;
        }
        private sealed class PlaceableModelDefinition
        {
            public readonly string assetId;
            public readonly string displayName;
            public readonly string meshName;
            public readonly string[] materialNames;
            public readonly bool blocksMovement;
            public readonly float collisionRadius;

            public PlaceableModelDefinition(string assetId, string displayName, string meshName, bool blocksMovement, float collisionRadius, params string[] materialNames)
            {
                this.assetId = assetId;
                this.displayName = displayName;
                this.meshName = meshName;
                this.blocksMovement = blocksMovement;
                this.collisionRadius = collisionRadius;
                this.materialNames = materialNames;
            }
        }

        private static readonly PlaceableModelDefinition[] BuiltInPlaceableModels =
        {
            new PlaceableModelDefinition("prop.park-tree", "Park Tree", "park-tree", true, 0.48f,
                "park-tree-trunk", "park-tree-lower-canopy", "park-tree-upper-canopy"),
            new PlaceableModelDefinition("prop.pine-tree", "Pine Tree", "pine-tree", true, 0.42f,
                "pine-tree-trunk", "pine-tree-needles-1", "pine-tree-needles-2", "pine-tree-needles-3", "pine-tree-needles-4"),
            new PlaceableModelDefinition("prop.park-bench", "Park Bench", "park-bench", true, 0.5f,
                "park-bench-seat", "park-bench-back", "park-bench-leg"),
            new PlaceableModelDefinition("prop.stone-fountain", "Stone Fountain", "stone-fountain", true, 0.55f,
                "stone-fountain-lower-plinth", "stone-fountain-pedestal", "hollow-stone-basin",
                "stone-fountain-basin-water", "stone-fountain-spout", "stone-fountain-water-jet-emissive"),
            new PlaceableModelDefinition("prop.waterfall", "Waterfall", "waterfall", true, 0.5f,
                "waterfall-left-rock-shoulder", "waterfall-right-rock-shoulder", "waterfall-water-curtain-emissive",
                "waterfall-rock-lip", "waterfall-splash-pool-emissive"),
            new PlaceableModelDefinition("prop.street-lamp", "Street Lamp", "street-lamp", false, 0.18f,
                "street-lamp-post", "street-lamp-arm", "street-lamp-light-emissive"),
            new PlaceableModelDefinition("building.cyberpunk-shop", "Cyberpunk Shop", "cyberpunk-shop", true, 1.9f,
                "cyberpunk-shop-concrete", "cyberpunk-shop-glass", "cyberpunk-shop-sign")
        };

        [InitializeOnLoadMethod]
        private static void QueuePlaceableModelCreation()
        {
            EditorApplication.delayCall += UpgradeGeneratedPreviewMaterials;
            EditorApplication.delayCall += CreateBuiltInPlaceablePrefabs;
        }

        private static void UpgradeGeneratedPreviewMaterials()
        {
            const string folder = "Assets/ASCII City/Generated Materials";
            if (!AssetDatabase.IsValidFolder(folder)) return;
            Shader shader = PreviewShader();
            if (shader == null) return;
            bool changed = false;
            foreach (string path in AssetDatabase.FindAssets("t:Material", new[] { folder })
                         .Select(AssetDatabase.GUIDToAssetPath))
            {
                Material material = AssetDatabase.LoadAssetAtPath<Material>(path);
                if (material == null || material.shader == shader) continue;
                material.shader = shader;
                EditorUtility.SetDirty(material);
                changed = true;
            }
            if (changed) AssetDatabase.SaveAssets();
        }

        private string projectRoot = string.Empty;
        private string selectedMap = string.Empty;
        private string[] mapIds = new string[0];
        private string[] rows = new string[0];
        private float[] terrainVertexHeights = new float[0];
        private CellDefinition[] cellDefinitions = new CellDefinition[0];
        private string selectedToken = ".";
        private int selectedX = -1;
        private int selectedRow = -1;
        private bool isLoaded;
        private Vector2 paintScroll;
        private float cellButtonSize = 22f;
        private string selectedCategory = "Ground & Streets";
        private GameObject prefabToPlace;
        private GameObject[] placeablePrefabs = new GameObject[0];
        private string prefabAssetId = string.Empty;
        private bool prefabBlocksMovement;
        private float prefabCollisionRadius = 0.5f;
        private bool prefabBrushEnabled;
        private bool elevationBrushEnabled;
        private int elevationBrushRadius = 2;
        private float elevationBrushStrength = 0.25f;
        private float flattenElevation;
        private int elevationBrushMode;
        private readonly HashSet<int> stampedPrefabCells = new HashSet<int>();
        private readonly HashSet<string> refreshedMeshes = new HashSet<string>(StringComparer.Ordinal);

        [MenuItem("Tools/ASCII City/World Authoring")]
        private static void Open()
        {
            GetWindow<AsciiCityWorldWindow>("ASCII City");
        }

        private void OnEnable()
        {
            minSize = new Vector2(540f, 420f);
            RefreshPlaceablePrefabs();
            if (string.IsNullOrEmpty(projectRoot))
            {
                string candidate = Path.GetFullPath(Path.Combine(Application.dataPath, ".."));
                string repositoryCandidate = Path.GetFullPath(Path.Combine(candidate, "..", ".."));
                projectRoot = Directory.Exists(Path.Combine(candidate, "assets", "maps"))
                    ? candidate
                    : Directory.Exists(Path.Combine(repositoryCandidate, "assets", "maps"))
                        ? repositoryCandidate
                        : string.Empty;
            }
        }

        private void OnGUI()
        {
            EditorGUILayout.LabelField("ASCII City World Authoring", EditorStyles.boldLabel);
            EditorGUILayout.LabelField("Paint the existing map grid, then place Unity prefabs in its scene.", EditorStyles.wordWrappedMiniLabel);
            DrawProjectPicker();
            if (!Directory.Exists(projectRoot))
            {
                EditorGUILayout.HelpBox("Choose the ASCII City repository folder. It must contain assets/maps and assets/data/cells.json.", MessageType.Info);
                return;
            }

            DrawMapPicker();
            if (!isLoaded)
                return;

            DrawMapActions();
            DrawTokenPalette();
            DrawTerrainElevationTools();
            DrawGrid();
            DrawPrefabPlacement();
        }

        private void DrawProjectPicker()
        {
            using (new EditorGUILayout.HorizontalScope())
            {
                EditorGUI.BeginChangeCheck();
                projectRoot = EditorGUILayout.TextField("ASCII City folder", projectRoot);
                if (EditorGUI.EndChangeCheck())
                    RefreshMaps();
                if (GUILayout.Button("Browse", GUILayout.Width(72f)))
                {
                    string picked = EditorUtility.OpenFolderPanel("Choose ASCII City repository", projectRoot, string.Empty);
                    if (!string.IsNullOrEmpty(picked))
                    {
                        projectRoot = picked;
                        RefreshMaps();
                    }
                }
                if (GUILayout.Button("Scan", GUILayout.Width(56f)))
                    RefreshMaps();
            }
        }

        private void DrawMapPicker()
        {
            if (mapIds.Length == 0)
                RefreshMaps();
            if (mapIds.Length == 0)
            {
                EditorGUILayout.HelpBox("No map folders were found under assets/maps.", MessageType.Warning);
                return;
            }

            int current = Array.IndexOf(mapIds, selectedMap);
            if (current < 0) current = 0;
            int next = EditorGUILayout.Popup("Map", current, mapIds);
            if (!string.Equals(selectedMap, mapIds[next], StringComparison.Ordinal))
                LoadMap(mapIds[next]);
        }

        private void DrawMapActions()
        {
            using (new EditorGUILayout.HorizontalScope())
            {
                EditorGUILayout.LabelField($"{rows.Length} rows x {Width} cells", GUILayout.Width(150f));
                if (GUILayout.Button("Reload files", GUILayout.Width(92f)))
                    LoadMap(selectedMap);
                if (GUILayout.Button("Save / export", GUILayout.Width(100f)))
                    SaveMap();
                if (GUILayout.Button("Create / sync scene", GUILayout.Width(135f)))
                    CreateOrSyncScene();
            }
            EditorGUILayout.HelpBox("Rows are north-to-south. Grid cell (x, row) maps to world position (x + 0.5, z = depth - row - 0.5).", MessageType.None);
            EditorGUILayout.HelpBox("Unity scene meshes preview map surfaces and assets. The Scene view stores terrain corner elevations; Save / export writes the game footprint and elevation data.", MessageType.Info);
        }

        private void DrawTerrainElevationTools()
        {
            EditorGUILayout.LabelField("Terrain elevation", EditorStyles.boldLabel);
            elevationBrushEnabled = EditorGUILayout.Toggle("Paint elevation brush", elevationBrushEnabled);
            string[] modes = { "Raise", "Lower", "Smooth", "Flatten" };
            elevationBrushMode = EditorGUILayout.Popup("Mode", elevationBrushMode, modes);
            elevationBrushRadius = EditorGUILayout.IntSlider("Brush radius (cells)", elevationBrushRadius, 1, 8);
            if (elevationBrushMode == 3)
                flattenElevation = EditorGUILayout.FloatField("Target elevation", flattenElevation);
            else if (elevationBrushMode != 2)
                elevationBrushStrength = EditorGUILayout.Slider("Strength per stroke", elevationBrushStrength, 0.05f, 1.5f);
            EditorGUILayout.HelpBox("Raise, lower, smooth, or flatten hills with a soft brush. Heights are stored in the Unity scene and exported with the map.", MessageType.None);
        }

        private void DrawTokenPalette()
        {
            EditorGUILayout.LabelField("Terrain brush", EditorStyles.boldLabel);
            CellDefinition[] validCells = cellDefinitions
                .Where(cell => !string.IsNullOrEmpty(cell.token) && cell.token.Length == 1)
                .ToArray();
            string[] categories = validCells.Select(CellCategory).Distinct().OrderBy(CategoryOrder).ThenBy(value => value).ToArray();
            if (categories.Length == 0)
                return;

            int categoryIndex = Array.IndexOf(categories, selectedCategory);
            if (categoryIndex < 0) categoryIndex = 0;
            int nextCategory = EditorGUILayout.Popup("Category", categoryIndex, categories);
            if (nextCategory >= 0 && nextCategory < categories.Length)
                selectedCategory = categories[nextCategory];

            CellDefinition[] brushes = validCells.Where(cell => CellCategory(cell) == selectedCategory).ToArray();
            string[] labels = brushes.Select(cell => $"{FriendlyCellName(cell.id)} ({DisplayToken(cell.token)})").ToArray();
            int selected = Array.FindIndex(brushes, cell => cell.token == selectedToken);
            int nextBrush = EditorGUILayout.Popup("Brush", Mathf.Max(0, selected), labels);
            if (brushes.Length > 0 && nextBrush >= 0 && nextBrush < brushes.Length)
                selectedToken = brushes[nextBrush].token;

            CellDefinition active = validCells.FirstOrDefault(cell => cell.token == selectedToken);
            if (active != null)
                EditorGUILayout.HelpBox($"Painting: {FriendlyCellName(active.id)} [{DisplayToken(active.token)}] — click or drag across cells.", MessageType.None);
        }

        private void DrawGrid()
        {
            EditorGUILayout.LabelField("Map footprint", EditorStyles.boldLabel);
            paintScroll = EditorGUILayout.BeginScrollView(paintScroll, GUILayout.MinHeight(180f), GUILayout.ExpandHeight(true));
            int paintControl = GUIUtility.GetControlID("AsciiCityGridPaint".GetHashCode(), FocusType.Passive);
            for (int rowIndex = 0; rowIndex < rows.Length; rowIndex++)
            {
                using (new EditorGUILayout.HorizontalScope())
                {
                    GUILayout.Label((rows.Length - 1 - rowIndex).ToString(), GUILayout.Width(30f));
                    string row = rows[rowIndex] ?? string.Empty;
                    for (int x = 0; x < row.Length; x++)
                    {
                        string token = row[x].ToString();
                        GUIStyle style = new GUIStyle(EditorStyles.miniButton);
                        style.padding = new RectOffset(1, 1, 1, 1);
                        if (selectedX == x && selectedRow == rowIndex)
                            style.fontStyle = FontStyle.Bold;
                        Rect cellRect = GUILayoutUtility.GetRect(cellButtonSize, cellButtonSize, GUILayout.Width(cellButtonSize), GUILayout.Height(cellButtonSize));
                        Event currentEvent = Event.current;
                        bool hovered = cellRect.Contains(currentEvent.mousePosition);
                        if (currentEvent.type == EventType.MouseDown && currentEvent.button == 0 && hovered)
                        {
                            GUIUtility.hotControl = paintControl;
                            if (prefabBrushEnabled)
                            {
                                stampedPrefabCells.Clear();
                                PlacePrefabAtCell(x, rowIndex);
                            }
                            else if (elevationBrushEnabled)
                                ApplyElevationBrush(x, rowIndex);
                            else
                            {
                                PaintCell(x, rowIndex);
                            }
                            currentEvent.Use();
                        }
                        else if (currentEvent.type == EventType.MouseDrag && currentEvent.button == 0 && GUIUtility.hotControl == paintControl && hovered)
                        {
                            if (prefabBrushEnabled)
                                PlacePrefabAtCell(x, rowIndex);
                            else if (elevationBrushEnabled)
                                ApplyElevationBrush(x, rowIndex);
                            else
                                PaintCell(x, rowIndex);
                            currentEvent.Use();
                        }
                        else if (currentEvent.type == EventType.MouseUp && GUIUtility.hotControl == paintControl)
                        {
                            GUIUtility.hotControl = 0;
                            stampedPrefabCells.Clear();
                            currentEvent.Use();
                        }
                        GUI.Button(cellRect, DisplayToken(token), style);
                    }
                }
            }
            EditorGUILayout.EndScrollView();

            using (new EditorGUILayout.HorizontalScope())
            {
                cellButtonSize = EditorGUILayout.Slider("Cell size", cellButtonSize, 16f, 36f);
                GUILayout.Label(selectedX >= 0 ? $"Selected: ({selectedX}, {rows.Length - 1 - selectedRow})" : "Select a cell", GUILayout.Width(150f));
            }
        }

        private void DrawPrefabPlacement()
        {
            EditorGUILayout.Space(4f);
            EditorGUILayout.LabelField("Placeable model shelf", EditorStyles.boldLabel);
            using (new EditorGUILayout.HorizontalScope())
            {
                string[] modelNames = placeablePrefabs.Select(model => model.name).ToArray();
                int selectedModel = Array.IndexOf(placeablePrefabs, prefabToPlace);
                if (modelNames.Length > 0)
                {
                    int nextModel = EditorGUILayout.Popup("Model", Mathf.Max(0, selectedModel), modelNames);
                    if (nextModel >= 0 && nextModel < placeablePrefabs.Length && nextModel != selectedModel)
                        SelectPlaceablePrefab(placeablePrefabs[nextModel]);
                }
                else
                {
                    EditorGUILayout.LabelField("No prefabs yet", GUILayout.Width(160f));
                }
                if (GUILayout.Button("Refresh shelf", GUILayout.Width(100f)))
                    RefreshPlaceablePrefabs();
            }
            using (new EditorGUILayout.HorizontalScope())
            {
                if (GUILayout.Button("Create missing built-in prefabs", GUILayout.Width(210f)))
                {
                    CreateBuiltInPlaceablePrefabs();
                    RefreshPlaceablePrefabs();
                }
                EditorGUILayout.LabelField("Add imported prefabs to Assets/ASCII City/Placeable Assets.", EditorStyles.wordWrappedMiniLabel);
            }
            EditorGUI.BeginChangeCheck();
            GameObject pickedPrefab = (GameObject)EditorGUILayout.ObjectField("Custom prefab", prefabToPlace, typeof(GameObject), false);
            prefabToPlace = pickedPrefab;
            if (EditorGUI.EndChangeCheck() && prefabToPlace != null)
                SelectPlaceablePrefab(prefabToPlace);
            prefabAssetId = EditorGUILayout.TextField("Stable asset ID", prefabAssetId);
            prefabBrushEnabled = EditorGUILayout.Toggle("Paint prefab brush", prefabBrushEnabled);
            prefabBlocksMovement = EditorGUILayout.Toggle("Blocks movement", prefabBlocksMovement);
            using (new EditorGUILayout.HorizontalScope())
            {
                prefabCollisionRadius = EditorGUILayout.FloatField("Collision radius", prefabCollisionRadius);
                using (new EditorGUI.DisabledScope(prefabToPlace == null || string.IsNullOrWhiteSpace(prefabAssetId) || (!prefabBrushEnabled && selectedX < 0)))
                {
                    if (GUILayout.Button(prefabBrushEnabled ? "Stamp selected cell" : "Place at selected cell", GUILayout.Width(170f)))
                    {
                        if (selectedX >= 0)
                            PlacePrefabAtCell(selectedX, selectedRow);
                    }
                }
            }
            EditorGUILayout.HelpBox(prefabBrushEnabled
                ? "Click or drag on the map to place one complete prefab per cell. Each placement is a single root object with its own position, rotation, scale, and collision settings. This does not change terrain cells."
                : "Place one complete prefab at the selected cell. Each placement stays a single root object; terrain painting remains separate.", MessageType.None);
            EditorGUILayout.LabelField("Move, rotate, or scale each placed root in the Scene view. Save the scene, then export placements from this window.", EditorStyles.wordWrappedMiniLabel);
            using (new EditorGUILayout.HorizontalScope())
            {
                if (GUILayout.Button("Export scene placements", GUILayout.Width(170f)))
                    ExportPlacements();
                if (GUILayout.Button("Focus map in Scene view", GUILayout.Width(170f)))
                    FocusMapInSceneView();
            }
        }

        private static string PlaceableAssetsFolder => "Assets/ASCII City/Placeable Assets";

        private void RefreshPlaceablePrefabs()
        {
            if (!AssetDatabase.IsValidFolder(PlaceableAssetsFolder))
            {
                placeablePrefabs = new GameObject[0];
                return;
            }
            placeablePrefabs = AssetDatabase.FindAssets("t:Prefab", new[] { PlaceableAssetsFolder })
                .Select(AssetDatabase.GUIDToAssetPath)
                .Select(AssetDatabase.LoadAssetAtPath<GameObject>)
                .Where(prefab => prefab != null)
                .OrderBy(prefab => prefab.name, StringComparer.OrdinalIgnoreCase)
                .ToArray();
            if (prefabToPlace != null && placeablePrefabs.Contains(prefabToPlace))
                SelectPlaceablePrefab(prefabToPlace);
            Repaint();
        }

        private void SelectPlaceablePrefab(GameObject prefab)
        {
            prefabToPlace = prefab;
            AsciiCityAssetPlacement placement = prefab != null ? prefab.GetComponent<AsciiCityAssetPlacement>() : null;
            prefabAssetId = placement != null && !string.IsNullOrWhiteSpace(placement.assetId)
                ? placement.assetId
                : prefab != null ? MakeAssetId(prefab.name) : string.Empty;
            if (placement != null)
            {
                prefabBlocksMovement = placement.blocksMovement;
                prefabCollisionRadius = placement.collisionRadius;
            }
        }

        private static void CreateCyberpunkShopMeshIfMissing()
        {
            const string meshName = "cyberpunk-shop";
            const string meshPath = "Assets/ASCII City/Generated Meshes/cyberpunk-shop.asset";
            if (AssetDatabase.LoadAssetAtPath<Mesh>(meshPath) != null) return;
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Generated Meshes");
            EnsureAssetFolder("Assets/ASCII City", "Generated Materials");
            Shader shader = PreviewShader();
            if (shader == null) return;

            Material concrete = LoadOrCreatePreviewMaterial("cyberpunk-shop-concrete", new Color(0.22f, 0.24f, 0.29f), shader);
            Material glass = LoadOrCreatePreviewMaterial("cyberpunk-shop-glass", new Color(0.02f, 0.58f, 0.66f), shader);
            Material sign = LoadOrCreatePreviewMaterial("cyberpunk-shop-sign", new Color(0.96f, 0.28f, 0.52f), shader);
            List<string> materialOrder = new List<string> { "concrete", "glass", "sign" };
            Dictionary<string, List<CombineInstance>> combines = materialOrder.ToDictionary(
                key => key,
                key => new List<CombineInstance>(),
                StringComparer.Ordinal);

            GameObject primitive = GameObject.CreatePrimitive(PrimitiveType.Cube);
            Mesh cube = primitive.GetComponent<MeshFilter>().sharedMesh;
            DestroyImmediate(primitive);
            AddBox("concrete", new Vector3(0f, 2.7f, 0f), new Vector3(3.5f, 5.4f, 2.8f));
            AddBox("concrete", new Vector3(0f, 0.14f, 0f), new Vector3(3.7f, 0.28f, 3.0f));
            AddBox("concrete", new Vector3(0f, 5.48f, 0f), new Vector3(3.72f, 0.2f, 3.02f));
            AddBox("glass", new Vector3(-0.95f, 1.65f, 1.425f), new Vector3(0.9f, 1.75f, 0.05f));
            AddBox("glass", new Vector3(0.95f, 1.65f, 1.425f), new Vector3(0.9f, 1.75f, 0.05f));
            AddBox("glass", new Vector3(0f, 1.25f, 1.425f), new Vector3(0.72f, 2.5f, 0.07f));
            AddBox("glass", new Vector3(-1.0f, 3.75f, 1.425f), new Vector3(0.65f, 0.9f, 0.05f));
            AddBox("glass", new Vector3(0f, 3.75f, 1.425f), new Vector3(0.65f, 0.9f, 0.05f));
            AddBox("glass", new Vector3(1.0f, 3.75f, 1.425f), new Vector3(0.65f, 0.9f, 0.05f));
            AddBox("sign", new Vector3(0f, 4.85f, 1.46f), new Vector3(2.35f, 0.42f, 0.08f));
            AddBox("sign", new Vector3(-1.8f, 2.65f, 0f), new Vector3(0.08f, 5.2f, 2.9f));
            AddBox("sign", new Vector3(1.8f, 2.65f, 0f), new Vector3(0.08f, 5.2f, 2.9f));

            List<Mesh> materialMeshes = new List<Mesh>();
            List<CombineInstance> finalParts = new List<CombineInstance>();
            foreach (string key in materialOrder)
            {
                Mesh part = new Mesh { name = meshName + " " + key };
                part.CombineMeshes(combines[key].ToArray(), true, true);
                materialMeshes.Add(part);
                finalParts.Add(new CombineInstance { mesh = part, subMeshIndex = 0, transform = Matrix4x4.identity });
            }
            Mesh generated = new Mesh { name = meshName };
            generated.CombineMeshes(finalParts.ToArray(), false, false);
            generated.RecalculateNormals();
            generated.RecalculateBounds();
            AssetDatabase.CreateAsset(generated, meshPath);
            foreach (Mesh part in materialMeshes) DestroyImmediate(part);
            AssetDatabase.SaveAssets();

            void AddBox(string materialKey, Vector3 center, Vector3 size)
            {
                combines[materialKey].Add(new CombineInstance
                {
                    mesh = cube,
                    subMeshIndex = 0,
                    transform = Matrix4x4.TRS(center, Quaternion.identity, size)
                });
            }
        }

        private static Material LoadOrCreatePreviewMaterial(string name, Color color, Shader shader)
        {
            string path = "Assets/ASCII City/Generated Materials/" + name + ".mat";
            Material material = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (material != null) return material;
            material = new Material(shader) { name = name, color = color };
            AssetDatabase.CreateAsset(material, path);
            return material;
        }

        private static void CreateBuiltInPlaceablePrefabs()
        {
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Placeable Assets");
            CreateCyberpunkShopMeshIfMissing();
            bool createdAny = false;
            foreach (PlaceableModelDefinition definition in BuiltInPlaceableModels)
            {
                string meshPath = "Assets/ASCII City/Generated Meshes/" + definition.meshName + ".asset";
                Mesh mesh = AssetDatabase.LoadAssetAtPath<Mesh>(meshPath);
                if (mesh == null) continue;

                Material[] materials = definition.materialNames
                    .Select(name => AssetDatabase.LoadAssetAtPath<Material>("Assets/ASCII City/Generated Materials/" + name + ".mat"))
                    .ToArray();
                if (materials.Any(material => material == null) || materials.Length != mesh.subMeshCount)
                {
                    Debug.LogWarning("Cannot build " + definition.displayName + " prefab: its generated material set does not match " + mesh.name + ".");
                    continue;
                }

                string prefabPath = PlaceableAssetsFolder + "/" + definition.meshName + ".prefab";
                bool prefabExists = AssetDatabase.LoadAssetAtPath<GameObject>(prefabPath) != null;
                GameObject model = prefabExists
                    ? PrefabUtility.LoadPrefabContents(prefabPath)
                    : new GameObject(definition.displayName);
                try
                {
                    GameObjectUtility.RemoveMonoBehavioursWithMissingScript(model);
                    model.name = definition.displayName;
                    MeshFilter filter = model.GetComponent<MeshFilter>();
                    if (filter == null) filter = model.AddComponent<MeshFilter>();
                    filter.sharedMesh = mesh;
                    MeshRenderer renderer = model.GetComponent<MeshRenderer>();
                    if (renderer == null) renderer = model.AddComponent<MeshRenderer>();
                    renderer.sharedMaterials = materials;
                    AsciiCityAssetPlacement placement = model.GetComponent<AsciiCityAssetPlacement>();
                    if (placement == null) placement = model.AddComponent<AsciiCityAssetPlacement>();
                    placement.assetId = definition.assetId;
                    placement.blocksMovement = definition.blocksMovement;
                    placement.collisionRadius = definition.collisionRadius;
                    PrefabUtility.SaveAsPrefabAsset(model, prefabPath);
                }
                finally
                {
                    if (prefabExists) PrefabUtility.UnloadPrefabContents(model);
                    else DestroyImmediate(model);
                }
                createdAny = true;
            }
            if (createdAny)
            {
                AssetDatabase.SaveAssets();
                AssetDatabase.Refresh();
            }
        }

        private int Width => rows.Length == 0 ? 0 : rows[0].Length;

        private void PaintCell(int x, int rowIndex)
        {
            if (string.IsNullOrEmpty(selectedToken) || selectedToken.Length != 1)
                return;
            selectedX = x;
            selectedRow = rowIndex;
            char[] chars = rows[rowIndex].ToCharArray();
            chars[x] = selectedToken[0];
            rows[rowIndex] = new string(chars);
            SyncLoadedPreview();
            Repaint();
        }

        private static string DisplayToken(string token) => token == " " ? "space" : token;

        private static string CellCategory(CellDefinition cell)
        {
            if (cell.id.StartsWith("building.", StringComparison.Ordinal)) return "Buildings";
            if (cell.id.StartsWith("park.", StringComparison.Ordinal)) return "Park";
            if (cell.id.StartsWith("nature.", StringComparison.Ordinal)) return "Nature";
            if (cell.id.StartsWith("interior.", StringComparison.Ordinal)) return "Interiors";
            return "Ground & Streets";
        }

        private static int CategoryOrder(string category)
        {
            switch (category)
            {
                case "Ground & Streets": return 0;
                case "Buildings": return 1;
                case "Park": return 2;
                case "Nature": return 3;
                case "Interiors": return 4;
                default: return 5;
            }
        }

        private static string FriendlyCellName(string id)
        {
            string[] words = id.Replace('.', ' ').Replace('-', ' ').Split(new[] { ' ' }, StringSplitOptions.RemoveEmptyEntries);
            return string.Join(" ", words.Select(word => char.ToUpperInvariant(word[0]) + word.Substring(1)));
        }

        private void RefreshMaps()
        {
            string mapsFolder = Path.Combine(projectRoot, "assets", "maps");
            mapIds = Directory.Exists(mapsFolder)
                ? Directory.GetDirectories(mapsFolder).Select(Path.GetFileName).Where(id => File.Exists(Path.Combine(mapsFolder, id, id + ".txt"))).OrderBy(id => id).ToArray()
                : new string[0];
            if (mapIds.Length == 0)
            {
                isLoaded = false;
                selectedMap = string.Empty;
                rows = new string[0];
            }
            if (mapIds.Length > 0 && !mapIds.Contains(selectedMap))
                LoadMap(mapIds.Contains("city-alpha") ? "city-alpha" : mapIds[0]);
            Repaint();
        }

        private void LoadMap(string mapId)
        {
            string mapPath = MapPath(mapId, ".txt");
            string cellsPath = Path.Combine(projectRoot, "assets", "data", "cells.json");
            if (!File.Exists(mapPath) || !File.Exists(cellsPath))
            {
                isLoaded = false;
                return;
            }

            selectedMap = mapId;
            rows = File.ReadAllLines(mapPath, Encoding.UTF8);
            terrainVertexHeights = LoadTerrainVertexHeights(mapId);
            bool loadedSceneAuthoringData = TryLoadMapSceneData(mapId, out string[] sceneRows, out float[] sceneHeights);
            if (loadedSceneAuthoringData)
            {
                rows = sceneRows;
                int expected = (Width + 1) * (rows.Length + 1);
                if (sceneHeights != null && sceneHeights.Length == expected)
                    terrainVertexHeights = sceneHeights;
            }
            CellCatalog catalog = JsonUtility.FromJson<CellCatalog>(File.ReadAllText(cellsPath, Encoding.UTF8));
            cellDefinitions = catalog != null && catalog.cells != null ? catalog.cells : new CellDefinition[0];
            if (!cellDefinitions.Any(cell => cell.token == selectedToken))
                selectedToken = cellDefinitions.Length > 0 ? cellDefinitions[0].token : ".";
            selectedX = -1;
            selectedRow = -1;
            isLoaded = rows.Length > 0;
            if (!loadedSceneAuthoringData)
                SyncLoadedPreview();
            Repaint();
        }

        private bool TryLoadMapSceneData(string mapId, out string[] sceneRows, out float[] sceneHeights)
        {
            sceneRows = null;
            sceneHeights = null;
            string scenePath = "Assets/ASCII City/Maps/" + mapId + ".unity";
            if (AssetDatabase.LoadAssetAtPath<SceneAsset>(scenePath) == null)
                return false;

            Scene scene = SceneManager.GetSceneByPath(scenePath);
            bool openedForRead = !scene.IsValid() || !scene.isLoaded;
            try
            {
                if (openedForRead)
                    scene = EditorSceneManager.OpenScene(scenePath, OpenSceneMode.Additive);
                AsciiCityMapPreview preview = scene.GetRootGameObjects()
                    .SelectMany(root => root.GetComponentsInChildren<AsciiCityMapPreview>(true))
                    .FirstOrDefault(item => item.mapId == mapId);
                if (preview == null || preview.rowsNorthToSouth == null || preview.rowsNorthToSouth.Length == 0)
                    return false;
                sceneRows = (string[])preview.rowsNorthToSouth.Clone();
                if (preview.terrainVertexHeightsSouthToNorth != null)
                    sceneHeights = (float[])preview.terrainVertexHeightsSouthToNorth.Clone();
                int sceneWidth = sceneRows[0].Length;
                return sceneRows.All(row => row != null && row.Length == sceneWidth);
            }
            catch (Exception exception)
            {
                Debug.LogWarning("Could not read Unity map scene " + scenePath + ": " + exception.Message);
                return false;
            }
            finally
            {
                if (openedForRead && scene.IsValid() && scene.isLoaded)
                    EditorSceneManager.CloseScene(scene, true);
            }
        }

        private float[] LoadTerrainVertexHeights(string mapId)
        {
            string path = MapPath(mapId, ".terrain.json");
            int expected = (Width + 1) * (rows.Length + 1);
            if (!File.Exists(path)) return new float[expected];
            try
            {
                TerrainHeightfieldFile file = JsonUtility.FromJson<TerrainHeightfieldFile>(File.ReadAllText(path, Encoding.UTF8));
                if (file == null || file.width != Width || file.depth != rows.Length || file.heightsSouthToNorth == null ||
                    file.heightsSouthToNorth.Length != expected || file.heightsSouthToNorth.Any(height => float.IsNaN(height) || float.IsInfinity(height)))
                {
                    Debug.LogWarning("Ignoring invalid terrain heightfield: " + path);
                    return new float[expected];
                }
                return (float[])file.heightsSouthToNorth.Clone();
            }
            catch (Exception exception)
            {
                Debug.LogWarning("Could not read terrain heightfield " + path + ": " + exception.Message);
                return new float[expected];
            }
        }

        private void EnsureTerrainVertexHeights()
        {
            int expected = (Width + 1) * (rows.Length + 1);
            if (terrainVertexHeights == null || terrainVertexHeights.Length != expected)
                terrainVertexHeights = new float[expected];
        }

        private void WriteTerrainHeightfield()
        {
            EnsureTerrainVertexHeights();
            TerrainHeightfieldFile file = new TerrainHeightfieldFile
            {
                width = Width,
                depth = rows.Length,
                heightsSouthToNorth = (float[])terrainVertexHeights.Clone()
            };
            File.WriteAllText(MapPath(selectedMap, ".terrain.json"), JsonUtility.ToJson(file, true) + "\n", new UTF8Encoding(false));
        }

        private void ApplyElevationBrush(int x, int rowIndex)
        {
            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None)
                .FirstOrDefault(item => item.mapId == selectedMap);
            if (preview == null)
            {
                EditorUtility.DisplayDialog("No map scene", "Create / sync the Unity scene before painting elevation.", "OK");
                return;
            }

            EnsureTerrainVertexHeights();
            float centerX = x + 0.5f;
            float centerZ = rows.Length - rowIndex - 0.5f;
            float radius = Mathf.Max(1, elevationBrushRadius);
            float[] source = (float[])terrainVertexHeights.Clone();
            int stride = Width + 1;
            int minimumX = Mathf.Max(0, Mathf.FloorToInt(centerX - radius));
            int maximumX = Mathf.Min(Width, Mathf.CeilToInt(centerX + radius));
            int minimumZ = Mathf.Max(0, Mathf.FloorToInt(centerZ - radius));
            int maximumZ = Mathf.Min(rows.Length, Mathf.CeilToInt(centerZ + radius));
            for (int vertexZ = minimumZ; vertexZ <= maximumZ; vertexZ++)
            for (int vertexX = minimumX; vertexX <= maximumX; vertexX++)
            {
                float distance = Vector2.Distance(new Vector2(vertexX, vertexZ), new Vector2(centerX, centerZ));
                if (distance > radius) continue;
                float t = 1f - distance / radius;
                float falloff = t * t * (3f - 2f * t);
                int index = vertexZ * stride + vertexX;
                switch (elevationBrushMode)
                {
                    case 0: terrainVertexHeights[index] = Mathf.Clamp(source[index] + elevationBrushStrength * falloff, -32f, 64f); break;
                    case 1: terrainVertexHeights[index] = Mathf.Clamp(source[index] - elevationBrushStrength * falloff, -32f, 64f); break;
                    case 2:
                        float total = 0f;
                        int count = 0;
                        for (int offsetZ = -1; offsetZ <= 1; offsetZ++)
                        for (int offsetX = -1; offsetX <= 1; offsetX++)
                        {
                            if (offsetX == 0 && offsetZ == 0) continue;
                            int sampleX = vertexX + offsetX;
                            int sampleZ = vertexZ + offsetZ;
                            if (sampleX < 0 || sampleX > Width || sampleZ < 0 || sampleZ > rows.Length) continue;
                            total += source[sampleZ * stride + sampleX];
                            count++;
                        }
                        if (count > 0) terrainVertexHeights[index] = Mathf.Lerp(source[index], total / count, falloff * 0.65f);
                        break;
                    default: terrainVertexHeights[index] = Mathf.Clamp(Mathf.Lerp(source[index], flattenElevation, falloff), -32f, 64f); break;
                }
            }
            selectedX = x;
            selectedRow = rowIndex;
            preview.rowsNorthToSouth = (string[])rows.Clone();
            preview.cellTypes = BuildPreviewCellTypes();
            preview.terrainVertexHeightsSouthToNorth = (float[])terrainVertexHeights.Clone();
            EditorUtility.SetDirty(preview);
            EditorSceneManager.MarkSceneDirty(preview.gameObject.scene);
            SceneView.RepaintAll();
            Repaint();
        }

        private void SaveMap()
        {
            AsciiCityMapPreview loadedPreview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None)
                .FirstOrDefault(item => item.mapId == selectedMap);
            if (loadedPreview != null && loadedPreview.rowsNorthToSouth != null && loadedPreview.rowsNorthToSouth.Length > 0)
            {
                rows = (string[])loadedPreview.rowsNorthToSouth.Clone();
                int sceneHeightCount = (Width + 1) * (rows.Length + 1);
                if (loadedPreview.terrainVertexHeightsSouthToNorth != null &&
                    loadedPreview.terrainVertexHeightsSouthToNorth.Length == sceneHeightCount)
                    terrainVertexHeights = (float[])loadedPreview.terrainVertexHeightsSouthToNorth.Clone();
            }
            if (!HasRectangularRows())
            {
                EditorUtility.DisplayDialog("Cannot save map", "All rows must have the same width.", "OK");
                return;
            }
            File.WriteAllText(MapPath(selectedMap, ".txt"), string.Join("\n", rows) + "\n", new UTF8Encoding(false));
            WriteTerrainHeightfield();
            SyncAndSaveMapScene();
            AssetDatabase.Refresh();
        }

        private void SyncAndSaveMapScene()
        {
            string scenePath = "Assets/ASCII City/Maps/" + selectedMap + ".unity";
            Scene scene = SceneManager.GetSceneByPath(scenePath);
            bool openedForSync = !scene.IsValid() || !scene.isLoaded;
            try
            {
                if (openedForSync && AssetDatabase.LoadAssetAtPath<SceneAsset>(scenePath) != null)
                    scene = EditorSceneManager.OpenScene(scenePath, OpenSceneMode.Additive);
                if (!scene.IsValid() || !scene.isLoaded) return;
                AsciiCityMapPreview preview = scene.GetRootGameObjects()
                    .SelectMany(root => root.GetComponentsInChildren<AsciiCityMapPreview>(true))
                    .FirstOrDefault(item => item.mapId == selectedMap);
                if (preview == null) return;
                preview.rowsNorthToSouth = (string[])rows.Clone();
                preview.cellTypes = BuildPreviewCellTypes();
                EnsureTerrainVertexHeights();
                preview.terrainVertexHeightsSouthToNorth = (float[])terrainVertexHeights.Clone();
                EditorUtility.SetDirty(preview);
                RebuildAuthoredDetails(preview);
                EditorSceneManager.MarkSceneDirty(scene);
                EditorSceneManager.SaveScene(scene);
            }
            finally
            {
                if (openedForSync && scene.IsValid() && scene.isLoaded)
                    EditorSceneManager.CloseScene(scene, true);
            }
        }

        private bool HasRectangularRows()
        {
            return rows.Length > 0 && rows.All(row => row != null && row.Length == rows[0].Length);
        }

        private void CreateOrSyncScene()
        {
            if (string.IsNullOrEmpty(selectedMap)) return;
            string sceneDirectory = Path.Combine(Application.dataPath, "ASCII City", "Maps");
            Directory.CreateDirectory(sceneDirectory);
            string sceneAssetPath = "Assets/ASCII City/Maps/" + selectedMap + ".unity";
            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            if (preview == null && AssetDatabase.LoadAssetAtPath<SceneAsset>(sceneAssetPath) != null)
            {
                if (!EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo()) return;
                EditorSceneManager.OpenScene(sceneAssetPath, OpenSceneMode.Single);
                preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            }

            if (preview == null)
            {
                if (!EditorSceneManager.SaveCurrentModifiedScenesIfUserWantsTo()) return;
                Scene scene = EditorSceneManager.NewScene(NewSceneSetup.EmptyScene, NewSceneMode.Single);
                GameObject root = new GameObject("ASCII City - " + selectedMap);
                preview = root.AddComponent<AsciiCityMapPreview>();
                preview.mapId = selectedMap;
                preview.rowsNorthToSouth = (string[])rows.Clone();
                preview.cellTypes = BuildPreviewCellTypes();
                preview.terrainVertexHeightsSouthToNorth = (float[])terrainVertexHeights.Clone();
                Selection.activeGameObject = root;
                EditorSceneManager.SaveScene(scene, sceneAssetPath);
            }

            preview.mapId = selectedMap;
            preview.rowsNorthToSouth = (string[])rows.Clone();
            preview.cellTypes = BuildPreviewCellTypes();
            EnsureTerrainVertexHeights();
            preview.terrainVertexHeightsSouthToNorth = (float[])terrainVertexHeights.Clone();
            RebuildAuthoredDetails(preview);
            CreateBuiltInPlaceablePrefabs();
            RefreshPlaceablePrefabs();
            EditorUtility.SetDirty(preview);
            EditorSceneManager.MarkSceneDirty(preview.gameObject.scene);
            EditorSceneManager.SaveScene(preview.gameObject.scene);
            FocusMapInSceneView();
            EditorUtility.DisplayDialog("Scene ready", "The 3D map scene is ready. Select a whole prefab asset to paint or place it, then edit its transform in the Scene view.", "OK");
        }

        private void RebuildAuthoredDetails(AsciiCityMapPreview preview)
        {
            refreshedMeshes.Clear();
            Transform oldDetails = preview.transform.Find("__Authored Details");
            GameObject root = new GameObject("__Authored Details (rebuilding)");
            root.transform.SetParent(preview.transform, false);
            try
            {
                Dictionary<string, Color> palette = LoadPalette();
                BuildCellMeshes(root.transform);
                string objectPath = MapPath(selectedMap, ".objects.json");
                if (File.Exists(objectPath))
                {
                    MapObjectFile authored = JsonUtility.FromJson<MapObjectFile>(File.ReadAllText(objectPath, Encoding.UTF8));
                    if (authored != null)
                    {
                        if (authored.objects != null)
                            foreach (MapObject item in authored.objects)
                                if (item != null && item.type == "lamp") BuildLamp(root.transform, item);
                        BuildFacadeArt(root.transform, authored.facadeSigns, palette, false);
                        BuildFacadeArt(root.transform, authored.facadeEntrances, palette, true);
                    }
                }
                if (oldDetails != null)
                    DestroyImmediate(oldDetails.gameObject);
                root.name = "__Authored Details";
            }
            catch
            {
                DestroyImmediate(root);
                throw;
            }
        }

        private void BuildCellMeshes(Transform parent)
        {
            for (int rowIndex = 0; rowIndex < rows.Length; rowIndex++)
            {
                string row = rows[rowIndex] ?? string.Empty;
                int worldZ = rows.Length - 1 - rowIndex;
                for (int x = 0; x < row.Length; x++)
                {
                    char token = row[x];
                    Vector3 basePosition = new Vector3(x + 0.5f, TerrainHeightAt(x + 0.5f, worldZ + 0.5f), worldZ + 0.5f);
                    if (token == 'g')
                    {
                        GameObject tree = new GameObject($"Park Tree [{x}, {worldZ}]");
                        tree.transform.SetParent(parent, false);
                        tree.transform.localPosition = basePosition;
                        CreatePrimitive(tree.transform, "Trunk", PrimitiveType.Cylinder, new Vector3(0f, 1.05f, 0f), new Vector3(0.15f, 1.05f, 0.15f), new Color(0.33f, 0.22f, 0.14f));
                        CreatePrimitive(tree.transform, "Lower Canopy", PrimitiveType.Sphere, new Vector3(0f, 3.2f, 0f), new Vector3(1.05f, 1.15f, 1.05f), new Color(0.16f, 0.36f, 0.21f));
                        CreatePrimitive(tree.transform, "Upper Canopy", PrimitiveType.Sphere, new Vector3(0f, 4.0f, 0f), new Vector3(0.78f, 0.9f, 0.78f), new Color(0.21f, 0.44f, 0.26f));
                        CollapsePropMeshes(tree, "Park Tree");
                    }
                    else if (token == 'y')
                    {
                        GameObject pine = new GameObject($"Pine Tree [{x}, {worldZ}]");
                        pine.transform.SetParent(parent, false);
                        pine.transform.localPosition = basePosition;
                        CreatePrimitive(pine.transform, "Trunk", PrimitiveType.Cylinder, new Vector3(0f, 0.8f, 0f), new Vector3(0.12f, 0.8f, 0.12f), new Color(0.32f, 0.23f, 0.15f));
                        for (int tier = 0; tier < 4; tier++)
                        {
                            float size = 1.0f - tier * 0.19f;
                            CreatePrimitive(pine.transform, $"Needles {tier + 1}", PrimitiveType.Cylinder, new Vector3(0f, 1.15f + tier * 0.7f, 0f), new Vector3(size, 0.72f, size), new Color(0.12f, 0.32f + tier * 0.025f, 0.2f));
                        }
                        CollapsePropMeshes(pine, "Pine Tree");
                    }
                    else if (token == 'b')
                    {
                        GameObject bench = new GameObject($"Park Bench [{x}, {worldZ}]");
                        bench.transform.SetParent(parent, false);
                        bench.transform.localPosition = basePosition;
                        CreatePrimitive(bench.transform, "Seat", PrimitiveType.Cube, new Vector3(0f, 0.5f, 0f), new Vector3(0.88f, 0.12f, 0.36f), new Color(0.47f, 0.29f, 0.16f));
                        CreatePrimitive(bench.transform, "Back", PrimitiveType.Cube, new Vector3(0f, 0.82f, -0.14f), new Vector3(0.88f, 0.52f, 0.1f), new Color(0.47f, 0.29f, 0.16f));
                        for (int leg = -1; leg <= 1; leg += 2)
                            CreatePrimitive(bench.transform, "Leg", PrimitiveType.Cube, new Vector3(leg * 0.31f, 0.25f, 0f), new Vector3(0.08f, 0.5f, 0.28f), new Color(0.25f, 0.27f, 0.29f));
                        CollapsePropMeshes(bench, "Park Bench");
                    }
                    else if (token == '~')
                    {
                        // Water is authored as part of the continuous terrain surface below.
                    }
                    else if (token == 'f')
                    {
                        GameObject fountain = new GameObject($"Stone Fountain [{x}, {worldZ}]");
                        fountain.transform.SetParent(parent, false);
                        fountain.transform.localPosition = basePosition;
                        Color stone = new Color(0.55f, 0.54f, 0.49f);
                        Color water = new Color(0.06f, 0.43f, 0.55f);
                        CreatePrimitive(fountain.transform, "Lower plinth", PrimitiveType.Cylinder, new Vector3(0f, 0.11f, 0f), new Vector3(0.9f, 0.11f, 0.9f), stone);
                        CreatePrimitive(fountain.transform, "Pedestal", PrimitiveType.Cylinder, new Vector3(0f, 0.5f, 0f), new Vector3(0.27f, 0.42f, 0.27f), stone);
                        CreateMeshObject(fountain.transform, "Hollow stone basin", FountainBasinMesh(), new Vector3(0f, 0.94f, 0f), stone);
                        CreatePrimitive(fountain.transform, "Basin water", PrimitiveType.Cylinder, new Vector3(0f, 0.94f, 0f), new Vector3(0.59f, 0.025f, 0.59f), water);
                        CreatePrimitive(fountain.transform, "Spout", PrimitiveType.Cylinder, new Vector3(0f, 1.3f, 0f), new Vector3(0.09f, 0.34f, 0.09f), stone);
                        CreatePrimitive(fountain.transform, "Water jet", PrimitiveType.Cylinder, new Vector3(0f, 1.72f, 0f), new Vector3(0.035f, 0.34f, 0.035f), water, true);
                        CollapsePropMeshes(fountain, "Stone Fountain");
                    }
                    else if (token == 'w')
                    {
                        GameObject waterfall = new GameObject($"Waterfall [{x}, {worldZ}]");
                        waterfall.transform.SetParent(parent, false);
                        waterfall.transform.localPosition = basePosition;
                        Color rock = new Color(0.34f, 0.38f, 0.39f);
                        Color flow = new Color(0.035f, 0.37f, 0.48f);
                        CreatePrimitive(waterfall.transform, "Left rock shoulder", PrimitiveType.Cube, new Vector3(-0.39f, 3.45f, 0f), new Vector3(0.22f, 6.9f, 0.88f), rock);
                        CreatePrimitive(waterfall.transform, "Right rock shoulder", PrimitiveType.Cube, new Vector3(0.39f, 3.45f, 0f), new Vector3(0.22f, 6.9f, 0.88f), rock);
                        CreatePrimitive(waterfall.transform, "Water curtain", PrimitiveType.Cube, new Vector3(0f, 3.35f, 0.03f), new Vector3(0.61f, 6.5f, 0.18f), flow, true);
                        CreatePrimitive(waterfall.transform, "Rock lip", PrimitiveType.Cube, new Vector3(0f, 6.8f, 0f), new Vector3(1f, 0.28f, 0.9f), rock);
                        CreatePrimitive(waterfall.transform, "Splash pool", PrimitiveType.Cube, new Vector3(0f, 0.12f, 0f), new Vector3(0.98f, 0.2f, 0.98f), new Color(0.04f, 0.3f, 0.39f), true);
                        CollapsePropMeshes(waterfall, "Waterfall");
                    }
                }
            }
            BuildTerrainSurface(parent);
        }

        private void BuildTerrainSurface(Transform parent)
        {
            if (Width <= 0 || rows.Length <= 0) return;
            List<Vector3> vertices = new List<Vector3>();
            Dictionary<string, List<int>> trianglesByRole = new Dictionary<string, List<int>>(StringComparer.Ordinal);
            Dictionary<string, Color> colorsByRole = new Dictionary<string, Color>(StringComparer.Ordinal);
            int stride = Width + 1;
            EnsureTerrainVertexHeights();
            for (int rowIndex = 0; rowIndex < rows.Length; rowIndex++)
            {
                string row = rows[rowIndex] ?? string.Empty;
                int worldZ = rows.Length - 1 - rowIndex;
                for (int x = 0; x < row.Length; x++)
                {
                    string role = TerrainSurfaceRole(row[x]);
                    List<int> triangles;
                    if (!trianglesByRole.TryGetValue(role, out triangles))
                    {
                        triangles = new List<int>();
                        trianglesByRole.Add(role, triangles);
                        colorsByRole.Add(role, TerrainSurfaceColor(role));
                    }
                    float offset = role == "water" ? 0.035f : 0.008f;
                    int first = vertices.Count;
                    vertices.Add(new Vector3(x, terrainVertexHeights[worldZ * stride + x] + offset, worldZ));
                    vertices.Add(new Vector3(x, terrainVertexHeights[worldZ * stride + x + 1] + offset, worldZ));
                    vertices.Add(new Vector3(x + 1, terrainVertexHeights[(worldZ + 1) * stride + x + 1] + offset, worldZ + 1));
                    vertices.Add(new Vector3(x + 1, terrainVertexHeights[(worldZ + 1) * stride + x] + offset, worldZ + 1));
                    triangles.Add(first);
                    triangles.Add(first + 3);
                    triangles.Add(first + 1);
                    triangles.Add(first + 1);
                    triangles.Add(first + 3);
                    triangles.Add(first + 2);
                }
            }

            List<string> roles = trianglesByRole.Keys.OrderBy(value => value, StringComparer.Ordinal).ToList();
            Mesh mesh = new Mesh { name = MakeAssetId(selectedMap + "-terrain-surface") };
            mesh.indexFormat = UnityEngine.Rendering.IndexFormat.UInt32;
            mesh.SetVertices(vertices);
            mesh.subMeshCount = roles.Count;
            for (int subMesh = 0; subMesh < roles.Count; subMesh++)
                mesh.SetTriangles(trianglesByRole[roles[subMesh]], subMesh, false);
            mesh.RecalculateNormals();
            mesh.RecalculateBounds();
            Mesh saved = SaveGeneratedMesh(mesh, selectedMap + "-terrain-surface");
            GameObject surface = CreateMeshObject(parent, "Painted Terrain Surface", saved, Vector3.zero, Color.white);
            surface.GetComponent<MeshRenderer>().sharedMaterials = roles
                .Select(role => PreviewMaterial(colorsByRole[role], false, "Terrain " + role))
                .ToArray();
        }

        private static string TerrainSurfaceRole(char token)
        {
            switch (token)
            {
                case '.': return "road";
                case ',': return "sidewalk";
                case ':': return "alley";
                case '_': return "plaza";
                case 'p':
                case 'g':
                case 'y':
                case 'b':
                case 'i':
                case 'c':
                case 'r': return "park-ground";
                case '=':
                case 'q': return "park-path";
                case 'h': return "bridge";
                case '~':
                case 'w': return "water";
                default: return "city-ground";
            }
        }

        private static Color TerrainSurfaceColor(string role)
        {
            switch (role)
            {
                case "road": return new Color(0.22f, 0.23f, 0.24f);
                case "sidewalk": return new Color(0.38f, 0.39f, 0.40f);
                case "alley": return new Color(0.23f, 0.19f, 0.25f);
                case "plaza": return new Color(0.34f, 0.31f, 0.28f);
                case "park-ground": return new Color(0.16f, 0.29f, 0.19f);
                case "park-path": return new Color(0.35f, 0.30f, 0.23f);
                case "bridge": return new Color(0.32f, 0.25f, 0.18f);
                case "water": return new Color(0.035f, 0.29f, 0.39f);
                default: return new Color(0.10f, 0.11f, 0.13f);
            }
        }

        private float TerrainHeightAt(float worldX, float worldZ)
        {
            int depth = rows.Length;
            if (Width <= 0 || depth <= 0) return 0f;
            EnsureTerrainVertexHeights();
            float x = Mathf.Clamp(worldX, 0f, Width);
            float z = Mathf.Clamp(worldZ, 0f, depth);
            int x0 = Mathf.Min(Mathf.FloorToInt(x), Width - 1);
            int z0 = Mathf.Min(Mathf.FloorToInt(z), depth - 1);
            float tx = x - x0;
            float tz = z - z0;
            int stride = Width + 1;
            float h00 = terrainVertexHeights[z0 * stride + x0];
            float h10 = terrainVertexHeights[z0 * stride + x0 + 1];
            float h01 = terrainVertexHeights[(z0 + 1) * stride + x0];
            float h11 = terrainVertexHeights[(z0 + 1) * stride + x0 + 1];
            return Mathf.Lerp(Mathf.Lerp(h00, h10, tx), Mathf.Lerp(h01, h11, tx), tz);
        }

        private Mesh SaveGeneratedMesh(Mesh generated, string assetName)
        {
            string folder = "Assets/ASCII City/Generated Meshes";
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Generated Meshes");
            string path = folder + "/" + MakeAssetId(assetName) + ".asset";
            Mesh saved = AssetDatabase.LoadAssetAtPath<Mesh>(path);
            if (saved == null)
            {
                generated.name = Path.GetFileNameWithoutExtension(path);
                AssetDatabase.CreateAsset(generated, path);
                return generated;
            }
            if (!refreshedMeshes.Contains(path))
            {
                EditorUtility.CopySerialized(generated, saved);
                saved.name = Path.GetFileNameWithoutExtension(path);
                EditorUtility.SetDirty(saved);
                refreshedMeshes.Add(path);
            }
            DestroyImmediate(generated);
            return saved;
        }

        private void BuildPondSurface(Transform parent, List<Vector3> cells)
        {
            if (cells.Count == 0) return;
            Mesh mesh = new Mesh { name = selectedMap + "-pond" };
            Vector3[] vertices = new Vector3[cells.Count * 4];
            int[] triangles = new int[cells.Count * 6];
            for (int index = 0; index < cells.Count; index++)
            {
                Vector3 center = cells[index];
                int vertex = index * 4;
                vertices[vertex] = new Vector3(center.x - 0.5f, 0.035f, center.z - 0.5f);
                vertices[vertex + 1] = new Vector3(center.x + 0.5f, 0.035f, center.z - 0.5f);
                vertices[vertex + 2] = new Vector3(center.x + 0.5f, 0.035f, center.z + 0.5f);
                vertices[vertex + 3] = new Vector3(center.x - 0.5f, 0.035f, center.z + 0.5f);
                int triangle = index * 6;
                triangles[triangle] = vertex;
                triangles[triangle + 1] = vertex + 2;
                triangles[triangle + 2] = vertex + 1;
                triangles[triangle + 3] = vertex;
                triangles[triangle + 4] = vertex + 3;
                triangles[triangle + 5] = vertex + 2;
            }
            mesh.vertices = vertices;
            mesh.triangles = triangles;
            mesh.RecalculateNormals();
            mesh.RecalculateBounds();
            string folder = "Assets/ASCII City/Generated Meshes";
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Generated Meshes");
            string assetPath = folder + "/" + selectedMap + "-pond.asset";
            Mesh saved = AssetDatabase.LoadAssetAtPath<Mesh>(assetPath);
            if (saved == null)
            {
                AssetDatabase.CreateAsset(mesh, assetPath);
                saved = mesh;
            }
            else
            {
                EditorUtility.CopySerialized(mesh, saved);
                saved.name = selectedMap + "-pond";
                EditorUtility.SetDirty(saved);
                DestroyImmediate(mesh);
            }
            CreateMeshObject(parent, "Continuous Pond Surface", saved, Vector3.zero, new Color(0.035f, 0.29f, 0.39f));
        }

        private static Mesh FountainBasinMesh()
        {
            const int segments = 32;
            const float outerRadius = 0.41f;
            const float innerRadius = 0.30f;
            const float halfHeight = 0.055f;
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Generated Meshes");
            const string assetPath = "Assets/ASCII City/Generated Meshes/fountain-basin-ring.asset";
            Mesh cached = AssetDatabase.LoadAssetAtPath<Mesh>(assetPath);
            if (cached != null) return cached;

            Vector3[] vertices = new Vector3[segments * 4];
            int[] triangles = new int[segments * 24];
            for (int i = 0; i < segments; i++)
            {
                float angle = i * Mathf.PI * 2f / segments;
                float x = Mathf.Cos(angle);
                float z = Mathf.Sin(angle);
                vertices[i] = new Vector3(x * outerRadius, halfHeight, z * outerRadius);
                vertices[segments + i] = new Vector3(x * innerRadius, halfHeight, z * innerRadius);
                vertices[segments * 2 + i] = new Vector3(x * outerRadius, -halfHeight, z * outerRadius);
                vertices[segments * 3 + i] = new Vector3(x * innerRadius, -halfHeight, z * innerRadius);
                int next = (i + 1) % segments;
                AddQuad(triangles, i * 24, i, next, segments + next, segments + i);
                AddQuad(triangles, i * 24 + 6, i, next, segments * 2 + next, segments * 2 + i);
                AddQuad(triangles, i * 24 + 12, segments + i, segments + next, segments * 3 + next, segments * 3 + i);
                AddQuad(triangles, i * 24 + 18, segments * 2 + i, segments * 2 + next, segments * 3 + next, segments * 3 + i);
            }
            Mesh ring = new Mesh { name = "fountain-basin-ring" };
            ring.vertices = vertices;
            ring.triangles = triangles;
            ring.RecalculateNormals();
            ring.RecalculateBounds();
            AssetDatabase.CreateAsset(ring, assetPath);
            return ring;
        }

        private static void AddQuad(int[] triangles, int start, int a, int b, int c, int d)
        {
            triangles[start] = a;
            triangles[start + 1] = b;
            triangles[start + 2] = c;
            triangles[start + 3] = a;
            triangles[start + 4] = c;
            triangles[start + 5] = d;
        }

        private static GameObject CreateMeshObject(Transform parent, string name, Mesh mesh, Vector3 localPosition, Color color)
        {
            GameObject item = new GameObject(name);
            item.transform.SetParent(parent, false);
            item.transform.localPosition = localPosition;
            item.AddComponent<MeshFilter>().sharedMesh = mesh;
            item.AddComponent<MeshRenderer>().sharedMaterial = PreviewMaterial(color, false, name);
            return item;
        }

        private void CollapsePropMeshes(GameObject root, string assetName)
        {
            Dictionary<Material, List<CombineInstance>> byMaterial = new Dictionary<Material, List<CombineInstance>>();
            List<Material> materialOrder = new List<Material>();
            MeshFilter[] filters = root.GetComponentsInChildren<MeshFilter>(true);
            foreach (MeshFilter filter in filters)
            {
                if (filter.transform == root.transform || filter.sharedMesh == null) continue;
                Renderer renderer = filter.GetComponent<Renderer>();
                if (renderer == null) continue;
                Material[] materials = renderer.sharedMaterials;
                for (int subMesh = 0; subMesh < filter.sharedMesh.subMeshCount; subMesh++)
                {
                    if (materials.Length == 0) continue;
                    Material material = materials[Mathf.Min(subMesh, materials.Length - 1)];
                    List<CombineInstance> combines;
                    if (!byMaterial.TryGetValue(material, out combines))
                    {
                        combines = new List<CombineInstance>();
                        byMaterial.Add(material, combines);
                        materialOrder.Add(material);
                    }
                    combines.Add(new CombineInstance
                    {
                        mesh = filter.sharedMesh,
                        subMeshIndex = subMesh,
                        transform = root.transform.worldToLocalMatrix * filter.transform.localToWorldMatrix
                    });
                }
            }

            if (byMaterial.Count == 0) return;
            List<KeyValuePair<Material, List<CombineInstance>>> groups = materialOrder
                .Select(material => new KeyValuePair<Material, List<CombineInstance>>(material, byMaterial[material]))
                .ToList();
            string folder = "Assets/ASCII City/Generated Meshes";
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Generated Meshes");
            string path = folder + "/" + MakeAssetId(assetName) + ".asset";
            Mesh saved = AssetDatabase.LoadAssetAtPath<Mesh>(path);
            if (!refreshedMeshes.Contains(path) || saved == null)
            {
                List<Mesh> materialMeshes = new List<Mesh>();
                List<CombineInstance> finalCombines = new List<CombineInstance>();
                foreach (KeyValuePair<Material, List<CombineInstance>> group in groups)
                {
                    Mesh materialMesh = new Mesh { name = assetName + " " + group.Key.name };
                    materialMesh.CombineMeshes(group.Value.ToArray(), true, true);
                    materialMeshes.Add(materialMesh);
                    finalCombines.Add(new CombineInstance { mesh = materialMesh, subMeshIndex = 0, transform = Matrix4x4.identity });
                }
                Mesh combined = new Mesh { name = MakeAssetId(assetName) };
                combined.CombineMeshes(finalCombines.ToArray(), false, false);
                combined.RecalculateBounds();
                if (saved == null)
                {
                    AssetDatabase.CreateAsset(combined, path);
                    saved = combined;
                }
                else
                {
                    EditorUtility.CopySerialized(combined, saved);
                    saved.name = MakeAssetId(assetName);
                    EditorUtility.SetDirty(saved);
                    DestroyImmediate(combined);
                }
                foreach (Mesh intermediate in materialMeshes)
                    DestroyImmediate(intermediate);
                refreshedMeshes.Add(path);
            }
            root.AddComponent<MeshFilter>().sharedMesh = saved;
            root.AddComponent<MeshRenderer>().sharedMaterials = groups.Select(group => group.Key).ToArray();
            foreach (Transform child in root.transform.Cast<Transform>().ToArray())
                DestroyImmediate(child.gameObject);
        }

        private void BuildLamp(Transform parent, MapObject item)
        {
            GameObject lamp = new GameObject(string.IsNullOrEmpty(item.id) ? "Street Lamp" : "Street Lamp [" + item.id + "]");
            lamp.transform.SetParent(parent, false);
            lamp.transform.localPosition = new Vector3(item.x, 0f, item.z);
            CreatePrimitive(lamp.transform, "Post", PrimitiveType.Cylinder, new Vector3(0f, 1.55f, 0f), new Vector3(0.055f, 1.55f, 0.055f), new Color(0.23f, 0.25f, 0.28f));
            CreatePrimitive(lamp.transform, "Arm", PrimitiveType.Cube, new Vector3(0.28f, 3.0f, 0f), new Vector3(0.56f, 0.07f, 0.07f), new Color(0.23f, 0.25f, 0.28f));
            CreatePrimitive(lamp.transform, "Light", PrimitiveType.Sphere, new Vector3(0.52f, 2.92f, 0f), new Vector3(0.2f, 0.14f, 0.2f), new Color(0.9f, 0.66f, 0.28f), true);
            CollapsePropMeshes(lamp, "Street Lamp");
        }

        private void BuildFacadeArt(Transform parent, FacadeArt[] artItems, Dictionary<string, Color> palette, bool entrance)
        {
            if (artItems == null) return;
            foreach (FacadeArt art in artItems)
            {
                if (art == null || art.widthCells <= 0 || art.height <= 0f) continue;
                Vector3 center;
                float yaw;
                switch ((art.face ?? string.Empty).ToLowerInvariant())
                {
                    case "south": center = new Vector3(art.cellX + art.widthCells * 0.5f, 0f, art.cellZ); yaw = 180f; break;
                    case "east": center = new Vector3(art.cellX + 1f, 0f, art.cellZ + art.widthCells * 0.5f); yaw = 90f; break;
                    case "west": center = new Vector3(art.cellX, 0f, art.cellZ + art.widthCells * 0.5f); yaw = 270f; break;
                    default: center = new Vector3(art.cellX + art.widthCells * 0.5f, 0f, art.cellZ + 1f); yaw = 0f; break;
                }
                center.y = art.bottom + art.height * 0.5f;
                GameObject panel = new GameObject((entrance ? "Entrance " : "Sign ") + (string.IsNullOrEmpty(art.id) ? "(unnamed)" : art.id));
                panel.transform.SetParent(parent, false);
                panel.transform.localPosition = center;
                panel.transform.localRotation = Quaternion.Euler(0f, yaw, 0f);
                Color background = PaletteColor(palette, art.backgroundPalette, entrance ? new Color(0.12f, 0.08f, 0.12f) : new Color(0.1f, 0.06f, 0.12f));
                Color edge = PaletteColor(palette, art.borderPalette, new Color(0.1f, 0.85f, 0.82f));
                CreatePrimitive(panel.transform, "Panel", PrimitiveType.Cube, Vector3.zero, new Vector3(art.widthCells * 0.94f, art.height * 0.94f, 0.12f), background);
                CreatePrimitive(panel.transform, "Top Frame", PrimitiveType.Cube, new Vector3(0f, art.height * 0.49f, -0.07f), new Vector3(art.widthCells, 0.07f, 0.08f), edge, true);
                CreatePrimitive(panel.transform, "Bottom Frame", PrimitiveType.Cube, new Vector3(0f, -art.height * 0.49f, -0.07f), new Vector3(art.widthCells, 0.07f, 0.08f), edge, true);
                string text = !string.IsNullOrWhiteSpace(art.label) ? art.label : string.Join("\n", art.glyphRows ?? new string[0]);
                if (!string.IsNullOrWhiteSpace(text))
                {
                    GameObject lettering = new GameObject("Authored glyph artwork");
                    lettering.transform.SetParent(panel.transform, false);
                    lettering.transform.localPosition = new Vector3(0f, 0f, -0.09f);
                    TextMesh mesh = lettering.AddComponent<TextMesh>();
                    mesh.text = text;
                    mesh.anchor = TextAnchor.MiddleCenter;
                    mesh.alignment = TextAlignment.Center;
                    mesh.font = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
                    mesh.fontSize = 48;
                    mesh.characterSize = Mathf.Min(art.widthCells / Mathf.Max(1, (art.glyphRows != null && art.glyphRows.Length > 0 ? art.glyphRows.Max(line => line.Length) : text.Length)) * 1.65f, art.height / Mathf.Max(1, art.glyphRows == null ? 1 : art.glyphRows.Length) * 1.5f);
                    mesh.color = PaletteColor(palette, art.textPalette, new Color(0.55f, 0.93f, 0.88f));
                }
            }
        }

        private Dictionary<string, Color> LoadPalette()
        {
            Dictionary<string, Color> result = new Dictionary<string, Color>();
            string path = Path.Combine(projectRoot, "assets", "data", "palette.json");
            if (!File.Exists(path)) return result;
            string json = File.ReadAllText(path, Encoding.UTF8);
            foreach (Match match in Regex.Matches(json, "\\\"(?<key>[^\\\"]+)\\\"\\s*:\\s*\\\"(?<value>#[0-9A-Fa-f]{6,8})\\\""))
            {
                Color color;
                if (ColorUtility.TryParseHtmlString(match.Groups["value"].Value, out color))
                    result[match.Groups["key"].Value] = color;
            }
            return result;
        }

        private static Color PaletteColor(Dictionary<string, Color> palette, string key, Color fallback)
        {
            Color color;
            return !string.IsNullOrEmpty(key) && palette.TryGetValue(key, out color) ? color : fallback;
        }

        private static GameObject CreatePrimitive(Transform parent, string name, PrimitiveType type, Vector3 localPosition, Vector3 localScale, Color color, bool emissive = false)
        {
            GameObject item = GameObject.CreatePrimitive(type);
            item.name = name;
            item.transform.SetParent(parent, false);
            item.transform.localPosition = localPosition;
            item.transform.localScale = localScale;
            Collider collider = item.GetComponent<Collider>();
            if (collider != null) DestroyImmediate(collider);
            Renderer renderer = item.GetComponent<Renderer>();
            if (renderer != null)
            {
                string rootName = parent.name;
                int locationStart = rootName.IndexOf(" [", StringComparison.Ordinal);
                if (locationStart >= 0) rootName = rootName.Substring(0, locationStart);
                if (rootName.StartsWith("Sign ", StringComparison.Ordinal)) rootName = "Sign";
                if (rootName.StartsWith("Entrance ", StringComparison.Ordinal)) rootName = "Entrance";
                renderer.sharedMaterial = PreviewMaterial(color, emissive, rootName + " " + name);
            }
            return item;
        }

        private static Material PreviewMaterial(Color color, bool emissive, string semanticName)
        {
            string folder = "Assets/ASCII City/Generated Materials";
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Generated Materials");
            string hex = ColorUtility.ToHtmlStringRGBA(color);
            string label = Regex.Replace(semanticName ?? "World Asset", "[^A-Za-z0-9]+", " ").Trim();
            if (string.IsNullOrEmpty(label)) label = "World Asset";
            string baseName = Regex.Replace(label, "\\s+", "-").ToLowerInvariant() + (emissive ? "-emissive" : string.Empty);
            string path = folder + "/" + baseName + ".mat";
            Shader shader = PreviewShader();
            Material material = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (material != null && material.color == color)
            {
                if (material.shader != shader)
                    material.shader = shader;
                if (material.name != baseName)
                {
                    material.name = baseName;
                    EditorUtility.SetDirty(material);
                }
                return material;
            }
            if (material != null)
            {
                path = folder + "/" + baseName + "-" + hex + ".mat";
                material = AssetDatabase.LoadAssetAtPath<Material>(path);
                if (material != null)
                {
                    if (material.shader != shader)
                        material.shader = shader;
                    if (material.color != color)
                        material.color = color;
                    if (material.name != Path.GetFileNameWithoutExtension(path))
                    {
                        material.name = Path.GetFileNameWithoutExtension(path);
                        EditorUtility.SetDirty(material);
                    }
                    return material;
                }
            }
            string oldHashPath = folder + "/preview-" + hex + (emissive ? "-emissive" : string.Empty) + ".mat";
            Material oldMaterial = AssetDatabase.LoadAssetAtPath<Material>(oldHashPath);
            if (oldMaterial != null)
            {
                string moveError = AssetDatabase.MoveAsset(oldHashPath, path);
                if (string.IsNullOrEmpty(moveError))
                {
                    oldMaterial.name = Path.GetFileNameWithoutExtension(path);
                    oldMaterial.shader = shader;
                    EditorUtility.SetDirty(oldMaterial);
                    return oldMaterial;
                }
            }
            if (shader == null) return null;
            material = new Material(shader) { color = color, name = Path.GetFileNameWithoutExtension(path) };
            if (emissive)
            {
                material.EnableKeyword("_EMISSION");
                material.SetColor("_EmissionColor", color * 1.8f);
            }
            AssetDatabase.CreateAsset(material, path);
            return material;
        }

        private static Shader PreviewShader()
        {
            return Shader.Find("Unlit/Color") ??
                Shader.Find("Universal Render Pipeline/Unlit") ??
                Shader.Find("Standard");
        }

        private static void EnsureAssetFolder(string parent, string name)
        {
            string path = parent + "/" + name;
            if (!AssetDatabase.IsValidFolder(path))
                AssetDatabase.CreateFolder(parent, name);
        }

        private void SyncLoadedPreview()
        {
            foreach (AsciiCityMapPreview preview in FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None))
            {
                if (preview.mapId == selectedMap)
                {
                    preview.rowsNorthToSouth = (string[])rows.Clone();
                    preview.cellTypes = BuildPreviewCellTypes();
                    EnsureTerrainVertexHeights();
                    preview.terrainVertexHeightsSouthToNorth = (float[])terrainVertexHeights.Clone();
                    EditorUtility.SetDirty(preview);
                    SceneView.RepaintAll();
                }
            }
        }

        private AsciiCityCellPreviewData[] BuildPreviewCellTypes()
        {
            return cellDefinitions.Select(cell => new AsciiCityCellPreviewData
            {
                token = cell.token,
                height = cell.height,
                solid = cell.solid
            }).ToArray();
        }

        private static string MakeAssetId(string value)
        {
            string normalized = Regex.Replace(value.Trim().ToLowerInvariant(), "[^a-z0-9]+", "-").Trim('-');
            return string.IsNullOrEmpty(normalized) ? "unity-asset" : normalized;
        }

        private void PlacePrefabAtCell(int x, int rowIndex)
        {
            if (prefabToPlace == null || string.IsNullOrWhiteSpace(prefabAssetId) || rowIndex < 0 || rowIndex >= rows.Length)
                return;
            int stampKey = rowIndex * Math.Max(1, Width) + x;
            if (prefabBrushEnabled && !stampedPrefabCells.Add(stampKey))
                return;

            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            if (preview == null)
            {
                EditorUtility.DisplayDialog("No map scene", "Create / sync the map scene first.", "OK");
                return;
            }

            GameObject instance = (GameObject)PrefabUtility.InstantiatePrefab(prefabToPlace, preview.gameObject.scene);
            Undo.RegisterCreatedObjectUndo(instance, "Place ASCII City prefab");
            instance.transform.SetParent(preview.transform, true);
            int worldZ = rows.Length - 1 - rowIndex;
            float terrainHeight = preview.TerrainHeightAt(x + 0.5f, worldZ + 0.5f, Width, rows.Length);
            instance.transform.position = preview.transform.position + new Vector3(x + 0.5f, terrainHeight, worldZ + 0.5f);
            AsciiCityAssetPlacement placement = instance.GetComponent<AsciiCityAssetPlacement>();
            if (placement == null) placement = Undo.AddComponent<AsciiCityAssetPlacement>(instance);
            else Undo.RecordObject(placement, "Set ASCII City placement metadata");
            if (string.IsNullOrWhiteSpace(placement.placementId))
                placement.placementId = "placement." + Guid.NewGuid().ToString("N");
            placement.assetId = prefabAssetId.Trim();
            placement.blocksMovement = prefabBlocksMovement;
            placement.collisionRadius = Mathf.Max(0f, prefabCollisionRadius);
            selectedX = x;
            selectedRow = rowIndex;
            Selection.activeGameObject = instance;
            EditorSceneManager.MarkSceneDirty(instance.scene);
            Repaint();
        }

        private void ExportPlacements()
        {
            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            if (preview == null)
            {
                EditorUtility.DisplayDialog("No map scene", "Create / sync the map scene and place assets before exporting.", "OK");
                return;
            }

            AsciiCityAssetPlacement[] scenePlacements = preview.GetComponentsInChildren<AsciiCityAssetPlacement>(true)
                .Where(item => !string.IsNullOrWhiteSpace(item.assetId))
                .OrderBy(item => item.assetId, StringComparer.Ordinal)
                .ThenBy(item => item.transform.position.x)
                .ThenBy(item => item.transform.position.z)
                .ToArray();
            HashSet<string> placementIds = new HashSet<string>(StringComparer.Ordinal);
            List<Placement> placements = new List<Placement>(scenePlacements.Length);
            foreach (AsciiCityAssetPlacement item in scenePlacements)
            {
                if (string.IsNullOrWhiteSpace(item.placementId) || !placementIds.Add(item.placementId))
                {
                    item.placementId = "placement." + Guid.NewGuid().ToString("N");
                    placementIds.Add(item.placementId);
                    EditorUtility.SetDirty(item);
                    EditorSceneManager.MarkSceneDirty(item.gameObject.scene);
                }
                EnsureAsciiSpriteProfile(item);
                placements.Add(new Placement
                {
                    id = item.placementId,
                    assetId = item.assetId,
                    x = item.transform.position.x - preview.transform.position.x,
                    y = item.transform.position.y - preview.transform.position.y,
                    z = item.transform.position.z - preview.transform.position.z,
                    yawDegrees = item.transform.eulerAngles.y,
                    scaleX = item.transform.lossyScale.x,
                    scaleY = item.transform.lossyScale.y,
                    scaleZ = item.transform.lossyScale.z,
                    blocksMovement = item.blocksMovement,
                    collisionRadius = item.collisionRadius
                });
            }
            AssetSpriteProfile[] profiles = scenePlacements
                .GroupBy(item => item.assetId, StringComparer.Ordinal)
                .Select(group => BuildAsciiSpriteProfile(group.First()))
                .OrderBy(profile => profile.assetId, StringComparer.Ordinal)
                .ToArray();
            PlacementFile file = new PlacementFile { version = 1, assets = profiles, placements = placements.ToArray() };
            File.WriteAllText(MapPath(selectedMap, ".unity-assets.json"), JsonUtility.ToJson(file, true) + "\n", new UTF8Encoding(false));
            AssetDatabase.Refresh();
            EditorUtility.DisplayDialog("Placements exported", $"Exported {placements.Count} placement(s) and {profiles.Length} ASCII model profile(s). The running game will render these placements as ASCII assets.", "OK");
        }

        private static void EnsureAsciiSpriteProfile(AsciiCityAssetPlacement placement)
        {
            if (placement.glyphRows != null && placement.glyphRows.Length > 0 &&
                placement.colorRows != null && placement.colorRows.Length == placement.glyphRows.Length &&
                placement.colors != null && placement.colors.Length > 0)
            {
                if (string.IsNullOrWhiteSpace(placement.spriteId))
                    placement.spriteId = "sprite.unity." + MakeAssetId(placement.assetId);
                return;
            }

            string assetId = (placement.assetId ?? string.Empty).ToLowerInvariant();
            placement.spriteId = "sprite.unity." + MakeAssetId(placement.assetId ?? placement.gameObject.name);
            if (assetId.Contains("park-tree"))
            {
                placement.asciiWidth = 1.8f; placement.asciiHeight = 4.8f;
                placement.glyphRows = new[] { "  /\\  ", " /YY\\ ", "/YYYY\\", "  ||  " };
                placement.colorRows = new[] { "  gg  ", " gggg ", "gggggg", "  tt  " };
                placement.colors = new[] { new AsciiCitySpriteColor { symbol = "g", palette = "neonGreen" }, new AsciiCitySpriteColor { symbol = "t", palette = "parkTrunk" } };
            }
            else if (assetId.Contains("pine-tree"))
            {
                placement.asciiWidth = 1.8f; placement.asciiHeight = 4.4f;
                placement.glyphRows = new[] { "   ^   ", "  /Y\\  ", " /YYY\\ ", "/YYYYY\\", "   |   " };
                placement.colorRows = new[] { "   g   ", "  ggg  ", " ggggg ", "ggggggg", "   t   " };
                placement.colors = new[] { new AsciiCitySpriteColor { symbol = "g", palette = "neonGreen" }, new AsciiCitySpriteColor { symbol = "t", palette = "parkTrunk" } };
            }
            else if (assetId.Contains("bench"))
            {
                placement.asciiWidth = 1.2f; placement.asciiHeight = 1.1f;
                placement.glyphRows = new[] { "  _____  ", " |=====| ", " o     o " };
                placement.colorRows = new[] { "  wwwww  ", " mwwwwwm ", " m     m " };
                placement.colors = new[] { new AsciiCitySpriteColor { symbol = "m", palette = "parkMetal" }, new AsciiCitySpriteColor { symbol = "w", palette = "parkWood" } };
            }
            else if (assetId.Contains("building"))
            {
                placement.asciiWidth = 3.7f; placement.asciiHeight = 5.7f;
                placement.glyphRows = new[] { "  +---+  ", "  |###|  ", "  |###|  ", "+-+---+-+", "|W|###|W|", "|W|   |W|", "+-------+" };
                placement.colorRows = new[] { "  mmmmm  ", "  mcccm  ", "  mcccm  ", "mmmaaammm", "mcmcccmcm", "mcm   mcm", "mmmmmmmmm" };
                placement.colors = new[]
                {
                    new AsciiCitySpriteColor { symbol = "m", palette = "wallFront" },
                    new AsciiCitySpriteColor { symbol = "c", palette = "windowLit" },
                    new AsciiCitySpriteColor { symbol = "a", palette = "neonAmber" }
                };
            }
            else if (assetId.Contains("fountain"))
            {
                placement.asciiWidth = 1.8f; placement.asciiHeight = 2.2f;
                placement.glyphRows = new[] { "    ^    ", "   /#\\   ", "  /###\\  ", " |~~~~~| ", "  |###|  ", " _|___|_ " };
                placement.colorRows = new[] { "    c    ", "   c#c   ", "  #####  ", " mwwwwwm ", "  m###m  ", " mmmmmmm " };
                placement.colors = new[] { new AsciiCitySpriteColor { symbol = "c", palette = "parkWater" }, new AsciiCitySpriteColor { symbol = "#", palette = "parkStone" }, new AsciiCitySpriteColor { symbol = "m", palette = "parkStone" }, new AsciiCitySpriteColor { symbol = "w", palette = "neonCyan" } };
            }
            else if (assetId.Contains("waterfall"))
            {
                placement.asciiWidth = 1.4f; placement.asciiHeight = 6.9f;
                placement.glyphRows = new[] { "|~~~~~~~|", "|~~~~~~~|", "|~~~~~~~|", "|~~~~~~~|", "|~~~~~~~|", "|~~~~~~~|", "\\_______/" };
                placement.colorRows = new[] { "mcccccccm", "mcccccccm", "mcccccccm", "mcccccccm", "mcccccccm", "mcccccccm", "mmmmmmmmm" };
                placement.colors = new[] { new AsciiCitySpriteColor { symbol = "m", palette = "parkStone" }, new AsciiCitySpriteColor { symbol = "c", palette = "neonCyan" } };
            }
            else if (assetId.Contains("lamp"))
            {
                placement.asciiWidth = 0.9f; placement.asciiHeight = 3.1f;
                placement.glyphRows = new[] { "   ( )   ", "    |    ", "    |    ", "    |    ", "   /|    " };
                placement.colorRows = new[] { "   aaa   ", "    m    ", "    m    ", "    m    ", "   mm    " };
                placement.colors = new[] { new AsciiCitySpriteColor { symbol = "a", palette = "neonAmber" }, new AsciiCitySpriteColor { symbol = "m", palette = "parkMetal" } };
            }
            else
            {
                placement.asciiWidth = 1.5f; placement.asciiHeight = 2.2f;
                placement.glyphRows = new[] { "+-------+", "|#######|", "|#######|", "|#######|", "+-------+" };
                placement.colorRows = new[] { "mmmmmmmmm", "mcccccccm", "mcccccccm", "mcccccccm", "mmmmmmmmm" };
                placement.colors = new[] { new AsciiCitySpriteColor { symbol = "m", palette = "parkStone" }, new AsciiCitySpriteColor { symbol = "c", palette = "neonCyan" } };
            }
            placement.asciiAnchorX = 0.5f;
            placement.asciiAnchorY = 0f;
            EditorUtility.SetDirty(placement);
        }

        private static AssetSpriteProfile BuildAsciiSpriteProfile(AsciiCityAssetPlacement placement)
        {
            EnsureAsciiSpriteProfile(placement);
            return new AssetSpriteProfile
            {
                assetId = placement.assetId,
                spriteId = placement.spriteId,
                physicalWidth = placement.asciiWidth,
                physicalHeight = placement.asciiHeight,
                anchorX = placement.asciiAnchorX,
                anchorY = placement.asciiAnchorY,
                glyphRows = (string[])placement.glyphRows.Clone(),
                colorRows = (string[])placement.colorRows.Clone(),
                colors = placement.colors.Select(color => new AsciiCitySpriteColor { symbol = color.symbol, palette = color.palette }).ToArray()
            };
        }

        private void FocusMapInSceneView()
        {
            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            if (preview == null || SceneView.lastActiveSceneView == null) return;
            float size = Mathf.Max(Width, rows.Length) * 0.7f;
            SceneView.lastActiveSceneView.LookAt(preview.transform.position + new Vector3(Width * 0.5f, 0f, rows.Length * 0.5f), Quaternion.Euler(38f, 0f, 0f), size);
        }

        private string MapPath(string mapId, string extension)
        {
            return Path.Combine(projectRoot, "assets", "maps", mapId, mapId + extension);
        }
    }
}
