package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.recycling.toolsapp.model.FileEntity
import com.recycling.toolsapp.model.LatticeEntity
import com.recycling.toolsapp.model.StateEntity
import com.recycling.toolsapp.model.TransEntity
import com.recycling.toolsapp.model.WeightEntity


/***
 *
 */
@Dao interface FileFlowDao {

    //key键重复的替换
    @Insert(onConflict = OnConflictStrategy.IGNORE) fun insert(fileEntity: FileEntity): Long

    //删除所有数据
    @Query("delete from FileEntity") fun deleteAll()

    @Query("select * from FileEntity WHERE cmd = :cmd and transId = :transId")
    fun queryFileEntity(cmd: String, transId: String): FileEntity

    @Update fun upFileEntity(fileEntity: FileEntity): Int

}
