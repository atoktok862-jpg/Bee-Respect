package com.beerespect.game.data

import android.content.Context

data class Record(val score: Int, val wave: Int, val date: String)

class SaveManager(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("bee_respect_save", Context.MODE_PRIVATE)

    var honey: Int
        get() = prefs.getInt("honey", 30)
        set(value) = prefs.edit().putInt("honey", value.coerceAtLeast(0)).apply()

    var lifetimeHoney: Int
        get() = prefs.getInt("lifetime_honey", 0)
        set(value) = prefs.edit().putInt("lifetime_honey", value).apply()

    var bestScore: Int
        get() = prefs.getInt("best_score", 0)
        set(value) = prefs.edit().putInt("best_score", value).apply()

    var bestWave: Int
        get() = prefs.getInt("best_wave", 0)
        set(value) = prefs.edit().putInt("best_wave", value).apply()

    var bestSurvival: Int
        get() = prefs.getInt("best_survival", 0)
        set(value) = prefs.edit().putInt("best_survival", value).apply()

    var swarm: Int
        get() = prefs.getInt("up_swarm", 0).coerceIn(0, 14)
        set(value) = prefs.edit().putInt("up_swarm", value.coerceIn(0, 14)).apply()

    var sting: Int
        get() = prefs.getInt("up_sting", 0).coerceIn(0, 8)
        set(value) = prefs.edit().putInt("up_sting", value.coerceIn(0, 8)).apply()

    var turbo: Int
        get() = prefs.getInt("up_turbo", 0).coerceIn(0, 8)
        set(value) = prefs.edit().putInt("up_turbo", value.coerceIn(0, 8)).apply()

    var armor: Int
        get() = prefs.getInt("up_armor", 0).coerceIn(0, 8)
        set(value) = prefs.edit().putInt("up_armor", value.coerceIn(0, 8)).apply()

    var selectedSkin: Int
        get() = prefs.getInt("skin", 0).coerceIn(0, 3)
        set(value) = prefs.edit().putInt("skin", value.coerceIn(0, 3)).apply()

    var soundEnabled: Boolean
        get() = prefs.getBoolean("sound", true)
        set(value) = prefs.edit().putBoolean("sound", value).apply()

    fun skinUnlocked(index: Int): Boolean = when (index) {
        0 -> true
        1 -> lifetimeHoney >= 120
        2 -> lifetimeHoney >= 400
        3 -> lifetimeHoney >= 900
        else -> false
    }

    fun skinRequirement(index: Int): String = when (index) {
        0 -> "Доступен с начала игры"
        1 -> "Собери 120 мёда за всё время"
        2 -> "Собери 400 мёда за всё время"
        3 -> "Собери 900 мёда за всё время"
        else -> ""
    }

    fun upgradeCost(kind: Int): Int {
        val level = when (kind) {
            0 -> swarm
            1 -> sting
            2 -> turbo
            3 -> armor
            else -> 0
        }
        val base = when (kind) {
            0 -> 22
            1 -> 28
            2 -> 26
            3 -> 32
            else -> 9999
        }
        return base + level * level * 6 + level * 14
    }

    fun upgradeMax(kind: Int): Int = when (kind) {
        0 -> 14
        1 -> 8
        2 -> 8
        3 -> 8
        else -> 0
    }

    fun upgradeLevel(kind: Int): Int = when (kind) {
        0 -> swarm
        1 -> sting
        2 -> turbo
        3 -> armor
        else -> 0
    }

    fun buyUpgrade(kind: Int): Boolean {
        val level = upgradeLevel(kind)
        if (level >= upgradeMax(kind)) return false
        val price = upgradeCost(kind)
        if (honey < price) return false
        honey -= price
        when (kind) {
            0 -> swarm = level + 1
            1 -> sting = level + 1
            2 -> turbo = level + 1
            3 -> armor = level + 1
        }
        return true
    }

    fun addHoney(amount: Int) {
        if (amount <= 0) return
        honey += amount
        lifetimeHoney += amount
    }

    fun addRecord(score: Int, wave: Int, survival: Boolean) {
        if (score > bestScore) bestScore = score
        if (wave > bestWave) bestWave = wave
        if (survival && wave - 20 > bestSurvival) bestSurvival = wave - 20
        val current = records().toMutableList()
        val date = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault())
            .format(java.util.Date())
        current.add(Record(score, wave, date))
        val sorted = current.sortedByDescending { it.score }.take(5)
        prefs.edit().putString("records", sorted.joinToString("|") { "${it.score};${it.wave};${it.date}" }).apply()
    }

    fun records(): List<Record> {
        val raw = prefs.getString("records", "") ?: return emptyList()
        if (raw.isEmpty()) return emptyList()
        return raw.split("|").mapNotNull { entry ->
            val parts = entry.split(";")
            if (parts.size == 3) {
                val score = parts[0].toIntOrNull() ?: return@mapNotNull null
                val wave = parts[1].toIntOrNull() ?: return@mapNotNull null
                Record(score, wave, parts[2])
            } else null
        }.sortedByDescending { it.score }
    }
}
