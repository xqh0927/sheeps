using System;
using System.Collections.Generic;
using System.Threading.Tasks;
using UnityEngine;
using UnityEngine.Networking;
using Newtonsoft.Json;
using Newtonsoft.Json.Linq;

namespace UnityGame.Core
{
    /// <summary>
    /// API 管理器
    /// 与 Android 原生代码（GameViewModel.kt）逻辑保持一致：
    /// 1. 使用 UnityWebRequest 进行网络请求
    /// 2. 实现离线模式降级逻辑（网络失败时使用本地生成）
    /// 3. 支持以下 API 接口：
    ///    - GET /api/level/{levelId} - 获取关卡数据
    ///    - POST /api/score - 上传分数
    ///    - GET /api/leaderboard/{levelId} - 获取排行榜
    ///    - POST /api/register - 注册用户
    ///    - POST /api/rename - 修改昵称
    /// </summary>
    public class ApiManager : MonoBehaviour
    {
        private static ApiManager _instance;
        public static ApiManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    GameObject go = new GameObject("ApiManager");
                    _instance = go.AddComponent<ApiManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        [Header("API 配置")]
        [SerializeField] private string baseUrl = "https://api.example.com";  // API 基础 URL
        [SerializeField] private float timeout = 10f;  // 请求超时时间（秒）

        private string userId;
        private string userToken;

        void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);

            // 加载用户 ID 和 Token
            LoadUserInfo();
        }

        #region 公共方法

        /// <summary>
        /// 设置用户 Token
        /// </summary>
        public void SetUserToken(string token)
        {
            userToken = token;
            PlayerPrefs.SetString("UserToken", token);
        }

        /// <summary>
        /// 设置用户 ID
        /// </summary>
        public void SetUserId(string id)
        {
            userId = id;
            PlayerPrefs.SetString("UserId", id);
        }

        /// <summary>
        /// 检查是否已登录
        /// </summary>
        public bool IsLoggedIn()
        {
            return !string.IsNullOrEmpty(userId) && !string.IsNullOrEmpty(userToken);
        }

        #endregion

        #region API 接口方法

        /// <summary>
        /// 获取关卡数据
        /// 与 Android 原生代码（GameViewModel.kt - handleLoadLevel）逻辑保持一致：
        /// 1. 尝试从网络获取关卡数据
        /// 2. 如果网络失败，使用本地生成
        /// </summary>
        public async Task<ApiResponse<List<TileData>>> GetLevel(int levelId)
        {
            string url = $"{baseUrl}/api/level/{levelId}";

            try
            {
                // 尝试从网络获取
                string json = await GetAsync(url);
                var tiles = JsonConvert.DeserializeObject<List<TileData>>(json);
                return new ApiResponse<List<TileData>>(true, tiles, "Success");
            }
            catch (Exception e)
            {
                Debug.LogWarning($"GetLevel failed (using local fallback): {e.Message}");
                // 网络失败，返回 null（调用方会使用本地生成）
                return new ApiResponse<List<TileData>>(false, null, e.Message);
            }
        }

        /// <summary>
        /// 上传分数
        /// </summary>
        public async Task<ApiResponse<bool>> SubmitScore(int levelId, int score, int timeUsed)
        {
            string url = $"{baseUrl}/api/score";

            var data = new
            {
                userId = this.userId,
                levelId = levelId,
                score = score,
                timeUsed = timeUsed,
                timestamp = DateTimeOffset.UtcNow.ToUnixTimeSeconds()
            };

            try
            {
                string json = JsonConvert.SerializeObject(data);
                string response = await PostAsync(url, json);
                return new ApiResponse<bool>(true, true, "Score submitted");
            }
            catch (Exception e)
            {
                Debug.LogWarning($"SubmitScore failed: {e.Message}");
                return new ApiResponse<bool>(false, false, e.Message);
            }
        }

        /// <summary>
        /// 获取排行榜
        /// 与 Android 原生代码（GameViewModel.kt - handleLoadLeaderboard）逻辑保持一致：
        /// 1. 尝试从网络获取排行榜
        /// 2. 如果网络失败，返回空列表
        /// </summary>
        public async Task<ApiResponse<List<LeaderboardEntry>>> GetLeaderboard(int levelId)
        {
            string url = $"{baseUrl}/api/leaderboard/{levelId}";

            try
            {
                string json = await GetAsync(url);
                var rankings = JsonConvert.DeserializeObject<List<LeaderboardEntry>>(json);
                return new ApiResponse<List<LeaderboardEntry>>(true, rankings, "Success");
            }
            catch (Exception e)
            {
                Debug.LogWarning($"GetLeaderboard failed: {e.Message}");
                return new ApiResponse<List<LeaderboardEntry>>(false, new List<LeaderboardEntry>(), e.Message);
            }
        }

        /// <summary>
        /// 注册用户
        /// </summary>
        public async Task<ApiResponse<RegisterResponse>> Register(string nickname)
        {
            string url = $"{baseUrl}/api/register";

            var data = new
            {
                nickname = nickname,
                deviceId = SystemInfo.deviceUniqueIdentifier
            };

            try
            {
                string json = JsonConvert.SerializeObject(data);
                string response = await PostAsync(url, json);
                var result = JsonConvert.DeserializeObject<RegisterResponse>(response);

                // 保存用户信息
                SetUserId(result.userId);
                SetUserToken(result.token);

                return new ApiResponse<RegisterResponse>(true, result, "Registration successful");
            }
            catch (Exception e)
            {
                Debug.LogWarning($"Register failed: {e.Message}");
                return new ApiResponse<RegisterResponse>(false, null, e.Message);
            }
        }

        /// <summary>
        /// 修改昵称
        /// </summary>
        public async Task<ApiResponse<bool>> Rename(string newNickname)
        {
            if (!IsLoggedIn())
            {
                return new ApiResponse<bool>(false, false, "Not logged in");
            }

            string url = $"{baseUrl}/api/rename";

            var data = new
            {
                userId = this.userId,
                newNickname = newNickname
            };

            try
            {
                string json = JsonConvert.SerializeObject(data);
                string response = await PostAsync(url, json, userToken);
                return new ApiResponse<bool>(true, true, "Rename successful");
            }
            catch (Exception e)
            {
                Debug.LogWarning($"Rename failed: {e.Message}");
                return new ApiResponse<bool>(false, false, e.Message);
            }
        }

        #endregion

        #region HTTP 请求方法

        /// <summary>
        /// 发送 GET 请求
        /// 使用 TaskCompletionSource 实现异步等待（兼容团结引擎）
        /// </summary>
        private Task<string> GetAsync(string url)
        {
            var tcs = new TaskCompletionSource<string>();

            UnityWebRequest request = UnityWebRequest.Get(url);
            request.timeout = (int)timeout;

            if (!string.IsNullOrEmpty(userToken))
            {
                request.SetRequestHeader("Authorization", $"Bearer {userToken}");
            }

            var operation = request.SendWebRequest();
            operation.completed += (op) =>
            {
                try
                {
                    if (request.result != UnityWebRequest.Result.Success)
                    {
                        tcs.SetException(new Exception($"GET {url} failed: {request.error}"));
                    }
                    else
                    {
                        tcs.SetResult(request.downloadHandler.text);
                    }
                }
                catch (Exception e)
                {
                    tcs.SetException(e);
                }
                finally
                {
                    request.Dispose();
                }
            };

            return tcs.Task;
        }

        /// <summary>
        /// 发送 POST 请求
        /// 使用 TaskCompletionSource 实现异步等待（兼容团结引擎）
        /// </summary>
        private Task<string> PostAsync(string url, string json, string token = null)
        {
            var tcs = new TaskCompletionSource<string>();

            UnityWebRequest request = new UnityWebRequest(url, "POST");
            byte[] bodyRaw = System.Text.Encoding.UTF8.GetBytes(json);
            request.uploadHandler = new UploadHandlerRaw(bodyRaw);
            request.downloadHandler = new DownloadHandlerBuffer();
            request.SetRequestHeader("Content-Type", "application/json");
            request.timeout = (int)timeout;

            string authToken = token ?? userToken;
            if (!string.IsNullOrEmpty(authToken))
            {
                request.SetRequestHeader("Authorization", $"Bearer {authToken}");
            }

            var operation = request.SendWebRequest();
            operation.completed += (op) =>
            {
                try
                {
                    if (request.result != UnityWebRequest.Result.Success)
                    {
                        tcs.SetException(new Exception($"POST {url} failed: {request.error}"));
                    }
                    else
                    {
                        tcs.SetResult(request.downloadHandler.text);
                    }
                }
                catch (Exception e)
                {
                    tcs.SetException(e);
                }
                finally
                {
                    request.Dispose();
                }
            };

            return tcs.Task;
        }

        #endregion

        #region 辅助方法

        /// <summary>
        /// 加载用户信息
        /// </summary>
        private void LoadUserInfo()
        {
            userId = PlayerPrefs.GetString("UserId", "");
            userToken = PlayerPrefs.GetString("UserToken", "");
        }

        #endregion
    }

    /// <summary>
    /// API 响应包装类
    /// </summary>
    public class ApiResponse<T>
    {
        public bool success;
        public T data;
        public string message;

        public ApiResponse(bool success, T data, string message)
        {
            this.success = success;
            this.data = data;
            this.message = message;
        }
    }

    /// <summary>
    /// 卡牌数据（用于 API 传输）
    /// </summary>
    public class TileData
    {
        public string id { get; set; }
        public int type { get; set; }
        public float x { get; set; }
        public float y { get; set; }
        public int z { get; set; }
        public int state { get; set; }
        public int sealedCount { get; set; }
        public bool isBlind { get; set; }
    }

    /// <summary>
    /// 排行榜条目
    /// </summary>
    public class LeaderboardEntry
    {
        public string userId { get; set; }
        public string nickname { get; set; }
        public int score { get; set; }
        public int timeUsed { get; set; }
        public long timestamp { get; set; }
    }

    /// <summary>
    /// 注册响应
    /// </summary>
    public class RegisterResponse
    {
        public string userId { get; set; }
        public string token { get; set; }
        public string nickname { get; set; }
    }
}
