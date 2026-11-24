package com.example.sheeps.ui

import java.io.File
import java.util.Locale
import javax.xml.parsers.DocumentBuilderFactory
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element

class PrivacyNoticeTest {
    @Test
    fun everyNoticeNamesTheCurrentAppAndIncludesBothPolicyLinks() {
        val links = mapOf(
            "values" to ("《用户协议》" to "《隐私政策》"),
            "values-zh-rTW" to ("《用戶協議》" to "《隱私政策》"),
            "values-en" to ("User Agreement" to "Privacy Policy"),
            "values-ja" to ("利用規約" to "プライバシーポリシー"),
            "values-ko" to ("이용 약관" to "개인정보 처리방침")
        )
        for ((directory, labels) in links) {
            val notice = File("src/main/res/$directory/strings.xml").inputStream().use {
                val strings = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
                    .getElementsByTagName("string")
                (0 until strings.length).map { index -> strings.item(index) as Element }
                    .single { node -> node.getAttribute("name") == "privacy_dialog_content" }.textContent
            }
            val rendered = String.format(Locale.ROOT, notice, "Test Game")
            assertTrue("$directory uses the current app name", "Test Game" in rendered)
            assertTrue("$directory links to both policies", labels.first in rendered && labels.second in rendered)
        }
    }
}
