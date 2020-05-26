package com.ihubin.av.app

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout


class CameraPreviewActivity : AppCompatActivity() {

    companion object {
        const val PREVIEW_TYPE = "preview_type"
        const val TYPE_CAMERA_SURFACE_VIEW = 0x01
        const val TYPE_CAMERA_TEXTURE_VIEW = 0x02
        const val TYPE_CAMERA2_SURFACE_VIEW = 0x03
        const val TYPE_CAMERA2_TEXTURE_VIEW = 0x04
    }

    private var mPreviewType = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        mPreviewType = intent.getIntExtra(PREVIEW_TYPE, TYPE_CAMERA_SURFACE_VIEW);
        previewCamera();
    }

    private fun previewCamera() {
        val root: ConstraintLayout = findViewById(R.id.cl_root)
        val layoutParams = ConstraintLayout.LayoutParams(1080, 1440)
        if (mPreviewType == TYPE_CAMERA_SURFACE_VIEW) {
            val view = CameraSurfaceView(this)
            root.addView(view, layoutParams)
        } else if (mPreviewType == TYPE_CAMERA_TEXTURE_VIEW) {
            val view = CameraTextureView(this)
            root.addView(view, layoutParams)
        } else if (mPreviewType == TYPE_CAMERA2_SURFACE_VIEW) {
            val view = Camera2SurfaceView(this)
            root.addView(view, layoutParams)
        } else {
            val view = Camera2TextureView(this)
            root.addView(view, layoutParams)
        }
    }

}