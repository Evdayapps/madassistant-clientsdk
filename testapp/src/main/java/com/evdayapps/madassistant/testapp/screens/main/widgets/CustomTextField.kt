package com.evdayapps.madassistant.testapp.screens.main.widgets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
@Preview
fun CustomTextField(
    modifier: Modifier = Modifier,
    value: String? = "Test",
    onStrValueChange: ((String) -> Unit)? = null,
    textField: TextFieldValue? = null,
    onValueChange: ((TextFieldValue) -> Unit)? = null,
    maxLines: Int = Int.MAX_VALUE,
    label: String? = null,
) {
    val field: TextFieldValue = textField ?: TextFieldValue(value ?: "")
    val tvValue: MutableState<TextFieldValue> = remember { mutableStateOf(field) }

    Box {
        if (tvValue.value.text.isBlank()) {
            Text(
                text = label ?: "",
                modifier = Modifier.padding(12.dp, vertical = 10.dp),
                color = Color.LightGray,
            )
        }
        BasicTextField(
            value = tvValue.value,
            textStyle = MaterialTheme.typography.body2.copy(color = MaterialTheme.colors.onBackground),
            onValueChange = { res: TextFieldValue ->
                tvValue.value = res
                onValueChange?.invoke(res) ?: onStrValueChange?.invoke(res.text)
            },
            maxLines = maxLines,
            modifier = modifier
                .fillMaxWidth()
                .background(color = Color.Transparent, shape = CircleShape)
                .border(BorderStroke(0.5.dp, color = MaterialTheme.colors.onBackground), shape = CircleShape)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}