package com.evdayapps.madassistant.clientlib.utils

import java.util.regex.Pattern

fun Pattern?.matches(target : String?) : Boolean {
    return this?.matcher(target ?: "")?.matches() ?: false
}