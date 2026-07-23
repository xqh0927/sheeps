package com.example.sheeps.lib_base.router

/**
 * 全局页面路由路径常量表。
 * 用于统一管理 TheRouter 的 @Route 注解与 TheRouter.build(path) 字符串，
 * 避免硬编码字符串散落于各个模块中导致拼写错误或重构断链。
 */
object RouterPath {

    /** 启动与分发 */
    object Splash {
        const val ENTRY = "/splash/entry"
    }

    /** 认证相关 */
    object Auth {
        const val LOGIN = "/auth/login"
        const val REGISTER = "/auth/register"
        const val RESET_PASSWORD = "/auth/reset-password"
    }

    /** 大厅与外围 */
    object Menu {
        const val MAIN = "/menu/main"
        const val USER_INFO = "/menu/userinfo"
        const val SETTINGS = "/menu/settings"
        const val NOTICES = "/menu/notices"
    }

    /** H5 网页 */
    object Web {
        const val H5 = "/web/h5"
    }

    /** 游戏核心 */
    object Game {
        const val PLAY = "/game/play"
        const val DUEL = "/game/duel"
    }

    /** 无尽模式 */
    object Endless {
        const val PLAY = "/endless/play"
        const val LEADERBOARD = "/endless/leaderboard"
    }

    /** 排行榜 */
    object Leaderboard {
        const val SHOW = "/leaderboard/show"
    }
}
