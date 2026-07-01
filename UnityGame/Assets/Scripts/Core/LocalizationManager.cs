using System.Collections.Generic;
using UnityEngine;

namespace UnityGame.Core
{
    /// <summary>
    /// 多语言管理器
    /// 与 Android 原生代码（GameViewModel.kt - getLocalizedString）逻辑保持一致：
    /// 1. 支持 5 种语言：中文、英文、繁体中文、日文、韩文
    /// 2. 所有 UI 文本使用多语言 Key
    /// 3. 用户可以在设置中切换语言
    /// </summary>
    public class LocalizationManager : MonoBehaviour
    {
        private static LocalizationManager _instance;
        public static LocalizationManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    GameObject go = new GameObject("LocalizationManager");
                    _instance = go.AddComponent<LocalizationManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        [Header("语言设置")]
        [SerializeField] private Language currentLanguage = Language.Chinese;

        // 语言枚举
        public enum Language
        {
            Chinese,           // 中文（简体）
            English,           // 英文
            TraditionalChinese, // 繁体中文
            Japanese,          // 日文
            Korean             // 韩文
        }

        // 多语言文本字典：Key -> (中文, 英文, 繁体, 日文, 韩文)
        private Dictionary<string, string[]> localizedTexts;

        // 语言切换事件
        public event System.Action<Language> OnLanguageChanged;

        void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);

            // 加载保存的语言设置
            LoadLanguageSettings();

            // 初始化多语言文本
            InitializeLocalizedTexts();
        }

        #region 公共方法

        /// <summary>
        /// 获取本地化文本
        /// 与 Android 原生代码：getLocalizedString(zh, en, tw, ja, ko) 一致
        /// </summary>
        public string GetLocalizedText(string key)
        {
            if (localizedTexts.ContainsKey(key))
            {
                string[] texts = localizedTexts[key];
                int index = GetLanguageIndex(currentLanguage);
                if (index < texts.Length && !string.IsNullOrEmpty(texts[index]))
                {
                    return texts[index];
                }
                // 如果当前语言没有翻译，返回中文（默认）
                return texts[0];
            }

            // 如果找不到 Key，返回 Key 本身
            Debug.LogWarning($"Localization key not found: {key}");
            return key;
        }

        /// <summary>
        /// 设置语言
        /// </summary>
        public void SetLanguage(Language language)
        {
            if (currentLanguage != language)
            {
                currentLanguage = language;
                SaveLanguageSettings();
                OnLanguageChanged?.Invoke(currentLanguage);
                Debug.Log($"Language changed to: {language}");
            }
        }

        /// <summary>
        /// 获取当前语言
        /// </summary>
        public Language GetCurrentLanguage()
        {
            return currentLanguage;
        }

        /// <summary>
        /// 获取当前语言的索引
        /// </summary>
        private int GetLanguageIndex(Language language)
        {
            return (int)language;
        }

        #endregion

        #region 私有方法

        /// <summary>
        /// 加载语言设置
        /// </summary>
        private void LoadLanguageSettings()
        {
            string savedLanguage = PlayerPrefs.GetString("Language", "Chinese");
            if (System.Enum.TryParse(savedLanguage, out Language language))
            {
                currentLanguage = language;
            }
        }

        /// <summary>
        /// 保存语言设置
        /// </summary>
        private void SaveLanguageSettings()
        {
            PlayerPrefs.SetString("Language", currentLanguage.ToString());
            PlayerPrefs.Save();
        }

        /// <summary>
        /// 初始化多语言文本
        /// 与 Android 原生代码逻辑保持一致：支持 5 种语言
        /// </summary>
        private void InitializeLocalizedTexts()
        {
            localizedTexts = new Dictionary<string, string[]>();

            // 游戏相关文本
            localizedTexts["game_title"] = new string[] { "羊了个羊", "Sheep's Got Game", "羊了個羊", "羊のゲーム", "양들의 게임" };
            localizedTexts["game_won"] = new string[] { "胜利！", "You Won!", "勝利！", "勝利！", "승리!" };
            localizedTexts["game_lost"] = new string[] { "失败！", "Game Over!", "失敗！", "失敗！", "실패!" };
            localizedTexts["game_restart"] = new string[] { "重新开始", "Restart", "重新開始", "再開する", "다시 시작" };
            localizedTexts["game_pause"] = new string[] { "暂停", "Pause", "暫停", "一時停止", "일시정지" };

            // 道具相关文本
            localizedTexts["item_undo"] = new string[] { "撤销", "Undo", "撤銷", "元に戻す", "실행 취소" };
            localizedTexts["item_shuffle"] = new string[] { "洗牌", "Shuffle", "洗牌", "シャッフル", "섞기" };
            localizedTexts["item_move_out"] = new string[] { "移出", "Move Out", "移出", "移動する", "이동" };
            localizedTexts["item_revive"] = new string[] { "复活", "Revive", "復活", "復活", "부활" };
            localizedTexts["item_hint"] = new string[] { "提示", "Hint", "提示", "ヒント", "힌트" };
            localizedTexts["item_bomb"] = new string[] { "炸弹", "Bomb", "炸彈", "爆弾", "폭탄" };
            localizedTexts["item_joker"] = new string[] { "万能牌", "Joker", "萬能牌", "ジョーカー", "조커" };
            localizedTexts["item_double_points"] = new string[] { "双倍积分", "Double Points", "雙倍積分", "ダブルポイント", "더블 포인트" };

            // 菜单相关文本
            localizedTexts["menu_start"] = new string[] { "开始游戏", "Start Game", "開始遊戲", "ゲーム開始", "게임 시작" };
            localizedTexts["menu_settings"] = new string[] { "设置", "Settings", "設定", "設定", "설정" };
            localizedTexts["menu_leaderboard"] = new string[] { "排行榜", "Leaderboard", "排行榜", "ランキング", "리더보드" };

            // 设置相关文本
            localizedTexts["settings_sound"] = new string[] { "音效", "Sound", "音效", "サウンド", "효과음" };
            localizedTexts["settings_music"] = new string[] { "音乐", "Music", "音樂", "音楽", "음악" };
            localizedTexts["settings_vibrate"] = new string[] { "震动", "Vibrate", "震動", "バイブレーション", "진동" };
            localizedTexts["settings_language"] = new string[] { "语言", "Language", "語言", "言語", "언어" };

            // 提示文本
            localizedTexts["toast_hint_not_found"] = new string[] { "没有可提示的卡牌", "No hints available", "沒有可提示的卡牌", "ヒントがありません", "힌트가 없습니다" };
            localizedTexts["toast_bomb_need_2"] = new string[] { "槽位需要至少2张卡牌", "Need at least 2 tiles in slot", "槽位需要至少2張卡牌", "スロットに少なくとも2枚必要", "슬롯에 최소 2개 필요" };
            localizedTexts["toast_joker_need_pair"] = new string[] { "槽位需要有2张相同的卡牌", "Need a pair in slot", "槽位需要有2張相同的卡牌", "スロットにペアが必要", "슬롯에 쌍이 필요" };

            // 皮肤相关文本
            localizedTexts["skin_default"] = new string[] { "默认", "Default", "默認", "デフォルト", "기본" };
            localizedTexts["skin_dark"] = new string[] { "暗黑", "Dark", "暗黑", "ダーク", "다크" };
            localizedTexts["skin_colorful"] = new string[] { "彩色", "Colorful", "彩色", "カラフル", "컬러풀" };
            localizedTexts["skin_cute"] = new string[] { "可爱", "Cute", "可愛", "キュート", "귀여운" };
            localizedTexts["settings_skin"] = new string[] { "皮肤", "Skin", "皮膚", "スキン", "스킨" };

            Debug.Log($"Localized texts initialized. Total keys: {localizedTexts.Count}");
        }

        #endregion
    }
}
