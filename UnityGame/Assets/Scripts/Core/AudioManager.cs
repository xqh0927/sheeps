using UnityEngine;

namespace UnityGame.Core
{
    /// <summary>
    /// 音效管理器
    /// 与 Android 原生代码（GameViewEffect.kt）逻辑保持一致：
    /// 1. 管理所有音效（Click, Match, Win, Lose, Unseal）
    /// 2. 在相应位置播放音效
    /// 3. 实现震动反馈（Handheld.Vibrate()）
    /// </summary>
    public class AudioManager : MonoBehaviour
    {
        private static AudioManager _instance;
        public static AudioManager Instance
        {
            get
            {
                if (_instance == null)
                {
                    GameObject go = new GameObject("AudioManager");
                    _instance = go.AddComponent<AudioManager>();
                    DontDestroyOnLoad(go);
                }
                return _instance;
            }
        }

        [Header("音效 clips")]
        [SerializeField] private AudioClip clickSound;      // 点击音效
        [SerializeField] private AudioClip matchSound;      // 匹配音效
        [SerializeField] private AudioClip winSound;        // 胜利音效
        [SerializeField] private AudioClip loseSound;       // 失败音效
        [SerializeField] private AudioClip unsealSound;     // 解封音效
        [SerializeField] private AudioClip shuffleSound;    // 洗牌音效
        [SerializeField] private AudioClip bombSound;       // 炸弹音效

        [Header("音频设置")]
        [SerializeField] private float soundVolume = 1.0f;    // 音效音量
        [SerializeField] private float musicVolume = 1.0f;    // 音乐音量
        [SerializeField] private bool vibrateEnabled = true;    // 震动开关

        private AudioSource soundAudioSource;
        private AudioSource musicAudioSource;

        void Awake()
        {
            if (_instance != null && _instance != this)
            {
                Destroy(gameObject);
                return;
            }
            _instance = this;
            DontDestroyOnLoad(gameObject);

            // 创建音频源
            soundAudioSource = gameObject.AddComponent<AudioSource>();
            soundAudioSource.volume = soundVolume;
            soundAudioSource.playOnAwake = false;

            musicAudioSource = gameObject.AddComponent<AudioSource>();
            musicAudioSource.volume = musicVolume;
            musicAudioSource.playOnAwake = false;
            musicAudioSource.loop = true;  // 背景音乐循环播放

            // 加载设置
            LoadSettings();
        }

        #region 公共方法

        /// <summary>
        /// 播放点击音效
        /// 与 Android 原生代码：GameViewEffect.PlaySound(SoundType.CLICK) 一致
        /// </summary>
        public void PlayClickSound()
        {
            PlaySound(clickSound);
        }

        /// <summary>
        /// 播放匹配音效
        /// 与 Android 原生代码：GameViewEffect.PlaySound(SoundType.MATCH) 一致
        /// </summary>
        public void PlayMatchSound()
        {
            PlaySound(matchSound);
        }

        /// <summary>
        /// 播放胜利音效
        /// 与 Android 原生代码：GameViewEffect.PlaySound(SoundType.WIN) 一致
        /// </summary>
        public void PlayWinSound()
        {
            PlaySound(winSound);
        }

        /// <summary>
        /// 播放失败音效
        /// 与 Android 原生代码：GameViewEffect.PlaySound(SoundType.LOSE) 一致
        /// </summary>
        public void PlayLoseSound()
        {
            PlaySound(loseSound);
        }

        /// <summary>
        /// 播放解封音效
        /// 与 Android 原生代码：GameViewEffect.PlaySound(SoundType.UNSEAL) 一致
        /// </summary>
        public void PlayUnsealSound()
        {
            PlaySound(unsealSound);
        }

        /// <summary>
        /// 播放洗牌音效
        /// </summary>
        public void PlayShuffleSound()
        {
            PlaySound(shuffleSound);
        }

        /// <summary>
        /// 播放炸弹音效
        /// </summary>
        public void PlayBombSound()
        {
            PlaySound(bombSound);
        }

        /// <summary>
        /// 触发震动反馈
        /// 与 Android 原生代码：GameViewEffect.Vibrate 一致
        /// </summary>
        public void Vibrate()
        {
            if (vibrateEnabled)
            {
                Handheld.Vibrate();
            }
        }

        /// <summary>
        /// 设置音效音量
        /// </summary>
        public void SetSoundVolume(float volume)
        {
            soundVolume = Mathf.Clamp01(volume);
            soundAudioSource.volume = soundVolume;
            PlayerPrefs.SetFloat("SoundVolume", soundVolume);
        }

        /// <summary>
        /// 设置音乐音量
        /// </summary>
        public void SetMusicVolume(float volume)
        {
            musicVolume = Mathf.Clamp01(volume);
            musicAudioSource.volume = musicVolume;
            PlayerPrefs.SetFloat("MusicVolume", musicVolume);
        }

        /// <summary>
        /// 设置震动开关
        /// </summary>
        public void SetVibrateEnabled(bool enabled)
        {
            vibrateEnabled = enabled;
            PlayerPrefs.SetInt("VibrateEnabled", enabled ? 1 : 0);
        }

        /// <summary>
        /// 获取音效音量
        /// </summary>
        public float GetSoundVolume()
        {
            return soundVolume;
        }

        /// <summary>
        /// 获取音乐音量
        /// </summary>
        public float GetMusicVolume()
        {
            return musicVolume;
        }

        /// <summary>
        /// 获取震动开关状态
        /// </summary>
        public bool IsVibrateEnabled()
        {
            return vibrateEnabled;
        }

        #endregion

        #region 私有方法

        /// <summary>
        /// 播放音效
        /// </summary>
        private void PlaySound(AudioClip clip)
        {
            if (clip != null && soundAudioSource != null)
            {
                soundAudioSource.PlayOneShot(clip, soundVolume);
            }
        }

        /// <summary>
        /// 加载设置
        /// </summary>
        private void LoadSettings()
        {
            soundVolume = PlayerPrefs.GetFloat("SoundVolume", 1.0f);
            musicVolume = PlayerPrefs.GetFloat("MusicVolume", 1.0f);
            vibrateEnabled = PlayerPrefs.GetInt("VibrateEnabled", 1) == 1;

            soundAudioSource.volume = soundVolume;
            musicAudioSource.volume = musicVolume;
        }

        #endregion
    }

    /// <summary>
    /// 音效类型枚举
    /// 与 Android 原生代码（SoundType.kt）保持一致
    /// </summary>
    public enum SoundType
    {
        CLICK,      // 点击
        MATCH,      // 匹配
        WIN,        // 胜利
        LOSE,       // 失败
        UNSEAL      // 解封
    }
}
