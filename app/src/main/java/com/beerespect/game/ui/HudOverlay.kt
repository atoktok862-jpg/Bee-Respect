package com.beerespect.game.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.beerespect.game.GameEngine
import com.beerespect.game.GameState
import com.beerespect.game.HudState
import com.beerespect.game.data.SaveManager
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

class HudOverlay @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Actions {
        fun onStart()
        fun onResumeGame()
        fun onPauseGame()
        fun onMenu()
        fun onShop()
        fun onNextWave()
        fun onDash()
        fun onUlt()
        fun onBuy(kind: Int)
        fun onSkin(index: Int)
        fun onToggleSound()
        fun onRestart()
    }

    var engine: GameEngine? = null
    var save: SaveManager? = null
    var actions: Actions? = null
    var hud = HudState()
        set(value) { field = value; postInvalidateOnAnimation() }

    private val panelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#AA1A1308") }
    private val panelStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; strokeWidth = 3f; color = Color.parseColor("#FFC48A22") }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 34f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    private val small = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFF4D8"); textSize = 24f }
    private val title = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFE28A"); textSize = 64f; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    private val buttonPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val buttonText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE; textSize = 30f; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }
    private val joyBase = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#55FFFFFF"); style = Paint.Style.FILL }
    private val joyKnob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#AAFFD24A") }
    private val hpBack = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#55333333") }
    private val hpFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val warnPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.parseColor("#FFFFE08A"); textSize = 40f; textAlign = Paint.Align.CENTER; typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) }

    private var joyPointer = -1
    private var joyCx = 0f; private var joyCy = 0f; private var joyR = 0f
    private var joyKx = 0f; private var joyKy = 0f

    private val fireBtn = RectF()
    private val dashBtn = RectF()
    private val ultBtn = RectF()
    private val pauseBtn = RectF()

    private val menuButtons = mutableListOf<Pair<RectF, () -> Unit>>()
    private val shopButtons = mutableListOf<Pair<RectF, () -> Unit>>()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val e = engine ?: return
        val state = e.state
        if (state == GameState.PLAYING || state == GameState.PAUSED) drawHud(canvas)
        when (state) {
            GameState.MENU -> drawMenu(canvas)
            GameState.SHOP -> { drawHud(canvas); drawShop(canvas) }
            GameState.PAUSED -> drawPause(canvas)
            GameState.GAMEOVER -> { drawHud(canvas); drawGameOver(canvas) }
            else -> {}
        }
    }

    private fun drawHud(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        val e = engine ?: return
        joyCx = w * 0.14f; joyCy = h * 0.75f; joyR = min(w, h) * 0.13f
        canvas.drawCircle(joyCx, joyCy, joyR, joyBase)
        canvas.drawCircle(joyKx.takeIf { joyPointer != -1 } ?: joyCx, joyKy.takeIf { joyPointer != -1 } ?: joyCy, joyR * 0.38f, joyKnob)

        val bSize = min(w, h) * 0.14f
        fireBtn.set(w - bSize * 1.35f, h - bSize * 1.65f, w - bSize * 0.15f, h - bSize * 0.45f)
        dashBtn.set(w - bSize * 2.65f, h - bSize * 2.35f, w - bSize * 1.55f, h - bSize * 1.25f)
        ultBtn.set(w - bSize * 2.75f, h - bSize * 1.05f, w - bSize * 1.65f, h - bSize * 0.05f)
        drawRoundButton(canvas, fireBtn, "ОГОНЬ\nЖАЛО", "#FFE7A028", "#FF8A4A00")
        val dashReady = hud.dashReady.coerceIn(0f, 1f)
        drawRoundButton(canvas, dashBtn, "РЫВОК\n+ ЩИТ", if (dashReady >= 1f) "#FF76C9FF" else "#FF52708A", "#FF1D4E6E")
        val ultReady = hud.ult >= 100f
        drawRoundButton(canvas, ultBtn, "УЛЬТА\nГНЕВ РОЯ", if (ultReady) "#FFFF8A3D" else "#FF855C3A", "#FF6A2600")

        val top = RectF(w * 0.22f, 12f, w * 0.78f, 118f)
        drawPanel(canvas, top)
        text.textSize = 30f; text.textAlign = Paint.Align.LEFT
        canvas.drawText("Волна ${hud.wave}${if (hud.survival) " (Выживание)" else ""}", top.left + 20f, top.top + 38f, text)
        small.textSize = 24f
        canvas.drawText("Счёт: ${hud.score}   Мёд: ${hud.honey}   Враги: ${hud.enemies}", top.left + 20f, top.top + 72f, small)
        drawBar(canvas, top.left + 20f, top.top + 84f, top.width() * 0.44f, 18f, hud.hiveHp / max(1f, hud.hiveMax), "#FF71D45A", "Улей")
        drawBar(canvas, top.left + top.width() * 0.52f, top.top + 84f, top.width() * 0.2f, 18f, hud.playerHp / 100f, "#FFFFD24A", "Пчела")
        drawBar(canvas, top.left + top.width() * 0.74f, top.top + 84f, top.width() * 0.22f, 18f, hud.ult / 100f, "#FFFF7A2E", "Ульта")

        pauseBtn.set(w - 110f, 14f, w - 14f, 86f)
        drawRoundButton(canvas, pauseBtn, "II", "#FFB97A2A", "#FF6E3D00")
        if (hud.message.isNotEmpty()) {
            canvas.drawText(hud.message, w / 2f, h * 0.22f, warnPaint)
        }
    }

    private fun drawBar(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, ratio: Float, color: String, label: String) {
        val r = RectF(x, y, x + w, y + h)
        canvas.drawRoundRect(r, 8f, 8f, hpBack)
        hpFill.color = Color.parseColor(color)
        val fill = RectF(x, y, x + w * ratio.coerceIn(0f, 1f), y + h)
        canvas.drawRoundRect(fill, 8f, 8f, hpFill)
        small.textSize = 18f; small.color = Color.WHITE
        canvas.drawText(label, x, y - 4f, small)
        small.color = Color.parseColor("#FFF4D8")
    }

    private fun drawMenu(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.parseColor("#33000000"))
        val panel = RectF(w * 0.18f, h * 0.1f, w * 0.82f, h * 0.9f)
        drawPanel(canvas, panel)
        title.textAlign = Paint.Align.CENTER; title.textSize = min(w, h) * 0.075f
        canvas.drawText("Кто пчёлок уважает", w / 2f, panel.top + 82f, title)
        small.textAlign = Paint.Align.CENTER; small.textSize = 28f
        canvas.drawText("3D-оборона Золотого Улья: защищай улей, собирай пыльцу, прокачивай рой!", w / 2f, panel.top + 126f, small)
        val s = save ?: return
        small.textSize = 24f
        canvas.drawText("Рекорд: ${s.bestScore} очков   Лучшая волна: ${s.bestWave}   Выживание: ${s.bestSurvival}", w / 2f, panel.top + 162f, small)

        menuButtons.clear()
        val bw = panel.width() * 0.34f; val bh = 74f
        val cx = w / 2f
        addButton(menuButtons, RectF(cx - bw - 20f, panel.top + 210f, cx - 20f, panel.top + 210f + bh), "Начать игру", "#FFFFC629", "#FF8A4A00") { actions?.onStart() }
        addButton(menuButtons, RectF(cx + 20f, panel.top + 210f, cx + bw + 20f, panel.top + 210f + bh), "Магазин / Скины", "#FF7BCBFF", "#FF1D4E6E") { actions?.onShop() }
        addButton(menuButtons, RectF(cx - bw - 20f, panel.top + 305f, cx - 20f, panel.top + 305f + bh), if (s.soundEnabled) "Звук: ВКЛ" else "Звук: ВЫКЛ", "#FFB8E986", "#FF3F6A1E") { actions?.onToggleSound() }
        addButton(menuButtons, RectF(cx + 20f, panel.top + 305f, cx + bw + 20f, panel.top + 305f + bh), "Таблица рекордов", "#FFF2B6FF", "#FF6B2D78") {}
        for ((r, _) in menuButtons) drawRoundButton(canvas, r, buttonLabel(r), buttonColor(r), buttonStroke(r))

        val rec = s.records()
        text.textAlign = Paint.Align.LEFT; text.textSize = 30f
        canvas.drawText("Лучшие вылеты:", panel.left + 40f, panel.top + 430f, text)
        small.textAlign = Paint.Align.LEFT; small.textSize = 24f
        if (rec.isEmpty()) {
            canvas.drawText("Пока нет рекордов. Первый вылет - ваш!", panel.left + 40f, panel.top + 470f, small)
        } else {
            rec.forEachIndexed { i, r ->
                canvas.drawText("${i + 1}. ${r.score} очков, волна ${r.wave}, ${r.date}", panel.left + 40f, panel.top + 470f + i * 34f, small)
            }
        }
    }

    private val buttonLabels = HashMap<Int, String>()
    private val buttonColors = HashMap<Int, String>()
    private val buttonStrokes = HashMap<Int, String>()
    private fun addButton(list: MutableList<Pair<RectF, () -> Unit>>, rect: RectF, label: String, color: String, stroke: String, action: () -> Unit) {
        val r = RectF(rect)
        val key = System.identityHashCode(r)
        buttonLabels[key] = label
        buttonColors[key] = color
        buttonStrokes[key] = stroke
        list.add(r to action)
    }
    private fun buttonLabel(r: RectF) = buttonLabels[System.identityHashCode(r)] ?: ""
    private fun buttonColor(r: RectF) = buttonColors[System.identityHashCode(r)] ?: "#FFFFC629"
    private fun buttonStroke(r: RectF) = buttonStrokes[System.identityHashCode(r)] ?: "#FF8A4A00"

    private fun drawShop(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.parseColor("#77000000"))
        val panel = RectF(w * 0.08f, h * 0.08f, w * 0.92f, h * 0.92f)
        drawPanel(canvas, panel)
        title.textAlign = Paint.Align.CENTER; title.textSize = 54f
        canvas.drawText("Магазин улучшений и скинов", w / 2f, panel.top + 62f, title)
        val s = save ?: return
        small.textAlign = Paint.Align.CENTER; small.textSize = 26f
        canvas.drawText("Мёд: ${s.honey}   Всего собрано: ${s.lifetimeHoney}", w / 2f, panel.top + 96f, small)
        shopButtons.clear()
        val names = arrayOf("Размер Роя", "Мощь Жала", "Турбо-Крылья", "Медовая Броня Улья")
        val desc = arrayOf(
            "Пчёлы-помощницы: ${1 + s.swarm}/15",
            "Скорострельность и выстрелы",
            "Скорость и откат рывка",
            "Прочность и реген улья"
        )
        val bw = panel.width() * 0.22f; val bh = 150f
        val startX = panel.left + 30f
        for (i in 0..3) {
            val x = startX + i * (bw + 18f)
            val r = RectF(x, panel.top + 130f, x + bw, panel.top + 130f + bh)
            drawPanel(canvas, r)
            text.textAlign = Paint.Align.CENTER; text.textSize = 26f
            canvas.drawText(names[i], r.centerX(), r.top + 34f, text)
            small.textAlign = Paint.Align.CENTER; small.textSize = 22f
            canvas.drawText(desc[i], r.centerX(), r.top + 66f, small)
            val lvl = s.upgradeLevel(i)
            val max = s.upgradeMax(i)
            canvas.drawText("Уровень $lvl/$max", r.centerX(), r.top + 94f, small)
            val cost = s.upgradeCost(i)
            val btn = RectF(r.left + 14f, r.bottom - 52f, r.right - 14f, r.bottom - 10f)
            val can = lvl < max && s.honey >= cost
            val label = if (lvl >= max) "Максимум" else "Купить: $cost"
            addButton(shopButtons, btn, label, if (can) "#FFFFC629" else "#FF8A7A62", "#FF6A4A00") { actions?.onBuy(i) }
            drawRoundButton(canvas, btn, label, if (can) "#FFFFC629" else "#FF8A7A62", "#FF6A4A00")
        }

        text.textAlign = Paint.Align.LEFT; text.textSize = 34f
        canvas.drawText("Скины героя", panel.left + 34f, panel.top + 340f, text)
        val skinNames = arrayOf("Классическая Пчёлка", "Кибер-Пчела", "Королевский Шершень", "Огненная Оса")
        val sw = panel.width() * 0.22f; val sh = 110f
        for (i in 0..3) {
            val x = panel.left + 30f + i * (sw + 18f)
            val r = RectF(x, panel.top + 360f, x + sw, panel.top + 360f + sh)
            val unlocked = s.skinUnlocked(i)
            val selected = s.selectedSkin == i
            drawPanel(canvas, r)
            if (selected) {
                panelStroke.color = Color.parseColor("#FF76FF8A"); canvas.drawRoundRect(r, 18f, 18f, panelStroke); panelStroke.color = Color.parseColor("#FFC48A22")
            }
            text.textAlign = Paint.Align.CENTER; text.textSize = 24f
            canvas.drawText(skinNames[i], r.centerX(), r.top + 36f, text)
            small.textAlign = Paint.Align.CENTER; small.textSize = 20f
            canvas.drawText(if (unlocked) "Открыт" else s.skinRequirement(i), r.centerX(), r.top + 64f, small)
            val btn = RectF(r.left + 12f, r.bottom - 42f, r.right - 12f, r.bottom - 8f)
            val label = if (selected) "Выбран" else if (unlocked) "Выбрать" else "Закрыт"
            addButton(shopButtons, btn, label, if (unlocked) "#FF8BE3FF" else "#FF777777", "#FF245E7A") { if (unlocked) actions?.onSkin(i) }
            drawRoundButton(canvas, btn, label, if (unlocked) "#FF8BE3FF" else "#FF777777", "#FF245E7A")
        }

        val next = RectF(panel.centerX() - 180f, panel.bottom - 82f, panel.centerX() + 180f, panel.bottom - 18f)
        val canResume = (engine?.wave ?: 0) > 0
        val label = if (canResume) "В бой!" else "Начать игру"
        addButton(shopButtons, next, label, "#FF90E26A", "#FF3A741A") {
            val e = engine
            if (e != null && e.wave == 0) actions?.onStart() else actions?.onNextWave()
        }
        drawRoundButton(canvas, next, label, "#FF90E26A", "#FF3A741A")
    }

    private fun drawPause(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.parseColor("#88000000"))
        val panel = RectF(w * 0.3f, h * 0.25f, w * 0.7f, h * 0.72f)
        drawPanel(canvas, panel)
        title.textAlign = Paint.Align.CENTER; title.textSize = 56f
        canvas.drawText("Пауза", w / 2f, panel.top + 72f, title)
        menuButtons.clear()
        val bw = panel.width() * 0.62f; val bh = 68f; val cx = w / 2f
        addButton(menuButtons, RectF(cx - bw / 2, panel.top + 120f, cx + bw / 2, panel.top + 120f + bh), "Продолжить", "#FF90E26A", "#FF3A741A") { actions?.onResumeGame() }
        val s = save
        addButton(menuButtons, RectF(cx - bw / 2, panel.top + 205f, cx + bw / 2, panel.top + 205f + bh), if (s?.soundEnabled == true) "Звук: ВКЛ" else "Звук: ВЫКЛ", "#FF7BCBFF", "#FF1D4E6E") { actions?.onToggleSound() }
        addButton(menuButtons, RectF(cx - bw / 2, panel.top + 290f, cx + bw / 2, panel.top + 290f + bh), "В меню", "#FFFFC629", "#FF8A4A00") { actions?.onMenu() }
        for ((r, _) in menuButtons) drawRoundButton(canvas, r, buttonLabel(r), buttonColor(r), buttonStroke(r))
    }

    private fun drawGameOver(canvas: Canvas) {
        val w = width.toFloat(); val h = height.toFloat()
        canvas.drawColor(Color.parseColor("#88000000"))
        val panel = RectF(w * 0.26f, h * 0.2f, w * 0.74f, h * 0.78f)
        drawPanel(canvas, panel)
        title.textAlign = Paint.Align.CENTER; title.textSize = 58f
        canvas.drawText("Улей пал...", w / 2f, panel.top + 76f, title)
        small.textAlign = Paint.Align.CENTER; small.textSize = 30f
        canvas.drawText("Счёт: ${hud.score}   Волна: ${hud.wave}", w / 2f, panel.top + 132f, small)
        canvas.drawText("Собрано мёда: ${save?.honey ?: 0}", w / 2f, panel.top + 170f, small)
        menuButtons.clear()
        val bw = panel.width() * 0.58f; val bh = 70f; val cx = w / 2f
        addButton(menuButtons, RectF(cx - bw / 2, panel.top + 230f, cx + bw / 2, panel.top + 230f + bh), "Сыграть ещё", "#FF90E26A", "#FF3A741A") { actions?.onRestart() }
        addButton(menuButtons, RectF(cx - bw / 2, panel.top + 318f, cx + bw / 2, panel.top + 318f + bh), "В меню", "#FFFFC629", "#FF8A4A00") { actions?.onMenu() }
        for ((r, _) in menuButtons) drawRoundButton(canvas, r, buttonLabel(r), buttonColor(r), buttonStroke(r))
    }

    private fun drawPanel(canvas: Canvas, rect: RectF) {
        canvas.drawRoundRect(rect, 24f, 24f, panelPaint)
        canvas.drawRoundRect(rect, 24f, 24f, panelStroke)
    }

    private fun drawRoundButton(canvas: Canvas, rect: RectF, label: String, fill: String, stroke: String) {
        buttonPaint.style = Paint.Style.FILL; buttonPaint.color = Color.parseColor(fill)
        canvas.drawRoundRect(rect, 22f, 22f, buttonPaint)
        buttonPaint.style = Paint.Style.STROKE; buttonPaint.strokeWidth = 4f; buttonPaint.color = Color.parseColor(stroke)
        canvas.drawRoundRect(rect, 22f, 22f, buttonPaint)
        val lines = label.split("\n")
        buttonText.textSize = if (lines.size > 1) 26f else 30f
        val baseY = rect.centerY() - (lines.size - 1) * 16f + 10f
        for ((i, line) in lines.withIndex()) canvas.drawText(line, rect.centerX(), baseY + i * 32f, buttonText)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val e = engine ?: return false
        val action = event.actionMasked
        val index = event.actionIndex
        val id = event.getPointerId(index)
        val x = event.getX(index); val y = event.getY(index)
        when (action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                if (handleTap(e, x, y)) return true
                if (e.state == GameState.PLAYING && !e.paused && inJoy(x, y)) {
                    joyPointer = id; updateJoy(event, id)
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (joyPointer != -1) updateJoy(event, joyPointer)
                updateFire(event)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                if (id == joyPointer) { joyPointer = -1; e.input = e.input.copy(mx = 0f, my = 0f) }
                updateFire(event)
            }
        }
        if (e.state == GameState.PLAYING) updateFire(event)
        return true
    }

    private fun inJoy(x: Float, y: Float) = hypot(x - joyCx, y - joyCy) <= joyR * 1.4f

    private fun updateJoy(event: MotionEvent, pointerId: Int) {
        val e = engine ?: return
        val idx = event.findPointerIndex(pointerId)
        if (idx < 0) return
        val x = event.getX(idx); val y = event.getY(idx)
        val dx = x - joyCx; val dy = y - joyCy
        val dist = hypot(dx, dy)
        val clamped = min(joyR, dist)
        val nx = if (dist > 0.001f) dx / dist else 0f
        val ny = if (dist > 0.001f) dy / dist else 0f
        joyKx = joyCx + nx * clamped
        joyKy = joyCy + ny * clamped
        val mx = (clamped / joyR) * nx
        val my = -((clamped / joyR) * ny)
        e.input = e.input.copy(mx = mx, my = my)
    }

    private fun updateFire(event: MotionEvent) {
        val e = engine ?: return
        if (e.state != GameState.PLAYING) { e.input = e.input.copy(firing = false); return }
        var firing = false
        for (i in 0 until event.pointerCount) {
            val x = event.getX(i); val y = event.getY(i)
            if (fireBtn.contains(x, y)) firing = true
        }
        e.input = e.input.copy(firing = firing)
    }

    private fun handleTap(e: GameEngine, x: Float, y: Float): Boolean {
        if (e.state == GameState.PLAYING || e.state == GameState.PAUSED) {
            if (pauseBtn.contains(x, y)) { if (e.state == GameState.PLAYING) actions?.onPauseGame() else actions?.onResumeGame(); return true }
        }
        if (e.state == GameState.PLAYING) {
            if (dashBtn.contains(x, y)) { actions?.onDash(); return true }
            if (ultBtn.contains(x, y)) { actions?.onUlt(); return true }
            if (fireBtn.contains(x, y)) return true
        }
        val buttons = when (e.state) {
            GameState.MENU, GameState.PAUSED, GameState.GAMEOVER -> menuButtons
            GameState.SHOP -> shopButtons
            else -> emptyList()
        }
        for ((r, action) in buttons) {
            if (r.contains(x, y)) { action(); return true }
        }
        return false
    }

    fun resetJoystick() {
        joyPointer = -1
        engine?.input = engine?.input?.copy(mx = 0f, my = 0f, firing = false) ?: return
    }
}
