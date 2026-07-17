package com.example.sheeps.data.repository

import com.example.sheeps.data.preference.UserPreferences
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.network.ApiService
import com.example.sheeps.data.result.ApiResult
import com.example.sheeps.data.result.TokenRefresher
import com.example.sheeps.data.result.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 关卡相关数据仓库（统一错误闸门，Phase 2）。
 *
 * 封装关卡布局获取 [getLevel]，返回 [ApiResult]。对决关卡接口不携带鉴权头，不会触发 401 重试。
 * 调用方（[com.example.sheeps.game.viewmodel.DuelViewModel]）在 [ApiResult.Error] 分支
 * 回退到本地生成算法，保证离线可玩。
 */
@Singleton
class GameRepository @Inject constructor(
    private val apiService: ApiService,
    private val tokenRefresher: TokenRefresher
) {
    private suspend fun <T> call(block: suspend () -> T): ApiResult<T> =
        safeApiCall(block) { code -> code == 401 && tokenRefresher.refreshToken() is ApiResult.Success }

    suspend fun getLevel(levelId: Int, seed: Int? = null): ApiResult<List<Tile>> =
        call { apiService.getLevel(levelId, seed) }
}
