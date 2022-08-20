package com.shubham.compose3dcard.ui.components

import android.content.res.Configuration
import android.graphics.Paint
import androidx.annotation.FloatRange
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.shubham.compose3dcard.ui.theme.Compose3DCardTheme

// Since we want to deal with smaller angles, therefore we'll divide the incoming value by this factor. You can increase or decrease the value to your liking.
private const val OffsetSlowDownFactor = 30f
private const val CardAspectRatio = 1.75f
private val CardRoundedCornerSize = 16.dp

@Composable
fun Card3D(
    modifier: Modifier = Modifier,
    glowOffset: Offset = Offset.Zero,
    glowRadius: Float = 16f,
    glowRoundedCornerSize: Dp = CardRoundedCornerSize,
    gradientColors: List<Color> = listOf(
        Color.Magenta,
        Color.Red,
        Color.Blue,
        Color.Green,
        Color.Yellow,
        Color.Cyan,
    ),
    glowColor: Color = Color.Green,
    animationMaxAngle: Float = 20f,
    offsetSlowDownFactor: Float = OffsetSlowDownFactor,
    @FloatRange(from = 1.0)
    cardAspectRatio: Float = CardAspectRatio,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val commonModifier = remember {
            Modifier
                .fillMaxWidth()
                .aspectRatio(cardAspectRatio)
                .padding(20.dp)
        }
        BackgroundGlowSurface(
            modifier = commonModifier,
            glowOffset = glowOffset,
            glowRadius = glowRadius,
            glowRoundedCornerSize = glowRoundedCornerSize,
            gradientColors = gradientColors,
            glowColor = glowColor,
        )
        ForegroundClickableSurface(
            modifier = commonModifier,
            maxAngle = animationMaxAngle,
            offsetSlowDownFactor = offsetSlowDownFactor,
        )
    }
}

@Composable
private fun BackgroundGlowSurface(
    modifier: Modifier = Modifier,
    glowOffset: Offset,
    glowRadius: Float,
    glowRoundedCornerSize: Dp,
    gradientColors: List<Color>,
    glowColor: Color,
) {
    Spacer(
        modifier = modifier
            .drawBehind {
                drawIntoCanvas { canvas ->
                    canvas.nativeCanvas.drawRoundRect(
                        0f,
                        0f,
                        size.width,
                        size.height,
                        glowRoundedCornerSize.toPx(),
                        glowRoundedCornerSize.toPx(),
                        Paint().apply {
                            shader = SweepGradientShader(
                                colors = gradientColors,
                                center = size.center
                            )
                            setShadowLayer(
                                glowRadius,
                                glowOffset.x,
                                glowOffset.y,
                                glowColor.toArgb()
                            )
                        }
                    )
                }
            },
    )
}

@Composable
private fun ForegroundClickableSurface(
    modifier: Modifier = Modifier,
    maxAngle: Float = 20f,
    offsetSlowDownFactor: Float,
) {
    var cardCenterOffset: Offset? by remember { mutableStateOf(null) }
    var currentOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    val animatedOffset by animateOffsetAsState(targetValue = currentOffset)

    Surface(
        modifier = modifier
            .padding(2.dp)
            .zIndex(1f)
            .onPlaced { coordinates -> cardCenterOffset = coordinates.centerOffset() }
            .graphicsLayer {
                rotationX =
                    -animatedOffset.y // Because Modifier.pointerInput gives Offset(y,x) instead of Offset(x,y)
                        .times(2)
                        .limitAngleValue(maxValue = maxAngle)

                rotationY = animatedOffset.x
                    .div(2)
                    .limitAngleValue(maxValue = maxAngle)
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = { offset ->
                        currentOffset = offset.centerOffsetAdjustedValue(
                            centerOffset = cardCenterOffset,
                            slowDownFactor = offsetSlowDownFactor
                        )

                        awaitRelease()

                        currentOffset = Offset.Zero

                        detectDragGestures(
                            onDrag = { change, _ ->
                                currentOffset =
                                    change.position.centerOffsetAdjustedValue(
                                        centerOffset = cardCenterOffset,
                                        slowDownFactor = offsetSlowDownFactor
                                    )
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

private fun Offset.centerOffsetAdjustedValue(
    centerOffset: Offset?,
    slowDownFactor: Float,
) = (this - centerOffset!!) / slowDownFactor

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