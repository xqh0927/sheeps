using UnityEngine;
using UnityEngine.UI;
using TMPro;
using UnityGame.Data;
using UnityGame.Game;

namespace UnityGame.UI
{
    /// <summary>
    /// 游戏主UI管理器
    /// 管理所有UI元素和按钮
    /// </summary>
    public class GameUI : MonoBehaviour
    {
        [Header("UI面板")]
        [SerializeField] private GameObject menuPanel;
        [SerializeField] private GameObject gamePanel;
        [SerializeField] private GameObject winPanel;
        [SerializeField] private GameObject losePanel;

        [Header("游戏HUD")]
        [SerializeField] private TextMeshProUGUI scoreText;
        [SerializeField] private TextMeshProUGUI levelText;
        [SerializeField] private TextMeshProUGUI statusText;

        [Header("槽位显示")]
        [SerializeField] private SlotUI slotUI;

        [Header("棋盘显示")]
        [SerializeField] private GameBoard gameBoard;

        [Header("道具按钮")]
        [SerializeField] private Button undoButton;
        [SerializeField] private Button shuffleButton;
        [SerializeField] private Button moveOutButton;
        [SerializeField] private Button reviveButton;
        [SerializeField] private Button hintButton;
        [SerializeField] private Button bombButton;
        [SerializeField] private Button jokerButton;
        [SerializeField] private Button doublePointsButton;

        [Header("道具数量文本")]
        [SerializeField] private TextMeshProUGUI undoCountText;
        [SerializeField] private TextMeshProUGUI shuffleCountText;
        [SerializeField] private TextMeshProUGUI moveOutCountText;
        [SerializeField] private TextMeshProUGUI reviveCountText;
        [SerializeField] private TextMeshProUGUI hintCountText;
        [SerializeField] private TextMeshProUGUI bombCountText;
        [SerializeField] private TextMeshProUGUI jokerCountText;
        [SerializeField] private TextMeshProUGUI doublePointsCountText;

        [Header("游戏控制按钮")]
        [SerializeField] private Button restartButton;
        [SerializeField] private Button menuButton;
        [SerializeField] private Button nextLevelButton;

        private GameManager gameManager;

        void Start()
        {
            gameManager = GameManager.Instance;
            if (gameManager == null)
            {
                Debug.LogError("GameManager not found!");
                return;
            }

            // 注册事件
            gameManager.OnGameStateChanged += OnGameStateChanged;
            gameManager.OnBoardChanged += OnBoardChanged;
            gameManager.OnSlotChanged += OnSlotChanged;
            gameManager.OnGameEnd += OnGameEnd;

            // 注册按钮事件
            RegisterButtonEvents();

            // 初始状态
            ShowMenu();
        }

        /// <summary>
        /// 注册按钮事件
        /// </summary>
        private void RegisterButtonEvents()
        {
            if (undoButton != null)
                undoButton.onClick.AddListener(() => gameManager.UseUndo());

            if (shuffleButton != null)
                shuffleButton.onClick.AddListener(() => gameManager.UseShuffle());

            if (moveOutButton != null)
                moveOutButton.onClick.AddListener(() => gameManager.UseMoveOut());

            if (reviveButton != null)
                reviveButton.onClick.AddListener(() => gameManager.UseRevive());

            if (hintButton != null)
                hintButton.onClick.AddListener(() => gameManager.UseHint());

            if (bombButton != null)
                bombButton.onClick.AddListener(() => gameManager.UseBomb());

            if (jokerButton != null)
                jokerButton.onClick.AddListener(() => gameManager.UseJoker());

            if (doublePointsButton != null)
                doublePointsButton.onClick.AddListener(() => gameManager.UseDoublePoints());

            if (restartButton != null)
                restartButton.onClick.AddListener(() => gameManager.RestartLevel());

            if (menuButton != null)
                menuButton.onClick.AddListener(() => gameManager.GoBackToMenu());

            if (nextLevelButton != null)
                nextLevelButton.onClick.AddListener(() => gameManager.LoadLevel(gameManager.currentLevelId + 1));
        }

        /// <summary>
        /// 游戏状态改变
        /// </summary>
        private void OnGameStateChanged()
        {
            UpdateUI();
        }

        /// <summary>
        /// 棋盘改变
        /// </summary>
        private void OnBoardChanged()
        {
            if (gameBoard != null && gameManager != null)
            {
                gameBoard.UpdateBoard(gameManager.boardTiles);
            }
        }

        /// <summary>
        /// 槽位改变
        /// </summary>
        private void OnSlotChanged()
        {
            if (slotUI != null && gameManager != null)
            {
                slotUI.UpdateSlot(gameManager.slotData.tiles);
            }
        }

        /// <summary>
        /// 游戏结束
        /// </summary>
        private void OnGameEnd(GameStatus status)
        {
            if (status == GameStatus.WON)
            {
                ShowWinPanel();
            }
            else if (status == GameStatus.LOST)
            {
                ShowLosePanel();
            }
        }

        /// <summary>
        /// 更新UI显示
        /// </summary>
        private void UpdateUI()
        {
            if (gameManager == null) return;

            // 更新分数
            if (scoreText != null)
                scoreText.text = $"分数: {gameManager.score}";

            // 更新关卡
            if (levelText != null)
                levelText.text = $"关卡 {gameManager.currentLevelId}";

            // 更新道具数量
            UpdateToolCounts();

            // 更新道具按钮可交互性
            UpdateToolButtons();
        }

        /// <summary>
        /// 更新道具数量显示
        /// </summary>
        private void UpdateToolCounts()
        {
            if (undoCountText != null)
                undoCountText.text = gameManager.undoCount.ToString();

            if (shuffleCountText != null)
                shuffleCountText.text = gameManager.shuffleCount.ToString();

            if (moveOutCountText != null)
                moveOutCountText.text = gameManager.moveOutCount.ToString();

            if (reviveCountText != null)
                reviveCountText.text = gameManager.reviveCount.ToString();

            if (hintCountText != null)
                hintCountText.text = gameManager.hintCount.ToString();

            if (bombCountText != null)
                bombCountText.text = gameManager.bombCount.ToString();

            if (jokerCountText != null)
                jokerCountText.text = gameManager.jokerCount.ToString();

            if (doublePointsCountText != null)
                doublePointsCountText.text = gameManager.doublePointsCount.ToString();
        }

        /// <summary>
        /// 更新道具按钮可交互性
        /// </summary>
        private void UpdateToolButtons()
        {
            if (undoButton != null)
                undoButton.interactable = gameManager.undoCount > 0;

            if (shuffleButton != null)
                shuffleButton.interactable = gameManager.shuffleCount > 0;

            if (moveOutButton != null)
                moveOutButton.interactable = gameManager.moveOutCount > 0;

            if (reviveButton != null)
                reviveButton.interactable = gameManager.reviveCount > 0 && gameManager.gameStatus == GameStatus.LOST;

            if (hintButton != null)
                hintButton.interactable = gameManager.hintCount > 0;

            if (bombButton != null)
                bombButton.interactable = gameManager.bombCount > 0;

            if (jokerButton != null)
                jokerButton.interactable = gameManager.jokerCount > 0;

            if (doublePointsButton != null)
                doublePointsButton.interactable = gameManager.doublePointsCount > 0 && !gameManager.isDoublePointsActive;
        }

        /// <summary>
        /// 显示主菜单
        /// </summary>
        public void ShowMenu()
        {
            if (menuPanel != null) menuPanel.SetActive(true);
            if (gamePanel != null) gamePanel.SetActive(false);
            if (winPanel != null) winPanel.SetActive(false);
            if (losePanel != null) losePanel.SetActive(false);
        }

        /// <summary>
        /// 显示游戏界面
        /// </summary>
        public void ShowGame()
        {
            if (menuPanel != null) menuPanel.SetActive(false);
            if (gamePanel != null) gamePanel.SetActive(true);
            if (winPanel != null) winPanel.SetActive(false);
            if (losePanel != null) losePanel.SetActive(false);

            UpdateUI();
        }

        /// <summary>
        /// 显示胜利界面
        /// </summary>
        public void ShowWinPanel()
        {
            if (winPanel != null)
            {
                winPanel.SetActive(true);
                
                // 更新胜利文本
                TextMeshProUGUI winScoreText = winPanel.GetComponentInChildren<TextMeshProUGUI>();
                if (winScoreText != null)
                    winScoreText.text = $"恭喜胜利！\n得分: {gameManager.score}";
            }
        }

        /// <summary>
        /// 显示失败界面
        /// </summary>
        public void ShowLosePanel()
        {
            if (losePanel != null)
            {
                losePanel.SetActive(true);
                
                // 更新失败文本
                TextMeshProUGUI loseScoreText = losePanel.GetComponentInChildren<TextMeshProUGUI>();
                if (loseScoreText != null)
                    loseScoreText.text = $"游戏结束\n得分: {gameManager.score}";
            }
        }

        void OnDestroy()
        {
            // 注销事件
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
