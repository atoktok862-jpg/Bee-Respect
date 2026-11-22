package com.beerespect.game.models

import com.beerespect.game.core.Geometry
import com.beerespect.game.core.Mesh
import com.beerespect.game.core.MeshBuilder
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

fun color(r: Float, g: Float, b: Float, a: Float = 1f): FloatArray = floatArrayOf(r, g, b, a)

data class Part(
    val mesh: Mesh,
    val color: FloatArray = floatArrayOf(1f, 1f, 1f, 1f),
    val px: Float = 0f,
    val py: Float = 0f,
    val pz: Float = 0f,
    val sx: Float = 1f,
    val sy: Float = 1f,
    val sz: Float = 1f,
    val rx: Float = 0f,
    val ry: Float = 0f,
    val rz: Float = 0f,
    val alpha: Float = 1f,
    val emissive: Float = 0f,
    val anim: Int = 0
)

class Model(val parts: List<Part>)

class ModelLibrary {
    private val owned = ArrayList<Mesh>()

    lateinit var sphere: Mesh
    lateinit var lowSphere: Mesh
    lateinit var box: Mesh
    lateinit var cylinder: Mesh
    lateinit var cone: Mesh
    lateinit var disc: Mesh
    lateinit var plane: Mesh
    lateinit var hex: Mesh
    lateinit var crystalMesh: Mesh
    lateinit var grass: Mesh
    lateinit var ground: Mesh
    lateinit var patches: Mesh

    lateinit var beeSkins: Array<Model>
    lateinit var allyBee: Model
    lateinit var beetle: Model
    lateinit var spider: Model
    lateinit var wasp: Model
    lateinit var boss: Model
    lateinit var hive: Model
    lateinit var flower: Model
    lateinit var crystal: Model
    lateinit var pollen: Model
    lateinit var bullet: Model
    lateinit var web: Model
    lateinit var cloud: Model
    lateinit var shadow: Model
    lateinit var shield: Model

    fun init() {
        sphere = keep(MeshBuilder.upload(MeshBuilder.sphere(1f, 12, 16, WHITE)))
        lowSphere = keep(MeshBuilder.upload(MeshBuilder.sphere(1f, 8, 10, WHITE)))
        box = keep(MeshBuilder.upload(MeshBuilder.box(1f, 1f, 1f, WHITE)))
        cylinder = keep(MeshBuilder.upload(MeshBuilder.cylinder(1f, 1f, 1f, 10, WHITE)))
        cone = keep(MeshBuilder.upload(MeshBuilder.cone(1f, 1f, 8, WHITE)))
        disc = keep(MeshBuilder.upload(MeshBuilder.disc(1f, 20, WHITE)))
        plane = keep(MeshBuilder.upload(MeshBuilder.plane(1f, 1f, WHITE)))
        hex = keep(MeshBuilder.upload(MeshBuilder.cylinder(1f, 1f, 1f, 6, WHITE)))
        crystalMesh = keep(MeshBuilder.upload(MeshBuilder.octahedron(1f, 1.8f, WHITE)))
        ground = keep(MeshBuilder.upload(MeshBuilder.plane(260f, 260f, color(0.3f, 0.58f, 0.22f))))
        patches = buildPatches()

        val random = Random(777)
        val blades = ArrayList<Geometry>()
        for (k in 0 until 520) {
            val angle = random.nextDouble() * 2.0 * PI
            val radius = 9.0 + random.nextDouble() * 62.0
            val tint = when (random.nextInt(5)) {
                0 -> color(0.29f, 0.55f, 0.18f)
                1 -> color(0.36f, 0.63f, 0.21f)
                2 -> color(0.23f, 0.48f, 0.16f)
                3 -> color(0.44f, 0.68f, 0.24f)
                else -> color(0.32f, 0.59f, 0.2f)
            }
            val blade = MeshBuilder.blade(
                0.08f + random.nextFloat() * 0.1f,
                0.35f + random.nextFloat() * 0.75f,
                tint
            )
            offsetGeometry(blade, (cos(angle) * radius).toFloat(), 0f, (sin(angle) * radius).toFloat())
            blades.add(blade)
        }
        grass = keep(MeshBuilder.upload(MeshBuilder.merge(*blades.toTypedArray())))

        beeSkins = arrayOf(
            bee(color(1f, 0.78f, 0.14f), color(0.18f, 0.11f, 0.03f), color(0.92f, 0.97f, 1f, 0.55f), color(0.1f, 0.08f, 0.06f), color(0.64f, 0.68f, 0.7f), color(1f, 0.8f, 0.15f)),
            bee(color(0.1f, 0.82f, 0.95f), color(0.05f, 0.1f, 0.3f), color(0.4f, 1f, 0.95f, 0.6f), color(1f, 0.2f, 0.7f), color(0.14f, 0.35f, 0.5f), color(0.2f, 1f, 0.9f), true, 0.5f),
            bee(color(0.55f, 0.24f, 0.75f), color(1f, 0.78f, 0.18f), color(1f, 0.92f, 0.6f, 0.55f), color(0.1f, 0.04f, 0.15f), color(1f, 0.76f, 0.16f), color(1f, 0.8f, 0.15f), true, 0.25f, true),
            bee(color(1f, 0.34f, 0.08f), color(0.32f, 0.06f, 0.02f), color(1f, 0.86f, 0.4f, 0.62f), color(0.2f, 0.02f, 0.01f), color(0.5f, 0.14f, 0.03f), color(1f, 0.55f, 0.08f), false, 0.45f)
        )

        allyBee = Model(listOf(
            Part(sphere, color(1f, 0.82f, 0.2f), 0f, 0f, 0.08f, 0.32f, 0.26f, 0.48f),
            Part(cylinder, color(0.2f, 0.13f, 0.04f), 0f, 0f, 0.06f, 0.33f, 0.14f, 0.33f, rx = HALF_PI),
            Part(lowSphere, color(0.95f, 0.98f, 1f, 0.55f), -0.3f, 0.14f, -0.02f, 0.28f, 0.04f, 0.16f, alpha = 0.55f, anim = 1),
            Part(lowSphere, color(0.95f, 0.98f, 1f, 0.55f), 0.3f, 0.14f, -0.02f, 0.28f, 0.04f, 0.16f, alpha = 0.55f, anim = 2)
        ))

        val bronze = color(0.72f, 0.46f, 0.16f)
        val bronzeDark = color(0.34f, 0.2f, 0.06f)
        beetle = Model(listOf(
            Part(sphere, bronze, 0f, 0f, 0.1f, 0.85f, 0.5f, 1.12f, emissive = 0.05f),
            Part(sphere, bronze, -0.4f, 0.26f, 0.1f, 0.52f, 0.32f, 0.95f),
            Part(sphere, bronze, 0.4f, 0.26f, 0.1f, 0.52f, 0.32f, 0.95f),
            Part(sphere, bronzeDark, 0f, 0.02f, -1.05f, 0.55f, 0.42f, 0.5f),
            Part(cone, bronzeDark, -0.28f, 0.18f, -1.5f, 0.14f, 0.6f, 0.14f, rx = -HALF_PI),
            Part(cone, bronzeDark, 0.28f, 0.18f, -1.5f, 0.14f, 0.6f, 0.14f, rx = -HALF_PI),
            Part(lowSphere, color(1f, 0.25f, 0.12f), -0.43f, 0.15f, -1.35f, 0.1f, 0.1f, 0.1f, emissive = 0.7f),
            Part(lowSphere, color(1f, 0.25f, 0.12f), 0.43f, 0.15f, -1.35f, 0.1f, 0.1f, 0.1f, emissive = 0.7f)
        ))

        val spiderColor = color(0.24f, 0.15f, 0.28f)
        val legs = ArrayList<Part>()
        for (s in 0 until 8) {
            val a = (s.toFloat() / 8f) * PI.toFloat() * 2f
            val side = if (s < 4) -1f else 1f
            legs.add(
                Part(
                    cylinder, spiderColor,
                    cos(a) * 0.85f, 0.05f, sin(a) * 0.85f,
                    0.05f, 0.62f, 0.05f,
                    rz = side * 0.85f, ry = a
                )
            )
        }
        spider = Model(listOf(
            Part(sphere, spiderColor, 0f, 0f, 0.25f, 0.78f, 0.56f, 0.9f),
            Part(sphere, spiderColor, 0f, 0f, -0.62f, 0.44f, 0.36f, 0.48f),
            Part(lowSphere, color(1f, 0.25f, 0.2f), -0.18f, 0.14f, -1f, 0.13f, 0.13f, 0.13f, emissive = 0.8f),
            Part(lowSphere, color(1f, 0.25f, 0.2f), 0.18f, 0.14f, -1f, 0.13f, 0.13f, 0.13f, emissive = 0.8f)
        ) + legs)

        wasp = bee(color(1f, 0.6f, 0.08f), color(0.1f, 0.07f, 0.04f), color(0.9f, 0.95f, 1f, 0.55f), color(1f, 0.3f, 0.05f), color(0.4f, 0.32f, 0.2f), color(1f, 0.4f, 0.05f))

        val bossDark = color(0.32f, 0.12f, 0.4f)
        val bossGold = color(1f, 0.7f, 0.12f)
        val bossParts = ArrayList<Part>()
        bossParts.add(Part(sphere, bossDark, 0f, 0f, 0.5f, 1.9f, 1.35f, 2.7f))
        bossParts.add(Part(sphere, bossDark, 0f, 0f, -1.9f, 1.1f, 0.95f, 1.05f))
        for (s in -1..1 step 2) {
            val sign = s.toFloat()
            bossParts.add(Part(lowSphere, color(0.8f, 0.7f, 1f, 0.6f), sign, 0.55f, 0.1f, 1.3f, 0.1f, 0.65f, alpha = 0.65f, anim = if (sign < 0f) 1 else 2))
            bossParts.add(Part(lowSphere, color(0.8f, 0.7f, 1f, 0.6f), sign * 0.95f, -0.1f, 1.1f, 1f, 0.08f, 0.5f, alpha = 0.6f, anim = if (sign < 0f) 2 else 1))
            bossParts.add(Part(lowSphere, color(1f, 0.08f, 0.04f), sign * 0.5f, 0.25f, -2.6f, 0.22f, 0.22f, 0.22f, emissive = 1f))
            bossParts.add(Part(cone, bossGold, sign * 0.6f, 1.3f, -2.05f, 0.22f, 0.9f, 0.22f, rx = -HALF_PI))
        }
        for (k in 0 until 5) {
            bossParts.add(Part(cone, bossGold, -1.2f + k * 0.6f, 1.2f, -2.2f + kotlin.math.abs(2 - k) * 0.25f, 0.18f, 0.65f + (2 - kotlin.math.abs(2 - k)) * 0.12f, 0.18f))
        }
        bossParts.add(Part(cone, bossGold, 0f, 0f, 3.2f, 0.35f, 1.3f, 0.35f, rx = HALF_PI))
        boss = Model(bossParts)

        val gold = color(1f, 0.72f, 0.12f)
        val honey = color(0.95f, 0.5f, 0.06f)
        hive = Model(listOf(
            Part(hex, gold, 0f, 0.55f, 0f, 5.4f, 1.1f, 5.4f),
            Part(hex, gold, 0f, 1.6f, 0f, 4.6f, 1.1f, 4.6f),
            Part(hex, honey, 0f, 2.65f, 0f, 3.7f, 1.1f, 3.7f),
            Part(hex, gold, 0f, 3.6f, 0f, 2.7f, 0.85f, 2.7f),
            Part(box, color(0.15f, 0.08f, 0.02f), 0f, 0.85f, -5.05f, 1.5f, 1f, 0.18f),
            Part(cylinder, honey, 0f, 4.15f, 0f, 0.55f, 0.3f, 0.55f),
            Part(lowSphere, color(1f, 0.95f, 0.35f), 0f, 4.55f, 0f, 0.4f, 0.5f, 0.4f, emissive = 0.9f)
        ))

        val petal = color(1f, 0.5f, 0.72f)
        val petals = ArrayList<Part>()
        for (k in 0 until 6) {
            val a = k.toFloat() * PI.toFloat() * 2f / 6f
            petals.add(Part(lowSphere, petal, cos(a) * 0.5f, 1.05f, sin(a) * 0.5f, 0.34f, 0.12f, 0.5f, ry = -a))
        }
        flower = Model(listOf(
            Part(cylinder, color(0.25f, 0.6f, 0.24f), 0f, 0.5f, 0f, 0.07f, 1f, 0.07f),
            Part(lowSphere, color(0.25f, 0.6f, 0.24f), -0.28f, 0.42f, 0f, 0.3f, 0.06f, 0.15f, rz = 0.4f),
            Part(lowSphere, color(0.25f, 0.6f, 0.24f), 0.28f, 0.55f, 0f, 0.3f, 0.06f, 0.15f, rz = -0.4f)
        ) + petals + listOf(Part(sphere, color(1f, 0.9f, 0.2f), 0f, 1.08f, 0f, 0.22f, 0.2f, 0.22f, emissive = 0.7f)))

        crystal = Model(listOf(Part(crystalMesh, color(1f, 0.8f, 0.2f), emissive = 0.7f, alpha = 0.9f)))
        pollen = Model(listOf(Part(crystalMesh, color(1f, 0.88f, 0.25f), emissive = 1f)))
        bullet = Model(listOf(Part(lowSphere, color(1f, 0.9f, 0.25f), emissive = 1f, sz = 0.36f, sx = 0.16f, sy = 0.16f)))
        web = Model(listOf(Part(lowSphere, color(0.9f, 0.95f, 1f), alpha = 0.55f, emissive = 0.2f)))
        cloud = Model(listOf(
            Part(lowSphere, WHITE, -2.6f, 0f, 0f, 2.6f, 1.1f, 1.8f, alpha = 0.48f),
            Part(lowSphere, WHITE, 0f, 0.5f, 0f, 3.2f, 1.5f, 2.2f, alpha = 0.52f),
            Part(lowSphere, WHITE, 2.7f, 0f, 0f, 2.5f, 1f, 1.7f, alpha = 0.46f)
        ))
        shadow = Model(listOf(Part(disc, color(0.05f, 0.12f, 0.03f), alpha = 0.22f)))
        shield = Model(listOf(Part(sphere, color(0.4f, 0.9f, 1f), alpha = 0.24f, emissive = 0.8f)))
    }

    private fun buildPatches(): Mesh {
        val random = Random(991)
        val list = ArrayList<Geometry>()
        for (k in 0 until 26) {
            val a = random.nextDouble() * 2.0 * PI
            val r = 12.0 + random.nextDouble() * 58.0
            val size = 2.5f + random.nextFloat() * 5f
            val tint = if (k % 3 == 0) color(0.27f, 0.53f, 0.18f) else color(0.36f, 0.61f, 0.23f)
            val patch = MeshBuilder.disc(size, 12, tint)
            offsetGeometry(patch, (cos(a) * r).toFloat(), 0.015f, (sin(a) * r).toFloat())
            list.add(patch)
        }
        return keep(MeshBuilder.upload(MeshBuilder.merge(*list.toTypedArray())))
    }

    private fun offsetGeometry(geometry: Geometry, x: Float, y: Float, z: Float) {
        val v = geometry.vertices
        var i = 0
        while (i < v.size) {
            v[i] += x
            v[i + 1] += y
            v[i + 2] += z
            i += 10
        }
    }

    private fun bee(
        body: FloatArray,
        stripe: FloatArray,
        wing: FloatArray,
        eye: FloatArray,
        helmet: FloatArray,
        glowColor: FloatArray,
        crown: Boolean = false,
        glow: Float = 0f,
        bigCrown: Boolean = false
    ): Model {
        val parts = ArrayList<Part>()
        parts.add(Part(sphere, body, 0f, 0f, 0.16f, 0.5f, 0.42f, 0.82f, emissive = glow))
        parts.add(Part(cylinder, stripe, 0f, 0f, -0.06f, 0.5f, 0.11f, 0.5f, rx = HALF_PI))
        parts.add(Part(cylinder, stripe, 0f, 0f, 0.42f, 0.47f, 0.1f, 0.47f, rx = HALF_PI))
        parts.add(Part(sphere, body, 0f, 0.02f, -0.76f, 0.37f, 0.35f, 0.36f))
        parts.add(Part(lowSphere, eye, -0.19f, 0.1f, -1.03f, 0.1f, 0.12f, 0.1f, emissive = 0.25f))
        parts.add(Part(lowSphere, eye, 0.19f, 0.1f, -1.03f, 0.1f, 0.12f, 0.1f, emissive = 0.25f))
        parts.add(Part(cone, stripe, 0f, 0f, 1.02f, 0.13f, 0.5f, 0.13f, rx = HALF_PI))
        parts.add(Part(lowSphere, wing, -0.52f, 0.32f, 0f, 0.55f, 0.05f, 0.3f, alpha = wing[3], anim = 1))
        parts.add(Part(lowSphere, wing, 0.52f, 0.32f, 0f, 0.55f, 0.05f, 0.3f, alpha = wing[3], anim = 2))
        parts.add(Part(lowSphere, helmet, 0f, 0.3f, -0.78f, 0.42f, 0.25f, 0.4f))
        parts.add(Part(box, glowColor, 0f, 0.12f, -1.08f, 0.56f, 0.1f, 0.14f, emissive = glow + 0.15f))
        if (crown) {
            val count = if (bigCrown) 5 else 3
            for (k in 0 until count) {
                val x = (k - (count - 1) / 2f) * 0.24f
                parts.add(Part(cone, glowColor, x, 0.62f, -0.78f, 0.12f, 0.4f + (count - k) * 0.05f, 0.12f, emissive = glow))
            }
        }
        return Model(parts)
    }

    private fun keep(mesh: Mesh): Mesh {
        owned.add(mesh)
        return mesh
    }

    fun dispose() {
        for (mesh in owned) mesh.dispose()
        owned.clear()
    }

    companion object {
        val WHITE: FloatArray = floatArrayOf(1f, 1f, 1f, 1f)
        val HALF_PI: Float = (PI / 2f).toFloat()
    }
}
