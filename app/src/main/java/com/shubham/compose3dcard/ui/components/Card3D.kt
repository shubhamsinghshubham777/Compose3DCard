package com.shubham.compose3dcard.ui.components

import android.content.res.Configuration
import android.graphics.Paint
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SweepGradientShader
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.shubham.compose3dcard.ui.theme.Compose3DCardTheme

val CardRoundedCornerSize = 16.dp

// Since we want to deal with smaller angles (i.e. between -20 to 20 degrees), therefore we'll divide the incoming value by this factor.
const val OffsetSlowDownFactor = 30f

@Composable
fun Card3D() {
    Box(contentAlignment = Alignment.Center) {
        BackgroundGlowSurface()
        ForegroundClickableSurface()
    }
}

@Composable
private fun BackgroundGlowSurface() {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(2f)
            .padding(20.dp)
            .drawBehind {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        CardRoundedCornerSize.toPx(),
                        CardRoundedCornerSize.toPx(),
                        Paint().apply {
                            shader = SweepGradientShader(
                                colors = listOf(
                                    Color.Magenta,
                                    Color.Red,
                                    Color.Blue,
                                    Color.Green,
                                    Color.Yellow,
                                    Color.Cyan,
                                ),
                                center = size.center
                            )
                            setShadowLayer(
                                8f,
                                0f,
                                0f,
                                Color.Green.toArgb()
                            )
                        }
                    )
                }
            },
        color = Color.Transparent,
        content = {}
    )
}

@Composable
private fun ForegroundClickableSurface() {
    var cardCenterOffset: Offset? by remember { mutableStateOf(null) }
    var currentOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    val animatedOffset by animateOffsetAsState(targetValue = currentOffset)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .aspectRatio(2f)
            .zIndex(1f)
            .padding(22.dp)
            .onPlaced { coordinates -> cardCenterOffset = coordinates.centerOffset() }
            .graphicsLayer {
                rotationX =
                    -animatedOffset.y // Because Modifier.pointerInput gives Offset(y,x) instead of Offset(x,y)
                        .times(2)
                        .limitAngleValue()

                rotationY = animatedOffset.x.limitAngleValue()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        currentOffset = offset.centerOffsetAdjustedValue(cardCenterOffset)

                        awaitRelease()

                        currentOffset = Offset.Zero

                        detectDragGestures(
                            onDrag = { change, _ ->
                                currentOffset =
                                    change.position.centerOffsetAdjustedValue(cardCenterOffset)
                            },
                            onDragStart = { dragStartOffset ->
                                currentOffset = dragStartOffset
                            },
                            onDragEnd = {
                                currentOffset = Offset.Zero
                            },
                            onDragCancel = {
                                currentOffset = Offset.Zero
                            },
                        )
                    }
                )
            }
            .clip(RoundedCornerShape(CardRoundedCornerSize)),
        content = {}
    )
}


// These are all experimented values, no particular formulae implemented.

private fun Float.limitAngleValue(maxValue: Float = 20f) =
    coerceIn(minimumValue = -maxValue, maximumValue = maxValue)

private fun LayoutCoordinates.centerOffset() = Offset(x = size.width / 2f, y = size.height / 2f)

private fun Offset.centerOffsetAdjustedValue(centerOffset: Offset?) =
    (this - centerOffset!!) / OffsetSlowDownFactor

@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Preview(showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_NO)
@Composable
private fun Card3DPreview() {
    Compose3DCardTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
        ) {
            Card3D()
        }
    }
}