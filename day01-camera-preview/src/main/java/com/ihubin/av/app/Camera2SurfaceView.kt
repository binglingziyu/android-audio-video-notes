package com.ihubin.av.app

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.app.ActivityCompat

class Camera2SurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {
    private var mContext: Context? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mWorkHandler: Handler? = null
    private var mCameraId: String? = null
    private var mCameraDevice: CameraDevice? = null
    private var mImageReader: ImageReader? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null

    constructor(context: Context?) : this(context, null) {}
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {}

    override fun surfaceCreated(holder: SurfaceHolder) {
        val handlerThread = HandlerThread("camera2")
        handlerThread.start()
        mWorkHandler = Handler(handlerThread.looper)
        checkCamera()
        openCamera()
    }

    /**
     * 检测相机
     */
    private fun checkCamera() {
        val cameraManager =
            mContext!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraIdList = cameraManager.cameraIdList
            for (s in cameraIdList) {
                val characteristics =
                    cameraManager.getCameraCharacteristics(s)
                val lensFacing = characteristics.get(CameraCharacteristics.LENS_FACING)
                val sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)
                val supportedHardwareLevel =
                    characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)
                if (lensFacing != null && lensFacing == CameraCharacteristics.LENS_FACING_BACK) {
                    mCameraId = s
                    break
                }
            }
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }
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
            return
        }
        if (mCameraId == null) {
            return
        }
        val cameraManager =
            mContext!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraManager.openCamera(mCameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    mCameraDevice = camera
                    mImageReader =
                        ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 8)
                    mImageReader?.setOnImageAvailableListener({ reader ->
                        val image: Image = reader.acquireLatestImage()
                        //我们可以将这帧数据转成字节数组，类似于Camera1的PreviewCallback回调的预览帧数据
                        //ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        //byte[] data = new byte[buffer.remaining()];
                        //buffer.get(data);
                        image.close()
                    }, mWorkHandler)
                    createCameraPreview()
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
    private fun createCameraPreview() {
        try {
            val captureRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val surface: Surface = mSurfaceHolder!!.surface
            captureRequestBuilder.addTarget(surface)
            val imageReaderSurface: Surface = mImageReader!!.surface
            captureRequestBuilder.addTarget(imageReaderSurface)
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO
            )
            mCameraDevice!!.createCaptureSession(
                listOf(surface, imageReaderSurface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        mCameraCaptureSession = session
                        val captureRequest = captureRequestBuilder.build()
                        try {
                            session.setRepeatingRequest(captureRequest, null, null)
                        } catch (e: CameraAccessException) {
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                },
                mWorkHandler
            )
        } catch (e: Exception) {
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        closeCameraPreview()
        if (mCameraDevice != null) {
            mCameraDevice!!.close()
        }
        if (mImageReader != null) {
            mImageReader!!.close()
        }
        mWorkHandler!!.looper.quitSafely()
    }

    private fun closeCameraPreview() {
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession!!.stopRepeating()
                mCameraCaptureSession!!.abortCaptures()
                mCameraCaptureSession!!.close()
            } catch (e: Exception) {
            }
            mCameraCaptureSession = null
        }
    }

    init {
        mContext = context
        mSurfaceHolder = holder
        mSurfaceHolder!!.addCallback(this)
    }
}