package com.beerespect.game.core

import android.opengl.GLES20
import android.util.Log

class ShaderProgram(vsText: String, fsText: String) {
    var programId: Int = 0
        private set

    private val locations = HashMap<String, Int>()
    private val vertexSource: String = vsText
    private val fragmentSource: String = fsText

    fun build() {
        val vertex = compile(GLES20.GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        programId = GLES20.glCreateProgram()
        GLES20.glAttachShader(programId, vertex)
        GLES20.glAttachShader(programId, fragment)
        GLES20.glBindAttribLocation(programId, 0, "aPosition")
        GLES20.glBindAttribLocation(programId, 1, "aNormal")
        GLES20.glBindAttribLocation(programId, 2, "aColor")
        GLES20.glLinkProgram(programId)
        val linked = IntArray(1)
        GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linked, 0)
        GLES20.glDeleteShader(vertex)
        GLES20.glDeleteShader(fragment)
        if (linked[0] == 0) {
            val error = GLES20.glGetProgramInfoLog(programId)
            GLES20.glDeleteProgram(programId)
            programId = 0
            throw IllegalStateException("Ошибка линковки шейдера: $error")
        }
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val error = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            Log.e("BeeShader", error)
            throw IllegalStateException("Ошибка компиляции шейдера: $error")
        }
        return shader
    }

    fun use() {
        if (programId != 0) GLES20.glUseProgram(programId)
    }

    private fun location(name: String): Int {
        val cached = locations[name]
        if (cached != null) return cached
        val found = GLES20.glGetUniformLocation(programId, name)
        locations[name] = found
        return found
    }

    fun setMat4(name: String, value: FloatArray) {
        val loc = location(name)
        if (loc >= 0) GLES20.glUniformMatrix4fv(loc, 1, false, value, 0)
    }

    fun setFloat(name: String, value: Float) {
        val loc = location(name)
        if (loc >= 0) GLES20.glUniform1f(loc, value)
    }

    fun setVec3(name: String, x: Float, y: Float, z: Float) {
        val loc = location(name)
        if (loc >= 0) GLES20.glUniform3f(loc, x, y, z)
    }

    fun setVec4(name: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = location(name)
        if (loc >= 0) GLES20.glUniform4f(loc, x, y, z, w)
    }

    fun dispose() {
        if (programId != 0) {
            GLES20.glDeleteProgram(programId)
            programId = 0
            locations.clear()
        }
    }
}

