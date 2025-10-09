package com.recycling.toolsapp.socket


import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.recycling.toolsapp.db.DatabaseManager
import com.recycling.toolsapp.utils.CmdValue
import com.recycling.toolsapp.utils.DynamicJsonBuilder
import com.recycling.toolsapp.utils.DynamicJsonBuilder.JsonArrayBuilder
import com.recycling.toolsapp.utils.JsonBuilder
import com.serial.port.utils.AppUtils
import com.serial.port.utils.BoxToolLogUtils
import com.serial.port.utils.ByteUtils
import com.serial.port.utils.Loge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.TimeUnit
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random

/**
 * Coroutine-based TCP socket client with auto-reconnect, heartbeat, and backpressure-aware send queue.
 */
class SocketClient(
    val config: Config,
) {
    data class Config(
        val host: String,
        val port: Int,
        val connectTimeoutMillis: Long = TimeUnit.SECONDS.toMillis(10),
        val readTimeoutMillis: Int = TimeUnit.SECONDS.toMillis(30).toInt(),
        val writeFlushIntervalMillis: Long = 0L,
        var heartbeatIntervalMillis: Long = TimeUnit.SECONDS.toMillis(10),
        val heartbeatPayload: ByteArray = byteArrayOf(),
        val idleTimeoutMillis: Long = TimeUnit.MINUTES.toMillis(2),
        val minReconnectDelayMillis: Long = 500,
        val maxReconnectDelayMillis: Long = TimeUnit.SECONDS.toMillis(30),
        val reconnectBackoffMultiplier: Double = 2.0,
        val maxSendQueueBytes: Int = 1_048_576,
        val maxFrameSizeBytes: Int = 4 * 1024 * 1024,
    )

    /***
     *  START  启动
     *  DISCONNECTED  已断开连接
     *  CONNECTING  正在连接
     *  CONNECTED  已连接
     */
    enum class ConnectionState { START, DISCONNECTED, CONNECTING, CONNECTED }

    private val clientScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(ConnectionState.DISCONNECTED)
    val state: StateFlow<ConnectionState> = _state.asStateFlow()

    private val _incoming = MutableSharedFlow<ByteArray>(replay = 0, extraBufferCapacity = 64)
    val incoming: SharedFlow<ByteArray> = _incoming.asSharedFlow()

    private val sendQueueByte = Channel<ByteArray>(capacity = Channel.BUFFERED)

    @Volatile
    private var socket: Socket? = null
    private val socketMutex = Mutex()

    @Volatile
    private var lastReceivedAtMillis: Long = System.currentTimeMillis()

    @Volatile
    private var running = false

    /***
     * 启动socket连接
     */
    suspend fun start() {
        if (running) return
        running = true
        _state.value = ConnectionState.START
        Loge.e("调试socket start ")
        clientScope.launch { runMainLoop() }
    }

    /***
     * 关闭socket连接
     */
    suspend fun stop() {
        Loge.e("调试socket stop ")
        running = false
        try {
            clientScope.coroutineContext.job.cancelAndJoin()
        } catch (e: CancellationException) {
            Loge.e("调试socket stop ${e.message}")
        }
        closeSocketQuietly()
    }

    /***
     * @param text
     * 发送字节
     */
    suspend fun send(data: ByteArray) {
//        Loge.e("调试socket send ByteArray  ${ByteUtils.toHexString(data)}")
        require(data.size <= config.maxFrameSizeBytes) { "Frame too large: ${data.size}" }
        // Backpressure control by counting queued bytes
        enqueueSend(data)
    }

    /***
     * @param text
     * 发送字符串
     */
    suspend fun sendText(text: String) {
//        Loge.e("调试socket sendText  $text")
        send(text.toByteArray())
    }

    /***
     * 调查send
     * @param data
     *
     */
    private suspend fun enqueueSend(data: ByteArray) {
//        Loge.e("调试socket enqueueSend  ${ByteUtils.toHexString(data)}")
        // Simple soft limit enforcement by suspending when over budget
        val queuedBytes = data.size
        if (queuedBytes > config.maxSendQueueBytes) {
            throw IOException("Send queue bytes over limit")
        }
        sendQueueByte.send(data)
    }

    /***
     * 运行主循环
     */
    private suspend fun runMainLoop() {
        var attempt = 0
        Loge.e("调试socket runMainLoop")
        while (running && clientScope.isActive) {
            try {
                _state.value = ConnectionState.CONNECTING
                Loge.e("调试socket runMainLoop 连接中")
                connectAndServe()
                attempt = 0 // reset backoff after successful session
            } catch (e: CancellationException) {
                Loge.e("调试socket runMainLoop catch1 ${e.message} running $running")
                break
            } catch (e: Exception) {
                // Swallow and backoff
                Loge.e("调试socket runMainLoop catch2 ${e.message} running $running")
            } finally {
                Loge.e("调试socket runMainLoop finally running $running")
                closeSocketQuietly()
                if (!running) break
                _state.value = ConnectionState.DISCONNECTED
            }

            attempt += 1
            val delayMs = computeReconnectDelay(attempt)
            Loge.e("调试socket runMainLoop 重连接延迟 $delayMs")
            delay(delayMs)
        }
    }

    /***
     * 计算重新连接延迟
     * @param attempt
     */
    private fun computeReconnectDelay(attempt: Int): Long {
        Loge.e("调试socket computeReconnectDelay attempt $attempt")
        val base =
                config.minReconnectDelayMillis * config.reconnectBackoffMultiplier.pow((attempt - 1).toDouble())
        val clamped = min(base, config.maxReconnectDelayMillis.toDouble()).toLong()
        val jitter = (clamped * 0.2 * Random.nextDouble()).toLong()
        return clamped + jitter
    }

    /***
     * 连接和服务
     */
    private suspend fun connectAndServe() {
        Loge.e("调试socket connectAndServe")
        val s = Socket()
        s.tcpNoDelay = true
        s.soTimeout = config.readTimeoutMillis
        s.connect(InetSocketAddress(config.host, config.port), config.connectTimeoutMillis.toInt())

        socketMutex.withLock { socket = s }

        lastReceivedAtMillis = System.currentTimeMillis()

        val input = BufferedInputStream(s.getInputStream())
        val output = BufferedOutputStream(s.getOutputStream())

        val reader = clientScope.launch { readLoop(input) }
        val writer = clientScope.launch { writeLoopByte(output) }
//        val monitor = clientScope.launch { heartbeatAndIdleMonitor() }
        Loge.e("调试socket connectAndServe 已连接")
        _state.value = ConnectionState.CONNECTED

        try {
            reader.join()
        } finally {
//            writer.cancel()
//            monitor.cancel()
//
        }
    }

    /***
     * 启动心跳查询
     */
    suspend fun sendHeartbeat() {
        val monitor = clientScope.launch { heartbeatAndIdleMonitor() }
//        monitor.cancel()
    }

    /***
     * 读取socket数据
     * @param input
     * 缓冲输入流
     */
    private suspend fun readLoop(input: BufferedInputStream) {
        Loge.e("调试socket readLoop ")
        val buffer = ByteArray(8 * 1024)
        while (running && clientScope.isActive) {
            try {
                val read = input.read(buffer)
                if (read == -1) {
                    BoxToolLogUtils.recordSocket(CmdValue.RECEIVE, "Stream closed")
                    throw IOException("Stream closed")
                }
                lastReceivedAtMillis = System.currentTimeMillis()
                val frame = buffer.copyOf(read)
//                Loge.e("调试socket readLoop ${ByteUtils.toHexString(frame)}")
                _incoming.emit(frame)
            } catch (e: IOException) {
                Loge.e("调试socket readLoop catch ${e.message}")
                break
            }
        }
    }

    /***
     * 读取socket数据
     * @param output
     * 缓冲输出流
     */
    private suspend fun writeLoopByte(output: BufferedOutputStream) {
        Loge.e("调试socket writeLoop running $running | isActive ${clientScope.isActive}")
        while (running && clientScope.isActive) {
            try {
                val data = sendQueueByte.receive()
//                Loge.e("调试socket writeLoopByte byte：${ByteUtils.toHexString(data)}")
                BoxToolLogUtils.recordSocket(CmdValue.SEND, JsonBuilder.toByteArrayToString(data))
                output.write(data)
                if (config.writeFlushIntervalMillis == 0L) {
                    output.flush()
                } else {
                    // Optional coalescing
                    delay(config.writeFlushIntervalMillis)
                    output.flush()
                }
            } catch (e: CancellationException) {
                Loge.e("调试socket writeLoop catch1 ${e.message}")
                break
            } catch (e: IOException) {
                Loge.e("调试socket writeLoop catch2 ${e.message}")
                break
            }
        }
    }

    /***
     *
     * 心跳启动
     */
    private suspend fun heartbeatAndIdleMonitor() {
//       Loge.e("调试socket heartbeatAndIdleMonitor $running | ${clientScope.isActive}")
        val hasHeartbeat =
                config.heartbeatIntervalMillis > 0 /*&& config.heartbeatPayload.isNotEmpty()*/
        while (running && clientScope.isActive) {
            val now = System.currentTimeMillis()
//            Loge.e("调试socket heartbeatAndIdleMonitor 分钟：${config.idleTimeoutMillis} | 当前毫秒：$lastReceivedAtMillis | 当前-最后：${now - lastReceivedAtMillis}")
            if (config.idleTimeoutMillis > 0 && now - lastReceivedAtMillis > config.idleTimeoutMillis) {
                // Force reconnect by closing the socket
                Loge.e("调试socket heartbeatAndIdleMonitor closeSocketQuietly")
                closeSocketQuietly()
                return
            }
//            Loge.e("调试socket heartbeatAndIdleMonitor $hasHeartbeat")
            if (hasHeartbeat) {
                try {
//                    Loge.e("调试socket heartbeatAndIdleMonitor trySend")
                    val stateList = DatabaseManager.queryStateList(AppUtils.getContext())
//                    Loge.e("调试socket stateList：${stateList.size}")
                    // 构建JSON对象
                    val jsonObject = JsonBuilder.build {
                        addProperty("cmd", "heartBeat")
                        addProperty("signal", 13)
                        // 添加数组
                        addArray("stateList") {
                            for (state in stateList) {
                                addObject {
                                    addProperty("smoke", state.smoke)
                                    addProperty("capacity", state.capacity)
                                    addProperty("irState", state.irState)
                                    addProperty("weigh", state.weigh)
                                    addProperty("doorStatus", state.doorStatus)
                                    addProperty("lockStatus", state.lockStatus)
                                    addProperty("cabinId", state.cabinId ?: "")
                                }
                            }
                        }
                    }
//                    Loge.e("调试socket 发送心跳数据：$jsonObject")
                    val byteArray = JsonBuilder.toByteArray(jsonObject)
                    sendQueueByte.trySend(byteArray)
                } catch (e: Exception) {
                    Loge.e("调试socket heartbeatAndIdleMonitor catch ${e.message}")
                }
            }
            delay(maxOf(1000L, config.heartbeatIntervalMillis))
        }
    }

    /***
     * 关闭socket
     */
    private fun closeSocketQuietly() {
        Loge.e("调试socket closeSocketQuietly $running | ${clientScope.isActive}")
        try {
            socketMutex.tryLock()?.let { locked ->
                if (locked) {
                    try {
                        socket?.close()
                    } catch (e: Exception) {
                        Loge.e("调试socket closeSocketQuietly catch1 ${e.message}")
                    } finally {
                        socket = null
                        socketMutex.unlock()
                        Loge.e("调试socket closeSocketQuietly finally")
                    }
                }
            }
        } catch (e: Exception) {
            Loge.e("调试socket closeSocketQuietly catch2 ${e.message}")
        }
    }
}


