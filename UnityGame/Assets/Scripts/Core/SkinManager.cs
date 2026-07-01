using UnityEngine;

namespace UnityGame.Core
{
    /// <summary>
    /// 皮肤管理器
    /// 管理游戏的不同皮肤主题
    /// </summary>
    public class SkinManager : MonoBehaviour
    {
        private static SkinManager _instance;
        public static SkinManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    GameObject go = new GameObject("SkinManager");
                    _instance = go.AddComponent<SkinManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        [Header("皮肤设置")]
        [SerializeField] private SkinType currentSkin = SkinType.Default;

        // 皮肤类型枚举
        public enum SkinType
        {
            Default,    // 默认皮肤
            Dark,       // 暗黑皮肤
            Colorful,   // 彩色皮肤
            Cute        // 可爱皮肤
        }

        // 皮肤切换事件
        public event System.Action<SkinType> OnSkinChanged;

        void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);

            // 加载保存的皮肤设置
            LoadSkinSettings();
        }

        #region 公共方法

        /// <summary>
        /// 设置皮肤
        /// </summary>
        public void SetSkin(SkinType skin)
        {
            if (currentSkin != skin)
            {
                currentSkin = skin;
                SaveSkinSettings();
                OnSkinChanged?.Invoke(currentSkin);
                Debug.Log($"Skin changed to: {skin}");
            }
        }

        /// <summary>
        /// 获取当前皮肤
        /// </summary>
        public SkinType GetCurrentSkin()
        {
            return currentSkin;
        }

        /// <summary>
        /// 获取当前皮肤的名称
        /// </summary>
        public string GetCurrentSkinName()
        {
            return LocalizationManager.Instance != null 
                ? LocalizationManager.Instance.GetLocalizedText($"skin_{currentSkin.ToString().ToLower()}")
                : currentSkin.ToString();
        }

        #endregion

        #region 私有方法

        /// <summary>
        /// 加载皮肤设置
        /// </summary>
        private void LoadSkinSettings()
        {
            string savedSkin = PlayerPrefs.GetString("Skin", "Default");
            if (System.Enum.TryParse(savedSkin, out SkinType skin))
            {
                currentSkin = skin;
            }
        }

        /// <summary>
        /// 保存皮肤设置
        /// </summary>
        private void SaveSkinSettings()
        {
            PlayerPrefs.SetString("Skin", currentSkin.ToString());
            PlayerPrefs.Save();
        }

        #endregion
    }
}
