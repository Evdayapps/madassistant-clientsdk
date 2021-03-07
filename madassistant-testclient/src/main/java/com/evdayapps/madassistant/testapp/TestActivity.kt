package com.evdayapps.madassistant.testapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.evdayapps.madassistant.adapters.okhttp3.MADAssistantOkHttp3Interceptor
import com.evdayapps.madassistant.clientlib.MADAssistantClient
import com.evdayapps.madassistant.clientlib.MADAssistantClientImpl
import com.evdayapps.madassistant.clientlib.connection.ConnectionManagerImpl
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request

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

        madAssistantClient = MADAssistantClientImpl(
            applicationContext = applicationContext,
            connectionManager = ConnectionManagerImpl(
                applicationContext = applicationContext,
                repoKey = "",
                logUtils = logUtils
            ),
            passphrase = "test",
            logUtils = logUtils
        )

        // Bind the client to the remote service
        madAssistantClient.bindToService()

        madAssistantClient.connectExceptionHandler()
    }

    fun testApiCall(view: View) {
        GlobalScope.launch {
            try {
                val client = OkHttpClient.Builder()
                    .addInterceptor(
                        MADAssistantOkHttp3Interceptor(
                            client = madAssistantClient
                        )
                    )
                    .build()
                val request = Request.Builder()
                    .url("https://in.bookmyshow.com/api/explore/v1/discover/home")
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
            message = "testGenericLog: Entered ${System.currentTimeMillis()}",
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

