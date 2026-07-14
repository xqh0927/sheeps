package com.example.sheeps.game.service

import com.example.sheeps.core.base.IGameService
import com.example.sheeps.core.preference.UserPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [IGameService] 游戏模块向外暴露的接口实现类。
 * 采用 Hilt 构造注入，不沾染路由反射依赖。
 */
@Singleton
class GameServiceImpl @Inject constructor(
    private val prefs: UserPreferences
) : IGameService {

    override fun getHighestLevelCleared(): Int {
        return prefs.getHighestLevelCleared()
    }

    override fun getEndlessBest(): Int {
        return prefs.getEndlessBest()
    }
}
