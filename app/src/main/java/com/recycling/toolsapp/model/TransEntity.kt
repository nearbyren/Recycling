package com.recycling.toolsapp.model

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.Companion.INTEGER
import androidx.room.ColumnInfo.Companion.TEXT
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/***
 * 服务器下发的开舱信息
 */
@Entity(tableName = "TransEntity") class TransEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id", typeAffinity = INTEGER) val id: Long = 0,
    /***
     * 指令
     */
    @ColumnInfo(name = "cmd", typeAffinity = TEXT, defaultValue = "") var cmd: String? = null,
    /***
     * 事务ID
     */
    @ColumnInfo(name = "transId", typeAffinity = TEXT, defaultValue = "") var transId: String? = null,
    /***
     * 仓类型
     */
    @ColumnInfo(name = "openType", typeAffinity = INTEGER) var openType: Int = 0,
    /***
     * 仓门编码
     */
    @ColumnInfo(name = "cabinId", typeAffinity = TEXT, defaultValue = "") var cabinId: String? = null,
    /***
     * 关闭 0.关闭成功
     */
    @ColumnInfo(name = "closeStatus", typeAffinity = INTEGER) var closeStatus: Int = -1,
    /***
     * 打开 1.打开成功
     */
    @ColumnInfo(name = "openStatus", typeAffinity = INTEGER) var openStatus: Int = -1,
    /***
     * 创建时间
     */
    @ColumnInfo(name = "time", typeAffinity = TEXT) var time: String? = null, ) {
    @Ignore constructor() : this(
        0,
        null,
        null,
        0,
        null,
        -1,
        -1,
        null,
    )

    override fun toString(): String {
        return "id=$id," + "transId=${transId}," + "openType=${openType}," + "cabinId=${cabinId}" + "closeStatus=${closeStatus}" + "openStatus=${openStatus}" + "time=${time}"
    }
}

