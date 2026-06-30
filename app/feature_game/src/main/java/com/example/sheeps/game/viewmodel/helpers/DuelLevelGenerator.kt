package com.example.sheeps.game.viewmodel.helpers

import com.example.sheeps.data.model.Tile
import javax.inject.Inject

/**
 * 对决模式本地关卡生成器
 */
class DuelLevelGenerator @Inject constructor() {

    fun generateDuelLevel(): List<Tile> {
        val levelId = 2
        val numTypes = 6
        val coordinates = mutableListOf<Point3D>()
        val baseSize = 6
        
        // 生成简单的 4 层堆叠布局
        for (z in 0 until 4) {
            val size = maxOf(3, baseSize - z / 3)
            val offset = if (z % 2 == 0) 0.0f else 0.5f
            for (r in 0 until size) {
                for (c in 0 until size) {
                    coordinates.add(Point3D(c + offset + 1.0f, r + offset + 1.0f, z))
                }
            }
        }
        
        // 保证是 3 的倍数
        val count = coordinates.size - (coordinates.size % 3)
        return coordinates.take(count).mapIndexed { index, coord ->
            Tile(
                id = "duel_tile_$index",
                type = (index % numTypes) + 1,
                x = coord.x,
                y = coord.y,
                z = coord.z
            )
        }
    }

    private data class Point3D(val x: Float, val y: Float, val z: Int)
}
