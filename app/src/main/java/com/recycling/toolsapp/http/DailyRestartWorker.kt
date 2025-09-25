package com.recycling.toolsapp.http

/**
 * @author: lr
 * @created on: 2025/5/14 上午11:25
 * @description:指定时间重启
 */
import android.content.Context
import android.os.Environment
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recycling.toolsapp.utils.HexConverter
import com.serial.port.utils.AppUtils
import com.serial.port.utils.Loge

/***
 * 指定时间重启
 */
class DailyRestartWorker(
    context: Context, params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // 在此调用你的 copyDB 方法
        restart()
        return Result.success()
    }

    private fun restart() {
        try {
            HexConverter.restartApp2(AppUtils.getContext(), 2 * 1000L)
        } catch (e: Exception) {
            Loge.d("指定时间重启动app... ${e.message}")
        }
    }

}