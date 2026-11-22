package com.beerespect.game.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import com.beerespect.game.GameEngine
import com.beerespect.game.core.Camera3D
import com.beerespect.game.core.Mat4
import com.beerespect.game.core.ShaderProgram
import com.beerespect.game.core.Vec3
import com.beerespect.game.models.ModelLibrary
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class GLRenderer(val game: GameEngine) : GLSurfaceView.Renderer {
    private var lastTime = 0L
    private var width = 1280
    private var height = 720

    lateinit var models: ModelLibrary
        private set
    private lateinit var solid: ShaderProgram
    private val camera = Camera3D()
    private val modelMatrix = FloatArray(16)

    private val cloudPositions = arrayOf(
        floatArrayOf(-38f, 18f, -24f), floatArrayOf(36f, 20f, -18f),
        floatArrayOf(-14f, 24f, -46f), floatArrayOf(50f, 22f, -36f),
        floatArrayOf(-52f, 19f, -8f)
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES30.glClearColor(0.52f, 0.79f, 0.93f, 1f)
        GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        GLES30.glDepthFunc(GLES30.GL_LEQUAL)
        GLES30.glEnable(GLES30.GL_BLEND)
        GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
        GLES30.glEnable(GLES30.GL_CULL_FACE)
        GLES30.glCullFace(GLES30.GL_BACK)

        solid = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        solid.build()
        models = ModelLibrary()
        models.init()
        lastTime = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        this.width = width
        this.height = height
        GLES30.glViewport(0, 0, width, height)
        camera.setAspect(width.toFloat() / height.toFloat().coerceAtLeast(1f))
    }

    override fun onDrawFrame(gl: GL10?) {
        val now = System.nanoTime()
        val delta = ((now - lastTime) / 1_000_000_000f).coerceIn(0f, 0.05f)
        lastTime = now

        game.update(delta)
        val shake = game.consumeShake()
        if (shake > 0f) camera.addShake(shake)

        val player = game.player
        val behindX = player.pos.x - sin(player.heading) * 16f
        val behindZ = player.pos.z + cos(player.heading) * 16f
        val wantedEye = Vec3(behindX, 10f + player.pos.y * 0.15f, behindZ + 14f)
        val wantedTarget = Vec3(player.pos.x, player.pos.y * 0.55f + 2.5f, player.pos.z - 3f)
        camera.update(delta, wantedEye, wantedTarget)

        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        solid.use()
        solid.setMat4("uViewProjection", camera.viewProjection)
        solid.setVec3("uLightDir", 0.45f, 0.82f, 0.22f)
        solid.setVec3("uLightColor", 1f, 0.98f, 0.9f)
        solid.setVec3("uAmbient", 0.42f, 0.48f, 0.55f)

        drawEnvironment()
        drawHive()
        drawFlowers()
        drawCrystals()
        drawPollens()
        drawShadows()
        drawPlayer()
        drawAllies()
        drawEnemies()
        drawBullets()
        drawParticles()
        drawUltEffect()
    }

    private fun drawEnvironment() {
        drawMesh(models.ground, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0.3f, 0.58f, 0.22f, 1f, 0f)
        drawMesh(models.patches, 0f, 0.02f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
        drawMesh(models.grass, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
        drawMesh(models.disc, 0f, 0.01f, 0f, 42f, 1f, 42f, 1f, 0f, 0f, 0f, 0.34f, 0.62f, 0.24f, 1f, 0f)
        for ((index, c) in cloudPositions.withIndex()) {
            val drift = game.time * (0.18f + index * 0.03f)
            val x = c[0] + sin(drift) * 4f
            val z = c[2] + cos(drift * 0.7f) * 3f
            drawMesh(models.cloud, x, c[1], z, 1.4f, 1.4f, 1.4f, 1f, 0.12f, drift, 0.05f, 1f, 1f, 1f, 0.48f, 0f)
        }
    }

    private fun drawHive() {
        drawModel(models.hive, 0f, 0f, -6f, 1f, 1f, 1f, 0f, game.time * 0.08f, 0f)
        val glow = 0.7f + sin(game.time * 2f) * 0.3f
        drawMesh(models.sphere, 0f, 4.55f, -6f, 0.42f, 0.52f, 0.42f, 1f, 0f, 0f, 0f, 1f, 0.95f, 0.35f, glow, 1.5f)
    }

    private fun drawFlowers() {
        for (flower in game.flowers) {
            val sway = sin(flower.phase + game.time) * 0.08f
            drawModel(models.flower, flower.x, 0f, flower.z, 1f, 1f, 1f, sway, flower.phase, 0f)
            if (flower.bloom > 0f) {
                val s = 0.6f + flower.bloom * 0.8f
                drawMesh(models.lowSphere, flower.x, 1.4f, flower.z, s, s, s, 1f, 0f, game.time * 1.5f, 0f, 1f, 0.95f, 0.2f, flower.bloom, 1.5f)
            }
        }
    }

    private fun drawCrystals() {
        for (crystal in game.crystals) {
            val y = crystal.y + sin(crystal.phase + game.time * 1.5f) * 0.4f
            val rot = game.time * 1.2f + crystal.phase
            drawMesh(models.crystalMesh, crystal.x, y, crystal.z, 0.55f, 0.8f, 0.55f, 1f, 0f, rot, 0.3f, 1f, 0.8f, 0.2f, 0.9f, 0.8f)
        }
    }

    private fun drawPollens() {
        for (pollen in game.pollens) {
            drawMesh(
                models.crystalMesh,
                pollen.pos.x, pollen.pos.y + sin(pollen.phase + game.time * 3f) * 0.15f, pollen.pos.z,
                0.2f, 0.3f, 0.2f, 1f, 0f, game.time * 4f + pollen.phase, 0.2f,
                1f, 0.9f, 0.3f, 1f, 1.2f
            )
        }
    }

    private fun drawShadows() {
        drawMesh(models.disc, game.player.pos.x, 0.03f, game.player.pos.z, 1.9f, 1f, 1.9f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0.22f, 0f)
        for (ally in game.allies) {
            drawMesh(models.disc, ally.pos.x, 0.03f, ally.pos.z, 0.65f, 1f, 0.65f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0.18f, 0f)
        }
        for (e in game.enemies) {
            val size = if (e.kind == 3) 3.8f else 1.5f
            drawMesh(models.disc, e.pos.x, 0.03f, e.pos.z, size, 1f, size, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0.2f, 0f)
        }
    }

    private fun drawPlayer() {
        val p = game.player
        val flash = if (p.hitFlash > 0f) 0.9f else 0f
        val model = models.beeSkins[game.save.selectedSkin.coerceIn(0, models.beeSkins.size - 1)]
        val invuln = if (p.invuln > 0f && ((game.time * 10f).toInt() % 2 == 0)) 0.35f else 1f
        drawModel(model, p.pos.x, p.pos.y, p.pos.z, 1.25f, 1.25f, 1.25f, 0.08f, p.heading, 0f, invuln, flash)
        if (p.shield > 0f) {
            val s = 1.7f + sin(game.time * 8f) * 0.1f
            drawMesh(models.shield, p.pos.x, p.pos.y, p.pos.z, s, s, s, 1f, game.time * 0.8f, game.time * 1.2f, 0f, 0.4f, 0.9f, 1f, p.shield * 0.5f, 0.9f)
        }
        if (game.ultActive()) {
            val r = 3.5f + sin(game.time * 14f) * 0.6f
            drawMesh(models.disc, p.pos.x, 0.06f, p.pos.z, r, 1f, r, 1f, 0f, -game.time * 3f, 0f, 1f, 0.85f, 0.2f, 0.35f, 1.3f)
        }
    }

    private fun drawAllies() {
        for (ally in game.allies) {
            drawModel(models.allyBee, ally.pos.x, ally.pos.y, ally.pos.z, 1f, 1f, 1f, sin(ally.phase) * 0.1f, game.time * 4f, 0f)
            drawMesh(models.lowSphere, ally.pos.x - 0.3f, ally.pos.y + 0.14f, ally.pos.z - 0.02f, 0.28f, 0.04f, 0.16f, 1f, sin(game.time * 30f) * 0.3f, 0f, 0f, 0.95f, 0.98f, 1f, 0.55f, 0f)
            drawMesh(models.lowSphere, ally.pos.x + 0.3f, ally.pos.y + 0.14f, ally.pos.z - 0.02f, 0.28f, 0.04f, 0.16f, 1f, -sin(game.time * 30f) * 0.3f, 0f, 0f, 0.95f, 0.98f, 1f, 0.55f, 0f)
        }
    }

    private fun drawEnemies() {
        for (e in game.enemies) {
            val flash = if (e.flash > 0f) 1f else 0f
            when (e.kind) {
                0 -> drawModel(models.beetle, e.pos.x, e.pos.y, e.pos.z, 1.1f, 1.1f, 1.1f, sin(e.phase) * 0.06f, e.yaw, 0f, 1f, flash)
                1 -> drawModel(models.spider, e.pos.x, e.pos.y, e.pos.z, 1.2f, 1.2f, 1.2f, sin(e.phase * 0.5f) * 0.04f, e.yaw, 0f, 1f, flash)
                2 -> drawModel(models.wasp, e.pos.x, e.pos.y, e.pos.z, 1f, 1f, 1f, sin(e.phase) * 0.1f, e.yaw, 0f, 1f, flash)
                3 -> drawModel(models.boss, e.pos.x, e.pos.y, e.pos.z, 1.2f, 1.2f, 1.2f, 0.05f, e.yaw + sin(game.time) * 0.1f, 0f, 1f, flash)
            }
        }
    }

    private fun drawBullets() {
        for (b in game.bullets) {
            if (b.friendly) {
                drawMesh(models.bullet, b.pos.x, b.pos.y, b.pos.z, 0.2f, 0.2f, 0.5f, 1f, 0f, atan2(b.vel.x, -b.vel.z), 0f, 1f, 0.9f, 0.2f, 1f, 1.2f)
            } else {
                val color = if (b.kind == 3) floatArrayOf(1f, 0.2f, 0.1f, 1f) else floatArrayOf(0.6f, 0.95f, 1f, 1f)
                drawMesh(models.web, b.pos.x, b.pos.y, b.pos.z, 0.55f, 0.55f, 0.55f, 1f, game.time * 4f, b.life * 2f, 0f, color[0], color[1], color[2], 0.7f, 0.4f)
            }
        }
    }

    private fun drawParticles() {
        for (p in game.particles) {
            if (!p.active) continue
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawMesh(
                models.lowSphere, p.x, p.y, p.z,
                p.size * a, p.size * a, p.size * a,
                1f, 0f, 0f, 0f,
                p.r, p.g, p.b, a, 0.6f
            )
        }
    }

    private fun drawUltEffect() {
        if (!game.ultActive()) return
        val t = game.time
        for (k in 0 until 6) {
            val a = t * 2.4f + k * 1.047f
            val x = cos(a) * (3.5f + sin(t * 3f) * 0.7f)
            val z = sin(a) * (3.5f + sin(t * 3f) * 0.7f)
            drawMesh(models.allyBee, game.player.pos.x + x, game.player.pos.y + 0.5f + sin(t * 6f + k) * 0.4f, game.player.pos.z + z, 0.7f, 0.7f, 0.7f, 0f, -a, 0f, 1f, 1f, 0.85f, 0.2f, 0.9f, 0.5f)
        }
    }

    private fun drawModel(
        model: com.beerespect.game.models.Model,
        px: Float, py: Float, pz: Float,
        sx: Float, sy: Float, sz: Float,
        rx: Float, ry: Float, rz: Float,
        globalAlpha: Float = 1f,
        flash: Float = 0f
    ) {
        for (part in model.parts) {
            drawPart(
                part.mesh, part.color,
                px + part.px, py + part.py, pz + part.pz,
                sx * part.sx, sy * part.sy, sz * part.sz,
                rx + part.rx, ry + part.ry, rz + part.rz,
                part.alpha * globalAlpha, part.emissive, part.anim, flash
            )
        }
    }

    private fun drawPart(
        mesh: com.beerespect.game.core.Mesh,
        color: FloatArray,
        px: Float, py: Float, pz: Float,
        sx: Float, sy: Float, sz: Float,
        rx: Float, ry: Float, rz: Float,
        alpha: Float,
        emissive: Float,
        anim: Int,
        flash: Float
    ) {
        var arx = rx
        var ary = ry
        var arz = rz
        var osx = sx
        var osy = sy
        var osz = sz
        if (anim == 1) {
            ary += sin(game.time * 42f) * 0.4f
            osz *= 0.85f
        } else if (anim == 2) {
            ary -= sin(game.time * 42f) * 0.4f
            osz *= 0.85f
        }
        modelMatrix[0] = 0f
        val model = Mat4.compose(px, py, pz, arx, ary, arz, osx, osy, osz)
        for (i in 0..15) modelMatrix[i] = model[i]
        solid.setMat4("uModel", modelMatrix)
        val fr = (color[0] + flash).coerceIn(0f, 1f)
        val fg = (color[1] + flash).coerceIn(0f, 1f)
        val fb = (color[2] + flash).coerceIn(0f, 1f)
        solid.setVec4("uColor", fr, fg, fb, alpha)
        solid.setFloat("uEmissive", (emissive + flash * 0.8f).coerceIn(0f, 2f))
        mesh.draw()
    }

    private fun drawMesh(
        mesh: com.beerespect.game.core.Mesh,
        px: Float, py: Float, pz: Float,
        sx: Float, sy: Float, sz: Float,
        alpha: Float,
        rx: Float, ry: Float, rz: Float,
        r: Float, g: Float, b: Float,
        a: Float,
        emissive: Float
    ) {
        solid.setMat4("uModel", Mat4.compose(px, py, pz, rx, ry, rz, sx, sy, sz))
        solid.setVec4("uColor", r, g, b, a)
        solid.setFloat("uEmissive", emissive)
        mesh.draw()
    }

    private fun drawMesh(
        model: com.beerespect.game.models.Model,
        px: Float, py: Float, pz: Float,
        sx: Float, sy: Float, sz: Float,
        alpha: Float,
        rx: Float, ry: Float, rz: Float,
        r: Float, g: Float, b: Float,
        a: Float,
        emissive: Float
    ) {
        for (part in model.parts) {
            val tinted = floatArrayOf(
                part.color[0] * r,
                part.color[1] * g,
                part.color[2] * b,
                part.color[3]
            )
            drawPart(
                part.mesh, tinted,
                px + part.px, py + part.py, pz + part.pz,
                sx * part.sx, sy * part.sy, sz * part.sz,
                rx + part.rx, ry + part.ry, rz + part.rz,
                part.alpha * alpha * a, maxOf(part.emissive, emissive), part.anim, 0f
            )
        }
    }

    companion object {
        private const val VERTEX_SHADER = """
attribute vec3 aPosition;
attribute vec3 aNormal;
attribute vec4 aColor;
uniform mat4 uModel;
uniform mat4 uViewProjection;
varying vec3 vNormal;
varying vec3 vWorldPos;
varying vec4 vColor;
void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vWorldPos = world.xyz;
    vNormal = mat3(uModel) * aNormal;
    vColor = aColor;
    gl_Position = uViewProjection * world;
}
"""
        private const val FRAGMENT_SHADER = """
precision mediump float;
varying vec3 vNormal;
varying vec3 vWorldPos;
varying vec4 vColor;
uniform vec3 uLightDir;
uniform vec3 uLightColor;
uniform vec3 uAmbient;
uniform vec4 uColor;
uniform float uEmissive;
void main() {
    vec3 normal = normalize(vNormal);
    float diffuse = max(dot(normal, normalize(uLightDir)), 0.0);
    vec3 base = uColor.rgb * vColor.rgb;
    vec3 color = base * (uAmbient + uLightColor * diffuse) + base * uEmissive;
    float fog = smoothstep(20.0, 140.0, length(vWorldPos));
    vec3 fogColor = vec3(0.74, 0.86, 0.96);
    color = mix(color, fogColor, fog * 0.22);
    gl_FragColor = vec4(color, uColor.a * vColor.a);
}
"""
    }
}

