package com.example.sheeps.splash

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.blankj.utilcode.util.LogUtils
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.core.preference.UserPreferences
import com.example.sheeps.theme.*
import com.example.sheeps.ui.components.PrimaryButton
import com.example.sheeps.ui.components.GhostButton
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
import kotlin.math.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.res.stringResource
import javax.inject.Inject

@Route(path = "/splash/entry")
@AndroidEntryPoint
class SplashActivity : BaseActivity() {

    @Inject
    lateinit var userPrefs: UserPreferences

    override fun initView(savedInstanceState: Bundle?) {
        LogUtils.d("SplashActivity initialized via Compose.")
        showSplashContent()
    }

    override fun initData() {}

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
                                    Color(0xFF0D1117),
                                    Color(0xFF1A0A0A),
                                    Color(0xFF2D0808)
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
                                Toaster.show("感谢您的理解与同意！")
                                showPrivacyDialog = false
                            },
                            onDismiss = {
                                Toaster.show("很抱歉，您需要同意隐私政策才能体验游戏")
                                finish()
                            }
                        )
                    } else {
                        LaunchedEffect(Unit) {
                            requestPermissionsAndProceed()
                        }
                    }
                }
            }
        }
    }

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

    private fun navigateToMenu() {
        lifecycleScope.launch {
            delay(2000)
            TheRouter.build("/menu/main").navigation()
            finish()
        }
    }
}

// --- 粒子背景（随机浮动金色光点）---
@Composable
fun ParticleBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue  = 2 * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation  = tween(8000, easing = LinearEasing),
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
            val alpha   = (sin(phase * speed * 0.7f + 1f) * 0.3f + 0.15f).coerceIn(0f, 0.5f)
            val radius  = sin(phase * speed * 0.5f + 2f) * 2f + 3f

            drawCircle(
                color  = Gold_Primary,
                radius = radius,
                center = Offset(xR * w, yR * h + offsetY),
                alpha  = alpha
            )
        }

        // 底部红色光晕
        drawCircle(
            brush  = Brush.radialGradient(
                colors = listOf(Crimson_Primary.copy(alpha = 0.3f), Color.Transparent),
                center = Offset(w * 0.5f, h * 0.75f),
                radius = w * 0.6f
            ),
            radius = w * 0.6f,
            center = Offset(w * 0.5f, h * 0.75f)
        )
    }
}

// --- Splash 主体视觉 ---
@Composable
fun SplashVisuals() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue  = 1.03f,
        animationSpec = infiniteRepeatable(
            animation  = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logoScale"
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation  = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    Column(
        modifier              = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.SpaceBetween
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
                    brush  = Brush.radialGradient(
                        colors = listOf(Gold_Primary.copy(alpha = glowAlpha * 0.3f), Color.Transparent)
                    ),
                    radius = this.size.width * 0.5f
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // 装饰图案（六角符文）
                Canvas(modifier = Modifier.size(80.dp)) {
                    val cx = this.size.width / 2f
                    val cy = this.size.height / 2f
                    val r  = this.size.width * 0.45f
                    for (i in 0 until 6) {
                        val angle = Math.toRadians(60.0 * i)
                        val x = cx + r * cos(angle).toFloat()
                        val y = cy + r * sin(angle).toFloat()
                        drawLine(
                            color       = Gold_Primary.copy(alpha = 0.6f),
                            start       = Offset(cx, cy),
                            end         = Offset(x, y),
                            strokeWidth = 1.5f,
                            cap         = StrokeCap.Round
                        )
                    }
                    drawCircle(color = Gold_Primary.copy(alpha = 0.4f), radius = r * 0.6f, style = Stroke(width = 1.5f))
                    drawCircle(color = Gold_Primary.copy(alpha = 0.2f), radius = r, style = Stroke(width = 1f))
                    drawCircle(color = Crimson_Primary.copy(alpha = 0.8f), radius = 8f)
                }

                Spacer(Modifier.height(20.dp))

                // 游戏标题
                Text(
                    text       = stringResource(id = com.example.sheeps.core.R.string.app_name),
                    fontSize   = 38.sp,
                    color      = Gold_Primary,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Serif,
                    textAlign  = TextAlign.Center,
                    letterSpacing = 4.sp
                )

                Spacer(Modifier.height(10.dp))

                // 副标题
                Text(
                    text       = stringResource(id = com.example.sheeps.core.R.string.app_subtitle),
                    fontSize   = 16.sp,
                    color      = Text_Secondary_Dark,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Serif,
                    textAlign  = TextAlign.Center,
                    letterSpacing = 2.sp
                )

                Spacer(Modifier.height(16.dp))

                // 分隔线
                Row(
                    modifier           = Modifier.fillMaxWidth(0.6f),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment  = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color.Transparent, Gold_Subtle.copy(alpha = 0.5f))
                                )
                            )
                    )
                    Text(
                        text   = "  ✦  ",
                        color  = Gold_Subtle,
                        fontSize = 12.sp
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Gold_Subtle.copy(alpha = 0.5f), Color.Transparent)
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
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(MoYe_SurfaceVariant.copy(alpha = 0.6f))
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
                    text     = "适龄提示：适合 8 岁及以上用户",
                    color    = Text_Secondary_Dark,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.height(12.dp))

            Text(
                text      = "健康游戏忠告\n抵制不良游戏，拒绝盗版游戏。注意自我保护，谨防受骗上当。\n适度游戏益脑，沉迷游戏伤身。合理安排时间，享受健康生活。",
                color     = Text_Disabled_Dark,
                fontSize  = 10.sp,
                lineHeight = 15.sp,
                textAlign = TextAlign.Center,
                modifier  = Modifier.fillMaxWidth()
            )
        }
    }
}

// --- 隐私协议 Dialog（使用新设计语言）---
@Composable
fun PrivacyComposeDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false, usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(ShapeLarge)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Gold_Subtle.copy(alpha = 0.15f), Color.Transparent)
                    )
                )
                .background(MoYe_Surface)
                .padding(1.5.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(ShapeLarge)
                    .background(MoYe_SurfaceVariant)
                    .padding(24.dp)
            ) {
                Text(
                    text       = "用户服务与隐私协议",
                    fontWeight = FontWeight.Bold,
                    fontSize   = 20.sp,
                    color      = Crimson_Primary,
                    fontFamily = FontFamily.Serif,
                    modifier   = Modifier.padding(bottom = 12.dp)
                )

                Text(
                    text      = "感谢您体验《秘境消消乐》！\n\n在您开始游戏前，请认真阅读并理解《用户协议》与《隐私政策》。\n\n点击‘同意并开始’表示您已了解并同意以上条款。",
                    fontSize  = 14.sp,
                    lineHeight = 22.sp,
                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f),
                    modifier  = Modifier.padding(bottom = 24.dp)
                )

                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    GhostButton(
                        text     = "不同意并退出",
                        onClick  = onDismiss,
                        modifier = Modifier.weight(1f),
                        color    = Text_Secondary_Dark
                    )
                    PrimaryButton(
                        text     = "同意并开始",
                        onClick  = onConfirm,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}
