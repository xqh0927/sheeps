using UnityEngine;
using UnityEditor;
using UnityEditor.Build.Reporting;
using System.IO;

namespace UnityGame.Editor
{
    /// <summary>
    /// Android APK 自动打包脚本
    /// 使用方法：
    /// 1. 在Unity编辑器中：Tools → UnityGame → Build Android APK
    /// 2. 命令行：Unity.exe -quit -batchmode -projectPath [项目路径] -executeMethod BuildScript.BuildAndroid
    /// </summary>
    public class BuildScript : EditorWindow
    {
        private static string outputPath = "";
        private static bool useKeystore = false;

        #region Unity Editor Menu Items

        /// <summary>
        /// 在Unity编辑器中显示打包窗口
        /// </summary>
        [MenuItem("Tools/UnityGame/Build Android APK")]
        public static void ShowBuildWindow()
        {
            BuildScript window = GetWindow<BuildScript>("Build Android APK");
            window.minSize = new Vector2(500, 300);
        }

        /// <summary>
        /// 快速打包（使用默认设置）
        /// </summary>
        [MenuItem("Tools/UnityGame/Quick Build Android APK")]
        public static void QuickBuild()
        {
            string defaultPath = Path.Combine(Application.dataPath, "../Builds");
            if (!Directory.Exists(defaultPath))
            {
                Directory.CreateDirectory(defaultPath);
            }

            string outputAPK = Path.Combine(defaultPath, $"sheeps_v1.0.{System.DateTime.Now.ToString("MMdd")}.apk");
            BuildAndroidAPK(outputAPK);
        }

        #endregion

        #region Build Process

        /// <summary>
        /// 命令行调用的打包方法
        /// </summary>
        public static void BuildAndroid()
        {
            string defaultPath = Path.Combine(Application.dataPath, "../Builds");
            if (!Directory.Exists(defaultPath))
            {
                Directory.CreateDirectory(defaultPath);
            }

            string outputAPK = Path.Combine(defaultPath, $"sheeps_v1.0.{PlayerSettings.Android.bundleVersionCode}.apk");
            BuildAndroidAPK(outputAPK);
        }

        /// <summary>
        /// 核心打包方法
        /// </summary>
        private static void BuildAndroidAPK(string outputPath)
        {
            Debug.Log("========== Starting Android APK Build ==========");

            // 步骤1：检查并切换平台
            if (EditorUserBuildSettings.activeBuildTarget != BuildTarget.Android)
            {
                Debug.Log("Switching platform to Android...");
                EditorUserBuildSettings.SwitchActiveBuildTarget(BuildTargetGroup.Android, BuildTarget.Android);
            }

            // 步骤2：配置Player Settings
            ConfigurePlayerSettings();

            // 步骤3：配置Android设置
            ConfigureAndroidSettings();

            // 步骤4：获取场景列表
            string[] scenes = GetScenePaths();
            if (scenes.Length == 0)
            {
                Debug.LogError("No scenes found! Please add at least one scene to the build.");
                return;
            }

            Debug.Log($"Building with {scenes.Length} scene(s):");
            foreach (string scene in scenes)
            {
                Debug.Log($"  - {scene}");
            }

            // 步骤5：执行打包
            BuildPlayerOptions buildOptions = new BuildPlayerOptions();
            buildOptions.scenes = scenes;
            buildOptions.locationPathName = outputPath;
            buildOptions.target = BuildTarget.Android;
            buildOptions.options = BuildOptions.None;

            // 如果是命令行模式，使用非开发构建
            if (Application.isBatchMode)
            {
                buildOptions.options |= BuildOptions.Il2CPP;
            }

            Debug.Log($"Output APK: {outputPath}");
            Debug.Log("Building... (this may take several minutes)");

            BuildReport report = BuildPipeline.BuildPlayer(buildOptions);
            BuildSummary summary = report.summary;

            // 步骤6：输出结果
            if (summary.result == BuildResult.Succeeded)
            {
                Debug.Log("========== Build Succeeded! ==========");
                Debug.Log($"APK Path: {outputPath}");
                Debug.Log($"Total Size: {summary.totalSize / 1024.0f / 1024.0f:F2} MB");
                Debug.Log($"Build Time: {summary.totalTime.TotalSeconds:F2} seconds");

                // 显示APK文件位置
                EditorUtility.RevealInFinder(outputPath);
            }
            else if (summary.result == BuildResult.Failed)
            {
                Debug.LogError("========== Build Failed! ==========");
                foreach (BuildStep step in report.steps)
                {
                    if (step.result == BuildStepResult.Failed)
                    {
                        Debug.LogError($"Failed Step: {step.name}");
                        Debug.LogError($"Error: {step.messages}");
                    }
                }
            }
        }

        #endregion

        #region Configuration

        /// <summary>
        /// 配置Player Settings
        /// </summary>
        private static void ConfigurePlayerSettings()
        {
            Debug.Log("Configuring Player Settings...");

            // 基本信息
            PlayerSettings.applicationIdentifier = "com.example.sheeps";
            PlayerSettings.bundleVersion = "1.0.0";
            PlayerSettings.Android.bundleVersionCode = 1;

            // 分辨率设置
            PlayerSettings.defaultInterfaceOrientation = UIOrientation.Portrait;
            PlayerSettings.allowedAutorotateToPortrait = true;
            PlayerSettings.allowedAutorotateToPortraitUpsideDown = false;
            PlayerSettings.allowedAutorotateToLandscapeLeft = false;
            PlayerSettings.allowedAutorotateToLandscapeRight = false;

            // 渲染设置
            PlayerSettings.colorSpace = ColorSpace.Gamma; // 对于2D游戏，Gamma就够了
            PlayerSettings.SetUseDefaultGraphicsAPIs(BuildTarget.Android, true);

            // 脚本设置
            PlayerSettings.SetApiCompatibilityLevel(BuildTargetGroup.Android, ApiCompatibilityLevel.NET_Standard_2_0);

            Debug.Log("Player Settings configured.");
        }

        /// <summary>
        /// 配置Android特定设置
        /// </summary>
        private static void ConfigureAndroidSettings()
        {
            Debug.Log("Configuring Android Settings...");

            // 目标架构
            PlayerSettings.Android.targetArchitectures = AndroidArchitecture.ARMv7 | AndroidArchitecture.ARM64;

            // API级别
            PlayerSettings.Android.minSdkVersion = AndroidSdkVersions.AndroidApiLevel22; // Android 5.1
            PlayerSettings.Android.targetSdkVersion = AndroidSdkVersions.AndroidApiLevelAuto;

            // 脚本后端
            EditorUserBuildSettings.androidBuildSystem = AndroidBuildSystem.Gradle;
            PlayerSettings.SetScriptingBackend(BuildTargetGroup.Android, ScriptingImplementation.IL2CPP);

            // 混淆和优化（发布版本）
            if (!EditorUserBuildSettings.developmentBuild)
            {
                EditorUserBuildSettings.androidBuildType = AndroidBuildType.Release;
            }

            Debug.Log("Android Settings configured.");
            Debug.Log($"  Target Architectures: {PlayerSettings.Android.targetArchitectures}");
            Debug.Log($"  Min SDK: {PlayerSettings.Android.minSdkVersion}");
            Debug.Log($"  Scripting Backend: {PlayerSettings.GetScriptingBackend(BuildTargetGroup.Android)}");
        }

        #endregion

        #region Helper Methods

        /// <summary>
        /// 获取所有场景路径
        /// </summary>
        private static string[] GetScenePaths()
        {
            List<string> scenes = new List<string>();
            foreach (EditorBuildSettingsScene scene in EditorBuildSettings.scenes)
            {
                if (scene.enabled)
                {
                    scenes.Add(scene.path);
                }
            }

            // 如果没有配置场景，尝试查找场景文件
            if (scenes.Count == 0)
            {
                string[] sceneGuids = AssetDatabase.FindAssets("t:Scene");
                foreach (string guid in sceneGuids)
                {
                    string path = AssetDatabase.GUIDToAssetPath(guid);
                    scenes.Add(path);
                }
            }

            return scenes.ToArray();
        }

        #endregion

        #region Editor Window GUI

        private string outputAPKPath = "";
        private bool developmentBuild = false;

        void OnGUI()
        {
            GUILayout.Label("Android APK Build Settings", EditorStyles.boldLabel);

            EditorGUILayout.Space(10);

            // 输出路径
            GUILayout.Label("Output APK Path:");
            EditorGUILayout.BeginHorizontal();
            outputAPKPath = EditorGUILayout.TextField(outputAPKPath);
            if (GUILayout.Button("Browse", GUILayout.Width(80)))
            {
                string path = EditorUtility.SaveFilePanel("Save APK", 
                    Application.dataPath + "/../Builds", 
                    $"sheeps_v1.0.apk", 
                    "apk");
                if (!string.IsNullOrEmpty(path))
                {
                    outputAPKPath = path;
                }
            }
            EditorGUILayout.EndHorizontal();

            EditorGUILayout.Space(10);

            // 开发构建选项
            developmentBuild = EditorGUILayout.Toggle("Development Build", developmentBuild);

            EditorGUILayout.Space(20);

            // 打包按钮
            if (GUILayout.Button("Build APK", GUILayout.Height(40)))
            {
                if (string.IsNullOrEmpty(outputAPKPath))
                {
                    string defaultPath = Path.Combine(Application.dataPath, "../Builds");
                    if (!Directory.Exists(defaultPath))
                    {
                        Directory.CreateDirectory(defaultPath);
                    }
                    outputAPKPath = Path.Combine(defaultPath, "sheeps_v1.0.apk");
                }

                // 设置开发构建
                EditorUserBuildSettings.developmentBuild = developmentBuild;

                BuildAndroidAPK(outputAPKPath);
            }

            EditorGUILayout.Space(10);

            // 快速打包按钮
            if (GUILayout.Button("Quick Build (Default Settings)"))
            {
                QuickBuild();
            }
        }

        #endregion
    }
}
