package com.yourname.gtneo2camera

import android.opengl.EGL14
import android.opengl.EGLContext
import android.view.Surface

class GLRecorder(private val renderer: CameraRenderer, private val sharedEglContext: EGLContext) {
    private var eglDisplay: EGL14.EGLDisplay? = null
    private var recorderEglSurface: EGL14.EGLSurface? = null
    private var recorderContext: EGL14.EGLContext? = null

    fun startRecording(surface: Surface, width: Int, height: Int) {
        eglDisplay = EGL14.eglGetCurrentDisplay()
        val configAttribs = intArrayOf(EGL14.EGL_RED_SIZE,8,EGL14.EGL_GREEN_SIZE,8,EGL14.EGL_BLUE_SIZE,8,EGL14.EGL_ALPHA_SIZE,8,EGL14.EGL_RENDERABLE_TYPE,EGL14.EGL_OPENGL_ES2_BIT,EGL14.EGL_NONE)
        val configs = arrayOfNulls<EGL14.EGLConfig>(1); val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs,0,configs,0,1,numConfigs,0)
        recorderEglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, null,0)
        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION,3,EGL14.EGL_NONE)
        recorderContext = EGL14.eglCreateContext(eglDisplay,configs[0],sharedEglContext,contextAttribs,0)
    }
    fun drawFrame() {
        val display=eglDisplay?:return; val surface=recorderEglSurface?:return; val context=recorderContext?:return
        val read=EGL14.eglGetCurrentSurface(EGL14.EGL_READ); val draw=EGL14.eglGetCurrentSurface(EGL14.EGL_DRAW); val old=EGL14.eglGetCurrentContext()
        EGL14.eglMakeCurrent(display,surface,surface,context)
        val w=IntArray(1);val h=IntArray(1);EGL14.eglQuerySurface(display,surface,EGL14.EGL_WIDTH,w,0);EGL14.eglQuerySurface(display,surface,EGL14.EGL_HEIGHT,h,0)
        renderer.drawToTarget(w[0],h[0]);EGL14.eglSwapBuffers(display,surface);EGL14.eglMakeCurrent(display,draw,read,old)
    }
    fun stopRecording(){val d=eglDisplay?:return;EGL14.eglDestroySurface(d,recorderEglSurface);EGL14.eglDestroyContext(d,recorderContext);recorderEglSurface=null;recorderContext=null}
}
