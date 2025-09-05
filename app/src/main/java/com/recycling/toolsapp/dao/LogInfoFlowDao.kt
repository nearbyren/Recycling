package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recycling.toolsapp.model.LogInfoEntity
import kotlinx.coroutines.flow.Flow


/***
 * 日志记录信息
 */
@Dao interface LogInfoFlowDao {
    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(LogInfo: LogInfoEntity): Long

    //结合flow监听数据变化，避免重复更新ui数据
    @Query("select * from LogInfo ORDER BY id DESC")
    fun queryLoginInfos(): Flow<List<LogInfoEntity>>

    //根据条件查询
    @Query("select * from LogInfo WHERE userId = :userId ORDER BY id DESC")
    fun queryLogInfoUserId(userId: String): Flow<List<LogInfoEntity>>

    @Query("select * from LogInfo WHERE boxCode = :boxCode")
    fun queryLogInfoBoxCode(boxCode: Int) : Flow<List<LogInfoEntity>>

    //根据条件查询
    @Query("select * from LogInfo WHERE boxStatus = :bxoStatus")
    fun queryLogInfoBoxStatus(bxoStatus: String): Flow<List<LogInfoEntity>>

    //删除所有数据
    @Query("delete from LogInfo")
    fun deleteAll()
}
