package com.ihubin.av.app

import android.app.Activity
import android.content.Context
import android.graphics.ImageFormat
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.Image
import android.os.Build
import android.view.Surface
import java.nio.ByteBuffer


object CameraUtil {

    @Suppress("DEPRECATION")
    fun setCameraDisplayOrientation(
        activity: Activity,
        cameraId: Int,
        camera: Camera
    ) {
        val info = CameraInfo()
        Camera.getCameraInfo(cameraId, info)
        val rotation = activity.windowManager.defaultDisplay.rotation
        var degrees = 0
        when (rotation) {
            Surface.ROTATION_0 -> degrees = 0
            Surface.ROTATION_90 -> degrees = 90
            Surface.ROTATION_180 -> degrees = 180
            Surface.ROTATION_270 -> degrees = 270
            else -> {
            }
        }
        var result: Int
        if (info.facing == CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360
            result = (360 - result) % 360 // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360
        }
        camera.setDisplayOrientation(result)
    }


    @Suppress("ObsoleteSdkInt", "Unused")
    fun hasCamera2(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            false
        } else try {
            val manager =
                context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val idList = manager.cameraIdList
            var notFull = true
            if (idList.isEmpty()) {
                notFull = false
            } else {
                for (str in idList) {
                    val characteristics =
                        manager.getCameraCharacteristics(str!!)
                    val supportLevel =
                        characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)!!
                    if (supportLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
                        notFull = false
                        break
                    }
                }
            }
            !notFull
        } catch (exp: Exception) {
            false
        }
    }

    fun toYUV420Data(image: Image): ByteArray? {
        val imageWidth: Int = image.width
        val imageHeight: Int = image.height
        val planes: Array<Image.Plane> = image.planes
        val data = ByteArray(
            imageWidth * imageHeight *
                    ImageFormat.getBitsPerPixel(ImageFormat.YUV_420_888) / 8
        )
        var offset = 0
        for (plane in planes.indices) {
            val buffer: ByteBuffer = planes[plane].buffer
            val rowStride: Int = planes[plane].rowStride
            // Experimentally, U and V planes have |pixelStride| = 2, which
            // essentially means they are packed.
            val pixelStride: Int = planes[plane].pixelStride
            val planeWidth = if ((plane == 0)) imageWidth else imageWidth / 2
            val planeHeight = if ((plane == 0)) imageHeight else imageHeight / 2
            if (pixelStride == 1 && rowStride == planeWidth) {
                // Copy whole plane from buffer into |data| at once.
                buffer.get(data, offset, planeWidth * planeHeight)
                offset += planeWidth * planeHeight
            } else {
                // Copy pixels one by one respecting pixelStride and rowStride.
                val rowData = ByteArray(rowStride)
                for (row in 0 until planeHeight - 1) {
                    buffer.get(rowData, 0, rowStride)
                    for (col in 0 until planeWidth) {
                        data[offset++] = rowData[col * pixelStride]
                    }
                }
                // Last row is special in some devices and may not contain the full
                // |rowStride| bytes of data.
                // See http://developer.android.com/reference/android/media/Image.Plane.html#getBuffer()
                buffer.get(rowData, 0, Math.min(rowStride, buffer.remaining()))
                for (col in 0 until planeWidth) {
                    data[offset++] = rowData[col * pixelStride]
                }
            }
        }
        return data
    }

}