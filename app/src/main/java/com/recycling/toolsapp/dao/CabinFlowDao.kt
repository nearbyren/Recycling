package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recycling.toolsapp.model.CabinEntity
import com.recycling.toolsapp.model.ConfigEntity
import kotlinx.coroutines.flow.Flow


/***
 * 箱体配置
 */
@Dao interface CabinFlowDao {

    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insert(cabinEntity: CabinEntity): Long

    @Query("select * from CabinEntity WHERE cabinId = :cabinId")
    fun queryCabinEntity(cabinId: String): CabinEntity

    //删除所有数据
    @Query("delete from CabinEntity") fun deleteAll()
}
