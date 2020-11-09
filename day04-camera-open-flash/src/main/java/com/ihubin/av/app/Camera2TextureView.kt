package com.ihubin.av.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.Image
import android.media.ImageReader
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.util.AttributeSet
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.TextureView.SurfaceTextureListener
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.ihubin.av.app.base.AspectRatio
import com.ihubin.av.app.base.Size
import com.ihubin.av.app.base.SizeMap

class Camera2TextureView(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
    TextureView(context, attrs, defStyleAttr), SurfaceTextureListener, ICamera {

    companion object {
        private const val TAG = "Camera2TextureView"
    }

    private var mWorkHandler: Handler? = null
    private var mCameraId: String? = null
    private var mCameraDevice: CameraDevice? = null
    private var mImageReader: ImageReader? = null
    private var mCaptureRequestBuilder: CaptureRequest.Builder? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private val mPreviewSizes = SizeMap()
    private val mDefaultAspectRatio = AspectRatio.of(16, 9)
//    private val DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3)

    init {
        surfaceTextureListener = this
    }

    constructor(context: Context) : this(context, null, 0) {}

    override fun onSurfaceTextureAvailable(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
        val handlerThread = HandlerThread("camera2")
        handlerThread.start()
        mWorkHandler = Handler(handlerThread.looper)
        checkCamera()
        openCamera()
    }

    override fun onSurfaceTextureSizeChanged(
        surface: SurfaceTexture,
        width: Int,
        height: Int
    ) {
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        closeCameraPreview()
        mCameraDevice?.close()
        mImageReader?.close()
        mWorkHandler?.looper?.quitSafely()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    /**
     * 检测相机
     */
    private fun checkCamera() {
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

                if (lensFacing == null || lensFacing != CameraCharacteristics.LENS_FACING_BACK) {
                    continue
                }
                mCameraId = s

                //获取相机输出格式/尺寸参数
                val configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val outputSizeList = configs!!.getOutputSizes(SurfaceTexture::class.java)
                for(size in outputSizeList) {
                    mPreviewSizes.add(Size(size.width, size.height))
                    Log.i(TAG, "支持的相机尺寸：width: ${size.width}, height: ${size.height}")
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
                context,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.CAMERA), 0X01)
            return
        }
        if (mCameraId == null) {
            return
        }
        val cameraManager =
            context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraManager.openCamera(mCameraId!!, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    mCameraDevice = camera
                    val sizes = mPreviewSizes.sizes(mDefaultAspectRatio)
                    val lastSize = sizes?.last()
                    mImageReader =
                        ImageReader.newInstance(lastSize!!.width, lastSize.height, ImageFormat.YUV_420_888, 2)
                    mImageReader!!.setOnImageAvailableListener({ reader ->
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
            mCaptureRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
            val sizes = mPreviewSizes.sizes(mDefaultAspectRatio)
            val lastSize = sizes?.last()
            lastSize?.let {
                surfaceTexture?.setDefaultBufferSize(it.width, it.height)
                Log.i(TAG, "最终预览尺寸：${it.width}:${it.height}")
            }
            val surface = Surface(surfaceTexture)
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

    private fun closeCameraPreview() {
        try {
            mCameraCaptureSession?.stopRepeating()
            mCameraCaptureSession?.abortCaptures()
            mCameraCaptureSession?.close()
        } catch (e: Exception) {
        }
        mCameraCaptureSession = null
    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun openFlash() {
        mCaptureRequestBuilder!!.set(
            CaptureRequest.FLASH_MODE,
            CaptureRequest.FLASH_MODE_TORCH
        )
        val mCaptureRequest = mCaptureRequestBuilder!!.build()
        mCameraCaptureSession!!.setRepeatingRequest(mCaptureRequest, null, mWorkHandler)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun closeFlash() {
        mCaptureRequestBuilder!!.set(
            CaptureRequest.FLASH_MODE,
            CaptureRequest.FLASH_MODE_OFF
        )
        val mCaptureRequest = mCaptureRequestBuilder!!.build()
        mCameraCaptureSession!!.setRepeatingRequest(mCaptureRequest, null, mWorkHandler)
    }

}