package com.evdayapps.madassistant.clientlib.transmission

data class MessageData(
    val timestamp : Long,
    val threadName : String,
    val first : Any? = null,
    val second : Any? = null,
    val third : Any? = null,
    val fourth: Any? = null
)