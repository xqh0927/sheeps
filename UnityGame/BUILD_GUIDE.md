# Unity 项目 Android APK 打包指南

## 系统要求

### 必需软件
1. **Unity 2021.3 LTS 或更高版本**
2. **JDK (Java Development Kit) 11 或更高版本**
3. **Android SDK (Command-line Tools)**
4. **Android NDK** (Unity Hub自动安装)

### Unity模块
在Unity Hub中，确保已安装以下模块：
- ✅ Android Build Support
- ✅ Android SDK & NDK Tools
- ✅ OpenJDK

---

## 方法一：通过Unity编辑器手动打包（推荐新手）

### 步骤1：打开项目
1. 打开Unity Hub
2. 选择 `Add` → 选择 `E:\file\sheeps\UnityGame` 文件夹
3. 打开项目，等待编译完成

### 步骤2：切换平台到Android
1. 点击菜单栏 `File` → `Build Settings...`
2. 在Platform列表中选择 `Android`
3. 点击 `Switch Platform` 按钮
4. 等待平台切换完成（可能需要几分钟）

### 步骤3：配置Player Settings
1. 在Build Settings窗口中，点击 `Player Settings...` 按钮
2. 配置以下选项：

#### Player → Other Settings
- **Identification**
  - **Package Name**: `com.example.sheeps` （必须是唯一的）
  - **Version**: `1.0.0`
  - **Bundle Version Code**: `1`

- **Configuration**
  - **Scripting Backend**: `IL2CPP` （推荐，性能更好）
  - **Target Architectures**: 
    - ✅ ARMv7
    - ✅ ARM64 （必须，Google Play要求）
    - ❌ x86 (取消)

- **Rendering**
  - **Auto Graphics API**: ✅ 勾选
  - **Color Space**: `Linear` （推荐）

- **Other**
  - **Api Compatibility Level**: `.NET Standard 2.0`
  - **Minimum API Level**: `Android 5.1 (API level 22)` （推荐）
  - **Target API Level**: `Automatic (highest installed)`

#### Player → Publishing Settings
- **Keystore**
  - 如果你是首次发布，点击 `Create a new keystore`
  - 设置密码并保存keystore文件（非常重要！备份好！）
  - 选择keystore文件并输入密码
  - 选择key alias并输入密码

#### Player → Resolution and Presentation
- **Default Orientation**: `Portrait` （竖屏游戏）
- **Allowed Orientations for Auto Rotation**:
  - ✅ Portrait
  - ❌ Portrait Upside Down
  - ❌ Landscape Left
  - ❌ Landscape Right

### 步骤4：构建APK
1. 回到 `Build Settings` 窗口
2. 确保场景已添加：
   - 点击 `Add Open Scenes`
   - 确保 `Scenes/GameScene` 已勾选
3. 选择输出位置：
   - 点击 `Build` 按钮
   - 选择输出文件夹（建议：`E:\file\sheeps\UnityGame\Builds\`）
   - 文件名：`sheeps_v1.0.0.apk`
4. 等待构建完成（可能需要5-15分钟）

---

## 方法二：使用自动化脚本打包（推荐CI/CD）

### 使用方法
1. 将 `BuildScript.cs` 放在 `Assets/Editor/` 文件夹中
2. 在Unity编辑器中，点击菜单栏 `Tools` → `UnityGame` → `Build Android APK`
3. 选择输出文件夹，脚本会自动构建APK

### 命令行打包（CI/CD）
```bash
# Windows
"C:\Program Files\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe" \
  -quit \
  -batchmode \
  -projectPath "E:\file\sheeps\UnityGame" \
  -executeMethod BuildScript.BuildAndroid \
  -logFile "build.log"

# 或者使用Unity Hub的Unity路径
```

---

## 常见问题解决

### 问题1：找不到Android SDK/NDK/JDK
**解决方法**：
1. 在Unity编辑器中，点击 `Edit` → `Preferences...` → `External Tools`
2. 确保以下路径正确：
   - **JDK**: `C:\Program Files\Unity\Hub\Editor\2021.3.45f1\Editor\Data\PlaybackEngines\AndroidPlayer\OpenJDK`
   - **Android SDK**: `C:\Program Files\Unity\Hub\Editor\2021.3.45f1\Editor\Data\PlaybackEngines\AndroidPlayer\SDK`
   - **Android NDK**: `C:\Program Files\Unity\Hub\Editor\2021.3.45f1\Editor\Data\PlaybackEngines\AndroidPlayer\NDK`
3. 如果路径不正确，取消勾选 `Use embedded JDK/SDK/NDK`，手动指定路径

### 问题2：构建失败，提示Gradle错误
**解决方法**：
1. 在 `Player Settings` → `Other Settings` → `Configuration` 中
2. 将 `Scripting Backend` 改为 `Mono`
3. 或者更新Gradle版本（高级用户）

### 问题3：APK文件太大
**优化方法**：
1. 在 `Player Settings` → `Other Settings` 中：
   - **Scripting Backend**: `IL2CPP`
   - **ARM64**: ✅ 勾选
   - **ARMv7**: ✅ 勾选
2. 在 `Build Settings` 中：
   - **Development Build**: ❌ 取消勾选
   - **Autoconnect Profiler**: ❌ 取消勾选
3. 使用纹理压缩：
   - `Edit` → `Project Settings` → `Player` → `Android` → `Publishing Settings`
   - **Texture Compression**: `ETC2 (GLES 3.0)`

### 问题4：打包后游戏无法启动
**解决方法**：
1. 检查 `AndroidManifest.xml` 权限配置
2. 确保最低API级别不低于Android 5.1
3. 检查Logcat日志：
   ```bash
   adb logcat -s Unity
   ```

---

## 构建输出说明

### 成功构建后
- **APK文件位置**: `E:\file\sheeps\UnityGame\Builds\sheeps_v1.0.0.apk`
- **文件大小**: 约30-80MB（取决于资源）

### 测试APK
1. 将APK文件传输到Android手机
2. 在手机上启用"未知来源"安装
3. 安装并测试

### 上传到Google Play
1. 注册Google Play开发者账号（$25一次性费用）
2. 创建应用并上传APK/AAB
3. 填写应用信息、截图、隐私政策等
4. 提交审核

---

## 高级配置

### 使用AAB格式（Google Play推荐）
在 `Build Settings` 中：
1. 勾选 `Export Project`
2. 或者使用 `Build App Bundle (.aab)` 选项

### 混淆和资源压缩
在 `Player Settings` → `Publishing Settings` 中：
- ✅ `Release`
- ✅ `Minify` → `Proguard`

### 多语言支持
在 `Player Settings` → `Other Settings` → `Configuration` 中：
- 添加支持的语言：`zh-CN`, `en`, `ja`, `ko`

---

## 快速检查清单

构建APK前，确保：
- [ ] Unity已安装Android Build Support模块
- [ ] 已切换平台到Android
- [ ] Package Name已修改为唯一标识
- [ ] Keystore已配置（发布版本）
- [ ] 场景已添加到Build Settings
- [ ] Development Build已取消勾选
- [ ] 最低API级别设置正确
- [ ] 目标架构包含ARM64

---

## 下一步

构建成功后：
1. **测试**: 在真机上测试游戏功能
2. **优化**: 根据性能分析结果优化
3. **签名**: 使用正确的Keystore签名（发布版本）
4. **发布**: 上传到Google Play或其他应用商店

---

**注意**: 首次构建可能需要下载Android SDK组件，确保网络畅通。

如有问题，请查看Unity Console窗口的错误信息，或参考Unity官方文档：
https://docs.unity3d.com/Manual/android-BuildProcess.html
