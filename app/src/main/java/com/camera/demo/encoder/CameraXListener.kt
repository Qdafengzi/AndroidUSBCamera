package com.camera.demo.encoder

interface CameraXListener {
    fun click()

    fun recordStart()
    fun recordStop()

    fun recordPrepareError(msg:String)
    fun recordError(msg: String)
    fun stopError(msg:String)
    fun captureError()
}
