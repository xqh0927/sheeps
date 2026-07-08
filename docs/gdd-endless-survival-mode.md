# GDD：消了个爽 · 无尽生存模式（代号「叠塔」）

> 版本：v0.2（评审反馈整合版）
> 作者：GameDesigner
> 日期：2026-07-08
> 状态：概念锁定 + 机制细化 + 运营规格已对齐代码（数值仍 `[PLACEHOLDER]`，待 playtest）

---

## 0. 一句话定位

把现有「羊了个羊式层叠三消」装进一个**俄罗斯方块式的无尽下落井**里：卡牌持续从顶部落入、从底部堆积，玩家靠不断凑 3 消来压低牌堆；牌堆顶到顶部死亡线（或卡槽溢出）即 Game Over，**以存活时长 + 消除积分排名**。

---

## 1. 设计支柱（Design Pillars）

| # | 支柱 | 含义 | 违背示例 |
| :-- | :-- | :-- | :-- |
| P1 | **持续压迫感** | 始终存在"牌堆在逼近死亡线"的时间压力，是模式与经典闯关的本质区别 | 做成"想玩多久玩多久"的休闲解谜 |
| P2 | **复用即爽点** | 直接复用 12 花色 / 7 槽 / 3 消 / 道具，手感与正作一致，零学习成本 | 引入全新消除规则导致老玩家重学 |
| P3 | **决策有取舍** | 玩家每一下在"消哪张、留哪张、用不用道具、换不换列"间权衡，不是无脑连点 | 只剩一种合法操作、或操作完全无关紧要 |
| P4 | **积分即荣誉** | 评价维度只有"分"，且分要体现技巧（连击/险胜/长线/波次），适配现有排行榜 | 分数只随存活时间线性涨、看不出水平 |

---

## 2. 概念选择结论（评审锁定「概念 A」）

**评审结论：采用概念 A —— 无尽井（Endless Well）。** 概念 B（滚动叠堆）因 3D 遮挡导致不可控视线、重力模拟困难、底层死局概率极高，已被否决。

### 2.1 概念 A 形态
- 6 列竖井。卡牌从顶部按节奏**下落**，按列堆积（每列独立重力，落到底或落到上一张牌上停住）。
- 任何"头顶空格未被占"的牌可点（即每列当前最高那张 + 悬空牌）。点击 → 进 7 槽。
- 卡槽凑 3 同花色 → 爆掉；其上方牌因重力下落补位。
- 判负：新牌无从落入（顶部死亡线被占）→ 或卡槽 7 格满且无 3 连。

### 2.2 视觉层叠传承（评审新增）
为保留《羊了个羊》的"3D 层叠感"，**概念 A 的网格采用卡牌纵向重叠 30%~40% 的排布视觉**，而非俄罗斯方块式的完全对齐方块。
- 玩法规则仍是"列重力网格"（信息透明、可点集合明确）；
- 美术上每列相邻卡牌上下重叠 30~40%，叠出立体厚重感。
- 这是"像正作"与"像方块、好做"的最佳折中。

---

## 3. 核心游戏循环（Core Loop）

```markdown
# Core Loop: 无尽生存模式「叠塔」

## Moment-to-Moment (0–30 秒)
- Action: 顶部新牌下落；玩家点选可见牌送入 7 槽，凑 3 同花色即爆
- Feedback: 落牌"咔哒"定位音 + 落点微震；3 消爆发粒子 + 连击数跳动 + 积分飘字
- Reward: 即时积分 + 连击倍率上涨（手感正反馈）

## Session Loop (单局 1–8 分钟，[PLACEHOLDER])
- Goal: 在牌堆顶到死亡线前尽可能多消、活更久、刷高分
- Tension: 阶梯波次提速（Wave）+ 同屏花色增多；牌堆高度逼近死亡线（顶部红区预警）
- Resolution: 牌堆触顶 / 卡槽溢出 → Game Over → 结算积分 → 推送无尽专属榜单

## Long-Term Loop (小时–周)
- Progression: 跨局解锁"叠塔之王"等专属称号/皮肤挂件
- Retention Hook: 每日挑战同种子公平竞速 + 周榜 + 赛季榜
```

---

## 4. 机制规格（Mechanic Specifications）

### 4.1 机制：持续下落与堆积（Endless Feed）

**Purpose**：提供 P1 持续压迫感，是模式存在的根本。
**Player Fantasy**：在源源不断的牌潮中稳住阵脚、越战越勇。
**Input**：系统节奏（非玩家输入）—— 卡牌按 `下落节拍` 从顶部生成并下落。
**Output**：牌堆高度随时间增长；玩家消除使其回落。
**Success Condition**：玩家能在提速曲线下维持牌堆低于死亡线 ≥ 平均单局时长。
**Failure State**：任意列顶部触及死亡线 → 触发 Game Over。
**Edge Cases**：
- 同时多列即将触顶：以最先触顶的列判定，避免多列竞态。
- 玩家在生成瞬间连点：生成与可点判定有 1 帧锁（[PLACEHOLDER] 100ms），防止"幽灵牌"。
**视觉**：每列卡牌上下重叠 30~40%（见 §2.2），保留层叠美术。
**Tuning Levers**：`下落节拍(秒/行)`、`列数`、`死亡线高度`、`视觉重叠率`。
**Dependencies**：卡槽系统、三消系统、生成器（见 4.4）、波次系统（见 4.9）。

### 4.2 机制：死亡线（Death Line）

**Purpose**：把"顶到头"可视化为明确失败边界（P1）。
**Player Fantasy**：牌堆逼近红线时的心脏紧缩感。
**Input**：牌堆实时最高高度。
**Output**：越过 → Game Over；接近（顶部红区）→ 屏幕边缘脉冲红光 + 急促音。
**Success Condition**：玩家能凭红区预警及时调整，而非突死。
**Failure State**：无声突死（无预警）→ 设计缺陷，必须避免。
**Edge Cases**：消除使牌堆瞬间回落越过死亡线又回落——以"生成判定时刻"快照为准，不回溯。
**Tuning Levers**：`红区预警行数`、`预警强度曲线`。
**Dependencies**：下落系统、消除系统。

### 4.3 机制：连击生存倍率（Combo Survival）

**Purpose**：把正作"连击刷分"转为"连击即生存工具"（P3/P4）。
**Player Fantasy**：手感发热、越连越上头。
**Input**：玩家在 `连击窗口` 内完成下一次 3 消。
**Output**：连击数 +1，积分倍率 = `1 + ln(Combo)`（复用正作 PvP 公式）；窗口外未消 → 连击归零。
**Success Condition**：高手靠稳定连击拉开分数差距（体现水平）。
**Failure State**：窗口太短导致连击几乎无法维持 → 需 playtest 调 `连击窗口`。
**Edge Cases**：道具触发的消除（炸弹/小丑）计连击（鼓励道具衔接）；炸弹清场不产连击链。
**Tuning Levers**：`连击窗口(秒) [PLACEHOLDER 3s]`、`倍率上限 [PLACEHOLDER 5x]`。
**Dependencies**：三消系统、计分系统。

### 4.4 机制：可控生成分布（Fair Spawn）★首要难度杠杆

**Purpose**：保证"理论上可解"，杜绝因花色分布导致必死锁（P3 公平）。
**Player Fantasy**：相信只要操作好就能活，死亡是自己的锅不是系统的锅。
**难度杠杆修正（评审）**：**同屏花色种类是无尽模式的首要难度杠杆**，而非单纯加快下落。
- 前期（分数低）：同屏仅 **3~4 种**花色；随分数/存活时间逐步解锁至 **8~12 种**。
- 花色种类越多 → 越难在 7 槽/可见牌里凑齐 3 连 → 难度上升。
- 下落速度仅作**次级**调节（见 4.9 波次提速）。
**Input**：滚动窗口内的待落牌序列。
**Output**：每 `批次窗口` 内，各花色生成数量恒为 3 的倍数（或保证窗口内可 3 消配对），使"牌堆触顶只因操作/节奏失误"。
**批注（评审）**：`批次窗口` 基准 18 偏小——其作用是"可解性奇偶校验"，不是难度本身；难度由花色种类承担。窗口大小仅需保证 3 倍数即可，`[PLACEHOLDER]`。
**Edge Cases**：
- 难度提升时增加花色种类（3→12 渐增曲线）。
- 用与正作同源 LCG(`lcg(seed)`) 生成，支持"每日挑战同种子"公平竞速。
**Tuning Levers**：`同屏花色种类(起→终) [PLACEHOLDER 3→12]`、`批次窗口(张) [PLACEHOLDER]`、`种类解锁曲线`。
**Dependencies**：生成器、卡槽系统。

### 4.5 机制：道具复用与重平衡（Props Rebalance）

复用正作 8 道具，无尽语境下重定位；**Freeze 锁定进首版**（评审确认）。

| 道具 | 正作定位 | 无尽模式重定位 | 首版 |
| :-- | :-- | :-- | :-- |
| 移出置物架 MoveOut | 救急 | 把卡槽里最碍事的牌暂时移出 → 降低卡槽溢出风险 | ✅ |
| 撤销 Undo | 救急 | 回退上一步点击 → 边际价值低（下落不可逆） | ✅ 弱化 |
| 洗牌 Shuffle | 破局 | 重排场上可见牌花色分布 → 高价值（破解死局） | ✅ |
| 提示 Hint | 辅助 | 高亮一组可立即 3 消的牌 | ✅ |
| 炸弹 Bomb | 清场 | 炸掉指定列顶部若干牌 → 直接压低牌堆 | ✅ |
| 小丑 Joker | 万能 | 卡槽内凑 2 同花色时激活消第 3 张；触发连击 | ✅ |
| 双倍积分 Double | 刷分 | 限时积分翻倍 → 冲榜神器 | ✅ |
| **冻结 Freeze** | — | 暂停下落 `N` 秒 → 喘息窗口（无尽精髓） | ✅ **新增首版** |
| （后置）列交换 | — | 见 4.8，v1.5 评估 | ❌ |

> 道具获取/经济沿用正作积分商城 + 背包。无尽模式内可设"局内掉落"：每达成里程碑连击给 1 次免费道具（P3 动机）。

### 4.6 机制：多层可见度（Multi-layer Visibility）★死锁缓解①

**Purpose（评审痛点）**：概念 A 中 6 列仅能看到 6 张顶牌，若 6 顶牌花色全异 + 卡槽已满 6 异色 → **绝对死锁**。多层可见度让玩家看见每列顶下 2~3 张花色，支持长线规划。
**Player Fantasy**：不是凭运气撞第一张下的未知花色，而是"看得见底牌"地布局。
**Input**：渲染层读取每列顶部往下 `可见层数` 张牌。
**Output**：顶层牌实色；其下 `可见层数-1` 张牌以半透明/暗色叠加显示（仍可识别花色）。
**Success Condition**：玩家能基于可见的 2~3 层做"先消哪列、留哪张"的规划，死锁率显著下降。
**Failure State**：仅显示 1 层 → 回到死锁风险，属未达标。
**Edge Cases**：重叠 30~40% 视觉下，下层花色需保证可辨识（对比度/描边）。
**Tuning Levers**：`可见层数 [PLACEHOLDER 2~3]`。
**Dependencies**：渲染层、生成器。

### 4.7 机制：暂存区（Hold Slot）★死锁缓解②

**Purpose（评审）**：给玩家一个 1 格容错缓冲，化解"想消的牌暂时不能进槽"的窘境。
**Player Fantasy**：手里有张烫手的牌，先"寄放"一下，随时取回。
**Input**：双击或拖拽一张场上可见牌 → 进入暂存区（1 格）。
**Output**：该牌离开列、暂存区占用；玩家可随时再点暂存区取回（回到可点状态/进槽）。
**Success Condition**：暂存区成为"腾挪空间"，降低卡槽溢出急性死亡。
**Failure State**：暂存区被滥用成第 8 个槽 → 需限制（取回有 `CD` 或每局限次 `[PLACEHOLDER]`）。
**Edge Cases**：暂存牌在取回前，其原列上方牌已塌落 → 取回归位到该列顶。
**Tuning Levers**：`暂存取回 CD [PLACEHOLDER]`、`每局限次（可选）`。
**Dependencies**：卡槽系统、渲染层。

### 4.8 机制：列顶交换（Column Swap）★死锁缓解③（v1.5 评估）

**Purpose（评审）**：消耗微量积分或 CD，交换相邻两列顶部卡牌，主动创造 3 连机会。
**Player Fantasy**：差一张就能消？把隔壁那张换过来！
**Input**：选择相邻两列 → 交换其顶部可点牌。
**Output**：两列顶牌位置互换；下方牌不动。
**Success Condition**：提供"主动性 agency"，破解局部花色死局。
**Failure State**：无成本滥用 → 变为无脑最优解，丧失取舍 → 必须绑定成本（积分/CD）。
**Edge Cases**：某列顶牌处于下落途中 → 禁止交换，避免竞态。
**Tuning Levers**：`交换成本(积分) [PLACEHOLDER]` 或 `交换 CD [PLACEHOLDER]`。
**Dependencies**：渲染层、计分系统。
> **分期建议**：首版落地 4.6 多层可见 + 4.7 暂存区即可化解绝大多数死锁；4.8 列交换作为 v1.5 增强，避免首版复杂度超阈。

### 4.9 机制：阶梯波次提速（Wave Speed）★替代连续斜率

**Purpose（评审）**：连续 8% 斜率提速易致中期突死；改为**离散波次**，提速有仪式感、可预期。
**Player Fantasy**：每过一关"Wave Up"的紧张与兴奋，而非温水煮青蛙。
**Input**：每累计 `波次间隔` 积分 或 存活 `波次时长` → 进入下一波。
**Output**：下落节拍 ×(1+`波次提速%`)；屏幕弹出 "Wave N: Speed Up!" 横幅 + 音效。
**Success Condition**：提速节奏可感知、玩家能据此调整策略。
**Failure State**：波次间隔过短 → 仍等价于连续突死。
**Tuning Levers**：`波次提速% [PLACEHOLDER 10%]`、`波次间隔(积分) [PLACEHOLDER 300]`、`波次时长(秒) [PLACEHOLDER 60]`、`最高波次(封顶) [PLACEHOLDER 10]`。
**Dependencies**：下落系统、连击/计分系统。

---

## 5. 经济与平衡表（Economy / Balance）

> 所有数值为**假设（hypothesis）**，标记 `[PLACEHOLDER]`，须 playtest 校准。
> 设计"broken"定义前置：单局平均时长 < 45s 或 > 12min 任一即视为失衡。

```
变量                       | 基准值      | 最小 | 最大 | 调优备注
---------------------------|------------|------|------|-------------------------------
井列数 Columns             | 6          | 5    | 7    | 列越多越易分流，难度↓
起始下落节拍(s/行)         | 2.5        | 1.2  | 4.0  | [PLACEHOLDER] 体感测试
死亡线距顶行数             | 1          | 0    | 2    | 0=触顶即死，容错↓
红区预警行数               | 2          | 1    | 3    | 预警充分但不过度
卡槽容量 Slots             | 7          | 5    | 7    | 复用正作 7，[PLACEHOLDER] 可试 6
视觉重叠率                 | 35%        | 30%  | 40%  | 层叠美术传承（§2.2）
同屏花色种类(起→终)        | 3→12       | 3→8  | 4→12 | ★首要难度杠杆 [PLACEHOLDER]
批次窗口(张)               | [PLACEHOLDER] | 12 | 30  | 仅需保 3 倍数，奇偶校验用
多层可见层数               | 2~3        | 1    | 3    | 死锁缓解① [PLACEHOLDER]
暂存区容量                 | 1          | 1    | 1    | 死锁缓解②
暂存取回 CD(s)             | [PLACEHOLDER] | 0  | 10   | 防滥用为第8槽
基础消除分/次              | 100        | 60   | 150  | 随难度档微调
连击窗口(s)                | 3          | 2    | 5    | [PLACEHOLDER] 手感测试
连击倍率上限               | 5x         | 3x   | 8x   | 1+ln(Combo) 封顶
存活被动分/秒              | 5          | 0    | 15   | 鼓励长线 [PLACEHOLDER]
波次提速%                  | 10%        | 5%   | 15%  | 替代连续斜率（§4.9）
波次间隔(积分)             | 300        | 150  | 600  | [PLACEHOLDER]
波次时长(秒)               | 60         | 30   | 120  | [PLACEHOLDER]
最高波次(封顶)             | 10         | 5    | 20   | 防后期崩盘
冻结道具时长(s)            | 3          | 2    | 5    | 新增道具（§4.5）
单局预期时长(min)          | 3          | 0.75 | 12   | broken 边界见上
```

**平衡原则**
- 牌堆高度 = f(生成速率, 消除速率)。难度主杠杆是**同屏花色种类**（4.4），次杠杆是**波次提速**（4.9）。
- 通胀监测：若 Top1% 分数周环比涨 > 30%，触发难度档上调（参考正作 `difficulty.ts` 思路，做无尽专属难度档）。

---

## 6. 与现有系统集成（Integration）

| 现有系统 | 集成方式 | 改动量 |
| :-- | :-- | :-- |
| 12 花色卡牌 / Canvas 矢量自绘 | 直接复用，仅增加"下落/重力/重叠"渲染态 | 低 |
| 7 槽卡槽 + 3 消 | 直接复用；新增"溢出即失败"分支 | 低 |
| 道具系统 + 背包 + 商城 | 复用；新增 Freeze，重定位 4 个道具语义 | 中 |
| 积分 + 防作弊签名(`score/submit`) | 复用；`mode` 字段区分榜单（见 §8.4） | 低 |
| 排行榜(日/周/总) | **独立无尽榜**（评审确认不合并），按 `game_mode=1` 分榜 | 低 |
| LCG 生成器 | 复用；支持每日挑战同种子 | 低 |
| 每日任务 / 签到 / 赛季 | 任务目标可挂"无尽模式消除 X 次/存活 X 秒" | 低 |
| 双人 PvP | 首版不做；v2 可"同屏竞速谁先顶到头" | 高(后置) |

> 后端 `leaderboard` 增 `game_mode` 列；`/api/score/submit` 增 `mode` 参数与分榜写入 + 防作弊校验（详见 §8.4）。前端 `feature_game` 新增 `endless` 子模块，复用 `:core` 游戏引擎。

---

## 7. 新手引导（Onboarding，复用 PrepareGameDialog）

```markdown
## Onboarding Checklist（无尽模式）
- [ ] 30 秒内看到"牌从顶部落下" + 死亡线红区提示
- [ ] 教学局第 1 步保证可 3 消（生成器特调教学批次），零失败可能
- [ ] 单独演示"牌堆逼近红区→Game Over"一次（慢速教学局）
- [ ] 演示多层可见度：指出"每列能看穿 2~3 张底牌"辅助规划
- [ ] 引导使用暂存区（把一张牌寄放再取回）在低 stakes 下
- [ ] 至少 1 个道具（MoveOut / Bomb / Freeze）在低 stakes 下引导使用
- [ ] 首局结束落在"差一点就能更高分"的钩子上，诱发"再来一局"
- [ ] 首局同屏花色仅 3~4 种，从简渐入（与 §5 曲线一致）
```

---

## 8. 运营后台改动规格（Admin Console Ops Spec）★已核对代码锚点

> 以下为实施交付规格，开发阶段按此落地。**所有锚点已对照当前代码核对一致**（2026-07-08）。如需我直接改动这些文件，告知即可。

### 8.1 商品管理 `admin-console/src/pages/ShopItems.tsx`
- **原因**：无尽模式引入新道具 `FREEZE`。
- **改动**：`ITEM_TYPES`（当前 L5-8，含 `REVIVE` 与 5 个皮肤项）追加 `'FREEZE'`。
```diff
  const ITEM_TYPES = [
    'UNDO', 'MOVEOUT', 'SHUFFLE', 'REVIVE', 'HINT', 'BOMB', 'JOKER', 'DOUBLE_POINTS',
    'SKIN_INK', 'SKIN_CYBER', 'SKIN_HENAN', 'SKIN_SICHUAN', 'SKIN_SHUANG',
+   'FREEZE',
  ].map((v) => ({ label: v, value: v }));
```

### 8.2 用户道具管理 `admin-console/src/pages/Users.tsx`
- **原因**：管理员需给玩家发放/扣除 `FREEZE`（补偿/活动）。
- **改动**：`PROPS_DEFINITIONS`（当前 L34-43）追加 Freeze（标签沿用现有"X (Type)"风格）。
```diff
  const PROPS_DEFINITIONS = [
    { type: 'UNDO', label: '乾坤符 (Undo)' },
    ...
    { type: 'DOUBLE_POINTS', label: '双倍符 (Double)' },
+   { type: 'FREEZE', label: '冻结符 (Freeze)' },
  ];
```

### 8.3 数据概览 `admin-console/src/pages/Dashboard.tsx` + 类型 `src/api/admin.ts`
- **原因**：运营需监控无尽模式活跃度。
- **前端改动 A**：`STAT_CARDS`（当前 L18-28）追加无尽指标卡片，并补充图标导入（当前已导入 `People/PersonAdd/.../GridView`，需新增如 `Timeline`/`EmojiEvents`/`Whatshot`）。
```diff
  const STAT_CARDS: { key: keyof Stats; label: string; icon: JSX.Element; color: string }[] = [
    ...
    { key: 'level_count', label: '关卡数', icon: <GridView />, color: '#827717' },
+   { key: 'endless_play_count', label: '今日无尽挑战次数', icon: <Timeline />, color: '#00695c' },
+   { key: 'endless_max_score', label: '无尽模式历史最高分', icon: <EmojiEvents />, color: '#ad1457' },
  ];
```
- **前端改动 B**：`src/api/admin.ts` 的 `interface Stats`（当前 L46-56）追加对应字段，否则 `keyof Stats` 报错。
```diff
  export interface Stats {
    ...
    level_count: number;
+   endless_play_count: number;
+   endless_max_score: number;
  }
```

### 8.4 后端 `server`
- **`schema.sql` · `leaderboard` 表**（当前 L174-182）新增 `game_mode` 列：
```sql
ALTER TABLE leaderboard ADD COLUMN game_mode INTEGER NOT NULL DEFAULT 0;
-- 0 = 闯关, 1 = 无尽；DEFAULT 0 兼容历史数据。
-- 同时在 CREATE TABLE 中补该列（migrateSchema 幂等加列）。
```
- **`/api/score/submit`**：新增 `mode` 参数；`mode=1` 时写入 `game_mode=1` 行，并沿用现有加盐 SHA256 防作弊签名校验；分榜查询按 `game_mode` 过滤。
- **`handlers/admin.ts` · `getStats`（当前 L277-304）**：追加无尽指标查询。
```diff
  const [users, today, exchange, points, notice, banned, shop, task, level] = await Promise.all([ ... ]);
+ const endlessPlay = await env.DB.prepare(
+   'SELECT COUNT(*) as c FROM leaderboard WHERE game_mode = 1 AND achieved_at >= ?'
+ ).bind(startOfToday()).first<{ c: number }>();
+ const endlessMax = await env.DB.prepare(
+   'SELECT COALESCE(MAX(score),0) as m FROM leaderboard WHERE game_mode = 1'
+ ).first<{ m: number }>();
  return new Response(JSON.stringify({
    ...
    level_count: level?.c || 0,
+   endless_play_count: endlessPlay?.c || 0,
+   endless_max_score: endlessMax?.m || 0,
  }), { headers: getCorsHeaders(env) });
```

### 8.5 系统配置 `admin-console/src/pages/Config.tsx`
- **无需前端改动**：配置页动态读取数据库 `config` 表，无尽参数（如 `endless_unlock_points`、各波次系数）直接后台"新增配置"或后端 SQL 初始化即可。

---

## 9. 技术路线评估（Implementation Effort: Medium）

1. **前端 `feature_game`**：新建 `EndlessGameScreen.kt` + `EndlessGameViewModel.kt`；复用 `TileView`/`GameDock`(7 槽)/三消核心；编写重力下落网格 + 多层可见 + 暂存区逻辑。
2. **后端 `server`**：`leaderboard` 增 `game_mode`；`/api/score/submit` 增 `mode` 校验与分榜；`admin.ts getStats` 增无尽指标。
3. **后台 `admin-console`**：§8 三处前端 + 类型 + 后端查询。
4. **改动量**：中等（Medium），核心玩法复用量大，主要新增在"下落/重力/死锁缓解"与"分榜/运营指标"。

---

## 10. 开放问题 → 评审已解答

| # | 问题 | 评审结论（已采纳） |
| :-- | :-- | :-- |
| 1 | 死亡主因优先级？ | **卡槽溢出 与 牌堆触顶 并重**（急性失误 vs 慢性窒息，双威胁） |
| 2 | 首发每日挑战同种子？ | **必须做**（公平性灵魂，激发社群竞速讨论） |
| 3 | Freeze 进首版？ | **进首版**（释放时间压迫的最直接爽点，成本极低） |
| 4 | 无尽积分进总榜？ | **独立榜单，切勿合并**（分数膨胀曲线不同；设"叠塔之王"专属称号） |
| 5 | 概念 A vs B？ | **锁定 A**（评审补充 B 致命缺陷：遮挡不可控、底层死局极高） |

---

## 11. 变更日志（Changelog）

- v0.1 (2026-07-08)：概念设计草案，提出 A/B/C 三概念、推荐 A、核心循环/机制/平衡表/集成/引导/开放问题。数值全 `[PLACEHOLDER]`。
- v0.2 (2026-07-08)：**评审反馈整合**。
  - 锁定概念 A；新增 §2.2 视觉层叠传承（30~40% 纵向重叠）。
  - 新增死锁缓解三机制：§4.6 多层可见度、§4.7 暂存区、§4.8 列顶交换（v1.5）。
  - 平衡修正：§4.4 花色种类定为**首要难度杠杆**；§4.9 阶梯波次提速替代连续斜率。
  - 开放问题全部解答（§10）。
  - 新增 §8 运营后台改动规格（已核对 ShopItems/Users/Dashboard/Stats/schema/admin 代码锚点）+ §9 技术路线评估。
