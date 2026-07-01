using System;
using System.Collections.Generic;
using System.Linq;
using UnityEngine;
using UnityGame.Data;
using UnityGame.Core;

namespace UnityGame.Game
{
    /// <summary>
    /// 游戏状态枚举
    /// </summary>
    public enum GameStatus
    {
        INIT,       // 初始化（隐私协议）
        MENU,       // 主菜单
        PLAYING,    // 游戏中
        WON,        // 胜利
        LOST        // 失败
    }

    /// <summary>
    /// 游戏管理器（单例）
    /// 负责游戏主逻辑的控制
    /// </summary>
    public class GameManager : MonoBehaviour
    {
        public static GameManager Instance { get; private set; }

        // 游戏状态
        public GameStatus gameStatus = GameStatus.MENU;
        public int currentLevelId = 1;
        public int score = 0;
        public bool isDoublePointsActive = false;
        
        // 棋盘和槽位数据
        public List<Tile> boardTiles = new List<Tile>();
        public SlotData slotData = new SlotData();
        public List<Tile> movedOutTiles = new List<Tile>();

        // 道具数量
        public int undoCount = 0;
        public int shuffleCount = 0;
        public int moveOutCount = 0;
        public int reviveCount = 0;
        public int hintCount = 0;
        public int bombCount = 0;
        public int jokerCount = 0;
        public int doublePointsCount = 0;

        // 历史状态栈（用于撤销）
        private Stack<GameHistoryState> historyStack = new Stack<GameHistoryState>();

        // 事件系统
        public event Action OnGameStateChanged;
        public event Action OnBoardChanged;
        public event Action OnSlotChanged;
        public event Action<GameStatus> OnGameEnd;
        public event System.Action<string> OnTileUnsealed;  // 封印解锁事件
        public event System.Action<List<string>> OnTilesHighlighted;  // 提示高亮事件（支持多张卡牌）

        void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        void Start()
        {
            LoadLevel(currentLevelId);
        }

        /// <summary>
        /// 加载关卡
        /// </summary>
        public void LoadLevel(int levelId)
        {
            currentLevelId = levelId;
            historyStack.Clear();
            score = 0;
            isDoublePointsActive = false;

            // 生成关卡
            GameLevelGenerator generator = new GameLevelGenerator();
            List<Tile> generatedTiles = generator.GenerateSolvableLevel(levelId);
            
            // 计算初始遮挡状态
            boardTiles = GameEngine.CalculateBlockedStates(generatedTiles);
            
            // 更新游戏状态
            gameStatus = GameStatus.PLAYING;
            
            // 触发事件
            OnGameStateChanged?.Invoke();
            OnBoardChanged?.Invoke();
            OnSlotChanged?.Invoke();
            
            Debug.Log($"Level {levelId} loaded. Tiles: {boardTiles.Count}");
        }

        /// <summary>
        /// 点击卡牌
        /// 与 Android 原生代码（GameLogicDelegate.kt）逻辑保持一致：
        /// 优先处理封印解锁，再处理正常移入槽位
        /// </summary>
        public void ClickTile(Tile tile)
        {
            if (gameStatus != GameStatus.PLAYING) return;
            if (tile.state != TileState.NORMAL) return;  // 只能点击正常状态的牌

            // 保存历史状态（用于撤销）
            SaveHistoryState();

            // 处理封印解锁逻辑 - 与 Android 原生代码保持一致
            if (tile.sealedCount > 0)
            {
                // 解锁封印
                tile.sealedCount--;
                
                // 播放解封音效和震动 - 与 Android 原生代码一致
                if (AudioManager.Instance != null)
                {
                    AudioManager.Instance.PlayUnsealSound();
                    AudioManager.Instance.Vibrate();
                }
                
                // 刷新棋盘遮挡状态
                boardTiles = GameEngine.CalculateBlockedStates(boardTiles);
                
                // 触发事件
                OnTileUnsealed?.Invoke(tile.id);
                
                Debug.Log($"Tile {tile.id} unsealed. Remaining seals: {tile.sealedCount}");
            }
            else
            {
                // 播放点击音效 - 与 Android 原生代码一致
                if (AudioManager.Instance != null)
                {
                    AudioManager.Instance.PlayClickSound();
                }

                // 正常移入槽位
                tile.state = TileState.IN_SLOT;
                
                // 添加到槽位
                if (slotData.CanAdd())
                {
                    slotData.AddTile(tile);
                    
                    // 刷新棋盘遮挡状态
                    boardTiles = GameEngine.CalculateBlockedStates(boardTiles);
                    
                    // 处理匹配和检查游戏结束
                    ProcessSlotMatchAndCheckEndGame();
                }
            }
            
            // 触发事件
            OnBoardChanged?.Invoke();
            OnSlotChanged?.Invoke();
        }

        /// <summary>
        /// 处理槽位匹配并检查游戏结束
        /// 与 Android 原生代码（GameLogicDelegate.kt）逻辑保持一致：
        /// 每次合并后自动解锁一个盲盒牌
        /// </summary>
        private void ProcessSlotMatchAndCheckEndGame()
        {
            // 检查是否有3张相同类型的卡牌
            var grouped = slotData.tiles.GroupBy(t => t.type);
            bool matched = false;

            foreach (var group in grouped)
            {
                if (group.Count() >= 3)
                {
                    // 移除匹配的卡牌
                    var toRemove = group.Take(3).ToList();
                    foreach (var tile in toRemove)
                    {
                        bool removed = slotData.RemoveTile(tile);
                        if (!removed)
                        {
                            Debug.LogError($"Failed to remove tile {tile.id} (type={tile.type}) from slot!");
                        }
                    }

                    // 加分
                    int points = 100;
                    if (isDoublePointsActive) points *= 2;
                    score += points;

                    matched = true;
                    
                    // 播放匹配音效 - 与 Android 原生代码一致
                    if (AudioManager.Instance != null)
                    {
                        AudioManager.Instance.PlayMatchSound();
                    }
                    
                    Debug.Log($"Match! +{points} points. Total: {score}");
                    
                    // 每次合并后自动解锁一个棋盘上的盲盒牌 - 与 Android 原生代码保持一致
                    UnlockOneBlindTile();
                    
                    break;  // 一次只处理一组匹配
                }
            }

            if (matched)
            {
                // 递归检查是否还有匹配
                ProcessSlotMatchAndCheckEndGame();
            }
            else
            {
                // 检查游戏结束条件
                CheckGameEnd();
            }
        }

        /// <summary>
        /// 解锁一个盲盒牌
        /// 与 Android 原生代码（GameLogicDelegate.kt）逻辑保持一致：
        /// 每合并一个自动解锁一个棋盘上的盲盒牌
        /// </summary>
        private void UnlockOneBlindTile()
        {
            // 寻找棋盘上的一个盲盒牌 - 与 Android 代码：firstOrNull { it.isBlind } 一致
            Tile blindTile = boardTiles.FirstOrDefault(t => t.isBlind && 
                (t.state == TileState.NORMAL || t.state == TileState.BLOCKED));
            
            if (blindTile != null)
            {
                blindTile.isBlind = false;
                Debug.Log($"Blind tile {blindTile.id} unlocked!");
                
                // 触发事件（更新棋盘显示）
                OnBoardChanged?.Invoke();
            }
        }

        /// <summary>
        /// 检查游戏是否结束
        /// </summary>
        private void CheckGameEnd()
        {
            // 检查是否胜利（棋盘没有卡牌了）
            var remainingTiles = GameEngine.GetVisibleTiles(boardTiles);
            if (remainingTiles.Count == 0 && slotData.tiles.Count == 0)
            {
                gameStatus = GameStatus.WON;
                
                // 播放胜利音效 - 与 Android 原生代码一致
                if (AudioManager.Instance != null)
                {
                    AudioManager.Instance.PlayWinSound();
                }
                
                OnGameEnd?.Invoke(GameStatus.WON);
                Debug.Log($"You Won! Score: {score}");
                return;
            }

            // 检查是否失败（槽位满了）
            if (slotData.IsFull())
            {
                gameStatus = GameStatus.LOST;
                
                // 播放失败音效 - 与 Android 原生代码一致
                if (AudioManager.Instance != null)
                {
                    AudioManager.Instance.PlayLoseSound();
                }
                
                OnGameEnd?.Invoke(GameStatus.LOST);
                Debug.Log("Game Over! Slot is full.");
            }
        }

        /// <summary>
        /// 使用撤销道具
        /// </summary>
        public void UseUndo()
        {
            if (undoCount <= 0 || historyStack.Count == 0) return;
            if (gameStatus != GameStatus.PLAYING && gameStatus != GameStatus.LOST) return;

            // 恢复上一个状态
            GameHistoryState prevState = historyStack.Pop();
            boardTiles = prevState.boardTiles;
            slotData.tiles = prevState.slotTiles;
            movedOutTiles = prevState.movedOutTiles;

            undoCount--;
            
            OnBoardChanged?.Invoke();
            OnSlotChanged?.Invoke();
            
            // 修复Bug #2：道具使用后触发匹配检测
            ProcessSlotMatchAndCheckEndGame();
            
            Debug.Log("Undo used.");
        }

        /// <summary>
        /// 使用洗牌道具
        /// 与 Android 原生代码（GameToolDelegate.kt）逻辑保持一致：
        /// 打乱可见卡牌的类型，而不是位置
        /// </summary>
        public void UseShuffle()
        {
            if (shuffleCount <= 0) return;
            if (gameStatus != GameStatus.PLAYING) return;

            SaveHistoryState();

            // 获取所有活跃卡牌（NORMAL 或 BLOCKED 状态）- 与 Android 代码保持一致
            var activeTiles = boardTiles.Where(t => t.state == TileState.NORMAL || t.state == TileState.BLOCKED).ToList();
            
            if (activeTiles.Count == 0) return;

            // 获取所有类型并打乱 - 与 Android 代码：activeTiles.map { it.type }.shuffled() 一致
            List<int> types = activeTiles.Select(t => t.type).ToList();
            ShuffleList(types);

            // 将打乱后的类型重新分配给活跃卡牌 - 与 Android 代码：tile.copy(type = shuffledTypes[idx++]) 一致
            for (int i = 0; i < activeTiles.Count; i++)
            {
                activeTiles[i].type = types[i];
            }

            // 重新计算遮挡状态 - 与 Android 代码：calculateBlockedStates(newBoard) 一致
            boardTiles = GameEngine.CalculateBlockedStates(boardTiles);

            shuffleCount--;
            
            // 播放洗牌音效
            if (AudioManager.Instance != null)
            {
                AudioManager.Instance.PlayShuffleSound();
            }
            
            OnBoardChanged?.Invoke();
            
            Debug.Log("Shuffle used. Types shuffled among active tiles.");
        }

        /// <summary>
        /// 打乱列表顺序（通用方法）
        /// </summary>
        private void ShuffleList<T>(List<T> list)
        {
            System.Random random = new System.Random();
            int n = list.Count;
            while (n > 1)
            {
                n--;
                int k = random.Next(n + 1);
                T temp = list[k];
                list[k] = list[n];
                list[n] = temp;
            }
        }

        /// <summary>
        /// 使用移出道具（将槽位前3张移出）
        /// </summary>
        public void UseMoveOut()
        {
            if (moveOutCount <= 0) return;
            if (gameStatus != GameStatus.PLAYING) return;
            if (slotData.tiles.Count == 0) return;

            SaveHistoryState();

            // 将槽位前3张移出
            int count = Math.Min(3, slotData.tiles.Count);
            for (int i = 0; i < count; i++)
            {
                Tile tile = slotData.tiles[0];
                tile.state = TileState.MOVED_OUT;
                slotData.RemoveTile(tile);
                movedOutTiles.Add(tile);
            }

            moveOutCount--;
            
            OnSlotChanged?.Invoke();
            
            // 修复Bug #2：道具使用后触发匹配检测
            ProcessSlotMatchAndCheckEndGame();
            
            Debug.Log("Move Out used.");
        }

        /// <summary>
        /// 使用复活道具
        /// 与 Android 原生代码（GameViewModel.kt）逻辑保持一致：
        /// 1. 将槽位前3张卡牌移出到置物架（movedOutTiles）
        /// 2. 恢复游戏状态为 PLAYING
        /// </summary>
        public void UseRevive()
        {
            if (reviveCount <= 0) return;
            if (gameStatus != GameStatus.LOST) return;
            if (slotData.tiles.Count < 3) return;

            SaveHistoryState();

            // 将槽位前3张移出到置物架 - 与 Android 代码：state.slotTiles.take(3) 一致
            int moveCount = Math.Min(3, slotData.tiles.Count);
            for (int i = 0; i < moveCount; i++)
            {
                Tile tile = slotData.tiles[0];
                tile.state = TileState.MOVED_OUT;  // 标记为已移出
                slotData.RemoveTile(tile);
                movedOutTiles.Add(tile);
            }

            // 重新计算遮挡状态 - 与 Android 代码：calculateBlockedStates(state.boardTiles) 一致
            boardTiles = GameEngine.CalculateBlockedStates(boardTiles);

            reviveCount--;
            gameStatus = GameStatus.PLAYING;
            
            OnGameStateChanged?.Invoke();
            OnBoardChanged?.Invoke();
            OnSlotChanged?.Invoke();
            
            Debug.Log("Revive used. Moved 3 tiles from slot to movedOutTiles. Continue playing.");
        }

        /// <summary>
        /// 保存历史状态（用于撤销）
        /// </summary>
        private void SaveHistoryState()
        {
            GameHistoryState state = new GameHistoryState
            {
                boardTiles = boardTiles.Select(t => t.Copy()).ToList(),
                slotTiles = slotData.tiles.Select(t => t.Copy()).ToList(),
                movedOutTiles = movedOutTiles.Select(t => t.Copy()).ToList()
            };
            historyStack.Push(state);
        }

        /// <summary>
        /// 打乱位置列表
        /// </summary>
        private void ShufflePositions(List<Vector2> positions)
        {
            System.Random random = new System.Random();
            int n = positions.Count;
            while (n > 1)
            {
                n--;
                int k = random.Next(n + 1);
                Vector2 temp = positions[k];
                positions[k] = positions[n];
                positions[n] = temp;
            }
        }

        /// <summary>
        /// 重新开始当前关卡
        /// </summary>
        public void RestartLevel()
        {
            LoadLevel(currentLevelId);
        }

        /// <summary>
        /// 返回主菜单
        /// </summary>
        public void GoBackToMenu()
        {
            gameStatus = GameStatus.MENU;
            OnGameStateChanged?.Invoke();
        }

        /// <summary>
        /// 使用提示道具
        /// 与 Android 原生代码（GameToolDelegate.kt）逻辑保持一致：
        /// 1. 优先从槽位中已有的牌寻找可匹配的牌
        /// 2. 如果槽位中没有可匹配的牌，则寻找棋盘上能凑成3张的组合
        /// 3. 高亮显示的卡牌，3秒后自动消失
        /// </summary>
        public void UseHint()
        {
            if (hintCount <= 0) return;
            if (gameStatus != GameStatus.PLAYING) return;

            List<string> targetIds = new List<string>();

            // 获取活跃卡牌（NORMAL 或 BLOCKED 状态）
            var activeTiles = boardTiles.Where(t => t.state == TileState.NORMAL || t.state == TileState.BLOCKED).ToList();

            if (activeTiles.Count == 0)
            {
                Debug.Log("Hint failed: No active tiles on board.");
                return;
            }

            // 1. 优先从槽位中已有的牌寻找可匹配的牌 - 与 Android 代码逻辑一致
            var slotTypeCount = slotData.tiles.GroupBy(t => t.type).ToDictionary(g => g.Key, g => g.Count());
            
            foreach (var entry in slotTypeCount)
            {
                int type = entry.Key;
                int count = entry.Value;
                int needed = 3 - count;

                if (needed <= 0) continue;

                // 寻找棋盘上该类型的卡牌
                var boardTilesOfType = activeTiles.Where(t => t.type == type).ToList();
                
                if (boardTilesOfType.Count >= needed)
                {
                    // 优先选择 NORMAL 状态的牌
                    var sortedTiles = boardTilesOfType.OrderBy(t => t.state == TileState.NORMAL ? 0 : 1).Take(needed);
                    targetIds = sortedTiles.Select(t => t.id).ToList();
                    break;
                }
            }

            // 2. 如果槽位中没有可匹配的牌，则寻找棋盘上能凑成3张的组合
            if (targetIds.Count == 0)
            {
                var groups = activeTiles.GroupBy(t => t.type).Where(g => g.Count() >= 3);
                
                if (groups.Any())
                {
                    // 选择最佳组合（优先选择 NORMAL 状态的牌多的组合）
                    var bestGroup = groups.OrderByDescending(g => 
                        g.OrderBy(t => t.state == TileState.NORMAL ? 0 : 1).Take(3).Count(t => t.state == TileState.NORMAL)
                    ).First();
                    
                    var sortedGroup = bestGroup.OrderBy(t => t.state == TileState.NORMAL ? 0 : 1).Take(3);
                    targetIds = sortedGroup.Select(t => t.id).ToList();
                }
            }

            // 3. 兜底寻找：任意一组3张的卡牌
            if (targetIds.Count == 0)
            {
                var fallbackGroup = activeTiles.GroupBy(t => t.type).Where(g => g.Count() >= 3).FirstOrDefault();
                if (fallbackGroup != null)
                {
                    targetIds = fallbackGroup.Take(3).Select(t => t.id).ToList();
                }
            }

            if (targetIds.Count == 0)
            {
                Debug.Log("Hint failed: No matchable tiles found.");
                return;
            }

            // 触发高亮事件（由 GameBoard 处理）- 支持多张卡牌高亮
            OnTilesHighlighted?.Invoke(targetIds);
            
            // 3秒后取消高亮 - 与 Android 代码逻辑一致
            MonoBehaviour mb = Instance;
            if (mb != null)
            {
                mb.StartCoroutine(ClearHintAfterDelay(targetIds));
            }

            hintCount--;
            
            Debug.Log($"Hint used. Highlighting {targetIds.Count} tiles.");
        }

        /// <summary>
        /// 延迟清除提示高亮（支持多张卡牌）
        /// </summary>
        private System.Collections.IEnumerator ClearHintAfterDelay(List<string> tileIds)
        {
            yield return new WaitForSeconds(3f);  // 与 Android 代码一致：3秒后消失
            OnTilesHighlighted?.Invoke(new List<string>()); // 空列表表示清除高亮
        }

        /// <summary>
        /// 延迟清除提示高亮
        /// </summary>
        private System.Collections.IEnumerator ClearHintAfterDelay(string tileId)
        {
            yield return new WaitForSeconds(2f);
            OnTileHighlighted?.Invoke(null); // null表示清除高亮
        }

        /// <summary>
        /// 使用炸弹道具
        /// 与 Android 原生代码（GameToolDelegate.kt）逻辑保持一致：
        /// 从槽位移除最后 2 张卡牌
        /// </summary>
        public void UseBomb()
        {
            if (bombCount <= 0) return;
            if (gameStatus != GameStatus.PLAYING) return;
            if (slotData.tiles.Count < 2) return;  // Android 代码检查：state.slotTiles.size < 2

            SaveHistoryState();

            // 移除槽位中的最后 2 张卡牌 - 与 Android 代码：state.slotTiles.dropLast(2) 一致
            int removeCount = Math.Min(2, slotData.tiles.Count);
            for (int i = 0; i < removeCount; i++)
            {
                Tile tile = slotData.tiles[slotData.tiles.Count - 1];
                tile.state = TileState.MOVED_OUT;
                slotData.RemoveTile(tile);
                movedOutTiles.Add(tile);
            }

            bombCount--;
            
            // 播放炸弹音效和震动 - 与 Android 原生代码一致
            if (AudioManager.Instance != null)
            {
                AudioManager.Instance.PlayBombSound();
                AudioManager.Instance.Vibrate();
            }
            
            OnSlotChanged?.Invoke();
            
            // 修复Bug #2：道具使用后触发匹配检测
            ProcessSlotMatchAndCheckEndGame();
            
            Debug.Log("Bomb used. Removed 2 tiles from slot.");
        }

        /// <summary>
        /// 使用万能牌道具
        /// 与 Android 原生代码（GameToolDelegate.kt）逻辑保持一致：
        /// 1. 检查卡槽中是否存在两张相同的卡牌
        /// 2. 从置物架或棋盘中找第3张相同卡牌
        /// 3. 消除这3张卡牌
        /// </summary>
        public void UseJoker()
        {
            if (jokerCount <= 0) return;
            if (gameStatus != GameStatus.PLAYING) return;

            SaveHistoryState();

            // 1. 检查卡槽中是否存在两张相同的卡牌 - 与 Android 代码一致
            var typeGroups = slotData.tiles.GroupBy(t => t.type);
            var targetGroup = typeGroups.FirstOrDefault(g => g.Count() == 2);
            
            if (targetGroup == null)
            {
                Debug.Log("Joker failed: No pair of identical tiles in slot.");
                return;
            }

            int targetType = targetGroup.Key;

            // 2. 从置物架（movedOutTiles）中寻找第3张
            Tile thirdTile = movedOutTiles.FirstOrDefault(t => t.type == targetType);
            bool eliminatedFromOutside = false;

            if (thirdTile != null)
            {
                // 从置物架中移除
                movedOutTiles.Remove(thirdTile);
                eliminatedFromOutside = true;
                Debug.Log($"Joker: Found 3rd tile from movedOutTiles.");
            }
            else
            {
                // 3. 从棋盘中找第3张
                thirdTile = boardTiles.FirstOrDefault(t => 
                    t.type == targetType && 
                    (t.state == TileState.NORMAL || t.state == TileState.BLOCKED));
                
                if (thirdTile != null)
                {
                    // 标记为已消除
                    thirdTile.state = TileState.IN_SLOT;
                    eliminatedFromOutside = true;
                    Debug.Log($"Joker: Found 3rd tile from board.");
                }
            }

            if (!eliminatedFromOutside)
            {
                Debug.Log("Joker failed: Cannot find 3rd tile.");
                return;
            }

            // 4. 将卡槽中该图案的 2 张卡牌移除
            var tilesToRemove = slotData.tiles.Where(t => t.type == targetType).Take(2).ToList();
            foreach (var tile in tilesToRemove)
            {
                slotData.RemoveTile(tile);
            }

            jokerCount--;
            
            // 5. 重新计算遮挡状态
            boardTiles = GameEngine.CalculateBlockedStates(boardTiles);
            
            // 6. 修复Bug #2：道具使用后触发匹配检测
            ProcessSlotMatchAndCheckEndGame();
            
            OnSlotChanged?.Invoke();
            OnBoardChanged?.Invoke();
            
            Debug.Log($"Joker used. Eliminated 3 tiles of type {targetType}.");
        }

        /// <summary>
        /// 使用双倍积分道具
        /// </summary>
        public void UseDoublePoints()
        {
            if (isDoublePointsActive || doublePointsCount <= 0) return;

            isDoublePointsActive = true;
            doublePointsCount--;
            
            Debug.Log("Double points activated!");
        }

        /// <summary>
        /// 卡牌高亮事件
        /// </summary>
        public event System.Action<string> OnTileHighlighted;
    }

    /// <summary>
    /// 游戏历史状态（用于撤销）
    /// </summary>
    [System.Serializable]
    public class GameHistoryState
    {
        public List<UnityGame.Data.Tile> boardTiles;
        public List<UnityGame.Data.Tile> slotTiles;
        public List<UnityGame.Data.Tile> movedOutTiles;
    }
}
