package com.recycling.toolsapp.model

import androidx.room.ColumnInfo
import androidx.room.ColumnInfo.Companion.INTEGER
import androidx.room.ColumnInfo.Companion.TEXT
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

/***
 * 日志信息
 */
@Entity(tableName = "LogInfo") class LogInfoEntity(
    /***
     * 日志主键id
     */
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id", typeAffinity = INTEGER) val id: Long = 0,
    /***
     * 用户id
     */
    @ColumnInfo(name = "userId", typeAffinity = TEXT, defaultValue = "") var userId: String? = null,
    /***
     * 员工账号
     */
    @ColumnInfo(name = "account", typeAffinity = TEXT, defaultValue = "") var account: String? = null,
    /***
     * 工号
     */
    @ColumnInfo(name = "userCode", typeAffinity = TEXT, defaultValue = "") var userNo: String? = null,
    /***
     * 名称
     */
    @ColumnInfo(name = "userName", typeAffinity = TEXT, defaultValue = "") var userName: String? = null,
    /***
     * 仓号
     */
    @ColumnInfo(name = "boxCode", typeAffinity = INTEGER) var boxCode: Int = 0,

    /***
     * 0.还箱子 1.取箱子 2.人脸检测 3.网络 4.定时检测非正常操作 5.仓状态 6.管理员开仓  101.开仓进行取箱子流程 111.开仓进行还箱子流程
     */
    @ColumnInfo(name = "optStatus", typeAffinity = INTEGER, defaultValue = 0.toString()) var optStatus: Int = 0,

    /***
     * 当前仓状态 1.在仓 领取开始还是1  领取流程完整则为0   |  0.空闲  归还开始还是0 归还流程完整则为1
     */
    @ColumnInfo(name = "boxStatus", typeAffinity = TEXT, defaultValue = "") var boxStatus: String? = null,
    /***
     * 仓类型
     */
    @ColumnInfo(name = "boxType", typeAffinity = TEXT, defaultValue = "") var boxType: String? = null,
    /***
     * 消息
     */
    @ColumnInfo(name = "msg", typeAffinity = TEXT, defaultValue = "") var msg: String? = null,
    /***
     * 时间
     */
    @ColumnInfo(name = "time", typeAffinity = TEXT, defaultValue = "") var time: String? = null,

    ) {
    @Ignore constructor() : this(0, null, null, null, null, 0, -1, null, null, null, null)

    override fun toString(): String {
        return "id=$id,userId=${userId},account=${account},userNo=${userNo},userName=${userName},boxCode=${boxCode},optStatus=${optStatus},boxStatus=${boxStatus},boxType=${boxType},msg=${msg},time=${time}"
    }
}

