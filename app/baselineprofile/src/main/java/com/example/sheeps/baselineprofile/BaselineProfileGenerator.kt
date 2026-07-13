package com.example.sheeps.baselineprofile

import androidx.benchmark.macro.ExperimentalBaselineProfilesApi
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Baseline Profile 生成器。
 *
 * 运行 `./gradlew :baselineprofile:generateBaselineProfile`（需连接设备/模拟器）后，
 * 该测试会启动目标 App（默认进入 SplashActivity → Menu），在关键交互路径上收集稳定的
 * Baseline Profile，并在 `:app:assembleRelease` 时自动合并进 release APK，
 * 从而加速启动与关键场景的运行速度。
 *
 * 注意：
 *  - 本文件只是「生成脚本」，不参与业务运行，不改动任何游戏/菜单业务代码。
 *  - 其中的 UI 交互（滚动、进入游戏）为示例占位。团队可按实际菜单/游戏界面的
 *    resourceId / 文本微调 selector，使 profile 覆盖更精准的交互路径。
 *  - 必须连接设备/模拟器执行；CI 中通常由 benchmark 流水线完成。
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val baselineProfileRule = MacrobenchmarkRule()

    @Test
    @OptIn(ExperimentalBaselineProfilesApi::class)
    fun startup() = baselineProfileRule.measureRepeated(
        packageName = "com.example.sheeps",
        metrics = listOf(),
        iterations = 3,
        setupBlock = {
            // 冷启动目标 App（进入 SplashActivity → Menu）
            startActivityAndWait()
        }
    ) {
        // 收集稳定的 Baseline Profile，覆盖启动后的关键代码路径
        collectStableBaselineProfile()

        // 示例交互：等待首屏稳定后向上滚动，覆盖菜单/列表滚动相关代码路径。
        // 此处使用通用滑动，不依赖具体业务控件；如需更精准覆盖，可按实际界面
        // 的 resourceId / 文本调整 selector（例如 clickable { text = "开始游戏" }）。
        device.waitForIdle()
        val size = device.displaySizeDp
        device.swipe(
            size.x / 2,
            (size.y * 0.8f).toInt(),
            size.x / 2,
            (size.y * 0.2f).toInt(),
            10
        )
    }
}
