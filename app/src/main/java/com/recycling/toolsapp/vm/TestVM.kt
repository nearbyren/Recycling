package com.recycling.toolsapp.vm

import androidx.lifecycle.ViewModel
import com.recycling.toolsapp.utils.CmdType
import com.serial.port.utils.CmdCode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import javax.inject.Inject

/**
 * @author: lr
 * @created on: 2025/9/23 下午9:17
 * @description:
 */

@HiltViewModel
class TestVM @Inject constructor() : ViewModel() {

  // 定义指令优先级
  enum class CommandPriority {
    HIGH,    // 紧急指令：开关门、状态查询
    MEDIUM,  // 重要指令：重量查询
    LOW      // 普通指令：清运、状态查询
  }

  // 指令数据类
  private data class DoorCommand(
    val type: Int,
    val priority: CommandPriority,
    val timestamp: Long = System.currentTimeMillis()
  )

  var doorJob: Job? = null
  // 使用优先级队列
  private val doorQueue = Channel<DoorCommand>(capacity = 20)
  private val pendingCommands = mutableSetOf<Int>() // 跟踪已排队但未处理的指令类型

  // 指令处理器映射
  private val commandHandlers: Map<Int, suspend () -> Unit> = mapOf(
    CmdType.CMD1 to ::handleDoorOperation,
    CmdType.CMD2 to ::handleDoorStatusQuery,
    CmdType.CMD3 to ::handleClearOperation,
    CmdType.CMD4 to ::handleWeightQuery,
    CmdType.CMD5 to ::handleCabinetStatusQuery,
    CmdType.CMD6 to ::handleCustomCommand
  )

  // 指令优先级映射
  private val commandPriorities: Map<Int, CommandPriority> = mapOf(
    CmdType.CMD1 to CommandPriority.HIGH,    // 开关门
    CmdType.CMD2 to CommandPriority.HIGH,    // 状态查询
    CmdType.CMD3 to CommandPriority.LOW,     // 清运
    CmdType.CMD4 to CommandPriority.MEDIUM,  // 重量查询
    CmdType.CMD5 to CommandPriority.LOW,     // 机柜状态
    CmdType.CMD6 to CommandPriority.LOW      // 自定义
  )
  /**
   * 用于处理 I/O 操作的协程作用域
   */
  val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

  /**
   * 用于处理 main 操作的协程作用域
   */
  val mainScope = MainScope()

  /***
   * @param cmdDoorType 指令类型
   * @param priority 优先级（可选）
   * 1.启动格口开门
   * 3.查询投口门状态
   * 4.查询格口重量
   */
  fun addDoorQueue(cmdDoorType: Int, priority: CommandPriority? = null) {
    if (doorJob?.isActive != true) {
      println("调试socket 门控制任务未启动，忽略指令: $cmdDoorType")
      return
    }

    val commandPriority = priority ?: commandPriorities[cmdDoorType] ?: CommandPriority.LOW

    // 检查是否已有相同类型的指令在队列中
    if (pendingCommands.contains(cmdDoorType)) {
      println("调试socket 已有相同类型指令在队列中，忽略: $cmdDoorType")
      return
    }

    ioScope.launch {
      val command = DoorCommand(cmdDoorType, commandPriority)
      if (doorQueue.trySend(command).isSuccess) {
        pendingCommands.add(cmdDoorType)
        println("调试socket 指令已添加到队列: $cmdDoorType, 优先级: $commandPriority")
      } else {
        println("调试socket 队列已满，丢弃指令: $cmdDoorType")
      }
    }
  }

  /**
   * 发送格口开关门
   * 实时格口门状态
   * 查询格口重量
   */
  fun startPollingDoor() {
    println("调试socket 调试串口 启动检测门状态轮询")
    if (doorJob != null && doorJob!!.isActive) {
      println("调试socket 门控制任务已在运行")
      return
    }

    println("调试socket 调试串口 启动检测门状态轮询发起")
    doorJob = ioScope.launch {
      while (isActive) {
        try {
          val command = doorQueue.receive()  // 从Channel中接收指令
          pendingCommands.remove(command.type)

          println("调试socket 处理指令: ${command.type}, 优先级: ${command.priority}")

          // 执行指令处理
          commandHandlers[command.type]?.invoke()

          // 根据指令类型和状态决定延迟时间
          val delayTime = when (command.type) {
            CmdType.CMD1 -> 800L // 开关门操作后等待
            CmdType.CMD2 -> 300L // 状态查询后等待
            CmdType.CMD4 -> 400L // 重量查询后等待
            else -> 200L
          }

          delay(delayTime)

        } catch (e: Exception) {
          println("调试socket 处理指令异常: ${e.message}")
          delay(500)
        }
      }
    }
  }
  /***
   * 当前设备格口类型
   */
  var doorGeXType = CmdCode.GE

  /***
   * 标记当前格口
   */
  var doorGeX = CmdCode.GE
  /***
   * 打开/关闭投口
   */
  var flowCmd01 = MutableStateFlow(false)
  val getCmd01: MutableStateFlow<Boolean> = flowCmd01

  /***
   * 查询投口状态
   */
  val flowCmd02 = MutableStateFlow(false)
  val getCmd02: MutableStateFlow<Boolean> = flowCmd02

  /***
   * 打开清运门
   */
  val flowCmd03 = MutableStateFlow(false)
  val getCmd03: MutableStateFlow<Boolean> = flowCmd03

  /***
   * 查询当前重量
   */
  val flowCmd04 = MutableStateFlow(false)
  val getCmd04: MutableStateFlow<Boolean> = flowCmd04

  /***
   * 查询当前设备状态
   */
  val flowCmd05 = MutableStateFlow(false)
  val getCmd05: MutableStateFlow<Boolean> = flowCmd05

  /***
   * 灯光控制
   */
  val flowCmd06 = MutableStateFlow(false)
  val getCmd06: MutableStateFlow<Boolean> = flowCmd06
  /***
   * 启动格口开启类型
   * 0.关门 1.开门
   */
  var geStartDoorType = CmdCode.GE
  // 处理格口开关门操作
  private suspend fun handleDoorOperation() {
    println("调试socket handleDoorOperation 1 ")
    val code = when (doorGeX) {
      CmdCode.GE1 -> if (geStartDoorType == CmdCode.GE_OPEN) CmdCode.GE11 else CmdCode.GE10
      CmdCode.GE2 -> if (geStartDoorType == CmdCode.GE_OPEN) CmdCode.GE21 else CmdCode.GE20
      else -> -1
    }

    if (code == -1) return



    // 更新状态流
    flowCmd01.emit(false)
    flowCmd02.emit(true)
  }

  // 查询门状态
  private suspend fun handleDoorStatusQuery() {
    println("调试socket handleDoorStatusQuery 2 发送查询门状态 接收回调 |$geStartDoorType")
  }

  // 处理清运门操作
  private suspend fun handleClearOperation() {
    println("调试socket handleClearOperation 3")

  }

  // 查询重量
  private suspend fun handleWeightQuery() {
    println("调试socket handleWeightQuery 4")

  }

  // 查询机柜状态
  private suspend fun handleCabinetStatusQuery() {
    println("调试socket handleCabinetStatusQuery 5 ")
  }

  // 处理自定义命令
  private suspend fun handleCustomCommand() {
    println("调试socket handleCustomCommand 6")
    // 实现自定义命令逻辑
  }
  // 定义格口操作状态
  sealed class DoorOperationState {
    object Idle : DoorOperationState()
    object Opening : DoorOperationState()
    object Closing : DoorOperationState()
    object Querying : DoorOperationState()
    object Weighing : DoorOperationState()
    object Clearing : DoorOperationState()
    object Fault : DoorOperationState()
  }

  // 使用状态机管理格口操作
  private var doorOperationState: DoorOperationState = DoorOperationState.Idle
  private val doorMutex = Mutex() // 防止并发操作

  // 智能指令调度器
  private val commandScheduler = ioScope.launch {
    while (isActive) {
      try {
        val currentState = doorOperationState
        val commandsToAdd = mutableListOf<Pair<Int, CommandPriority>>()

        // 根据当前状态智能调度指令
        when (currentState) {
          DoorOperationState.Idle -> {
            if (getCmd01.value) commandsToAdd.add(CmdType.CMD1 to CommandPriority.HIGH)
            if (getCmd02.value) commandsToAdd.add(CmdType.CMD2 to CommandPriority.HIGH)
            if (getCmd03.value) commandsToAdd.add(CmdType.CMD3 to CommandPriority.LOW)
            if (getCmd04.value) commandsToAdd.add(CmdType.CMD4 to CommandPriority.MEDIUM)
            if (getCmd05.value) commandsToAdd.add(CmdType.CMD5 to CommandPriority.LOW)
            if (getCmd06.value) commandsToAdd.add(CmdType.CMD6 to CommandPriority.LOW)
          }

          DoorOperationState.Opening, DoorOperationState.Closing -> {
            // 开关门过程中优先查询状态
            commandsToAdd.add(CmdType.CMD2 to CommandPriority.HIGH)

            // 同时监控重量变化
            if (getCmd04.value) {
              commandsToAdd.add(CmdType.CMD4 to CommandPriority.MEDIUM)
            }
          }

          DoorOperationState.Weighing -> {
            // 称重过程中减少其他指令
            if (getCmd04.value) {
              commandsToAdd.add(CmdType.CMD4 to CommandPriority.HIGH)
            }
          }

          else -> {
            // 其他状态下谨慎添加指令
            if (getCmd02.value) {
              commandsToAdd.add(CmdType.CMD2 to CommandPriority.MEDIUM)
            }
          }
        }

        // 添加指令到队列（带优先级）
        commandsToAdd.forEach { (cmdType, priority) ->
          addDoorQueue(cmdType, priority)
        }

        delay(300)

      } catch (e: Exception) {
        println("调试socket 指令调度器异常: ${e.message}")
        delay(1000)
      }
    }
  }

  // 改进的状态管理
  private fun updateCommandFlagsBasedOnState() {
    when (doorOperationState) {
      DoorOperationState.Opening, DoorOperationState.Closing -> {
        // 开关门过程中只允许状态查询和必要的重量查询
        getCmd01.value = false
        getCmd02.value = true  // 允许状态查询
        getCmd03.value = false
        getCmd04.value = true  // 允许重量监控
        getCmd05.value = false
        getCmd06.value = false
      }

      DoorOperationState.Weighing -> {
        // 称重过程中专注于重量查询
        getCmd01.value = false
        getCmd02.value = false
        getCmd03.value = false
        getCmd04.value = true  // 允许重量查询
        getCmd05.value = false
        getCmd06.value = false
      }

      DoorOperationState.Fault -> {
        // 故障状态下停止所有操作，只允许状态查询
        getCmd01.value = false
        getCmd02.value = true  // 允许状态查询
        getCmd03.value = false
        getCmd04.value = false
        getCmd05.value = false
        getCmd06.value = false
      }

      else -> {
        // 正常状态下根据业务需求设置
        // 这里保持原来的getCmdXX值不变
      }
    }
  }

  // 安全停止门控制任务
  fun cancelDoorJob() {
    doorJob?.cancel()
    commandScheduler?.cancel()
    doorJob = null

    // 清空队列和待处理指令
    ioScope.launch {
      pendingCommands.clear()
      while (doorQueue.tryReceive().isSuccess) {
        // 清空队列
      }
    }

    println("调试socket 门控制任务已停止")
  }

  // 重置所有指令状态
  fun resetAllCommands() {
    getCmd01.value = false
    getCmd02.value = false
    getCmd03.value = false
    getCmd04.value = false
    getCmd05.value = false
    getCmd06.value = false
    pendingCommands.clear()
  }

  // 添加紧急指令方法
  fun addEmergencyCommand(cmdType: Int) {
    println("调试socket addEmergencyCommand $cmdType")
    addDoorQueue(cmdType, CommandPriority.HIGH)
  }

  // 添加批量指令方法
  fun addBatchCommands(vararg cmdTypes: Int) {
    println("调试socket addBatchCommands $cmdTypes")
    cmdTypes.forEach { cmdType ->
      addDoorQueue(cmdType)
    }
  }

  // 查询队列状态
  fun getQueueStatus(): String {
    return "待处理指令: ${pendingCommands.size}, 队列状态: ${if (doorQueue.isClosedForSend) "已关闭" else "活跃"}"
  }
}
