package com.recycling.toolsapp.http

/**
 * @author: lr
 * @created on: 2025/5/14 上午11:26
 * @description: 每日删除db数据库文件
 */
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.serial.port.utils.Loge
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.concurrent.TimeUnit



object TaskRestartScheduler {

    // 任务标识前缀
    private const val DAILY_TASK_PREFIX = "restart_daily_task_"
    private const val WEEKLY_TASK_PREFIX = "restart_weekly_task_"
    private const val MONTHLY_TASK_PREFIX = "restart_monthly_task_"
    private const val SPECIFIC_DATE_TASK_PREFIX = "restart_specific_date_task_"
    private const val TIME_RANGE_TASK_PREFIX = "restart_time_range_task_"
    private const val IMMEDIATE_TASK_PREFIX = "restart_immediate_task_"

    // 时间格式化器
    @RequiresApi(Build.VERSION_CODES.O)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    @RequiresApi(Build.VERSION_CODES.O)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /**
     * 调度每日任务
     * @param context 上下文
     * @param time 任务执行时间（格式 "HH:mm" 或 "HH:mm:ss"）
     * @param taskName 可选的自定义任务名称
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleDaily(context: Context, time: String, taskName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val fullTaskName = buildTaskName(DAILY_TASK_PREFIX, time, taskName)

        val dailyRequest = buildDailyRequest(time)
        workManager.enqueueUniquePeriodicWork(
            fullTaskName,
            ExistingPeriodicWorkPolicy.UPDATE,
            dailyRequest
        )
        Loge.d("已调度每日任务: $fullTaskName, 执行时间: $time")
    }

    /**
     * 调度每周任务
     * @param context 上下文
     * @param dayOfWeek 星期几 (1-7, 1=周日, 7=周六)
     * @param time 任务执行时间
     * @param taskName 可选的自定义任务名称
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleWeekly(context: Context, dayOfWeek: Int, time: String, taskName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val fullTaskName = buildTaskName(WEEKLY_TASK_PREFIX, "${dayOfWeek}_$time", taskName)

        val weeklyRequest = buildWeeklyRequest(dayOfWeek, time)
        workManager.enqueueUniquePeriodicWork(
            fullTaskName,
            ExistingPeriodicWorkPolicy.UPDATE,
            weeklyRequest
        )
        Loge.d("已调度每周任务: $fullTaskName, 星期$dayOfWeek, 时间: $time")
    }

    /**
     * 调度每月任务
     * @param context 上下文
     * @param dayOfMonth 每月的哪一天 (1-31)
     * @param time 任务执行时间
     * @param taskName 可选的自定义任务名称
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleMonthly(context: Context, dayOfMonth: Int, time: String, taskName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val fullTaskName = buildTaskName(MONTHLY_TASK_PREFIX, "${dayOfMonth}_$time", taskName)

        val monthlyRequest = buildMonthlyRequest(dayOfMonth, time)
        workManager.enqueueUniquePeriodicWork(
            fullTaskName,
            ExistingPeriodicWorkPolicy.UPDATE,
            monthlyRequest
        )
        Loge.d("已调度每月任务: $fullTaskName, 日期: $dayOfMonth, 时间: $time")
    }

    /**
     * 调度特定日期任务
     * @param context 上下文
     * @param date 特定日期 (格式 "yyyy-MM-dd")
     * @param time 任务执行时间
     * @param taskName 可选的自定义任务名称
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleSpecificDate(context: Context, date: String, time: String, taskName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val fullTaskName = buildTaskName(SPECIFIC_DATE_TASK_PREFIX, "${date}_$time", taskName)

        val specificDateRequest = buildSpecificDateRequest(date, time)
        workManager.enqueueUniqueWork(
            fullTaskName,
            ExistingWorkPolicy.REPLACE,
            specificDateRequest
        )
        Loge.d("已调度特定日期任务: $fullTaskName, 日期: $date, 时间: $time")
    }

    /**
     * 调度时间段任务（在指定时间段内随机执行）
     * @param context 上下文
     * @param startTime 开始时间 (格式 "HH:mm")
     * @param endTime 结束时间 (格式 "HH:mm")
     * @param taskName 可选的自定义任务名称
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun scheduleTimeRange(context: Context, startTime: String, endTime: String, taskName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val fullTaskName = buildTaskName(TIME_RANGE_TASK_PREFIX, "${startTime}_$endTime", taskName)

        val timeRangeRequest = buildTimeRangeRequest(startTime, endTime)
        workManager.enqueueUniquePeriodicWork(
            fullTaskName,
            ExistingPeriodicWorkPolicy.UPDATE,
            timeRangeRequest
        )
        Loge.d("已调度时间段任务: $fullTaskName, 时间段: $startTime - $endTime")
    }

    /**
     * 立即执行一次任务
     * @param context 上下文
     * @param taskName 可选的自定义任务名称
     */
    fun triggerImmediately(context: Context, taskName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val fullTaskName = buildTaskName(IMMEDIATE_TASK_PREFIX, "now", taskName)

        val immediateRequest = buildImmediateRequest()
        workManager.enqueueUniqueWork(
            fullTaskName,
            ExistingWorkPolicy.REPLACE,
            immediateRequest
        )
        Loge.d("已触发立即执行任务: $fullTaskName")
    }

    /**
     * 取消指定任务
     */
    fun cancelTask(context: Context, taskType: String, identifier: String, taskName: String? = null) {
        val workManager = WorkManager.getInstance(context)
        val prefix = when (taskType) {
            "daily" -> DAILY_TASK_PREFIX
            "weekly" -> WEEKLY_TASK_PREFIX
            "monthly" -> MONTHLY_TASK_PREFIX
            "specific" -> SPECIFIC_DATE_TASK_PREFIX
            "range" -> TIME_RANGE_TASK_PREFIX
            else -> DAILY_TASK_PREFIX
        }
        val fullTaskName = buildTaskName(prefix, identifier, taskName)
        workManager.cancelUniqueWork(fullTaskName)
        Loge.d("已取消任务: $fullTaskName")
    }

    /**
     * 取消所有任务
     */
    fun cancelAllTasks(context: Context) {
        WorkManager.getInstance(context).cancelAllWork()
        Loge.d("已取消所有任务")
    }

    // 构建每日请求
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildDailyRequest(time: String): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<DailyRestartWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(time))
            .addTag("restart_daily_task")
            .build()
    }

    // 构建每周请求
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildWeeklyRequest(dayOfWeek: Int, time: String): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<DailyRestartWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(calculateWeeklyDelay(dayOfWeek, time))
            .addTag("restart_weekly_task")
            .build()
    }

    // 构建每月请求
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildMonthlyRequest(dayOfMonth: Int, time: String): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<DailyRestartWorker>(30, TimeUnit.DAYS) // 近似每月
            .setInitialDelay(calculateMonthlyDelay(dayOfMonth, time))
            .addTag("restart_monthly_task")
            .build()
    }

    // 构建特定日期请求
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildSpecificDateRequest(date: String, time: String): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<DailyRestartWorker>()
            .setInitialDelay(calculateSpecificDateDelay(date, time))
            .addTag("restart_specific_date_task")
            .build()
    }

    // 构建时间段请求
    @RequiresApi(Build.VERSION_CODES.O)
    private fun buildTimeRangeRequest(startTime: String, endTime: String): PeriodicWorkRequest {
        return PeriodicWorkRequestBuilder<DailyRestartWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateRandomTimeRangeDelay(startTime, endTime))
            .addTag("restart_time_range_task")
            .build()
    }

    // 构建立即请求
    private fun buildImmediateRequest(): OneTimeWorkRequest {
        return OneTimeWorkRequestBuilder<DailyRestartWorker>()
            .addTag("restart_immediate_task")
            .build()
    }

    // 计算每日任务的初始延迟
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateInitialDelay(time: String): Duration {
        val targetTime = parseTimeString(time)
        val now = Calendar.getInstance()
        val targetCal = createTargetCalendar(targetTime, now)
        val delayMillis = targetCal.timeInMillis - now.timeInMillis
        return Duration.ofMillis(delayMillis)
    }

    // 计算每周任务的初始延迟
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateWeeklyDelay(dayOfWeek: Int, time: String): Duration {
        val targetTime = parseTimeString(time)
        val now = Calendar.getInstance()
        val currentDayOfWeek = now.get(Calendar.DAY_OF_WEEK)

        // 计算距离目标星期几还有几天
        var daysToAdd = dayOfWeek - currentDayOfWeek
        if (daysToAdd < 0) {
            daysToAdd += 7
        }

        val targetCal = createTargetCalendar(targetTime, now).apply {
            add(Calendar.DAY_OF_YEAR, daysToAdd)

            // 如果当前时间已经过了今天的目标时间，且目标日期就是今天，则推迟到下周
            if (daysToAdd == 0 && timeInMillis <= now.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 7)
            }
        }

        val delayMillis = targetCal.timeInMillis - now.timeInMillis
        return Duration.ofMillis(delayMillis)
    }

    // 计算每月任务的初始延迟
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateMonthlyDelay(dayOfMonth: Int, time: String): Duration {
        val targetTime = parseTimeString(time)
        val now = Calendar.getInstance()
        val currentDayOfMonth = now.get(Calendar.DAY_OF_MONTH)

        val targetCal = createTargetCalendar(targetTime, now).apply {
            // 设置目标日期
            if (dayOfMonth >= currentDayOfMonth) {
                // 本月还有目标日期
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            } else {
                // 目标日期在下个月
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }

            // 如果当前时间已经过了今天的目标时间，且目标日期就是今天，则推迟到下个月
            if (get(Calendar.DAY_OF_MONTH) == currentDayOfMonth && timeInMillis <= now.timeInMillis) {
                add(Calendar.MONTH, 1)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
            }
        }

        val delayMillis = targetCal.timeInMillis - now.timeInMillis
        return Duration.ofMillis(delayMillis)
    }

    // 计算特定日期任务的延迟
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateSpecificDateDelay(date: String, time: String): Duration {
        val targetDateTime = parseDateTimeString(date, time)
        val now = Instant.now()
        val delayMillis = targetDateTime.toInstant(ZoneOffset.UTC).toEpochMilli() - now.toEpochMilli()

        // 如果目标时间已经过去，返回0（立即执行）
        return Duration.ofMillis(maxOf(delayMillis, 0))
    }

    // 计算时间段内的随机延迟
    @RequiresApi(Build.VERSION_CODES.O)
    private fun calculateRandomTimeRangeDelay(startTime: String, endTime: String): Duration {
        val startLocalTime = parseTimeString(startTime)
        val endLocalTime = parseTimeString(endTime)

        val now = LocalTime.now()
        val todayStart = LocalDateTime.of(LocalDate.now(), startLocalTime)
        val todayEnd = LocalDateTime.of(LocalDate.now(), endLocalTime)

        // 如果当前时间在时间段内，随机选择时间段内的一个时间
        val randomTimeInRange = if (now.isAfter(startLocalTime) && now.isBefore(endLocalTime)) {
            // 在当前时间到结束时间之间随机
            val randomMinutes = (0 until Duration.between(now, endLocalTime).toMinutes()).random()
            now.plusMinutes(randomMinutes)
        } else {
            // 在开始时间到结束时间之间随机
            val totalMinutes = Duration.between(startLocalTime, endLocalTime).toMinutes()
            val randomMinutes = (0 until totalMinutes).random()
            startLocalTime.plusMinutes(randomMinutes)
        }

        val targetDateTime = LocalDateTime.of(LocalDate.now(), randomTimeInRange)
        val delayMillis = Duration.between(LocalDateTime.now(), targetDateTime).toMillis()

        // 如果延迟为负（当前时间已过随机时间），推迟到明天
        return Duration.ofMillis(if (delayMillis < 0) delayMillis + 24 * 60 * 60 * 1000 else delayMillis)
    }

    // 创建目标日历
    @RequiresApi(Build.VERSION_CODES.O) private fun createTargetCalendar(targetTime: LocalTime, now: Calendar): Calendar {
        return Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, targetTime.hour)
            set(Calendar.MINUTE, targetTime.minute)
            set(Calendar.SECOND, targetTime.second)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // 解析时间字符串
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseTimeString(time: String): LocalTime {
        return try {
            LocalTime.parse(time)
        } catch (e: Exception) {
            try {
                LocalTime.parse("$time:00")
            } catch (e: Exception) {
                Loge.e("Invalid time format: $time. Using default 23:00")
                LocalTime.of(23, 0)
            }
        }
    }

    // 解析日期时间字符串
    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDateTimeString(date: String, time: String): LocalDateTime {
        val localDate = try {
            LocalDate.parse(date)
        } catch (e: Exception) {
            Loge.e("Invalid date format: $date. Using today")
            LocalDate.now()
        }

        val localTime = parseTimeString(time)
        return LocalDateTime.of(localDate, localTime)
    }

    // 构建唯一的任务名称
    private fun buildTaskName(prefix: String, identifier: String, customName: String?): String {
        return if (!customName.isNullOrEmpty()) {
            "${prefix}_${customName}_$identifier"
        } else {
            "${prefix}_$identifier"
        }
    }

    /**
     * 获取所有已调度的任务信息
     */
    @RequiresApi(Build.VERSION_CODES.O)
    fun getScheduledTasksInfo(context: Context): List<String> {
        val workManager = WorkManager.getInstance(context)
        val tasksInfo = mutableListOf<String>()

        // 注意：这里需要在实际使用中通过WorkManager的getWorkInfos方法来获取任务信息
        // 这里只是返回一个简单的信息列表

        return tasksInfo
    }



//// 调度每日任务（每天14:30执行）
//    TaskRestartScheduler.scheduleDaily(context, "14:30", "daily_cleanup")
//
//// 调度每周任务（每周一09:00执行）
//    TaskRestartScheduler.scheduleWeekly(context, Calendar.MONDAY, "09:00", "weekly_report")
//
//// 调度每月任务（每月15号18:00执行）
//    TaskRestartScheduler.scheduleMonthly(context, 15, "18:00", "monthly_maintenance")
//
//// 调度特定日期任务（2024年12月25日10:00执行）
//    TaskRestartScheduler.scheduleSpecificDate(context, "2024-12-25", "10:00", "christmas_special")
//
//// 调度时间段任务（在09:00-17:00之间随机时间执行）
//    TaskRestartScheduler.scheduleTimeRange(context, "09:00", "17:00", "random_check")
//
//// 立即执行任务
//    TaskRestartScheduler.triggerImmediately(context, "urgent_cleanup")
//
//// 取消特定任务
//    TaskRestartScheduler.cancelTask(context, "daily", "14:30", "daily_cleanup")
//
//// 取消所有任务
//    TaskRestartScheduler.cancelAllTasks(context)
}