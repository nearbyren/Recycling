package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recycling.toolsapp.model.ConfigEntity
import com.recycling.toolsapp.model.LogInfoEntity
import kotlinx.coroutines.flow.Flow


/***
 * 日志记录信息
 */
@Dao interface InitConfigFlowDao {
    
    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(configEntity: ConfigEntity): Long

    @Query("select * from ConfigEntity WHERE sn = :sn")
    fun queryInitConfig(sn:String): ConfigEntity

    //删除所有数据
    @Query("delete from ConfigEntity")
    fun deleteAll()
}
