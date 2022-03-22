package com.evdayapps.madassistant.clientlib.utils

import com.evdayapps.madassistant.clientlib.connection.ConnectionState

interface Logger {

    fun i(tag: String, message: String)

    fun v(tag: String, message: String)

    fun d(tag: String, message: String)

    fun e(throwable: Throwable)

    fun onConnectionStateChanged(state : ConnectionState)
}