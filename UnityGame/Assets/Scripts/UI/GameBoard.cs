using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using UnityEngine.UI;
using UnityGame.Data;
using UnityGame.Game;

namespace UnityGame.UI
{
    /// <summary>
    /// 游戏棋盘组件
    /// 负责渲染棋盘背景、层级排列的所有卡牌（自动连接GameManager）
    /// </summary>
    public class GameBoard : MonoBehaviour
    {
        [Header("Layout Settings")]
        [SerializeField] private float tileSize = 70f;
        [SerializeField] private float tileSpacing = 82f;

        // 存储所有卡牌视图
        private Dictionary<string, GameObject> tileObjects = new Dictionary<string, GameObject>();
        private List<Tile> currentBoardTiles = new List<Tile>();

        // 颜色表（根据卡牌类型生成不同颜色）
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
                GameManager.Instance.OnBoardChanged += OnBoardDataChanged;
                GameManager.Instance.OnTilesHighlighted += OnTilesHighlighted;
                
                // 立即更新显示
                if (GameManager.Instance.boardTiles.Count > 0)
                    UpdateBoard(GameManager.Instance.boardTiles);
            }
        }

        void OnDestroy()
        {
            if (GameManager.Instance != null)
            {
                GameManager.Instance.OnBoardChanged -= OnBoardDataChanged;
                GameManager.Instance.OnTilesHighlighted -= OnTilesHighlighted;
            }
            ClearBoard();
        }

        private void OnBoardDataChanged()
        {
            if (GameManager.Instance != null)
                UpdateBoard(GameManager.Instance.boardTiles);
        }

        private void OnTilesHighlighted(List<string> tileIds)
        {
            // 清除所有高亮
            foreach (var kvp in tileObjects)
            {
                SetHighlight(kvp.Value, false);
            }
            
            // 高亮目标卡牌
            if (tileIds != null && tileIds.Count > 0)
            {
                foreach (string tileId in tileIds)
                {
                    if (tileObjects.ContainsKey(tileId))
                        SetHighlight(tileObjects[tileId], true);
                }
            }
        }

        /// <summary>
        /// 更新棋盘显示
        /// </summary>
        public void UpdateBoard(List<Tile> boardTiles)
        {
            ClearBoard();
            currentBoardTiles = new List<Tile>(boardTiles);

            var visibleTiles = boardTiles.FindAll(t =>
                t.state == TileState.NORMAL || t.state == TileState.BLOCKED);

            if (visibleTiles.Count == 0) return;

            // 计算边界用于居中
            float minX = visibleTiles.Min(t => t.x);
            float maxX = visibleTiles.Max(t => t.x);
            float minY = visibleTiles.Min(t => t.y);
            float maxY = visibleTiles.Max(t => t.y);

            float centerX = (minX + maxX) / 2f;
            float centerY = (minY + maxY) / 2f;

            // 创建卡牌视图
            foreach (var tile in visibleTiles)
            {
                CreateTileView(tile, centerX, centerY);
            }

            Debug.Log($"[GameBoard] Rendered {visibleTiles.Count} tiles");
        }

        /// <summary>
        /// 创建单张卡牌视图（程序化生成，无需预制体）
        /// </summary>
        private void CreateTileView(Tile tile, float centerX, float centerY)
        {
            // 创建卡牌GameObject
            GameObject tileObj = new GameObject($"Tile_{tile.id}");
            tileObj.transform.SetParent(transform, false);

            // 添加Image组件作为背景
            Image bgImage = tileObj.AddComponent<Image>();
            bgImage.sprite = CreateRoundedRectSprite();
            
            // 根据类型设置颜色
            int colorIndex = Mathf.Abs(tile.type) % TypeColors.Length;
            bgImage.color = TypeColors[colorIndex];

            // 设置位置（相对于中心偏移）
            RectTransform rectTransform = tileObj.GetComponent<RectTransform>();
            rectTransform.sizeDelta = new Vector2(tileSize, tileSize);
            rectTransform.anchoredPosition = new Vector2(
                (tile.x - centerX) * tileSpacing,
                -(tile.y - centerY) * tileSpacing
            );
            
            // Z轴层级通过sibling index模拟
            rectTransform.SetSiblingIndex(tile.z);

            // 添加Button组件实现点击
            Button btn = tileObj.AddComponent<Button>();
            btn.targetGraphic = bgImage;

            // 根据状态设置视觉效果
            if (tile.state == TileState.BLOCKED)
            {
                bgImage.color = new Color(0.6f, 0.6f, 0.6f, 0.85f);
                btn.interactable = false;
            }
            else
            {
                btn.interactable = true;
            }

            // 显示封印数量
            if (tile.sealedCount > 0)
            {
                CreateLabel(tileObj, $"🔒{tile.sealedCount}", new Vector2(-15, 15));
            }

            // 盲盒标记
            if (tile.isBlind)
            {
                CreateLabel(tileObj, "?", Vector2.zero, 28);
            }

            // 绑定点击事件
            string capturedId = tile.id; // 闭包捕获
            btn.onClick.AddListener(() =>
            {
                if (GameManager.Instance != null)
                    GameManager.Instance.ClickTile(tile);
            });

            tileObjects[tile.id] = tileObj;
        }

        /// <summary>
        /// 创建圆角矩形Sprite
        /// </summary>
        private Sprite CreateRoundedRectSprite()
        {
            // 使用默认白色精灵，圆角效果通过Image的sprite实现
            Texture2D tex = new Texture2D(64, 64, TextureFormat.RGBA32, false);
            for (int x = 0; x < 64; x++)
            {
                for (int y = 0; y < 64; y++)
                {
                    bool edgeX = x < 4 || x >= 60;
                    bool edgeY = y < 4 || y >= 60;
                    if (edgeX && edgeY)
                        tex.SetPixel(x, y, Color.clear);
                    else
                        tex.SetPixel(x, y, Color.white);
                }
            }
            tex.Apply();
            return Sprite.Create(tex, new Rect(0, 0, 64, 64), new Vector2(0.5f, 0.5f));
        }

        /// <summary>
        /// 在卡牌上创建文字标签
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
            labelText.horizontalOverflow = HorizontalWrapMode.Overflow;

            // 尝试使用系统字体或Arial
            Font sysFont = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            if (sysFont == null) sysFont = Font.CreateDynamicFontFromOSFont("Microsoft YaHei", 14);
            if (sysFont == null) sysFont = Font.CreateDynamicFontFromOSFont("Arial", 14);
            if (sysFont != null) labelText.font = sysFont;
        }

        /// <summary>
        /// 设置高亮
        /// </summary>
        private void SetHighlight(GameObject tileObj, bool highlight)
        {
            if (tileObj == null) return;
            Image img = tileObj.GetComponent<Image>();
            if (img != null)
            {
                img.color = highlight ? new Color(1f, 1f, 0.3f, 1f) : TypeColors[0]; // 黄色高亮
            }
        }

        /// <summary>
        /// 清除棋盘
        /// </summary>
        private void ClearBoard()
        {
            foreach (var obj in tileObjects.Values)
            {
                if (obj != null) DestroyImmediate(obj);
            }
            tileObjects.Clear();
        }

        public Vector3 GetTileWorldPosition(string tileId)
        {
            if (tileObjects.ContainsKey(tileId))
                return tileObjects[tileId].transform.position;
            return Vector3.zero;
        }
    }
}
