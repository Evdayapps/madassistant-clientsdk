package com.evdayapps.madassistant.clientlib.transmission

import com.evdayapps.madassistant.common.models.NetworkCallLogModel

interface Transmitter {
    fun logNetworkCall(data: NetworkCallLogModel)

    fun logCrashReport(throwable: Throwable)

    fun logAnalyticsEvent(destination: String, eventName: String, data: Map<String, Any?>)

    fun logGenericLog(type: Int, tag: String, message: String)

    fun logException(throwable: Throwable)
}