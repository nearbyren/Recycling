package com.recycling.toolsapp.model

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.Companion.INTEGER
import androidx.room.ColumnInfo.Companion.REAL
import androidx.room.ColumnInfo.Companion.TEXT
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/***
 * 心跳上报的状态
 */
@Entity(tableName = "StateEntity") class StateEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id", typeAffinity = INTEGER) val id: Long = 0,
    /***
     * 烟雾警报状态
     */
    @ColumnInfo(name = "smoke", typeAffinity = INTEGER) var smoke: Int = 0,
    /***
     * 容量
     */
    @ColumnInfo(name = "capacity", typeAffinity = INTEGER) var capacity: Int = 0,
    /***
     * 红外状态
     */
    @ColumnInfo(name = "irState", typeAffinity = INTEGER) var irState: Int = 0,
    /***
     * 当前重量
     */
    @ColumnInfo(name = "weigh", typeAffinity = REAL) var weigh: Float = 0f,
    /**
     * 门状态
     */
    @ColumnInfo(name = "doorStatus", typeAffinity = INTEGER) var doorStatus: Int = 0,
    /***
     * 舱门编码
     */
    @ColumnInfo(name = "cabinId", typeAffinity = TEXT, defaultValue = "") var cabinId: String? = null,
    /***
     * 创建时间
     */
    @ColumnInfo(name = "time", typeAffinity = TEXT, defaultValue = "") var time: String? = null,
    ) {
    @Ignore constructor() : this(
        0,
        0,
        0,
        0,
        0f,
        0,
        null,
        null,
        )

    override fun toString(): String {
        return "id=$id," + "smoke=${smoke}," + "capacity=${capacity}," + "irState=${irState}," + "weigh=${weigh}," + "doorStatus=${doorStatus}," + "cabinId=${cabinId}," + "time=${time}"
    }
}

