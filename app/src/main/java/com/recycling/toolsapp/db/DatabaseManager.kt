package com.recycling.toolsapp.db

/**
 * @author: lr
 * @created on: 2024/8/29 10:45 PM
 * @description:
 */
import android.content.Context
import android.os.Environment
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.recycling.toolsapp.dao.CabinFlowDao
import com.recycling.toolsapp.dao.InitConfigFlowDao
import com.recycling.toolsapp.dao.LogInfoFlowDao
import com.recycling.toolsapp.dao.StateFlowDao
import com.recycling.toolsapp.dao.TransFlowDao
import com.recycling.toolsapp.http.MailConfig
import com.recycling.toolsapp.http.MailSender
import com.recycling.toolsapp.model.CabinEntity
import com.recycling.toolsapp.model.ConfigEntity
import com.recycling.toolsapp.model.LogInfoEntity
import com.recycling.toolsapp.model.StateEntity
import com.recycling.toolsapp.model.TransEntity
import com.serial.port.utils.AppUtils
import com.serial.port.utils.Loge
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

object DatabaseManager {

    private const val DATABASE_NAME = "recycling_database"
    private const val DATABASE_PATH = "/data/data/com.recycling.toolsapp/databases/"

    @Volatile
    private var instance: SQLDatabase? = null
    private fun getDatabase(context: Context): SQLDatabase {
        return instance ?: synchronized(this) {
            //升级数据添加字段
            val MIGRATION_1_2 = object : Migration(1, 2) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // 执行 ALTER TABLE 添加新字段
                    database.execSQL("ALTER TABLE BoxDevice ADD COLUMN boxUTime TEXT DEFAULT ''")
                    //        database.execSQL("ALTER TABLE your_table ADD COLUMN new_int_column INTEGER DEFAULT 0")
                }
            }
            // 数据库名称
            val newInstance =
                    Room.databaseBuilder(context.applicationContext, SQLDatabase::class.java, DATABASE_NAME)
//                        .addMigrations(MIGRATION_1_2)
                        .addCallback(object : RoomDatabase.Callback() {
                            override fun onCreate(db: SupportSQLiteDatabase) {
                                super.onCreate(db)
                                CoroutineScope(Dispatchers.IO).launch {
                                    //初始化信息
//                                    initState()
                                }
                            }
                        }).build()
            instance = newInstance
            newInstance
        }

    }

    private fun initState() {
        println("调试socket  initState")
        val state = StateEntity().apply {
            smoke = 0
            capacity = 0
            irState = 0
            weigh = 0f
            doorStatus = 0
            cabinId = "12345679"
            time = AppUtils.getDateYMDHMS()
        }
        val row = insertState(AppUtils.getContext(), state)
        println("调试socket  initState row $row")
    }

    fun copyDatabase(context: Context, databaseName: String = DATABASE_NAME) {
        val dbFile = File(DATABASE_PATH + DATABASE_NAME)
        //导出的文件路径+文件名称
        val path =
                AppUtils.getContext().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.absolutePath + "/"
        Loge.d("每天拷贝数据库 数据库创建目录... $path")
        val externalFile = File(path + databaseName)

        // 确保外部存储目录存在
        val externalDir = File(path)
        if (!externalDir.exists()) {
            Loge.d("每天拷贝数据库 数据库创建目录...")
            externalDir.mkdirs()
        }
        try {
            CoroutineScope(Dispatchers.IO).launch {
                // 复制文件到外部存储
                copyFile(dbFile, externalFile)
//                insertBackUp(AppUtils.getContext(), BackupEntity().apply {
//                    backupName = databaseName
//                    time = AppUtils.getDateYMDHMS()
//                })
                delay(1000)
                senMail(externalFile)

                Loge.d("每天拷贝数据库 数据库导出成功...")
            }
        } catch (e: IOException) {
            Loge.d("每天拷贝数据库 导出数据库失败...")
            e.printStackTrace()
        }
    }

    private suspend fun senMail(file: File) {
        val mailConfig = MailConfig.Builder().apply {
            host = "smtp.qq.com"
            port = 587
//                    port = 465
            username = "860023654@qq.com"
            password = "raiszbpinaznbbjd" // 或 oauthToken("ya29.token")
            setRecipient("860023654@qq.com")
            setSubject("拷贝db文件")
            setBody("<b>主要查看附件信息</b>")
            setAttach(file)
        }.build()
        when (val result = MailSender.sendDirectly(mailConfig)) {
            is MailSender.Result.Success -> Loge.d("发送邮件 发送成功")
            is MailSender.Result.Failure -> Loge.d("发送邮件 ${result.exception}")
        }
    }

    /**
     * 复制数据库文件到外部存储
     */
    private fun copyFile(inputFile: File, outputFile: File) {
        val inputStream: InputStream = FileInputStream(inputFile)
        val outputStream: OutputStream = FileOutputStream(outputFile)

        val buffer = ByteArray(1024)
        var length: Int
        while (inputStream.read(buffer).also { length = it } > 0) {
            outputStream.write(buffer, 0, length)
        }

        outputStream.flush()
        outputStream.close()
        inputStream.close()
    }

    /***************************************获取 箱体 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return 返回日志dao
     */
    private fun getCabinFlowDao(context: Context): CabinFlowDao {
        return getDatabase(context).cabinFlow()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param cabinEntity
     */
    fun insertCabin(context: Context, cabinEntity: CabinEntity): Long {
        return getCabinFlowDao(context).insert(cabinEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param cabinId
     * @return
     */
    fun queryCabinEntity(context: Context, cabinId: String): CabinEntity {
        return getCabinFlowDao(context).queryCabinEntity(cabinId)
    }
    /***************************************获取 箱体 实例2 *************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return 返回日志dao
     */
    private fun getStateFlowDao(context: Context): StateFlowDao {
        return getDatabase(context).stateFlow()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param stateEntity
     */
    fun insertState(context: Context, stateEntity: StateEntity): Long {
        return getStateFlowDao(context).insert(stateEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param cabinId
     * @return
     */
    fun queryStateEntity(context: Context, cabinId: String): StateEntity {
        return getStateFlowDao(context).queryStateEntity(cabinId)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param cabinId
     * @return
     */
    fun queryStateList(context: Context): List<StateEntity> {
        return getStateFlowDao(context).queryStateList()
    }

    /***************************************获取 箱体 实例*************************************************/

    /***************************************获取 初始化配置 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return 返回日志dao
     */
    private fun getInitConfigFlowDao(context: Context): InitConfigFlowDao {
        return getDatabase(context).initConfigFlow()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param configEntity
     */
    fun insertInitConfig(context: Context, configEntity: ConfigEntity): Long {
        return getInitConfigFlowDao(context).insert(configEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param sn
     * @return
     */
    fun queryInitConfig(context: Context, sn: String): ConfigEntity {
        return getInitConfigFlowDao(context).queryInitConfig(sn)
    }

    /***************************************获取 初始化配置 实例*************************************************/

    /***************************************获取 打开仓 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return
     */
    private fun getTransFlowDao(context: Context): TransFlowDao {
        return getDatabase(context).transFlowFlow()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param trensEntity 插入一条记录
     */
    fun insertTrans(context: Context, trensEntity: TransEntity): Long {
        return getTransFlowDao(context).insert(trensEntity)
    }

    /***************************************获取 打开仓 实例*************************************************/

    /***************************************获取 日志记录 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return
     */
    private fun getLogInfoFlowDao(context: Context): LogInfoFlowDao {
        return getDatabase(context).logInfoFlow()
    }

    /***
     * 提供外部 API 方法 插入日志记录信息
     * @param context 上下文
     * @param logInfoEntity 插入日志信息
     */
    fun insertLogInfo(context: Context, logInfoEntity: LogInfoEntity): Long {
        return getLogInfoFlowDao(context).insert(logInfoEntity)
    }

    /***
     * 提供外部 API 方法 获取所有日志记录
     * @param context 上下文
     * @return 返回日志信息集合 flow
     */
    fun queryLoginInfos(context: Context): Flow<List<LogInfoEntity>> {
        return getLogInfoFlowDao(context).queryLoginInfos()
    }

    /***
     * 提供外部 API 方法 根据user id 获取所有日志记录
     * @param context 上下文
     * @param userId  用户id
     * @return 返回日志信息集合 flow
     */
    fun queryLogInfoUserId(context: Context, userId: String): Flow<List<LogInfoEntity>> {
        return getLogInfoFlowDao(context).queryLogInfoUserId(userId)
    }

    /***
     * 提供外部 API 方法 获取所有日志记录
     * @param context 上下文
     * @param boxCode 仓号
     * @return 返回仓号的所有日志记录 flow
     */
    fun queryLogInfoboxCode(context: Context, boxCode: Int): Flow<List<LogInfoEntity>> {
        return getLogInfoFlowDao(context).queryLogInfoBoxCode(boxCode)
    }

    /***
     * 提供外部 API 方法 获取所有日志记录
     * @param context 上下文
     * @param boxStatus 仓状态
     * @return 返回某种仓状态所有仓日志记录 flow
     */
    fun queryLogInfoboxStatus(context: Context, boxStatus: String): Flow<List<LogInfoEntity>> {
        return getLogInfoFlowDao(context).queryLogInfoBoxStatus(boxStatus)
    }

    /***************************************获取 日志记录 实例*************************************************/

}
