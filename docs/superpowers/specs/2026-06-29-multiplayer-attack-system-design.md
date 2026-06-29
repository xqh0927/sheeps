# 多人实时竞技与攻击反馈系统设计方案

本设计方案旨在为“国风多人消消乐”手游的多人实时竞技模式提供网络传输、状态同步、攻击计算以及异常容错的技术设计。

## 1. 总体网络架构与选型
* **协议选型**：WebSocket 协议。
* **客户端实现**：基于现有项目的 OkHttp 3 WebSocket 接口，不额外引入 Ktor Client，以节省包体积并复用现有的连接管理配置。
* **服务端实现**：Cloudflare Workers WebSocket / Durable Objects (提供多端协同的对局状态同步存储)。
* **序列化格式**：采用 `Kotlinx.serialization` 进行 JSON 文本格式的序列化和反序列化。

## 2. 通用数据模型设计 (`GameCommand`)

定义一个支持多种操作类型、防重放、支持版本和校验的数据载体。

```kotlin
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
    SYSTEM_EVENT       // 系统通知（例如对方断线倒计时、宣判胜负）
}

/**
 * 详细载荷数据模型
 */
@Serializable
data class CommandPayload(
    val tilesEliminated: List<String>? = null, // 本次消除的卡牌 ID 列表（用于合法性校验）
    val comboCount: Int = 0,                   // 当前连击数
    val attackPower: Double = 0.0,             // 客户端计算的预估攻击力（供显示，以服务器校验为准）
    val obstacleType: String? = null,          // 攻击对方的障碍物类型（如: SEALED 封印, BLIND 暗牌）
    val targetPlayerId: String? = null,        // 接收攻击的目标玩家 ID
    val activeBuffId: String? = null,          // 触发的 BUFF 法宝 ID
    val systemMessage: String? = null          // 系统事件描述
)
```

## 3. 非线性攻击力转换与防刷机制

### 3.1 非线性攻击力转换公式
为了平衡对局节奏，防止高连击造成的瞬杀，采用对数缓动攻击力公式：

$$Attack = Base \times (1 + \ln(Combo))$$

其中 $Base = 10.0$（基础伤害），公式的转换规律如下：

| 连击数 (Combo) | 攻击力公式运算 | 攻击力结果 (Attack) |
| :--- | :--- | :--- |
| 1 | $10.0 \times (1 + \ln(1))$ | 10.0 |
| 2 | $10.0 \times (1 + \ln(2))$ | 16.93 |
| 3 | $10.0 \times (1 + \ln(3))$ | 20.99 |
| 4 | $10.0 \times (1 + \ln(4))$ | 23.86 |
| 5 | $10.0 \times (1 + \ln(5))$ | 26.09 |
| 10 | $10.0 \times (1 + \ln(10))$ | 33.03 |

当 Combo 极大时，攻击力由于对数衰减而变得平缓，确保对手仍有法宝防守或逆袭的可能。

### 3.2 服务器防恶意刷接口设计
1. **状态机推演同步（Server-side State Machine）**：
   * 服务器内存中根据关卡初始种子（Seed）和玩家置物架队列维护状态。
   * 客户端每次 `ELIMINATE` 必须发送消除的卡牌 ID 列表。
   * 服务器验证该卡牌在层叠关系中是否已被完全暴露（Z轴最上层，四周无重叠），以及它们能否在置物架队列中组成 3 张消除。若校验失败，直接丢弃该指令，并对作弊方处以断开连接或判负的惩罚。
2. **操作间隔滑窗速率限制**：
   * 物理消除存在必要的手指移动和动画过渡时间（单次点击判定通常 $\ge 120\text{ms}$）。
   * 服务器针对每个连接使用滑窗限制：同一客户端在 100ms 内只能处理 1 次 `ELIMINATE` 或 `ATTACK`，超过频次的指令视为恶意刷接口，予以过滤。

## 4. 健壮的重连与优雅离线逻辑

### 4.1 指数退避重连策略
当客户端 WebSocket 网络异常中断（如 PING 心跳超时或收到非正常 `onFailure`）时：
* 触发自动重连机制，设置最大重试期限为 **15秒**。
* 重连尝试间隔采用指数退避算法：
  $$RetryDelay = \min(InitialDelay \times 2^{retryCount}, MaxDelay)$$
  where $InitialDelay = 1\text{s}$, $MaxDelay = 8\text{s}$。
* 若 15 秒内重连成功，发送 `RECONNECT_RESUME` 请求数据恢复同步；若超时未连上，则向用户提示对局已结束并切换至结算页。

### 4.2 优雅离线与胜负判定逻辑
* **心跳丢失检测**：服务端检测到 TCP 链路中断或心跳超时，将该玩家状态设置为 `DISCONNECTED`，并向其对手发送系统广播：对方断线，进入 **15秒** 重连等待期，此时游戏对局被服务器挂起。
* **重连成功**：若断线玩家在 15 秒内完成握手并发送 `RECONNECT_RESUME`，服务器下发 15 秒内对局丢失的增量状态，同时广播重连成功，对局继续。
* **超时判负**：若 15 秒定时器触发仍未连接成功，服务器自动判定断线玩家负，在线玩家胜，更新 D1 数据库结算对局。
* **主动退出立即判负**：若客户端通过 UI 主动点击退出对战，或发送主动退出指令，服务器立即判定该玩家失败。
