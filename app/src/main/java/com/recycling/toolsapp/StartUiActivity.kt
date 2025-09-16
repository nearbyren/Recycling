package com.recycling.toolsapp

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.recycling.toolsapp.socket.SocketClient.ConnectionState
import com.recycling.toolsapp.utils.SocketManager
import com.recycling.toolsapp.vm.CabinetVM
import com.serial.port.utils.AppUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nearby.lib.netwrok.response.SPreUtil

class StartUiActivity : AppCompatActivity() {
    private val cabinetVM: CabinetVM by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_start_ui)
        //这里http获取业务ID
        CoroutineScope(Dispatchers.Main).launch {
            val init = SPreUtil[AppUtils.getContext(), "init", false] as Boolean
            if (init) {
                println("调试socket startUI 进入主界面")
                initSocket()
//                startActivity(Intent(this@StartUiActivity, HomeActivity::class.java))
            } else {
                println("调试socket startUI 进入初始化")
                startActivity(Intent(this@StartUiActivity, InitFactoryActivity::class.java))
            }
        }
    }

    private fun initSocket() {
        cabinetVM.ioScope.launch {
            SocketManager.initializeSocketClient(host = "58.251.251.79", port = 9095)
            cabinetVM.vmClient = SocketManager.socketClient
            SocketManager.socketClient.start()
            delay(500)
            val state = cabinetVM.vmClient?.state?.value ?: ConnectionState.DISCONNECTED
            println("调试socket startUI 当前线程：${Thread.currentThread().name} | state $state")
            when (state) {
                ConnectionState.START -> {

                }

                ConnectionState.DISCONNECTED -> {

                }

                ConnectionState.CONNECTING -> {

                }

                ConnectionState.CONNECTED -> {
                    startActivity(Intent(this@StartUiActivity, HomeActivity::class.java))
                }
            }
        }
    }
}