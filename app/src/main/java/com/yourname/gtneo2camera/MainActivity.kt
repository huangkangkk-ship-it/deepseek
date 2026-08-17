package com.yourname.gtneo2camera

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
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

class MainActivity : AppCompatActivity() {
    private lateinit var glView: GLSurfaceView
    private lateinit var renderer: CameraRenderer
    private var cameraDevice: CameraDevice? = null
    private var cameraSession: CameraCaptureSession? = null
    private var mediaRecorder: MediaRecorder? = null
    private var glRecorder: GLRecorder? = null
    private val cameraHandler = Handler(Looper.getMainLooper())
    private val recordHandler = Handler(Looper.getMainLooper())

    private val recordRunnable = object : Runnable {
        override fun run() {
            if (mediaRecorder != null) {
                glView.queueEvent { glRecorder?.drawFrame() }
                recordHandler.postDelayed(this, 33L)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        glView = findViewById(R.id.glSurfaceView)
        renderer = CameraRenderer(this)
        glView.setEGLContextClientVersion(3)
        glView.setRenderer(renderer)
        glView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

        val seek = findViewById<SeekBar>(R.id.seekBar)
        seek.max = 200
        seek.progress = 100
        seek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(bar: SeekBar?, progress: Int, fromUser: Boolean) {
                renderer.setTint(progress - 100)
            }
            override fun onStartTrackingTouch(bar: SeekBar?) = Unit
            override fun onStopTrackingTouch(bar: SeekBar?) = Unit
        })

        findViewById<Button>(R.id.btnRecord).setOnClickListener {
            if (mediaRecorder == null) startRecording() else stopRecording()
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO), 100)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 100 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) openCamera()
    }

    private fun openCamera() {
        val manager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val id = manager.cameraIdList.firstOrNull() ?: error("No camera available")
            manager.openCamera(id, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    startPreview()
                }
                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    if (cameraDevice === camera) cameraDevice = null
                }
                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    if (cameraDevice === camera) cameraDevice = null
                }
            }, cameraHandler)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun startPreview() {
        val texture = SurfaceTexture(0)
        texture.setDefaultBufferSize(1920, 1080)
        val surface = Surface(texture)
        renderer.setSurfaceTexture(texture)
        cameraDevice?.createCaptureSession(
            listOf(surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    cameraSession = session
                    val request = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                    request.addTarget(surface)
                    session.setRepeatingRequest(request.build(), null, cameraHandler)
                }
                override fun onConfigureFailed(session: CameraCaptureSession) = Unit
            },
            cameraHandler
        )
    }

    private fun startRecording() {
        val file = File(filesDir, "recorded_${System.currentTimeMillis()}.mp4")
        val recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setOutputFile(file.absolutePath)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoSize(1920, 1080)
            setVideoFrameRate(30)
            prepare()
        }

        glView.queueEvent {
            glRecorder = GLRecorder(renderer, renderer.getEglContext())
            glRecorder?.startRecording(recorder.surface, 1920, 1080)
            recorder.start()
            mediaRecorder = recorder
        }
        findViewById<Button>(R.id.btnRecord).text = "停止录制"
        recordHandler.post(recordRunnable)
    }

    private fun stopRecording() {
        recordHandler.removeCallbacks(recordRunnable)
        mediaRecorder?.apply {
            try { stop() } catch (_: RuntimeException) { }
            release()
        }
        glView.queueEvent {
            glRecorder?.stopRecording()
            glRecorder = null
        }
        mediaRecorder = null
        findViewById<Button>(R.id.btnRecord).text = "开始录制"
    }

    override fun onPause() {
        recordHandler.removeCallbacks(recordRunnable)
        mediaRecorder?.release()
        mediaRecorder = null
        cameraSession?.close()
        cameraSession = null
        cameraDevice?.close()
        cameraDevice = null
        super.onPause()
    }

    override fun onDestroy() {
        cameraHandler.removeCallbacksAndMessages(null)
        recordHandler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }
}
