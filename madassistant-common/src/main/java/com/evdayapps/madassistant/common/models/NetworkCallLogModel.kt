package com.evdayapps.madassistant.common.models

import org.json.JSONObject

/**
 * // Request
 * val requestHeaders : Map<String></String>, String>,
 * val requestMethod : String,
 * val requestTimestamp : Long,
 * val requestUrl : String,
 * val requestBody: Map<String></String>, *>,
 * // Response
 * val responseHeaders : Map<String></String>, String>,
 * val responseStatusCode: Int,
 * val responseTimestamp : Long,
 * val response : String
 */
class NetworkCallLogModel {

    var method: String? = null
    var url: String? = null
    var requestHeaders: JSONObject? = null
    var requestTimestamp: Long? = null
    var requestBody: String? = null
    var responseHeaders: JSONObject? = null
    var responseStatusCode: Int? = null
    var responseTimestamp: Long? = null
    var responseBody: String? = null

    companion object {
        const val KEY_METHOD = "method"
        const val KEY_URL = "url"
        const val KEY_REQUESTTIMESTAMP = "request_timestamp"
        const val KEY_REQUEST_HEADERS = "request_headers"
        const val KEY_REQUEST_BODY = "request_body"
        const val KEY_REQUEST_TIMESTAMP = "request_timestamp"
        const val KEY_RESPONSE_HEADERS = "response_headers"
        const val KEY_RESPONSE_STATUS_CODE = "response_status"
        const val KEY_RESPONSE_TIMESTAMP = "response_timestamp"
        const val KEY_RESPONSE_BODY = "response_body"
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_METHOD, method)
            put(KEY_URL, url)

            put(KEY_REQUEST_TIMESTAMP, requestTimestamp)
            put(KEY_REQUEST_HEADERS, requestHeaders)
            put(KEY_REQUEST_BODY, requestBody)

            put(KEY_RESPONSE_STATUS_CODE, responseStatusCode)
            put(KEY_RESPONSE_TIMESTAMP, responseTimestamp)
            put(KEY_RESPONSE_BODY, responseBody)
            put(KEY_RESPONSE_HEADERS, responseHeaders)
        }
    }

    constructor(
        method: String,
        url: String,
        requestTimestamp: Long,
        requestHeaders: Map<String, String>? = null,
        requestBody: String? = null,
        responseTimestamp: Long,
        responseStatusCode: Int,
        responseHeaders: Map<String, String>? = null,
        responseBody: String?
    ) {
        this.method = method
        this.url = url

        this.requestTimestamp = requestTimestamp
        this.requestHeaders = JSONObject().apply {
            requestHeaders?.forEach {
                put(it.key, it.value)
            }
        }
        this.requestBody = requestBody

        this.responseStatusCode = responseStatusCode
        this.responseHeaders = JSONObject().apply {
            responseHeaders?.forEach {
                put(it.key, it.value)
            }
        }
        this.responseTimestamp = responseTimestamp
        this.responseBody = responseBody
    }

    @Throws(Exception::class)
    constructor(json: String) {
        JSONObject(json).apply {
            method = optString(KEY_METHOD, null)
            url = optString(KEY_URL, null)

            // Request
            requestTimestamp = optLong(KEY_REQUESTTIMESTAMP, 0)
            requestHeaders = getJSONObject(KEY_REQUEST_HEADERS)
            requestBody = optString(KEY_REQUEST_BODY)

            // Response
            responseTimestamp = getLong(KEY_RESPONSE_TIMESTAMP)
            responseStatusCode = getInt(KEY_RESPONSE_STATUS_CODE)
            responseHeaders = optJSONObject(KEY_RESPONSE_HEADERS)
            responseBody = optString(KEY_RESPONSE_BODY)
        }
    }

}