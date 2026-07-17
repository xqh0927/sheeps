package com.example.sheeps.splash.ui

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animate
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
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
import com.example.sheeps.lib_network.AppConfig
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.ui.theme.ShapeLarge
import com.example.sheeps.ui.theme.SheepsTheme
import com.example.sheeps.ui.components.GhostButton
import com.example.sheeps.ui.components.PrimaryButton
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
import kotlin.math.cos
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
     * 并按 [com.example.sheeps.data.preference.UserPreferences.isPrivacyAccepted]
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
                            // 极地白金明亮渐变色
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFFF8FAFC),
                                    Color(0xFFEDF2F7)
                                )
                            )
                        )
                ) {
                    // 粒子背景动画（含四角光晕装饰）
                    ParticleBackground()

                    // 主体内容
                    SplashVisuals()

                    if (showPrivacyDialog) {
                        PrivacyComposeDialog(
                            onConfirm = {
                                userPrefs.setPrivacyAccepted(true)
                                Toaster.show(getString(com.example.sheeps.ui.R.string.toast_privacy_agree))
                                showPrivacyDialog = false
                            },
                            onDismiss = {
                                Toaster.show(getString(com.example.sheeps.ui.R.string.toast_privacy_disagree))
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

// --- 粒子背景（随机浮动银河碎星 + 弥散星云云雾 + 星芒）---
/**
 * 启动页粒子背景。
 * 借助 `rememberInfiniteTransition` 驱动 24 个银河冷光微粒的三维缓漂与缩放，
 * 并在 Canvas 中动态渲染紫罗兰色与星海蓝色的流光星云（低频浮动），
 * 营造空灵深邃的宇宙质感。
 * ⚠️ 内存/生命周期：无限动画绑定组合生命周期，组件销毁时自动停止，无泄漏。
 * 线程约束：Composable 运行于主线程；绘制在每帧主线程完成。
 */
@Composable
fun ParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase"
    )
    val phase2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(16000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particlePhase2"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = this.size.width
        val h = this.size.height

        // 1. 绘制弥散星河云雾（淡马卡龙粉 nebula）
        val nebula1X = w * 0.25f + sin(phase * 0.2f) * w * 0.08f
        val nebula1Y = h * 0.30f + cos(phase * 0.15f) * h * 0.06f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFFBC2EB).copy(alpha = 0.35f), Color.Transparent),
                center = Offset(nebula1X, nebula1Y),
                radius = w * 0.55f
            ),
            radius = w * 0.55f,
            center = Offset(nebula1X, nebula1Y)
        )

        // 2. 绘制弥散星河云雾（淡天蓝色 nebula）
        val nebula2X = w * 0.75f + cos(phase2 * 0.18f) * w * 0.06f
        val nebula2Y = h * 0.65f + sin(phase2 * 0.22f) * h * 0.07f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFA6C1EE).copy(alpha = 0.30f), Color.Transparent),
                center = Offset(nebula2X, nebula2Y),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(nebula2X, nebula2Y)
        )

        // 3. 24 个碎星漂浮微粒
        val particles = listOf(
            Particle(0.10f, 0.20f, 1.0f, 3f),
            Particle(0.80f, 0.15f, 0.7f, 5f),
            Particle(0.30f, 0.60f, 1.3f, 2f),
            Particle(0.60f, 0.45f, 0.9f, 4f),
            Particle(0.15f, 0.75f, 1.1f, 6f),
            Particle(0.90f, 0.65f, 0.8f, 3f),
            Particle(0.45f, 0.10f, 1.4f, 2f),
            Particle(0.70f, 0.85f, 0.6f, 5f),
            Particle(0.25f, 0.40f, 1.2f, 4f),
            Particle(0.85f, 0.35f, 0.95f, 3f),
            Particle(0.05f, 0.50f, 1.15f, 2f),
            Particle(0.55f, 0.80f, 0.75f, 6f),
            Particle(0.35f, 0.30f, 1.25f, 3f),
            Particle(0.65f, 0.20f, 0.85f, 4f),
            Particle(0.20f, 0.90f, 1.05f, 2f),
            Particle(0.75f, 0.55f, 0.65f, 5f),
            Particle(0.40f, 0.70f, 1.35f, 3f),
            Particle(0.95f, 0.45f, 0.9f, 4f),
            Particle(0.50f, 0.05f, 1.1f, 2f),
            Particle(0.12f, 0.62f, 0.8f, 5f),
            Particle(0.82f, 0.78f, 1.2f, 3f),
            Particle(0.28f, 0.18f, 0.7f, 4f),
            Particle(0.68f, 0.38f, 1.3f, 2f),
            Particle(0.48f, 0.92f, 0.95f, 6f)
        )

        particles.forEachIndexed { index, p ->
            val offsetY = sin(phase * p.speed) * h * 0.04f
            val offsetX = cos(phase2 * p.speed * 0.8f) * w * 0.02f
            val alpha = (sin(phase * p.speed * 0.7f + 1f) * 0.3f + 0.15f).coerceIn(0f, 0.5f)
            val radius = sin(phase * p.speed * 0.5f + 2f) * 1.5f + p.baseRadius
            // 奇偶索引交替使用暖金与珊瑚暖粉色系
            val particleColor = if (index % 2 == 0) Color(0xFFFBBF24) else Color(0xFFFB7185)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(particleColor.copy(alpha = alpha), Color.Transparent),
                    center = Offset(p.xR * w + offsetX, p.yR * h + offsetY),
                    radius = radius * 3f
                ),
                radius = radius * 3f,
                center = Offset(p.xR * w + offsetX, p.yR * h + offsetY)
            )
        }

        // 4. 星芒闪烁
        val sparkles = listOf(
            Sparkle(0.18f, 0.28f, 1.6f),
            Sparkle(0.78f, 0.52f, 1.2f),
            Sparkle(0.42f, 0.82f, 1.4f),
            Sparkle(0.88f, 0.22f, 1.0f)
        )
        sparkles.forEach { s ->
            val sa = (sin(phase * s.speed + 0.5f) * 0.4f + 0.4f).coerceIn(0f, 0.8f)
            val cx = s.xR * w
            val cy = s.yR * h
            val len = 8f * sa + 3f
            val sparkleColor = Color(0xFFFBBF24)
            drawLine(color = sparkleColor.copy(alpha = sa), start = Offset(cx - len, cy), end = Offset(cx + len, cy), strokeWidth = 1.2f)
            drawLine(color = sparkleColor.copy(alpha = sa), start = Offset(cx, cy - len), end = Offset(cx, cy + len), strokeWidth = 1.2f)
        }

        // 5. 底部脉冲云气光晕
        val pulse = (sin(phase * 0.5f) * 0.05f + 0.12f).coerceIn(0f, 0.20f)
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(Color(0xFFC084FC).copy(alpha = pulse * 1.5f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.78f),
                radius = w * 0.65f
            ),
            radius = w * 0.65f,
            center = Offset(w * 0.5f, h * 0.78f)
        )
    }
}

private data class Particle(val xR: Float, val yR: Float, val speed: Float, val baseRadius: Float)
private data class Sparkle(val xR: Float, val yR: Float, val speed: Float)

// --- Splash 主体视觉 ---
/**
 * 启动页主体视觉。
 * 展示 Logo（双层反向旋转虚线环 + 三层叠放卡牌旋转动画 + 金色光晕）、
 * 渐变色游戏标题、副标题、分隔线、自定义雷达脉冲加载指示，以及底部 2s 假进度条；
 * 底部呈现适龄提示与防沉迷健康提示。
 * ⚠️ 内存/生命周期：Logo 缩放/光晕/旋转、雷达脉冲、进度条均由
 * `rememberInfiniteTransition` / `Animatable` 驱动，绑定组合生命周期，销毁即停止，无泄漏。
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
    // 3D 浮动俯仰晃动动画
    val rotX by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "3dRotX"
    )
    val rotY by infiniteTransition.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "3dRotY"
    )

    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        animate(0f, 1f, animationSpec = tween(2000)) { value, _ -> progress = value }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(0.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.scale(logoScale)
        ) {
            // 背景弥散极光圈
            Canvas(modifier = Modifier.size(240.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFFFCD34D).copy(alpha = glowAlpha * 0.4f),
                            Color(0xFFFBC2EB).copy(alpha = glowAlpha * 0.2f),
                            Color.Transparent
                        ),
                        radius = size.width * 0.5f
                    ),
                    radius = this.size.width * 0.5f
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val rotatePhase by infiniteTransition.animateFloat(
                    initialValue = 0f,
                    targetValue = 360f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(6000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "logoRotation"
                )
                val rotatePhaseRev by infiniteTransition.animateFloat(
                    initialValue = 360f,
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(9000, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "logoRotationRev"
                )

                // 应用 3D 浮摆的卡牌容器 Box
                Box(
                    modifier = Modifier
                        .size(110.dp)
                        .graphicsLayer {
                            rotationX = rotX
                            rotationY = rotY
                            cameraDistance = 18f * density
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // 星轨渐变扫光轨圈
                    Canvas(
                        modifier = Modifier
                            .size(110.dp)
                            .graphicsLayer { rotationZ = rotatePhase }
                    ) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFFFCD34D).copy(alpha = 0.9f),
                                    Color(0xFF60A5FA).copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            startAngle = 0f,
                            sweepAngle = 270f,
                            useCenter = false,
                            style = Stroke(
                                width = 2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 12f), 0f)
                            )
                        )
                    }

                    Canvas(
                        modifier = Modifier
                            .size(88.dp)
                            .graphicsLayer { rotationZ = rotatePhaseRev }
                    ) {
                        drawCircle(
                            color = Color(0xFF818CF8).copy(alpha = 0.4f),
                            radius = size.width * 0.48f,
                            style = Stroke(
                                width = 1.2f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            )
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .size(56.dp)
                            .graphicsLayer { rotationZ = -rotatePhase * 0.4f }
                    ) {
                        drawRoundRect(
                            color = Color(0xFFFBC2EB).copy(alpha = 0.5f),
                            size = size,
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                            style = Stroke(width = 3f)
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .size(48.dp)
                            .graphicsLayer { rotationZ = rotatePhase * 0.7f }
                    ) {
                        drawRoundRect(
                            color = themePrimary.copy(alpha = 0.65f),
                            size = size,
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                            style = Stroke(width = 3f)
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .size(40.dp)
                            .graphicsLayer { rotationZ = rotatePhase }
                    ) {
                        drawRoundRect(
                            color = themePrimary,
                            size = size,
                            cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx()),
                            style = Stroke(width = 5f)
                        )
                        drawCircle(color = Color(0xFFFBBF24), radius = 6f)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // 标题流光移动动画
                val shimmerTranslateX by infiniteTransition.animateFloat(
                    initialValue = -500f,
                    targetValue = 800f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(2800, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    ),
                    label = "shimmerText"
                )

                val shimmerBrush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF1E293B),
                        Color(0xFFF59E0B),
                        Color(0xFF1E293B)
                    ),
                    start = Offset(shimmerTranslateX, 0f),
                    end = Offset(shimmerTranslateX + 250f, 100f)
                )

                Text(
                    text = stringResource(id = com.example.sheeps.ui.R.string.app_name),
                    fontSize = 38.sp,
                    style = TextStyle(
                        brush = shimmerBrush
                    ),
                    fontWeight = FontWeight.ExtraBold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    letterSpacing = 5.sp
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = stringResource(id = com.example.sheeps.ui.R.string.app_subtitle),
                    fontSize = 15.sp,
                    color = Color(0xFF475569),
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.SansSerif,
                    textAlign = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(0.6f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Brush.horizontalGradient(colors = listOf(Color.Transparent, Color(0xFF93C5FD).copy(alpha = 0.4f))))
                    )
                    Text(text = "  ✦  ", color = Color(0xFF93C5FD).copy(alpha = 0.6f), fontSize = 12.sp)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(Brush.horizontalGradient(colors = listOf(Color(0xFF93C5FD).copy(alpha = 0.4f), Color.Transparent)))
                    )
                }

                Spacer(Modifier.height(20.dp))

                SplashLoadingPulse(primary = themePrimary, secondary = themeSecondary)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF2563EB),
                trackColor = Color(0xFF2563EB).copy(alpha = 0.2f)
            )

            Spacer(Modifier.height(16.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
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
                    text = stringResource(id = com.example.sheeps.ui.R.string.splash_age_tip),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text = stringResource(id = com.example.sheeps.ui.R.string.splash_health_advice),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                fontSize = 10.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SplashLoadingPulse(
    primary: Color,
    secondary: Color
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val rotateAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )

    Box(
        modifier = Modifier.size(50.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(36.dp)) {
            val w = size.width
            val h = size.height
            val cx = w / 2f
            val cy = h / 2f
            val orbitRadius = w * 0.42f

            // 1. 绘制虚线圆形轨道
            drawCircle(
                color = Color(0xFFC084FC).copy(alpha = 0.3f),
                radius = orbitRadius,
                style = Stroke(
                    width = 1.2f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 6f), 0f)
                )
            )

            // 2. 计算轨道运行粒子的 Offset 坐标
            val angle1Rad = rotateAngle * (kotlin.math.PI.toFloat() / 180f)
            val angle2Rad = (rotateAngle + 180f) * (kotlin.math.PI.toFloat() / 180f)

            val p1x = cx + kotlin.math.cos(angle1Rad) * orbitRadius
            val p1y = cy + kotlin.math.sin(angle1Rad) * orbitRadius

            val p2x = cx + kotlin.math.cos(angle2Rad) * orbitRadius
            val p2y = cy + kotlin.math.sin(angle2Rad) * orbitRadius

            // 3. 绘制绕行粒子 1 (珊瑚暖粉)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFFB7185), Color(0xFFFBC2EB).copy(alpha = 0.8f), Color.Transparent),
                    center = Offset(p1x, p1y),
                    radius = 8f
                ),
                radius = 8f,
                center = Offset(p1x, p1y)
            )

            // 4. 绘制绕行粒子 2 (晴空蓝)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF38BDF8), Color(0xFF60A5FA).copy(alpha = 0.7f), Color.Transparent),
                    center = Offset(p2x, p2y),
                    radius = 6.5f
                ),
                radius = 6.5f,
                center = Offset(p2x, p2y)
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
                    text = stringResource(id = com.example.sheeps.ui.R.string.privacy_dialog_title),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontFamily = FontFamily.SansSerif,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                val fullText =
                    stringResource(id = com.example.sheeps.ui.R.string.privacy_dialog_content)
                val agreementKeywords =
                    listOf("《用户协议》", "《用戶協議》", "《金色体育用户协议》", "User Agreement", "利用規約", "이용 약관")
                val privacyKeywords = listOf(
                    "《隐私政策》",
                    "《隱私政策》",
                    "《金色体育隐私协议》",
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
                        text = stringResource(id = com.example.sheeps.ui.R.string.btn_disagree_exit),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    PrimaryButton(
                        text = stringResource(id = com.example.sheeps.ui.R.string.btn_agree_start),
                        onClick = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
