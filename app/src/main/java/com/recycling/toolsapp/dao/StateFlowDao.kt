package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recycling.toolsapp.model.CabinEntity
import com.recycling.toolsapp.model.ConfigEntity
import com.recycling.toolsapp.model.StateEntity
import kotlinx.coroutines.flow.Flow


/***
 *
 */
@Dao interface StateFlowDao {

    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insert(stateEntity: StateEntity): Long

    @Query("select * from StateEntity")
    fun queryStateList(): List<StateEntity>

    @Query("select * from StateEntity WHERE cabinId = :cabinId")
    fun queryStateEntity(cabinId: String): StateEntity

    //删除所有数据
    @Query("delete from StateEntity") fun deleteAll()
}
