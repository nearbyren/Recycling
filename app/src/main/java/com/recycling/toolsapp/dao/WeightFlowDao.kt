package com.recycling.toolsapp.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.recycling.toolsapp.model.LatticeEntity
import com.recycling.toolsapp.model.StateEntity
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

    @Query("select * from WeightEntity WHERE transId = :transId ")
    fun queryWeightId(transId: String): WeightEntity

    @Update fun upWeightEntity(weightEntity: WeightEntity): Int

    @Query("select * from WeightEntity ORDER BY ROWID DESC LIMIT 1")
    fun queryWeightMax(): WeightEntity

    @Query("UPDATE WeightEntity SET status = :status WHERE transId = :transId")
    fun upWeightStatus(status: Int, transId: String)

    @Query("select * from WeightEntity WHERE transId = :transId")
    fun queryWeightEntity(transId: String): WeightEntity
}
