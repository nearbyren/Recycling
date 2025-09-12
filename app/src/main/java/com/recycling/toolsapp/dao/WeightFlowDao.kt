package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recycling.toolsapp.model.TransEntity
import com.recycling.toolsapp.model.WeightEntity


/***
 *
 */
@Dao interface WeightFlowDao {

    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insert(weightEntity: WeightEntity): Long

    //删除所有数据
    @Query("delete from WeightEntity") fun deleteAll()
}
