package com.example.sheeps.game.viewmodel.engine

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import kotlin.math.ln

/**
 * 无尽生存模式纯逻辑核心（概念 A：列重力网格）。
 *
 * 所有方法均为 pure（输入状态片段 → 输出新片段），不依赖 Compose / Android，
 * 便于 ViewModel 直接调用与（未来）单元测试。
 *
 * 设计要点：
 * - 列重力天然只有列顶（最后一员）可点，不涉及 3D 遮挡逻辑（与 GameEngine 解耦）。
 * - 计分：score += baseGain * comboMultiplier(combo)，match 成功 combo+1，否则归零。
 * - 死亡：任意列高 >= deathRow → "death_line"；卡槽 >= maxSlot → "slot_overflow"。
 */
object EndlessEngine {

    /** 每张消除的基础分 */
    const val BASE_GAIN = 100

    // ===== 返回数据类 =====

    data class ClickResult(
        val columns: List<List<Tile>>,
        val slot: List<Tile>,
        val matched: Boolean,
        val eliminatedIds: Set<String>
    )

    data class MatchResult(
        val newSlot: List<Tile>,
        val eliminatedIds: Set<String>,
        val baseGain: Int
    )

    /**
     * 行式推入：从顶部压入一整行 [rowTiles]（每行随机列数）。
     *
     * 约定：[rowTiles] 与 [columns] 等长；[rowTiles] 中非空元素（[Tile?]）代表该列本行有牌，
     * 追加到对应列**末尾（顶部）**，并修正其 x/y（y = 当前列高）以贴合网格。
     * @return 新 columns 与该次推入是否导致触顶死亡。
     */
    fun pushRow(
        columns: List<List<Tile>>,
        rowTiles: List<Tile?>,
        deathRow: Int
    ): Pair<List<List<Tile>>, Boolean> {
        require(rowTiles.size == columns.size) { "pushRow: rowTiles.size(${rowTiles.size}) must equal columns.size(${columns.size})" }
        val newColumns = columns.mapIndexed { col, column ->
            val t = rowTiles[col]
            if (t != null) {
                column + t.copy(
                    x = col.toFloat(),
                    y = column.size.toFloat(),
                    z = 0,
                    state = TileState.NORMAL
                )
            } else {
                column
            }
        }
        val dead = newColumns.any { it.size >= deathRow }
        return newColumns to dead
    }

    /**
     * 点击 [col] 列内的指定卡牌 [tileId]：取出该牌并让其飞入槽中，
     * 同时其上方的所有卡牌在逻辑上由于重组自动坠落 1 格。之后立即检测三消。
     */
    fun clickColumn(
        columns: List<List<Tile>>,
        slot: List<Tile>,
        col: Int,
        tileId: String,
        maxSlot: Int
    ): ClickResult {
        if (col < 0 || col >= columns.size) return ClickResult(columns, slot, false, emptySet())
        val column = columns[col]
        val clickedIndex = column.indexOfFirst { it.id == tileId }
        if (clickedIndex == -1) return ClickResult(columns, slot, false, emptySet())

        val targetTile = column[clickedIndex]
        val newColumn = column.toMutableList().also { it.removeAt(clickedIndex) }
        val newColumns = columns.toMutableList().also { it[col] = newColumn }
        val newSlot = slot + targetTile.copy(state = TileState.IN_SLOT)

        val matchResult = resolveMatch(newSlot)
        return ClickResult(
            columns = newColumns,
            slot = matchResult.newSlot,
            matched = matchResult.eliminatedIds.isNotEmpty(),
            eliminatedIds = matchResult.eliminatedIds
        )
    }

    /**
     * 在 slot 中检测并消除 3 同花：任意 type 数量 >= 3 时移除前 3 张，返回消除 ids。
     * 一次点击只消一组（简化手感，余下随后续点击再消）。
     */
    fun resolveMatch(slot: List<Tile>): MatchResult {
        val groups = slot.groupBy { it.type }
        val matchType = groups.entries.firstOrNull { it.value.size >= 3 }?.key ?: return MatchResult(slot, emptySet(), 0)
        val toRemove = groups[matchType]!!.take(3).map { it.id }.toSet()
        val newSlot = slot.filter { it.id !in toRemove }
        return MatchResult(newSlot, toRemove, BASE_GAIN)
    }

    /**
     * 连击倍率：1 + ln(combo)，combo 至少取 1（保证首消倍率 = 1）。
     */
    fun comboMultiplier(combo: Int): Double = 1.0 + ln(combo.coerceAtLeast(1).toDouble())

    /**
     * 死亡判定：返回死因字符串，未死亡返回 null。
     */
    fun isDead(
        columns: List<List<Tile>>,
        slot: List<Tile>,
        deathRow: Int,
        maxSlot: Int
    ): String? {
        if (columns.any { it.size >= deathRow }) return "death_line"
        if (slot.size >= maxSlot) return "slot_overflow"
        return null
    }
}
