package com.serial.port.vm

import androidx.lifecycle.ViewModel
import com.serial.port.utils.ByteUtils
import com.serial.port.utils.Loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.FileDescriptor
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class CamerasVM : ViewModel() {

    //串口232描述文件
    val _232fd = MutableStateFlow<FileDescriptor?>(null)
    private val fileDes232: StateFlow<FileDescriptor?> = _232fd

    //接收
    private val _232fis = MutableStateFlow<FileInputStream?>(null)
    private val fis232Read: StateFlow<FileInputStream?> = _232fis

    //发送
    private val _232fos = MutableStateFlow<FileOutputStream?>(null)
    private val fos232Send: StateFlow<FileOutputStream?> = _232fos

    /**
     * 用于处理 I/O 操作的协程作用域
     */
    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    /**
     * 用于处理 main 操作的协程作用域
     */
    private val mainScope = MainScope()

    /***
     * 用于处理 默认协程作用域
     */
    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)


    /****
     * 初始化串口成功回调
     */
    fun initCollect() {

        mainScope.launch {
            fileDes232.collect { file ->
                //获取串口文件描述符成功
                file?.let {
                    Loge.d("接 获取串口文件描述完成开启构建读写流232注册")
                    _232fis.value = FileInputStream(file)
                    _232fos.value = FileOutputStream(file)
                } ?: run {
                    Loge.d("接 获取串口文件描述完成开启构建读写流232注销")
                }
            }
        }

        mainScope.launch {
            fis232Read.collect { c ->
                c?.apply {
                    Loge.d("接 构建读取流232注册")
                    startRead232Job()
                } ?: run {
                    stopRead232Job()
                    Loge.d("接 构建读取流流232注销")
                }
            }
        }

        mainScope.launch {
            fos232Send.collect { c ->
                c?.apply {
                    Loge.d("接 构建写入流232注册")
                } ?: run {
                    stopSend232Job()
                    Loge.d("接 构建写入流232注销")
                }
            }
        }
    }

    ///线程安全  用于存储不完整的数据包 方案一
//    private val bufferSBuffer = StringBuffer()
    /// 用于存储不完整的数据包 方案二
    private val byteListOf = mutableListOf<Byte>()

    /***
     *  启动接收消息协程任务
     */
    private fun startRead232Job() {
        // 启动协程，处理接收消息的任务
        read232Job = ioScope.launch {
            // 缓存区大小可以根据需要调整
            val bufferRead = ByteArray(1024)
            try {
                // 保持协程任务处于活动状态
                while (isActive) {
                    Loge.d("授权成功.. 粘包测试 接收到数据了 isActive $isActive ${fis232Read.value}")
                    fis232Read.value?.let {
                        val bytesRead = it.read(bufferRead)
                        Loge.d("授权成功.. 粘包测试 接收到数据了 bytesRead $bytesRead ")

//                        Loge.d("接 size $bytesRead ")
                        if (bytesRead > 0) {
                            val receivedData = bufferRead.copyOf(bytesRead)
                            ///---------------------方案一代码处理粘包-------------///


                            /***
                             *
                             *
                             * 末位63代表数据包结束位
                             * 正常包数据包如下：
                             * 9F 06 00 27 05 0B 00 01 63
                             * 0A 04 00 00 00 00 00 00 00 02 63
                             * 02 02 02 02 02 02 02 02 02 02 02 02 02 03 63
                             *
                             *
                             * 粘包数据如下：
                             * 9F 06 00 27 05 0B
                             * 01 63 0A 04 00 00 00 00 00 00
                             * 02 63 02 02 02 02 02 02 02 02 02 02 02 02 02 03 63
                             */

//                            val s = ByteUtils.toHexString(receivedData)
//                            Loge.d("授权成功.. 粘包测试 接收数据：$s")
//                            Loge.d("授权成功.. 粘包测试 当前数据：$bufferSuilder")
//                            if(bufferSuilder.isNotEmpty()){
//                                bufferSuilder.append(" ")
//                                bufferSuilder.append(s)
//                            }else{
//                                bufferSuilder.append(s)
//                            }
//                            if(!bufferSuilder.contains("63")){
//                                Loge.d("授权成功.. 粘包测试 数据不完整，等待更多数据")
//                                return@let
//                            }
//                            while (bufferSuilder.contains("63")) {
//                                val endIndex = bufferSuilder.indexOf("63")
//                                val packet = bufferSuilder.substring(0, endIndex)
//                                bufferSuilder.delete(0, endIndex + 2)  // 移除已处理的部分
//                                Loge.d("授权成功.. 粘包测试 需要显示的数据：长度：${packet.length} 数据：${packet}")
//                                handlePacket(packet.hexToByteArray()) // 处理数据包
//
//                            }
//

                            ///---------------------方案一代码处理粘包-------------///


                            ///---------------------方案二代码处理粘包-------------///


                            /***
                             *
                             * 前两位代表数据包长度
                             * 正常包数据包如下：
                             * 00 06 9F 06 00 27 05 0B 00 01
                             * 00 0A 0A 04 00 00 00 00 00 00 00 02
                             * 00 0E 02 02 02 02 02 02 02 02 02 02 02 02 02 03
                             *
                             * 粘包数据如下：
                             * 00 06 9F 06 00 27 05 0B
                             * 00 01 00 0A 0A 04 00 00 00 00 00 00
                             * 00 02 00 0E 02 02 02 02 02 02 02 02 02 02 02 02 02 03
                             */
                            /***
                             * 如果 buffer[0] 是 0x09，buffer[1] 是 0x0A，
                             * 那么 (buffer[0].toInt() and 0xFF) 的结果是 9，
                             * shl 8 将 9 左移 8 位，得到 0x0900，
                             * or (buffer[1].toInt() and 0xFF) 得到 0x0A，最终结果是 0x090A，即十进制的 2314。
                             */

                            Loge.d("授权成功.. 粘包测试 接收数据：${ByteUtils.toHexString(receivedData)}")
                            Loge.d("授权成功.. 粘包测试 当前数据：${ByteUtils.toHexString(byteListOf.toByteArray())}")
                            byteListOf.addAll(receivedData.toList()) // 将接收到的数据添加到缓冲区
                            while (byteListOf.size >= 2) {
                                val expectedPacketLength = (byteListOf[0].toInt() and 0xFF) shl 8 or (byteListOf[1].toInt() and 0xFF)
                                Loge.d("授权成功.. 粘包测试 size = ${byteListOf.size} , expectedPacketLength：$expectedPacketLength")
                                // 判断缓冲区中的数据是否足够一个完整的数据包
                                if (byteListOf.size >= expectedPacketLength + 2) {
                                    val packet = byteListOf.subList(2, expectedPacketLength + 2)
                                        .toByteArray()
                                    handlePacket(packet) // 处理数据包
                                    byteListOf.subList(0, expectedPacketLength + 2)
                                        .clear() // 移除已经处理的数据
                                    Loge.d("授权成功.. 粘包测试 处理完毕包数据：$byteListOf")
                                } else {
                                    Loge.d("授权成功.. 粘包测试 数据不完整，等待更多数据")
                                    break // 数据不完整，等待更多数据
                                }
                            }


                            ///---------------------方案二代码处理粘包-------------///

//                            Loge.d("接 接收到串口数据：长度：${bufferQueue.size} 数据：${ByteUtils.toHexString(bufferQueue.toByteArray())}")
                            /* if (isComplete(bufferQueue)) {
                                 Loge.d("接 接收到串口数据：长度：${bufferQueue.size} 处理数据")
                                 // 处理完整数据
                                 val completeData = bufferQueue.toByteArray()
                                 // 在协程内接收数据并通过回调传递 根据数据拆分指令
                                 val list = parseReceivedCmd(completeData)
                                 for (byteOne in list) {
                                     parseCmd(byteOne)
                                 }
                                 bufferQueue.clear() // 清空缓存
                             }*/
                            // withContext(Dispatchers.Main) {}
                        }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /***
     * 启动发送消息协程任务
     */
    private fun startSend232Job(sendBytes: ByteArray) {
        // 启动协程，处理发送消息的任务
        sending232Job = ioScope.launch {
            // 保持协程任务处于活动状态
            if (isActive) {
                fos232Send.value?.apply {
                    write(sendBytes)
                    Loge.d("发 startSend232Job ${ByteUtils.toHexString(sendBytes)}")
                } ?: run {
                    Loge.d("发 startSend232Job 失败")
                }
            } else {
                Loge.d("发 startSend232Job 失败")
            }
        }
    }

    fun handlePacket(packet: ByteArray) {
        Loge.d("授权成功.. 粘包测试 处理数据包: ${ByteUtils.toHexString(packet)}")
        // 在这里处理你的业务逻辑

    }

    /**
     * 开启发送消息的协程
     */
    private var sending232Job: Job? = null

    /**
     * 开启接收消息的协程
     */
    private var read232Job: Job? = null


    /**
     * 停止发送消息的协程任务
     */
    private fun stopSend232Job() {
        // 取消发送任务
        sending232Job?.cancel()
        sending232Job = null
    }

    /**
     * 停止接收消息的协程任务
     */
    private fun stopRead232Job() {
        // 取消接收任务
        read232Job?.cancel()
        read232Job = null
    }
    /***
     * 释放232资源
     */
    fun close232SerialPort() {

        _232fd.value = null
        _232fis.value?.close()
        _232fis.value = null
        _232fos.value?.close()
        _232fos.value = null

        stopSend232Job()
        stopRead232Job()

    }
    /***
     *
     */
    fun test(sendBytes: ByteArray) {
        mainScope.launch {
            startSend232Job(sendBytes)
        }
    }
}