package com.recycling.toolsapp.socket


import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.recycling.toolsapp.db.DatabaseManager
import com.recycling.toolsapp.utils.DynamicJsonBuilder
import com.recycling.toolsapp.utils.DynamicJsonBuilder.JsonArrayBuilder
import com.serial.port.utils.AppUtils
import com.serial.port.utils.ByteUtils
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
        var heartbeatIntervalMillis: Long = TimeUnit.SECONDS.toMillis(20),
        var heartbeatIntervalMillis1: Long = TimeUnit.SECONDS.toMillis(3),
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


    suspend fun start() {
        if (running) return
        running = true
        _state.value = ConnectionState.START
        println("调试socket start ")
        clientScope.launch { runMainLoop() }
    }

    suspend fun stop() {
        println("调试socket stop ")
        running = false
        try {
            clientScope.coroutineContext.job.cancelAndJoin()
        } catch (e: CancellationException) {
            println("调试socket stop ${e.message}")
        }
        closeSocketQuietly()
    }

    suspend fun send(data: ByteArray) {
        println("调试socket send ByteArray  ${ByteUtils.toHexString(data)}")
        require(data.size <= config.maxFrameSizeBytes) { "Frame too large: ${data.size}" }
        // Backpressure control by counting queued bytes
        enqueueSend(data)
    }

    suspend fun sendText(text: String) {
        println("调试socket sendText  $text")
        send(text.toByteArray())
    }

    private suspend fun enqueueSend(data: ByteArray) {
        println("调试socket enqueueSend  ${ByteUtils.toHexString(data)}")
        // Simple soft limit enforcement by suspending when over budget
        var queuedBytes = data.size
        if (queuedBytes > config.maxSendQueueBytes) {
            throw IOException("Send queue bytes over limit")
        }
        sendQueueByte.send(data)
    }

    private suspend fun runMainLoop() {
        var attempt = 0
        println("调试socket runMainLoop")
        while (running && clientScope.isActive) {
            try {
                _state.value = ConnectionState.CONNECTING
                println("调试socket runMainLoop 连接中")
                connectAndServe()
                attempt = 0 // reset backoff after successful session
            } catch (e: CancellationException) {
                println("调试socket runMainLoop catch1 ${e.message} running $running")
                break
            } catch (e: Exception) {
                // Swallow and backoff
                println("调试socket runMainLoop catch2 ${e.message} running $running")
            } finally {
                println("调试socket runMainLoop finally running $running")
                closeSocketQuietly()
                if (!running) break
                _state.value = ConnectionState.DISCONNECTED
            }

            attempt += 1
            val delayMs = computeReconnectDelay(attempt)
            println("调试socket runMainLoop 重连接延迟 $delayMs")
            delay(delayMs)
        }
    }

    private fun computeReconnectDelay(attempt: Int): Long {
        println("调试socket computeReconnectDelay attempt $attempt")
        val base =
                config.minReconnectDelayMillis * config.reconnectBackoffMultiplier.pow((attempt - 1).toDouble())
        val clamped = min(base, config.maxReconnectDelayMillis.toDouble()).toLong()
        val jitter = (clamped * 0.2 * Random.nextDouble()).toLong()
        return clamped + jitter
    }

    private suspend fun connectAndServe() {
        println("调试socket connectAndServe")
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
        println("调试socket connectAndServe 已连接")
        _state.value = ConnectionState.CONNECTED

        try {
            reader.join()
        } finally {
//            writer.cancel()
//            monitor.cancel()
//
        }
    }

    suspend fun sendHeartbeat() {
        val monitor = clientScope.launch { heartbeatAndIdleMonitor() }
//        monitor.cancel()
    }

    private suspend fun readLoop(input: BufferedInputStream) {
        println("调试socket readLoop ")
        val buffer = ByteArray(8 * 1024)
        while (running && clientScope.isActive) {
            try {
                val read = input.read(buffer)
                if (read == -1) throw IOException("Stream closed")
                lastReceivedAtMillis = System.currentTimeMillis()
                val frame = buffer.copyOf(read)
                _incoming.emit(frame)
            } catch (e: IOException) {
                break
            }
        }
    }

    private suspend fun writeLoopByte(output: BufferedOutputStream) {
        println("调试socket writeLoop ")
        while (running && clientScope.isActive) {
            try {
                val data = sendQueueByte.receive()
                println("调试socket writeLoopByte byte：${ByteUtils.toHexString(data)}")
                output.write(data)
                if (config.writeFlushIntervalMillis == 0L) {
                    output.flush()
                } else {
                    // Optional coalescing
                    delay(config.writeFlushIntervalMillis)
                    output.flush()
                }
            } catch (e: CancellationException) {
                break
            } catch (e: IOException) {
                break
            }
        }
    }

    private suspend fun heartbeatAndIdleMonitor() {
        println("调试socket heartbeatAndIdleMonitor $running | ${clientScope.isActive}")
        val hasHeartbeat =
                config.heartbeatIntervalMillis1 > 0 /*&& config.heartbeatPayload.isNotEmpty()*/
        while (running && clientScope.isActive) {
            val now = System.currentTimeMillis()
            println("调试socket heartbeatAndIdleMonitor 分钟：${config.idleTimeoutMillis} | 当前毫秒：$lastReceivedAtMillis | 当前-最后：${now - lastReceivedAtMillis}")
            if (config.idleTimeoutMillis > 0 && now - lastReceivedAtMillis > config.idleTimeoutMillis) {
                // Force reconnect by closing the socket
                println("调试socket heartbeatAndIdleMonitor closeSocketQuietly")
                closeSocketQuietly()
                return
            }
            println("调试socket heartbeatAndIdleMonitor $hasHeartbeat")
            if (hasHeartbeat) {
                try {
                    println("调试socket heartbeatAndIdleMonitor trySend")
                    val stateList = DatabaseManager.queryStateList(AppUtils.getContext())
                    println("回收柜 stateList：${stateList.size}")
                    val root = JsonObject()

                    val gson = GsonBuilder().setPrettyPrinting().create()
                    val array = JsonArray()
                    for (stetes in stateList) {
                        val obj = JsonObject()
                        obj.addProperty("smoke", stetes.smoke)
                        obj.addProperty("capacity", stetes.capacity)
                        obj.addProperty("irState", stetes.irState)
                        obj.addProperty("weigh", stetes.weigh)
                        obj.addProperty("doorStatus", stetes.doorStatus)
                        obj.addProperty("cabinId", stetes.cabinId)
                        array.add(obj)
                    }
                    root.addProperty("cmd", "heartBeat")
                    root.addProperty("signal", 13)
                    root.add("stateList", array)
                    println("回收柜 $root")
                    val newJson = gson.toJson(root).toByteArray(Charsets.UTF_8)

                    val json =
                            DynamicJsonBuilder().addPrimitive("cmd", "heartBeat").addPrimitive("signal", 13)
//                        .addObject("gps") {  }
                                .addArray("stateList") {
                                    addObject {
                                        addPrimitive("smoke", 0)
                                        addPrimitive("capacity", 0)
                                        addPrimitive("irState", 0)
                                        addPrimitive("weigh", 0.0)
                                        addPrimitive("doorStatus", 0)
//                                        addPrimitive("cabinId", "20250118161240405726")
                                    }
                                }.addPrimitive("timestamp", System.currentTimeMillis()).build().toByteArray(Charsets.UTF_8)
                    sendQueueByte.trySend(json)
//                    sendQueueByte.trySend(config.heartbeatPayload)

                } catch (e: Exception) {
                    println("调试socket heartbeatAndIdleMonitor catch ${e.message}")
                }
            }
            delay(maxOf(1000L, config.heartbeatIntervalMillis1))
        }
    }

    private fun closeSocketQuietly() {
        println("调试socket closeSocketQuietly $running | ${clientScope.isActive}")
        try {
            socketMutex.tryLock()?.let { locked ->
                if (locked) {
                    try {
                        socket?.close()
                    } catch (e: Exception) {
                        println("调试socket closeSocketQuietly catch1 ${e.message}")
                    } finally {
                        socket = null
                        socketMutex.unlock()
                        println("调试socket closeSocketQuietly finally")
                    }
                }
            }
        } catch (e: Exception) {
            println("调试socket closeSocketQuietly catch2 ${e.message}")
        }
    }
}


