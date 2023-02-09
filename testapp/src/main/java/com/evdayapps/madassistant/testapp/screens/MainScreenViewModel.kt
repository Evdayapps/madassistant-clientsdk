package com.evdayapps.madassistant.testapp.screens

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.evdayapps.madassistant.adapters.okhttp3.MADAssistantOkHttp3Interceptor
import com.evdayapps.madassistant.clientlib.MADAssistantClient
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlin.random.Random

@Suppress("KotlinConstantConditions")
class MainScreenViewModel(
    private val madAssistantClient: MADAssistantClient
) : ViewModel() {

    class NetworkCallConfig(
        val url: String,
        val headers: List<Triple<String, String, Boolean>>
    )



    val networkCallConfig: MutableState<NetworkCallConfig> = mutableStateOf(
        NetworkCallConfig(
            url = "https://api.github.com/users/google/repos",
            headers = listOf(
                Triple("content-type", "application-data/json", false),
                Triple("password", "2423dfsf5$232", false)
            )
        )
    )

    fun testApiCall() {
        viewModelScope.launch {
            try {
                val client = OkHttpClient.Builder()
                    .addInterceptor(
                        MADAssistantOkHttp3Interceptor(
                            client = madAssistantClient
                        )
                    )
                    .build()

                // Make the call
                val bldr = Request.Builder().url(networkCallConfig.value.url)
                networkCallConfig.value.headers.forEach {
                    bldr.addHeader(it.first, it.second)
                }

                client.newCall(bldr.build()).execute()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }



    fun testCrashReport() {
        println(""[4])
    }

    fun testAnalytics() {
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

    fun testGenericLog() {
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

    fun testNonFatalException() {
        try {
            println(""[4])
        } catch (ex: Exception) {
            madAssistantClient.logException(ex)
        }
    }

}