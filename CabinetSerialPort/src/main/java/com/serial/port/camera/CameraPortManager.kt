package com.serial.port.camera

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.serial.port.ConfigurationSdk
import com.serial.port.SerialPort
import com.serial.port.utils.AppUtils
import com.serial.port.utils.Loge
import com.serial.port.utils.ShellUtils
import com.serial.port.vm.CamerasVM
import java.io.File


class CameraPortManager private constructor() : SerialPort() {
    private var mSdk232: ConfigurationSdk? = null

    //标记是否初始化
    private var isInit = false
    var camerasVM: CamerasVM? = null

    private object SerialPortInstance {
        @SuppressLint("StaticFieldLeak")
        val SERIALPORT = CameraPortManager()
    }

    companion object {
        val instance: CameraPortManager
            get() = SerialPortInstance.SERIALPORT
    }

    fun init(sdk232: ConfigurationSdk?) {
        if (isInit) return
        this.mSdk232 = sdk232
        camerasVM = CamerasVM()
        openSerialPort232()
        camerasVM?.initCollect()
        isInit = true
    }

    fun getUsbSerialDevices(): List<UsbDevice> {
        val dev = File("/dev")
        val files = dev.listFiles()
        files.forEach { index ->
            if(index.name.startsWith("tty")){
                Loge.d("授权成功 ${index.name},${index.canRead()},${index.canWrite()},${index.path}")
            }
        }


        val usbManager = AppUtils.getContext().getSystemService(Context.USB_SERVICE) as UsbManager
        val deviceList = usbManager.deviceList
        // 注册接收器
        val permissionIntent = PendingIntent.getBroadcast( AppUtils.getContext(), 0, Intent("com.recycling.toolsapp.USB_PERMISSION"), PendingIntent.FLAG_IMMUTABLE)
        AppUtils.getContext().registerReceiver(usbReceiver, IntentFilter("com.recycling.toolsapp.USB_PERMISSION"))

        return deviceList.values.filter {device->
            if (usbManager.hasPermission(device)) {
                Loge.d("授权成功 所有串口 ${device.vendorId}-${device.productId}-${device.productName}-${device.serialNumber}")
            } else {
                // 请求权限
                usbManager.requestPermission(device, permissionIntent)
            }
            isUsbSerialDevice(device)

        }
    }
    val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
            if (intent.action == "com.recycling.toolsapp.USB_PERMISSION") {
                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    // 权限已授予
                    device?.let {
                        Loge.d("授权成功 设备信息：${it.vendorId}-${it.productId}-${it.productName}-${it.serialNumber}")
                    }
                } else {
                    Loge.d("授权成功 用户拒绝了 USB 设备权限")
                }
            }
        }
    }


    private fun isUsbSerialDevice(device: UsbDevice): Boolean {
        // 根据你的需要定义如何判断是否为串口设备
        // 例如，根据 VID 和 PID 判断
        val vid = device.vendorId
        val pid = device.productId
        // 假设你知道某些 VID 和 PID 是串口设备
        return (vid == 0x1234 && pid == 0x5678) || (vid == 0x4321 && pid == 0x8765)
    }


    /**
     * 打开串口232
     *
     */
    @SuppressLint("ServiceCast")
    private fun openSerialPort232() {
        getUsbSerialDevices()
        close232SerialPort()
        mSdk232?.device?.let { device ->
            if (!device.canRead() || !device.canWrite()) {
                val chmod777: Boolean = chmod777(device)
                if (!chmod777) {
                    Loge.d("授权成功 打开串口232失败 没有权限")
                    return@let
                }
            }
            if (!device.canRead() || !device.canWrite()) {
                try {
                    val command: MutableList<String> = ArrayList()
                    command.add("chmod 777 " + device.absolutePath)
                    ShellUtils.execCommand(command, true)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Loge.d("授权成功 打开串口232失败 catch异常")
                    throw SecurityException()
                }
            }
            try {
                val fileDescriptor = open(device.absolutePath, mSdk232!!.baudRate, 0)
                camerasVM?._232fd?.value = fileDescriptor
            } catch (e: Exception) {
                e.printStackTrace()
                Loge.d("授权成功 打开串口232失败 catch异常")
            }
        }
    }


    fun closeAllSerialPort() {
        isInit = false
        close232SerialPort()
    }

    /**
     * 关闭232串口
     */
    fun close232SerialPort() {
        isInit = false
        camerasVM?._232fd?.let {
//            close(1)
        }
        camerasVM?.close232SerialPort()
    }


    fun test(sendBytes: ByteArray) {
        Loge.d( "发 CameraPortManager")
        camerasVM?.test(sendBytes)
    }

}