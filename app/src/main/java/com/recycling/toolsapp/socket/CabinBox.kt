package com.recycling.toolsapp.socket

/**
 * 箱体信息
 */
data class CabinBox(
    var cabinId: String? = null,
    var capacity: String? = null,
    var createTime: String? = null,
    var delFlag: String? = null,
    var doorStatus: String? = null,
    var filledTime: String? = null,
    var id: Int  ,
    var ir: Int ,
    var overweight: String? = null,
    var price: String? = null,
    var rodHinderValue: Int,
    var smoke: Int,
    var sn: String? = null,
    var sort: Int,
    var sync: String? = null,
    var volume: Int,
    var weight: String? = null,
)