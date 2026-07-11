package com.example.sheeps.splash

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.lifecycleScope
import com.apkfuns.logutils.LogUtils
import com.example.sheeps.core.AppConfig
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.theme.ShapeLarge
import com.example.sheeps.theme.SheepsTheme
import com.example.sheeps.ui.components.GhostButton
import com.example.sheeps.ui.components.PrimaryButton
import com.example.sheeps.ui.components.SheepsLoading
import com.hjq.permissions.OnPermissionCallback
import com.hjq.permissions.Permission
import com.hjq.permissions.XXPermissions
import com.hjq.toast.Toaster
import com.therouter.TheRouter
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.PI
import kotlin.math.sin

/**
 * 启动页（Splash）Activity，路由路径 `/splash/entry`。
 *
 * 启动流程：
 * 1. [initView] 构建 Compose 内容（[showSplashContent]）。
 * 2. 若用户尚未同意隐私政策，先弹 [PrivacyComposeDialog]；同意后进入主流程。
 * 3. 已同意则通过 `LaunchedEffect` 申请通知权限（[requestPermissionsAndProceed]），
 *    无论授权与否最终都 [navigateToMenu]：延迟 2s 后路由至 `/menu/main` 并 `finish()`。
 *
 * 线程安排：UI 构建与动画均运行于主线程；权限申请、路由跳转在主线程发起，
 * 延迟由 `lifecycleScope` 协程 `delay(2000)` 实现，协程绑定生命周期、销毁即取消。
 *
 * ⚠️ 内存隐患：[userPrefs] 由 Hilt 字段注入（Application 级依赖），
 * 不持有 Activity 引用；所有协程均经 `lifecycleScope` / Compose 作用域，无静态泄漏。
 */
@Route(path = "/splash/entry")
@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    @Inject
    lateinit var userPrefs: UserPreferences

    /**
     * 初始化界面（由 [BaseActivity] 在 onCreate 之后调用）。
     * 职责：记录日志并调用 [showSplashContent] 构建 Compose 启动页内容。
     * ⚠️ 资源释放：仅做组合构建，无需要手动释放的资源；
     * 协程与动画由 Compose / lifecycleScope 在销毁时自动取消。
     */
    override fun initView(savedInstanceState: Bundle?) {
        LogUtils.d("SplashActivity initialized via Compose.")
        showSplashContent()
    }

    /**
     * 初始化数据（由 [BaseActivity] 在 initView 之后调用）。
     * 本启动页无需额外数据加载，故为空实现；耗时初始化已在 [showSplashContent]
     * 的 Compose 组合与权限回调中完成。
     */
    override fun initData() {}

    /**
     * 构建启动页 Compose 内容。
     * 组合粒子背景（[ParticleBackground]）、主体视觉（[SplashVisuals]），
     * 并按 [com.example.sheeps.core.preference.UserPreferences.isPrivacyAccepted]
     * 决定展示隐私协议弹窗（[PrivacyComposeDialog]）或发起权限申请。
     * 纯 UI 组合，无副作用挂起；权限与跳转交由下方方法处理。
     */
    private fun showSplashContent() {
        setContent {
            SheepsTheme {
                var showPrivacyDialog by remember { mutableStateOf(!userPrefs.isPrivacyAccepted()) }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.background,
                                    MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    MaterialTheme.colorScheme.background
                                )
                            )
                        )
                ) {
                    // 粒子背景动画
                    ParticleBackground()

                    // 主体内容
                    SplashVisuals()

                    if (showPrivacyDialog) {
                        PrivacyComposeDialog(
                            onConfirm = {
                                userPrefs.setPrivacyAccepted(true)
                                Toaster.show(getString(com.example.sheeps.core.R.string.toast_privacy_agree))
                                showPrivacyDialog = false
                            },
                            onDismiss = {
                                Toaster.show(getString(com.example.sheeps.core.R.string.toast_privacy_disagree))
                                finish()
                            }
                        )
                    } else {
                        // 已同意隐私政策：进入主流程前先申请通知权限（协程仅执行一次）
                        LaunchedEffect(Unit) {
                            requestPermissionsAndProceed()
                        }
                    }
                }
            }
        }
    }

    /**
     * 申请通知权限并继续启动流程。
     * 使用 XXPermissions 请求 [Permission.POST_NOTIFICATIONS]；无论用户授权或拒绝
     * （含“不再询问”），回调中均调用 [navigateToMenu] 进入主菜单，保证流程不卡死。
     * ⚠️ 内存/生命周期：权限回调为一次性匿名对象，授权结束后即无引用，无泄漏。
     */
    private fun requestPermissionsAndProceed() {
        XXPermissions.with(this)
            .permission(Permission.POST_NOTIFICATIONS)
            .request(object : OnPermissionCallback {
                override fun onGranted(permissions: MutableList<String>, allGranted: Boolean) {
                    navigateToMenu()
                }

                override fun onDenied(permissions: MutableList<String>, doNotAskAgain: Boolean) {
                    navigateToMenu()
                }
            })
    }

    /**
     * 跳转至主菜单并完成启动页。
     * 在 [lifecycleScope] 协程中 `delay(2000)` 后通过 TheRouter 路由到 `/menu/main`
     * 并 `finish()` 关闭启动页。
     * ⚠️ 内存/生命周期：协程绑定于 lifecycleScope，若 Activity 在 2s 内被销毁，
     * 协程自动取消，不会泄漏也不会触发重复 `finish()`。
     */
    private fun navigateToMenu() {
        // ⚠️ 协程绑定 lifecycleScope：delay 期间销毁即取消，无泄漏
        lifecycleScope.launch {
            delay(2000)
            TheRouter.build("/menu/main").navigation()
            finish()
        }
    }
}

// --- 粒子背景（随机浮动金色光点）---
/**
 * 启动页粒子背景。
 * 借助 `rememberInfiniteTransition` 驱动 10 个金色光点的浮动与明暗呼吸，
 * 并在底部叠加主题主色光晕；`Canvas` 纯绘制，不依赖外部状态。
 * ⚠️ 内存/生命周期：无限动画绑定组合生命周期，组件销毁时自动停止，无泄漏。
 * 线程约束：Composable 运行于主线程；绘制在每帧主线程完成。
 */
@Composable
fun ParticleBackground() {
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = this.size.width
        val h = this.size.height

        // 绘制 20 个随机浮动光点
        val particles = listOf(
            Triple(0.1f, 0.3f, 1.0f),
            Triple(0.8f, 0.2f, 0.7f),
            Triple(0.3f, 0.7f, 1.3f),
            Triple(0.6f, 0.5f, 0.9f),
            Triple(0.15f, 0.8f, 1.1f),
            Triple(0.9f, 0.65f, 0.8f),
            Triple(0.45f, 0.15f, 1.4f),
            Triple(0.7f, 0.85f, 0.6f),
            Triple(0.25f, 0.45f, 1.2f),
            Triple(0.85f, 0.4f, 0.95f)
        )

        particles.forEach { (xR, yR, speed) ->
            val offsetY = sin(phase * speed) * h * 0.04f
            val alpha = (sin(phase * speed * 0.7f + 1f) * 0.25f + 0.15f).coerceIn(0f, 0.4f)
            val radius = sin(phase * speed * 0.5f + 2f) * 4f + 6f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(themeSecondary.copy(alpha = alpha), Color.Transparent),
                    center = Offset(xR * w, yR * h + offsetY),
                    radius = radius * 2.5f
                ),
                radius = radius * 2.5f,
                center = Offset(xR * w, yR * h + offsetY)
            )
        }

        // 底部主题主色调光晕
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(themePrimary.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.75f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.5f, h * 0.75f)
        )
    }
}

// --- Splash 主体视觉 ---
/**
 * 启动页主体视觉。
 * 展示 Logo（多层叠放卡牌旋转动画 + 金色光晕）、游戏标题/副标题、
 * 分隔线与 `SheepsLoading` 加载指示；底部呈现适龄提示与防沉迷健康提示。
 * ⚠️ 内存/生命周期：Logo 缩放/光晕/旋转均由 `rememberInfiniteTransition`
 * 驱动，绑定组合生命周期，销毁即停止，无泄漏。
 * 线程约束：Composable 运行于主线程；动画在每帧主线程计算。
 */
@Composable
fun SplashVisuals() {
    val themePrimary = MaterialTheme.colorScheme.primary
    val themeSecondary = MaterialTheme.colorScheme.secondary
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(0.dp))

        // 中心Logo区域
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(logoScale)
        ) {
            // 金色光晕（Logo背后发光）
            Canvas(modifier = Modifier.size(240.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            themeSecondary.copy(alpha = glowAlpha * 0.3f),
                            Color.Transparent
                        )
                    ),
                    radius = this.size.width * 0.5f
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 现代叠放卡牌 Logo 动画 (代替国风六角符文)
                val rotatePhase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "logoRotation"
                )

                Box(
                    modifier = Modifier.size(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 外环：旋转的虚线圈
                    Canvas(modifier = Modifier.size(90.dp)) {
                        drawCircle(
                            color = themeSecondary.copy(alpha = 0.25f),
                            radius = size.width * 0.45f,
                            style = Stroke(
                                width = 1.5f,
                                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                    floatArrayOf(15f, 10f),
                                    0f
                                )
                            )
                        )
                    }

                    // 叠放卡牌 1 (底层，逆时针微调旋转)
                    Canvas(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                rotationZ = -rotatePhase * 0.5f
                            }
                    ) {
                        drawRoundRect(
                            color = themeSecondary.copy(alpha = 0.35f),
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                12.dp.toPx(),
                                12.dp.toPx()
                            ),
                            style = Stroke(width = 3f)
                        )
                    }

                    // 叠放卡牌 2 (顶层，顺时针旋转)
                    Canvas(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer {
                                rotationZ = rotatePhase
                            }
                    ) {
                        drawRoundRect(
                            color = themePrimary,
                            size = size,
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(
                                12.dp.toPx(),
                                12.dp.toPx()
                            ),
                            style = Stroke(width = 5f)
                        )
                        // 中心小核心圆点
                        drawCircle(
                            color = themeSecondary,
                            radius = 6f
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 游戏标题
                Text(
                    text = stringResource(id = com.example.sheeps.core.R.string.app_name),
                    fontSize = 38.sp,
                    color = themeSecondary,
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    letterSpacing = 5.sp
                )

                Spacer(Modifier.height(10.dp))

                // 副标题
                Text(
                    text = stringResource(id = com.example.sheeps.core.R.string.app_subtitle),
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(16.dp))

                // 分隔线
                Row(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f)
                                    )
                                )
                            )
                    )
                    Text(
                        text = "  ✦  ",
                        color = themeSecondary.copy(alpha = 0.6f),
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                )
                            )
                    )
                }

                Spacer(Modifier.height(20.dp))

                // Loading 指示器
                SheepsLoading(size = 36.dp)
            }
        }

        // 底部健康游戏提示区域
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 适龄提示徽章
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // 绿色适龄标记
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF00C853)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("8", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(id = com.example.sheeps.core.R.string.splash_age_tip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(id = com.example.sheeps.core.R.string.splash_health_advice),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- 隐私协议 Dialog（使用新设计语言）---
/**
 * 隐私协议弹窗（Compose `Dialog`，不可通过返回/外部点击关闭）。
 * 以富文本展示隐私政策，并将正文中的《用户协议》《隐私政策》关键词标注为
 * 可点击链接（[androidx.compose.foundation.text.ClickableText] +
 * [androidx.compose.ui.platform.LocalUriHandler] 打开对应网页）。
 * 同意则写入隐私标记并关闭弹窗；拒绝则提示并 `finish()` 退出应用。
 * ⚠️ 内存隐患：弹窗仅在未同意隐私时组合，同意后即从组合树移除并释放；
 * 不持有 Activity/Context 或静态引用。`LocalUriHandler` 由 Compose 提供，随组件销毁释放。
 *
 * @param onConfirm 用户点击“同意并开始”的回调
 * @param onDismiss  用户点击“不同意并退出”的回调
 */
@Composable
fun PrivacyComposeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(ShapeLarge)
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    )
                )
                .background(MaterialTheme.colorScheme.surface)
                .padding(1.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeLarge)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(id = com.example.sheeps.core.R.string.privacy_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val fullText =
                    stringResource(id = com.example.sheeps.core.R.string.privacy_dialog_content)
                val agreementKeywords =
                    listOf("《用户协议》", "《用戶協議》", "User Agreement", "利用規約", "이용 약관")
                val privacyKeywords = listOf(
                    "《隐私政策》",
                    "《隱私政策》",
                    "Privacy Policy",
                    "プライバシーポリシー",
                    "개인정보 처리방침"
                )

                val agreementKeyword = agreementKeywords.firstOrNull { fullText.contains(it) }
                val privacyKeyword = privacyKeywords.firstOrNull { fullText.contains(it) }

                val annotatedString = buildAnnotatedString {
                    append(fullText)
                    agreementKeyword?.let { keyword ->
                        val start = fullText.indexOf(keyword)
                        if (start != -1) {
                            val end = start + keyword.length
                            addStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold
                                ),
                                start = start,
                                end = end
                            )
                            addStringAnnotation(
                                tag = "URL",
                                annotation = AppConfig.BASE_URL + "agreement.html",
                                start = start,
                                end = end
                            )
                        }
                    }
                    privacyKeyword?.let { keyword ->
                        val start = fullText.indexOf(keyword)
                        if (start != -1) {
                            val end = start + keyword.length
                            addStyle(
                                style = SpanStyle(
                                    color = MaterialTheme.colorScheme.primary,
                                    textDecoration = TextDecoration.Underline,
                                    fontWeight = FontWeight.Bold
                                ),
                                start = start,
                                end = end
                            )
                            addStringAnnotation(
                                tag = "URL",
                                annotation = AppConfig.BASE_URL + "privacy.html",
                                start = start,
                                end = end
                            )
                        }
                    }
                }

                val uriHandler = LocalUriHandler.current
                ClickableText(
                    text = annotatedString,
                    style = androidx.compose.ui.text.TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                    ),
                    modifier = Modifier.padding(bottom = 24.dp),
                    onClick = { offset ->
                        annotatedString.getStringAnnotations(
                            tag = "URL",
                            start = offset,
                            end = offset
                        )
                            .firstOrNull()?.let { annotation ->
                                uriHandler.openUri(annotation.item)
                            }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GhostButton(
                        text = stringResource(id = com.example.sheeps.core.R.string.btn_disagree_exit),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryButton(
                        text = stringResource(id = com.example.sheeps.core.R.string.btn_agree_start),
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
