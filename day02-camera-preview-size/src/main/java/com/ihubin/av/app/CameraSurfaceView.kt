package com.ihubin.av.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import com.ihubin.av.app.base.AspectRatio
import com.ihubin.av.app.base.Size
import com.ihubin.av.app.base.SizeMap
import java.io.IOException


class CameraSurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "CameraSurfaceView"
    }

    private var mCamera: Camera? = null
    private var mContext: Context? = null

    constructor(context: Context?) : this(context, null) {}
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {}

    init {
        holder.addCallback(this)
        mContext = context
    }

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
        Log.i(TAG, "surfaceChanged: width[$width], height[$height]")
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.e(TAG, "surfaceDestroyed: ")
        releaseCamera()
    }

    /**
     * 打开相机
     */
    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(
                mContext!!,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(mContext as Activity, arrayOf(Manifest.permission.CAMERA), 0X01)
            return
        }
        val number: Int = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until number) {
            Camera.getCameraInfo(i, cameraInfo)
            // 打开后置摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                mCamera = Camera.open(i)
                CameraUtil.setCameraDisplayOrientation(mContext as Activity, i, mCamera!!)
//                mCamera?.setDisplayOrientation(cameraInfo.orientation)
                val parameters = mCamera?.parameters;

                val mPreviewSizes = SizeMap()
                val supportedPreviewSizes = parameters!!.supportedPreviewSizes
                mPreviewSizes.clear()
                for(size in supportedPreviewSizes) {
                    mPreviewSizes.add(Size(size.width, size.height))
                }
                val DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3)
                val sizes = mPreviewSizes.sizes(DEFAULT_ASPECT_RATIO)
                val lastSize = sizes?.last()
                lastSize?.let {
                    Log.i(TAG, "最终预览尺寸：${it.width}:${it.height}")
                    parameters.setPreviewSize(it.width, it.height)
                }
                mCamera?.parameters = parameters
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
        mCamera?.setPreviewCallback({ data, camera -> })
        try {
            mCamera?.setPreviewDisplay(holder)
            mCamera?.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 关闭相机
     */
    private fun releaseCamera() {
        Log.e(TAG, "releaseCamera: ")
        try {
            mCamera?.stopPreview()
            mCamera?.setPreviewCallback(null)
            mCamera?.setPreviewDisplay(null)
            mCamera?.release()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mCamera = null
    }

}