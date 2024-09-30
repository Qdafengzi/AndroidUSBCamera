package com.jiangdg.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableFloatState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderView(
    name: String,
    range: ClosedFloatingPointRange<Float> = 0f..1f,
    sliderValue: MutableFloatState,
    onValueChange: (progress: Float) -> Unit
) {
    Text(
        modifier = Modifier.padding(start = 10.dp),
        text = "${name}:${sliderValue.floatValue}", fontSize = 8.sp
    )
    Slider(
        modifier = Modifier
            .fillMaxWidth()
            .height(25.dp),
        value = sliderValue.floatValue,
        valueRange = range,
        onValueChange = { progress ->
            onValueChange(progress)
        },
        onValueChangeFinished = {
        },
        track = {
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .background(color = Color(0xFF0099A1), shape = RoundedCornerShape(7.dp))
            )
        },

        thumb = {
            val shape = CircleShape
            Spacer(
                modifier = Modifier
                    .size(20.dp)
                    .indication(
                        interactionSource = MutableInteractionSource(),
                        indication = null
                    )
                    .hoverable(interactionSource = MutableInteractionSource())
                    .shadow(6.dp, shape, clip = false)
                    .background(color = Color.White, shape)
            )
        }
    )
}