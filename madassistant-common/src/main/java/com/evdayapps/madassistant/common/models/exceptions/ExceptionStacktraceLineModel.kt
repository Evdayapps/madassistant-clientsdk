package com.evdayapps.madassistant.common.models.exceptions

import org.json.JSONObject

class ExceptionStacktraceLineModel {

    val className: String
    val fileName: String
    val nativeMethod: Boolean
    val methodName: String
    val lineNumber: Int

    companion object {
        const val keyClassName = "className"
        const val keyFileName = "fileName"
        const val keyNativeMethod = "nativeMethod"
        const val keyMethodName = "methodName"
        const val keyLineNumber = "lineNumber"
    }

    constructor(element: StackTraceElement) {
        this.className = element.className
        this.fileName = element.fileName
        this.nativeMethod = element.isNativeMethod
        this.methodName = element.methodName
        this.lineNumber = element.lineNumber
    }

    @Throws(Exception::class)
    constructor(json: String) {
        JSONObject(json).apply {
            className = getString(keyClassName)
            fileName = getString(keyFileName)
            nativeMethod = getBoolean(keyNativeMethod)
            methodName = getString(keyMethodName)
            lineNumber = getInt(keyLineNumber)
        }
    }

    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(keyClassName, className)
            put(keyFileName, fileName)
            put(keyNativeMethod, nativeMethod)
            put(keyMethodName, methodName)
            put(keyLineNumber, lineNumber)
        }
    }

}