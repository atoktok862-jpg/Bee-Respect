package com.beerespect.game

import com.beerespect.game.audio.Sfx
import com.beerespect.game.audio.SoundEngine
import com.beerespect.game.core.Vec3
import com.beerespect.game.data.SaveManager
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

enum class GameState { MENU, PLAYING, SHOP, PAUSED, GAMEOVER }

data class InputState(val mx: Float = 0f, val my: Float = 0f, val firing: Boolean = false)

data class HudState(
    val wave: Int = 1,
    val score: Int = 0,
    val honey: Int = 0,
    val hiveHp: Float = 100f,
    val hiveMax: Float = 100f,
    val playerHp: Float = 100f,
    val ult: Float = 0f,
    val dashReady: Float = 1f,
    val enemies: Int = 0,
    val survival: Boolean = false,
    val message: String = "",
    val boss: Boolean = false
)

class Particle {
    var active = false
    var x = 0f
    var y = 0f
    var z = 0f
    var vx = 0f
    var vy = 0f
    var vz = 0f
    var life = 0f
    var maxLife = 1f
    var size = 1f
    var r = 1f
    var g = 1f
    var b = 1f
    var gravity = 0f
    var drag = 0f
}

class Player {
    val pos = Vec3(0f, 3.4f, 12f)
    var heading = 0f
    var hp = 100f
    var maxHp = 100f
    var fireCd = 0f
    var dashTime = 0f
    var dashCd = 0f
    var shield = 0f
    var invuln = 0f
    var hitFlash = 0f
    var wing = 0f
}

class Ally {
    val pos = Vec3()
    var phase = 0f
    var fireCd = 0f
    var orbit = 0f
}

class Enemy {
    var kind = 0
    val pos = Vec3()
    var hp = 40f
    var maxHp = 40f
    var speed = 6f
    var damage = 10f
    var radius = 1.4f
    var yaw = 0f
    var phase = 0f
    var fireCd = 0f
    var flash = 0f
    var shootCount = 0
    var alive = true
}

class Bullet(
    val pos: Vec3,
    val vel: Vec3,
    var life: Float,
    val friendly: Boolean,
    val damage: Float,
    val kind: Int
)

class Pollen(val pos: Vec3, val vel: Vec3, var life: Float, var phase: Float)

class Flower(val x: Float, val z: Float, var phase: Float) {
    var bloom = 0f
}

class Crystal(val x: Float, val y: Float, val z: Float, var phase: Float) {
    var cooldown = 0f
}

class GameEngine(
    private val sound: SoundEngine,
    val save: SaveManager
) {
    interface Listener {
        fun onWaveCleared(wave: Int, bonus: Int)
        fun onGameOver(score: Int, wave: Int)
        fun onHud(state: HudState)
    }

    var listener: Listener? = null
    @Volatile var input: InputState = InputState()
    @Volatile var paused = false

    val player = Player()
    val allies = ArrayList<Ally>()
    val enemies = ArrayList<Enemy>()
    val bullets = ArrayList<Bullet>()
    val pollens = ArrayList<Pollen>()
    val particles = Array(700) { Particle() }
    val flowers = ArrayList<Flower>()
    val crystals = ArrayList<Crystal>()

    var state = GameState.MENU
        private set
    var time = 0f
        private set
    var wave = 0
        private set
    var score = 0
        private set
    var hiveHp = 300f
        private set
    var hiveMax = 300f
        private set
    var ultCharge = 0f
        private set
    var bossAlive = false
        private set
    var shakeRequest = 0f
        private set

    private var spawnLeft = 0
    private var spawnTimer = 0f
    private var waveActive = false
    private var clearTimer = 0f
    private var bloomTimer = 5f
    private var ultTime = 0f
    private var ultTick = 0f
    private var hudTimer = 0f
    private var hitSoundTimer = 0f
    private var message = ""
    private var messageTimer = 0f
    private var particleCursor = 0
    @Volatile private var dashRequested = false
    @Volatile private var ultRequested = false
    private val lock = Any()

    fun startGame() {
        synchronized(lock) {
            time = 0f
            wave = 0
            score = 0
            ultCharge = 0f
            ultTime = 0f
            bossAlive = false
            spawnLeft = 0
            spawnTimer = 0f
            waveActive = false
            clearTimer = 0f
            bloomTimer = 4f
            player.pos.set(0f, 3.4f, 12f)
            player.heading = 0f
            player.hp = 100f
            player.maxHp = 100f
            player.fireCd = 0f
            player.dashTime = 0f
            player.dashCd = 0f
            player.shield = 0f
            player.invuln = 0f
            player.hitFlash = 0f
            enemies.clear()
            bullets.clear()
            pollens.clear()
            allies.clear()
            for (particle in particles) particle.active = false
            hiveMax = 260f + save.armor * 70f
            hiveHp = hiveMax
            prepareField()
            state = GameState.PLAYING
            paused = false
            input = InputState()
            startWave(1)
        }
    }

    private fun prepareField() {
        flowers.clear()
        val random = Random(2024)
        for (k in 0 until 12) {
            val a = k.toFloat() / 12f * 6.283185f + random.nextFloat() * 0.2f
            val r = 13f + random.nextFloat() * 25f
            flowers.add(Flower(cos(a) * r, sin(a) * r, random.nextFloat() * 6.28f))
        }
        crystals.clear()
        for (k in 0 until 9) {
            val a = k.toFloat() / 9f * 6.283185f
            val r = 18f + random.nextFloat() * 32f
            crystals.add(Crystal(cos(a) * r, 2.2f + random.nextFloat() * 4f, sin(a) * r, random.nextFloat() * 6.28f))
        }
    }

    private fun startWave(number: Int) {
        wave = number
        waveActive = true
        clearTimer = 0f
        val count = if (number <= 20) {
            min(46, 4 + number * 2)
        } else {
            min(78, 24 + (number - 20) * 3)
        }
        spawnLeft = count
        spawnTimer = 0.7f
        if (number % 5 == 0) {
            spawnEnemy(3)
            showMessage("БОСС: КОРОЛЬ ШЕРШНЕЙ!", 3f)
            sound.play(Sfx.BOSS)
        } else {
            showMessage(if (number > 20) "Выживание Улья: волна $number" else "Волна $number", 2.2f)
        }
        syncAllies()
    }

    fun nextWave() {
        if (state != GameState.SHOP) return
        state = GameState.PLAYING
        paused = false
        if (wave <= 0) startGame() else startWave(wave + 1)
    }

    fun resumeGame() {
        if (state == GameState.PAUSED) {
            state = GameState.PLAYING
            paused = false
        }
    }

    fun pauseGame() {
        if (state == GameState.PLAYING) {
            state = GameState.PAUSED
            paused = true
        }
    }

    fun toMenu() {
        state = GameState.MENU
        paused = true
        input = InputState()
    }

    fun openShop() {
        if (state == GameState.MENU || state == GameState.GAMEOVER || state == GameState.PAUSED) {
            state = GameState.SHOP
            paused = true
            input = InputState()
        }
    }

    fun requestDash() { dashRequested = true }

    fun requestUlt() { ultRequested = true }

    fun consumeShake(): Float {
        synchronized(lock) {
            val value = shakeRequest
            shakeRequest = 0f
            return value
        }
    }

    fun isPlayable(): Boolean = state == GameState.PLAYING && !paused

    fun update(dt: Float) {
        val step = min(dt, 0.05f)
        synchronized(lock) {
            time += step
            updateParticles(step)
            updateFlowers(step)
            if (messageTimer > 0f) messageTimer -= step
            if (hitSoundTimer > 0f) hitSoundTimer -= step
            if (!isPlayable()) {
                updateHud(step)
                return
            }
            updatePlayer(step)
            updateAllies(step)
            updateSpawning(step)
            updateEnemies(step)
            updateBullets(step)
            updatePickups(step)
            updateUlt(step)
            hiveHp = min(hiveMax, hiveHp + (0.9f + save.armor * 0.7f) * step)
            updateWaveCompletion(step)
            updateHud(step)
        }
    }

    private fun updatePlayer(dt: Float) {
        val control = input
        var turn = false
        var dash = false
        dash = dashRequested
        dashRequested = false
        if (dash && player.dashCd <= 0f) {
            player.dashTime = 0.45f
            player.dashCd = max(2.4f, 6.4f - save.turbo * 0.45f)
            player.shield = 0.95f
            player.invuln = 0.5f
            sound.play(Sfx.DASH)
            burst(player.pos.x, player.pos.y, player.pos.z, 18, 0.4f, 1f, 0.5f, 0.85f, 0.2f)
        }
        if (abs(control.mx) > 0.12f) {
            player.heading += control.mx * dt * 1.9f
            turn = true
        }
        val speed = (10.5f + save.turbo * 1.05f) * if (player.dashTime > 0f) 2.5f else 1f
        val forward = max(0f, control.my)
        val backward = min(0f, control.my)
        val move = forward + backward * 0.55f
        player.pos.x += sin(player.heading) * speed * move * dt
        player.pos.z -= cos(player.heading) * speed * move * dt
        val radius = kotlin.math.hypot(player.pos.x, player.pos.z)
        if (radius > 68f) {
            player.pos.x *= 68f / radius
            player.pos.z *= 68f / radius
        }
        val wantedY = 3.2f + sin(time * 1.6f) * 0.18f + if (player.dashTime > 0f) 0.8f else 0f
        player.pos.y += (wantedY - player.pos.y) * min(1f, dt * 4f)
        player.wing += dt * (34f + save.turbo * 2f)
        if (player.fireCd > 0f) player.fireCd -= dt
        if (player.dashTime > 0f) player.dashTime -= dt
        if (player.dashCd > 0f) player.dashCd -= dt
        if (player.shield > 0f) player.shield -= dt
        if (player.invuln > 0f) player.invuln -= dt
        if (player.hitFlash > 0f) player.hitFlash -= dt
        sound.setEngineActive(turn || abs(control.my) > 0.1f)
        if (control.firing && player.fireCd <= 0f) fireSting()
    }

    private fun fireSting() {
        player.fireCd = 0.36f / (1f + save.sting * 0.16f)
        val target = nearestEnemy(player.pos, 34f)
        val baseYaw = if (target != null) {
            atan2(target.pos.x - player.pos.x, -(target.pos.z - player.pos.z))
        } else player.heading
        val shots = 1 + (if (save.sting >= 3) 1 else 0) + (if (save.sting >= 6) 1 else 0)
        val spread = 0.11f
        for (k in 0 until shots) {
            val offset = (k - (shots - 1) / 2f) * spread
            val yaw = baseYaw + offset
            val vel = Vec3(sin(yaw) * 34f, 0f, -cos(yaw) * 34f)
            val origin = Vec3(player.pos.x + sin(baseYaw) * 1.1f, player.pos.y, player.pos.z - cos(baseYaw) * 1.1f)
            bullets.add(Bullet(origin, vel, 1.4f, true, 11f + save.sting * 3.2f, 0))
        }
        sound.play(Sfx.STING)
        burst(player.pos.x + sin(baseYaw), player.pos.y, player.pos.z - cos(baseYaw), 4, 0.3f, 0.5f, 1f, 0.8f, 0.15f)
    }

    private fun syncAllies() {
        val wanted = min(15, 1 + save.swarm)
        while (allies.size < wanted) {
            val ally = Ally()
            ally.pos.set(player.pos.x, player.pos.y + 0.7f, player.pos.z + 2f)
            ally.phase = allies.size * 0.7f
            allies.add(ally)
        }
        while (allies.size > wanted) allies.removeAt(allies.size - 1)
    }

    private fun updateAllies(dt: Float) {
        syncAllies()
        for (index in allies.indices) {
            val ally = allies[index]
            ally.phase += dt * 1.4f
            val focus = nearestEnemy(ally.pos, 14f)
            val tx: Float
            val ty: Float
            val tz: Float
            if (focus != null) {
                val angle = time * 2.2f + ally.phase
                tx = focus.pos.x + cos(angle) * 3.2f
                ty = focus.pos.y + sin(angle) * 1.4f + 0.6f
                tz = focus.pos.z + sin(angle) * 3.2f
            } else {
                val side = index - (allies.size - 1) / 2f
                val back = 2.2f + abs(side) * 0.42f
                tx = player.pos.x - sin(player.heading) * back + cos(player.heading) * side * 1.1f
                ty = player.pos.y + 0.5f + sin(time * 2f + ally.phase) * 0.4f
                tz = player.pos.z + cos(player.heading) * back + sin(player.heading) * side * 1.1f
            }
            val k = min(1f, dt * 3.6f)
            ally.pos.x += (tx - ally.pos.x) * k
            ally.pos.y += (ty - ally.pos.y) * k
            ally.pos.z += (tz - ally.pos.z) * k
            ally.fireCd -= dt
            if (focus != null && ally.fireCd <= 0f) {
                ally.fireCd = max(0.33f, 0.95f - save.swarm * 0.035f)
                val dir = Vec3(focus.pos.x - ally.pos.x, focus.pos.y - ally.pos.y, focus.pos.z - ally.pos.z).normalize()
                val vel = Vec3(dir.x * 30f, dir.y * 30f, dir.z * 30f)
                bullets.add(Bullet(ally.pos.copy(), vel, 1.2f, true, 5f + save.swarm * 0.5f, 1))
            }
            if (abs(ally.pos.x) > 72f || abs(ally.pos.z) > 72f) {
                ally.pos.x = player.pos.x
                ally.pos.z = player.pos.z
            }
        }
    }

    private fun updateSpawning(dt: Float) {
        if (!waveActive || spawnLeft <= 0) return
        spawnTimer -= dt
        if (spawnTimer > 0f) return
        spawnTimer = max(0.32f, 1.65f - wave * 0.055f)
        val roll = Random.nextFloat()
        val kind = when {
            wave <= 2 -> 0
            wave <= 4 -> if (roll < 0.72f) 0 else 1
            else -> when {
                roll < 0.46f -> 0
                roll < 0.72f -> 1
                else -> 2
            }
        }
        spawnEnemy(kind)
        spawnLeft--
    }

    private fun spawnEnemy(kind: Int) {
        val enemy = Enemy()
        enemy.kind = kind
        val angle = Random.nextFloat() * 6.283185f
        val radius = 43f + Random.nextFloat() * 15f
        enemy.pos.set(cos(angle) * radius, if (kind == 3) 5f else 1.5f + Random.nextFloat() * 2.6f, sin(angle) * radius)
        when (kind) {
            0 -> {
                enemy.hp = 34f + wave * 6f
                enemy.maxHp = enemy.hp
                enemy.speed = 6.2f + wave * 0.13f
                enemy.damage = 11f + wave * 0.4f
                enemy.radius = 1.5f
            }
            1 -> {
                enemy.hp = 28f + wave * 5f
                enemy.maxHp = enemy.hp
                enemy.speed = 5f + wave * 0.1f
                enemy.damage = 7f + wave * 0.25f
                enemy.radius = 1.3f
                enemy.fireCd = 1.6f
            }
            2 -> {
                enemy.hp = 24f + wave * 4.5f
                enemy.maxHp = enemy.hp
                enemy.speed = 9.4f + wave * 0.2f
                enemy.damage = 9f + wave * 0.3f
                enemy.radius = 1.2f
            }
            else -> {
                enemy.hp = 340f + wave * 48f
                enemy.maxHp = enemy.hp
                enemy.speed = 3.8f
                enemy.damage = 34f + wave * 0.8f
                enemy.radius = 3.6f
                enemy.fireCd = 2.2f
                bossAlive = true
            }
        }
        enemies.add(enemy)
    }

    private fun updateEnemies(dt: Float) {
        val iterator = enemies.iterator()
        while (iterator.hasNext()) {
            val e = iterator.next()
            e.phase += dt * 8f
            if (e.flash > 0f) e.flash -= dt
            var tx = 0f
            var ty = 1.6f
            var tz = 0f
            val hiveDist = kotlin.math.hypot(e.pos.x, e.pos.z)
            if (e.kind == 2 && player.pos.distanceTo(e.pos) < 20f) {
                tx = player.pos.x
                ty = player.pos.y
                tz = player.pos.z
            }
            val dx = tx - e.pos.x
            val dz = tz - e.pos.z
            val dy = ty - e.pos.y
            val dist = kotlin.math.hypot(dx, dz)
            if (e.kind == 1 && hiveDist < 21f) {
                val orbit = time * 0.35f + e.phase * 0.05f
                tx = cos(orbit) * 20f
                tz = sin(orbit) * 20f
            }
            val moveX = tx - e.pos.x
            val moveZ = tz - e.pos.z
            val moveLen = max(0.01f, kotlin.math.hypot(moveX, moveZ))
            val speed = e.speed * if (e.kind == 1 && hiveDist < 21f) 0.5f else 1f
            e.pos.x += moveX / moveLen * speed * dt
            e.pos.z += moveZ / moveLen * speed * dt
            e.pos.y += (ty - e.pos.y) * min(1f, dt * 1.5f)
            if (moveLen > 0.2f) e.yaw = atan2(moveX, -moveZ)
            if (e.flash <= 0f && dist < 0.1f) e.yaw += 0f

            e.fireCd -= dt
            if (e.kind == 1 && hiveDist < 30f && e.fireCd <= 0f) {
                e.fireCd = max(1.2f, 2.5f - wave * 0.03f)
                shootAt(e, player.pos, 15f, e.damage, 2)
            }
            if (e.kind == 3 && e.fireCd <= 0f) {
                e.fireCd = 2.3f
                for (k in -1..1) {
                    val target = Vec3(player.pos.x + k * 2.5f, player.pos.y, player.pos.z)
                    shootAt(e, target, 17f, e.damage * 0.4f, 3)
                }
                burst(e.pos.x, e.pos.y, e.pos.z, 16, 0.8f, 0.9f, 0.25f, 1f, 0.2f)
                shake(0.5f)
            }

            val playerDist = player.pos.distanceTo(e.pos)
            if (playerDist < e.radius + 1.3f && player.shield <= 0f) {
                damagePlayer(e.damage * 0.6f)
                e.pos.x -= moveX / moveLen * 2f
                e.pos.z -= moveZ / moveLen * 2f
            }
            if (hiveDist < e.radius + 4.2f && e.kind != 1) {
                damageHive(e.damage)
                burst(e.pos.x, e.pos.y, e.pos.z, 20, 0.9f, 1f, 0.5f, 1f, 0.3f)
                if (e.kind == 3) {
                    e.pos.x *= 1.35f
                    e.pos.z *= 1.35f
                    e.fireCd = 1.5f
                } else {
                    e.alive = false
                }
            }
            if (!e.alive) {
                if (e.kind == 3) bossAlive = false
                iterator.remove()
            }
        }
    }

    private fun shootAt(e: Enemy, target: Vec3, speed: Float, damage: Float, kind: Int) {
        val dir = Vec3(target.x - e.pos.x, target.y - e.pos.y, target.z - e.pos.z).normalize()
        val vel = Vec3(dir.x * speed, dir.y * speed, dir.z * speed)
        bullets.add(Bullet(e.pos.copy(), vel, 2.8f, false, damage, kind))
    }

    private fun updateBullets(dt: Float) {
        val iterator = bullets.iterator()
        while (iterator.hasNext()) {
            val b = iterator.next()
            b.pos.addScaled(b.vel, dt)
            b.life -= dt
            var remove = b.life <= 0f || b.pos.y < 0f
            if (!remove && b.friendly) {
                val enemyIterator = enemies.iterator()
                while (enemyIterator.hasNext()) {
                    val e = enemyIterator.next()
                    if (b.pos.distanceTo(e.pos) < e.radius + 0.55f) {
                        damageEnemy(e, b.damage)
                        burst(b.pos.x, b.pos.y, b.pos.z, 6, 0.5f, 1f, 0.7f, 0.9f, 0.2f)
                        remove = true
                        break
                    }
                }
            } else if (!remove) {
                if (b.pos.distanceTo(player.pos) < 1.5f) {
                    damagePlayer(b.damage)
                    remove = true
                } else if (player.shield > 0f && b.pos.distanceTo(player.pos) < 2.6f) {
                    remove = true
                    burst(b.pos.x, b.pos.y, b.pos.z, 8, 0.4f, 0.5f, 1f, 1f, 0.2f)
                } else if (kotlin.math.hypot(b.pos.x, b.pos.z) < 4.6f && b.pos.y < 4.5f) {
                    damageHive(b.damage * 0.7f)
                    remove = true
                }
            }
            if (remove) iterator.remove()
        }
    }

    private fun damageEnemy(e: Enemy, amount: Float) {
        e.hp -= amount
        e.flash = 0.12f
        if (e.hp <= 0f) {
            if (e.kind == 3) bossAlive = false
            e.alive = false
            val reward = if (e.kind == 3) 55 + wave * 2 else 2 + wave / 5
            save.addHoney(reward)
            score += (if (e.kind == 3) 900 else 45 + wave * 8) + (if (e.kind == 2) 25 else 0)
            sound.play(Sfx.EXPLODE)
            shake(if (e.kind == 3) 1.6f else 0.45f)
            burst(e.pos.x, e.pos.y, e.pos.z, if (e.kind == 3) 90 else 24, 1.4f, if (e.kind == 3) 2f else 1f, 0.5f, 1.2f, 0.5f)
            burst(e.pos.x, e.pos.y, e.pos.z, if (e.kind == 3) 45 else 12, 0.8f, 0.35f, 0.28f, 0.2f, -0.2f)
        }
    }

    private fun damageHive(amount: Float) {
        hiveHp -= amount
        shake(0.7f)
        burst(0f, 2f, -4.5f, 12, 0.7f, 1f, 0.7f, 0.2f, 0.1f)
        if (hiveHp <= 0f) {
            hiveHp = 0f
            gameOver()
        }
    }

    private fun damagePlayer(amount: Float) {
        if (player.invuln > 0f || player.shield > 0f) return
        player.hp -= amount
        player.invuln = 0.55f
        player.hitFlash = 0.2f
        sound.play(Sfx.HIT)
        shake(0.55f)
        burst(player.pos.x, player.pos.y, player.pos.z, 12, 0.6f, 1f, 0.35f, 0.2f, 0.1f)
        if (player.hp <= 0f) {
            player.hp = 0f
            gameOver()
        }
    }

    private fun updatePickups(dt: Float) {
        val iterator = pollens.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.life -= dt
            p.phase += dt * 3f
            p.pos.addScaled(p.vel, dt)
            p.vel.y -= 0.6f * dt
            p.pos.y += sin(p.phase) * 0.012f
            if (p.pos.distanceTo(player.pos) < 2.7f) {
                save.addHoney(2)
                score += 30
                ultCharge = min(100f, ultCharge + 9f)
                sound.play(Sfx.POLLEN)
                burst(p.pos.x, p.pos.y, p.pos.z, 14, 1f, 1f, 0.75f, 1f, 0.25f)
                iterator.remove()
            } else if (p.life <= 0f || p.pos.y < 0.2f) {
                iterator.remove()
            }
        }
        for (crystal in crystals) {
            crystal.phase += dt * 1.5f
            if (crystal.cooldown > 0f) crystal.cooldown -= dt
            val bob = sin(crystal.phase) * 0.4f
            if (crystal.cooldown <= 0f &&
                abs(player.pos.x - crystal.x) < 2.2f &&
                abs(player.pos.z - crystal.z) < 2.2f &&
                abs(player.pos.y - (crystal.y + bob)) < 2.5f
            ) {
                crystal.cooldown = 8f
                save.addHoney(1)
                score += 15
                ultCharge = min(100f, ultCharge + 4f)
                sound.play(Sfx.POLLEN)
                burst(crystal.x, crystal.y + bob, crystal.z, 10, 0.8f, 1f, 0.8f, 1f, 0.2f)
            }
        }
    }

    private fun updateFlowers(dt: Float) {
        for (flower in flowers) {
            flower.phase += dt * 1.4f
            if (flower.bloom > 0f) flower.bloom = max(0f, flower.bloom - dt * 0.06f)
        }
        bloomTimer -= dt
        if (bloomTimer <= 0f) {
            bloomTimer = 8.5f
            val flower = flowers[Random.nextInt(flowers.size)]
            flower.bloom = 1f
            for (k in 0 until 4) {
                val a = Random.nextFloat() * 6.283185f
                pollens.add(
                    Pollen(
                        Vec3(flower.x, 1.7f, flower.z),
                        Vec3(cos(a) * 1.4f, 1.9f + Random.nextFloat(), sin(a) * 1.4f),
                        11f,
                        Random.nextFloat() * 6f
                    )
                )
            }
            burst(flower.x, 1.4f, flower.z, 12, 0.7f, 1f, 0.85f, 0.3f, 0.2f)
        }
    }

    private fun updateUlt(dt: Float) {
        var ult = false
        ult = ultRequested
        ultRequested = false
        if (ult && ultCharge >= 100f) {
            ultCharge = 0f
            ultTime = 3.4f
            ultTick = 0f
            sound.play(Sfx.ULT)
            showMessage("ГНЕВ РОЯ!", 2f)
            shake(1.4f)
        }
        if (ultTime <= 0f) return
        ultTime -= dt
        ultTick -= dt
        if (ultTick > 0f) return
        ultTick = 0.16f
        val targets = ArrayList(enemies)
        for (e in targets) {
            if (!e.alive) continue
            damageEnemy(e, 16f + wave * 0.5f)
            val a = Random.nextFloat() * 6.283185f
            burst(
                e.pos.x + cos(a) * 1.5f, e.pos.y + 1f, e.pos.z + sin(a) * 1.5f,
                8, 1.2f, 1f, 0.9f, 0.3f, 0.1f
            )
        }
        if (enemies.any { !it.alive }) enemies.removeAll { !it.alive }
        hiveHp = min(hiveMax, hiveHp + 2f)
        burst(0f, 4f, 0f, 14, 1.6f, 1f, 0.85f, 0.3f, 0f)
    }

    fun ultActive(): Boolean = ultTime > 0f

    private fun updateWaveCompletion(dt: Float) {
        if (!waveActive || spawnLeft > 0 || enemies.isNotEmpty()) return
        clearTimer += dt
        if (clearTimer < 1.4f) return
        waveActive = false
        val bonus = 14 + wave * 3
        save.addHoney(bonus)
        score += 120 + wave * 25
        sound.play(Sfx.WAVE)
        state = GameState.SHOP
        listener?.onWaveCleared(wave, bonus)
    }

    private fun gameOver() {
        state = GameState.GAMEOVER
        paused = true
        sound.play(Sfx.EXPLODE)
        shake(1.8f)
        save.addRecord(score, wave, wave > 20)
        listener?.onGameOver(score, wave)
    }

    private fun nearestEnemy(from: Vec3, range: Float): Enemy? {
        var best: Enemy? = null
        var bestDist = range
        for (e in enemies) {
            val d = from.distanceTo(e.pos)
            if (d < bestDist) {
                bestDist = d
                best = e
            }
        }
        return best
    }

    private fun burst(
        x: Float, y: Float, z: Float,
        count: Int, speed: Float, size: Float,
        r: Float, g: Float, b: Float
    ) {
        repeat(count) {
            val p = particles[particleCursor]
            particleCursor = (particleCursor + 1) % particles.size
            val a = Random.nextFloat() * 6.283185f
            val el = (Random.nextFloat() - 0.35f) * 1.6f
            val power = speed * (0.35f + Random.nextFloat() * 0.65f)
            p.active = true
            p.x = x
            p.y = y
            p.z = z
            p.vx = cos(a) * power
            p.vy = el * power
            p.vz = sin(a) * power
            p.maxLife = 0.35f + Random.nextFloat() * 0.65f
            p.life = p.maxLife
            p.size = size * (0.6f + Random.nextFloat() * 0.9f)
            p.r = r
            p.g = g
            p.b = b
            p.gravity = 2.4f
            p.drag = 1.8f
        }
    }

    private fun updateParticles(dt: Float) {
        for (p in particles) {
            if (!p.active) continue
            p.life -= dt
            if (p.life <= 0f) {
                p.active = false
                continue
            }
            p.vy -= p.gravity * dt
            val k = max(0f, 1f - p.drag * dt)
            p.vx *= k
            p.vz *= k
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.z += p.vz * dt
            if (p.y < 0.05f) {
                p.y = 0.05f
                p.vy = abs(p.vy) * 0.25f
            }
        }
    }

    private fun shake(power: Float) {
        shakeRequest = max(shakeRequest, power)
    }

    private fun showMessage(text: String, duration: Float) {
        message = text
        messageTimer = duration
    }

    private fun updateHud(dt: Float) {
        hudTimer -= dt
        if (hudTimer > 0f) return
        hudTimer = 0.08f
        listener?.onHud(
            HudState(
                wave = wave,
                score = score,
                honey = save.honey,
                hiveHp = hiveHp,
                hiveMax = hiveMax,
                playerHp = player.hp,
                ult = ultCharge,
                dashReady = if (save.turbo > 0) 1f - (player.dashCd / max(2.4f, 6.4f - save.turbo * 0.45f)) else 1f - player.dashCd / 6.4f,
                enemies = enemies.size + spawnLeft,
                survival = wave > 20,
                message = if (messageTimer > 0f) message else "",
                boss = bossAlive
            )
        )
    }
}
