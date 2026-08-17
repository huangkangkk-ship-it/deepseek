package com.yourname.gtneo2camera

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.view.Surface

class GLRecorder(private val renderer: CameraRenderer, private val sharedEglContext: EGLContext) {
    private var eglDisplay: EGLDisplay? = null
    private var recorderEglSurface: EGLSurface? = null
    private var recorderContext: EGLContext? = null

    fun startRecording(surface: Surface, width: Int, height: Int) {
        val display = EGL14.eglGetCurrentDisplay()
        check(display != EGL14.EGL_NO_DISPLAY) { "Unable to get EGL display" }
        eglDisplay = display

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        check(EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0) {
            "Unable to choose EGL config"
        }
        val config = configs[0] ?: error("EGL config unavailable")
        val eglSurface = EGL14.eglCreateWindowSurface(display, config, surface, intArrayOf(EGL14.EGL_NONE), 0)
        check(eglSurface != EGL14.EGL_NO_SURFACE) { "Unable to create recorder EGL surface" }
        recorderEglSurface = eglSurface

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(display, config, sharedEglContext, contextAttribs, 0)
        check(context != EGL14.EGL_NO_CONTEXT) { "Unable to create recorder EGL context" }
        recorderContext = context
    }

    fun drawFrame() {
        val display = eglDisplay ?: return
        val surface = recorderEglSurface ?: return
        val context = recorderContext ?: return
        val read = EGL14.eglGetCurrentSurface(EGL14.EGL_READ)
        val draw = EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW)
        val old = EGL14.eglGetCurrentContext()

        check(EGL14.eglMakeCurrent(display, surface, surface, context)) { "Unable to make recorder EGL context current" }
        val w = IntArray(1)
        val h = IntArray(1)
        EGL14.eglQuerySurface(display, surface, EGL14.EGL_WIDTH, w, 0)
        EGL14.eglQuerySurface(display, surface, EGL14.EGL_HEIGHT, h, 0)
        renderer.drawToTarget(w[0], h[0])
        EGL14.eglSwapBuffers(display, surface)
        EGL14.eglMakeCurrent(display, draw, read, old)
    }

    fun stopRecording() {
        val display = eglDisplay ?: return
        recorderEglSurface?.let { EGL14.eglDestroySurface(display, it) }
        recorderContext?.let { EGL14.eglDestroyContext(display, it) }
        recorderEglSurface = null
        recorderContext = null
        eglDisplay = null
    }
}
