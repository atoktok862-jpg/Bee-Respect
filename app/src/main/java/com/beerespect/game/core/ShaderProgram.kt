package com.beerespect.game.core

import android.opengl.GLES30
import android.util.Log

class ShaderProgram(vsText: String, fsText: String) {
    var programId: Int = 0
        private set

    private val locations = HashMap<String, Int>()
    private val vertexSource: String = vsText
    private val fragmentSource: String = fsText

    fun build() {
        val vertex = compile(GLES30.GL_VERTEX_SHADER, vertexSource)
        val fragment = compile(GLES30.GL_FRAGMENT_SHADER, fragmentSource)
        programId = GLES30.glCreateProgram()
        GLES30.glAttachShader(programId, vertex)
        GLES30.glAttachShader(programId, fragment)
        GLES30.glBindAttribLocation(programId, 0, "aPosition")
        GLES30.glBindAttribLocation(programId, 1, "aNormal")
        GLES30.glBindAttribLocation(programId, 2, "aColor")
        GLES30.glLinkProgram(programId)
        val linked = IntArray(1)
        GLES30.glGetProgramiv(programId, GLES30.GL_LINK_STATUS, linked, 0)
        GLES30.glDeleteShader(vertex)
        GLES30.glDeleteShader(fragment)
        if (linked[0] == 0) {
            val error = GLES30.glGetProgramInfoLog(programId)
            GLES30.glDeleteProgram(programId)
            programId = 0
            throw IllegalStateException("Ошибка линковки шейдера: $error")
        }
    }

    private fun compile(type: Int, source: String): Int {
        val shader = GLES30.glCreateShader(type)
        GLES30.glShaderSource(shader, source)
        GLES30.glCompileShader(shader)
        val status = IntArray(1)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val error = GLES30.glGetShaderInfoLog(shader)
            GLES30.glDeleteShader(shader)
            Log.e("BeeShader", error)
            throw IllegalStateException("Ошибка компиляции шейдера: $error")
        }
        return shader
    }

    fun use() {
        if (programId != 0) GLES30.glUseProgram(programId)
    }

    private fun location(name: String): Int {
        val cached = locations[name]
        if (cached != null) return cached
        val found = GLES30.glGetUniformLocation(programId, name)
        locations[name] = found
        return found
    }

    fun setMat4(name: String, value: FloatArray) {
        val loc = location(name)
        if (loc >= 0) GLES30.glUniformMatrix4fv(loc, 1, false, value, 0)
    }

    fun setFloat(name: String, value: Float) {
        val loc = location(name)
        if (loc >= 0) GLES30.glUniform1f(loc, value)
    }

    fun setVec3(name: String, x: Float, y: Float, z: Float) {
        val loc = location(name)
        if (loc >= 0) GLES30.glUniform3f(loc, x, y, z)
    }

    fun setVec4(name: String, x: Float, y: Float, z: Float, w: Float) {
        val loc = location(name)
        if (loc >= 0) GLES30.glUniform4f(loc, x, y, z, w)
    }

    fun dispose() {
        if (programId != 0) {
            GLES30.glDeleteProgram(programId)
            programId = 0
            locations.clear()
        }
    }
}
