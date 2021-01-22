package com.evdayapps.madassistant.clientlib.utils

interface LogUtils {

    fun i(tag: String, message: String)

    fun d(tag: String, message: String)

    fun e(throwable: Throwable)

}