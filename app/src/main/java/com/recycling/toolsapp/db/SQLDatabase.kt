package com.recycling.toolsapp.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.recycling.toolsapp.dao.InitConfigFlowDao
import com.recycling.toolsapp.dao.LogInfoFlowDao
import com.recycling.toolsapp.dao.CabinFlowDao
import com.recycling.toolsapp.dao.StateFlowDao
import com.recycling.toolsapp.dao.TransFlowDao
import com.recycling.toolsapp.model.CabinEntity
import com.recycling.toolsapp.model.ConfigEntity
import com.recycling.toolsapp.model.LogInfoEntity
import com.recycling.toolsapp.model.StateEntity
import com.recycling.toolsapp.model.TransEntity

@Database(entities = [
    CabinEntity::class,
    ConfigEntity::class,
    StateEntity::class,
    TransEntity::class,
    LogInfoEntity::class], version = 1, exportSchema = false)
abstract class SQLDatabase : RoomDatabase() {

    ///日志操作
    abstract fun logInfoFlow(): LogInfoFlowDao

    ///箱体配置
    abstract fun cabinFlow(): CabinFlowDao

    ///箱体配置
    abstract fun stateFlow(): StateFlowDao

    ///初始化配置
    abstract fun initConfigFlow(): InitConfigFlowDao

    ///打开舱门
    abstract fun transFlowFlow(): TransFlowDao
}
