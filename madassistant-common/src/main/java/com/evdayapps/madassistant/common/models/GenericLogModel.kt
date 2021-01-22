package com.evdayapps.madassistant.common.models

import org.json.JSONObject
import java.lang.Exception

class GenericLogModel {

    companion object {
        private const val KEY_type = "type"
        private const val KEY_tag = "tag"
        private const val KEY_message = "message"
    }

    val type : Int
    val tag : String
    val message : String

    constructor(
        type : Int,
        tag : String,
        message : String
    ) {
        this.tag = tag
        this.type = type
        this.message = message
    }

    @Throws(Exception::class)
    constructor(json : String) {
        JSONObject(json).apply {
            type = getInt(KEY_type)
            tag = getString(KEY_tag)
            message = getString(KEY_message)
        }
    }

    fun toJsonObject() : JSONObject {
        return JSONObject().apply {
            put(KEY_type, type)
            put(KEY_tag, tag)
            put(KEY_message, message)
        }
    }

}