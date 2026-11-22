package com.beerespect.game.render

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
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

class GLRenderer(
    val game: GameEngine,
    private val onFatalError: ((Throwable) -> Unit)? = null
) : GLSurfaceView.Renderer {
    private var lastTime = 0L
    private var width = 1280
    private var height = 720

    var models: ModelLibrary? = null
        private set
    private var solid: ShaderProgram? = null
    private val camera = Camera3D()
    @Volatile private var initialized = false
    @Volatile private var fatalReported = false

    private val cloudPositions = arrayOf(
        floatArrayOf(-38f, 18f, -24f), floatArrayOf(36f, 20f, -18f),
        floatArrayOf(-14f, 24f, -46f), floatArrayOf(50f, 22f, -36f),
        floatArrayOf(-52f, 19f, -8f)
    )

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        try {
            initialized = false
            GLES30.glClearColor(0.52f, 0.79f, 0.93f, 1f)
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glDepthFunc(GLES30.GL_LEQUAL)
            GLES30.glEnable(GLES30.GL_BLEND)
            GLES30.glBlendFunc(GLES30.GL_SRC_ALPHA, GLES30.GL_ONE_MINUS_SRC_ALPHA)
            GLES30.glEnable(GLES30.GL_CULL_FACE)
            GLES30.glCullFace(GLES30.GL_BACK)

            val program = ShaderProgram(VERTEX_SHADER, FRAGMENT_SHADER)
            program.build()
            solid = program

            val library = ModelLibrary()
            library.init()
            models = library

            lastTime = System.nanoTime()
            initialized = true
        } catch (t: Throwable) {
            reportFatal(t)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        try {
            this.width = width
            this.height = height
            val safeW = if (width > 0) width else 1
            val safeH = if (height > 0) height else 1
            GLES30.glViewport(0, 0, safeW, safeH)
            camera.setAspect(safeW.toFloat() / safeH.toFloat().coerceAtLeast(1f))
        } catch (t: Throwable) {
            reportFatal(t)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        try {
            val program = solid ?: return
            val lib = models ?: return
            if (!initialized || fatalReported) return

            val now = System.nanoTime()
            if (lastTime == 0L) lastTime = now
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
            program.use()
            program.setMat4("uViewProjection", camera.viewProjection)
            program.setVec3("uLightDir", 0.45f, 0.82f, 0.22f)
            program.setVec3("uLightColor", 1f, 0.98f, 0.9f)
            program.setVec3("uAmbient", 0.42f, 0.48f, 0.55f)

            drawEnvironment(program, lib)
            drawHive(program, lib)
            drawFlowers(program, lib)
            drawCrystals(program, lib)
            drawPollens(program, lib)
            drawShadows(program, lib)
            drawPlayer(program, lib)
            drawAllies(program, lib)
            drawEnemies(program, lib)
            drawBullets(program, lib)
            drawParticles(program, lib)
            drawUltEffect(program, lib)
        } catch (t: Throwable) {
            reportFatal(t)
        }
    }

    private fun reportFatal(t: Throwable) {
        if (fatalReported) return
        fatalReported = true
        Log.e("BeeGL", "Ошибка рендера", t)
        onFatalError?.invoke(t)
    }

    fun dispose() {
        initialized = false
        try { models?.dispose() } catch (_: Throwable) {}
        models = null
        try { solid?.dispose() } catch (_: Throwable) {}
        solid = null
    }

    private fun drawEnvironment(program: ShaderProgram, lib: ModelLibrary) {
        drawMesh(program, lib.ground, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 0.3f, 0.58f, 0.22f, 1f, 0f)
        drawMesh(program, lib.patches, 0f, 0.02f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
        drawMesh(program, lib.grass, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 1f, 0f)
        drawMesh(program, lib.disc, 0f, 0.01f, 0f, 42f, 1f, 42f, 1f, 0f, 0f, 0f, 0.34f, 0.62f, 0.24f, 1f, 0f)
        for ((index, c) in cloudPositions.withIndex()) {
            val drift = game.time * (0.18f + index * 0.03f)
            val x = c[0] + sin(drift) * 4f
            val z = c[2] + cos(drift * 0.7f) * 3f
            drawModel(program, lib.cloud, x, c[1], z, 1.4f, 1.4f, 1.4f, 0.12f, drift, 0.05f, 1f, 0f)
        }
    }

    private fun drawHive(program: ShaderProgram, lib: ModelLibrary) {
        drawModel(program, lib.hive, 0f, 0f, -6f, 1f, 1f, 1f, 0f, game.time * 0.08f, 0f)
        val glow = 0.7f + sin(game.time * 2f) * 0.3f
        drawMesh(program, lib.sphere, 0f, 4.55f, -6f, 0.42f, 0.52f, 0.42f, 1f, 0f, 0f, 0f, 1f, 0.95f, 0.35f, glow, 1.5f)
    }

    private fun drawFlowers(program: ShaderProgram, lib: ModelLibrary) {
        for (flower in game.flowers) {
            val sway = sin(flower.phase + game.time) * 0.08f
            drawModel(program, lib.flower, flower.x, 0f, flower.z, 1f, 1f, 1f, sway, flower.phase, 0f)
            if (flower.bloom > 0f) {
                val s = 0.6f + flower.bloom * 0.8f
                drawMesh(program, lib.lowSphere, flower.x, 1.4f, flower.z, s, s, s, 1f, 0f, game.time * 1.5f, 0f, 1f, 0.95f, 0.2f, flower.bloom, 1.5f)
            }
        }
    }

    private fun drawCrystals(program: ShaderProgram, lib: ModelLibrary) {
        for (crystal in game.crystals) {
            val y = crystal.y + sin(crystal.phase + game.time * 1.5f) * 0.4f
            val rot = game.time * 1.2f + crystal.phase
            drawMesh(program, lib.crystalMesh, crystal.x, y, crystal.z, 0.55f, 0.8f, 0.55f, 1f, 0f, rot, 0.3f, 1f, 0.8f, 0.2f, 0.9f, 0.8f)
        }
    }

    private fun drawPollens(program: ShaderProgram, lib: ModelLibrary) {
        for (pollen in game.pollens) {
            drawMesh(
                program, lib.crystalMesh,
                pollen.pos.x, pollen.pos.y + sin(pollen.phase + game.time * 3f) * 0.15f, pollen.pos.z,
                0.2f, 0.3f, 0.2f, 1f, 0f, game.time * 4f + pollen.phase, 0.2f,
                1f, 0.9f, 0.3f, 1f, 1.2f
            )
        }
    }

    private fun drawShadows(program: ShaderProgram, lib: ModelLibrary) {
        drawMesh(program, lib.disc, game.player.pos.x, 0.03f, game.player.pos.z, 1.9f, 1f, 1.9f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0.22f, 0f)
        for (ally in game.allies) {
            drawMesh(program, lib.disc, ally.pos.x, 0.03f, ally.pos.z, 0.65f, 1f, 0.65f, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0.18f, 0f)
        }
        for (e in game.enemies) {
            val size = if (e.kind == 3) 3.8f else 1.5f
            drawMesh(program, lib.disc, e.pos.x, 0.03f, e.pos.z, size, 1f, size, 1f, 0f, 0f, 0f, 1f, 1f, 1f, 0.2f, 0f)
        }
    }

    private fun drawPlayer(program: ShaderProgram, lib: ModelLibrary) {
        val p = game.player
        val flash = if (p.hitFlash > 0f) 0.9f else 0f
        val skinIndex = game.save.selectedSkin.coerceIn(0, lib.beeSkins.size - 1)
        val model = lib.beeSkins[skinIndex]
        val invuln = if (p.invuln > 0f && ((game.time * 10f).toInt() % 2 == 0)) 0.35f else 1f
        drawModel(program, model, p.pos.x, p.pos.y, p.pos.z, 1.25f, 1.25f, 1.25f, 0.08f, p.heading, 0f, invuln, flash)
        if (p.shield > 0f) {
            val s = 1.7f + sin(game.time * 8f) * 0.1f
            drawModel(program, lib.shield, p.pos.x, p.pos.y, p.pos.z, s, s, s, game.time * 0.8f, game.time * 1.2f, 0f, p.shield * 0.5f, 0f)
        }
        if (game.ultActive()) {
            val r = 3.5f + sin(game.time * 14f) * 0.6f
            drawMesh(program, lib.disc, p.pos.x, 0.06f, p.pos.z, r, 1f, r, 1f, 0f, -game.time * 3f, 0f, 1f, 0.85f, 0.2f, 0.35f, 1.3f)
        }
    }

    private fun drawAllies(program: ShaderProgram, lib: ModelLibrary) {
        for (ally in game.allies) {
            drawModel(program, lib.allyBee, ally.pos.x, ally.pos.y, ally.pos.z, 1f, 1f, 1f, sin(ally.phase) * 0.1f, game.time * 4f, 0f)
            drawMesh(program, lib.lowSphere, ally.pos.x - 0.3f, ally.pos.y + 0.14f, ally.pos.z - 0.02f, 0.28f, 0.04f, 0.16f, 1f, sin(game.time * 30f) * 0.3f, 0f, 0f, 0.95f, 0.98f, 1f, 0.55f, 0f)
            drawMesh(program, lib.lowSphere, ally.pos.x + 0.3f, ally.pos.y + 0.14f, ally.pos.z - 0.02f, 0.28f, 0.04f, 0.16f, 1f, -sin(game.time * 30f) * 0.3f, 0f, 0f, 0.95f, 0.98f, 1f, 0.55f, 0f)
        }
    }

    private fun drawEnemies(program: ShaderProgram, lib: ModelLibrary) {
        for (e in game.enemies) {
            val flash = if (e.flash > 0f) 1f else 0f
            when (e.kind) {
                0 -> drawModel(program, lib.beetle, e.pos.x, e.pos.y, e.pos.z, 1.1f, 1.1f, 1.1f, sin(e.phase) * 0.06f, e.yaw, 0f, 1f, flash)
                1 -> drawModel(program, lib.spider, e.pos.x, e.pos.y, e.pos.z, 1.2f, 1.2f, 1.2f, sin(e.phase * 0.5f) * 0.04f, e.yaw, 0f, 1f, flash)
                2 -> drawModel(program, lib.wasp, e.pos.x, e.pos.y, e.pos.z, 1f, 1f, 1f, sin(e.phase) * 0.1f, e.yaw, 0f, 1f, flash)
                3 -> drawModel(program, lib.boss, e.pos.x, e.pos.y, e.pos.z, 1.2f, 1.2f, 1.2f, 0.05f, e.yaw + sin(game.time) * 0.1f, 0f, 1f, flash)
            }
        }
    }

    private fun drawBullets(program: ShaderProgram, lib: ModelLibrary) {
        for (b in game.bullets) {
            if (b.friendly) {
                drawModel(program, lib.bullet, b.pos.x, b.pos.y, b.pos.z, 1f, 1f, 1f, 0f, atan2(b.vel.x, -b.vel.z), 0f, 1f, 0f)
            } else {
                val red = if (b.kind == 3) 1f else 0.6f
                val green = if (b.kind == 3) 0.2f else 0.95f
                val blue = if (b.kind == 3) 0.1f else 1f
                drawMesh(program, lib.web.parts[0].mesh, b.pos.x, b.pos.y, b.pos.z, 0.55f, 0.55f, 0.55f, 1f, game.time * 4f, b.life * 2f, 0f, red, green, blue, 0.7f, 0.4f)
            }
        }
    }

    private fun drawParticles(program: ShaderProgram, lib: ModelLibrary) {
        for (p in game.particles) {
            if (!p.active) continue
            val a = (p.life / p.maxLife).coerceIn(0f, 1f)
            drawMesh(
                program, lib.lowSphere, p.x, p.y, p.z,
                p.size * a, p.size * a, p.size * a,
                1f, 0f, 0f, 0f,
                p.r, p.g, p.b, a, 0.6f
            )
        }
    }

    private fun drawUltEffect(program: ShaderProgram, lib: ModelLibrary) {
        if (!game.ultActive()) return
        val t = game.time
        for (k in 0 until 6) {
            val a = t * 2.4f + k * 1.047f
            val x = cos(a) * (3.5f + sin(t * 3f) * 0.7f)
            val z = sin(a) * (3.5f + sin(t * 3f) * 0.7f)
            drawModel(program, lib.allyBee, game.player.pos.x + x, game.player.pos.y + 0.5f + sin(t * 6f + k) * 0.4f, game.player.pos.z + z, 0.7f, 0.7f, 0.7f, 0f, -a, 0f, 0.9f, 0.5f)
        }
    }

    private fun drawModel(
        program: ShaderProgram,
        model: com.beerespect.game.models.Model,
        px: Float, py: Float, pz: Float,
        sx: Float, sy: Float, sz: Float,
        rx: Float, ry: Float, rz: Float,
        globalAlpha: Float = 1f,
        flash: Float = 0f
    ) {
        for (part in model.parts) {
            drawPart(
                program, part.mesh, part.color,
                px + part.px, py + part.py, pz + part.pz,
                sx * part.sx, sy * part.sy, sz * part.sz,
                rx + part.rx, ry + part.ry, rz + part.rz,
                part.alpha * globalAlpha, part.emissive, part.anim, flash
            )
        }
    }

    private fun drawPart(
        program: ShaderProgram,
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
        val modelMat = Mat4.compose(px, py, pz, arx, ary, arz, osx, osy, osz)
        program.setMat4("uModel", modelMat)
        val cr = if (color.size > 0) color[0] else 1f
        val cg = if (color.size > 1) color[1] else 1f
        val cb = if (color.size > 2) color[2] else 1f
        val ca = if (color.size > 3) color[3] else 1f
        val fr = (cr + flash).coerceIn(0f, 1f)
        val fg = (cg + flash).coerceIn(0f, 1f)
        val fb = (cb + flash).coerceIn(0f, 1f)
        program.setVec4("uColor", fr, fg, fb, (alpha * ca).coerceIn(0f, 1f))
        program.setFloat("uEmissive", (emissive + flash * 0.8f).coerceIn(0f, 2f))
        mesh.draw()
    }

    private fun drawMesh(
        program: ShaderProgram,
        mesh: com.beerespect.game.core.Mesh,
        px: Float, py: Float, pz: Float,
        sx: Float, sy: Float, sz: Float,
        alpha: Float,
        rx: Float, ry: Float, rz: Float,
        r: Float, g: Float, b: Float,
        a: Float,
        emissive: Float
    ) {
        program.setMat4("uModel", Mat4.compose(px, py, pz, rx, ry, rz, sx, sy, sz))
        program.setVec4("uColor", r, g, b, (a * alpha).coerceIn(0f, 1f))
        program.setFloat("uEmissive", emissive)
        mesh.draw()
    }

    companion object {
        private const val VERTEX_SHADER = """
#version 300 es
precision highp float;
layout(location = 0) in vec3 aPosition;
layout(location = 1) in vec3 aNormal;
layout(location = 2) in vec4 aColor;
uniform mat4 uModel;
uniform mat4 uViewProjection;
out vec3 vNormal;
out vec3 vWorldPos;
out vec4 vColor;
void main() {
    vec4 world = uModel * vec4(aPosition, 1.0);
    vWorldPos = world.xyz;
    vNormal = mat3(uModel) * aNormal;
    vColor = aColor;
    gl_Position = uViewProjection * world;
}
"""
        private const val FRAGMENT_SHADER = """
#version 300 es
precision mediump float;
in vec3 vNormal;
in vec3 vWorldPos;
in vec4 vColor;
uniform vec3 uLightDir;
uniform vec3 uLightColor;
uniform vec3 uAmbient;
uniform vec4 uColor;
uniform float uEmissive;
out vec4 fragColor;
void main() {
    vec3 normal = normalize(vNormal);
    float diffuse = max(dot(normal, normalize(uLightDir)), 0.0);
    vec3 base = uColor.rgb * vColor.rgb;
    vec3 color = base * (uAmbient + uLightColor * diffuse) + base * uEmissive;
    float fog = smoothstep(20.0, 140.0, length(vWorldPos));
    vec3 fogColor = vec3(0.74, 0.86, 0.96);
    color = mix(color, fogColor, fog * 0.22);
    fragColor = vec4(color, uColor.a * vColor.a);
}
"""
    }
}
