package com.evdayapps.madassistant.clientlib.adapters

import com.evdayapps.madassistant.clientlib.MADAssistantClient
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.models.NetworkCallLogModel
import okhttp3.Headers
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.http.promisesBody
import okio.Buffer
import okio.EOFException
import okio.GzipSource
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.charset.StandardCharsets.UTF_8

class MADAssistantOkHttpInterceptor(
    private val madAssistantClient: MADAssistantClient,
    private val logUtils: LogUtils? = null
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        return logApiCall(
            request = chain.request(),
            chain = chain
        )!!
    }

    @Throws(Exception::class)
    private fun logApiCall(
        request: Request,
        chain: Interceptor.Chain
    ): Response? {
        var chainException: Exception? = null

        try {
            // Read request data
            val requestHeaders = request.headers.map { it.first to it.second }.toMap()
            val requestBody = when {
                request.body == null -> null
                bodyHasUnknownEncoding(request.headers) -> "<!encoded body omitted>"
                request.body?.isDuplex() == true -> "<!duplex request body omitted>"
                request.body?.isOneShot() == true -> "<!one-shot body omitted>"
                else -> {
                    val buffer = Buffer()
                    request.body?.writeTo(buffer)

                    val contentType = request.body?.contentType()
                    val charset: Charset = contentType?.charset(StandardCharsets.UTF_8) ?: StandardCharsets.UTF_8
                    if (buffer.isProbablyUtf8()) {
                        buffer.readString(charset)
                    } else {
                        "<!byte body omitted>"
                    }
                }
            }

            // Make the call
            val requestTimestamp = System.currentTimeMillis()
            var response : Response? = null
            try {
                response = chain.proceed(request)
            } catch (ex: java.lang.Exception) {
                chainException = ex
            }
            val responseTimestamp = System.currentTimeMillis()

            // Read response data
            val responseCode = response?.code ?: -1
            val responseHeaders = response?.headers?.map { it.first to it.second }?.toMap() ?: emptyMap()
            val responseBody: String? = when {
                chainException != null -> chainException.toString()
                response?.promisesBody() != true -> null
                bodyHasUnknownEncoding(response.headers) -> "<!encoded body omitted>"
                else -> {
                    val buff = response.body?.source()?.run {
                        request(Long.MAX_VALUE)
                        this.buffer
                    }
                    buff?.let { newBuff ->
                        var buffer = newBuff
                        var gzippedLength: Long? = null
                        if ("gzip".equals(response.headers["Content-Encoding"], ignoreCase = true)) {
                            gzippedLength = buffer.size
                            GzipSource(buffer.clone()).use { gzippedResponseBody ->
                                buffer = Buffer()
                                buffer.writeAll(gzippedResponseBody)
                            }
                        }

                        val contentType = response.body?.contentType()
                        val charset: Charset = contentType?.charset(UTF_8) ?: UTF_8

                        if (buffer.isProbablyUtf8()) {
                            buffer.clone().readString(charset)
                        } else {
                            "<!binary byte body omitted>"
                        }
                    }
                }
            }

            val networkCallLogModel = NetworkCallLogModel(
                method = request.method,
                url = request.url.toString(),
                requestTimestamp = requestTimestamp,
                requestHeaders = requestHeaders,
                requestBody = requestBody,
                responseTimestamp = responseTimestamp,
                responseStatusCode = responseCode,
                responseHeaders = responseHeaders,
                responseBody = responseBody
            )
            madAssistantClient.logNetworkCall(networkCallLogModel)

            if(chainException == null) {
                return response
            }
        } catch (ex: Exception) {
            logUtils?.e(ex)
        }

        if(chainException != null) {
            throw chainException
        }

        return null
    }

    private fun bodyHasUnknownEncoding(headers: Headers): Boolean {
        val contentEncoding = headers["Content-Encoding"] ?: return false
        return !contentEncoding.equals("identity", ignoreCase = true) &&
                !contentEncoding.equals("gzip", ignoreCase = true)
    }

    /**
     * Returns true if the body in question probably contains human readable text. Uses a small
     * sample of code points to detect unicode control characters commonly used in binary file
     * signatures.
     */
    private fun Buffer.isProbablyUtf8(): Boolean {
        try {
            val prefix = Buffer()
            val byteCount = size.coerceAtMost(64)
            copyTo(prefix, 0, byteCount)
            for (i in 0 until 16) {
                if (prefix.exhausted()) {
                    break
                }
                val codePoint = prefix.readUtf8CodePoint()
                if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                    return false
                }
            }
            return true
        } catch (_: EOFException) {
            return false // Truncated UTF-8 sequence.
        }
    }

}