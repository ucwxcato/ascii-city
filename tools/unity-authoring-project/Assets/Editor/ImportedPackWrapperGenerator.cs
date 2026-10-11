using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text.RegularExpressions;
using AsciiCity.WorldAuthoring;
using UnityEditor;
using UnityEditor.SceneManagement;
using UnityEngine;
using UnityEngine.SceneManagement;

[InitializeOnLoad]
internal static class ImportedPackWrapperGenerator
{
    // Creates reusable model-prefab wrappers from the imported source FBX packs.
    private sealed class Pack
    {
        public readonly string name;
        public readonly string prefix;
        public readonly string sourceFolder;
        public readonly int expectedModels;

        public Pack(string name, string prefix, string sourceFolder, int expectedModels)
        {
            this.name = name;
            this.prefix = prefix;
            this.sourceFolder = sourceFolder;
            this.expectedModels = expectedModels;
        }
    }

    private sealed class ModelWork
    {
        public string sourcePath;
        public string prefabPath;
        public string prefabName;
        public string assetId;
        public GameObject model;
    }

    private static readonly Pack[] Packs =
    {
        new Pack("Kenney City Kit Commercial 2.1", "Commercial", "Assets/Third Party/Imported Packs/Kenney City Kit Commercial 2.1/Models/FBX format", 41),
        new Pack("Kenney City Kit Industrial 2.0", "Industrial", "Assets/Third Party/Imported Packs/Kenney City Kit Industrial 2.0/Models/FBX format", 37),
        new Pack("Quaternius Modular SciFi MegaKit Standard", "Sci-Fi", "Assets/Third Party/Imported Packs/Quaternius Modular SciFi MegaKit Standard/FBX (Unity)", 189),
        new Pack("Nature FBX Set", "Nature", "Assets/Third Party/Imported Packs/Nature FBX Set/FBX", 63),
        new Pack("Quaternius Downtown City MegaKit Standard", "Downtown", "Assets/Third Party/Imported Packs/Quaternius Downtown City MegaKit Standard/Exports/FBX (Unity)", 153)
    };

    private const string WrapperRoot = "Assets/ASCII City/Placeable Assets/Imported Packs";
    private const string RequestMarker = "Temp/ascii-city-create-imported-pack-wrappers.once";
    private static readonly List<ModelWork> Pending = new List<ModelWork>();
    private static Scene previewScene;
    private static int nextWorkIndex;
    private static int pollCount;
    private static double nextReadinessPoll;
    private static bool readinessPolling;
    private static bool generationRunning;

    static ImportedPackWrapperGenerator()
    {
        EditorApplication.delayCall += BeginRequestedGeneration;
    }

    [MenuItem("Tools/ASCII City/Generate Imported Pack Wrapper Prefabs")]
    private static void GenerateFromMenu()
    {
        BeginReadinessPolling(false);
    }

    private static void BeginRequestedGeneration()
    {
        if (File.Exists(ProjectPath(RequestMarker)))
            BeginReadinessPolling(true);
    }

    private static void BeginReadinessPolling(bool removeRequestMarker)
    {
        if (generationRunning || readinessPolling)
            return;

        if (removeRequestMarker)
        {
            string marker = ProjectPath(RequestMarker);
            if (File.Exists(marker))
                File.Delete(marker);
        }

        readinessPolling = true;
        pollCount = 0;
        nextReadinessPoll = 0;
        EditorApplication.update += PollForImportedModels;
    }

    private static void PollForImportedModels()
    {
        if (EditorApplication.timeSinceStartup < nextReadinessPoll)
            return;

        nextReadinessPoll = EditorApplication.timeSinceStartup + 1.0;
        pollCount++;
        Pending.Clear();

        foreach (Pack pack in Packs)
        {
            string sourceDirectory = ProjectPath(pack.sourceFolder);
            if (!Directory.Exists(sourceDirectory))
            {
                if (pollCount >= 120)
                    AbortReadiness("Missing imported model folder: " + pack.sourceFolder);
                return;
            }

            string[] sourceFiles = Directory.GetFiles(sourceDirectory, "*.fbx", SearchOption.AllDirectories)
                .OrderBy(path => path, StringComparer.OrdinalIgnoreCase)
                .ToArray();

            if (sourceFiles.Length != pack.expectedModels)
            {
                if (pollCount >= 120)
                    AbortReadiness(pack.name + " has " + sourceFiles.Length + " FBX files; expected " + pack.expectedModels + ".");
                return;
            }

            string sourcePrefix = sourceDirectory.TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar) + Path.DirectorySeparatorChar;
            foreach (string sourceFile in sourceFiles)
            {
                string sourcePath = AbsolutePathToAssetPath(sourceFile);
                GameObject model = AssetDatabase.LoadAssetAtPath<GameObject>(sourcePath);
                if (model == null)
                {
                    if (pollCount >= 120)
                        AbortReadiness("Unity has not imported model asset: " + sourcePath);
                    return;
                }

                string relativePath = sourceFile.Substring(sourcePrefix.Length).Replace('\\', '/');
                string relativeDirectory = Path.GetDirectoryName(relativePath)?.Replace('\\', '/');
                string outputDirectory = WrapperRoot + "/" + pack.name;
                if (!string.IsNullOrEmpty(relativeDirectory))
                    outputDirectory += "/" + relativeDirectory;

                string modelName = Path.GetFileNameWithoutExtension(sourceFile);
                string prefabName = pack.prefix + " - " + modelName;
                Pending.Add(new ModelWork
                {
                    sourcePath = sourcePath,
                    prefabPath = outputDirectory + "/" + prefabName + ".prefab",
                    prefabName = prefabName,
                    assetId = MakeAssetId(pack, modelName, relativePath),
                    model = model
                });
            }
        }

        readinessPolling = false;
        EditorApplication.update -= PollForImportedModels;
        StartGeneration();
    }

    private static void AbortReadiness(string reason)
    {
        readinessPolling = false;
        EditorApplication.update -= PollForImportedModels;
        Debug.LogError("ASCII City wrapper prefab generation stopped: " + reason);
    }

    private static void StartGeneration()
    {
        generationRunning = true;
        nextWorkIndex = 0;
        EnsureUnityFolder(WrapperRoot);
        previewScene = EditorSceneManager.NewPreviewScene();
        EditorApplication.update += ProcessBatch;
    }

    private static void ProcessBatch()
    {
        const int itemsPerUpdate = 4;
        int end = Math.Min(nextWorkIndex + itemsPerUpdate, Pending.Count);

        while (nextWorkIndex < end)
        {
            ModelWork work = Pending[nextWorkIndex++];
            EditorUtility.DisplayProgressBar(
                "Creating imported model prefabs",
                work.prefabName,
                Pending.Count == 0 ? 1f : (float)nextWorkIndex / Pending.Count);

            string outputDirectory = Path.GetDirectoryName(work.prefabPath)?.Replace('\\', '/');
            EnsureUnityFolder(outputDirectory);
            if (File.Exists(ProjectPath(work.prefabPath)))
                continue;

            GameObject wrapper = null;
            try
            {
                wrapper = new GameObject(work.prefabName);
                SceneManager.MoveGameObjectToScene(wrapper, previewScene);

                GameObject modelInstance = PrefabUtility.InstantiatePrefab(work.model, previewScene) as GameObject;
                if (modelInstance == null)
                    throw new InvalidOperationException("Unity could not instantiate " + work.sourcePath);

                modelInstance.transform.SetParent(wrapper.transform, false);
                AsciiCityAssetPlacement placement = wrapper.AddComponent<AsciiCityAssetPlacement>();
                placement.assetId = work.assetId;
                placement.blocksMovement = false;
                placement.collisionRadius = 0.5f;

                if (work.assetId.Contains("pine-tree"))
                {
                    placement.asciiWidth = 1.8f;
                    placement.asciiHeight = 4.4f;
                }
                else if (work.assetId.Contains("park-tree"))
                {
                    placement.asciiWidth = 1.8f;
                    placement.asciiHeight = 4.8f;
                }
                else if (work.assetId.Contains("building"))
                {
                    placement.asciiWidth = 3.7f;
                    placement.asciiHeight = 5.7f;
                }

                GameObject savedPrefab = PrefabUtility.SaveAsPrefabAsset(wrapper, work.prefabPath);
                if (savedPrefab == null)
                    throw new InvalidOperationException("Unity could not save prefab " + work.prefabPath);
            }
            catch (Exception exception)
            {
                Debug.LogError("Failed to create wrapper prefab for " + work.sourcePath + ": " + exception);
            }
            finally
            {
                if (wrapper != null)
                    UnityEngine.Object.DestroyImmediate(wrapper);
            }
        }

        if (nextWorkIndex < Pending.Count)
            return;

        EditorApplication.update -= ProcessBatch;
        EditorUtility.ClearProgressBar();
        if (previewScene.IsValid())
            EditorSceneManager.ClosePreviewScene(previewScene);
        AssetDatabase.SaveAssets();
        AssetDatabase.Refresh();
        generationRunning = false;
        Debug.Log("ASCII City created or retained " + Pending.Count + " imported pack wrapper prefabs under " + WrapperRoot + ".");
    }

    private static string MakeAssetId(Pack pack, string modelName, string relativePath)
    {
        string modelSlug = Slug(relativePath.Substring(0, relativePath.Length - Path.GetExtension(relativePath).Length));
        string lowerName = modelName.ToLowerInvariant();

        if (pack.prefix == "Nature" && lowerName.StartsWith("pinetree", StringComparison.Ordinal))
            return "nature.pine-tree." + Slug(modelName.Substring("PineTree".Length));

        if (pack.prefix == "Nature" && Regex.IsMatch(lowerName, "(birch|normal|maple|dead|palm)tree", RegexOptions.CultureInvariant))
            return "nature.park-tree." + Slug(modelName);

        string packSlug = pack.prefix == "Commercial" ? "kenney-commercial" :
            pack.prefix == "Industrial" ? "kenney-industrial" :
            pack.prefix == "Sci-Fi" ? "quaternius-scifi" :
            pack.prefix == "Downtown" ? "quaternius-downtown" : "nature-fbx";
        return packSlug + "." + modelSlug;
    }

    private static string Slug(string value)
    {
        return Regex.Replace(value.ToLowerInvariant(), "[^a-z0-9]+", "-").Trim('-');
    }

    private static void EnsureUnityFolder(string assetFolder)
    {
        if (string.IsNullOrEmpty(assetFolder))
            return;

        string current = "Assets";
        string[] segments = assetFolder.Replace('\\', '/').Split('/').Skip(1).ToArray();
        foreach (string segment in segments)
        {
            string next = current + "/" + segment;
            if (!AssetDatabase.IsValidFolder(next))
                AssetDatabase.CreateFolder(current, segment);
            current = next;
        }
    }

    private static string AbsolutePathToAssetPath(string absolutePath)
    {
        string assetRoot = Path.GetFullPath(Application.dataPath).TrimEnd(Path.DirectorySeparatorChar, Path.AltDirectorySeparatorChar) + Path.DirectorySeparatorChar;
        string fullPath = Path.GetFullPath(absolutePath);
        if (!fullPath.StartsWith(assetRoot, StringComparison.OrdinalIgnoreCase))
            throw new InvalidOperationException("Model path is outside the Unity Assets folder: " + fullPath);
        return "Assets/" + fullPath.Substring(assetRoot.Length).Replace('\\', '/');
    }

    private static string ProjectPath(string relativePath)
    {
        return Path.GetFullPath(Path.Combine(Application.dataPath, "..", relativePath.Replace('/', Path.DirectorySeparatorChar)));
    }
}
