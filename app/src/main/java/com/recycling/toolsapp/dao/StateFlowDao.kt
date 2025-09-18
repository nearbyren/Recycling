package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.recycling.toolsapp.model.LatticeEntity
import com.recycling.toolsapp.model.StateEntity


/***
 *心跳操作dao
 */
@Dao interface StateFlowDao {

    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insert(stateEntity: StateEntity): Long

    @Query("select * from StateEntity") fun queryStateList(): List<StateEntity>

    @Query("select * from StateEntity WHERE cabinId = :cabinId")
    fun queryStateEntity(cabinId: String): StateEntity

    @Update fun upStateEntity(stateEntity: StateEntity): Int

    //删除所有数据
    @Query("delete from StateEntity") fun deleteAll()
}
