# 秘境消消乐 · 用户道具与皮肤管理设计规格书

本规格书详述了在管理后台（Admin Console）增加对 App 用户的道具卡数量、拥有的皮肤进行查看与编辑修改的技术方案。

---

## 1. 业务背景与目标
目前系统中的三消游戏支持多种局内法宝道具（撤销、移出、洗牌、还魂丹、提示等）和多种高清 Canvas 矢量自绘制卡牌皮肤。这些资产和数据在云端 D1 数据库的 `user_items` 表中存储。
本项改进旨在为管理后台提供完整的用户资产查看与快速修改入口，包括：
1. 查看和修改用户在游戏中的消耗性道具卡数量。
2. 查看和授予/收回用户的卡牌皮肤所有权。
3. 整合统一的积分（金币）与道具编辑入口。

---

## 2. 系统架构与数据关系
道具和皮肤都统一作为 `item_type` 记录在 `user_items` 表中：
- **道具类型 (Props)**：`UNDO` (撤销符), `MOVEOUT` (缩地咒), `SHUFFLE` (流沙契), `REVIVE` (还魂丹), `HINT` (天眼符), `BOMB` (雷震子), `JOKER` (太极牌), `DOUBLE_POINTS` (双倍符)
- **皮肤类型 (Skins)**：`SKIN_INK` (水墨江山), `SKIN_CYBER` (赛博霓虹), `SKIN_HENAN` (河南·省味), `SKIN_SICHUAN` (四川·省味), `SKIN_SHUANG` (萌趣竞技)

在 `user_items` 表结构中：
- 道具：`count` 代表拥有张数。
- 皮肤：`count >= 1` 代表已拥有，`count = 0` 或无对应行记录代表未拥有。

---

## 3. 接口变更说明 (Backend Changes)

### 3.1 查询用户资产
- **请求方法**：`GET`
- **端点**：`/api/admin/users/:id/items`
- **鉴权要求**：三级管理员角色守卫 (`requireAdmin`)
- **响应体**：
  ```json
  {
    "success": true,
    "list": [
      { "item_type": "UNDO", "count": 5 },
      { "item_type": "SKIN_INK", "count": 1 }
    ]
  }
  ```

### 3.2 批量保存修改用户资产
- **请求方法**：`POST`
- **端点**：`/api/admin/users/:id/items`
- **鉴权要求**：管理员写权限守卫 (`requireAdmin`, `assertCanWrite`)
- **请求体**：
  ```json
  {
    "items": [
      { "item_type": "UNDO", "count": 10 },
      { "item_type": "SKIN_INK", "count": 0 }
    ]
  }
  ```
- **数据库事务/批处理行为**：
  在 Cloudflare D1 数据库中进行以下批量操作：
  - 对提交的每一项进行 `INSERT OR REPLACE INTO user_items (user_id, item_type, count) VALUES (?, ?, ?)`。
- **审计日志**：
  写入 `admin_audit_log` 审计记录，操作动作为 `UPDATE_USER_ITEMS`。

---

## 4. 前端界面设计 (Frontend Changes)

### 4.1 用户管理列表页增加入口
- 在 [Users.tsx](file:///e:/file/sheeps/admin-console/src/pages/Users.tsx) 表格的操作栏中，在原有按钮的旁边新增一个💼（背包，Mui图标：`ShoppingBag`）按钮，提供 Tooltip “管理道具与皮肤”。

### 4.2 资产管理对话框
点击背包按钮后弹出 `AssetDialog` 窗口：
1. **标题**：`管理用户资产 · [用户手机号/昵称]`
2. **道具修改区**：
   - 包含 `UNDO`、`MOVEOUT`、`SHUFFLE`、`REVIVE`、`HINT`、`BOMB`、`JOKER`、`DOUBLE_POINTS` 8 种道具。
   - 使用数字输入框，管理员可直接增减或填入数值（限制大于等于 0 的整数）。
3. **皮肤修改区**：
   - 包含 `SKIN_INK`、`SKIN_CYBER`、`SKIN_HENAN`、`SKIN_SICHUAN`、`SKIN_SHUANG` 5 种皮肤。
   - 每种皮肤右侧展示为 Mui `Switch`（开关组件），开启即代表拥有（提交数量 `1`），关闭即代表收回（提交数量 `0`）。
4. **积分（金币）辅助显示**：
   - 底部提供一个链接或辅助文本，展示当前积分数值，并允许直接导航到已有的“调整积分”对话框。
5. **保存与提交**：
   - 点击“保存”时，调用 `POST /api/admin/users/:id/items` 将所有表单数据序列化后提交，刷新列表并弹窗提示成功。
