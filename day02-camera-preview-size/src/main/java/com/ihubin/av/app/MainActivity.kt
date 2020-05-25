package com.ihubin.av.app

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 0X01)
    }

    fun cameraSurfaceView(view: View) {
        cameraPreview(0)
    }
    fun cameraTextureView(view: View) {
        cameraPreview(1)
    }
    fun camera2SurfaceView(view: View) {
        cameraPreview(2)
    }
    fun camera2TextureView(view: View) {
        cameraPreview(3)
    }

    private fun cameraPreview(which: Int) {
        val previewType: Int = when (which) {
            0 -> {
                CameraPreviewActivity.TYPE_CAMERA_SURFACE_VIEW
            }
            1 -> {
                CameraPreviewActivity.TYPE_CAMERA_TEXTURE_VIEW
            }
            2 -> {
                CameraPreviewActivity.TYPE_CAMERA2_SURFACE_VIEW
            }
            else -> {
                CameraPreviewActivity.TYPE_CAMERA2_TEXTURE_VIEW
            }
        }
        val intent = Intent(this@MainActivity, CameraPreviewActivity::class.java)
        intent.putExtra(CameraPreviewActivity.PREVIEW_TYPE, previewType)
        startActivity(intent)
    }
}
