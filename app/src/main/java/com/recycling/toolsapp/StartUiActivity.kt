package com.recycling.toolsapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.gson.Gson
import com.recycling.toolsapp.socket.InitConfigDto
import com.recycling.toolsapp.socket.LoginDto
import com.recycling.toolsapp.socket.SocketClient
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.CommandParser
import com.recycling.toolsapp.utils.SocketManager
import com.recycling.toolsapp.vm.CabinetVM
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class StartUiActivity : AppCompatActivity() {
    private val cabinetVM: CabinetVM by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_ui)
        //这里http获取业务ID
        CoroutineScope(Dispatchers.Main).launch {
            delay(50)
            startActivity(Intent(this@StartUiActivity, OneActivity::class.java))

//            initSocket()
        }
    }

    private fun initSocket() {
        cabinetVM.ioScope.launch {
//            SocketManager.initializeSocketClient(host = "58.251.251.79", port = 9095)
//            SocketManager.socketClient.start()

            val vmClient =
                    SocketClient(SocketClient.Config(host = "58.251.251.79", port = 9095, heartbeatIntervalMillis = 10_000, heartbeatPayload = "PING".toByteArray()))
            cabinetVM.vmClient = vmClient
            vmClient.start()
            delay(500)
            val state = vmClient.state.value
            println("调试socket startUI 当前线程：${Thread.currentThread().name} | state $state")
            when (state) {
                ConnectionState.START -> {

                }

                ConnectionState.DISCONNECTED -> {

                }

                ConnectionState.CONNECTING -> {

                }

                ConnectionState.CONNECTED -> {
                    startActivity(Intent(this@StartUiActivity, OneActivity::class.java))
                }
            }
        }
    }
}