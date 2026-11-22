package com.beerespect.game.core

import kotlin.math.max

class Camera3D {
    val eye = Vec3(0f, 12f, 24f)
    val target = Vec3(0f, 2f, 0f)
    var viewProjection = FloatArray(16)
        private set

    private var shakePower = 0f
    private var shakeTime = 0f
    private var aspect = 1.6f

    fun addShake(power: Float) {
        shakePower = max(shakePower, power)
        shakeTime = 0.42f
    }

    fun update(dt: Float, wantedEye: Vec3, wantedTarget: Vec3) {
        aspect = max(0.5f, aspect)
        eye.copyFrom(wantedEye)
        target.copyFrom(wantedTarget)
        if (shakeTime > 0f) {
            shakeTime -= dt
            val amount = shakePower * (shakeTime / 0.42f)
            eye.x += (Math.random().toFloat() - 0.5f) * amount
            eye.y += (Math.random().toFloat() - 0.5f) * amount
            eye.z += (Math.random().toFloat() - 0.5f) * amount
            if (shakeTime <= 0f) shakePower = 0f
        }
        val view = Mat4.lookAt(eye, target, Vec3(0f, 1f, 0f))
        val projection = Mat4.perspective(1.05f, aspect, 0.1f, 240f)
        viewProjection = Mat4.multiply(projection, view)
    }

    fun setAspect(value: Float) {
        aspect = value
    }
}
