package com.recycling.toolsapp.http

import nearby.lib.netwrok.response.ResponseHolder

interface Repo {
    suspend fun version(params: MutableMap<String, Any>): ResponseHolder<VersionDto>
    suspend fun gdlist3(params: MutableMap<String, Any>): ResponseHolder<MutableList<GdList3Dto>>
    suspend fun facedload(params: MutableMap<String, Any>): ResponseHolder<MutableList<UserDto>>
    suspend fun gdfinish(params: MutableMap<String, Any>): ResponseHolder<String>
    suspend fun boxstatus(params: MutableMap<String, Any>): ResponseHolder<String>
    suspend fun boxiang(params: MutableMap<String, Any>): ResponseHolder<String>
    suspend fun faceUpdate(params: MutableMap<String, Any>): ResponseHolder<String>
    suspend fun addUserFace(params: MutableMap<String, Any>): ResponseHolder<UserDto>
    suspend fun uploadPhoto(params: MutableMap<String, Any>): ResponseHolder<String>
}
