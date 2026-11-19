using System.Collections.Generic;
using UnityEngine;

namespace AsciiCity.WorldAuthoring
{
    [System.Serializable]
    public sealed class AsciiCityCellPreviewData
    {
        public string token;
        public float height;
        public bool solid;
    }

    [DisallowMultipleComponent]
    public sealed class AsciiCityMapPreview : MonoBehaviour
    {
        public string mapId;
        public string[] rowsNorthToSouth = new string[0];
        public AsciiCityCellPreviewData[] cellTypes = new AsciiCityCellPreviewData[0];
        [Tooltip("Unity-authored terrain elevations at map-cell corners, stored south-to-north in row-major order.")]
        public float[] terrainVertexHeightsSouthToNorth = new float[0];
        public float cellSize = 1f;

        private void OnDrawGizmos()
        {
            if (rowsNorthToSouth == null)
                return;

            int depth = rowsNorthToSouth.Length;
            int width = rowsNorthToSouth.Length > 0 ? (rowsNorthToSouth[0] ?? string.Empty).Length : 0;
            bool hasTerrainObject = transform.Find("__Authored Details/Painted Terrain Surface") != null;
            bool hasAuthoredTerrain = HasUsableTerrainSurface();
            HashSet<string> authoredObjectNames = new HashSet<string>();
            Transform authoredDetails = transform.Find("__Authored Details");
            if (authoredDetails != null)
                foreach (Transform authoredObject in authoredDetails.GetComponentsInChildren<Transform>(true))
                    authoredObjectNames.Add(authoredObject.name);
            Dictionary<char, AsciiCityCellPreviewData> definitions = new Dictionary<char, AsciiCityCellPreviewData>();
            if (cellTypes != null)
            {
                foreach (AsciiCityCellPreviewData cellType in cellTypes)
                {
                    if (cellType != null && !string.IsNullOrEmpty(cellType.token) && cellType.token.Length == 1)
                        definitions[cellType.token[0]] = cellType;
                }
            }

            for (int row = 0; row < depth; row++)
            {
                string cells = rowsNorthToSouth[row] ?? string.Empty;
                int worldZ = depth - 1 - row;
                for (int x = 0; x < cells.Length; x++)
                {
                    char token = cells[x];
                    AsciiCityCellPreviewData type;
                    bool hasType = definitions.TryGetValue(token, out type);
                    float groundHeight = TerrainHeightAt(x + 0.5f, worldZ + 0.5f, width, depth);
                    float groundCenterY = hasTerrainObject ? groundHeight + 0.02f : groundHeight - 0.03f;
                    float groundThickness = hasTerrainObject ? 0.02f : 0.06f;
                    Vector3 groundCenter = transform.position + new Vector3((x + 0.5f) * cellSize, groundCenterY, (worldZ + 0.5f) * cellSize);
                    Gizmos.color = GroundColor(token);
                    if (!hasAuthoredTerrain)
                        Gizmos.DrawCube(groundCenter, new Vector3(cellSize * 0.96f, groundThickness, cellSize * 0.96f));

                    if (hasType && type.solid && type.height > 0f)
                    {
                        Vector3 cellCenter = transform.position + new Vector3((x + 0.5f) * cellSize, groundHeight, (worldZ + 0.5f) * cellSize);
                        string generatedObjectName = AuthoredObjectName(token, x, worldZ);
                        bool hasGeneratedMesh = generatedObjectName != null && authoredObjectNames.Contains(generatedObjectName);
                        bool hasWaterSurface = token == '~' && hasAuthoredTerrain;
                        if (!hasGeneratedMesh)
                        {
                            if (!hasWaterSurface)
                                DrawSolidCell(token, cellCenter, type.height * cellSize, cellSize);
                        }
                    }
                }
            }
        }

        private static string AuthoredObjectName(char token, int x, int z)
        {
            string label;
            switch (token)
            {
                case 'g': label = "Park Tree"; break;
                case 'y': label = "Pine Tree"; break;
                case 'b': label = "Park Bench"; break;
                case 'f': label = "Stone Fountain"; break;
                case 'w': label = "Waterfall"; break;
                default: return null;
            }
            return label + " [" + x + ", " + z + "]";
        }

        public float TerrainHeightAt(float worldX, float worldZ, int width, int depth)
        {
            int expected = (width + 1) * (depth + 1);
            if (terrainVertexHeightsSouthToNorth == null || terrainVertexHeightsSouthToNorth.Length != expected)
                return 0f;
            float x = Mathf.Clamp(worldX / cellSize, 0f, width);
            float z = Mathf.Clamp(worldZ / cellSize, 0f, depth);
            int x0 = Mathf.Min(Mathf.FloorToInt(x), width - 1);
            int z0 = Mathf.Min(Mathf.FloorToInt(z), depth - 1);
            int x1 = x0 + 1;
            int z1 = z0 + 1;
            float tx = x - x0;
            float tz = z - z0;
            float h00 = terrainVertexHeightsSouthToNorth[z0 * (width + 1) + x0];
            float h10 = terrainVertexHeightsSouthToNorth[z0 * (width + 1) + x1];
            float h01 = terrainVertexHeightsSouthToNorth[z1 * (width + 1) + x0];
            float h11 = terrainVertexHeightsSouthToNorth[z1 * (width + 1) + x1];
            return Mathf.Lerp(Mathf.Lerp(h00, h10, tx), Mathf.Lerp(h01, h11, tx), tz);
        }

        private bool HasUsableTerrainSurface()
        {
            Transform surface = transform.Find("__Authored Details/Painted Terrain Surface");
            if (surface == null) return false;
            MeshFilter filter = surface.GetComponent<MeshFilter>();
            MeshRenderer renderer = surface.GetComponent<MeshRenderer>();
            if (filter == null || filter.sharedMesh == null || filter.sharedMesh.vertexCount < 4 || renderer == null ||
                !renderer.enabled || !renderer.gameObject.activeInHierarchy)
                return false;
            return filter.sharedMesh.subMeshCount > 0;
        }

        private static Color GroundColor(char token)
        {
            switch (token)
            {
                case '.': return new Color(0.16f, 0.17f, 0.18f);
                case ',': return new Color(0.33f, 0.34f, 0.35f);
                case ':': return new Color(0.30f, 0.24f, 0.35f);
                case '_': return new Color(0.36f, 0.32f, 0.29f);
                case 'p':
                case 'g':
                case 'y': return new Color(0.20f, 0.34f, 0.23f);
                case '~':
                case 'w': return new Color(0.12f, 0.28f, 0.38f);
                default: return new Color(0.27f, 0.27f, 0.31f);
            }
        }

        private static Color SolidColor(char token)
        {
            if (token == 'g' || token == 'y') return new Color(0.22f, 0.42f, 0.28f);
            if (token == 'x') return new Color(0.58f, 0.58f, 0.55f);
            if (token == 'n') return new Color(0.38f, 0.22f, 0.42f);
            if (token == 't' || token == 'm' || token == 'l') return new Color(0.35f, 0.37f, 0.40f);
            return new Color(0.43f, 0.39f, 0.34f);
        }

        private static void DrawSolidCell(char token, Vector3 center, float height, float size)
        {
            Gizmos.color = SolidColor(token);
            if (token == 'g')
            {
                Gizmos.DrawCube(center + Vector3.up * 1.1f, new Vector3(0.18f, 2.2f, 0.18f));
                DrawEllipsoid(center + Vector3.up * (height * 0.62f), new Vector3(size * 1.05f, height * 0.42f, size * 1.05f));
                DrawEllipsoid(center + Vector3.up * (height * 0.78f), new Vector3(size * 0.78f, height * 0.32f, size * 0.78f));
                DrawEllipsoid(center + Vector3.up * (height * 0.48f), new Vector3(size * 0.82f, height * 0.25f, size * 0.82f));
                return;
            }
            if (token == 'y')
            {
                for (int tier = 0; tier < 5; tier++)
                {
                    float t = tier / 5f;
                    float tierHeight = height * 0.25f;
                    DrawEllipsoid(center + Vector3.up * (height * (0.20f + t * 0.62f)), new Vector3(size * (0.92f - t * 0.72f), tierHeight, size * (0.92f - t * 0.72f)));
                }
                return;
            }
            if (token == 'b')
            {
                Gizmos.DrawCube(center + Vector3.up * 0.5f, new Vector3(size * 0.85f, 0.12f, size * 0.34f));
                Gizmos.DrawCube(center + Vector3.up * 0.85f + Vector3.back * 0.26f, new Vector3(size * 0.85f, 0.55f, 0.12f));
                for (int leg = -1; leg <= 1; leg += 2)
                    Gizmos.DrawCube(center + new Vector3(leg * size * 0.31f, 0.25f, 0f), new Vector3(0.1f, 0.5f, size * 0.3f));
                return;
            }
            if (token == 'i')
            {
                Gizmos.DrawCube(center + Vector3.up * 0.45f, new Vector3(size * 0.42f, 0.9f, size * 0.42f));
                Gizmos.DrawCube(center + Vector3.up * 0.94f, new Vector3(size * 0.5f, 0.08f, size * 0.5f));
                return;
            }
            if (token == 'f')
            {
                Gizmos.DrawCube(center + Vector3.up * 0.35f, new Vector3(size * 0.72f, 0.7f, size * 0.72f));
                DrawEllipsoid(center + Vector3.up * 1.15f, new Vector3(size * 0.9f, 0.24f, size * 0.9f));
                DrawEllipsoid(center + Vector3.up * 1.55f, new Vector3(size * 0.32f, 0.5f, size * 0.32f));
                return;
            }
            if (token == 'v')
            {
                for (int sx = -1; sx <= 1; sx += 2)
                for (int sz = -1; sz <= 1; sz += 2)
                    Gizmos.DrawCube(center + new Vector3(sx * size * 0.38f, height * 0.5f, sz * size * 0.38f), new Vector3(0.12f, height, 0.12f));
                Gizmos.DrawCube(center + Vector3.up * (height - 0.1f), new Vector3(size, 0.2f, size));
                return;
            }

            Vector3 massCenter = center + Vector3.up * height * 0.5f;
            Gizmos.DrawCube(massCenter, new Vector3(size * 0.82f, height, size * 0.82f));
        }

        private static void DrawEllipsoid(Vector3 center, Vector3 scale)
        {
            Matrix4x4 previous = Gizmos.matrix;
            Gizmos.matrix = Matrix4x4.TRS(center, Quaternion.identity, scale);
            Gizmos.DrawSphere(Vector3.zero, 0.5f);
            Gizmos.matrix = previous;
        }
    }

}
