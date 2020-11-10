package com.ihubin.av.app

interface ICamera {

    fun startPreview()

    fun stopPreview()

    fun checkCamera()

    fun openCamera()

    fun releaseCamera()

    fun openFlash()

    fun closeFlash()

    fun switchToFront()

    fun switchToBack()

}