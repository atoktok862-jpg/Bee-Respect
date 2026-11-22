package com.beerespect.game.core

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f) {
    fun set(nx: Float, ny: Float, nz: Float): Vec3 {
        x = nx
        y = ny
        z = nz
        return this
    }

    fun copyFrom(other: Vec3): Vec3 {
        x = other.x
        y = other.y
        z = other.z
        return this
    }

    fun copy(): Vec3 = Vec3(x, y, z)

    fun addScaled(other: Vec3, scale: Float): Vec3 {
        x += other.x * scale
        y += other.y * scale
        z += other.z * scale
        return this
    }

    fun distanceTo(other: Vec3): Float {
        val dx = x - other.x
        val dy = y - other.y
        val dz = z - other.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }

    fun length(): Float = sqrt(x * x + y * y + z * z)

    fun normalize(): Vec3 {
        val len = length()
        if (len > 0.0001f) {
            x /= len
            y /= len
            z /= len
        }
        return this
    }
}

object Mat4 {
    fun identity(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    fun multiply(a: FloatArray, b: FloatArray): FloatArray {
        val out = FloatArray(16)
        for (col in 0..3) {
            for (row in 0..3) {
                var sum = 0f
                for (k in 0..3) sum += a[k * 4 + row] * b[col * 4 + k]
                out[col * 4 + row] = sum
            }
        }
        return out
    }

    fun perspective(fovY: Float, aspect: Float, near: Float, far: Float): FloatArray {
        val f = 1f / kotlin.math.tan(fovY * 0.5f)
        val nf = 1f / (near - far)
        return floatArrayOf(
            f / aspect, 0f, 0f, 0f,
            0f, f, 0f, 0f,
            0f, 0f, (far + near) * nf, -1f,
            0f, 0f, 2f * far * near * nf, 0f
        )
    }

    fun lookAt(eye: Vec3, center: Vec3, up: Vec3): FloatArray {
        val f = Vec3(center.x - eye.x, center.y - eye.y, center.z - eye.z).normalize()
        val s = Vec3(
            f.y * up.z - f.z * up.y,
            f.z * up.x - f.x * up.z,
            f.x * up.y - f.y * up.x
        ).normalize()
        val u = Vec3(
            s.y * f.z - s.z * f.y,
            s.z * f.x - s.x * f.z,
            s.x * f.y - s.y * f.x
        )
        return floatArrayOf(
            s.x, u.x, -f.x, 0f,
            s.y, u.y, -f.y, 0f,
            s.z, u.z, -f.z, 0f,
            -(s.x * eye.x + s.y * eye.y + s.z * eye.z),
            -(u.x * eye.x + u.y * eye.y + u.z * eye.z),
            f.x * eye.x + f.y * eye.y + f.z * eye.z,
            1f
        )
    }

    fun translation(x: Float, y: Float, z: Float): FloatArray {
        val m = identity()
        m[12] = x
        m[13] = y
        m[14] = z
        return m
    }

    fun scaling(x: Float, y: Float, z: Float): FloatArray {
        val m = identity()
        m[0] = x
        m[5] = y
        m[10] = z
        return m
    }

    fun rotateX(a: Float): FloatArray {
        val c = cos(a)
        val s = sin(a)
        return floatArrayOf(1f, 0f, 0f, 0f, 0f, c, s, 0f, 0f, -s, c, 0f, 0f, 0f, 0f, 1f)
    }

    fun rotateY(a: Float): FloatArray {
        val c = cos(a)
        val s = sin(a)
        return floatArrayOf(c, 0f, -s, 0f, 0f, 1f, 0f, 0f, s, 0f, c, 0f, 0f, 0f, 0f, 1f)
    }

    fun rotateZ(a: Float): FloatArray {
        val c = cos(a)
        val s = sin(a)
        return floatArrayOf(c, s, 0f, 0f, -s, c, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f)
    }

    fun compose(
        px: Float, py: Float, pz: Float,
        rx: Float, ry: Float, rz: Float,
        sx: Float, sy: Float, sz: Float
    ): FloatArray {
        var m = translation(px, py, pz)
        if (ry != 0f) m = multiply(m, rotateY(ry))
        if (rx != 0f) m = multiply(m, rotateX(rx))
        if (rz != 0f) m = multiply(m, rotateZ(rz))
        if (sx != 1f || sy != 1f || sz != 1f) m = multiply(m, scaling(sx, sy, sz))
        return m
    }
}
