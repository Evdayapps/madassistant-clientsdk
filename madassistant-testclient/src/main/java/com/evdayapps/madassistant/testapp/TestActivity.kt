package com.evdayapps.madassistant.testapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.evdayapps.madassistant.clientlib.adapters.MADAssistantOkHttpInterceptor
import com.evdayapps.madassistant.clientlib.client.MADAssistantClient
import com.evdayapps.madassistant.clientlib.client.MADAssistantClientImpl
import com.evdayapps.madassistant.clientlib.connection.ConnectionManagerImpl
import com.evdayapps.madassistant.clientlib.permission.PermissionManagerImpl
import com.evdayapps.madassistant.clientlib.transmission.TransmissionManagerImpl
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import com.evdayapps.madassistant.common.encryption.MADAssistantCipherImpl
import com.evdayapps.madassistant.common.models.NetworkCallLogModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import org.json.JSONObject

class TestActivity : AppCompatActivity() {

    private val TAG = "SimpleActivity"

    lateinit var madAssistantClient: MADAssistantClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_simple)

        initMADAssistant()
    }

    private fun initMADAssistant() {
        val logUtils = object : LogUtils {
            override fun i(tag: String, message: String) {
                Log.i(tag, message)
            }

            override fun d(tag: String, message: String) {
                Log.d(tag, message)
            }

            override fun e(throwable: Throwable) {
                throwable.printStackTrace()
            }
        }

        val cipher = MADAssistantCipherImpl(
            passPhrase = "Thisisatest"
        )

        val connectionManagerImpl = ConnectionManagerImpl(
            applicationContext = applicationContext,
            logUtils = logUtils
        )

        val permissionManagerImpl = PermissionManagerImpl(
            cipher = cipher,
            logUtils = logUtils
        )

        val transmissionManagerImpl = TransmissionManagerImpl(
            cipher,
            permissionManagerImpl,
            connectionManagerImpl,
            logUtils
        )

        madAssistantClient = MADAssistantClientImpl(
            applicationContext = applicationContext,
            connectionManager = connectionManagerImpl,
            permissionManager = permissionManagerImpl,
            transmitter = transmissionManagerImpl,
            logUtils = logUtils
        )

        // Bind the client to the remote service
        madAssistantClient.bindToService()

        madAssistantClient.connectExceptionHandler()
    }

    fun testApiCall2(view: View) {
        val apiCall = NetworkCallLogModel(
            method = "GET",
            url = "https://www.evdayapps.com",
            requestTimestamp = System.currentTimeMillis(),
            requestHeaders = mapOf(
                "x-auth-token" to 10.toString(),
                "x-client-id" to "ANDROID",
                "x-type" to "application/json"
            ),
            requestBody = null,
            // Response
            responseTimestamp = System.currentTimeMillis() + (Math.random() * 1000).toLong(),
            responseStatusCode = 200,
            responseHeaders = mapOf(
                "x-datacenter" to "ind-01",
                "x-content-length" to 43234.toString()
            ),
            responseBody = JSONObject(
                """{
  "data": [{
    "type": "articles",
    "id": "1",
    "attributes": {
      "title": "JSON:API paints my bikeshed!",
      "body": "The shortest article. Ever.",
      "created": "2015-05-22T14:56:29.000Z",
      "updated": "2015-05-22T14:56:28.000Z"
    },
    "relationships": {
      "author": {
        "data": {"id": "42", "type": "people"}
      }
    }
  }],
  "included": [
    {
      "type": "people",
      "id": "42",
      "attributes": {
        "name": "John",
        "age": 80,
        "gender": "male"
      }
    }
  ]
}"""
            ).toString()
        )

        madAssistantClient.logNetworkCall(apiCall)
    }

    fun testApiCall(view: View) {
        GlobalScope.launch {
            try {
                val client = OkHttpClient.Builder()
                    .addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BODY
                    })
                    .addInterceptor(MADAssistantOkHttpInterceptor(
                        madAssistantClient = madAssistantClient,
                        logUtils = object : LogUtils {
                            override fun i(tag: String, message: String) {
                                Log.i(tag, message)
                            }

                            override fun d(tag: String, message: String) {
                                Log.d(tag, message)
                            }

                            override fun e(throwable: Throwable) {
                                throwable.printStackTrace()
                            }
                        }
                    ))
                    .build()
                val request = Request.Builder()
                    .url("https://api.github.com/repos/square/okhttp/issues?state=open")
                    .build()
                val response = client.newCall(request).execute()
                Log.i("TestActivity", "response: ${response.body.toString()}")
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    fun testCrashReport(view: View) {
        val nullString: String = ""
        println(nullString[4])
    }

    fun testAnalytics(view: View) {
        madAssistantClient.logAnalyticsEvent(
            destination = "GoogleAnalytics",
            eventName = "Heartbeat",
            data = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "random1" to Math.random() * 2000,
                "random2" to Math.random() * 2000,
            )
        )
    }

    fun testGenericLog(view: View) {
        madAssistantClient.logGenericLog(
            type = Log.VERBOSE,
            tag = "TestActivity",
            message = "testGenericLog: Entered ${System.currentTimeMillis()}"
        )
    }

    fun testNonFatalException(view: View) {
        try {
            "fsdfsa".toInt()
        } catch (ex: Exception) {
            madAssistantClient.logException(ex)
        }
    }
}

