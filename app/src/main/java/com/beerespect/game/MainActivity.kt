package com.beerespect.game

import android.app.Activity
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.FrameLayout
import com.beerespect.game.audio.Sfx
import com.beerespect.game.audio.SoundEngine
import com.beerespect.game.data.SaveManager
import com.beerespect.game.render.GLRenderer
import com.beerespect.game.ui.HudOverlay

class MainActivity : Activity(), GameEngine.Listener, HudOverlay.Actions {

    private lateinit var glView: GLSurfaceView
    private lateinit var overlay: HudOverlay
    private lateinit var engine: GameEngine
    private lateinit var sound: SoundEngine
    private lateinit var save: SaveManager
    private var renderer: GLRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        hideSystemUi()

        sound = SoundEngine()
        save = SaveManager(this)
        engine = GameEngine(sound, save)
        engine.listener = this
        sound.setEnabled(save.soundEnabled)

        glView = GLSurfaceView(this)
        glView.setEGLContextClientVersion(3)
        glView.preserveEGLContextOnPause = true
        val glRenderer = GLRenderer(engine)
        renderer = glRenderer
        glView.setRenderer(glRenderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        overlay = HudOverlay(this)
        overlay.engine = engine
        overlay.save = save
        overlay.actions = this

        val root = FrameLayout(this)
        root.addView(glView, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        root.addView(overlay, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT))
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        glView.onResume()
        sound.start()
        sound.setEnabled(save.soundEnabled)
        if (engine.state == GameState.PLAYING) sound.resume() else sound.pause()
    }

    override fun onPause() {
        if (engine.state == GameState.PLAYING) {
            engine.pauseGame()
            overlay.resetJoystick()
        }
        sound.pause()
        glView.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        glView.queueEvent { renderer?.models?.dispose() }
        sound.stop()
        super.onDestroy()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    private fun hideSystemUi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            window.insetsController?.let { controller ->
                controller.hide(WindowInsets.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            @Suppress("DEPRECATION")
            window.decorView.systemUiVisibility =
                (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        or View.SYSTEM_UI_FLAG_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        or View.SYSTEM_UI_FLAG_LAYOUT_STABLE)
        }
    }

    override fun onWaveCleared(wave: Int, bonus: Int) {
        runOnUiThread { overlay.hud = overlay.hud.copy(message = "Волна $wave зачищена! +$bonus мёда") }
    }

    override fun onGameOver(score: Int, wave: Int) {
        runOnUiThread { overlay.hud = overlay.hud.copy(message = "Улей пал на волне $wave") }
    }

    override fun onHud(state: com.beerespect.game.HudState) {
        runOnUiThread { overlay.hud = state }
    }

    override fun onStart() {
        sound.play(Sfx.CLICK)
        engine.startGame()
        sound.resume()
        overlay.resetJoystick()
    }

    override fun onResumeGame() {
        sound.play(Sfx.CLICK)
        engine.resumeGame()
        sound.resume()
    }

    override fun onPauseGame() {
        sound.play(Sfx.CLICK)
        engine.pauseGame()
        sound.pause()
        overlay.resetJoystick()
    }

    override fun onMenu() {
        sound.play(Sfx.CLICK)
        engine.toMenu()
        sound.pause()
        overlay.resetJoystick()
    }

    override fun onShop() {
        sound.play(Sfx.CLICK)
        engine.openShop()
        overlay.postInvalidateOnAnimation()
    }

    override fun onNextWave() {
        sound.play(Sfx.CLICK)
        engine.nextWave()
        sound.resume()
        overlay.resetJoystick()
    }

    override fun onDash() {
        engine.requestDash()
    }

    override fun onUlt() {
        engine.requestUlt()
    }

    override fun onBuy(kind: Int) {
        if (save.buyUpgrade(kind)) {
            sound.play(Sfx.CLICK)
            overlay.postInvalidateOnAnimation()
        }
    }

    override fun onSkin(index: Int) {
        if (save.skinUnlocked(index)) {
            save.selectedSkin = index
            sound.play(Sfx.CLICK)
            overlay.postInvalidateOnAnimation()
        }
    }

    override fun onToggleSound() {
        save.soundEnabled = !save.soundEnabled
        sound.setEnabled(save.soundEnabled)
        sound.play(Sfx.CLICK)
        overlay.postInvalidateOnAnimation()
    }

    override fun onRestart() {
        sound.play(Sfx.CLICK)
        engine.startGame()
        sound.resume()
        overlay.resetJoystick()
    }
}
