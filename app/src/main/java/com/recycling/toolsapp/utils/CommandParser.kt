package com.recycling.toolsapp.utils


import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.serial.port.utils.Loge

class CommandParser {
    companion object {
        private val validCommands = setOf("heartBeat", "ota", "update", "status")

        fun parseCommand(jsonString: String): String {
            val jsonElement = JsonParser.parseString(jsonString)
//            require(jsonElement.isJsonObject) { "Invalid JSON format" }

            val jsonObj = jsonElement.asJsonObject
//            require(jsonObj.has("cmd")) { "Missing 'cmd' field" }

            val cmd = jsonObj.get("cmd").asString
//            require(validCommands.contains(cmd)) { "Unsupported command: $cmd" }

            return cmd
        }
    }
}

fun main() {
    val json = """{"cmd":"ota","version":"1.285","url":"","md5":"","sn":"0136004ST00041"}"""
    Loge.e("Extracted command: ${CommandParser.parseCommand(json)}")
}
