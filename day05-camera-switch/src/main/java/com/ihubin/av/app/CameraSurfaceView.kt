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
import java.lang.Exception

class CameraSurfaceView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, ICamera {

    companion object {
        private const val TAG = "CameraSurfaceView"
    }

    private var frontCamera: Int? = null
    private var backCamera: Int? = null
    private var currentCamera: Int? = null

    @Suppress("DEPRECATION")
    private var mCamera: Camera? = null

    @Suppress("DEPRECATION")
    private var mCameraParameters: Camera.Parameters? = null

    private val mDefaultAspectRatio = AspectRatio.of(16, 9)
//    private val DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3)

    init {
        holder.addCallback(this)

        checkCamera()
    }

    constructor(context: Context) : this(context, null, 0)

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.e(TAG, "surfaceCreated: ")
        // 预览
        openCamera()
        startPreview()
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
     * 检测相机
     */
    @Suppress("DEPRECATION")
    override fun checkCamera() {
        val number: Int = Camera.getNumberOfCameras()
        val cameraInfo = Camera.CameraInfo()
        for (i in 0 until number) {
            Camera.getCameraInfo(i, cameraInfo)
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                backCamera = i
            } else if(cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                frontCamera = i
            }
        }
        // 默认使用后置摄像头
        currentCamera = backCamera
    }

    /**
     * 打开相机
     */
    @Suppress("DEPRECATION")
    override fun openCamera() {
        if (ActivityCompat.checkSelfPermission(context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.CAMERA), 0X01)
            return
        }

        mCamera = Camera.open(currentCamera!!)
        CameraUtil.setCameraDisplayOrientation(context as Activity, currentCamera!!, mCamera!!)
        mCameraParameters = mCamera?.parameters

        val mPreviewSizes = SizeMap()
        val supportedPreviewSizes = mCameraParameters!!.supportedPreviewSizes
        mPreviewSizes.clear()
        for(size in supportedPreviewSizes) {
            mPreviewSizes.add(Size(size.width, size.height))
            Log.i(TAG, "支持的相机尺寸：width: ${size.width}, height: ${size.height}")
        }
        val sizes = mPreviewSizes.sizes(mDefaultAspectRatio)
        val lastSize = sizes?.last()
        lastSize?.let {
            Log.i(TAG, "最终预览尺寸：${it.width}:${it.height}")
            mCameraParameters?.setPreviewSize(it.width, it.height)
        }

        setAutoFocusInternal(true)
        try {
            mCamera?.parameters = mCameraParameters
        } catch (e: Exception) {
            e.message?.let { Log.e("openCamera setParameter", it) }
        }
    }

    /**
     * 开始预览
     */
    @Suppress("DEPRECATION", "SameParameterValue")
    override fun startPreview() {
        mCamera?.setPreviewCallback { _, _ -> }
        try {
            mCamera?.setPreviewDisplay(holder)
            mCamera?.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 停止预览
     */
    @Suppress("DEPRECATION", "SameParameterValue")
    override fun stopPreview() {
        try {
            mCamera?.stopPreview()
            mCamera?.setPreviewCallback(null)
            mCamera?.setPreviewDisplay(null)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 关闭相机
     */
    @Suppress("DEPRECATION")
    override fun releaseCamera() {
        Log.d(TAG, "releaseCamera")
        stopPreview()
        try {
            mCamera?.release()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        mCamera = null
    }

    @Suppress("DEPRECATION", "SameParameterValue")
    private fun setAutoFocusInternal(autoFocus: Boolean): Boolean {
        return if (mCamera != null) {
            val modes: List<String> = mCameraParameters!!.supportedFocusModes
            if (autoFocus && modes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                mCameraParameters?.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_FIXED)) {
                mCameraParameters?.focusMode = Camera.Parameters.FOCUS_MODE_FIXED
            } else if (modes.contains(Camera.Parameters.FOCUS_MODE_INFINITY)) {
                mCameraParameters?.focusMode = Camera.Parameters.FOCUS_MODE_INFINITY
            } else {
                mCameraParameters?.focusMode = modes[0]
            }
            true
        } else {
            false
        }
    }

    @Suppress("DEPRECATION")
    override fun openFlash() {
        //打开闪光灯
        mCameraParameters?.flashMode = Camera.Parameters.FLASH_MODE_TORCH
        try {
            mCamera?.parameters = mCameraParameters
        } catch (e: Exception) {
            e.message?.let { Log.e("openFlash Faild", it) }
        }
    }

    @Suppress("DEPRECATION")
    override fun closeFlash() {
        //关闭闪光灯
        mCameraParameters?.flashMode = Camera.Parameters.FLASH_MODE_OFF
        try {
            mCamera?.parameters = mCameraParameters
        } catch (e: Exception) {
            e.message?.let { Log.e("closeFlash Faild", it) }
        }
    }

    override fun switchToFront() {
        releaseCamera()
        currentCamera = frontCamera
        openCamera()
        startPreview()
    }

    override fun switchToBack() {
        releaseCamera()
        currentCamera = backCamera
        openCamera()
        startPreview()
    }

}