package com.example.sheeps.game.viewmodel.helpers

import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import javax.inject.Inject

/**
 * 无尽生存模式卡牌生成器。
 *
 * 可解性保证（核心）：以 `batchSize = activeSuitCount * 3` 为一批，
 * 每批恰好包含每种当前激活花色各 3 张，打乱顺序后逐个吐出。
 * 因此整条序列每种花色总数均为 3 的倍数 → 理论上全可消除，
 * 死亡只可能是玩家操作失误。
 *
 * 使用与正作同源的 LCG（linear congruential generator）生成，支持每日挑战同种子公平竞速。
 * LCG：state = (state * 1103515245 + 12345) and 0x7FFFFFFF
 */
class EndlessLevelGenerator @Inject constructor() {

    private var lcgState: Long = 1
    private var batchBuffer: List<Int> = emptyList()
    private var counter: Int = 0

    /** 重置生成器到指定种子（seed == 0 时退化为 1 防止 LCG 死锁） */
    fun reset(seed: Long) {
        lcgState = if (seed == 0L) 1 else (seed and 0x7FFFFFFF)
        batchBuffer = emptyList()
        counter = 0
    }

    /**
     * LCG 步进：state = (state * 1103515245 + 12345) and 0x7FFFFFFF，返回非负 Int。
     */
    private fun lcgStep(): Int {
        lcgState = (lcgState * 1103515245 + 12345) and 0x7FFFFFFF
        return lcgState.toInt()
    }

    /**
     * 取下一整行待落牌（行式下落）。
     *
     * 智能防暴毙优化：
     * 1. 动态落牌量：前期（花色数小）每行仅落 2~3 张；中后期逐步增加至 2~4 或 3~5 张，难度曲线更平滑。
     * 2. 矮列优先放置：获取当前各列的实时高度，优先将新牌放置于最矮的几列中，防止单列随机连续落牌导致突刺暴毙，大幅提升可控性与可玩性。
     *
     * @param columns 当前棋盘的卡牌排列状态
     * @param activeSuitCount 当前激活的花色种类数
     * @param colCount 棋盘列数，默认 6
     * @return 长度 = [colCount] 的 [Tile?] 列表，非空处该列本行有牌（已带唯一 id）。
     */
    fun nextRow(columns: List<List<Tile>>, activeSuitCount: Int, colCount: Int = 6): List<Tile?> {
        // 1. 本行非空列数 k：低波次 2~3，中波次 2~4，高波次 3~5
        val k = if (activeSuitCount <= 4) {
            2 + (lcgStep() % 2) // 2 或 3 张
        } else if (activeSuitCount <= 7) {
            2 + (lcgStep() % 3) // 2, 3, 或 4 张
        } else {
            3 + (lcgStep() % 3) // 3, 4, 或 5 张
        }

        // 2. 智能矮列优先：按当前各列高度升序排列，选择较矮的 (k + 1) 列作为候选池，打乱后取前 k 个，
        // 既保障了棋盘高度大体均等防暴毙，又保留了随机性的乐趣。
        val colHeights = columns.mapIndexed { index, list -> index to list.size }
        val sortedCols = colHeights.sortedBy { it.second }.map { it.first }
        val candidatePool = sortedCols.take(minOf(colCount, k + 1))
        val chosen = shuffle(candidatePool).take(k)

        // 3. 从批次缓冲取 k 个 type（不足先补一批，以保障 3 的倍数可消除性）
        val types = mutableListOf<Int>()
        repeat(k) {
            if (batchBuffer.isEmpty()) batchBuffer = buildBatch(activeSuitCount)
            types += batchBuffer.first()
            batchBuffer = batchBuffer.drop(1)
        }

        // 4. 组装行（其余列为 null）
        val row = MutableList<Tile?>(colCount) { null }
        chosen.forEachIndexed { i, c ->
            row[c] = Tile(
                id = "endless_${counter++}",
                type = types[i],
                x = c.toFloat(),
                y = 0f,
                z = 0,
                state = TileState.NORMAL
            )
        }
        return row
    }

    /** 构建一批：每种激活花色各 3 张，再 LCG 打乱 */
    private fun buildBatch(activeSuitCount: Int): List<Int> {
        val batch = mutableListOf<Int>()
        for (t in 1..activeSuitCount) {
            repeat(3) { batch.add(t) }
        }
        return shuffle(batch)
    }

    /** 基于 LCG 的 Fisher–Yates 洗牌 */
    private fun shuffle(list: List<Int>): List<Int> {
        val arr = list.toMutableList()
        for (i in arr.size - 1 downTo 1) {
            lcgState = (lcgState * 1103515245 + 12345) and 0x7FFFFFFF
            val j = (lcgState % (i + 1)).toInt()
            val tmp = arr[i]
            arr[i] = arr[j]
            arr[j] = tmp
        }
        return arr
    }
}
