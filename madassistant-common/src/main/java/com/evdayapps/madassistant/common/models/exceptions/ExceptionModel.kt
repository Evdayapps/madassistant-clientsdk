package com.evdayapps.madassistant.common.models.exceptions

import org.json.JSONArray
import org.json.JSONObject

class ExceptionModel {

    val exceptionThreadName: String
    val crash: Boolean
    val type: String?
    val message: String?
    val stackTrace: List<ExceptionStacktraceLineModel>
    val cause: ExceptionModel?
    val threads: Map<String, List<ExceptionStacktraceLineModel>>?

    companion object {
        private const val KEY_exceptionThreadName = "exceptionThreadName"
        private const val KEY_isCrash = "isCrash"
        private const val KEY_type = "type"
        private const val KEY_message = "message"
        private const val KEY_stacktrace = "stacktrace"
        private const val KEY_cause = "cause"
        private const val KEY_threads = "threads"
    }

    /**
     * @param throwable The throwable to log
     * @param isCrash Whether this is a crash or a handled exception
     * @param nested Is this a nested model ([cause] for another exception)?
     *               if yes, wont log threads
     */
    constructor(throwable: Throwable, isCrash: Boolean, nested: Boolean = false) {
        exceptionThreadName = Thread.currentThread().name
        crash = isCrash
        type = throwable.javaClass.canonicalName
        message = throwable.message
        stackTrace = throwable.stackTrace.map { ExceptionStacktraceLineModel(it) }
        cause = throwable.cause?.run {
            ExceptionModel(
                throwable = this,
                isCrash = false,
                nested = true
            )
        }
        threads = if (!nested) {
            mutableMapOf<String, List<ExceptionStacktraceLineModel>>().apply {
                putAll(
                    Thread.getAllStackTraces().map {
                        Pair(
                            first = it.key.name,
                            second = it.value.map { ExceptionStacktraceLineModel(it) }
                        )
                    }
                )
            }
        } else null
    }

    @Throws(Exception::class)
    constructor(json: String) {
        JSONObject(json).apply {
            exceptionThreadName = getString(KEY_exceptionThreadName)
            crash = optBoolean(KEY_isCrash, true)
            type = getString(KEY_type)
            message = optString(KEY_message)
            cause = if (has(KEY_cause)) getString(KEY_cause).run { ExceptionModel(this) } else null
            threads = if (!has(KEY_threads)) null else getString(KEY_threads).run {
                try {
                    JSONArray(this).run {

                    }
                } catch (ex: Exception) {
                    //
                }
                emptyMap()
            }
            stackTrace = getJSONArray(KEY_stacktrace).run {
                mutableListOf<ExceptionStacktraceLineModel>().apply {
                    for (i in 0 until this@run.length()) {
                        add(ExceptionStacktraceLineModel(getString(i)))
                    }
                }
            }
        }
    }

    @Throws(Exception::class)
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_exceptionThreadName, exceptionThreadName)
            put(KEY_isCrash, crash)
            put(KEY_type, type)
            put(KEY_message, message)
            cause?.run { put(KEY_cause, this.toJsonObject()) }
            threads?.let {
                JSONObject().apply {
                    it.forEach {
                        put(it.key, JSONArray().apply {
                            it.value.forEach {
                                put(it.toJsonObject())
                            }
                        })
                    }
                }.let {
                    put(KEY_threads, it)
                }
            }
            put(KEY_stacktrace, JSONArray().apply {
                stackTrace.forEach {
                    put(it.toJsonObject())
                }
            })
        }
    }
}