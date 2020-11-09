package com.ihubin.av.app

import android.app.Activity
import android.content.Context
import android.hardware.Camera
import android.hardware.Camera.CameraInfo
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.view.Surface


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
}