package com.recycling.toolsapp.model

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.Companion.INTEGER
import androidx.room.ColumnInfo.Companion.TEXT
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/***
 * 配置信息
 */
@Entity(tableName = "ConfigEntity") class ConfigEntity(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id", typeAffinity = INTEGER) val id: Long = 0,
    @ColumnInfo(name = "sn", typeAffinity = TEXT, defaultValue = "") var sn: String? = null,
    @ColumnInfo(name = "heartBeatInterval", typeAffinity = TEXT, defaultValue = "") var heartBeatInterval: String? = null,
    @ColumnInfo(name = "turnOnLight", typeAffinity = TEXT, defaultValue = "") var turnOnLight: String? = null,
    @ColumnInfo(name = "turnOffLight", typeAffinity = TEXT, defaultValue = "") var turnOffLight: String? = null,
    @ColumnInfo(name = "lightTime", typeAffinity = TEXT, defaultValue = "") var lightTime: String? = null,
    @ColumnInfo(name = "uploadPhotoURL", typeAffinity = TEXT, defaultValue = "") var uploadPhotoURL: String? = null,
    @ColumnInfo(name = "uploadLogURL", typeAffinity = TEXT, defaultValue = "") var uploadLogURL: String? = null,
    @ColumnInfo(name = "qrCode", typeAffinity = TEXT, defaultValue = "") var qrCode: String? = null,
    @ColumnInfo(name = "logLevel", typeAffinity = INTEGER) var logLevel: Int = 0,
    @ColumnInfo(name = "status", typeAffinity = INTEGER) var status: Int = 0,
    @ColumnInfo(name = "debugPasswd", typeAffinity = TEXT, defaultValue = "") var debugPasswd: String? = null,
    @ColumnInfo(name = "irDefaultState",  typeAffinity = INTEGER) var  irDefaultState: Int = 0,
    @ColumnInfo(name = "weightSensorMode",  typeAffinity = INTEGER) var  weightSensorMode: Int = 0,


    ) {
    @Ignore constructor() : this(
        0,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        0,
        0,
        null,
        0,
        0
        )

    override fun toString(): String {
        return "id=$id," +
                "sn=${sn}," +
                "heartBeatInterval=${heartBeatInterval}," +
                "turnOnLight=${turnOnLight}," +
                "turnOffLight=${turnOffLight}," +
                "lightTime=${lightTime}," +
                "uploadPhotoURL=${uploadPhotoURL}," +
                "uploadLogURL=${uploadLogURL}," +
                "qrCode=${qrCode}," +
                "logLevel=${logLevel}," +
                "status=${status}," +
                "debugPasswd=${debugPasswd}," +
                "irDefaultState=${irDefaultState}," +
                "weightSensorMode=${weightSensorMode}"
    }
}

