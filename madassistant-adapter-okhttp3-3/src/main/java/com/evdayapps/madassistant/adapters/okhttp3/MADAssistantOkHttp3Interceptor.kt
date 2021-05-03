package com.evdayapps.madassistant.adapters.okhttp3

import com.evdayapps.madassistant.clientlib.MADAssistantClient
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.models.exceptions.ExceptionModel
import com.evdayapps.madassistant.common.models.networkcalls.NetworkCallLogModel
import okhttp3.*
import okio.Buffer
import okio.BufferedSource
import okio.GzipSource
import org.json.JSONArray
import org.json.JSONObject
import java.io.EOFException
import java.nio.charset.Charset


class MADAssistantOkHttp3Interceptor(
    private val client: MADAssistantClient,
    private val logUtils: LogUtils? = null
) : Interceptor {

    companion object {
        private val UTF8 = Charset.forName("UTF-8")
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestTimeMillis = System.currentTimeMillis()
        try {
            val response = chain.proceed(request)

            logNetworkCall(
                chain = chain,
                request = request,
                requestTimeMillis = requestTimeMillis,
                response = response,
                responseTimeMillis = System.currentTimeMillis()
            )

            return response
        } catch (ex: Exception) {
            logNetworkCall(
                chain = chain,
                request = request,
                requestTimeMillis = requestTimeMillis,
                exception = ex,
                responseTimeMillis = System.currentTimeMillis()
            )

            throw ex
        }
    }

    private fun logNetworkCall(
        chain: Interceptor.Chain,
        request: Request,
        requestTimeMillis: Long,
        response: Response? = null,
        responseTimeMillis: Long,
        exception: Exception? = null
    ) {
        try {
            val data = NetworkCallLogModel()
            data.threadName = Thread.currentThread().name

            // Base
            data.method = request.method()
            data.url = request.url().toString()
            data.protocol = chain.connection()?.protocol()?.name

            // Timeouts
            data.connectTimeoutMillis = chain.connectTimeoutMillis()
            data.readTimeoutMillis = chain.readTimeoutMillis()
            data.writeTimeoutMillis = chain.writeTimeoutMillis()

            // Request
            data.requestTimestamp = requestTimeMillis
            data.requestHeaders = request.headers().toJSONArray()
            request.body()?.let { body ->
                if (request.header("Content-Type") == null && body.contentType() != null) {
                    data.requestHeaders
                        ?.put(
                            JSONObject().apply {
                                put("Content-Type", body.contentType())
                            }
                        )
                }
                if (request.header("Content-Length") == null) {
                    data.requestHeaders
                        ?.put(
                            JSONObject().apply {
                                put("Content-Length", body.contentLength())
                            }
                        )
                }

                data.requestBody = getRequestBody(request.headers(), body)
            }

            // Response
            data.responseStatusCode = -1
            data.responseTimestamp = responseTimeMillis
            response?.let {
                data.responseHeaders = it.headers().toJSONArray()
                data.responseStatusCode = it.code()
                fillResponseBody(response, data)
            }

            // Exception
            exception?.let {
                data.exception = ExceptionModel(
                    threadName = Thread.currentThread().name,
                    throwable = it,
                    isCrash = false
                )
            }

            client.logNetworkCall(data)
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }
    }

    private fun getRequestBody(headers: Headers, body: RequestBody): String {
        return when {
            bodyHasUnknownEncoding(headers) -> "(encoded body omitted)"
            body.isDuplex -> "(duplex request body omitted)"
            body.isOneShot -> "(one-shot body omitted)"
            else -> {
                val buffer = Buffer()
                body.writeTo(buffer)

                val charset: Charset = body.contentType()?.charset(UTF8) ?: UTF8
                when {
                    isPlaintext(buffer) -> buffer.readString(charset)
                    else -> "(byte body omitted)"
                }
            }
        }
    }

    private fun fillResponseBody(response: Response, data: NetworkCallLogModel) {
        data.responseBody = when {
            bodyHasUnknownEncoding(response.headers()) -> "(encoded body omitted)"
            else -> {
                when (val responseBody = response.body()) {
                    null -> "(no response body)"
                    else -> {
                        val source: BufferedSource = responseBody.source()
                        source.request(Long.MAX_VALUE)
                        var buffer: Buffer = source.buffer
                        data.responseLength = buffer.size()

                        if ("gzip".equals(
                                response.headers().get("Content-Encoding"),
                                ignoreCase = true
                            )
                        ) {
                            data.gzippedLength = buffer.size()
                            GzipSource(buffer.clone()).use { gzippedResponseBody ->
                                buffer = Buffer()
                                buffer.writeAll(gzippedResponseBody)
                                data.responseLength = buffer.size()
                            }
                        }

                        val charset = responseBody.contentType()?.charset(UTF8) ?: UTF8

                        when {
                            !isPlaintext(buffer) -> "(byte body omitted)"
                            else -> buffer.clone().readString(charset)
                        }
                    }
                }
            }

        }
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small sample
     * of code points to detect unicode control characters commonly used in binary file signatures.
     */
    private fun isPlaintext(buffer: Buffer): Boolean {
        return try {
            val prefix = Buffer()
            val byteCount = if (buffer.size() < 64) buffer.size() else 64
            buffer.copyTo(prefix, 0, byteCount)
            for (i in 0..15) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            true
        } catch (e: EOFException) {
            false // Truncated UTF-8 sequence.
        }
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"]
        return when {
            contentEncoding == null -> false
            contentEncoding.equals("identity", ignoreCase = true) -> false
            contentEncoding.equals("gzip", ignoreCase = true) -> false
            else -> true
        }
    }

    private fun Headers.toJSONArray(): JSONArray {
        val headers = this
        return JSONArray().apply {
            for (name in headers.names()) {
                headers.values(name).forEach { value ->
                    put(JSONObject().apply { put(name, value) })
                }
            }
        }
    }

}