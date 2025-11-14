package barsikbarbosik.midimapper.ui.controls

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun RotaryKnob(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 0,
    max: Int = 127,
    offset: Int = 0
) {
    val startAngle = 120f
    val sweepAngle = 300f

    val angle = startAngle + (value - min) / (max - min).toFloat() * sweepAngle

    Canvas(
        modifier
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val dx = change.position.x - center.x
                    val dy = change.position.y - center.y

                    // Angle in degrees 0..360
                    val touchAngle =
                        ((atan2(dy.toDouble(), dx.toDouble()) * 180 / Math.PI) + 360) % 360

                    // Relative angle from start
                    var relativeAngle = (touchAngle - startAngle + 360) % 360

                    if (relativeAngle > sweepAngle) {
                        // We are in the dead zone, snap to the nearest end
                        if (relativeAngle > (360 + sweepAngle) / 2f) {
                            relativeAngle = 0.0
                        } else {
                            relativeAngle = sweepAngle.toDouble()
                        }
                    }

                    // Map to value
                    val floatValue = min + (relativeAngle / sweepAngle) * (max - min)

                    onValueChange(floatValue.roundToInt().coerceIn(min, max))
                }
            }
    ) {
        val radius = min(size.width, size.height) / 2f
        val center = Offset(size.width / 2f, size.height / 2f)

        // Arc showing knob range
        drawArc(
            color = Color.LightGray,
            startAngle = startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 12f)
        )

        // Knob circle
        drawCircle(Color.Gray, radius * 0.85f, center)

        // Indicator line
        val rad = Math.toRadians(angle.toDouble())
        val indicatorLength = radius * 0.8f
        val endX = center.x + cos(rad).toFloat() * indicatorLength
        val endY = center.y + sin(rad).toFloat() * indicatorLength
        drawLine(Color.LightGray, center, Offset(endX, endY), strokeWidth = 15f)

        // Value text
        drawContext.canvas.nativeCanvas.apply {
            val text = (value - offset).toString()
            val textPaint = Paint().apply {
                color = Color.White.toArgb()
                textAlign = Paint.Align.CENTER
                textSize = radius * 0.4f
            }

            drawText(
                text,
                center.x,
                center.y + radius * 0.5f,
                textPaint
            )
        }
    }
}
