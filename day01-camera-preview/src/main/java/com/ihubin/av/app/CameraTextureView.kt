package com.ihubin.av.app

import android.app.Activity
import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.AttributeSet
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import java.io.IOException

class CameraTextureView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    TextureView(context, attrs, defStyleAttr), SurfaceTextureListener {
    private var mCamera: Camera? = null
    private val mActivity: Activity?

    constructor(context: Context?) : this(context, null) {}
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {}

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        openCamera()
        startPreview(surface)
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        releaseCamera()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    /**
     * 打开相机
     */
    private fun openCamera() {
        val number: Int = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until number) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                // 打开后置摄像头
                mCamera = Camera.open(i)
                CameraUtil.setCameraDisplayOrientation(mActivity!!, i, mCamera!!)
            }
        }
    }

    /**
     * 开始预览
     *
     * @param texture
     */
    private fun startPreview(texture: SurfaceTexture) {
        if (mCamera != null) {
            mCamera!!.setPreviewCallback({ data, camera -> })
            try {
                mCamera!!.setPreviewTexture(texture)
                mCamera!!.startPreview()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 关闭相机
     */
    private fun releaseCamera() {
        if (mCamera != null) {
            try {
                mCamera!!.stopPreview()
                mCamera!!.setPreviewCallback(null)
                mCamera!!.setPreviewDisplay(null)
                mCamera!!.release()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            mCamera = null
        }
    }

    init {
        mActivity = context as Activity
        surfaceTextureListener = this
    }
}