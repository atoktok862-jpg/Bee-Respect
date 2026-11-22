package com.beerespect.game

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast

class CrashActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val trace = intent.getStringExtra(EXTRA_TRACE) ?: "Неизвестная ошибка запуска."

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#1A1208"))
            setPadding(dp(16), dp(16), dp(16), dp(16))
        }

        val title = TextView(this).apply {
            text = "Обнаружена ошибка"
            setTextColor(Color.parseColor("#FFD24A"))
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
        }
        val hint = TextView(this).apply {
            text = "Приложение перехватило сбой. Скопируйте текст ниже и отправьте для исправления."
            setTextColor(Color.WHITE)
            textSize = 14f
            setPadding(0, dp(8), 0, dp(12))
        }
        val copy = Button(this).apply {
            text = "Скопировать ошибку"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
                Toast.makeText(this@CrashActivity, "Текст ошибки скопирован", Toast.LENGTH_SHORT).show()
            }
        }
        val close = Button(this).apply {
            text = "Закрыть"
            setOnClickListener { finishAffinity() }
        }
        val buttons = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.END
            addView(copy)
            addView(close)
        }

        val text = TextView(this).apply {
            this.text = trace
            setTextColor(Color.parseColor("#FFF3D6"))
            textSize = 12f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
        }
        val scroll = ScrollView(this).apply {
            addView(text)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f
            )
        }

        root.addView(title)
        root.addView(hint)
        root.addView(buttons)
        root.addView(scroll)
        setContentView(root)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    companion object {
        const val EXTRA_TRACE = "extra_trace"
    }
}
