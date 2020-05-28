package com.ihubin.av.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.Camera
import android.util.AttributeSet
import android.util.Log
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.core.app.ActivityCompat
import com.ihubin.av.app.base.AspectRatio
import com.ihubin.av.app.base.Size
import com.ihubin.av.app.base.SizeMap
import java.io.IOException

class CameraTextureView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    TextureView(context, attrs, defStyleAttr), SurfaceTextureListener {

    companion object {
        private const val TAG = "CameraTextureView"
    }

    private var mCamera: Camera? = null
    private var mContext: Context? = null
//    private val DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3)
                private val DEFAULT_ASPECT_RATIO = AspectRatio.of(16, 9)

    constructor(context: Context?) : this(context, null)
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    init {
        mContext = context
        surfaceTextureListener = this
    }

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
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                // 打开后置摄像头
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

                val sizes = mPreviewSizes.sizes(DEFAULT_ASPECT_RATIO)
                val lastSize = sizes?.last()
                lastSize?.let {
                    Log.i(TAG, "最终预览尺寸：${it.width}:${it.height}")
                    parameters.setPreviewSize(it.width, it.height)
                }
                mCamera?.parameters = parameters
            }
        }
    }

    /**
     * 开始预览
     *
     * @param texture
     */
    private fun startPreview(texture: SurfaceTexture) {
        mCamera?.setPreviewCallback({ data, camera -> })
        try {
            mCamera?.setPreviewTexture(texture)
            mCamera?.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 关闭相机
     */
    private fun releaseCamera() {
        try {
            mCamera?.stopPreview()
            mCamera?.setPreviewCallback(null)
            mCamera?.setPreviewDisplay(null)
            mCamera?.release()
        } catch (e:IOException) {
            e.printStackTrace()
        }
        mCamera = null
    }
}