package com.beerespect.game.core

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Mesh(val geometry: Geometry) {
    var vbo = 0
        private set
    var ebo = 0
        private set
    var indexCount = geometry.indices.size
        private set

    fun upload() {
        if (vbo != 0) return
        if (geometry.vertices.isEmpty() || geometry.indices.isEmpty()) return
        val arrays = IntArray(2)
        GLES20.glGenBuffers(2, arrays, 0)
        vbo = arrays[0]
        ebo = arrays[1]

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        val vertexBuffer = ByteBuffer.allocateDirect(geometry.vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        vertexBuffer.put(geometry.vertices)
        vertexBuffer.position(0)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            geometry.vertices.size * 4,
            vertexBuffer,
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ebo)
        val indexBuffer = ByteBuffer.allocateDirect(geometry.indices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asIntBuffer()
        indexBuffer.put(geometry.indices)
        indexBuffer.position(0)
        GLES20.glBufferData(
            GLES20.GL_ELEMENT_ARRAY_BUFFER,
            geometry.indices.size * 4,
            indexBuffer,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun draw() {
        if (vbo == 0 || ebo == 0 || indexCount <= 0) return
        val stride = 10 * 4
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, ebo)
        GLES20.glEnableVertexAttribArray(0)
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, stride, 0)
        GLES20.glEnableVertexAttribArray(1)
        GLES20.glVertexAttribPointer(1, 3, GLES20.GL_FLOAT, false, stride, 3 * 4)
        GLES20.glEnableVertexAttribArray(2)
        GLES20.glVertexAttribPointer(2, 4, GLES20.GL_FLOAT, false, stride, 6 * 4)
        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indexCount, GLES20.GL_UNSIGNED_INT, 0)
        GLES20.glDisableVertexAttribArray(0)
        GLES20.glDisableVertexAttribArray(1)
        GLES20.glDisableVertexAttribArray(2)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        GLES20.glBindBuffer(GLES20.GL_ELEMENT_ARRAY_BUFFER, 0)
    }

    fun dispose() {
        val buffers = IntArray(2)
        var count = 0
        if (vbo != 0) buffers[count++] = vbo
        if (ebo != 0) buffers[count++] = ebo
        if (count > 0) GLES20.glDeleteBuffers(count, buffers, 0)
        vbo = 0
        ebo = 0
    }
}

class Geometry(val vertices: FloatArray, val indices: IntArray)

object MeshBuilder {
    fun sphere(radius: Float, stacks: Int, slices: Int, color: FloatArray): Geometry {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Int>()
        for (s in 0..stacks) {
            val v = s.toFloat() / stacks.toFloat()
            val phi = (v - 0.5f) * Math.PI.toFloat()
            val y = radius * kotlin.math.sin(phi)
            val r = radius * kotlin.math.cos(phi)
            for (sl in 0..slices) {
                val u = sl.toFloat() / slices.toFloat()
                val theta = u * 2f * Math.PI.toFloat()
                val x = r * kotlin.math.cos(theta)
                val z = r * kotlin.math.sin(theta)
                val nx = x / radius
                val ny = y / radius
                val nz = z / radius
                vertices.add(x); vertices.add(y); vertices.add(z)
                vertices.add(nx); vertices.add(ny); vertices.add(nz)
                vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
            }
        }
        val row = slices + 1
        for (s in 0 until stacks) {
            for (sl in 0 until slices) {
                val a = s * row + sl
                val b = a + row
                indices.add(a); indices.add(b); indices.add(a + 1)
                indices.add(a + 1); indices.add(b); indices.add(b + 1)
            }
        }
        return Geometry(vertices.toFloatArray(), indices.toIntArray())
    }

    fun box(w: Float, h: Float, d: Float, color: FloatArray): Geometry {
        val hw = w / 2f; val hh = h / 2f; val hd = d / 2f
        val faces = arrayOf(
            floatArrayOf(0f, 0f, 1f, -hw, -hh, hd, hw, -hh, hd, hw, hh, hd, -hw, hh, hd),
            floatArrayOf(0f, 0f, -1f, hw, -hh, -hd, -hw, -hh, -hd, -hw, hh, -hd, hw, hh, -hd),
            floatArrayOf(0f, 1f, 0f, -hw, hh, hd, hw, hh, hd, hw, hh, -hd, -hw, hh, -hd),
            floatArrayOf(0f, -1f, 0f, -hw, -hh, -hd, hw, -hh, -hd, hw, -hh, hd, -hw, -hh, hd),
            floatArrayOf(1f, 0f, 0f, hw, -hh, hd, hw, -hh, -hd, hw, hh, -hd, hw, hh, hd),
            floatArrayOf(-1f, 0f, 0f, -hw, -hh, -hd, -hw, -hh, hd, -hw, hh, hd, -hw, hh, -hd)
        )
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Int>()
        for (face in faces) {
            val base = vertices.size / 10
            val nx = face[0]; val ny = face[1]; val nz = face[2]
            for (p in 0 until 4) {
                val i = 3 + p * 3
                vertices.add(face[i]); vertices.add(face[i + 1]); vertices.add(face[i + 2])
                vertices.add(nx); vertices.add(ny); vertices.add(nz)
                vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
            }
            indices.add(base); indices.add(base + 1); indices.add(base + 2)
            indices.add(base); indices.add(base + 2); indices.add(base + 3)
        }
        return Geometry(vertices.toFloatArray(), indices.toIntArray())
    }

    fun cylinder(radiusTop: Float, radiusBottom: Float, height: Float, slices: Int, color: FloatArray): Geometry {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Int>()
        val hh = height / 2f
        for (ring in 0..1) {
            val y = if (ring == 0) -hh else hh
            val r = if (ring == 0) radiusBottom else radiusTop
            for (sl in 0..slices) {
                val u = sl.toFloat() / slices.toFloat()
                val a = u * 2f * Math.PI.toFloat()
                val x = r * kotlin.math.cos(a)
                val z = r * kotlin.math.sin(a)
                val nx = kotlin.math.cos(a); val nz = kotlin.math.sin(a)
                vertices.add(x); vertices.add(y); vertices.add(z)
                vertices.add(nx); vertices.add(0f); vertices.add(nz)
                vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
            }
        }
        val row = slices + 1
        for (sl in 0 until slices) {
            val a = sl; val b = sl + row
            indices.add(a); indices.add(b); indices.add(a + 1)
            indices.add(a + 1); indices.add(b); indices.add(b + 1)
        }
        fun cap(y: Float, r: Float, normalY: Float, top: Boolean) {
            val center = vertices.size / 10
            vertices.add(0f); vertices.add(y); vertices.add(0f)
            vertices.add(0f); vertices.add(normalY); vertices.add(0f)
            vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
            for (sl in 0..slices) {
                val u = sl.toFloat() / slices.toFloat()
                val a = u * 2f * Math.PI.toFloat()
                val x = r * kotlin.math.cos(a)
                val z = r * kotlin.math.sin(a)
                vertices.add(x); vertices.add(y); vertices.add(z)
                vertices.add(0f); vertices.add(normalY); vertices.add(0f)
                vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
            }
            for (sl in 0 until slices) {
                val a = center + 1 + sl
                val b = center + 2 + sl
                if (top) { indices.add(center); indices.add(a); indices.add(b) }
                else { indices.add(center); indices.add(b); indices.add(a) }
            }
        }
        cap(hh, radiusTop, 1f, true)
        cap(-hh, radiusBottom, -1f, false)
        return Geometry(vertices.toFloatArray(), indices.toIntArray())
    }

    fun cone(radius: Float, height: Float, slices: Int, color: FloatArray): Geometry {
        val vertices = ArrayList<Float>()
        val indices = ArrayList<Int>()
        val hh = height / 2f
        val tip = vertices.size / 10
        vertices.add(0f); vertices.add(hh); vertices.add(0f)
        vertices.add(0f); vertices.add(1f); vertices.add(0f)
        vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
        for (sl in 0..slices) {
            val u = sl.toFloat() / slices.toFloat()
            val a = u * 2f * Math.PI.toFloat()
            val x = radius * kotlin.math.cos(a)
            val z = radius * kotlin.math.sin(a)
            val nx = kotlin.math.cos(a); val nz = kotlin.math.sin(a)
            vertices.add(x); vertices.add(-hh); vertices.add(z)
            vertices.add(nx); vertices.add(0.3f); vertices.add(nz)
            vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
        }
        for (sl in 0 until slices) {
            indices.add(tip); indices.add(tip + 1 + sl); indices.add(tip + 2 + sl)
        }
        val center = vertices.size / 10
        vertices.add(0f); vertices.add(-hh); vertices.add(0f)
        vertices.add(0f); vertices.add(-1f); vertices.add(0f)
        vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
        for (sl in 0..slices) {
            val u = sl.toFloat() / slices.toFloat()
            val a = u * 2f * Math.PI.toFloat()
            val x = radius * kotlin.math.cos(a)
            val z = radius * kotlin.math.sin(a)
            vertices.add(x); vertices.add(-hh); vertices.add(z)
            vertices.add(0f); vertices.add(-1f); vertices.add(0f)
            vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
        }
        for (sl in 0 until slices) { indices.add(center); indices.add(center + 2 + sl); indices.add(center + 1 + sl) }
        return Geometry(vertices.toFloatArray(), indices.toIntArray())
    }

    fun disc(radius: Float, slices: Int, color: FloatArray): Geometry {
        val vertices = ArrayList<Float>(); val indices = ArrayList<Int>()
        vertices.add(0f); vertices.add(0f); vertices.add(0f); vertices.add(0f); vertices.add(1f); vertices.add(0f)
        vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
        for (sl in 0..slices) {
            val a = sl.toFloat() / slices * 2f * Math.PI.toFloat()
            val x = radius * kotlin.math.cos(a); val z = radius * kotlin.math.sin(a)
            vertices.add(x); vertices.add(0f); vertices.add(z); vertices.add(0f); vertices.add(1f); vertices.add(0f)
            vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f)
        }
        for (sl in 0 until slices) { indices.add(0); indices.add(sl + 1); indices.add(sl + 2) }
        return Geometry(vertices.toFloatArray(), indices.toIntArray())
    }

    fun plane(w: Float, d: Float, color: FloatArray): Geometry {
        val hw = w / 2f; val hd = d / 2f
        val vertices = floatArrayOf(
            -hw, 0f, -hd, 0f, 1f, 0f, color[0], color[1], color[2], if (color.size > 3) color[3] else 1f,
             hw, 0f, -hd, 0f, 1f, 0f, color[0], color[1], color[2], if (color.size > 3) color[3] else 1f,
             hw, 0f,  hd, 0f, 1f, 0f, color[0], color[1], color[2], if (color.size > 3) color[3] else 1f,
            -hw, 0f,  hd, 0f, 1f, 0f, color[0], color[1], color[2], if (color.size > 3) color[3] else 1f
        )
        return Geometry(vertices, intArrayOf(0, 1, 2, 0, 2, 3))
    }

    fun octahedron(radius: Float, heightScale: Float, color: FloatArray): Geometry {
        val top = floatArrayOf(0f, radius * heightScale, 0f)
        val bottom = floatArrayOf(0f, -radius * heightScale, 0f)
        val mid = Array(4) { i ->
            val a = i * Math.PI.toFloat() / 2f
            floatArrayOf(kotlin.math.cos(a) * radius, 0f, kotlin.math.sin(a) * radius)
        }
        val vertices = ArrayList<Float>(); val indices = ArrayList<Int>()
        fun addTri(a: FloatArray, b: FloatArray, c: FloatArray) {
            val ux = b[0]-a[0]; val uy = b[1]-a[1]; val uz = b[2]-a[2]
            val vx = c[0]-a[0]; val vy = c[1]-a[1]; val vz = c[2]-a[2]
            var nx = uy*vz-uz*vy; var ny = uz*vx-ux*vz; var nz = ux*vy-uy*vx
            val len = kotlin.math.sqrt(nx*nx+ny*ny+nz*nz).coerceAtLeast(0.0001f)
            nx/=len; ny/=len; nz/=len
            val base = vertices.size / 10
            for (p in arrayOf(a,b,c)) { vertices.add(p[0]); vertices.add(p[1]); vertices.add(p[2]); vertices.add(nx); vertices.add(ny); vertices.add(nz); vertices.add(color[0]); vertices.add(color[1]); vertices.add(color[2]); vertices.add(if (color.size > 3) color[3] else 1f) }
            indices.add(base); indices.add(base+1); indices.add(base+2)
        }
        for (i in 0 until 4) { val n=(i+1)%4; addTri(top, mid[i], mid[n]); addTri(bottom, mid[n], mid[i]) }
        return Geometry(vertices.toFloatArray(), indices.toIntArray())
    }

    fun blade(width: Float, height: Float, color: FloatArray): Geometry {
        val hw = width / 2f
        val vertices = floatArrayOf(
            -hw, 0f, 0f, 0f, 0f, 1f, color[0], color[1], color[2], 1f,
             hw, 0f, 0f, 0f, 0f, 1f, color[0], color[1], color[2], 1f,
             0f, height, 0f, 0f, 0f, 1f, color[0], color[1]*1.05f, color[2], 1f
        )
        return Geometry(vertices, intArrayOf(0, 1, 2))
    }

    fun merge(vararg geoms: Geometry): Geometry {
        val verts = ArrayList<Float>(); val inds = ArrayList<Int>()
        var base = 0
        for (g in geoms) {
            for (v in g.vertices) verts.add(v)
            for (i in g.indices) inds.add(i + base)
            base += g.vertices.size / 10
        }
        return Geometry(verts.toFloatArray(), inds.toIntArray())
    }

    fun upload(geometry: Geometry): Mesh { val m = Mesh(geometry); m.upload(); return m }
}

