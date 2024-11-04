package com.camera.demo

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SliderView(
    modifier: Modifier = Modifier,
    name: String,
    range: ClosedFloatingPointRange<Float> = 1f..100f,
    defaultValue: Float = 1f,
    onValueChange: (progress: Float) -> Unit
) {

    val sliderProgress = remember { mutableFloatStateOf(defaultValue) }

    Column(modifier = modifier) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(fontSize = 8.sp, text = "${range.start}")
                Text(fontSize = 8.sp, text = "${range.endInclusive}")
            }

            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                text = "${name}:${sliderProgress.floatValue}",
                fontSize = 8.sp,
                textAlign = TextAlign.Center
            )
        }

//        AndroidView(
//            factory = { context ->
//                val binding = SliderViewBinding.inflate(LayoutInflater.from(context), null, false)
//                binding.root
//            },
//            modifier = Modifier.fillMaxWidth(),
//            onReset = {},
//            update = { slider ->
//                val sliderView = SliderViewBinding.bind(slider).slider
//                sliderView.value = sliderProgress.floatValue
//                sliderView.valueFrom = range.start
//                sliderView.valueTo = range.endInclusive
//                sliderView.addOnChangeListener { slider, value, fromUser ->
//                    onValueChange(value)
//                    sliderProgress.floatValue = value
//                }
//            }
//        )

        Slider(
            modifier = Modifier
                .fillMaxWidth()
                .height(25.dp),
            value = sliderProgress.floatValue,
            valueRange = range,
            onValueChange = { progress ->
                onValueChange(progress)
                sliderProgress.floatValue = progress
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
//                val shape = CircleShape
                Column {
                    Canvas(modifier = Modifier.size(20.dp)) {
                        drawCircle(color = Color.White)
                    }

//                    Spacer(
//                        modifier = Modifier
//                            .size(20.dp)
//                            .indication(
//                                interactionSource = MutableInteractionSource(),
//                                indication = null
//                            )
//                            .hoverable(interactionSource = MutableInteractionSource())
//                            .shadow(6.dp, shape, clip = false)
//                            .background(color = Color.White, shape)
//                    )
//                    Text(text = "${sliderValue.floatValue}")
                }
            }
        )
    }
}