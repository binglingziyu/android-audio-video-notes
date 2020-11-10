package com.ihubin.av.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat
import com.ihubin.av.app.base.AspectRatio
import com.ihubin.av.app.base.Size
import com.ihubin.av.app.base.SizeMap
import java.io.File

class Camera2SurfaceView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback, ICamera {

    companion object {
        private const val TAG = "Camera2SurfaceView"
    }

    private var frontCameraId: String? = null
    private var backCameraId: String? = null
    private var currentCameraId: String? = null

    private var mWorkHandler: Handler? = null
    private var mCameraDevice: CameraDevice? = null
    private var mImageReader: ImageReader? = null
    private var mCaptureRequestBuilder: CaptureRequest.Builder? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private val mPreviewSizes = SizeMap()
    private val mDefaultAspectRatio = AspectRatio.of(16, 9)
    //    private val mDefaultAspectRatio = AspectRatio.of(4, 3)

    init {
        holder.addCallback(this)

        checkCamera()
    }

    constructor(context: Context) : this(context, null, 0)

    override fun surfaceCreated(holder: SurfaceHolder) {
        val handlerThread = HandlerThread("camera2")
        handlerThread.start()
        mWorkHandler = Handler(handlerThread.looper)
    }

    private var firstInit = false

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        Log.e(TAG, "surfaceChanged: $width / $height")
        if(!firstInit) {
            openCamera()
            firstInit = true
        } else {
            stopPreview()
            startPreview()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceDestroyed")
        firstInit = false
        releaseCamera()
        mWorkHandler?.looper?.quitSafely()
    }

    /**
     * 检测相机
     */
    override fun checkCamera() {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraIdList = cameraManager.cameraIdList
            for (s in cameraIdList) {
                val characteristics =
                    cameraManager.getCameraCharacteristics(s)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
//                val sensorOrientation =
//                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
//                val supportedHardwareLevel =
//                    characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)

                if (lensFacing == null) {
                    continue
                } else if(lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    backCameraId = s

                    mPreviewSizes.clear()
                    //获取相机输出格式/尺寸参数
                    val configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val outputSizeList = configs!!.getOutputSizes(SurfaceHolder::class.java)
                    for(size in outputSizeList) {
                        mPreviewSizes.add(Size(size.width, size.height))
                        Log.i(TAG, "后置支持的相机尺寸：width: ${size.width}, height: ${size.height}")
                    }
                } else if(lensFacing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = s

                    mPreviewSizes.clear()
                    //获取相机输出格式/尺寸参数
                    val configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val outputSizeList = configs!!.getOutputSizes(SurfaceHolder::class.java)
                    for(size in outputSizeList) {
                        mPreviewSizes.add(Size(size.width, size.height))
                        Log.i(TAG, "前置支持的相机尺寸：width: ${size.width}, height: ${size.height}")
                    }
                }
                // 默认使用后置摄像头
                currentCameraId = backCameraId
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }


    val storeFile = File(context.getExternalFilesDir(""), "preview_2592x1458.nv21")

    /**
     * 打开相机
     */
    override fun openCamera() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                context as Activity,
                arrayOf(Manifest.permission.CAMERA),
                0X01
            )
            return
        }
        if (currentCameraId == null) {
            return
        }

        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraManager.openCamera(currentCameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    mCameraDevice = camera
                    val sizes = mPreviewSizes.sizes(mDefaultAspectRatio)
                    val lastSize = sizes?.last()
                    mImageReader =
                        ImageReader.newInstance(
                            lastSize!!.width,
                            lastSize.height,
                            ImageFormat.YUV_420_888,
                            2
                        )
                    mImageReader?.setOnImageAvailableListener({ reader ->
                        val image: Image? = reader.acquireLatestImage()
                        if(image != null) {
                            val data = CameraUtil.toYUV420Data(image)
                            Log.d(TAG, "数据大小："+data?.size)
                            if (data != null) {
                                storeFile.appendBytes(data)
                            }
                        }
                        image?.close()
                    }, mWorkHandler)
                    // 开始预览
                    startPreview()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                    mCameraDevice = null
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    camera.close()
                    mCameraDevice = null
                }
            }, mWorkHandler)
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
    }

    /**
     * 相机预览
     */
    @Suppress("DEPRECATION")
    override fun startPreview() {
        try {
            val sizes = mPreviewSizes.sizes(mDefaultAspectRatio)
            val lastSize = sizes?.last()
            lastSize?.let {
                holder?.setFixedSize(lastSize.width, lastSize.height)
                Log.e(TAG, " mSurfaceHolder == null ? " + (holder == null))
                Log.e(TAG, " mSurfaceHolder.isCreating ? " + (holder?.isCreating))
            }

            Log.i(TAG, "最终选择：${lastSize!!.width} / ${lastSize.height}")

            mCaptureRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            val surface: Surface = holder!!.surface
            mCaptureRequestBuilder!!.addTarget(surface)

            val imageReaderSurface: Surface = mImageReader!!.surface
            mCaptureRequestBuilder!!.addTarget(imageReaderSurface)
            mCaptureRequestBuilder!!.set(
                CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO
            )
            mCameraDevice!!.createCaptureSession(
                listOf(surface, imageReaderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCameraCaptureSession = session
                        val captureRequest = mCaptureRequestBuilder!!.build()
                        try {
                            session.setRepeatingRequest(captureRequest, captureCallback, mWorkHandler)
                        } catch (e: CameraAccessException) {
                        }
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                    }
                },
                mWorkHandler
            )

        } catch (e: Exception) {
        }
    }

    private var captureCallback = object: CameraCaptureSession.CaptureCallback() {

    }

    override fun stopPreview() {
        try {
            mCameraCaptureSession?.stopRepeating()
            mCameraCaptureSession?.abortCaptures()
            mCameraCaptureSession?.close()
        } catch (e: Exception) {
        }
        mCameraCaptureSession = null
    }

    override fun releaseCamera() {
        stopPreview()
        try {
            mCameraDevice?.close()
            mImageReader?.close()
        } catch (e: Exception) {

        }
    }

    override fun openFlash() {
        mCaptureRequestBuilder!!.set(
            CaptureRequest.FLASH_MODE,
            CaptureRequest.FLASH_MODE_TORCH
        )
        val mCaptureRequest = mCaptureRequestBuilder!!.build()
        mCameraCaptureSession!!.setRepeatingRequest(mCaptureRequest,  captureCallback, mWorkHandler)
    }

    override fun closeFlash() {
        mCaptureRequestBuilder!!.set(
            CaptureRequest.FLASH_MODE,
            CaptureRequest.FLASH_MODE_OFF
        )
        val mCaptureRequest = mCaptureRequestBuilder!!.build()
        mCameraCaptureSession!!.setRepeatingRequest(mCaptureRequest,  captureCallback, mWorkHandler)
    }

    override fun switchToFront() {
        releaseCamera()
        currentCameraId = frontCameraId
        openCamera()
    }

    override fun switchToBack() {
        releaseCamera()
        currentCameraId = backCameraId
        openCamera()
    }

}