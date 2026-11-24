package com.example.sheeps.core.utils

import com.example.sheeps.ui.R
import org.junit.Assert.assertEquals
import org.junit.Test

class ItemNameTest {
    @Test
    fun catalogNameWinsForCustomAndBuiltInSkins() {
        for (type in listOf("SKIN_HENAN", "SKIN_LANTERN")) {
            assertEquals("Lantern Parade", resolveItemName(type, "Lantern Parade") { error("Unexpected fallback") })
        }
    }

    @Test
    fun emptyCatalogStillResolvesCurrentLegacyAndCustomSkinsAndProps() {
        val knownResources = mapOf(
            "SKIN_ELECTRONIC" to R.string.item_skin_electronic,
            "SKIN_HENAN" to R.string.item_skin_henan,
            "SKIN_SICHUAN" to R.string.item_skin_sichuan,
            "SKIN_SHUANG" to R.string.item_skin_shuang,
            "SKIN_DAILY" to R.string.item_skin_daily,
            "SKIN_VEGETABLE" to R.string.item_skin_vegetable,
            "SKIN_FRUIT" to R.string.item_skin_fruit,
            "CLASSIC" to R.string.item_skin_classic,
            "SKIN_INK" to R.string.item_skin_ink,
            "SKIN_CYBER" to R.string.item_skin_cyber,
            "SKIN_KEAI" to R.string.item_skin_keai,
            "SKIN_DAIMENG" to R.string.item_skin_daimeng,
            "skin_lantern" to R.string.item_skin_custom,
            "UNDO" to R.string.item_undo,
            "DOUBLE_POINTS" to R.string.item_double_points
        )
        for ((type, resource) in knownResources) {
            for (catalogName in listOf(null, "   ")) {
                val actual = resolveItemName(type, catalogName) { id ->
                    assertEquals(type, resource, id)
                    "localized"
                }
                assertEquals(type, "localized", actual)
            }
        }
    }

    @Test
    fun unknownLegacyNonSkinDescriptionsStayReadable() {
        assertEquals("节日礼包", resolveItemName("节日礼包", null) { error("Unexpected fallback") })
    }
}
