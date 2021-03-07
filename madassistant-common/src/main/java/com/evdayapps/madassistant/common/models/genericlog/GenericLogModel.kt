package com.evdayapps.madassistant.common.models.genericlog

import org.json.JSONObject
import java.lang.Exception

class GenericLogModel {

    companion object {
        private const val KEY_type = "type"
        private const val KEY_tag = "tag"
        private const val KEY_message = "message"
        private const val KEY_data = "data"
    }

    val type : Int
    val tag : String
    val message : String
    val data : JSONObject?

    constructor(
        type : Int,
        tag : String,
        message : String,
        data : Map<String, Any?>? = null
    ) {
        this.tag = tag
        this.type = type
        this.message = message
        this.data = JSONObject()
        data?.let {
            JSONObject().let { js ->
                data.forEach {
                    js.put(it.key, it.value)
                }
            }
        }
    }

    @Throws(Exception::class)
    constructor(json : String) {
        JSONObject(json).apply {
            type = getInt(KEY_type)
            tag = getString(KEY_tag)
            message = getString(KEY_message)
            data = optJSONObject(KEY_data)
        }
    }

    fun toJsonObject() : JSONObject {
        return JSONObject().apply {
            put(KEY_type, type)
            put(KEY_tag, tag)
            put(KEY_message, message)
            data?.let { put(KEY_data, it) }
        }
    }

}