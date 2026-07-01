# Unity游戏项目 - 启动指南

## 🚀 快速启动

### 第一次打开项目

1. **删除Library文件夹**（如果存在）
   - 位置：`UnityGame/Library/`
   - 删除后Unity会重新导入所有资源

2. **用Tuanjie Hub打开项目**
   - 打开Tuanjie Hub
   - 点击"添加项目" → "从磁盘添加"
   - 选择 `E:\file\sheeps\UnityGame` 文件夹
   - 等待导入完成（查看底部状态栏）

3. **创建游戏场景**
   - 菜单栏：Tools → UnityGame → Setup Game Scene
   - 等待Hierarchy面板出现GameManager、Canvas等对象

4. **保存场景**
   - Ctrl + S
   - 保存到 `Assets/Scenes/MainScene.unity`

5. **测试运行**
   - 点击Play按钮
   - 应该能看到游戏界面

---

## ⚠️ 常见问题

### 问题1：项目无法打开/卡在导入界面

**解决方案**：
- 删除Library文件夹，重新打开项目
- 检查Unity版本是否为团结引擎 1.9.2

### 问题2：Console面板显示编译错误

**解决方案**：
- 截图发给我
- 常见错误：缺少TextMeshPro包（已包含在manifest.json中）

### 问题3：场景创建后看不到游戏界面

**解决方案**：
- 检查Hierarchy面板是否有GameManager、Canvas
- 检查Console是否有运行时错误
- 确保GameManager组件已正确添加

### 问题4：卡牌图片不显示

**解决方案**：
- 将卡牌图片放到 `Assets/Sprites/` 文件夹
- 选中TileView预制体，设置sprite字段

---

## 📁 项目结构

```
UnityGame/
├── Assets/
│   ├── Editor/          # 编辑器工具
│   ├── Prefabs/         # 预制体
│   ├── Resources/       # 运行时资源
│   ├── Scenes/          # 场景文件
│   ├── Scripts/         # 游戏脚本
│   │   ├── Core/        # 核心逻辑
│   │   ├── Game/        # 游戏逻辑
│   │   └── UI/          # UI脚本
│   └── Sprites/         # 图片资源
├── Packages/            # 依赖包
└── ProjectSettings/     # 项目设置
```

---

## 🎮 游戏功能

- ✅ 卡牌匹配消除
- ✅ 7个道具功能
- ✅ 多语言支持
- ✅ 音效系统
- ✅ 皮肤系统
- ✅ 网络请求（API）

---

## 📝 下一步

1. **添加卡牌图片**：将图片放到 `Assets/Sprites/`
2. **添加音效**：将音频文件放到 `Assets/Resources/Audio/`
3. **调整游戏参数**：在GameManager组件中修改
4. **打包APK**：参考 `文档/Unity打包APK指南.md`

---

## 🆘 需要帮助？

如果遇到问题，请：
1. 查看Console面板的错误信息
2. 截图发给我
3. 描述具体操作步骤

---

**最后更新**：2026-07-01
