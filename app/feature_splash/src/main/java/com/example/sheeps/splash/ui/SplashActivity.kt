package com.example.sheeps.splash.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import com.example.sheeps.lib_base.router.RouterPath

/**
 * 启动页 / 入口 Activity。
 */
@Route(path = RouterPath.Splash.ENTRY)
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

                Box(Modifier.fillMaxSize()) {
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
            TheRouter.build(RouterPath.Menu.MAIN).navigation()
            finish()
        }
    }
}

@Composable
private fun SplashVisuals() {
    val reveal = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        reveal.animateTo(1f, tween(900, easing = FastOutSlowInEasing))
    }
    val ink = MaterialTheme.colorScheme.background
    Box(Modifier.fillMaxSize().background(ink)) {
        Image(
            painter = painterResource(com.example.sheeps.ui.R.drawable.sheep_glass_art),
            contentDescription = null, contentScale = ContentScale.Crop,
            alignment = androidx.compose.ui.BiasAlignment(.64f, 0f),
            modifier = Modifier.fillMaxWidth().fillMaxHeight(.67f)
                .graphicsLayer {
                    val progress = reveal.value
                    scaleX = 1.09f - .09f * progress
                    scaleY = scaleX
                    alpha = .55f + .45f * progress
                }
        )
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(
            0f to Color.Transparent, .39f to Color.Transparent, .67f to ink, 1f to ink
        )))
        Column(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)
                .navigationBarsPadding().padding(horizontal = 25.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                stringResource(com.example.sheeps.ui.R.string.app_name),
                style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.graphicsLayer {
                    alpha = reveal.value
                    translationY = (1f - reveal.value) * 24.dp.toPx()
                }
            )
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth().height(2.dp),
                color = MaterialTheme.colorScheme.primary, trackColor = MaterialTheme.colorScheme.outlineVariant)
            Text(stringResource(com.example.sheeps.ui.R.string.splash_age_tip),
                color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelMedium)
            Text(stringResource(com.example.sheeps.ui.R.string.splash_health_advice),
                color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
    }
}

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
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
                    .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .navigationBarsPadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 22.dp)
            ) {
                Text(
                    stringResource(com.example.sheeps.ui.R.string.privacy_dialog_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 14.dp)
                )
                val fullText =
                    stringResource(
                        com.example.sheeps.ui.R.string.privacy_dialog_content,
                        stringResource(com.example.sheeps.ui.R.string.app_name)
                    )
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
