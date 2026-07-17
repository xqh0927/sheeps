package com.example.sheeps.game.ui.components

import android.content.Context
import android.graphics.Bitmap
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
import androidx.compose.ui.geometry.Offset
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withClip
import com.example.sheeps.ui.R
import com.example.sheeps.core.game.SkinColors
import com.example.sheeps.core.game.getSkinColors
import com.example.sheeps.data.model.Tile
import com.example.sheeps.data.model.TileState
import com.example.sheeps.game.BuildConfig
import com.example.sheeps.game.state.GameViewState
import com.example.sheeps.game.ui.utils.TileTextureLoader
import kotlin.math.sin
import androidx.core.graphics.withSave

/**
 * 闯关模式卡牌棋盘自绘 View。
 *
 * 【注】虽然类名保留为 GameBoardSurfaceView 历史遗留命名，但在实际实现中改为了继承自标准 View。
 * 这使它能够在 Android 硬件加速（Hardware-Accelerated）的 Canvas 上执行高性能绘制，同时便于在 Compose 布局中流畅嵌套。
 * 该类通过直接操作 Canvas 绘制数以百计的堆叠卡牌，彻底剥离了 Compose 的组件树与繁重的重组（Recomposition）过程，
 * 将触摸点击响应延迟降到最低，实现极致流畅的操控质感。
 */
class GameBoardSurfaceView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        // ==========================================
        // 静态颜色常量（支持 Android Studio 颜色预览）
        // ==========================================
        /** 卡牌柔和投影颜色 */
        private val COLOR_CARD_SHADOW = "#33000000".toColorInt()
        /** DEBUG模式下被遮挡卡牌的高亮亮色 */
        private val COLOR_MASK_DEBUG_LIGHT = "#ffffff".toColorInt()
        /** 正常模式下被遮挡卡牌的半透明灰色蒙版 */
        private val COLOR_MASK_NORMAL = "#77ffffff".toColorInt()
        /** 黄金高亮发光特效边框颜色 */
        private val COLOR_HIGHLIGHT = "#FFD700".toColorInt()

        /** 封印剩余层数显示气泡背景蓝色 */
        private val COLOR_BUBBLE_BLUE = "#1976D2".toColorInt()

        /** 棋盘底座外围细边框颜色 */
        private val COLOR_BG_BORDER = "#CCCCCC".toColorInt()

        /** 锁关卡半透明深灰背景底色 */
        private val COLOR_GATE_BG = "#BB2C3E50".toColorInt()

        /** 锁关卡发光金黄色细边框 */
        private val COLOR_GATE_BORDER = "#F1C40F".toColorInt()

        /** 棋盘背景渐变起始浅灰色 */
        private val COLOR_BG_GRADIENT_START = "#F5F5F5".toColorInt()

        /** 棋盘背景渐变结束深灰色 */
        private val COLOR_BG_GRADIENT_END = "#EEEEEE".toColorInt()

        /** 兜底锁头主体圆角矩形金黄色 */
        private val COLOR_LOCK_BODY = "#F1C40F".toColorInt()

        /** 兜底锁头锁孔底色 */
        private val COLOR_LOCK_HOLE = "#2C3E50".toColorInt()
        /** 兜底牌背深紫色背景 */
        private val COLOR_TILE_BACK_BG = "#5E35B1".toColorInt()
        /** 兜底牌背斜纹浅紫色 */
        private val COLOR_TILE_BACK_STRIPE = "#7E57C2".toColorInt()
    }

    /** 核心游戏状态快照，用于驱动棋盘上所有卡牌的位置、类型、高亮及解密锁显示 */
    private var state: GameViewState? = null

    /** 正在执行飞行动画的卡牌 ID 集合，处于飞行动画中的卡牌将被本地静默隐藏以免产生叠影 */
    private var flyingTileIds: Set<String> = emptySet()

    /** 卡牌点击动作事件监听器，用于将触摸命中的卡牌分发到 ViewModel 触发消除动作 */
    private var onTileClick: ((Tile) -> Unit)? = null

    /** 卡牌在屏幕上的物理坐标缓存 Map。用于供 Compose 侧识别飞行动画的起始像素坐标 */
    private var tileGlobalPositions: MutableMap<String, Offset>? = null

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
        color = COLOR_CARD_SHADOW
        maskFilter = BlurMaskFilter(dpToPx(4f), BlurMaskFilter.Blur.NORMAL)
    }

    /** 卡牌外侧极细的圆角描边边框画笔 */
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
    }

    /** 卡牌锁头、右上角气泡内数字等文本专用的字体与画笔 */
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.DEFAULT_BOLD
    }

    /** 处于被遮挡（BLOCKED）状态的卡牌在其上覆盖的半透明变暗蒙版画笔，DEBUG 模式下为半透明白以供诊断 */
    private val maskPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = if (BuildConfig.DEBUG) COLOR_MASK_DEBUG_LIGHT else COLOR_MASK_NORMAL
    }

    /** 黄金高亮边框画笔，用于绘制提示道具选中时的发光特效 */
    private val highlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(2f)
        color = COLOR_HIGHLIGHT
        maskFilter = BlurMaskFilter(dpToPx(2f), BlurMaskFilter.Blur.NORMAL)
    }

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

    /** 屏幕自适应缩放因子，用于根据当前屏幕宽度等比例缩小卡牌以完美装入棋盘 */
    private var scale = 1f

    /** 居中计算后的水平偏移像素，确保卡牌在容器正中央显示 */
    private var drawOffsetX = 0f

    /** 居中计算后的垂直偏移像素，确保卡牌在容器正中央显示 */
    private var drawOffsetY = 0f

    /** 棋盘组件当前的宽和高像素大小 */
    private var surfaceWidth = 0f
    private var surfaceHeight = 0f

    // ==========================================
    // 阻挡/抖动交互动画状态变量
    // ==========================================

    /** 当前处于由于非法操作（例如点击被锁定遮挡的卡牌）而需要执行抖动动画的卡牌 ID 列表 */
    private var activeShakeIds = emptySet<String>()

    /** 抖动动画开始的时间戳（以系统当前毫秒为准） */
    private var shakeStartTime = 0L

    /** 抖动动画的固定生命周期时间（毫秒） */
    private val shakeDuration = 500L

    /** 标识当前是否正在后台运行抖动的重绘定时器轮询 */
    private var isShaking = false

    /** 封印剩余层数显示气泡的背景蓝色画笔 */
    private val bubblePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_BUBBLE_BLUE
    }

    /** 缓存的封印小锁头矢量图素材。如果无法从 XML 中读取（例如某些极端设备），则使用自绘逻辑生成 fallback 小锁 */
    private val lockBitmap: Bitmap by lazy {
        val lockSizePx = dpToPx(32f).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(lockSizePx, lockSizePx)
        val canvas = Canvas(bitmap)
        val drawable = ContextCompat.getDrawable(context, R.drawable.ic_seal_lock)
        if (drawable != null) {
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } else {
            drawFallbackLock(canvas, lockSizePx)
        }
        bitmap
    }

    private val tileBackBitmap: Bitmap by lazy {
        // 优先使用 app 自有牌背图，若资源缺失则回退到 Canvas 自绘的牌背，
        // 保证盲盒牌与普牌（花色）在视觉上始终可区分。
        val backSizePx = dpToPx(48f).toInt().coerceAtLeast(1)
        val bitmap = createBitmap(backSizePx, backSizePx)
        val canvas = Canvas(bitmap)
        val drawable = ContextCompat.getDrawable(context, R.drawable.tile_back)
        if (drawable != null) {
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
        } else {
            drawFallbackTileBack(canvas, backSizePx)
        }
        bitmap
    }

    /** 数据同步锁，防止非 UI 线程更新状态数据（如协程回调）与 UI 线程 onDraw 渲染之间发生读写冲突 */
    private val renderLock = Any()

    /** 棋盘背景填充画笔 */
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    /** 棋盘底座外围细边框画笔 */
    private val bgBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = dpToPx(1f)
        color = COLOR_BG_BORDER
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

    private val gatePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = COLOR_GATE_BG
    }

    private val gateBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = COLOR_GATE_BORDER
    }

    init {
        // 标准 View 构造期初始化，当前采用硬件加速渲染
    }

    /**
     * 更新状态数据并触发布局计算。
     *
     * 该方法由 Compose 的 AndroidView 包装闭包在高频重组中喂入最新数据。
     * 当数据状态更新时，在 `renderLock` 保护下重算缩放比例和偏移量，随后调用 `invalidate` 重绘界面。
     *
     * @param state 核心游戏状态快照
     * @param flyingTileIds 当前处于飞行动画中的卡牌 ID 集合，本 View 会静默它们
     * @param onTileClick 点击交互回调函数
     * @param tileGlobalPositions 绝对屏幕像素位置映射 Map
     */
    fun updateData(
        state: GameViewState,
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

            // 1. 监测最新被锁定的卡牌是否发生变化，若是则启动抖动动效
            val newShakes = state.shakingTileIds
            if (newShakes.isNotEmpty() && newShakes != oldShakes) {
                startShakeAnimation(newShakes)
            }

            // 2. 多分辨率自适应布局计算：根据当前卡牌在网格中的最大与最小边界，动态计算卡牌的缩放尺度与居中偏移，
            //    从而保证无论卡牌数量多寡、排版多宽，都能完美居中显示在棋盘中间而不会溢出屏幕。
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
     * 由于 Canvas View 无法直接读取 Compose 的 MaterialTheme.colorScheme，而 Android XML 主题
     * 的 colorPrimary/colorSecondary 又与 Compose 主题不一致，因此统一在 Composable 中解析
     * 好颜色再传入，避免棋盘边框与卡槽 TileView 色差。
     */
    fun setSkinColors(borderColor: Int, decorColor: Int) {
        synchronized(renderLock) {
            skinBorderColor = borderColor
            skinDecorColor = decorColor
        }
        triggerRender()
    }

    /**
     * 针对无法点击的阻挡卡牌触发正弦波物理抖动特效。
     *
     * 利用 Handler 消息队列，在 500ms 内以约 60fps 频率进行周期性 `invalidate`，
     * 并基于当前正弦相位偏移绘制卡牌的水平像素位置，向用户展示明显的交互阻挡警示。
     *
     * @param blockerIds 需要发生抖动的卡牌 ID 集合
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
                        postDelayed(this, 16) // 约 60fps 执行循环定时重画
                    }
                    triggerRender()
                }
            })
        }
    }

    /**
     * 触发 Canvas 请求重绘。
     * 内部包装 invalidate 以利于将来重构优化。
     */
    private fun triggerRender() {
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        synchronized(renderLock) {
            drawBoard(canvas)
        }
    }

    /**
     * 核心渲染主流程：绘制棋盘卡牌矩阵。
     *
     * 1. 首先绘制棋盘渐变底座面板。
     * 2. 然后筛选出当前正常存在且不在飞行动画中的所有卡牌。
     * 3. **核心物理分层排序**：基于卡牌的 Z 轴高度由小到大排序。
     *    这确保了在硬件绘制时，下层卡牌先画、上层卡牌后画，完美还原出卡牌的前后压制重叠立体关系。
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
        }.sortedBy { it.z } // 保证 Z 轴小在下，大在上（正确叠层）

        val curTime = System.currentTimeMillis()

        for (tile in visibleTiles) {
            // 计算渲染像素坐标
            val baseLeft = drawOffsetX + (tile.x - minX) * spacing * scale
            val baseTop = drawOffsetY + (tile.y - minY) * spacing * scale
            val currentTileSize = tileSize * scale

            // 应用抖动偏移
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
     * 对单个卡牌卡面进行 Canvas 精细组合叠加自绘。
     *
     * 绘制执行顺序遵循现实世界卡牌层叠物理原理：
     * 1. 绘制底层泥色阴影（向右下偏移 1.5dp，烘托 Z 轴厚度空间感）
     * 2. 绘制卡牌主白亮圆角底板。
     * 3. 绘制卡牌图案层（若是盲盒牌且未消则画盲盒专属牌背，若是普通牌则从皮肤管理器异步读入花色贴图进行拉伸贴合）。
     * 4. 描绘卡牌边缘浅灰细线，增强视觉硬朗感。
     * 5. 若当前卡牌是道具高亮提示对象，在其最外围画一层发光的黄金描边。
     * 6. 若该卡牌被上层阻挡（BLOCKED），覆盖一层暗色半透明置灰图层。
     * 7. 若卡牌带封印层数且处于未自动解密状态（isGateLocked），在其上叠加灰蓝冰晶滤镜与黄色描边，并在中心点渲染小锁头。
     * 8. 若当前卡牌已被门控解密解锁，但 `sealedCount > 0` 仍含有层数，在右上角绘制醒目的层数气泡数字（仅在 debug 时展示以供直观校正）。
     */
    private fun drawTile(
        canvas: Canvas,
        tile: Tile,
        left: Float,
        top: Float,
        size: Float,
        gameState: GameViewState
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
        tempClipPath.rewind()
        tempClipPath.addRoundRect(rect, cornerRadius, cornerRadius, Path.Direction.CW)
        canvas.withClip(tempClipPath) {

            // 2.1 绘制卡牌亮白色圆角主体
            canvas.drawRect(rect, baseCardPaint)

            // 步骤 2.2 获取皮肤配置，绘制皮肤描边外框 + 四角 L 形折角装饰线
            // 注意：与 TileCardBase 保持一致，外框和装饰线都在 clipPath 内部绘制；
            // 2dp 描边居中后外侧一半会被裁剪，实际可见约 1dp，避免棋盘卡牌边框比 TileView 粗一倍。
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
            canvas.drawLine(
                rect.right - decorLen,
                rect.bottom,
                rect.right,
                rect.bottom,
                borderPaint
            )
            canvas.drawLine(
                rect.right,
                rect.bottom,
                rect.right,
                rect.bottom - decorLen,
                borderPaint
            )

            // 2.3 绘制花色/图案内容（比例：普通牌缩进 5%，盲盒牌缩进 15% 以对齐 TileView）
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
                val bitmap =
                    TileTextureLoader.getTileBitmap(context, gameState.currentSkin, tile.type) {
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

            // 2.5 绘制被道具提示选中时的闪烁黄金边框
            if (gameState.highlightedTileIds.contains(tile.id)) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, highlightPaint)
            }

            // 2.6 绘制被遮挡无法交互卡牌的置灰变暗遮罩
            if (isBlocked) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, maskPaint)
            }

            // 2.7 绘制未解封状态下的冰晶蓝封印门控锁与锁头 icon
            val isGateLocked = tile.sealedCount > 0 && tile.id !in gameState.sealedUnlockedIds
            if (isGateLocked) {
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gatePaint)
                gateBorderPaint.strokeWidth = dpToPx(1.5f) * scale
                canvas.drawRoundRect(rect, cornerRadius, cornerRadius, gateBorderPaint)

                val lockSize = size * 0.4f
                val lLeft = rect.centerX() - lockSize / 2f
                val lTop = rect.centerY() - lockSize / 2f
                tempDstRectF.set(lLeft, lTop, lLeft + lockSize, lTop + lockSize)
                canvas.drawBitmap(lockBitmap, null, tempDstRectF, cardPaint)
            }

        }

        // 8. 状态四：封印层数蓝色小气泡（仅在 sealedCount > 0 时显示在卡牌右上角）
        if (tile.sealedCount > 0 && BuildConfig.DEBUG) {
            val bubbleRadius = size * 0.18f
            val bCx = rect.right - bubbleRadius / 1.5f
            val bCy = rect.top + bubbleRadius / 1.5f

            // 绘制蓝色小圆圈气泡
            canvas.drawCircle(bCx, bCy, bubbleRadius, bubblePaint)

            // 气泡内边框
            borderPaint.color = Color.WHITE
            canvas.drawCircle(bCx, bCy, bubbleRadius, borderPaint)

            // 绘制气泡中的数字
            textPaint.color = Color.WHITE
            textPaint.textSize = bubbleRadius * 1.3f
            // 字体垂直居中修正偏移
            val fontMetrics = textPaint.fontMetrics
            val baseline = bCy - (fontMetrics.ascent + fontMetrics.descent) / 2f
            canvas.drawText(tile.sealedCount.toString(), bCx, baseline, textPaint)
        }
    }

    /**
     * 手势触摸按下判定，通过反向位置换算，获取点击对应的 Tile卡牌。
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val clickX = event.x
            val clickY = event.y

            synchronized(renderLock) {
                val s = state ?: return false
                val visibleTiles = s.boardTiles.filter { tile ->
                    (tile.state == TileState.NORMAL || tile.state == TileState.BLOCKED) &&
                            tile.id !in flyingTileIds
                }.sortedByDescending { it.z } // 从上往下判定，最顶层的优先命中点击！

                for (tile in visibleTiles) {
                    val baseLeft = drawOffsetX + (tile.x - minX) * spacing * scale
                    val baseTop = drawOffsetY + (tile.y - minY) * spacing * scale
                    val currentTileSize = tileSize * scale

                    val rect = RectF(
                        baseLeft,
                        baseTop,
                        baseLeft + currentTileSize,
                        baseTop + currentTileSize
                    )
                    if (rect.contains(clickX, clickY)) {
                        // 命中卡牌，算出卡牌的屏幕绝对物理坐标供外部 Compose 飞行动画起点使用
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
                COLOR_BG_GRADIENT_START,
                COLOR_BG_GRADIENT_END,
                Shader.TileMode.CLAMP
            )
        }
        state?.let {
            updateData(it, flyingTileIds, onTileClick ?: {}, tileGlobalPositions ?: mutableMapOf())
        }
    }

    /**
     * 矢量锁图标资源缺失时的兜底：用 Canvas 自绘一个醒目黄色的小锁头
     * （锁梁 + 锁身 + 冰晶蓝锁孔），保证任何机型上封印牌中央都有可见的锁图标。
     */
    private fun drawFallbackLock(canvas: Canvas, size: Int) {
        val s = size.toFloat()
        val lockColor = COLOR_LOCK_BODY
        // 锁身（圆角矩形）
        val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lockColor
            style = Paint.Style.FILL
        }
        val bodyLeft = s * 0.2f
        val bodyRight = s * 0.8f
        val bodyTop = s * 0.42f
        val bodyBottom = s * 0.92f
        val bodyRadius = s * 0.1f
        canvas.drawRoundRect(
            RectF(bodyLeft, bodyTop, bodyRight, bodyBottom),
            bodyRadius, bodyRadius, bodyPaint
        )
        // 锁梁（半圆弧）
        val shacklePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = lockColor
            style = Paint.Style.STROKE
            strokeWidth = s * 0.16f
        }
        val shackleRect = RectF(
            bodyLeft + s * 0.16f,
            s * 0.16f,
            bodyRight - s * 0.16f,
            bodyTop + s * 0.1f
        )
        canvas.drawArc(shackleRect, 180f, 180f, false, shacklePaint)
        // 锁孔（冰晶蓝，与封印背景呼应）
        val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_LOCK_HOLE
            style = Paint.Style.FILL
        }
        val holeR = s * 0.07f
        canvas.drawCircle(s / 2f, bodyTop + (bodyBottom - bodyTop) * 0.4f, holeR, holePaint)
    }

    /**
     * 牌背图资源缺失时的兜底：绘制一个明显区别于普牌花色的牌背
     * （深紫圆角矩形 + 斜纹 + 中央 '?'），保证盲盒牌始终可见。
     */
    private fun drawFallbackTileBack(canvas: Canvas, size: Int) {
        val s = size.toFloat()
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TILE_BACK_BG
            style = Paint.Style.FILL
        }
        val radius = s * 0.12f
        canvas.drawRoundRect(RectF(0f, 0f, s, s), radius, radius, bgPaint)
        // 斜纹装饰
        val stripePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = COLOR_TILE_BACK_STRIPE
            style = Paint.Style.STROKE
            strokeWidth = s * 0.06f
        }
        val step = s * 0.22f
        var x = -s
        while (x < s) {
            canvas.drawLine(x, s, x + s, 0f, stripePaint)
            x += step
        }
        // 中央 '?'
        val qPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            textSize = s * 0.5f
            typeface = Typeface.DEFAULT_BOLD
        }
        val fm = qPaint.fontMetrics
        val baseline = s / 2f - (fm.ascent + fm.descent) / 2f
        canvas.drawText("?", s / 2f, baseline, qPaint)
    }

    private fun dpToPx(dp: Float): Float {
        return dp * context.resources.displayMetrics.density
    }

}
