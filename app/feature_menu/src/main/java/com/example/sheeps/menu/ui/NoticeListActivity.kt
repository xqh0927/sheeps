package com.example.sheeps.menu.ui

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import com.example.sheeps.core.base.BaseActivity
import com.example.sheeps.data.model.Notice
import com.example.sheeps.menu.ui.screens.NoticeListScreen
import com.example.sheeps.theme.SheepsTheme
import com.therouter.router.Route
import dagger.hilt.android.AndroidEntryPoint
import jakarta.inject.Inject
import kotlinx.serialization.json.Json

/**
 * 公告列表全屏界面。
 *
 * 生命周期与内存说明：
 * - 数据通过 Intent 的 "noticesJson" 透传（由 MenuActivity 序列化后带入），本身不发起网络请求、无协程。
 * - 反序列化得到的 [notices] 列表作为 Activity 属性持有，随 Activity 销毁而释放，不跨页长期引用，无泄漏。
 * - 反序列化失败则降级为空列表，保证界面可渲染。
 */
@Route(path = "/menu/notices")
@AndroidEntryPoint
class NoticeListActivity : BaseActivity() {

    @Inject
    lateinit var json: Json

    // 反序列化后的公告列表，仅在本 Activity 生命周期内有效；不参与持久化，无跨页引用风险。
    private lateinit var notices: List<Notice>

    override fun initView(savedInstanceState: Bundle?) {
        // 从 Intent 读取公告数据
        val noticesJson = intent.getStringExtra("noticesJson")
        notices = if (noticesJson != null) {
            try {
                json.decodeFromString<List<Notice>>(noticesJson)
            } catch (e: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }

        setContent {
            SheepsTheme {
                BackHandler { finish() }
                NoticeListScreen(
                    notices = notices,
                    onBack = { finish() }
                )
            }
        }
    }

    override fun initData() {}
}
