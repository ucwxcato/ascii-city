using UnityEngine;

namespace AsciiCity.WorldAuthoring
{
    [System.Serializable]
    public sealed class AsciiCitySpriteColor
    {
        public string symbol;
        public string palette;
    }

    [DisallowMultipleComponent]
    public sealed class AsciiCityAssetPlacement : MonoBehaviour
    {
        [Tooltip("Stable unique scene-instance ID saved to the map's .unity-assets.json sidecar.")]
        public string placementId;

        [Tooltip("Stable prefab asset ID used to resolve this placed model at runtime.")]
        public string assetId;

        [Tooltip("Whether this asset blocks player movement in the Kotlin game.")]
        public bool blocksMovement;

        [Min(0f)]
        public float collisionRadius = 0.5f;

        [Tooltip("Stable ASCII sprite ID exported with this placeable model.")]
        public string spriteId;
        [Min(0.01f)] public float asciiWidth = 1f;
        [Min(0.01f)] public float asciiHeight = 1f;
        [Range(0f, 1f)] public float asciiAnchorX = 0.5f;
        [Range(0f, 1f)] public float asciiAnchorY;
        [TextArea(1, 16)] public string[] glyphRows = new string[0];
        [TextArea(1, 16)] public string[] colorRows = new string[0];
        public AsciiCitySpriteColor[] colors = new AsciiCitySpriteColor[0];
    }
}
