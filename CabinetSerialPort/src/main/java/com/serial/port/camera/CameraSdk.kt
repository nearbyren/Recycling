package com.serial.port.camera

import com.serial.port.ConfigurationSdk
import com.serial.port.utils.Loge
import java.io.File

class CameraSdk private constructor() {

    companion object {

        private var isInit = false
        private var cameraPortCore: CameraPortCore? = null

        fun init() {
            if (isInit) return
            isInit = true
            //
            val sdk: ConfigurationSdk = ConfigurationSdk.ConfigurationBuilder(File("/dev/ttyS2"), 115200)
                .log("TAG", true, false).build()
            cameraPortCore = CameraPortCore()
            CameraPortManager.instance.init(sdk)
        }

        /***
         * 释放资源
         */
        fun release() {
            cameraPortCore = null
            isInit = false
            CameraPortManager.instance.closeAllSerialPort()
        }


        @Synchronized
        fun test() {
            Loge.d( "发 CameraSdk")
            cameraPortCore?.test()
        }
    }
}
