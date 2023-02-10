package com.evdayapps.madassistant.testapp.screens.main

enum class ParameterType {
    String,
    Number,
    Boolean,
    List
}

data class Parameter<Type>(
    val type: ParameterType = ParameterType.String,
    val key : String = "",
    val value : Type,
    val inEditing : Boolean = false,
) {
    override fun toString(): String {
        return super.toString()
    }
}