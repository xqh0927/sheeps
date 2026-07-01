@echo off
chcp 65001 >nul
REM ==========================================
REM Unity Android APK 自动打包脚本
REM 使用方法：双击运行此脚本
REM ==========================================

echo.
echo ========== Unity Android APK 打包工具 ==========
echo.

REM 设置Unity项目路径
set UNITY_PROJECT_PATH=E:\file\sheeps\UnityGame

REM 设置输出APK路径
set OUTPUT_PATH=%UNITY_PROJECT_PATH%\Builds\sheeps_v1.0.0.apk

REM 设置Unity编辑器路径（需要根据实际安装位置修改）
REM 尝试常见的Unity安装路径
set UNITY_PATH=

REM 检查Unity Hub默认安装路径
if exist "%LOCALAPPDATA%\Programs\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe" (
    set UNITY_PATH=%LOCALAPPDATA%\Programs\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe
    goto :found_unity
)

if exist "%PROGRAMFILES%\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe" (
    set UNITY_PATH=%PROGRAMFILES%\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe
    goto :found_unity
)

if exist "C:\Program Files\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe" (
    set UNITY_PATH=C:\Program Files\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe
    goto :found_unity
)

REM 如果没找到，询问用户
echo [错误] 未找到Unity编辑器！
echo.
echo 请手动指定Unity.exe路径，例如：
echo   C:\Program Files\Unity\Hub\Editor\2021.3.45f1\Editor\Unity.exe
echo.
set /p UNITY_PATH=请输入Unity.exe完整路径: 

if not exist "%UNITY_PATH%" (
    echo [错误] 指定的路径不存在！
    pause
    exit /b 1
)

:found_unity
echo [成功] 找到Unity编辑器：
echo   %UNITY_PATH%
echo.

REM 检查项目路径
if not exist "%UNITY_PROJECT_PATH%" (
    echo [错误] Unity项目路径不存在：%UNITY_PROJECT_PATH%
    pause
    exit /b 1
)

echo [信息] Unity项目路径：%UNITY_PROJECT_PATH%
echo [信息] 输出APK路径：%OUTPUT_PATH%
echo.

REM 创建输出目录
if not exist "%UNITY_PROJECT_PATH%\Builds" (
    mkdir "%UNITY_PROJECT_PATH%\Builds"
)

echo ========== 开始打包APK ==========
echo.
echo 提示：首次打包可能需要下载Android SDK组件，请耐心等待...
echo.

REM 执行Unity命令行打包
"%UNITY_PATH%" ^
    -quit ^
    -batchmode ^
    -projectPath "%UNITY_PROJECT_PATH%" ^
    -executeMethod BuildScript.BuildAndroid ^
    -logFile "%UNITY_PROJECT_PATH%\build_log.txt" ^
    -acceptedAPIterms ^
    -acceptSourceQA

REM 检查打包结果
if %ERRORLEVEL% EQU 0 (
    echo.
    echo ========== 打包成功！ ==========
    echo.
    echo APK文件位置：
    echo   %OUTPUT_PATH%
    echo.
    echo 文件大小：
    dir "%OUTPUT_PATH%" | find ".apk"
    echo.
    echo 是否要打开输出文件夹？
    set /p OPEN_FOLDER=按Y打开，按任意键继续: 
    if /i "%OPEN_FOLDER%"=="Y" (
        start "" "%UNITY_PROJECT_PATH%\Builds"
    )
) else (
    echo.
    echo ========== 打包失败！ ==========
    echo.
    echo 请查看日志文件：
    echo   %UNITY_PROJECT_PATH%\build_log.txt
    echo.
    echo 常见错误：
    echo   1. 未安装Android Build Support模块
    echo   2. Android SDK/NDK路径未配置
    echo   3. Package Name冲突
    echo   4. 场景文件未保存
    echo.
    echo 是否要打开日志文件？
    set /p OPEN_LOG=按Y打开，按任意键继续: 
    if /i "%OPEN_LOG%"=="Y" (
        start "" "%UNITY_PROJECT_PATH%\build_log.txt"
    )
)

echo.
pause
exit /b %ERRORLEVEL%
