using UnityEngine;
using UnityEngine.UI;
using UnityGame.Data;

namespace UnityGame.UI
{
    /// <summary>
    /// 卡牌视图组件
    /// 负责渲染单张卡牌并处理点击事件
    /// </summary>
    [RequireComponent(typeof(Button))]
    [RequireComponent(typeof(Image))]
    public class TileView : MonoBehaviour
    {
        [Header("UI组件")]
        [SerializeField] private Image tileImage;
        [SerializeField] private Image frameImage;
        [SerializeField] private GameObject blockedOverlay;
        [SerializeField] private GameObject sealOverlay;    // 封印覆盖层（显示锁图标）
        [SerializeField] private GameObject blindOverlay;    // 盲盒覆盖层（显示问号）

        [Header("视觉效果")]
        [SerializeField] private Color normalColor = Color.white;
        [SerializeField] private Color blockedColor = new Color(0.7f, 0.7f, 0.7f, 0.8f);
        [SerializeField] private Color highlightedColor = new Color(1f, 0.95f, 0.8f);

        private Tile tileData;
        private Button button;
        private Image backgroundImage;
        private bool isHighlighted = false;
        private bool isShaking = false;

        // 点击事件
        public event System.Action<Tile> OnTileClicked;

        void Awake()
        {
            button = GetComponent<Button>();
            backgroundImage = GetComponent<Image>();
            
            if (tileImage == null)
                tileImage = GetComponent<Image>();
        }

        void Start()
        {
            button.onClick.AddListener(OnClick);
        }

        /// <summary>
        /// 初始化卡牌
        /// </summary>
        public void Initialize(Tile tile, Sprite sprite)
        {
            tileData = tile;
            SetSprite(sprite);
            UpdateVisualState();
        }

        /// <summary>
        /// 设置卡牌图片
        /// </summary>
        public void SetSprite(Sprite sprite)
        {
            if (tileImage != null && sprite != null)
            {
                tileImage.sprite = sprite;
            }
        }

        /// <summary>
        /// 更新视觉状态
        /// 与 Android 原生代码逻辑保持一致：处理封印和盲盒牌的视觉效果
        /// </summary>
        public void UpdateVisualState()
        {
            if (tileData == null) return;

            // 根据状态设置颜色
            if (tileData.state == TileState.BLOCKED)
            {
                SetBlockedState();
            }
            else if (tileData.state == TileState.NORMAL)
            {
                SetNormalState();
            }
            else
            {
                SetInactiveState();
            }

            // 更新封印覆盖层 - 与 Android 原生代码保持一致（显示锁图标）
            if (sealOverlay != null)
            {
                sealOverlay.SetActive(tileData.sealedCount > 0 && tileData.state == TileState.NORMAL);
            }

            // 更新盲盒覆盖层 - 与 Android 原生代码保持一致（显示问号）
            if (blindOverlay != null)
            {
                blindOverlay.SetActive(tileData.isBlind && tileData.state == TileState.NORMAL);
            }

            // 更新按钮可交互性
            button.interactable = (tileData.state == TileState.NORMAL);
        }

        /// <summary>
        /// 设置为正常状态
        /// </summary>
        private void SetNormalState()
        {
            if (isHighlighted)
            {
                backgroundImage.color = highlightedColor;
            }
            else
            {
                backgroundImage.color = normalColor;
            }

            if (blockedOverlay != null)
                blockedOverlay.SetActive(false);
        }

        /// <summary>
        /// 设置为被遮挡状态
        /// </summary>
        private void SetBlockedState()
        {
            backgroundImage.color = blockedColor;
            
            if (blockedOverlay != null)
                blockedOverlay.SetActive(true);
        }

        /// <summary>
        /// 设置为非活动状态（已消除、在槽位中等）
        /// </summary>
        private void SetInactiveState()
        {
            backgroundImage.color = new Color(1, 1, 1, 0.5f);
            button.interactable = false;
            
            if (blockedOverlay != null)
                blockedOverlay.SetActive(false);
        }

        /// <summary>
        /// 设置高亮状态
        /// </summary>
        public void SetHighlighted(bool highlighted)
        {
            isHighlighted = highlighted;
            UpdateVisualState();
        }

        /// <summary>
        /// 设置抖动状态
        /// </summary>
        public void SetShaking(bool shaking)
        {
            if (isShaking == shaking) return;
            
            isShaking = shaking;
            
            if (shaking)
            {
                StartCoroutine(ShakeAnimation());
            }
        }

        /// <summary>
        /// 抖动动画
        /// </summary>
        private System.Collections.IEnumerator ShakeAnimation()
        {
            Vector3 originalPos = transform.localPosition;
            float shakeAmount = 5f;

            while (isShaking && tileData != null && tileData.state == TileState.NORMAL)
            {
                float x = UnityEngine.Random.Range(-shakeAmount, shakeAmount);
                float y = UnityEngine.Random.Range(-shakeAmount, shakeAmount);
                transform.localPosition = originalPos + new Vector3(x, y, 0);
                
                yield return null;
            }

            transform.localPosition = originalPos;
        }

        /// <summary>
        /// 点击事件处理
        /// </summary>
        private void OnClick()
        {
            if (tileData == null) return;
            if (tileData.state != TileState.NORMAL) return;

            Debug.Log($"Tile clicked: {tileData}");
            OnTileClicked?.Invoke(tileData);
        }

        /// <summary>
        /// 获取卡牌数据
        /// </summary>
        public Tile GetTileData()
        {
            return tileData;
        }

        /// <summary>
        /// 设置卡牌数据
        /// </summary>
        public void SetTileData(Tile tile)
        {
            tileData = tile;
            UpdateVisualState();
        }

        void OnDestroy()
        {
            if (button != null)
                button.onClick.RemoveListener(OnClick);
        }
    }
}
