package com.example.sheeps.game.ui.components

import android.content.Context
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withTranslation
import com.example.sheeps.core.game.SkinColors
import com.example.sheeps.core.game.getSkinColors
import com.example.sheeps.data.model.Tile
import com.example.sheeps.game.BuildConfig
import com.example.sheeps.game.ui.utils.TileTextureLoader
import java.util.concurrent.ConcurrentHashMap


/**
 * 无尽模式（竖井叠塔）自绘 View。
 *
 * 【注】虽然类名保留为 EndlessBoardSurfaceView 历史遗留命名，但在实际实现中同样继承自标准 View。
 * 该类针对无尽模式中卡牌的下落机制进行了深度定制：
 * 1. 采用“列堆叠”逻辑（6 列通道）。
 * 2. 引入基于 LERP 算法的流畅下坠插值计算——当底部卡牌被点走时，上方的卡牌会平滑下滑填充空缺。
 * 3. 引入平滑上升滑入动效——新加入列底的卡牌会从底部边缘滑入，增强消除动感。
 */
class EndlessBoardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /** 存储当前 6 列中每列的卡牌数据列表（从井底往井口排序） */
    private var columns: List<List<Tile>> = emptyList()

    /** 当前玩家装备的主题卡牌皮肤名称 */
    private var currentSkin: String = "shuang"

    /** 警戒线卡牌行数限制，达到该高度表示临界危险（通常为 8 行） */
    private var deathRow: Int = 8

    /** 当前可见的下坠卡牌层数 */
    private var visibleLayers: Int = 3

    /** 卡牌列点按消除回调函数，传出（列索引, 卡牌ID） */
    private var onColumnClick: ((Int, String) -> Unit)? = null

    /** 正在执行飞行动画的卡牌 ID 集合，本 View 会静默它们 */
    private var flyingTileIds: Set<String> = emptySet()

    /** 坐标缓存 Map，为 Compose 的飞行动画提供精准起点 */
    private var tileGlobalPositions: MutableMap<String, androidx.compose.ui.geometry.Offset>? = null

    /** 当前皮肤边框/装饰色，由 Compose 层通过 [setSkinColors] 预解析传入，保证与 TileCardBase 一致 */
    private var skinBorderColor: Int? = null
    private var skinDecorColor: Int? = null

    /** 缓存所有卡牌位移动画实时位置的并发安全 Map，键为卡牌 ID，值为其实时的 currentY 坐标 */
    private val animStateMap = ConcurrentHashMap<String, TileAnimState>()

    // ==========================================
    // 绘图画笔声明
    // ==========================================

    /** 卡牌白色面板绘制画笔 */
    private val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 卡牌柔和投影画笔，对齐 TileView 中 TileCardBase 的 Modifier.shadow(4.dp) 软阴影 */
    private val cardShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = "#33000000".toColorInt()
        maskFilter = BlurMaskFilter(dpToPx(4f), BlurMaskFilter.Blur.NORMAL)
    }

    /** 卡牌边缘细描边线画笔 */
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = "#E0E0E0".toColorInt()
    }

    // ==========================================
    // 布局网格尺寸常量与计算变量
    // ==========================================

    /** 卡牌之间的横向/纵向网格微小空隙（4dp） */
    private val gap = dpToPx(4f)

    /** 卡牌边框圆角内补白（4dp） */
    private val pad = dpToPx(4f)

    /** 竖井卡牌的圆角正方形像素边长（默认 48dp） */
    private val tileSize = dpToPx(48f)

    /** 纵向单格卡牌累加像素高度步长（tileSize + gap） */
    private val verticalStep = tileSize + gap

    /** 竖井通道容器的计算像素高度 */
    private var totalColumnHeight = 0f

    /** 标识当前是否正在运行重绘刷新动画的 Looper 执行器 */
    private var isAnimating = false

    /** 线程同步锁，保证数据交互更新时的绝对安全 */
    private val renderLock = Any()

    /** 竖井底盘容器填充背景画笔 */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 竖井外边缘线画笔 */
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

    private val baseCardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    private val debugPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    private var lastOffsetX = -1f
    private var lastOffsetY = -1f
    private var lastTotalColumnHeight = -1f

    /**
     * 封印牌单张位移动画状态属性类。
     * 实时跟踪 currentY 并在每一帧通过 Lerp 差值逐渐向 targetY 逼近，实现平滑弹性动画。
     */
    private class TileAnimState(
        var currentY: Float,
        var targetY: Float
    )

    init {
        // 标准 View 初始化
    }

    /**
     * 更新无尽模式状态。
     * 接收最新的卡牌排列布局并计算得出每一张卡牌在通道中的最终 Y 轴物理像素目标高度。
     */
    fun updateData(
        columns: List<List<Tile>>,
        currentSkin: String,
        deathRow: Int,
        visibleLayers: Int,
        flyingTileIds: Set<String> = emptySet(),
        tileGlobalPositions: MutableMap<String, androidx.compose.ui.geometry.Offset> = mutableMapOf(),
        onColumnClick: (Int, String) -> Unit
    ) {
        synchronized(renderLock) {
            this.columns = columns
            this.currentSkin = currentSkin
            this.deathRow = deathRow
            this.visibleLayers = visibleLayers
            this.flyingTileIds = flyingTileIds
            this.tileGlobalPositions = tileGlobalPositions
            this.onColumnClick = onColumnClick

            val stackHeight = deathRow * tileSize + (deathRow - 1) * gap
            totalColumnHeight = pad + stackHeight + pad

            // If the board is completely empty, clear the cached anim states (e.g., on restart/init)
            if (columns.isEmpty() || columns.all { it.isEmpty() }) {
                animStateMap.clear()
            }

            // 1. 重算并更新所有卡牌的动画目标点
            columns.forEachIndexed { _, colList ->
                colList.forEachIndexed { tileIndex, tile ->
                    // 距底部第几张 (0 为最底层)
                    val p = colList.size - 1 - tileIndex
                    // 算出目标 Y 相对竖井顶部的位置
                    val targetY = totalColumnHeight - pad - tileSize - p * verticalStep

                    val existing = animStateMap[tile.id]
                    if (existing != null) {
                        existing.targetY = targetY
                    } else {
                        // 新加入列底的卡牌，将其 currentY 设在屏幕底部（或竖井最下方），平滑上升滑入
                        animStateMap[tile.id] = TileAnimState(
                            currentY = totalColumnHeight,
                            targetY = targetY
                        )
                    }
                }
            }
        }
        startAnimationLoop()
    }

    /**
     * 由 Compose 层预传入与 TileCardBase 一致的皮肤边框/装饰色。
     *
     * 由于 Canvas View 无法直接读取 Compose 的 MaterialTheme.colorScheme，统一在 Composable 中
     * 解析好颜色再传入，避免无尽棋盘边框与卡槽 TileView 色差。
     */
    fun setSkinColors(borderColor: Int, decorColor: Int) {
        synchronized(renderLock) {
            skinBorderColor = borderColor
            skinDecorColor = decorColor
        }
        triggerRender()
    }

    /**
     * 位移动画插值渲染循环。
     *
     * 使用极轻量级 Lerp (Linear Interpolation) 算法：`currentY += (targetY - currentY) * 0.25f`
     * 在每一帧通过 Handler 不断按需 invalidate，直到所有卡牌的位置误差均在 0.5 像素以内，实现物理顺滑的下坠填充。
     */
    private fun startAnimationLoop() {
        if (isAnimating) return
        isAnimating = true
        post(object : Runnable {
            override fun run() {
                var needsMoreFrames = false
                synchronized(renderLock) {
                    animStateMap.forEach { (id, state) ->
                        val diff = state.targetY - state.currentY
                        if (kotlin.math.abs(diff) > 0.5f) {
                            state.currentY += diff * 0.25f
                            needsMoreFrames = true
                        } else {
                            state.currentY = state.targetY
                        }
                    }
                }
                triggerRender()
                if (needsMoreFrames && isAnimating) {
                    postDelayed(this, 16) // ~60fps
                } else {
                    isAnimating = false
                }
            }
        })
    }

    /**
     * 发起 View 重画请求
     */
    private fun triggerRender() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(renderLock) {
            drawWellBoard(canvas)
        }
    }

    /**
     * 绘制无尽模式竖井容器底板与卡牌网格。
     *
     * 1. 计算总高度和在容器内的居中平移参数 offsetX / offsetY。
     * 2. 绘制渐变色竖井长条背景。
     * 3. 开启 Translate，循环遍历 6 列，调用 drawTile 依次渲染各卡牌。
     *    无尽模式由于没有遮挡锁定（BLOCKED）状态，所有卡牌的画笔 Alpha 均强制重置为 255（完全不透明）。
     */
    private fun drawWellBoard(canvas: Canvas) {
        val colCount = columns.size
        if (colCount == 0) return

        val totalBoardWidth = colCount * tileSize + (colCount - 1) * gap + pad * 2
        val offsetX = (width.toFloat() - totalBoardWidth) / 2f
        val offsetY = (height.toFloat() - (totalColumnHeight + pad * 2)) / 2f

        // 步骤 1：绘制竖井背景座
        bgRectF.set(offsetX, offsetY, offsetX + totalBoardWidth, offsetY + totalColumnHeight)
        val bgRadius = dpToPx(10f)
        
        var shader = bgPaint.shader
        if (shader == null || lastOffsetX != offsetX || lastOffsetY != offsetY || lastTotalColumnHeight != totalColumnHeight) {
            lastOffsetX = offsetX
            lastOffsetY = offsetY
            lastTotalColumnHeight = totalColumnHeight
            shader = LinearGradient(
                offsetX, offsetY, offsetX, offsetY + totalColumnHeight,
                "#F5F5F5".toColorInt(),
                "#EEEEEE".toColorInt(),
                Shader.TileMode.CLAMP
            )
            bgPaint.shader = shader
        }

        canvas.drawRoundRect(bgRectF, bgRadius, bgRadius, bgPaint)
        canvas.drawRoundRect(bgRectF, bgRadius, bgRadius, bgBorderPaint)

        // 步骤 2：对准偏移行，迭代卡牌绘制
        val saveCount = canvas.save()
        canvas.clipRect(offsetX, offsetY, offsetX + totalBoardWidth, offsetY + totalColumnHeight)
        canvas.withTranslation(offsetX, offsetY) {
            columns.forEachIndexed { colIndex, colList ->
                val colLeft = pad + colIndex * (tileSize + gap)

                colList.forEachIndexed { i, tile ->
                    if (tile.id !in flyingTileIds) {
                        val animState = animStateMap[tile.id]
                        val drawY = animState?.currentY ?: 0f

                        cardPaint.alpha = 255
                        borderPaint.alpha = 255

                        drawTile(this, tile, colLeft, drawY, tileSize)
                    }
                }
            }
        }
        canvas.restoreToCount(saveCount)
        cardPaint.alpha = 255
        borderPaint.alpha = 255
    }

    /**
     * 单张卡牌的 Canvas 详细渲染子程序（支持 3D 阴影、底板、皮肤图案及灰框）。
     */
    private fun drawTile(
        canvas: Canvas,
        tile: Tile,
        left: Float,
        top: Float,
        size: Float
    ) {
        tempRectF.set(left, top, left + size, top + size)
        val rect = tempRectF
        val cornerRadius = dpToPx(8f)

        // 1. 绘制柔和投影，对齐 TileView 中 TileCardBase 的 Modifier.shadow(4.dp) 软阴影
        val shadowOffset = dpToPx(2f)
        tempShadowRectF.set(
            rect.left,
            rect.top + shadowOffset,
            rect.right,
            rect.bottom + shadowOffset
        )
        canvas.drawRoundRect(tempShadowRectF, cornerRadius, cornerRadius, cardShadowPaint)

        // 2. 对卡牌本体、折线、图案进行圆角路径裁剪，以物理裁剪代替直角边缘
        val saveCount = canvas.save()
        tempClipPath.rewind()
        tempClipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.clipPath(tempClipPath)

        // 2.1 绘制卡牌底板
        canvas.drawRect(rect, baseCardPaint)

        // 2.2 获取皮肤配置，绘制皮肤描边外框 + 四角 L 形折角装饰线
        // 与 TileCardBase 保持一致：外框和装饰线都在 clipPath 内部绘制，2dp 描边外侧一半被裁剪，
        // 实际可见约 1dp，避免无尽棋盘卡牌边框比 TileView 粗一倍。
        val bColor = skinBorderColor
        val dColor = skinDecorColor
        val colors = if (bColor != null && dColor != null) {
            SkinColors(bColor, dColor)
        } else {
            getSkinColors(context, currentSkin)
        }
        val strokeWidthPx = dpToPx(2f)

        // 2.2.1 外边框（borderColor）
        borderPaint.style = Paint.Style.STROKE
        borderPaint.strokeWidth = strokeWidthPx
        borderPaint.color = colors.borderColor
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, borderPaint)

        // 2.2.2 四角装饰折线（decorColor）
        borderPaint.color = colors.decorColor

        val decorLen = dpToPx(12f)
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

        // 2.3 绘制卡牌图案（比例：普通牌缩进 5% 以对齐 TileView）
        val bitmap = TileTextureLoader.getTileBitmap(context, currentSkin, tile.type) {
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

        // 2.4 在 DEBUG 模式下，居中绘制红色 ID 和 Z 层以对齐 TileView.kt
        if (BuildConfig.DEBUG) {
            debugPaint.color = Color.RED
            debugPaint.textSize = size * 0.18f
            val cleanId = tile.id.removePrefix("tile_").removePrefix("endless_")
            val yId = rect.top + size * 0.35f
            canvas.drawText(cleanId, rect.centerX(), yId, debugPaint)

            val yZ = rect.top + size * 0.65f
            canvas.drawText("z${tile.z}", rect.centerX(), yZ, debugPaint)
        }

        canvas.restoreToCount(saveCount)
    }

    /**
     * 触摸事件处理器：
     * 横向碰撞检测（X轴定位到列），接着在 Y 轴上对列内卡牌进行倒序（从上往下）叠压检测，
     * 确保手指最先命中叠在表层的顶部卡牌，触发对应列 of 卡牌消除意图。
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val clickX = event.x
            val clickY = event.y

            synchronized(renderLock) {
                val colCount = columns.size
                if (colCount == 0) return false

                val totalBoardWidth = colCount * tileSize + (colCount - 1) * gap + pad * 2
                val offsetX = (width.toFloat() - totalBoardWidth) / 2f
                val offsetY = (height.toFloat() - (totalColumnHeight + pad * 2)) / 2f

                val localX = clickX - offsetX
                val localY = clickY - offsetY

                columns.forEachIndexed { colIndex, colList ->
                    val colLeft = pad + colIndex * (tileSize + gap)
                    if (localX >= colLeft && localX <= colLeft + tileSize) {
                        for (i in colList.indices.reversed()) {
                            val tile = colList[i]
                            val animState = animStateMap[tile.id]
                            val drawY = animState?.currentY ?: 0f
                            if (tile.id !in flyingTileIds && localY >= drawY && localY <= drawY + tileSize) {
                                val rootPos = IntArray(2)
                                getLocationOnScreen(rootPos)
                                val globalX = rootPos[0] + offsetX + colLeft
                                val globalY = rootPos[1] + offsetY + drawY
                                tileGlobalPositions?.put(
                                    tile.id,
                                    androidx.compose.ui.geometry.Offset(globalX, globalY)
                                )

                                onColumnClick?.invoke(colIndex, tile.id)
                                return true
                            }
                        }
                    }
                }
            }
        }
        return super.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        lastOffsetX = -1f
        lastOffsetY = -1f
        lastTotalColumnHeight = -1f
        triggerRender()
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

// Skin utilities moved to SkinUtils.kt
}
