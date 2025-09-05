package com.recycling.toolsapp.http

import com.google.gson.reflect.TypeToken
import nearby.lib.netwrok.response.CorHttp
import nearby.lib.netwrok.response.InfoResponse
import nearby.lib.netwrok.response.ResponseHolder

class RepoImpl : Repo {
    override suspend fun version(params: MutableMap<String, Any>): ResponseHolder<VersionDto> {
        return CorHttp.getInstance().get(url = HttpUrl.verupdateg, params = params, type = object : TypeToken<InfoResponse<VersionDto>>() {}.type, kClazz = Any::class)
    }

    override suspend fun gdlist3(params: MutableMap<String, Any>): ResponseHolder<MutableList<GdList3Dto>> {
        return CorHttp.getInstance().get(url = HttpUrl.gdlist3, params = params, type = object : TypeToken<InfoResponse<MutableList<GdList3Dto>>>() {}.type, kClazz = Array::class)
    }

    override suspend fun facedload(params: MutableMap<String, Any>): ResponseHolder<MutableList<UserDto>> {
        return CorHttp.getInstance().post(url = HttpUrl.facedload, params = params, type = object : TypeToken<InfoResponse<MutableList<UserDto>>>() {}.type, kClazz = Array::class)
    }


    override suspend fun gdfinish(params: MutableMap<String, Any>): ResponseHolder<String> {
        return CorHttp.getInstance().post(url = HttpUrl.gdfinish, params = params, type = object : TypeToken<InfoResponse<String>>() {}.type, kClazz = String::class)
    }

    override suspend fun boxstatus(params: MutableMap<String, Any>): ResponseHolder<String> {
        return CorHttp.getInstance().post(url = HttpUrl.boxstatus, params = params, type = object : TypeToken<InfoResponse<String>>() {}.type, kClazz = String::class)
    }

    override suspend fun boxiang(params: MutableMap<String, Any>): ResponseHolder<String> {
        return CorHttp.getInstance().post(url = HttpUrl.boxiang, params = params, type = object : TypeToken<InfoResponse<String>>() {}.type, kClazz = String::class)
    }

    override suspend fun faceUpdate(params: MutableMap<String, Any>): ResponseHolder<String> {
        return CorHttp.getInstance().postMultipart(url = HttpUrl.faceUpdate, headers = null, params = params, type = object : TypeToken<InfoResponse<String>>() {}.type, kClazz = String::class)
    }

    override suspend fun addUserFace(params: MutableMap<String, Any>): ResponseHolder<UserDto> {
        return CorHttp.getInstance().postMultipart(url = HttpUrl.addFaceUser, params = params, type = object : TypeToken<InfoResponse<UserDto>>() {}.type, kClazz = Any::class)
    }
}