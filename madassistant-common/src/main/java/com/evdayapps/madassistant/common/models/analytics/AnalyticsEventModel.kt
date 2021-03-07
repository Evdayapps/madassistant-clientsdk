package com.evdayapps.madassistant.common.models.analytics

import org.json.JSONObject

class AnalyticsEventModel {

    companion object {
        private const val KEY_destination = "destination"
        private const val KEY_name = "name"
        private const val KEY_parameters = "parameters"
    }

    val destination : String
    val name : String
    val parameters : JSONObject

    constructor(destination: String, name: String, params: Map<String, Any?>) {
        this.destination = destination
        this.name = name
        this.parameters = JSONObject().apply {
            params.forEach {
                put(it.key, it.value)
            }
        }
    }

    @Throws(Exception::class)
    constructor(json: String) {
        JSONObject(json).run {
            destination = getString(KEY_destination)
            name = getString(KEY_name)
            parameters = getJSONObject(KEY_parameters)
        }
    }

    fun toJsonObject() : JSONObject {
        return JSONObject().apply {
            put(KEY_name, name)
            put(KEY_destination, destination)
            put(KEY_parameters, parameters)
        }
    }

}