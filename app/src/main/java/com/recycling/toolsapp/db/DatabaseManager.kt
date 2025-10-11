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
import com.recycling.toolsapp.dao.LatticeFlowDao
import com.recycling.toolsapp.dao.ConfigFlowDao
import com.recycling.toolsapp.dao.FileFlowDao
import com.recycling.toolsapp.dao.LogFlowDao
import com.recycling.toolsapp.dao.ResFlowDao
import com.recycling.toolsapp.dao.StateFlowDao
import com.recycling.toolsapp.dao.TransFlowDao
import com.recycling.toolsapp.dao.WeightFlowDao
import com.recycling.toolsapp.http.MailConfig
import com.recycling.toolsapp.http.MailSender
import com.recycling.toolsapp.model.LatticeEntity
import com.recycling.toolsapp.model.ConfigEntity
import com.recycling.toolsapp.model.FileEntity
import com.recycling.toolsapp.model.LogEntity
import com.recycling.toolsapp.model.ResEntity
import com.recycling.toolsapp.model.StateEntity
import com.recycling.toolsapp.model.TransEntity
import com.recycling.toolsapp.model.WeightEntity
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

private const val s = "TABLE"

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
//                    database.execSQL("ALTER TABLE BoxDevice ADD COLUMN boxUTime TEXT DEFAULT ''")
//                    database.execSQL("ALTER TABLE ResEntity ADD COLUMN sn TEXT DEFAULT NULL")
//                    database.execSQL("ALTER TABLE FileEntity ADD COLUMN status INTEGER NOT NULL DEFAULT 0")
                }
            }
            val MIGRATION_2_3 = object : Migration(2, 3) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // 执行 ALTER TABLE 添加新字段
                    database.execSQL("ALTER TABLE ResEntity ADD COLUMN cmd TEXT DEFAULT ''")
                    database.execSQL("ALTER TABLE ResEntity ADD COLUMN version TEXT DEFAULT ''")
                    database.execSQL("ALTER TABLE ResEntity ADD COLUMN sn TEXT DEFAULT ''")
                }
            }
            val MIGRATION_3_4 = object : Migration(3, 4) {
                override fun migrate(database: SupportSQLiteDatabase) {
                    // 执行 ALTER TABLE 添加新字段
                    database.execSQL("ALTER TABLE WeightEntity ADD COLUMN cabinId TEXT DEFAULT ''")
                }
            }
            // 数据库名称
            val newInstance =
                    Room.databaseBuilder(context.applicationContext, SQLDatabase::class.java, DATABASE_NAME).addMigrations(MIGRATION_3_4).addCallback(object : RoomDatabase.Callback() {
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
    private fun getLatticeFlowDao(context: Context): LatticeFlowDao {
        return getDatabase(context).latticeFlow()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     */
    fun queryLattices(context: Context): List<LatticeEntity> {
        return getLatticeFlowDao(context).queryLattices()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param latticeEntity
     */
    fun insertLattice(context: Context, latticeEntity: LatticeEntity): Long {
        return getLatticeFlowDao(context).insert(latticeEntity)
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param latticeEntity
     * @return
     */
    fun upLatticeEntity(context: Context, latticeEntity: LatticeEntity): Int {
        return getLatticeFlowDao(context).upLatticeEntity(latticeEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param cabinId
     * @return
     */
    fun queryLatticeEntity(context: Context, cabinId: String): LatticeEntity {
        return getLatticeFlowDao(context).queryLatticeEntity(cabinId)
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

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param stateEntity
     * @return
     */
    fun upStateEntity(context: Context, stateEntity: StateEntity): Int {
        return getStateFlowDao(context).upStateEntity(stateEntity)
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
    private fun getInitConfigFlowDao(context: Context): ConfigFlowDao {
        return getDatabase(context).initConfigFlow()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param configEntity
     */
    fun insertConfig(context: Context, configEntity: ConfigEntity): Long {
        return getInitConfigFlowDao(context).insert(configEntity)
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param configEntity
     * @return
     */
    fun upConfigEntity(context: Context, configEntity: ConfigEntity): Int {
        return getInitConfigFlowDao(context).upConfigEntity(configEntity)
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

    /***************************************获取 事务记录 实例*************************************************/
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

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param closeStatus
     * @param transId
     */
    fun upTransCloseStatus(context: Context, closeStatus: Int, transId: String) {
        getTransFlowDao(context).upTransCloseStatus(closeStatus, transId)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param openStatus
     * @param transId
     */
    fun upTransOpenStatus(context: Context, openStatus: Int, transId: String) {
        getTransFlowDao(context).upTransOpenStatus(openStatus, transId)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param openStatus
     */
    fun queryTransOpenStatus(context: Context, openStatus: Int): List<TransEntity> {
        return getTransFlowDao(context).queryTransOpenStatus(openStatus)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     */
    fun queryTransMax(context: Context): TransEntity {
        return getTransFlowDao(context).queryTransMax()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param transId
     */
    fun queryTransEntity(context: Context, transId: String): TransEntity {
        return getTransFlowDao(context).queryTransEntity(transId)
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param transEntity
     * @return
     */
    fun upTransEntity(context: Context, transEntity: TransEntity): Int {
        return getTransFlowDao(context).upTransEntity(transEntity)
    }
    /***************************************获取 打开仓 实例*************************************************/

    /***************************************获取 记录当前重量 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return
     */
    private fun getWeightFlowDao(context: Context): WeightFlowDao {
        return getDatabase(context).weightFlowDao()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param weightEntity 插入一条记录
     */
    fun insertWeight(context: Context, weightEntity: WeightEntity): Long {
        return getWeightFlowDao(context).insert(weightEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param status
     */
    fun queryWeightStatus(context: Context, status: Int): List<WeightEntity> {
        return getWeightFlowDao(context).queryWeightStatus(status)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param transId
     */
    fun queryWeightId(context: Context, transId: String): WeightEntity {
        return getWeightFlowDao(context).queryWeightId(transId)
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param weightEntity
     * @return
     */
    fun upWeightEntity(context: Context, weightEntity: WeightEntity): Int {
        return getWeightFlowDao(context).upWeightEntity(weightEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     */
    fun queryWeightMax(context: Context): WeightEntity {
        return getWeightFlowDao(context).queryWeightMax()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param status  10.进行中 1.完成
     * @param transId 事务id
     */
    fun upWeightStatus(context: Context, status: Int, transId: String) {
        getWeightFlowDao(context).upWeightStatus(status, transId)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param transId
     */
    fun queryWeightEntity(context: Context, transId: String): WeightEntity {
        return getWeightFlowDao(context).queryWeightEntity(transId)
    }

    /***************************************获取 记录当前重量*************************************************/

    /***************************************获取 文件上传 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return
     */
    private fun getFileFlowDao(context: Context): FileFlowDao {
        return getDatabase(context).fileFlowDao()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param fileEntity 插入一条记录
     */
    fun insertFile(context: Context, fileEntity: FileEntity): Long {
        return getFileFlowDao(context).insert(fileEntity)
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param cmd
     * @param transId
     * @return
     */
    fun queryFileEntity(context: Context, cmd: String, transId: String): FileEntity {
        return getFileFlowDao(context).queryFileEntity(cmd, transId)
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param FileEntity
     * @return
     */
    fun upFileEntity(context: Context, fileEntity: FileEntity): Int {
        return getFileFlowDao(context).upFileEntity(fileEntity)
    }

    /***************************************获取 文件上传*************************************************/

    /***************************************获取 资源 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return
     */
    private fun getResFlowDao(context: Context): ResFlowDao {
        return getDatabase(context).resFlowDao()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param resourceEntity 插入一条记录
     */
    fun insertRes(context: Context, resourceEntity: ResEntity): Long {
        return getResFlowDao(context).insert(resourceEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param filename
     * @return
     */
    fun queryResName(context: Context, filename: String): ResEntity {
        return getResFlowDao(context).queryResName(filename)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param version
     * @param sn
     * @param cmd
     * @return
     */
    fun queryResCmd(context: Context, version: String, sn: String, cmd: String): ResEntity {
        return getResFlowDao(context).queryResCmd(version, sn, cmd)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param version
     * @return
     */
    fun queryResVersion(context: Context, version: String): ResEntity {
        return getResFlowDao(context).queryResVersion(version)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     */
    fun queryResMax(context: Context): ResEntity {
        return getResFlowDao(context).queryResEntityMax()
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param resourceEntity
     * @return
     */
    fun upResEntity(context: Context, resourceEntity: ResEntity): Int {
        return getResFlowDao(context).upResEntity(resourceEntity)
    }

    /**
     * 提供外部 API 方法
     * @param context 上下文
     * @param id
     * @param status
     * @return
     */
    fun upResStatus(context: Context, id: Long, status: Int) {
        getResFlowDao(context).upResStatus(id, status)
    }

    /***************************************获取 资源*************************************************/

    /***************************************获取 日志记录 实例*************************************************/
    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return
     */
    private fun getLogInfoFlowDao(context: Context): LogFlowDao {
        return getDatabase(context).logFlow()
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @param
     */
    fun insertLog(context: Context, logEntity: LogEntity): Long {
        return getLogInfoFlowDao(context).insert(logEntity)
    }

    /***
     * 提供外部 API 方法
     * @param context 上下文
     * @return
     */
    fun queryLogs(context: Context): Flow<List<LogEntity>> {
        return getLogInfoFlowDao(context).queryLoginInfos()
    }

    /***************************************获取 日志记录 实例*************************************************/

}
