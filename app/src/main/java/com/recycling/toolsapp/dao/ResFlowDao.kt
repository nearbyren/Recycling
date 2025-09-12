package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.recycling.toolsapp.model.ResEntity


/***
 *图片资源 音频资源dao
 */
@Dao interface ResFlowDao {

    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insert(resEntity: ResEntity): Long

    @Query("select * from ResEntity WHERE filename = :filename")
    fun queryRes(filename: String): ResEntity

    @Update fun upResEntity(resEntity: ResEntity): Int

    //删除所有数据
    @Query("delete from ResEntity") fun deleteAll()
}
