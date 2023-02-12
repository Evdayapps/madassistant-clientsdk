package com.evdayapps.madassistant.clientlib.utils

interface Logger {

    fun i(tag: String, message: String)

    fun v(tag: String, message: String)

    fun d(tag: String, message: String)

    fun w(tag: String, message: String)

    fun e(throwable: Throwable)
}