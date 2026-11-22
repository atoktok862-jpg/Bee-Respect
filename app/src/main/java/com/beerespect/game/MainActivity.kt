package com.beerespect.game

import android.app.Activity
import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.beerespect.game.audio.Sfx
import com.beerespect.game.audio.SoundEngine
import com.beerespect.game.data.SaveManager
import com.beerespect.game.render.GLRenderer
import com.beerespect.game.ui.HudOverlay
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : Activity(), GameEngine.Listener, HudOverlay.Actions {

    private var glView: GLSurfaceView? = null
    private lateinit var overlay: HudOverlay
    private lateinit var loading: LoadingOverlay
    private lateinit var engine: GameEngine
    private lateinit var sound: SoundEngine
    private lateinit var save: SaveManager
    private var renderer: GLRenderer? = null
    @Volatile private var fatalShown = false
    @Volatile private var firstFrameShown = false
    private val uiHandler = Handler(Looper.getMainLooper())
    private var loadingTimeout: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installCrashHandler()
        super.onCreate(savedInstanceState)
        try {
            hideSystemUi()
            buildUi()
            startRenderer()
        } catch (t: Throwable) {
            showFatalError(t)
        }
    }

    private fun buildUi() {
        sound = SoundEngine()
        save = SaveManager(this)
        engine = GameEngine(sound, save)
        engine.listener = this
        sound.setEnabled(save.soundEnabled)

        overlay = HudOverlay(this)
        overlay.engine = engine
        overlay.save = save
        overlay.actions = this
        overlay.setBackgroundColor(Color.TRANSPARENT)

        loading = LoadingOverlay(this)
        loading.onRetry = { recreate() }

        val root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#88CEF0"))
        root.addView(overlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(loading, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }

    private fun startRenderer() {
        val surface = createGlSurface() ?: return
        glView = surface
        val root = findViewById<FrameLayout>(android.R.id.content).getChildAt(0) as FrameLayout
        root.addView(surface, 0, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        scheduleLoadingTimeout()
    }

    private fun scheduleLoadingTimeout() {
        loadingTimeout?.let { uiHandler.removeCallbacks(it) }
        val r = Runnable {
            if (!firstFrameShown && !fatalShown) {
                val msg = "3D-рендер не запустился за 4 секунды.\nПроверьте поддержку OpenGL ES 2.0 / драйвер GPU."
                loading.statusText = msg
                loading.showRetry = true
                Log.e("BeeGL", msg)
            }
        }
        loadingTimeout = r
        uiHandler.postDelayed(r, 4000)
    }

    private fun installCrashHandler() {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = buildTrace(thread.name, throwable)
            Log.e("BeeCrash", trace, throwable)
            if (!fatalShown) {
                fatalShown = true
                runOnUiThread {
                    try { showFatalError(trace) } catch (_: Throwable) { previous?.uncaughtException(thread, throwable) }
                }
            } else {
                previous?.uncaughtException(thread, throwable)
            }
        }
    }

    private fun createGlSurface(): GLSurfaceView? {
        return try {
            val am = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val glEsVersion = am.deviceConfigurationInfo.glEsVersion
            Log.i("BeeGL", "Device glEsVersion=$glEsVersion")
            val surface = GLSurfaceView(this)
            surface.setEGLContextClientVersion(2)
            surface.preserveEGLContextOnPause = true
            val glRenderer = GLRenderer(engine, onFatalError = { t -> showFatalError(t) }, onFirstFrame = { onFirstFrame() })
            renderer = glRenderer
            surface.setRenderer(glRenderer)
            surface.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
            surface.setBackgroundColor(Color.TRANSPARENT)
            surface
        } catch (t: Throwable) {
            Log.e("BeeGL", "Не удалось создать GLSurfaceView", t)
            showFatalError(t)
            null
        }
    }

    private fun onFirstFrame() {
        if (firstFrameShown) return
        firstFrameShown = true
        runOnUiThread {
            loadingTimeout?.let { uiHandler.removeCallbacks(it) }
            loading.animate().alpha(0f).setDuration(350).withEndAction {
                loading.visibility = View.GONE
                loading.alpha = 1f
            }.start()
        }
    }

    private fun buildTrace(threadName: String, throwable: Throwable): String {
        val sw = StringWriter()
        throwable.printStackTrace(PrintWriter(sw))
        return "Поток: $threadName\n\n$sw"
    }

    private fun showFatalError(throwable: Throwable) {
        showFatalError(buildTrace(Thread.currentThread().name, throwable))
    }

    @Synchronized
    private fun showFatalError(trace: String) {
        if (isFinishing || isDestroyed) return
        runOnUiThread {
            try {
                loadingTimeout?.let { uiHandler.removeCallbacks(it) }
                try { glView?.renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY } catch (_: Throwable) {}
                try { glView?.onPause() } catch (_: Throwable) {}
                if (::sound.isInitialized) { try { sound.stop() } catch (_: Throwable) {} }
                if (::loading.isInitialized) {
                    loading.statusText = "Ошибка 3D/EGL. Нажмите \"Попробовать снова\" или скопируйте ошибку."
                    loading.showRetry = true
                    loading.visibility = View.VISIBLE
                    loading.alpha = 1f
                }
                val root = LinearLayout(this).apply {
                    orientation = LinearLayout.VERTICAL
                    setBackgroundColor(Color.parseColor("#CC1A1208"))
                    setPadding(dp(16), dp(16), dp(16), dp(16))
                }
                val title = TextView(this).apply {
                    text = "Ошибка запуска"
                    setTextColor(Color.parseColor("#FFD24A"))
                    textSize = 22f; typeface = Typeface.DEFAULT_BOLD
                }
                val copy = Button(this).apply {
                    text = "Скопировать ошибку"
                    setOnClickListener {
                        val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
                        Toast.makeText(this@MainActivity, "Текст ошибки скопирован", Toast.LENGTH_SHORT).show()
                    }
                }
                val text = TextView(this).apply {
                    this.text = trace
                    setTextColor(Color.parseColor("#FFF3D6"))
                    textSize = 12f; typeface = Typeface.MONOSPACE; setTextIsSelectable(true)
                }
                val scroll = ScrollView(this).apply {
                    addView(text)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f)
                }
                root.addView(title)
                root.addView(copy)
                root.addView(scroll)
                val container = FrameLayout(this)
                val bg = View(this).apply { setBackgroundColor(Color.parseColor("#FFF3B14A")) }
                container.addView(bg, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                container.addView(root, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
                setContentView(container)
            } catch (e: Throwable) {
                Log.e("BeeCrash", "Не удалось показать экран ошибки", e)
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onResume() {
        super.onResume(); hideSystemUi()
        try { glView?.onResume() } catch (t: Throwable) { showFatalError(t) }
        if (::sound.isInitialized) {
            sound.start()
            if (::save.isInitialized) sound.setEnabled(save.soundEnabled)
            if (::engine.isInitialized && engine.state == GameState.PLAYING) sound.resume() else sound.pause()
        }
    }

    override fun onPause() {
        if (::engine.isInitialized && engine.state == GameState.PLAYING) {
            engine.pauseGame()
            if (::overlay.isInitialized) overlay.resetJoystick()
        }
        if (::sound.isInitialized) sound.pause()
        try { glView?.onPause() } catch (_: Throwable) {}
        super.onPause()
    }

    override fun onDestroy() {
        loadingTimeout?.let { uiHandler.removeCallbacks(it) }
        try { glView?.queueEvent { try { renderer?.dispose() } catch (_: Throwable) {} } } catch (_: Throwable) {}
        if (::sound.isInitialized) { try { sound.stop() } catch (_: Throwable) {} }
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) { super.onWindowFocusChanged(hasFocus); if (hasFocus) hideSystemUi() }

    private fun hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onWaveCleared(wave: Int, bonus: Int) { runOnUiThread { if (::overlay.isInitialized) overlay.hud = overlay.hud.copy(message = "Волна $wave зачищена! +$bonus мёда") } }
    override fun onGameOver(score: Int, wave: Int) { runOnUiThread { if (::overlay.isInitialized) overlay.hud = overlay.hud.copy(message = "Улей пал на волне $wave") } }
    override fun onHud(state: com.beerespect.game.HudState) { runOnUiThread { overlay.hud = state } }
    override fun onStart() { sound.play(Sfx.CLICK); engine.startGame(); sound.resume(); overlay.resetJoystick() }
    override fun onResumeGame() { sound.play(Sfx.CLICK); engine.resumeGame(); sound.resume() }
    override fun onPauseGame() { sound.play(Sfx.CLICK); engine.pauseGame(); sound.pause(); overlay.resetJoystick() }
    override fun onMenu() { sound.play(Sfx.CLICK); engine.toMenu(); sound.pause(); overlay.resetJoystick() }
    override fun onShop() { sound.play(Sfx.CLICK); engine.openShop(); overlay.postInvalidateOnAnimation() }
    override fun onNextWave() { sound.play(Sfx.CLICK); engine.nextWave(); sound.resume(); overlay.resetJoystick() }
    override fun onDash() { engine.requestDash() }
    override fun onUlt() { engine.requestUlt() }
    override fun onBuy(kind: Int) { if (save.buyUpgrade(kind)) { sound.play(Sfx.CLICK); overlay.postInvalidateOnAnimation() } }
    override fun onSkin(index: Int) { if (save.skinUnlocked(index)) { save.selectedSkin = index; sound.play(Sfx.CLICK); overlay.postInvalidateOnAnimation() } }
    override fun onToggleSound() { save.soundEnabled = !save.soundEnabled; sound.setEnabled(save.soundEnabled); sound.play(Sfx.CLICK); overlay.postInvalidateOnAnimation() }
    override fun onRestart() { sound.play(Sfx.CLICK); engine.startGame(); sound.resume(); overlay.resetJoystick() }
}
