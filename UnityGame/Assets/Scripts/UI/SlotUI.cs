using UnityEngine;
using UnityEngine.UI;
using UnityGame.Data;
using System.Collections.Generic;

namespace UnityGame.UI
{
    /// <summary>
    /// 槽位UI组件
    /// 显示玩家收集的卡牌（最多7个，自动连接GameManager）
    /// </summary>
    public class SlotUI : MonoBehaviour
    {
        [Header("Layout Settings")]
        [SerializeField] private float slotItemSize = 70f;
        [SerializeField] private float slotSpacing = 78f;

        // 存储槽位中的卡牌视图
        private Dictionary<string, GameObject> slotItems = new Dictionary<string, GameObject>();
        private List<Tile> currentSlotTiles = new List<Tile>();

        // 颜色表（与GameBoard一致）
        private static readonly Color[] TypeColors = new Color[]
        {
            Color.red, Color.blue, Color.green, Color.yellow, Color.magenta,
            Color.cyan, new Color(1f, 0.5f, 0f),     // 橙色
            new Color(0.5f, 0f, 1f),                 // 紫色
            new Color(1f, 0.75f, 0.8f),              // 粉色
            new Color(0.75f, 0.75f, 0.75f),         // 灰色
            new Color(0f, 0.5f, 0.25f),             // 深绿
            new Color(0.54f, 0.17f, 0.89f),         // 靛蓝
            new Color(1f, 0.84f, 0f),                // 金色
            new Color(0f, 0.7f, 0.7f),              // 青绿
            new Color(0.6f, 0.3f, 0f),               // 棕色
        };

        void Start()
        {
            // 自动订阅GameManager事件
            if (GameManager.Instance != null)
            {
                GameManager.Instance.OnSlotChanged += OnSlotDataChanged;
                
                // 立即更新显示
                if (GameManager.Instance.slotData != null)
                    UpdateSlot(GameManager.Instance.slotData.tiles);
            }
        }

        void OnDestroy()
        {
            if (GameManager.Instance != null)
                GameManager.Instance.OnSlotChanged -= OnSlotDataChanged;
            ClearSlot();
        }

        private void OnSlotDataChanged()
        {
            if (GameManager.Instance != null && GameManager.Instance.slotData != null)
                UpdateSlot(GameManager.Instance.slotData.tiles);
        }

        /// <summary>
        /// 更新槽位显示
        /// </summary>
        public void UpdateSlot(List<Tile> slotTiles)
        {
            ClearSlot();
            currentSlotTiles = new List<Tile>(slotTiles);

            for (int i = 0; i < slotTiles.Count; i++)
            {
                Tile tile = slotTiles[i];
                CreateSlotItem(tile, i);
            }

            CheckAndHighlightMatches();
            
            Debug.Log($"[SlotUI] Updated: {slotTiles.Count} items in slot");
        }

        /// <summary>
        /// 创建槽位卡牌（程序化生成）
        /// </summary>
        private void CreateSlotItem(Tile tile, int index)
        {
            GameObject itemObj = new GameObject($"Slot_{tile.id}");
            itemObj.transform.SetParent(transform, false);

            Image img = itemObj.AddComponent<Image>();
            img.sprite = CreateRoundedRectSprite();

            // 根据类型设置颜色
            int colorIndex = Mathf.Abs(tile.type) % TypeColors.Length;
            img.color = TypeColors[colorIndex];

            RectTransform rectTransform = itemObj.GetComponent<RectTransform>();
            rectTransform.sizeDelta = new Vector2(slotItemSize, slotItemSize);
            // 居中排列
            float totalWidth = (currentSlotTiles.Count - 1) * slotSpacing;
            rectTransform.anchoredPosition = new Vector2(index * slotSpacing - totalWidth / 2f, 0);

            // 显示类型编号（帮助区分不同类型）
            CreateLabel(itemObj, $"{tile.type}", Vector2.zero, 18);

            slotItems[tile.id] = itemObj;
        }

        /// <summary>
        /// 检查并高亮可匹配的组
        /// </summary>
        private void CheckAndHighlightMatches()
        {
            Dictionary<int, int> typeCount = new Dictionary<int, int>();
            foreach (var tile in currentSlotTiles)
            {
                if (!typeCount.ContainsKey(tile.type))
                    typeCount[tile.type] = 0;
                typeCount[tile.type]++;
            }

            foreach (var kvp in typeCount)
            {
                if (kvp.Value >= 3)
                {
                    foreach (var tile in currentSlotTiles.FindAll(t => t.type == kvp.Key))
                    {
                        if (slotItems.ContainsKey(tile.id))
                        {
                            Image img = slotItems[tile.id].GetComponent<Image>();
                            if (img != null) img.color = new Color(1f, 0.85f, 0.85f); // 匹配提示色
                        }
                    }
                }
            }
        }

        /// <summary>
        /// 创建圆角矩形Sprite
        /// </summary>
        private Sprite CreateRoundedRectSprite()
        {
            Texture2D tex = new Texture2D(64, 64, TextureFormat.RGBA32, false);
            for (int x = 0; x < 64; x++)
            {
                for (int y = 0; y < 64; y++)
                {
                    bool edgeX = x < 4 || x >= 60;
                    bool edgeY = y < 4 || y >= 60;
                    tex.SetPixel(x, y, (edgeX && edgeY) ? Color.clear : Color.white);
                }
            }
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, 64, 64), new Vector2(0.5f, 0.5f));
        }

        /// <summary>
        /// 创建文字标签
        /// </summary>
        private void CreateLabel(GameObject parent, string text, Vector2 offset, int fontSize = 14)
        {
            GameObject label = new GameObject("Label");
            label.transform.SetParent(parent.transform, false);
            
            RectTransform labelRect = label.AddComponent<RectTransform>();
            labelRect.anchoredPosition = offset;
            labelRect.sizeDelta = new Vector2(40, 20);

            Text labelText = label.AddComponent<Text>();
            labelText.text = text;
            labelText.fontSize = fontSize;
            labelText.fontStyle = FontStyle.Bold;
            labelText.alignment = TextAnchor.MiddleCenter;
            labelText.color = Color.white;

            Font sysFont = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            if (sysFont == null) sysFont = Font.CreateDynamicFontFromOSFont("Microsoft YaHei", fontSize);
            if (sysFont == null) sysFont = Font.CreateDynamicFontFromOSFont("Arial", fontSize);
            if (sysFont != null) labelText.font = sysFont;
        }

        private void ClearSlot()
        {
            foreach (var obj in slotItems.Values)
            {
                if (obj != null) DestroyImmediate(obj);
            }
            slotItems.Clear();
            currentSlotTiles.Clear();
        }
    }
}
