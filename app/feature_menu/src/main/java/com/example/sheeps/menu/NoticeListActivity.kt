package com.example.sheeps.menu

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
 * 公告列表全屏界面
 */
@Route(path = "/menu/notices")
@AndroidEntryPoint
class NoticeListActivity : BaseActivity() {

    @Inject
    lateinit var json: Json

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
