package com.recycling.toolsapp.model

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.Companion.INTEGER
import androidx.room.ColumnInfo.Companion.TEXT
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/***
 * 箱体信息
 */
@Entity(tableName = "CabinEntity") class CabinEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id", typeAffinity = INTEGER) val id: Long = 0,
    /***
     * 舱门编码
     */
    @ColumnInfo(name = "cabinId", typeAffinity = TEXT, defaultValue = "") var cabinId: String? = null,
    /***
     * 容量
     */
    @ColumnInfo(name = "capacity", typeAffinity = TEXT, defaultValue = "") var capacity: String? = null,
    /***
     * 创建时间
     */
    @ColumnInfo(name = "createTime", typeAffinity = TEXT, defaultValue = "") var createTime: String? = null,
    /***
     *删除flag
     */
    @ColumnInfo(name = "delFlag", typeAffinity = TEXT, defaultValue = "") var delFlag: String? = null,
    /***
     * 门状态
     */
    @ColumnInfo(name = "doorStatus", typeAffinity = TEXT, defaultValue = "") var doorStatus: String? = null,
    /***
     *填充时间
     */
    @ColumnInfo(name = "filledTime", typeAffinity = TEXT, defaultValue = "") var filledTime: String? = null,
    /***
     *网络ID
     */
    @ColumnInfo(name = "netId", typeAffinity = INTEGER) var netId: Int = 0,
    /***
     *红外
     */
    @ColumnInfo(name = "ir", typeAffinity = INTEGER) var ir: Int = 0,
    /***
     *超重
     */
    @ColumnInfo(name = "overweight", typeAffinity = TEXT, defaultValue = "") var overweight: String? = null,
    /***
     *价格
     */
    @ColumnInfo(name = "price", typeAffinity = TEXT, defaultValue = "") var price: String? = null,
    /***
     *挡杆的值
     */
    @ColumnInfo(name = "rodHinderValue",  typeAffinity = INTEGER) var rodHinderValue: Int = 0,
    /***
     *sn码
     */
    @ColumnInfo(name = "sn", typeAffinity = TEXT, defaultValue = "") var sn: String? = null,
    /***
     *烟雾警报
     */
    @ColumnInfo(name = "smoke", typeAffinity = INTEGER) var smoke: Int = 0,
    /***
     *排序
     */
    @ColumnInfo(name = "sort", typeAffinity = INTEGER) var sort: Int = 0,
    /***
     *同步状态  0 1
     */
    @ColumnInfo(name = "sync", typeAffinity = TEXT, defaultValue = "") var sync: String? = null,
    /***
     *音量大小
     */
    @ColumnInfo(name = "volume", typeAffinity = INTEGER) var volume: Int = 0,
    /***
     *重量
     */
    @ColumnInfo(name = "weight", typeAffinity = TEXT, defaultValue = "") var weight: String? = null,

    ) {
    @Ignore constructor() : this(
        0,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        0,
        null,
        null,
        0,
        null,
        0,
        0,
        null,
        0,
        null,

        )

    override fun toString(): String {
        return "id=$id," + "cabinId=${cabinId}," + "capacity=${capacity}," + "createTime=${createTime}," + "delFlag=${delFlag}," + "doorStatus=${doorStatus}," + "filledTime=${filledTime}," + "netId=${netId}," + "ir=${ir}," + "overweight=${overweight}," + "overweight=${overweight}," + "price=${price}," + "rodHinderValue=${rodHinderValue}," + "sn=${sn}," + "smoke=${smoke}," + "sort=${sort}," + "sync=${sync}," + "volume=${volume}," + "weight=${weight}"
    }
}

