package com.example.sheeps.core.multiplayer.model

import kotlinx.serialization.Serializable

/**
 * 核心对局同步指令包
 */
@Serializable
data class GameCommand(
    val gameId: String,          // 对局唯一 ID
    val seqId: Long,             // 自增序列号，用于防重放与时序纠错
    val timestamp: Long,         // 发送时间戳（毫秒）
    val senderId: String,        // 发送者玩家 ID
    val type: CommandType,       // 指令核心类型
    val payload: CommandPayload   // 对应详细负载
)

/**
 * 指令类别
 */
enum class CommandType {
    ELIMINATE,         // 消除卡牌操作
    ATTACK,            // 触发攻击动作
    BUFF,              // 使用法宝（如金丹/星盘防御或双倍积分）
    RECONNECT_RESUME,  // 客户端请求重连恢复
    SYSTEM_EVENT,      // 系统通知（例如对方断线倒计时、宣判胜负）
    CAST_SPELL         // 新增：主动施放恶搞诅咒
}

/**
 * 详细载荷数据模型
 */
@Serializable
data class CommandPayload(
    val tilesEliminated: List<String>? = null, // 本次消除的卡牌 ID 列表（用于合法性校验）
    val comboCount: Int = 0,                   // 当前连击数
    val attackPower: Double = 0.0,             // 客户端计算 of 预估攻击力（供显示，以服务器校验为准）
    val obstacleType: String? = null,          // 攻击对方的障碍物类型（如: SEALED 封印, BLIND 暗牌）
    val targetPlayerId: String? = null,        // 接收攻击的目标玩家 ID
    val activeBuffId: String? = null,          // 触发 of BUFF 法宝 ID
    val systemMessage: String? = null,         // 系统事件描述
    val spellType: String? = null,             // 新增：恶搞法术类型 ("FOG", "SHRINK", "SEAL_ALL")
    val spellDuration: Long = 0L               // 新增：恶搞法术持续时间（毫秒）
)
