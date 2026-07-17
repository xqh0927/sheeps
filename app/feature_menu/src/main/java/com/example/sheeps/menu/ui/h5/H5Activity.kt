package com.example.sheeps.menu.ui.h5

import android.annotation.SuppressLint
import android.os.Bundle
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.example.sheeps.lib_base.base.BaseActivity
import com.example.sheeps.ui.theme.SheepsTheme
import com.example.sheeps.ui.components.SheepsTopAppBar
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint

/**
 * 独立的全屏 H5 加载 Activity。
 *
 * 通过 TheRouter 路由 "/web/h5" 跳转，接收 extras 中的 "url" 与 "title"，
 * 在 Activity 内部使用 WebView（非 Dialog）加载目标页面，统一承载
 * 本地资产页（file:///android_asset/...）与远程协议页（https://...），
 * 不再弹出 Compose Dialog，也不再逃逸到外部浏览器。
 *
 * @Route path "/web/h5"
 */
@Route(path = "/web/h5")
@AndroidEntryPoint
class H5Activity : BaseActivity() {

    override fun initView(savedInstanceState: Bundle?) {
        val url = intent?.getStringExtra("url") ?: ""
        val title = intent?.getStringExtra("title") ?: ""
        setContent {
            SheepsTheme {
                H5Content(url = url, title = title)
            }
        }
    }

    override fun initData() {}

    /**
     * H5 内容可组合项：基于 Scaffold 承载顶栏（标题 + 返回 + 刷新）与内部 WebView，
     * 自动处理状态栏与底部物理导航栏的间距（PaddingValues），保证全屏体验样式统一。
     *
     * @param url 要加载的地址
     * @param title 顶部标题
     */
    @SuppressLint("SetJavaScriptEnabled")
    @Composable
    private fun H5Content(url: String, title: String) {
        var isLoading by remember { mutableStateOf(true) }
        var webView by remember { mutableStateOf<WebView?>(null) }

        // 当 WebView 可后退时拦截系统返回键执行网页后退，否则交由系统默认返回（finish）。
        BackHandler(enabled = webView?.canGoBack() == true) {
            webView?.goBack()
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                SheepsTopAppBar(
                    title = title,
                    onBack = { finish() },
                    showAction = true,
                    actionIcon = Icons.Default.Refresh,
                    onActionClick = { webView?.reload() }
                )
            },
            containerColor = MaterialTheme.colorScheme.background
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(
                                    view: WebView?,
                                    url: String?,
                                    favicon: android.graphics.Bitmap?
                                ) {
                                    isLoading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    isLoading = false
                                }

                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    // 内部加载：返回 false 让 WebView 自行处理链接，不逃逸到外部浏览器。
                                    return false
                                }
                            }
                            loadUrl(url)
                        }.also { webView = it }
                    },
                    modifier = Modifier.fillMaxSize(),
                    onRelease = { it.destroy() }
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
