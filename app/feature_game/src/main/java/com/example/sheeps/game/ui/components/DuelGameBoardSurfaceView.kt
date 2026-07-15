package com.example.sheeps.game.ui.components

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import com.example.sheeps.core.R
import com.example.sheeps.core.game.SkinColors
import com.example.sheeps.core.game.getSkinColors
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.BuildConfig
import com.example.sheeps.game.state.DuelViewState
import com.example.sheeps.game.ui.utils.TileTextureLoader
import kotlin.math.sin

/**
 * 多人对决（PVP）模式卡牌棋盘自绘 View。
 *
 * 【注】虽然类名保留为 DuelGameBoardSurfaceView 历史遗留命名，但在实际实现中同样继承自标准 View。
 * 本组件针对多人竞技模式进行了深度定制：
 * 1. 支持高帧率重画以展示对攻时的卡牌消除节奏。
 * 2. 完美整合了“恶搞诅咒与大招系统”——包括迷雾术（Fog Overlay 探照灯擦除，用 DST_OUT 离屏渲染进行局部擦穿）和缩槽术等。
 * 3. 剥离了复杂状态机重组开销，极佳地保障了激烈竞技对战下的无延迟跟手度。
 */
class DuelGameBoardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 核心多人联机对局状态快照，携带己方棋盘卡牌、分数、诅咒效果时长等属性 */
    private var state: DuelViewState? = null

    /** 正在执行飞行动画的卡牌 ID 集合，本 View 会静默它们 */
    private var flyingTileIds: Set<String> = emptySet()

    /** 棋盘卡牌点击交互监听，通知对决 ActionDelegate 运行槽位扣减与网络同步 */
    private var onTileClick: ((Tile) -> Unit)? = null

    /** 坐标缓存 Map，为 Compose 的飞行动画提供精准起点 */
    private var tileGlobalPositions: MutableMap<String, androidx.compose.ui.geometry.Offset>? = null

    /** 当前皮肤边框/装饰色，由 Compose 层通过 [setSkinColors] 预解析传入，保证与 TileCardBase 一致 */
    private var skinBorderColor: Int? = null
    private var skinDecorColor: Int? = null

    // ==========================================
    // 绘图资源 Paint 声明与调色板定义
    // ==========================================

    /** 卡牌本体背景绘制画笔 */
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 卡牌柔和投影画笔，对齐 TileView 中 TileCardBase 的 Modifier.shadow(4.dp) 软阴影 */
    private val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#33000000".toColorInt()
        maskFilter = BlurMaskFilter(dpToPx(4f), BlurMaskFilter.Blur.NORMAL)
    }

    /** 卡牌细边缘框画笔 */
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    /** 卡牌被上层叠压时的置灰蒙版画笔 */
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (BuildConfig.DEBUG) "#ffffff".toColorInt() else "#30ffffff".toColorInt()
    }

    /** 迷雾法术专用遮罩画笔 */
    private val fogOverlayPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // ==========================================
    // 布局与多分辨率自适应缩放计算变量
    // ==========================================

    /** 卡牌在逻辑网格上的最小 X 坐标 */
    private var minX = 0f

    /** 卡牌在逻辑网格上的最小 Y 坐标 */
    private var minY = 0f

    /** 逻辑网格中卡牌的像素边长与间距大小（默认 48dp，与 TileView 对齐） */
    private var spacing = dpToPx(48f)
    private var tileSize = dpToPx(48f)

    /** 自适应缩放比例，适应不同的屏幕大小 */
    private var scale = 1f

    /** 居中偏移量 X */
    private var drawOffsetX = 0f

    /** 居中偏移量 Y */
    private var drawOffsetY = 0f

    /** 棋盘画布当前的宽和高像素大小 */
    private var surfaceWidth = 0f
    private var surfaceHeight = 0f

    /** 盲盒卡牌的背板贴图 */
    private val tileBackBitmap: Bitmap by lazy {
        val drawable = ContextCompat.getDrawable(context, R.drawable.tile_back)
        val bitmap = createBitmap(dpToPx(48f).toInt(), dpToPx(48f).toInt())
        val canvas = Canvas(bitmap)
        drawable?.setBounds(0, 0, canvas.width, canvas.height)
        drawable?.draw(canvas)
        bitmap
    }

    /** 迷雾法术生效时，用户用手指擦除黑雾的中心触控坐标点（用于计算探照灯擦穿圆形区域） */
    private var touchOffset: PointF? = null

    // ==========================================
    // 阻挡/抖动交互动画状态变量
    // ==========================================

    /** 正在发生阻挡抖动动画的卡牌 ID 集合 */
    private var activeShakeIds = emptySet<String>()

    /** 抖动动画开始的时间戳 */
    private var shakeStartTime = 0L

    /** 抖动时长定义（500ms） */
    private val shakeDuration = 500L

    /** 标识当前是否正在轮询定时器刷新抖动正弦相位 */
    private var isShaking = false

    /** 线程数据安全同步锁 */
    private val renderLock = Any()

    /** 棋盘底版背景色画笔 */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 棋盘底盘圆角边缘线画笔 */
    private val bgBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        color = "#CCCCCC".toColorInt()
    }

    // Reusable drawing objects to avoid object allocation in onDraw loop
    private val bgRectF = RectF()
    private val tempRectF = RectF()
    private val tempShadowRectF = RectF()
    private val tempClipPath = Path()
    private val tempSrcRect = Rect()
    private val tempDstRectF = RectF()
    private val tempTouchPoint = PointF()
    private var fogGradient: RadialGradient? = null

    private val baseCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#BB2C3E50".toColorInt()
    }

    private val gateBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = "#F1C40F".toColorInt()
    }

    private val lockPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
    }

    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#2980B9".toColorInt()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    init {
        // 标准 View 初始化
    }

    /**
     * 更新对战数据状态快照并重新排布渲染参数。
     */
    fun updateData(
        state: DuelViewState,
        flyingTileIds: Set<String>,
        onTileClick: (Tile) -> Unit,
        tileGlobalPositions: MutableMap<String, androidx.compose.ui.geometry.Offset>
    ) {
        synchronized(renderLock) {
            val oldShakes = this.state?.shakingTileIds ?: emptySet()

            this.state = state
            this.flyingTileIds = flyingTileIds
            this.onTileClick = onTileClick
            this.tileGlobalPositions = tileGlobalPositions

            // 监听抖动
            val newShakes = state.shakingTileIds
            if (newShakes.isNotEmpty() && newShakes != oldShakes) {
                startShakeAnimation(newShakes)
            }

            val tiles = state.boardTiles
            if (tiles.isNotEmpty()) {
                val actualMinX = tiles.minOfOrNull { it.x } ?: 0f
                val actualMaxX = tiles.maxOfOrNull { it.x } ?: 0f
                val actualMinY = tiles.minOfOrNull { it.y } ?: 0f
                val actualMaxY = tiles.maxOfOrNull { it.y } ?: 0f

                minX = minOf(state.boardBounds.minX, actualMinX)
                val maxX = maxOf(state.boardBounds.maxX, actualMaxX)
                minY = minOf(state.boardBounds.minY, actualMinY)
                val maxY = maxOf(state.boardBounds.maxY, actualMaxY)

                val contentWidth = (maxX - minX) * spacing + tileSize
                val contentHeight = (maxY - minY) * spacing + tileSize

                val viewW = if (surfaceWidth > 0f) surfaceWidth else width.toFloat()
                val viewH = if (surfaceHeight > 0f) surfaceHeight else height.toFloat()
                if (viewW > 0 && viewH > 0) {
                    scale = minOf(viewW / contentWidth, viewH / contentHeight, 1.0f)
                    drawOffsetX = (viewW - contentWidth * scale) / 2f
                    drawOffsetY = (viewH - contentHeight * scale) / 2f
                }
            }
        }
        triggerRender()
    }

    /**
     * 由 Compose 层预传入与 TileCardBase 一致的皮肤边框/装饰色。
     *
     * 由于 Canvas View 无法直接读取 Compose 的 MaterialTheme.colorScheme，统一在 Composable 中
     * 解析好颜色再传入，避免对决棋盘边框与卡槽 TileView 色差。
     */
    fun setSkinColors(borderColor: Int, decorColor: Int) {
        synchronized(renderLock) {
            skinBorderColor = borderColor
            skinDecorColor = decorColor
        }
        triggerRender()
    }

    /**
     * 对无法被点击的阻挡牌播放物理正弦抖动特效。
     *
     * @param blockerIds 当前正受到点击阻碍需要发生抖动的卡牌 ID 集合
     */
    private fun startShakeAnimation(blockerIds: Set<String>) {
        activeShakeIds = blockerIds
        shakeStartTime = System.currentTimeMillis()
        if (!isShaking) {
            isShaking = true
            post(object : Runnable {
                override fun run() {
                    if (!isShaking) return
                    val elapsed = System.currentTimeMillis() - shakeStartTime
                    if (elapsed >= shakeDuration) {
                        isShaking = false
                        activeShakeIds = emptySet()
                    } else {
                        postDelayed(this, 16) // 约 60fps 重绘制抖动相位偏移
                    }
                    triggerRender()
                }
            })
        }
    }

    /**
     * 主动要求 Canvas 重绘整个棋盘组件
     */
    private fun triggerRender() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(renderLock) {
            // 步骤 1：在底层绘制卡牌矩阵与底座面版
            drawBoard(canvas)
            // 步骤 2：在顶层叠加迷雾大招障眼法（如有迷雾被触发）
            drawFogEffect(canvas)
        }
    }

    /**
     * 渲染卡牌矩阵核心控制器。
     *
     * 1. 绘制带有圆边线和线性渐变的棋盘基座。
     * 2. 物理高度排序：对可视且不在动画飞入槽中的卡牌以 Z 轴递增排序，保证前后重压叠掩准确无误。
     * 3. 逐个渲染单张卡牌。
     */
    private fun drawBoard(canvas: Canvas) {
        if (surfaceWidth > 0f && surfaceHeight > 0f) {
            val bgRadius = dpToPx(16f)
            canvas.drawRoundRect(bgRectF, bgRadius, bgRadius, bgPaint)
            canvas.drawRoundRect(bgRectF, bgRadius, bgRadius, bgBorderPaint)
        }

        val s = state ?: return
        val visibleTiles = s.boardTiles.filter { tile ->
            (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) &&
                    tile.id !in flyingTileIds
        }.sortedBy { it.z }

        val curTime = System.currentTimeMillis()

        for (tile in visibleTiles) {
            val baseLeft = drawOffsetX + (tile.x - minX) * spacing * scale
            val baseTop = drawOffsetY + (tile.y - minY) * spacing * scale
            val currentTileSize = tileSize * scale

            var finalLeft = baseLeft
            if (tile.id in activeShakeIds && isShaking) {
                val elapsed = curTime - shakeStartTime
                val offset = sin(elapsed.toFloat() / 20f) * dpToPx(3f) * scale
                finalLeft += offset
            }

            drawTile(canvas, tile, finalLeft, baseTop, currentTileSize, s)
        }
    }

    /**
     * 执行单张卡牌像素层叠的 Canvas 原始绘制。
     *
     * 顺序包括：
     * 1. 卡牌底部立体阴影 (3dp 物理视差)。
     * 2. 卡牌主底板填充。
     * 3. 盲盒图案或普通皮肤花色图像填充。
     * 4. 细灰防重合粘连边线描边。
     * 5. 阻挡层蒙版覆盖。
     * 6. 对方大招施加的封印状态（灰蓝冰盖锁 + 右上角层数泡数字）。
     */
    private fun drawTile(
        canvas: Canvas,
        tile: Tile,
        left: Float,
        top: Float,
        size: Float,
        gameState: DuelViewState
    ) {
        tempRectF.set(left, top, left + size, top + size)
        val rect = tempRectF
        val cornerRadius = dpToPx(8f) * scale
        val isBlocked = tile.state == TileState.BLOCKED

        // 步骤 1：绘制柔和投影，对齐 TileView 中 TileCardBase 的 Modifier.shadow(4.dp) 软阴影
        val shadowOffset = dpToPx(2f) * scale
        tempShadowRectF.set(
            rect.left,
            rect.top + shadowOffset,
            rect.right,
            rect.bottom + shadowOffset
        )
        canvas.drawRoundRect(tempShadowRectF, cornerRadius, cornerRadius, cardShadowPaint)

        // 步骤 2：对卡牌本体、折线、图案、遮罩进行圆角路径裁剪，以物理裁剪代替直角边缘
        val saveCount = canvas.save()
        tempClipPath.rewind()
        tempClipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(tempClipPath)

        // 2.1 绘制卡牌亮白色圆角主体
        canvas.drawRect(rect, baseCardPaint)

        // 2.2 获取皮肤配置，绘制皮肤描边外框 + 四角 L 形折角装饰线
        // 与 TileCardBase 保持一致：外框和装饰线都在 clipPath 内部绘制，2dp 描边外侧一半被裁剪，
        // 实际可见约 1dp，避免对决棋盘卡牌边框比 TileView 粗一倍。
        val bColor = skinBorderColor
        val dColor = skinDecorColor
        val colors = if (bColor != null && dColor != null) {
            SkinColors(bColor, dColor)
        } else {
            getSkinColors(context, gameState.currentSkin)
        }
        val strokeWidthPx = dpToPx(2f) * scale

        // 2.2.1 外边框（borderColor）
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = strokeWidthPx
        borderPaint.color = colors.borderColor
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // 2.2.2 四角装饰折线（decorColor）
        borderPaint.color = colors.decorColor

        val decorLen = dpToPx(12f) * scale
        // 左上角
        canvas.drawLine(rect.left, rect.top, rect.left, rect.top + decorLen, borderPaint)
        canvas.drawLine(rect.left, rect.top, rect.left + decorLen, rect.top, borderPaint)
        // 右上角
        canvas.drawLine(rect.right - decorLen, rect.top, rect.right, rect.top, borderPaint)
        canvas.drawLine(rect.right, rect.top, rect.right, rect.top + decorLen, borderPaint)
        // 左下角
        canvas.drawLine(rect.left, rect.bottom - decorLen, rect.left, rect.bottom, borderPaint)
        canvas.drawLine(rect.left, rect.bottom, rect.left + decorLen, rect.bottom, borderPaint)
        // 右下角
        canvas.drawLine(rect.right - decorLen, rect.bottom, rect.right, rect.bottom, borderPaint)
        canvas.drawLine(rect.right, rect.bottom, rect.right, rect.bottom - decorLen, borderPaint)

        // 2.3 绘制内部图案（比例：普通牌缩进 5%，盲盒牌缩进 15% 以对齐 TileView）
        val isBlind =
            tile.isBlind && (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED)
        if (isBlind) {
            val innerPadding = size * 0.15f
            tempSrcRect.set(0, 0, tileBackBitmap.width, tileBackBitmap.height)
            tempDstRectF.set(
                rect.left + innerPadding,
                rect.top + innerPadding,
                rect.right - innerPadding,
                rect.bottom - innerPadding
            )
            canvas.drawBitmap(tileBackBitmap, tempSrcRect, tempDstRectF, cardPaint)
        } else {
            val bitmap = TileTextureLoader.getTileBitmap(context, gameState.currentSkin, tile.type) {
                triggerRender()
            }
            val innerPadding = size * 0.05f
            tempSrcRect.set(0, 0, bitmap.width, bitmap.height)
            tempDstRectF.set(
                rect.left + innerPadding,
                rect.top + innerPadding,
                rect.right - innerPadding,
                rect.bottom - innerPadding
            )
            canvas.drawBitmap(bitmap, tempSrcRect, tempDstRectF, cardPaint)
        }

        // 2.4 在 DEBUG 模式下，居中绘制红色/蓝色 ID 和 Z 层以对齐 TileView.kt
        if (BuildConfig.DEBUG) {
            debugPaint.color = if (isBlocked) Color.BLUE else Color.RED
            debugPaint.textSize = size * 0.18f
            val cleanId = tile.id.removePrefix("tile_")
            val yId = rect.top + size * 0.35f
            canvas.drawText(cleanId, rect.centerX(), yId, debugPaint)

            val yZ = rect.top + size * 0.65f
            canvas.drawText("z${tile.z}", rect.centerX(), yZ, debugPaint)
        }

        // 2.5 被遮挡锁定变暗
        if (isBlocked) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, maskPaint)
        }

        // 2.6 状态三：封印锁（对决模式未解锁的封印牌，有黄金边框、灰蓝背景与🔒黄色锁头）
        val isGateLocked = tile.sealedCount > 0
        if (isGateLocked) {
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gatePaint)
            gateBorderPaint.strokeWidth = dpToPx(1.5f) * scale
            canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gateBorderPaint)

            // 绘制居中锁头黄锁图标 🔒
            val lockText = "🔒"
            lockPaint.textSize = size * 0.45f
            val fontMetrics = lockPaint.fontMetrics
            val baselineY = rect.centerY() - (fontMetrics.top + fontMetrics.bottom) / 2f
            canvas.drawText(lockText, rect.centerX(), baselineY, lockPaint)
        } else if (tile.sealedCount > 0) {
            // 7. 状态四：已解除门控但仍存在剩余封印层数（绘制右上角蓝色圆形气泡）
            val bubbleRadius = dpToPx(7f) * scale
            val bubbleCenterX = rect.right - bubbleRadius / 2f
            val bubbleCenterY = rect.top + bubbleRadius / 2f

            canvas.drawCircle(bubbleCenterX, bubbleCenterY, bubbleRadius, bubblePaint)

            // 绘制数字层数文本
            val numText = tile.sealedCount.toString()
            textPaint.textSize = bubbleRadius * 1.3f
            val fontMetrics = textPaint.fontMetrics
            val baselineY = bubbleCenterY - (fontMetrics.top + fontMetrics.bottom) / 2f
            canvas.drawText(numText, bubbleCenterX, baselineY, textPaint)
        }

        canvas.restoreToCount(saveCount)
    }


    /**
     * 绘制迷雾法术的障眼黑色遮罩与手势探照灯镂空效果。
     *
     * 【探照灯镂空算法实现】
     * 1. 开启离屏渲染通道（saveLayer），在该独立缓冲画布上进行绘制。
     * 2. 首先用 `drawColor` 填充一层带有超高不透明度的接近全黑的面版遮罩（`#FA202020`）。
     * 3. 如果当前存在有效的手势擦除坐标点 `touchOffset`，利用 `RadialGradient` 创建一个径向渐变（渐变中心为手指点触坐标，边缘衰减半径为 65dp）。
     * 4. 设置混合排版过渡模式 `Xfermode` 为 **`PorterDuff.Mode.DST_OUT`**，然后在此模式下以径向渐变作为渲染器绘制圆形。
     *    `DST_OUT` 会将重叠区域的源像素黑罩物理擦除并镂空，形成一个柔和过渡的“探照灯探入可见区域”，使玩家能够在黑雾下短暂窥探其下的卡牌花色。
     * 5. 最后还原 Canvas 离屏栈（restoreToCount），实现极具科技与魔幻感的物理擦穿迷雾效果。
     */
    private fun drawFogEffect(canvas: Canvas) {
        val s = state ?: return
        if (!s.isFogActive) return

        val saveCount = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        try {
            // 步骤 1：填充全屏幕的暗黑色大招迷雾遮盖罩
            canvas.drawColor("#FA202020".toColorInt())

            // 步骤 2：应用 DST_OUT 擦除混合，绘制手指拖拽位置的镂空光圈
            val offset = touchOffset
            val grad = fogGradient
            if (offset != null && grad != null) {
                val radius = dpToPx(65f)
                canvas.save()
                canvas.translate(offset.x, offset.y)
                fogOverlayPaint.shader = grad
                fogOverlayPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
                canvas.drawCircle(0f, 0f, radius, fogOverlayPaint)
                fogOverlayPaint.xfermode = null
                fogOverlayPaint.shader = null
                canvas.restore()
            }
        } finally {
            canvas.restoreToCount(saveCount)
        }
    }

    /**
     * 触摸手势事件路由器。
     *
     * - **迷雾遮罩阻拦状态**：如果当前正被对方施放的迷雾诅咒致盲，本手势处理器将优先消耗拦截该 Action，
     *   仅负责将手指坐标 `event.x/y` 赋值给探照灯中心点并进行局部高频 `triggerRender` 擦雾重画，**不允许点按和消耗黑雾下方的卡牌**。
     *   手指离开（UP/CANCEL）时清空探照灯坐标，恢复全面黑雾。
     * - **正常竞技消除状态**：若没有迷雾，则从上层到底层逆向进行碰撞测试，保证最表层压制的卡牌优先被响应。
     *   计算卡牌屏幕绝对坐标存入缓存 Map，同时唤起 ViewModel 点击回调触发连消机制。
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val isFog = state?.isFogActive ?: false

        if (isFog) {
            when (event.action) {
                MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                    tempTouchPoint.set(event.x, event.y)
                    touchOffset = tempTouchPoint
                    triggerRender()
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    touchOffset = null
                    triggerRender()
                }
            }
            return true // 迷雾高频拦截，禁止点击卡牌
        }

        // 正常点击消除事件分发
        if (event.action == MotionEvent.ACTION_DOWN) {
            val clickX = event.x
            val clickY = event.y

            synchronized(renderLock) {
                val s = state ?: return false
                val visibleTiles = s.boardTiles.filter { tile ->
                    (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) &&
                            tile.id !in flyingTileIds
                }.sortedByDescending { it.z }

                for (tile in visibleTiles) {
                    val baseLeft = drawOffsetX + (tile.x - minX) * spacing * scale
                    val baseTop = drawOffsetY + (tile.y - minY) * spacing * scale
                    val currentTileSize = tileSize * scale

                    tempRectF.set(
                        baseLeft,
                        baseTop,
                        baseLeft + currentTileSize,
                        baseTop + currentTileSize
                    )
                    if (tempRectF.contains(clickX, clickY)) {
                        val rootPos = IntArray(2)
                        getLocationOnScreen(rootPos)

                        val globalX = rootPos[0] + baseLeft
                        val globalY = rootPos[1] + baseTop
                        tileGlobalPositions?.put(
                            tile.id,
                            androidx.compose.ui.geometry.Offset(globalX, globalY)
                        )

                        onTileClick?.invoke(tile)
                        return true
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        surfaceWidth = w.toFloat()
        surfaceHeight = h.toFloat()
        if (w > 0 && h > 0) {
            bgRectF.set(0f, 0f, surfaceWidth, surfaceHeight)
            bgPaint.shader = LinearGradient(
                0f, 0f, 0f, surfaceHeight,
                "#F5F5F5".toColorInt(),
                "#EEEEEE".toColorInt(),
                Shader.TileMode.CLAMP
            )
            val radius = dpToPx(65f)
            fogGradient = RadialGradient(
                0f, 0f, radius,
                intArrayOf(Color.TRANSPARENT, "#FA202020".toColorInt()),
                floatArrayOf(0f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        state?.let {
            updateData(it, flyingTileIds, onTileClick ?: {}, tileGlobalPositions ?: mutableMapOf())
        }
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }
}
