package com.example.sheeps.core.base

/**
 * 游戏子模块向外暴露的跨模块服务通信接口契约。
 * 用于 feature_menu (大厅模块) 与 feature_game 之间的业务数据与逻辑解耦。
 */
interface IGameService {
    /**
     * 获取用户当前已通过的最高关卡数。
     */
    fun getHighestLevelCleared(): Int

    /**
     * 获取用户在无尽生存模式中取得的历史最佳分数。
     */
    fun getEndlessBest(): Int
}
