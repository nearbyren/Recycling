package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.recycling.toolsapp.model.TransEntity


/***
 * 箱体配置
 */
@Dao interface TransFlowDao {

    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(trensEntity: TransEntity): Long

    //删除所有数据
    @Query("delete from TransEntity")
    fun deleteAll()
}
