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

class Camera2SurfaceView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SurfaceView(context, attrs, defStyleAttr), SurfaceHolder.Callback {

    companion object {
        private const val TAG = "Camera2SurfaceView"
    }

    private var mContext: Context? = null
    private var mSurfaceHolder: SurfaceHolder? = null
    private var mWorkHandler: Handler? = null
    private var mCameraId: String? = null
    private var mCameraDevice: CameraDevice? = null
    private var mImageReader: ImageReader? = null
    private var mCameraCaptureSession: CameraCaptureSession? = null
    private val mPreviewSizes = SizeMap()
    //    private val DEFAULT_ASPECT_RATIO = AspectRatio.of(4, 3)
    private val DEFAULT_ASPECT_RATIO = AspectRatio.of(16, 9)

    constructor(context: Context?) : this(context, null) {}
    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0) {}

    init {
        mContext = context
        mSurfaceHolder = holder
        mSurfaceHolder?.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        val handlerThread = HandlerThread("camera2")
        handlerThread.start()
        mWorkHandler = Handler(handlerThread.looper)
//        if(firstInit) {
//            checkCamera()
//            openCamera()
//        }
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

                if (lensFacing == null || lensFacing != CameraCharacteristics.LENS_FACING_BACK) {
                    continue
                }
                mCameraId = s

                mPreviewSizes.clear()
                //获取相机输出格式/尺寸参数
                val configs = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                val outputSizeList = configs!!.getOutputSizes(SurfaceHolder::class.java)
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
                mContext!!,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(mContext as Activity, arrayOf(Manifest.permission.CAMERA), 0X01)
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
                    val sizes = mPreviewSizes.sizes(DEFAULT_ASPECT_RATIO)
                    val lastSize = sizes?.last()
                    mImageReader =
                        ImageReader.newInstance(lastSize!!.width, lastSize.height, ImageFormat.YUV_420_888, 8)
//                    mImageReader =
//                        ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 8)
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
            val sizes = mPreviewSizes.sizes(DEFAULT_ASPECT_RATIO)
            val lastSize = sizes?.last()
            lastSize?.let {
                mSurfaceHolder?.setFixedSize(lastSize.width, lastSize.height)
                Log.e(TAG, " mSurfaceHolder == null ? " + (mSurfaceHolder == null))
                Log.e(TAG, " mSurfaceHolder.isCreating ? " + (mSurfaceHolder?.isCreating))
            }

            Log.i(TAG, "最终选择：${lastSize!!.width} / ${lastSize!!.height}")

            val captureRequestBuilder =
                mCameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

            Log.e(TAG, " mSurfaceHolder == null ? " + (mSurfaceHolder == null))
            Log.e(TAG, " mSurfaceHolder.isCreating ? " + (mSurfaceHolder?.isCreating))

//            //根据TextureView 和 选定的 previewSize 创建用于显示预览数据的Surface
//            SurfaceTexture surfaceTexture = previewView.getSurfaceTexture();
//            surfaceTexture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());//设置SurfaceTexture缓冲区大小
//            final Surface previewSurface = new Surface(surfaceTexture);
            val surface: Surface = mSurfaceHolder!!.surface
            captureRequestBuilder.addTarget(surface)

//            val imageReaderSurface: Surface = mImageReader!!.surface
//            captureRequestBuilder.addTarget(imageReaderSurface)
            captureRequestBuilder.set(
                CaptureRequest.CONTROL_MODE,
                CaptureRequest.CONTROL_MODE_AUTO
            )
            mCameraDevice!!.createCaptureSession(
                listOf(surface),
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

    var firstInit = false

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        Log.e(TAG, "surfaceChanged: $width / $height")
        if(!firstInit) {
            checkCamera()
            openCamera()
            firstInit = true
        } else {
//            checkCamera()
//            openCamera()
            closeCameraPreview()
            createCameraPreview()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        firstInit = false
        closeCameraPreview()
        mCameraDevice?.close()
        mImageReader?.close()
        mWorkHandler?.looper?.quitSafely()
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

}