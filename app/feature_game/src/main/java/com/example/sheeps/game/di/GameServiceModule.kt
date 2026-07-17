package com.example.sheeps.game.di

import com.example.sheeps.core.base.IGameService
import com.example.sheeps.game.service.GameServiceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * 游戏服务 Hilt 依赖绑定模块。
 * 向全局容器暴露 [IGameService] 的实现，支持跨模块无感依赖注入。
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class GameServiceModule {

    @Binds
    @Singleton
    abstract fun bindGameService(impl: GameServiceImpl): IGameService
}
