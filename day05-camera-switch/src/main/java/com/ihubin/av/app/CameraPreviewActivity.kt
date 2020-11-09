package com.ihubin.av.app

import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
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
    private var camera:ICamera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_preview)

        mPreviewType = intent.getIntExtra(PREVIEW_TYPE, TYPE_CAMERA_SURFACE_VIEW)
        previewCamera()
        addControlBtn()
    }

    private fun previewCamera() {
        val root: ConstraintLayout = findViewById(R.id.cl_root)
        val layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, 0)
        layoutParams.dimensionRatio = "h,9:16"
//        layoutParams.dimensionRatio = "h,3:4"
        if (mPreviewType == TYPE_CAMERA_SURFACE_VIEW) {
            val view = CameraSurfaceView(this)
            camera = view
            root.addView(view, layoutParams)
        } else if (mPreviewType == TYPE_CAMERA_TEXTURE_VIEW) {
            val view = CameraTextureView(this)
            camera = view
            root.addView(view, layoutParams)
        } else if (mPreviewType == TYPE_CAMERA2_SURFACE_VIEW) {
            val view = Camera2SurfaceView(this)
            camera = view
            root.addView(view, layoutParams)
        } else {
            val view = Camera2TextureView(this)
            camera = view
            root.addView(view, layoutParams)
        }
    }

    var flashOpen = false

    var backCamera = true

    private fun addControlBtn() {
        val root: ConstraintLayout = findViewById(R.id.cl_root)
        val layoutParams = ConstraintLayout.LayoutParams(ConstraintLayout.LayoutParams.MATCH_PARENT, ConstraintLayout.LayoutParams.WRAP_CONTENT)
        layoutParams.leftToLeft = R.id.cl_root
        layoutParams.rightToRight = R.id.cl_root

        val linearLayout = LinearLayout(this)
        linearLayout.orientation = LinearLayout.HORIZONTAL
        val linearLayoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)

        val flashToggleButton = Button(this)
        flashToggleButton.text = "闪光灯"
        flashToggleButton.setOnClickListener {
            if(flashOpen) {
                camera?.closeFlash()
            } else {
                camera?.openFlash()
            }
            flashOpen = !flashOpen
        }
        linearLayout.addView(flashToggleButton, linearLayoutParams)

        val cameraToggleButton = Button(this)
        cameraToggleButton.text = "切换镜头"
        cameraToggleButton.setOnClickListener {
            if(backCamera) {
                camera?.switchToFront()
            } else {
                camera?.switchToBack()
            }
            backCamera = !backCamera
        }
        linearLayout.addView(cameraToggleButton, linearLayoutParams)

        root.addView(linearLayout, layoutParams)
    }

}