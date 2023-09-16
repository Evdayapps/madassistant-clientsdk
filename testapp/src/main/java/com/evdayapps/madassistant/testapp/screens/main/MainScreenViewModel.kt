package com.evdayapps.madassistant.testapp.screens.main

import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.evdayapps.madassistant.clientlib.MADAssistantClient

@Suppress("KotlinConstantConditions")
class MainScreenViewModel(
    private val madAssistantClient: MADAssistantClient
) : ViewModel() {

    data class NetworkCallConfig(
        val url: String,
        val headers: List<Parameter<Any>>
    )

    data class AnalyticsConfig(
        val destination: String,
        val eventName: String,
        val parameters: List<Parameter<Any>>
    )

    data class LogConfig(
        val type: Int,
        val tag: String,
        val message: String,
        val parameters: List<Parameter<Any>>
    )

    data class ExceptionConfig(
        val message: String,
        val data: List<Parameter<Any>>
    )

    val networkCallConfig: MutableState<NetworkCallConfig> = mutableStateOf(
        NetworkCallConfig(
            url = "https://api.github.com/users/google/repos",
            headers = listOf(
                Parameter(
                    key = "content-type",
                    value = "application-data/json",
                    inEditing = false
                ),
                Parameter(key = "password", value = "2423dfsf5$232", inEditing = false)
            )
        )
    )

    val analyticsConfig = mutableStateOf(
        AnalyticsConfig(
            destination = "MyAnalytics", "Open Screen", listOf(
                Parameter(key = "screenName", value = "main"),
                Parameter(key = "source", value = "organic")
            )
        )
    )

    val logsConfig = mutableStateOf(
        LogConfig(
            type = Log.INFO,
            tag = "MainScreen",
            message = "Just checking in",
            parameters = listOf(
                Parameter(key = "area", value = "World"),
                Parameter(key = "param2", value = "value2")
            )
        )
    )

    val exceptionConfig = mutableStateOf(
        ExceptionConfig(
            message = "Test error occured",
            data = listOf(
                Parameter(key = "area", value = "World"),
                Parameter(key = "param2", value = "value2")
            )
        )
    )

    fun testApiCall() {
//        viewModelScope.launch(Dispatchers.IO) {
//            try {
//                val client = OkHttpClient.Builder()
//                    .addInterceptor(
//                        MADAssistantOkHttp3Interceptor(client = madAssistantClient)
//                    )
//                    .build()
//
//                // Make the call
//                val bldr = Request.Builder().url(networkCallConfig.value.url)
//                networkCallConfig.value.headers.forEach {
//                    bldr.addHeader(it.key, it.value as String)
//                }
//
//                client.newCall(bldr.build()).execute()
//            } catch (ex: Exception) {
//                madAssistantClient.logException(ex)
//            }
//        }
    }


    fun testCrashReport() {
        println(""[4])
    }

    fun testAnalytics() {
        madAssistantClient.logAnalyticsEvent(
            destination = analyticsConfig.value.destination,
            eventName = analyticsConfig.value.eventName,
            data = analyticsConfig.value.parameters.associate { it.key to it.value }
        )
    }

    fun testGenericLog() {
        madAssistantClient.logGenericLog(
            type = logsConfig.value.type,
            tag = logsConfig.value.tag,
            message = logsConfig.value.message,
            data = logsConfig.value.parameters.associate { it.key to it.value }
        )
    }

    fun testNonFatalException() {
        try {
            println(""[4])
        } catch (ex: Exception) {
            madAssistantClient.logException(
                throwable = ex,
                message = exceptionConfig.value.message,
                data = exceptionConfig.value.data.associate { it.key to it.value }
            )
        }
    }

}