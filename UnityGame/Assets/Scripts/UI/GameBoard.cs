using System.Collections.Generic;
using UnityEngine;
using UnityGame.Data;

namespace UnityGame.UI
{
    /// <summary>
    /// 游戏棋盘组件
    /// 负责渲染棋盘背景、层级排列的所有卡牌
    /// </summary>
    public class GameBoard : MonoBehaviour
    {
        [Header("Prefabs")]
        [SerializeField] private GameObject tilePrefab;

        [Header("Layout Settings")]
        [SerializeField] private float tileSize = 80f;
        [SerializeField] private float tileSpacing = 85f;
        [SerializeField] private float boardWidth = 500f;
        [SerializeField] private float boardHeight = 600f;

        [Header("Visual Settings")]
        [SerializeField] private UnityEngine.UI.Image boardBackground;

        // 存储所有卡牌视图
        private Dictionary<string, TileView> tileViews = new Dictionary<string, TileView>();
        private List<Tile> currentBoardTiles = new List<Tile>();

        // 点击事件
        public event System.Action<Tile> OnTileClicked;

        // 飞行动画相关
        private HashSet<string> flyingTileIds = new HashSet<string>();

        void Start()
        {
            if (boardBackground == null)
                boardBackground = GetComponent<UnityEngine.UI.Image>();
        }

        /// <summary>
        /// 更新棋盘显示
        /// </summary>
        public void UpdateBoard(List<Tile> boardTiles)
        {
            // 清除旧的卡牌
            ClearBoard();

            currentBoardTiles = new List<Tile>(boardTiles);

            // 获取可见的卡牌（NORMAL 或 BLOCKED 状态）
            var visibleTiles = boardTiles.FindAll(t =>
                t.state == TileState.NORMAL || t.state == TileState.BLOCKED);

            if (visibleTiles.Count == 0) return;

            // 计算边界
            float minX = visibleTiles.Min(t => t.x);
            float maxX = visibleTiles.Max(t => t.x);
            float minY = visibleTiles.Min(t => t.y);
            float maxY = visibleTiles.Max(t => t.y);

            // 创建卡牌视图
            foreach (var tile in visibleTiles)
            {
                CreateTileView(tile, minX, minY);
            }
        }

        /// <summary>
        /// 创建单张卡牌视图
        /// </summary>
        private void CreateTileView(Tile tile, float minX, float minY)
        {
            if (tilePrefab == null)
            {
                Debug.LogError("TilePrefab is not assigned!");
                return;
            }

            // 实例化卡牌
            GameObject tileObj = Instantiate(tilePrefab, transform);
            TileView tileView = tileObj.GetComponent<TileView>();

            if (tileView == null)
            {
                Debug.LogError("TilePrefab does not have TileView component!");
                Destroy(tileObj);
                return;
            }

            // 设置位置
            float posX = (tile.x - minX) * tileSpacing;
            float posY = (tile.y - minY) * tileSpacing;

            RectTransform rectTransform = tileObj.GetComponent<RectTransform>();
            if (rectTransform != null)
            {
                rectTransform.anchoredPosition = new Vector2(posX, -posY);
                rectTransform.sizeDelta = new Vector2(tileSize, tileSize);
                rectTransform.SetSiblingIndex(tile.z); // Z轴层级
            }

            // 初始化卡牌视图
            Sprite tileSprite = GetTileSprite(tile.type);
            tileView.Initialize(tile, tileSprite);

            // 设置点击事件
            tileView.OnTileClicked += (clickedTile) =>
            {
                if (!flyingTileIds.Contains(clickedTile.id))
                {
                    OnTileClicked?.Invoke(clickedTile);
                }
            };

            // 存储引用
            tileViews[tile.id] = tileView;
        }

        /// <summary>
        /// 获取卡牌精灵图
        /// </summary>
        private Sprite GetTileSprite(int type)
        {
            // TODO: 从资源管理器获取对应类型的精灵图
            // 临时：使用默认精灵图
            if (tilePrefab != null)
            {
                TileView prefabView = tilePrefab.GetComponent<TileView>();
                if (prefabView != null)
                {
                    // 这里应该根据不同的type返回不同的精灵图
                    // 暂时返回null，使用默认图片
                }
            }
            return null;
        }

        /// <summary>
        /// 高亮卡牌
        /// </summary>
        public void HighlightTile(string tileId, bool highlight)
        {
            if (tileViews.ContainsKey(tileId))
            {
                tileViews[tileId].SetHighlighted(highlight);
            }
        }

        /// <summary>
        /// 抖动卡牌
        /// </summary>
        public void ShakeTile(string tileId, bool shake)
        {
            if (tileViews.ContainsKey(tileId))
            {
                tileViews[tileId].SetShaking(shake);
            }
        }

        /// <summary>
        /// 设置卡牌飞行动画
        /// </summary>
        public void SetTileFlying(string tileId, bool isFlying)
        {
            if (isFlying)
            {
                flyingTileIds.Add(tileId);
            }
            else
            {
                flyingTileIds.Remove(tileId);
            }

            if (tileViews.ContainsKey(tileId))
            {
                tileViews[tileId].gameObject.SetActive(!isFlying);
            }
        }

        /// <summary>
        /// 清除棋盘
        /// </summary>
        private void ClearBoard()
        {
            foreach (var tileView in tileViews.Values)
            {
                if (tileView != null && tileView.gameObject != null)
                {
                    DestroyImmediate(tileView.gameObject);
                }
            }

            tileViews.Clear();
            flyingTileIds.Clear();
        }

        /// <summary>
        /// 刷新单个卡牌的状态
        /// </summary>
        public void RefreshTile(Tile tile)
        {
            if (tileViews.ContainsKey(tile.id))
            {
                tileViews[tile.id].SetTileData(tile);
                tileViews[tile.id].UpdateVisualState();
            }
        }

        /// <summary>
        /// 获取卡牌的世界坐标
        /// </summary>
        public Vector3 GetTileWorldPosition(string tileId)
        {
            if (tileViews.ContainsKey(tileId))
            {
                return tileViews[tileId].transform.position;
            }
            return Vector3.zero;
        }

        void OnDestroy()
        {
            ClearBoard();
        }
    }
}
