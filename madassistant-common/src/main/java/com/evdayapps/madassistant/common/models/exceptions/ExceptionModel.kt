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
    constructor(
        threadName: String,
        throwable: Throwable,
        isCrash: Boolean,
        nested: Boolean = false
    ) {
        exceptionThreadName = threadName
        crash = isCrash
        type = throwable.javaClass.canonicalName
        message = throwable.message
        stackTrace = throwable.stackTrace.map { ExceptionStacktraceLineModel(it) }

        cause = throwable.cause?.run {
            ExceptionModel(
                threadName = threadName,
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
            threads = if (!has(KEY_threads)) null else getThreadsStacktrace(getString(KEY_threads))
            stackTrace = getJSONArray(KEY_stacktrace).run {
                mutableListOf<ExceptionStacktraceLineModel>().apply {
                    for (i in 0 until this@run.length()) {
                        add(ExceptionStacktraceLineModel(getString(i)))
                    }
                }
            }
        }
    }

    private fun getThreadsStacktrace(string : String) : Map<String, List<ExceptionStacktraceLineModel>> {
        try {
            val map = mutableMapOf<String , List<ExceptionStacktraceLineModel>>()
            val json = JSONObject(string)
            json.keys().forEach { key ->
                val arr = json.getJSONArray(key)
                val list = mutableListOf<ExceptionStacktraceLineModel>()
                for(i in 0 until arr.length()) {
                    arr.getString(i)
                        .run { ExceptionStacktraceLineModel(this) }
                        .let { list.add(it) }

                }
                map[key] = list
            }

            return map
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
        return mapOf()
    }

    @Throws(Exception::class)
    fun toJsonObject(): JSONObject {
        return JSONObject().apply {
            put(KEY_exceptionThreadName, exceptionThreadName)
            put(KEY_isCrash, crash)
            put(KEY_type, type)
            put(KEY_message, message)
            cause?.run { put(KEY_cause, this.toJsonObject()) }
            threads
                ?.let {
                    JSONObject()
                        .apply {
                            it.forEach {
                                put(
                                    it.key,
                                    JSONArray().apply {
                                        it.value.forEach {
                                            put(it.toJsonObject())
                                        }
                                    }
                                )
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