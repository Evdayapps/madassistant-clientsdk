package com.evdayapps.madassistant.common.models.genericlog

import org.json.JSONObject
import java.lang.Exception

class GenericLogModel {

    companion object {
        private const val KEY_threadName = "threadName"
        private const val KEY_type = "type"
        private const val KEY_tag = "tag"
        private const val KEY_message = "message"
        private const val KEY_data = "data"
    }

    val threadName : String
    val type : Int
    val tag : String
    val message : String
    val data : JSONObject?

    constructor(
        threadName : String,
        type : Int,
        tag : String,
        message : String,
        data : Map<String, Any?>? = null
    ) {
        this.threadName = threadName
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
            threadName = getString(KEY_threadName)
            type = getInt(KEY_type)
            tag = getString(KEY_tag)
            message = getString(KEY_message)
            data = optJSONObject(KEY_data)
        }
    }

    fun toJsonObject() : JSONObject {
        return JSONObject().apply {
            put(KEY_threadName, threadName)
            put(KEY_type, type)
            put(KEY_tag, tag)
            put(KEY_message, message)
            data?.let { put(KEY_data, it) }
        }
    }

}