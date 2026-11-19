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
        [Serializable] private sealed class PlacementFile { public Placement[] placements; }
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

        private string projectRoot = string.Empty;
        private string selectedMap = string.Empty;
        private string[] mapIds = new string[0];
        private string[] rows = new string[0];
        private CellDefinition[] cellDefinitions = new CellDefinition[0];
        private string selectedToken = ".";
        private int selectedX = -1;
        private int selectedRow = -1;
        private bool isLoaded;
        private Vector2 paintScroll;
        private float cellButtonSize = 22f;
        private string selectedCategory = "Ground & Streets";
        private GameObject prefabToPlace;
        private string prefabAssetId = string.Empty;
        private bool prefabBlocksMovement;
        private float prefabCollisionRadius = 0.5f;

        [MenuItem("Tools/ASCII City/World Authoring")]
        private static void Open()
        {
            GetWindow<AsciiCityWorldWindow>("ASCII City");
        }

        private void OnEnable()
        {
            minSize = new Vector2(540f, 420f);
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
                if (GUILayout.Button("Save map", GUILayout.Width(80f)))
                    SaveMap();
                if (GUILayout.Button("Create / sync scene", GUILayout.Width(135f)))
                    CreateOrSyncScene();
            }
            EditorGUILayout.HelpBox("Rows are north-to-south. Grid cell (x, row) maps to world position (x + 0.5, z = depth - row - 0.5).", MessageType.None);
            EditorGUILayout.HelpBox("The Scene view shows Unity preview meshes for map-authored trees, benches, lamps, signs, and entrances. Create / sync scene refreshes those objects after map edits.", MessageType.Info);
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
                            PaintCell(x, rowIndex);
                            currentEvent.Use();
                        }
                        else if (currentEvent.type == EventType.MouseDrag && currentEvent.button == 0 && GUIUtility.hotControl == paintControl && hovered)
                        {
                            PaintCell(x, rowIndex);
                            currentEvent.Use();
                        }
                        else if (currentEvent.type == EventType.MouseUp && GUIUtility.hotControl == paintControl)
                        {
                            GUIUtility.hotControl = 0;
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
            EditorGUILayout.LabelField("Place Unity prefab", EditorStyles.boldLabel);
            prefabToPlace = (GameObject)EditorGUILayout.ObjectField("Prefab", prefabToPlace, typeof(GameObject), false);
            prefabAssetId = EditorGUILayout.TextField("Stable asset ID", prefabAssetId);
            prefabBlocksMovement = EditorGUILayout.Toggle("Blocks movement", prefabBlocksMovement);
            using (new EditorGUILayout.HorizontalScope())
            {
                prefabCollisionRadius = EditorGUILayout.FloatField("Collision radius", prefabCollisionRadius);
                using (new EditorGUI.DisabledScope(prefabToPlace == null || selectedX < 0 || string.IsNullOrWhiteSpace(prefabAssetId)))
                {
                    if (GUILayout.Button("Place at selected cell", GUILayout.Width(170f)))
                        PlacePrefab();
                }
            }
            EditorGUILayout.LabelField("Move and rotate placed prefabs in the Scene view. Save the scene, then export placements from this window.", EditorStyles.wordWrappedMiniLabel);
            using (new EditorGUILayout.HorizontalScope())
            {
                if (GUILayout.Button("Export scene placements", GUILayout.Width(170f)))
                    ExportPlacements();
                if (GUILayout.Button("Focus map in Scene view", GUILayout.Width(170f)))
                    FocusMapInSceneView();
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
            CellCatalog catalog = JsonUtility.FromJson<CellCatalog>(File.ReadAllText(cellsPath, Encoding.UTF8));
            cellDefinitions = catalog != null && catalog.cells != null ? catalog.cells : new CellDefinition[0];
            if (!cellDefinitions.Any(cell => cell.token == selectedToken))
                selectedToken = cellDefinitions.Length > 0 ? cellDefinitions[0].token : ".";
            selectedX = -1;
            selectedRow = -1;
            isLoaded = rows.Length > 0;
            SyncLoadedPreview();
            Repaint();
        }

        private void SaveMap()
        {
            if (!HasRectangularRows())
            {
                EditorUtility.DisplayDialog("Cannot save map", "All rows must have the same width.", "OK");
                return;
            }
            File.WriteAllText(MapPath(selectedMap, ".txt"), string.Join("\n", rows) + "\n", new UTF8Encoding(false));
            SyncLoadedPreview();
            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            if (preview != null)
            {
                RebuildAuthoredDetails(preview);
                EditorSceneManager.MarkSceneDirty(preview.gameObject.scene);
            }
            AssetDatabase.Refresh();
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
                Selection.activeGameObject = root;
                EditorSceneManager.SaveScene(scene, sceneAssetPath);
            }

            preview.mapId = selectedMap;
            preview.rowsNorthToSouth = (string[])rows.Clone();
            preview.cellTypes = BuildPreviewCellTypes();
            RebuildAuthoredDetails(preview);
            EditorUtility.SetDirty(preview);
            EditorSceneManager.MarkSceneDirty(preview.gameObject.scene);
            FocusMapInSceneView();
            EditorUtility.DisplayDialog("Scene ready", "The 3D map scene is ready. Use Place at selected cell, then move and rotate prefab instances with the Scene view tools.", "OK");
        }

        private void RebuildAuthoredDetails(AsciiCityMapPreview preview)
        {
            Transform oldDetails = preview.transform.Find("__Authored Details");
            if (oldDetails != null)
                DestroyImmediate(oldDetails.gameObject);
            GameObject root = new GameObject("__Authored Details");
            root.transform.SetParent(preview.transform, false);

            Dictionary<string, Color> palette = LoadPalette();
            BuildCellMeshes(root.transform);
            string objectPath = MapPath(selectedMap, ".objects.json");
            if (!File.Exists(objectPath)) return;
            MapObjectFile authored = JsonUtility.FromJson<MapObjectFile>(File.ReadAllText(objectPath, Encoding.UTF8));
            if (authored == null) return;
            if (authored.objects != null)
                foreach (MapObject item in authored.objects)
                    if (item != null && item.type == "lamp") BuildLamp(root.transform, item);
            BuildFacadeArt(root.transform, authored.facadeSigns, palette, false);
            BuildFacadeArt(root.transform, authored.facadeEntrances, palette, true);
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
                    Vector3 basePosition = new Vector3(x + 0.5f, 0f, worldZ + 0.5f);
                    if (token == 'g')
                    {
                        GameObject tree = new GameObject($"Park Tree [{x}, {worldZ}]");
                        tree.transform.SetParent(parent, false);
                        tree.transform.localPosition = basePosition;
                        CreatePrimitive(tree.transform, "Trunk", PrimitiveType.Cylinder, new Vector3(0f, 1.05f, 0f), new Vector3(0.15f, 1.05f, 0.15f), new Color(0.33f, 0.22f, 0.14f));
                        CreatePrimitive(tree.transform, "Lower Canopy", PrimitiveType.Sphere, new Vector3(0f, 3.2f, 0f), new Vector3(1.05f, 1.15f, 1.05f), new Color(0.16f, 0.36f, 0.21f));
                        CreatePrimitive(tree.transform, "Upper Canopy", PrimitiveType.Sphere, new Vector3(0f, 4.0f, 0f), new Vector3(0.78f, 0.9f, 0.78f), new Color(0.21f, 0.44f, 0.26f));
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
                    }
                    else if (token == '~')
                    {
                        GameObject water = new GameObject($"Pond Water [{x}, {worldZ}]");
                        water.transform.SetParent(parent, false);
                        water.transform.localPosition = basePosition;
                        CreatePrimitive(water.transform, "Water surface", PrimitiveType.Cube, new Vector3(0f, 0.025f, 0f), new Vector3(1f, 0.045f, 1f), new Color(0.035f, 0.29f, 0.39f));
                        float glintOffset = ((x * 17 + worldZ * 31) % 9) * 0.045f - 0.18f;
                        CreatePrimitive(water.transform, "Water glint", PrimitiveType.Cube, new Vector3(glintOffset, 0.051f, 0.13f), new Vector3(0.3f, 0.008f, 0.025f), new Color(0.12f, 0.58f, 0.62f), true);
                    }
                    else if (token == 'f')
                    {
                        GameObject fountain = new GameObject($"Stone Fountain [{x}, {worldZ}]");
                        fountain.transform.SetParent(parent, false);
                        fountain.transform.localPosition = basePosition;
                        Color stone = new Color(0.55f, 0.54f, 0.49f);
                        Color water = new Color(0.06f, 0.43f, 0.55f);
                        CreatePrimitive(fountain.transform, "Lower plinth", PrimitiveType.Cylinder, new Vector3(0f, 0.13f, 0f), new Vector3(0.92f, 0.13f, 0.92f), stone);
                        CreatePrimitive(fountain.transform, "Pedestal", PrimitiveType.Cylinder, new Vector3(0f, 0.53f, 0f), new Vector3(0.33f, 0.42f, 0.33f), stone);
                        CreatePrimitive(fountain.transform, "Basin rim", PrimitiveType.Cylinder, new Vector3(0f, 0.99f, 0f), new Vector3(0.82f, 0.11f, 0.82f), stone);
                        CreatePrimitive(fountain.transform, "Basin water", PrimitiveType.Cylinder, new Vector3(0f, 1.065f, 0f), new Vector3(0.66f, 0.025f, 0.66f), water, true);
                        CreatePrimitive(fountain.transform, "Spout", PrimitiveType.Cylinder, new Vector3(0f, 1.42f, 0f), new Vector3(0.11f, 0.38f, 0.11f), stone);
                        CreatePrimitive(fountain.transform, "Water jet", PrimitiveType.Cylinder, new Vector3(0f, 1.73f, 0f), new Vector3(0.045f, 0.25f, 0.045f), water, true);
                        CreatePrimitive(fountain.transform, "Finial", PrimitiveType.Sphere, new Vector3(0f, 1.97f, 0f), new Vector3(0.12f, 0.16f, 0.12f), stone);
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
                    }
                }
            }
        }

        private static void BuildLamp(Transform parent, MapObject item)
        {
            GameObject lamp = new GameObject(string.IsNullOrEmpty(item.id) ? "Street Lamp" : item.id);
            lamp.transform.SetParent(parent, false);
            lamp.transform.localPosition = new Vector3(item.x, 0f, item.z);
            CreatePrimitive(lamp.transform, "Post", PrimitiveType.Cylinder, new Vector3(0f, 1.55f, 0f), new Vector3(0.055f, 1.55f, 0.055f), new Color(0.23f, 0.25f, 0.28f));
            CreatePrimitive(lamp.transform, "Arm", PrimitiveType.Cube, new Vector3(0.28f, 3.0f, 0f), new Vector3(0.56f, 0.07f, 0.07f), new Color(0.23f, 0.25f, 0.28f));
            CreatePrimitive(lamp.transform, "Light", PrimitiveType.Sphere, new Vector3(0.52f, 2.92f, 0f), new Vector3(0.2f, 0.14f, 0.2f), new Color(0.9f, 0.66f, 0.28f), true);
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
                    mesh.font = Resources.GetBuiltinResource<Font>("Arial.ttf");
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
                renderer.sharedMaterial = PreviewMaterial(color, emissive);
            return item;
        }

        private static Material PreviewMaterial(Color color, bool emissive)
        {
            string folder = "Assets/ASCII City/Generated Materials";
            EnsureAssetFolder("Assets", "ASCII City");
            EnsureAssetFolder("Assets/ASCII City", "Generated Materials");
            string hex = ColorUtility.ToHtmlStringRGBA(color);
            string path = folder + "/preview-" + hex + (emissive ? "-emissive" : string.Empty) + ".mat";
            Material material = AssetDatabase.LoadAssetAtPath<Material>(path);
            if (material != null) return material;
            Shader shader = Shader.Find("Standard");
            if (shader == null) return null;
            material = new Material(shader) { color = color };
            if (emissive)
            {
                material.EnableKeyword("_EMISSION");
                material.SetColor("_EmissionColor", color * 1.8f);
            }
            AssetDatabase.CreateAsset(material, path);
            return material;
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

        private void PlacePrefab()
        {
            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            if (preview == null)
            {
                EditorUtility.DisplayDialog("No map scene", "Create / sync the map scene first.", "OK");
                return;
            }

            GameObject instance = (GameObject)PrefabUtility.InstantiatePrefab(prefabToPlace, preview.gameObject.scene);
            instance.transform.SetParent(preview.transform, true);
            int worldZ = rows.Length - 1 - selectedRow;
            instance.transform.position = preview.transform.position + new Vector3(selectedX + 0.5f, 0f, worldZ + 0.5f);
            AsciiCityAssetPlacement placement = instance.GetComponent<AsciiCityAssetPlacement>();
            if (placement == null) placement = instance.AddComponent<AsciiCityAssetPlacement>();
            placement.assetId = prefabAssetId.Trim();
            placement.blocksMovement = prefabBlocksMovement;
            placement.collisionRadius = Mathf.Max(0f, prefabCollisionRadius);
            Selection.activeGameObject = instance;
            EditorSceneManager.MarkSceneDirty(instance.scene);
        }

        private void ExportPlacements()
        {
            AsciiCityMapPreview preview = FindObjectsByType<AsciiCityMapPreview>(FindObjectsSortMode.None).FirstOrDefault(item => item.mapId == selectedMap);
            if (preview == null)
            {
                EditorUtility.DisplayDialog("No map scene", "Create / sync the map scene and place assets before exporting.", "OK");
                return;
            }

            Placement[] placements = preview.GetComponentsInChildren<AsciiCityAssetPlacement>(true)
                .Select(item => new Placement
                {
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
                })
                .OrderBy(item => item.assetId, StringComparer.Ordinal)
                .ToArray();
            PlacementFile file = new PlacementFile { placements = placements };
            File.WriteAllText(MapPath(selectedMap, ".unity-assets.json"), JsonUtility.ToJson(file, true) + "\n", new UTF8Encoding(false));
            AssetDatabase.Refresh();
            EditorUtility.DisplayDialog("Placements exported", $"Exported {placements.Length} placement(s). The sidecar is authoring data; the current Kotlin runtime does not load it yet.", "OK");
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
