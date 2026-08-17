package com.yourname.gtneo2camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.MediaRecorder
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Surface
import android.opengl.GLSurfaceView
import android.widget.Button
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var glView: GLSurfaceView; private lateinit var renderer: CameraRenderer; private var cameraDevice:CameraDevice?=null;private var cameraSession:CameraCaptureSession?=null;private val cameraExecutor=Executors.newSingleThreadExecutor();private var mediaRecorder:MediaRecorder?=null;private var glRecorder:GLRecorder?=null;private val recordHandler=Handler(Looper.getMainLooper())
    private val recordRunnable=object:Runnable{override fun run(){if(mediaRecorder!=null){glRecorder?.drawFrame();recordHandler.postDelayed(this,33)}}}
    override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContentView(R.layout.activity_main);glView=findViewById(R.id.glSurfaceView);renderer=CameraRenderer(this);glView.setEGLContextClientVersion(3);glView.setRenderer(renderer);glView.renderMode=GLSurfaceView.RENDERMODE_CONTINUOUSLY;val seek=findViewById<SeekBar>(R.id.seekBar);seek.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(p0:SeekBar?,p1:Int,p2:Boolean){renderer.setTint(p1-100)};override fun onStartTrackingTouch(p0:SeekBar?){ };override fun onStopTrackingTouch(p0:SeekBar?){ }});seek.progress=100;findViewById<Button>(R.id.btnRecord).setOnClickListener{if(mediaRecorder==null)startRecording()else stopRecording()};if(ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO),100)else openCamera()}
    override fun onRequestPermissionsResult(r:Int,p:Array<String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==100&&g.isNotEmpty()&&g[0]==PackageManager.PERMISSION_GRANTED)openCamera()}
    private fun openCamera(){val manager=getSystemService(CAMERA_SERVICE)as CameraManager;try{val id=manager.cameraIdList[0];manager.openCamera(id,object:CameraDevice.StateCallback(){override fun onOpened(c:CameraDevice){cameraDevice=c;startPreview()};override fun onDisconnected(c:CameraDevice){c.close()};override fun onError(c:CameraDevice,e:Int){c.close()}},cameraExecutor)}catch(e:Exception){e.printStackTrace()}}
    private fun startPreview(){val texture=SurfaceTexture(0);texture.setDefaultBufferSize(1920,1080);val surface=Surface(texture);renderer.setSurfaceTexture(texture);cameraDevice?.createCaptureSession(listOf(surface),object:CameraCaptureSession.StateCallback(){override fun onConfigured(s:CameraCaptureSession){cameraSession=s;val req=cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);req.addTarget(surface);s.setRepeatingRequest(req.build(),null,null)};override fun onConfigureFailed(s:CameraCaptureSession){}},cameraExecutor)}
    private fun startRecording(){val file=File(filesDir,"recorded_${System.currentTimeMillis()}.mp4");val recorder=MediaRecorder().apply{setAudioSource(MediaRecorder.AudioSource.MIC);setVideoSource(MediaRecorder.VideoSource.SURFACE);setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);setOutputFile(file.absolutePath);setVideoEncoder(MediaRecorder.VideoEncoder.H264);setAudioEncoder(MediaRecorder.AudioEncoder.AAC);setVideoSize(1920,1080);setVideoFrameRate(30);prepare()};glRecorder=GLRecorder(renderer,renderer.getEglContext());glRecorder?.startRecording(recorder.surface,1920,1080);recorder.start();mediaRecorder=recorder;findViewById<Button>(R.id.btnRecord).text="停止录制";recordHandler.post(recordRunnable)}
    private fun stopRecording(){recordHandler.removeCallbacks(recordRunnable);mediaRecorder?.apply{stop();release()};glRecorder?.stopRecording();mediaRecorder=null;glRecorder=null;findViewById<Button>(R.id.btnRecord).text="开始录制"}
    override fun onPause(){cameraSession?.close();cameraDevice?.close();mediaRecorder?.release();recordHandler.removeCallbacks(recordRunnable);super.onPause()}
}
