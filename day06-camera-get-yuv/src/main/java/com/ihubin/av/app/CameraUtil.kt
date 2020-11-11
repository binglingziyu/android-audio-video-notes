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

    // https://github.com/githubhaohao/OpenGLCamera2  CameraUtil
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

    // https://www.cnblogs.com/cmai/p/8372607.html
    fun rotateYUV420Degree90(data: ByteArray, imageWidth: Int, imageHeight: Int): ByteArray? {
        val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
        var i = 0
        for (x in 0 until imageWidth) {
            for (y in imageHeight - 1 downTo 0) {
                yuv[i] = data[y * imageWidth + x]
                i++
            }
        }
        i = imageWidth * imageHeight * 3 / 2 - 1
        var x = imageWidth - 1
        while (x > 0) {
            for (y in 0 until imageHeight / 2) {
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth + x]
                i--
                yuv[i] = data[imageWidth * imageHeight + y * imageWidth
                        + (x - 1)]
                i--
            }
            x -= 2
        }
        return yuv
    }

    // https://www.cnblogs.com/cmai/p/8372607.html
    private fun rotateYUV420Degree180(
        data: ByteArray,
        imageWidth: Int,
        imageHeight: Int
    ): ByteArray? {
        val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
        var count = 0
        var i: Int = imageWidth * imageHeight - 1
        while (i >= 0) {
            yuv[count] = data[i]
            count++
            i--
        }
        i = imageWidth * imageHeight * 3 / 2 - 1
        while (i >= imageWidth
            * imageHeight
        ) {
            yuv[count++] = data[i - 1]
            yuv[count++] = data[i]
            i -= 2
        }
        return yuv
    }

    // https://www.cnblogs.com/cmai/p/8372607.html
    fun rotateYUV420Degree270(
        data: ByteArray, imageWidth: Int,
        imageHeight: Int
    ): ByteArray? {
        val yuv = ByteArray(imageWidth * imageHeight * 3 / 2)
        var nWidth = 0
        var nHeight = 0
        var wh = 0
        var uvHeight = 0
        if (imageWidth != nWidth || imageHeight != nHeight) {
            nWidth = imageWidth
            nHeight = imageHeight
            wh = imageWidth * imageHeight
            uvHeight = imageHeight shr 1 // uvHeight = height / 2
        }
        var k = 0
        for (i in 0 until imageWidth) {
            var nPos = 0
            for (j in 0 until imageHeight) {
                yuv[k] = data[nPos + i]
                k++
                nPos += imageWidth
            }
        }
        var i = 0
        while (i < imageWidth) {
            var nPos = wh
            for (j in 0 until uvHeight) {
                yuv[k] = data[nPos + i]
                yuv[k + 1] = data[nPos + i + 1]
                k += 2
                nPos += imageWidth
            }
            i += 2
        }
        return rotateYUV420Degree90(data, imageWidth, imageHeight)?.let {
            rotateYUV420Degree180(
                it,
                imageWidth,
                imageHeight
            )
        }
    }

}