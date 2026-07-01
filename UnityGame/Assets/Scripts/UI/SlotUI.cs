using UnityEngine;
using UnityEngine.UI;
using UnityGame.Data;
using System.Collections.Generic;

namespace UnityGame.UI
{
    /// <summary>
    /// 槽位UI组件
    /// 显示玩家收集的卡牌（最多7个）
    /// </summary>
    public class SlotUI : MonoBehaviour
    {
        [Header("UI组件")]
        [SerializeField] private Transform slotContainer;
        [SerializeField] private GameObject slotItemPrefab;
        
        [Header("Layout Settings")]
        [SerializeField] private float slotSpacing = 90f;
#pragma warning disable CS0414 // 保留用于Inspector配置
        [SerializeField] private int maxSlotCount = 7;
#pragma warning restore CS0414

        [Header("Visual Settings")]
        [SerializeField] private Color normalColor = Color.white;
        [SerializeField] private Color matchColor = new Color(1f, 0.8f, 0.8f);

        // 存储槽位中的卡牌视图
        private Dictionary<string, GameObject> slotItems = new Dictionary<string, GameObject>();
        private List<Tile> currentSlotTiles = new List<Tile>();

        void Start()
        {
            if (slotContainer == null)
                slotContainer = transform;
        }

        /// <summary>
        /// 更新槽位显示
        /// </summary>
        public void UpdateSlot(List<Tile> slotTiles)
        {
            // 清除旧的显示
            ClearSlot();

            currentSlotTiles = new List<Tile>(slotTiles);

            // 创建槽位卡牌显示
            for (int i = 0; i < slotTiles.Count; i++)
            {
                Tile tile = slotTiles[i];
                CreateSlotItem(tile, i);
            }

            // 检查是否有匹配
            CheckAndHighlightMatches();
        }

        /// <summary>
        /// 创建槽位中的单个卡牌显示
        /// </summary>
        private void CreateSlotItem(Tile tile, int index)
        {
            if (slotItemPrefab == null)
            {
                Debug.LogError("SlotItemPrefab is not assigned!");
                return;
            }

            // 实例化槽位卡牌
            GameObject slotItem = Instantiate(slotItemPrefab, slotContainer);
            
            // 设置位置
            RectTransform rectTransform = slotItem.GetComponent<RectTransform>();
            if (rectTransform != null)
            {
                rectTransform.anchoredPosition = new Vector2(index * slotSpacing, 0);
            }

            // 设置卡牌图片
            Image slotImage = slotItem.GetComponent<Image>();
            if (slotImage != null)
            {
                Sprite tileSprite = GetTileSprite(tile.type);
                if (tileSprite != null)
                {
                    slotImage.sprite = tileSprite;
                    slotImage.color = normalColor;
                }
            }

            // 存储引用
            slotItems[tile.id] = slotItem;
        }

        /// <summary>
        /// 检查并高亮匹配的卡牌
        /// </summary>
        private void CheckAndHighlightMatches()
        {
            // 统计每种类型的数量
            Dictionary<int, List<string>> typeGroups = new Dictionary<int, List<string>>();
            
            foreach (var tile in currentSlotTiles)
            {
                if (!typeGroups.ContainsKey(tile.type))
                    typeGroups[tile.type] = new List<string>();
                typeGroups[tile.type].Add(tile.id);
            }

            // 高亮有3张或以上的组
            foreach (var group in typeGroups.Values)
            {
                if (group.Count >= 3)
                {
                    foreach (var tileId in group)
                    {
                        if (slotItems.ContainsKey(tileId))
                        {
                            Image img = slotItems[tileId].GetComponent<Image>();
                            if (img != null)
                            {
                                img.color = matchColor;
                            }
                        }
                    }
                }
            }
        }

        /// <summary>
        /// 添加卡牌到槽位（带动画）
        /// </summary>
        public void AddTileToSlot(Tile tile, Vector3 fromPosition)
        {
            if (slotItemPrefab == null) return;

            // 创建卡牌
            GameObject slotItem = Instantiate(slotItemPrefab, slotContainer);
            int index = currentSlotTiles.Count;
            
            // 设置位置
            RectTransform rectTransform = slotItem.GetComponent<RectTransform>();
            if (rectTransform != null)
            {
                rectTransform.anchoredPosition = new Vector2(index * slotSpacing, 0);
            }

            // 设置卡牌图片
            Image slotImage = slotItem.GetComponent<Image>();
            if (slotImage != null)
            {
                Sprite tileSprite = GetTileSprite(tile.type);
                if (tileSprite != null)
                {
                    slotImage.sprite = tileSprite;
                }
            }

            // 存储引用
            slotItems[tile.id] = slotItem;
            currentSlotTiles.Add(tile);

            // 播放飞行动画
            StartCoroutine(FlyToSlotAnimation(slotItem, fromPosition));
        }

        /// <summary>
        /// 飞行动画：卡牌从棋盘飞到槽位
        /// </summary>
        private System.Collections.IEnumerator FlyToSlotAnimation(GameObject slotItem, Vector3 fromPosition)
        {
            RectTransform rectTransform = slotItem.GetComponent<RectTransform>();
            if (rectTransform == null) yield break;

            // 转换为屏幕坐标
            Vector3 startPos = Camera.main.ScreenToWorldPoint(fromPosition);
            Vector3 endPos = rectTransform.position;

            float duration = 0.3f;
            float elapsed = 0f;

            while (elapsed < duration)
            {
                elapsed += Time.deltaTime;
                float t = elapsed / duration;
                
                // 使用平滑曲线
                t = t * t * (3f - 2f * t); // Smoothstep
                
                rectTransform.position = Vector3.Lerp(startPos, endPos, t);
                
                yield return null;
            }

            rectTransform.position = endPos;
        }

        /// <summary>
        /// 从槽位移除卡牌（匹配消除时）
        /// </summary>
        public void RemoveTileFromSlot(Tile tile)
        {
            if (slotItems.ContainsKey(tile.id))
            {
                DestroyImmediate(slotItems[tile.id]);
                slotItems.Remove(tile.id);
            }

            currentSlotTiles.Remove(tile);
            
            // 重新排列
            RearrangeSlotItems();
        }

        /// <summary>
        /// 重新排列槽位卡牌
        /// </summary>
        private void RearrangeSlotItems()
        {
            int index = 0;
            foreach (var tile in currentSlotTiles)
            {
                if (slotItems.ContainsKey(tile.id))
                {
                    RectTransform rectTransform = slotItems[tile.id].GetComponent<RectTransform>();
                    if (rectTransform != null)
                    {
                        rectTransform.anchoredPosition = new Vector2(index * slotSpacing, 0);
                    }
                }
                index++;
            }
        }

        /// <summary>
        /// 清除槽位显示
        /// </summary>
        private void ClearSlot()
        {
            foreach (var slotItem in slotItems.Values)
            {
                if (slotItem != null)
                {
                    DestroyImmediate(slotItem);
                }
            }

            slotItems.Clear();
            currentSlotTiles.Clear();
        }

        /// <summary>
        /// 获取卡牌精灵图
        /// </summary>
        private Sprite GetTileSprite(int type)
        {
            // TODO: 从资源管理器获取对应类型的精灵图
            // 临时：返回null，使用默认图片
            return null;
        }

        void OnDestroy()
        {
            ClearSlot();
        }
    }
}
