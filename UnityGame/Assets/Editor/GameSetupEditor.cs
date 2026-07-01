using UnityEngine;
using UnityEditor;
using UnityEditor.SceneManagement;

namespace UnityGame.Editor
{
    /// <summary>
    /// 游戏场景自动搭建工具
    /// 菜单栏：Tools/UnityGame/Setup Game Scene
    /// </summary>
    public class GameSetupEditor : EditorWindow
    {
        [MenuItem("Tools/UnityGame/Setup Game Scene")]
        public static void SetupGameScene()
        {
            // 创建GameManager
            CreateGameManager();
            
            // 创建UI Canvas
            CreateCanvas();
            
            Debug.Log("Game scene setup completed! Please check the Hierarchy panel.");
            
            // 保存场景
            EditorSceneManager.MarkSceneDirty(UnityEngine.SceneManagement.SceneManager.GetActiveScene());
        }

        /// <summary>
        /// 创建GameManager对象
        /// </summary>
        private static void CreateGameManager()
        {
            // 检查是否已存在
            GameManager existingManager = FindObjectOfType<GameManager>();
            if (existingManager != null)
            {
                Debug.LogWarning("GameManager already exists in the scene!");
                Selection.activeGameObject = existingManager.gameObject;
                return;
            }

            // 创建GameManager对象
            GameObject gmObj = new GameObject("GameManager");
            GameManager gm = gmObj.AddComponent<GameManager>();
            
            // 设置初始道具数量（用于测试）
            gm.undoCount = 3;
            gm.shuffleCount = 3;
            gm.moveOutCount = 3;
            gm.reviveCount = 1;
            gm.hintCount = 5;
            gm.bombCount = 3;
            gm.jokerCount = 1;
            gm.doublePointsCount = 1;

            Selection.activeGameObject = gmObj;
            Debug.Log("GameManager created!");
        }

        /// <summary>
        /// 创建UI Canvas和基本UI结构
        /// </summary>
        private static void CreateCanvas()
        {
            // 检查是否已存在Canvas
            Canvas existingCanvas = FindObjectOfType<Canvas>();
            if (existingCanvas != null)
            {
                Debug.LogWarning("Canvas already exists in the scene!");
                return;
            }

            // 创建Canvas
            GameObject canvasObj = new GameObject("Canvas");
            Canvas canvas = canvasObj.AddComponent<Canvas>();
            canvas.renderMode = RenderMode.ScreenSpaceOverlay;
            
            CanvasScaler scaler = canvasObj.AddComponent<CanvasScaler>();
            scaler.uiScaleMode = CanvasScaler.ScaleMode.ScaleWithScreenSize;
            scaler.referenceResolution = new Vector2(1080, 1920);
            
            canvasObj.AddComponent<GraphicRaycaster>();

            // 创建EventSystem（如果没有的话）
            if (FindObjectOfType<UnityEngine.EventSystems.EventSystem>() == null)
            {
                GameObject eventSystem = new GameObject("EventSystem");
                eventSystem.AddComponent<UnityEngine.EventSystems.EventSystem>();
                eventSystem.AddComponent<UnityEngine.EventSystems.StandaloneInputModule>();
            }

            // 创建游戏面板
            CreateGamePanel(canvasObj);

            Debug.Log("Canvas created with basic UI structure!");
        }

        /// <summary>
        /// 创建游戏面板
        /// </summary>
        private static void CreateGamePanel(GameObject canvasObj)
        {
            // 创建游戏面板
            GameObject gamePanel = new GameObject("GamePanel");
            gamePanel.transform.SetParent(canvasObj.transform, false);
            
            RectTransform panelRect = gamePanel.AddComponent<RectTransform>();
            panelRect.anchorMin = Vector2.zero;
            panelRect.anchorMax = Vector2.one;
            panelRect.offsetMin = Vector2.zero;
            panelRect.offsetMax = Vector2.zero;

            // 添加Image组件（可选背景）
            Image panelImage = gamePanel.AddComponent<Image>();
            panelImage.color = new Color(0.9f, 0.9f, 0.9f);

            // 创建GameBoard
            CreateGameBoard(gamePanel);

            // 创建SlotContainer
            CreateSlotContainer(gamePanel);

            // 创建GameUI
            CreateGameUI(gamePanel);
        }

        /// <summary>
        /// 创建GameBoard
        /// </summary>
        private static void CreateGameBoard(GameObject parent)
        {
            GameObject boardObj = new GameObject("GameBoard");
            boardObj.transform.SetParent(parent.transform, false);
            
            RectTransform boardRect = boardObj.AddComponent<RectTransform>();
            boardRect.anchoredPosition = new Vector2(0, 100);
            boardRect.sizeDelta = new Vector2(900, 600);
            
            // 添加Image组件（棋盘背景）
            Image boardImage = boardObj.AddComponent<Image>();
            boardImage.color = new Color(0.8f, 0.8f, 0.8f);

            // 添加GameBoard脚本
            boardObj.AddComponent<UnityGame.UI.GameBoard>();
        }

        /// <summary>
        /// 创建SlotContainer
        /// </summary>
        private static void CreateSlotContainer(GameObject parent)
        {
            GameObject slotObj = new GameObject("SlotContainer");
            slotObj.transform.SetParent(parent.transform, false);
            
            RectTransform slotRect = slotObj.AddComponent<RectTransform>();
            slotRect.anchoredPosition = new Vector2(0, -350);
            slotRect.sizeDelta = new Vector2(900, 120);

            // 添加SlotUI脚本
            slotObj.AddComponent<UnityGame.UI.SlotUI>();
        }

        /// <summary>
        /// 创建GameUI
        /// </summary>
        private static void CreateGameUI(GameObject parent)
        {
            GameObject uiObj = new GameObject("GameUI");
            uiObj.transform.SetParent(parent.transform, false);
            
            // 添加GameUI脚本
            UnityGame.UI.GameUI gameUI = uiObj.AddComponent<UnityGame.UI.GameUI>();

            // 创建分数文本
            CreateText(uiObj, "ScoreText", "Score: 0", 24);

            // 创建关卡文本
            CreateText(uiObj, "LevelText", "Level 1", 20);

            Debug.Log("GameUI created! Please assign references in the Inspector.");
        }

        /// <summary>
        /// 创建文本对象
        /// </summary>
        private static GameObject CreateText(GameObject parent, string name, string text, int fontSize)
        {
            GameObject textObj = new GameObject(name);
            textObj.transform.SetParent(parent.transform, false);
            
            RectTransform textRect = textObj.AddComponent<RectTransform>();
            textRect.anchoredPosition = new Vector2(0, 400);
            textRect.sizeDelta = new Vector2(300, 50);
            
            TMPro.TextMeshProUGUI tmp = textObj.AddComponent<TMPro.TextMeshProUGUI>();
            tmp.text = text;
            tmp.fontSize = fontSize;
            tmp.alignment = TMPro.TextAlignmentOptions.Center;

            return textObj;
        }
    }
}
