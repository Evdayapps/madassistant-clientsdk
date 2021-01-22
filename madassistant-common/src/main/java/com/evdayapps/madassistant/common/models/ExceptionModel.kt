package com.evdayapps.madassistant.common.models

import org.json.JSONArray
import org.json.JSONObject
import java.lang.reflect.InvocationTargetException

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

class ExceptionModel {

    val crash : Boolean
    val exceptionType : String?
    val description : String?
    val message: String?
    val stackTrace: List<ExceptionStacktraceLineModel>

    companion object {
        private const val KEY_ISCRASH = "ISCRASH"
        private const val KEY_TYPE = "TYPE"
        private const val KEY_DESCRIPTION = "DESCRIPTION"
        private const val KEY_MESSAGE = "MESSAGE"
        private const val KEY_STACKTRACE = "STACKTRACE"
    }

    constructor(throwable: Throwable, isCrash: Boolean) {
        var actual = when (throwable.cause) {
            is InvocationTargetException -> (throwable.cause as InvocationTargetException).targetException
            else -> throwable
        }
        actual = actual.cause ?: actual ?: throwable

        crash = isCrash
        exceptionType = actual.javaClass.simpleName
        description = actual.toString()
        message = actual.message ?: ""
        stackTrace = actual.stackTrace.map {
            ExceptionStacktraceLineModel(it)
        }
    }

    @Throws(Exception::class)
    constructor(json: String) {
        JSONObject(json).apply {
            crash = optBoolean(KEY_ISCRASH, true)
            exceptionType = getString(KEY_TYPE)
            description = getString(KEY_DESCRIPTION)
            message = getString(KEY_MESSAGE)
            stackTrace = getJSONArray(KEY_STACKTRACE).run {
                mutableListOf<ExceptionStacktraceLineModel>().apply {
                    for (i in 0 until this@run.length()) {
                        this@apply.add(ExceptionStacktraceLineModel(getString(i)))
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_ISCRASH, crash)
            put(KEY_TYPE, exceptionType)
            put(KEY_DESCRIPTION, description)
            put(KEY_MESSAGE, message)
            put(KEY_STACKTRACE, JSONArray().apply {
                stackTrace.forEach {
                    put(it.toJsonObject())
                }
            })
        }
    }


}