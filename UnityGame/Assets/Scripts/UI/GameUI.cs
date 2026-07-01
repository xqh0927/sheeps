using UnityEngine;
using UnityEngine.UI;
using TMPro;
using UnityGame.Data;
using UnityGame.Game;

namespace UnityGame.UI
{
    /// <summary>
    /// 游戏主UI管理器
    /// 自动连接GameManager，无需手动配置引用
    /// </summary>
    public class GameUI : MonoBehaviour
    {
        private GameManager gameManager;
        
        // HUD文本（自动查找或创建）
        private Text scoreTextLegacy;
        private TextMeshProUGUI scoreText;
        private TextMeshProUGUI levelText;
        private Text statusText;

        void Start()
        {
            // 延迟一帧等待GameManager初始化
            StartCoroutine(InitDelayed());
        }

        private System.Collections.IEnumerator InitDelayed()
        {
            yield return null; // 等待一帧

            gameManager = GameManager.Instance;
            if (gameManager == null)
            {
                Debug.LogError("[GameUI] GameManager not found!");
                yield break;
            }

            // 注册事件
            gameManager.OnGameStateChanged += OnGameStateChanged;
            gameManager.OnBoardChanged += OnBoardChanged;
            gameManager.OnSlotChanged += OnSlotChanged;
            gameManager.OnGameEnd += OnGameEnd;

            // 查找或创建HUD文本
            FindOrCreateHUD();

            // 初始更新
            UpdateHUD();
            
            Debug.Log("[GameUI] Initialized and connected to GameManager");
        }

        /// <summary>
        /// 查找或创建HUD元素
        /// </summary>
        private void FindOrCreateHUD()
        {
            // 尝试从子对象中查找TextMeshProUGUI
            Transform hudTransform = transform.Find("ScoreText");
            if (hudTransform != null)
                scoreText = hudTransform.GetComponent<TextMeshProUGUI>();
            
            if (scoreText == null)
                scoreText = GetComponentInChildren<TextMeshProUGUI>();

            // 如果没有TextMeshPro，使用普通Text
            if (scoreText == null)
            {
                scoreTextLegacy = GetComponentInChildren<Text>(true);
                
                // 如果都没有，创建一个
                if (scoreTextLegacy == null)
                    CreateScoreLabel();
            }

            Debug.Log($"[GameUI] Score text: {(scoreText != null ? "TMP" : (scoreTextLegacy != null ? "Legacy" : "Created"))}");
        }

        private void CreateScoreLabel()
        {
            GameObject labelObj = new GameObject("ScoreText_Label");
            labelObj.transform.SetParent(transform, false);
            
            RectTransform rect = labelObj.AddComponent<RectTransform>();
            rect.anchoredPosition = new Vector2(0, 380);
            rect.sizeDelta = new Vector2(300, 40);

            Text txt = labelObj.AddComponent<Text>();
            txt.text = "分数: 0";
            txt.fontSize = 22;
            txt.fontStyle = FontStyle.Bold;
            txt.alignment = TextAnchor.MiddleCenter;
            txt.color = Color.black;

            Font sysFont = Resources.GetBuiltinResource<Font>("LegacyRuntime.ttf");
            if (sysFont == null) sysFont = Font.CreateDynamicFontFromOSFont("Microsoft YaHei", 22);
            if (sysFont == null) sysFont = Font.CreateDynamicFontFromOSFont("Arial", 22);
            if (sysFont != null) txt.font = sysFont;

            scoreTextLegacy = txt;
        }

        private void OnGameStateChanged()
        {
            UpdateHUD();
        }

        private void OnBoardChanged()
        {
            // GameBoard自己处理渲染，这里只更新状态
            UpdateHUD();
        }

        private void OnSlotChanged()
        {
            UpdateHUD();
        }

        private void OnGameEnd(GameStatus status)
        {
            string message = status == GameStatus.WON 
                ? $"🎉 胜利！得分: {gameManager.score}" 
                : $"💀 游戏结束！得分: {gameManager.score}";
            
            Debug.Log($"[GameUI] {message}");

            // 显示结果（简单方式：在Console + 更新分数文本）
            if (scoreText != null)
                scoreText.text = message;
            else if (scoreTextLegacy != null)
                scoreTextLegacy.text = message;
        }

        private void UpdateHUD()
        {
            if (gameManager == null) return;

            string displayText = $"分数: {gameManager.score} | 关卡 {gameManager.currentLevelId}";
            
            if (gameManager.gameStatus == GameStatus.PLAYING)
            {
                int remaining = gameManager.boardTiles.Count(t => t.state == TileState.NORMAL || t.state == TileState.BLOCKED);
                displayText += $" | 剩余: {remaining}";
            }
            else if (gameManager.gameStatus == GameStatus.LOST)
            {
                displayText += " | 槽位已满!";
            }

            if (scoreText != null)
                scoreText.text = displayText;
            else if (scoreTextLegacy != null)
                scoreTextLegacy.text = displayText;
        }

        void OnDestroy()
        {
            if (gameManager != null)
            {
                gameManager.OnGameStateChanged -= OnGameStateChanged;
                gameManager.OnBoardChanged -= OnBoardChanged;
                gameManager.OnSlotChanged -= OnSlotChanged;
                gameManager.OnGameEnd -= OnGameEnd;
            }
        }
    }
}
