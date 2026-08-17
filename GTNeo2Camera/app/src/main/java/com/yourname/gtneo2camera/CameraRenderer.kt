package com.yourname.gtneo2camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class CameraRenderer(private val context: Context) : GLSurfaceView.Renderer {
    private var surfaceTexture: SurfaceTexture?=null; private var textureId=-1; private var program=0; private var tintUniform=-1; private var tintValue=0f; private var eglContext:EGL14.EGLContext?=null; private var vbo=-1
    fun setSurfaceTexture(st:SurfaceTexture){surfaceTexture=st}; fun setTint(value:Int){tintValue=value.toFloat()/100f}; fun getEglContext():EGL14.EGLContext=eglContext?:error("EGL context not ready")
    override fun onSurfaceCreated(gl:GL10?,config:EGLConfig?){GLES30.glClearColor(0f,0f,0f,1f);val tex=IntArray(1);GLES30.glGenTextures(1,tex,0);textureId=tex[0];GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textureId);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MIN_FILTER,GLES30.GL_LINEAR);GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D,GLES30.GL_TEXTURE_MAG_FILTER,GLES30.GL_LINEAR);program=createProgram();tintUniform=GLES30.glGetUniformLocation(program,"uTint");eglContext=EGL14.eglGetCurrentContext()}
    override fun onSurfaceChanged(gl:GL10?,width:Int,height:Int){GLES30.glViewport(0,0,width,height)}
    override fun onDrawFrame(gl:GL10?){drawToTarget(1920,1080)}
    fun drawToTarget(width:Int,height:Int){if(program==0||textureId==-1)return;GLES30.glViewport(0,0,width,height);GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT);surfaceTexture?.let{st->st.updateTexImage();GLES30.glUseProgram(program);GLES30.glActiveTexture(GLES30.GL_TEXTURE0);GLES30.glBindTexture(GLES30.GL_TEXTURE_2D,textureId);GLES30.glUniform1i(GLES30.glGetUniformLocation(program,"sTexture"),0);GLES30.glUniform1f(tintUniform,tintValue);drawFullScreenQuad()}}
    private fun drawFullScreenQuad(){if(vbo==-1){val vertices=floatArrayOf(-1f,-1f,0f,0f,0f,1f,-1f,0f,1f,0f,-1f,1f,0f,0f,1f,1f,1f,0f,1f,1f);val bufs=IntArray(1);GLES30.glGenBuffers(1,bufs,0);vbo=bufs[0];GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo);GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER,vertices.size*4,FloatBuffer.wrap(vertices),GLES30.GL_STATIC_DRAW)};GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER,vbo);GLES30.glEnableVertexAttribArray(0);GLES30.glVertexAttribPointer(0,3,GLES30.GL_FLOAT,false,20,0);GLES30.glEnableVertexAttribArray(1);GLES30.glVertexAttribPointer(1,2,GLES30.GL_FLOAT,false,20,12);GLES30.glDrawArrays(GLES30.GL_TRIANGLE_STRIP,0,4)}
    private fun createProgram():Int{val vs=loadShader(GLES30.GL_VERTEX_SHADER,VERTEX_SHADER);val fs=loadShader(GLES30.GL_FRAGMENT_SHADER,FRAGMENT_SHADER);val p=GLES30.glCreateProgram();GLES30.glAttachShader(p,vs);GLES30.glAttachShader(p,fs);GLES30.glLinkProgram(p);val linked=IntArray(1);GLES30.glGetProgramiv(p,GLES30.GL_LINK_STATUS,linked,0);if(linked[0]==0){GLES30.glDeleteProgram(p);throw RuntimeException("Shader program link failed")};return p}
    private fun loadShader(type:Int,src:String):Int{val s=GLES30.glCreateShader(type);GLES30.glShaderSource(s,src);GLES30.glCompileShader(s);return s}
    companion object{private const val VERTEX_SHADER="#version 300 es\nlayout(location=0) in vec4 aPos; layout(location=1) in vec2 aTexCoord; out vec2 vTexCoord; void main(){gl_Position=aPos;vTexCoord=aTexCoord;}";private const val FRAGMENT_SHADER="#version 300 es\nprecision mediump float; uniform sampler2D sTexture; uniform float uTint; in vec2 vTexCoord; out vec4 fragColor; void main(){vec4 tex=texture(sTexture,vTexCoord);float r=tex.r*(1.0+uTint);float b=tex.b*(1.0-uTint);fragColor=vec4(clamp(r,0.0,1.0),tex.g,clamp(b,0.0,1.0),tex.a);}"}
}
