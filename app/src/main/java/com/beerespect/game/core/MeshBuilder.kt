package com.beerespect.game.core

import android.opengl.GLES30
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.PI

data class Geometry(val vertices: FloatArray, val indices: IntArray)

class Mesh(private val geometry: Geometry) {
    var vao: Int = 0
        private set
    var vbo: Int = 0
        private set
    var ebo: Int = 0
        private set

    val indexCount: Int get() = geometry.indices.size

    fun upload() {
        if (vao != 0) return
        if (geometry.vertices.isEmpty() || geometry.indices.isEmpty()) return
        val arrays = IntArray(1)
        GLES30.glGenVertexArrays(1, arrays, 0)
        vao = arrays[0]
        GLES30.glGenBuffers(1, arrays, 0)
        vbo = arrays[0]
        GLES30.glGenBuffers(1, arrays, 0)
        ebo = arrays[0]

        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, vbo)
        val vertexBuffer = java.nio.ByteBuffer.allocateDirect(geometry.vertices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(geometry.vertices)
        vertexBuffer.position(0)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            geometry.vertices.size * 4,
            vertexBuffer,
            GLES30.GL_STATIC_DRAW
        )
        val stride = 10 * 4
        GLES30.glEnableVertexAttribArray(0)
        GLES30.glVertexAttribPointer(0, 3, GLES30.GL_FLOAT, false, stride, 0)
        GLES30.glEnableVertexAttribArray(1)
        GLES30.glVertexAttribPointer(1, 3, GLES30.GL_FLOAT, false, stride, 3 * 4)
        GLES30.glEnableVertexAttribArray(2)
        GLES30.glVertexAttribPointer(2, 4, GLES30.GL_FLOAT, false, stride, 6 * 4)

        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indexBuffer = java.nio.ByteBuffer.allocateDirect(geometry.indices.size * 4)
            .order(java.nio.ByteOrder.nativeOrder())
            .asIntBuffer()
        indexBuffer.put(geometry.indices)
        indexBuffer.position(0)
        GLES30.glBufferData(
            GLES30.GL_ELEMENT_ARRAY_BUFFER,
            geometry.indices.size * 4,
            indexBuffer,
            GLES30.GL_STATIC_DRAW
        )
        GLES30.glBindVertexArray(0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, 0)
    }

    fun draw() {
        if (vao == 0 || indexCount <= 0) return
        GLES30.glBindVertexArray(vao)
        GLES30.glDrawElements(GLES30.GL_TRIANGLES, indexCount, GLES30.GL_UNSIGNED_INT, 0)
        GLES30.glBindVertexArray(0)
    }

    fun dispose() {
        if (vao != 0) GLES30.glDeleteVertexArrays(1, intArrayOf(vao), 0)
        if (vbo != 0) GLES30.glDeleteBuffers(1, intArrayOf(vbo), 0)
        if (ebo != 0) GLES30.glDeleteBuffers(1, intArrayOf(ebo), 0)
        vao = 0
        vbo = 0
        ebo = 0
    }
}

object MeshBuilder {
    private class Buffer {
        val v = ArrayList<Float>()
        val i = ArrayList<Int>()

        fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, c: FloatArray) {
            v.add(x); v.add(y); v.add(z)
            v.add(nx); v.add(ny); v.add(nz)
            v.add(c[0]); v.add(c[1]); v.add(c[2]); v.add(c[3])
        }

        fun quad(
            ax: Float, ay: Float, az: Float,
            bx: Float, by: Float, bz: Float,
            cx: Float, cy: Float, cz: Float,
            dx: Float, dy: Float, dz: Float,
            nx: Float, ny: Float, nz: Float, c: FloatArray
        ) {
            val base = v.size / 10
            vertex(ax, ay, az, nx, ny, nz, c)
            vertex(bx, by, bz, nx, ny, nz, c)
            vertex(cx, cy, cz, nx, ny, nz, c)
            vertex(dx, dy, dz, nx, ny, nz, c)
            i.add(base); i.add(base + 1); i.add(base + 2)
            i.add(base); i.add(base + 2); i.add(base + 3)
        }

        fun build() = Geometry(v.toFloatArray(), i.toIntArray())
    }

    fun box(w: Float, h: Float, d: Float, c: FloatArray): Geometry {
        val b = Buffer()
        val x = w / 2f
        val y = h / 2f
        val z = d / 2f
        b.quad(-x, -y, z, x, -y, z, x, y, z, -x, y, z, 0f, 0f, 1f, c)
        b.quad(x, -y, -z, -x, -y, -z, -x, y, -z, x, y, -z, 0f, 0f, -1f, c)
        b.quad(x, -y, z, x, -y, -z, x, y, -z, x, y, z, 1f, 0f, 0f, c)
        b.quad(-x, -y, -z, -x, -y, z, -x, y, z, -x, y, -z, -1f, 0f, 0f, c)
        b.quad(-x, y, z, x, y, z, x, y, -z, -x, y, -z, 0f, 1f, 0f, c)
        b.quad(-x, -y, -z, x, -y, -z, x, -y, z, -x, -y, z, 0f, -1f, 0f, c)
        return b.build()
    }

    fun sphere(r: Float, lat: Int, lon: Int, c: FloatArray): Geometry {
        val b = Buffer()
        for (la in 0..lat) {
            val theta = PI * la / lat
            val sy = cos(theta).toFloat()
            val sr = sin(theta).toFloat()
            for (lo in 0..lon) {
                val phi = 2.0 * PI * lo / lon
                val px = sr * cos(phi).toFloat()
                val pz = sr * sin(phi).toFloat()
                b.vertex(px * r, sy * r, pz * r, px, sy, pz, c)
            }
        }
        for (la in 0 until lat) {
            for (lo in 0 until lon) {
                val a = la * (lon + 1) + lo
                val d = a + lon + 1
                b.i.add(a); b.i.add(a + 1); b.i.add(d + 1)
                b.i.add(a); b.i.add(d + 1); b.i.add(d)
            }
        }
        return b.build()
    }

    fun cylinder(rBottom: Float, rTop: Float, h: Float, segments: Int, c: FloatArray): Geometry {
        val b = Buffer()
        val half = h / 2f
        for (s in 0..segments) {
            val a = 2.0 * PI * s / segments
            val nx = cos(a).toFloat()
            val nz = sin(a).toFloat()
            b.vertex(nx * rBottom, -half, nz * rBottom, nx, 0f, nz, c)
            b.vertex(nx * rTop, half, nz * rTop, nx, 0f, nz, c)
        }
        for (s in 0 until segments) {
            val a = s * 2
            b.i.add(a); b.i.add(a + 1); b.i.add(a + 3)
            b.i.add(a); b.i.add(a + 3); b.i.add(a + 2)
        }
        val bottomCenter = b.v.size / 10
        b.vertex(0f, -half, 0f, 0f, -1f, 0f, c)
        for (s in 0..segments) {
            val a = 2.0 * PI * s / segments
            b.vertex(cos(a).toFloat() * rBottom, -half, sin(a).toFloat() * rBottom, 0f, -1f, 0f, c)
        }
        for (s in 0 until segments) {
            b.i.add(bottomCenter); b.i.add(bottomCenter + s + 2); b.i.add(bottomCenter + s + 1)
        }
        val topCenter = b.v.size / 10
        b.vertex(0f, half, 0f, 0f, 1f, 0f, c)
        for (s in 0..segments) {
            val a = 2.0 * PI * s / segments
            b.vertex(cos(a).toFloat() * rTop, half, sin(a).toFloat() * rTop, 0f, 1f, 0f, c)
        }
        for (s in 0 until segments) {
            b.i.add(topCenter); b.i.add(topCenter + s + 1); b.i.add(topCenter + s + 2)
        }
        return b.build()
    }

    fun cone(r: Float, h: Float, segments: Int, c: FloatArray): Geometry =
        cylinder(0f, r, h, segments, c)

    fun disc(r: Float, segments: Int, c: FloatArray): Geometry {
        val b = Buffer()
        val center = b.v.size / 10
        b.vertex(0f, 0f, 0f, 0f, 1f, 0f, c)
        for (s in 0..segments) {
            val a = 2.0 * PI * s / segments
            b.vertex(cos(a).toFloat() * r, 0f, sin(a).toFloat() * r, 0f, 1f, 0f, c)
        }
        for (s in 0 until segments) {
            b.i.add(center); b.i.add(center + s + 1); b.i.add(center + s + 2)
        }
        return b.build()
    }

    fun plane(w: Float, d: Float, c: FloatArray): Geometry {
        val b = Buffer()
        b.quad(-w / 2f, 0f, -d / 2f, w / 2f, 0f, -d / 2f, w / 2f, 0f, d / 2f, -w / 2f, 0f, d / 2f, 0f, 1f, 0f, c)
        return b.build()
    }

    fun blade(w: Float, h: Float, c: FloatArray): Geometry {
        val b = Buffer()
        b.vertex(-w / 2f, 0f, 0f, 0f, 0f, 1f, c)
        b.vertex(w / 2f, 0f, 0f, 0f, 0f, 1f, c)
        b.vertex(0f, h, 0f, 0f, 0f, 1f, c)
        b.i.add(0); b.i.add(1); b.i.add(2)
        return b.build()
    }

    fun octahedron(r: Float, h: Float, c: FloatArray): Geometry {
        val b = Buffer()
        b.vertex(0f, h / 2f, 0f, 0f, 1f, 0f, c)
        b.vertex(0f, -h / 2f, 0f, 0f, -1f, 0f, c)
        b.vertex(r, 0f, 0f, 1f, 0f, 0f, c)
        b.vertex(-r, 0f, 0f, -1f, 0f, 0f, c)
        b.vertex(0f, 0f, r, 0f, 0f, 1f, c)
        b.vertex(0f, 0f, -r, 0f, 0f, -1f, c)
        val faces = intArrayOf(
            0, 2, 4, 0, 4, 3, 0, 3, 5, 0, 5, 2,
            1, 4, 2, 1, 3, 4, 1, 5, 3, 1, 2, 5
        )
        for (k in faces.indices) b.i.add(faces[k])
        return b.build()
    }

    fun merge(vararg parts: Geometry): Geometry {
        val v = ArrayList<Float>()
        val idx = ArrayList<Int>()
        var offset = 0
        for (part in parts) {
            v.addAll(part.vertices.asIterable())
            for (value in part.indices) idx.add(value + offset)
            offset += part.vertices.size / 10
        }
        return Geometry(v.toFloatArray(), idx.toIntArray())
    }

    fun upload(geometry: Geometry): Mesh {
        val mesh = Mesh(geometry)
        mesh.upload()
        return mesh
    }
}
