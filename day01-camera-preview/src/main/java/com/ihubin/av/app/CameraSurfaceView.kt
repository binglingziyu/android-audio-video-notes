package com.ihubin.av.app

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import java.io.IOException

class CameraSurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    private var mCamera: Camera? = null
    private val mActivity: Activity?

    constructor(context: Context?) : this(context, null) {}
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.e(TAG, "surfaceCreated: ")
        // 异步预览
        openCamera()
        startPreview(holder)
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        Log.e(TAG, "surfaceChanged: ")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.e(TAG, "surfaceDestroyed: ")
        releaseCamera()
    }

    /**
     * 打开相机
     */
    private fun openCamera() {
        val number: Int = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until number) {
            Camera.getCameraInfo(i, cameraInfo)
            // 打开后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(i)
                CameraUtil.setCameraDisplayOrientation(mActivity!!, i, mCamera!!)
                break
            }
        }
    }

    /**
     * 开始预览
     *
     * @param holder
     */
    private fun startPreview(holder: SurfaceHolder) {
        if (mCamera != null) {
            mCamera!!.setPreviewCallback({ data, camera -> })
            try {
                mCamera!!.setPreviewDisplay(holder)
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
        Log.e(TAG, "releaseCamera: ")
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

    companion object {
        private const val TAG = "CameraSurfaceView"
    }

    init {
        holder.addCallback(this)
        mActivity = context as Activity?
    }

}