package com.evdayapps.madassistant.testapp

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.evdayapps.madassistant.adapters.okhttp3.MADAssistantOkHttp3Interceptor
import com.evdayapps.madassistant.clientlib.MADAssistantClient
import com.evdayapps.madassistant.clientlib.MADAssistantClientImpl
import com.evdayapps.madassistant.clientlib.utils.LogUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.random.Random

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
            repositorySignature = "",
            passphrase = "test",
            logUtils = logUtils
        )

        // Bind the client to the remote service
        madAssistantClient.bindToService()

        madAssistantClient.logCrashes()
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

                // Make the call
                val request = Request.Builder()
                    .url("https://api.github.com/users/google/repos")
                    .build()
                client.newCall(request).execute()
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
        val destination = listOf(
            "Destination 1",
            "Destination 2",
            "Destination 3"
        )[Random.nextInt(0, 3)]

        madAssistantClient.logAnalyticsEvent(
            destination = destination,
            eventName = "Heartbeat",
            data = mapOf(
                "timestamp" to System.currentTimeMillis(),
                "random1" to Math.random() * 2000,
                "random2" to Math.random() * 2000,
            )
        )
    }

    fun testGenericLog(view: View) {
        val type = listOf(
            Log.VERBOSE,
            Log.DEBUG,
            Log.ERROR,
            Log.INFO,
            Log.WARN
        )[Random.nextInt(from = 0, until = 5)]

        madAssistantClient.logGenericLog(
            type = type,
            tag = "TestActivity",
            message = "testGenericLog: Entered ${System.currentTimeMillis()}",
            data = mapOf(
                "Key1" to "value1",
                "Random1" to Random.nextInt() * 3000
            )
        )
    }

    fun testNonFatalException(view: View) {
        try {
            val nullString: String = ""
            println(nullString[4])
        } catch (ex: Exception) {
            madAssistantClient.logException(ex)
        }
    }
}

