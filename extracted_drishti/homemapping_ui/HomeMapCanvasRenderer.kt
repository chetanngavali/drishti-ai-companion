package com.example.drishti.homemapping.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import com.example.drishti.homemapping.model.HomeMap
import com.example.drishti.homemapping.model.HouseholdObject
import com.example.drishti.homemapping.model.RoomBoundary
import com.example.drishti.ui.theme.IntelligentBlue

@Composable
fun HomeMapCanvas2D(
    map: HomeMap,
    selectedRoomId: String? = null,
    selectedObjectId: String? = null,
    onRoomSelected: (RoomBoundary) -> Unit = {},
    onObjectSelected: (HouseholdObject) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(100f) } // Pixels per meter
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(40f, 300f)
                    offset += pan
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f) + offset

            map.floors.forEach { floor ->
                floor.rooms.forEach { room ->
                    val isRoomSelected = room.roomId == selectedRoomId
                    val roomColor = if (isRoomSelected) IntelligentBlue.copy(alpha = 0.25f) else Color(0xFFE2E8F0).copy(alpha = 0.4f)
                    val strokeColor = if (isRoomSelected) IntelligentBlue else Color(0xFF475569)

                    // Draw room polygon
                    if (room.polygonPoints.size >= 3) {
                        val path = Path().apply {
                            val first = room.polygonPoints.first()
                            moveTo(center.x + first.x * scale, center.y + first.z * scale)
                            for (i in 1 until room.polygonPoints.size) {
                                val p = room.polygonPoints[i]
                                lineTo(center.x + p.x * scale, center.y + p.z * scale)
                            }
                            close()
                        }
                        drawPath(path = path, color = roomColor)
                        drawPath(path = path, color = strokeColor, style = Stroke(width = 4f))
                    }

                    // Draw doorways
                    room.doorways.forEach { door ->
                        val startX = center.x + door.startPoint.x * scale
                        val startY = center.y + door.startPoint.z * scale
                        val endX = center.x + door.endPoint.x * scale
                        val endY = center.y + door.endPoint.z * scale
                        drawLine(
                            color = Color(0xFF0EA5E9), // Cyan doorway arch line
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = 8f
                        )
                    }

                    // Draw household objects
                    room.objects.forEach { obj ->
                        val isObjSelected = obj.objectId == selectedObjectId
                        val objX = center.x + obj.position.x * scale
                        val objY = center.y + obj.position.z * scale
                        val widthPx = (obj.dimensions.width * scale).coerceAtLeast(16f)
                        val depthPx = (obj.dimensions.depth * scale).coerceAtLeast(16f)

                        val objColor = if (isObjSelected) Color(0xFFEF4444) else Color(0xFF2563EB)

                        drawRect(
                            color = objColor.copy(alpha = 0.7f),
                            topLeft = Offset(objX - widthPx / 2f, objY - depthPx / 2f),
                            size = androidx.compose.ui.geometry.Size(widthPx, depthPx)
                        )
                        drawRect(
                            color = objColor,
                            topLeft = Offset(objX - widthPx / 2f, objY - depthPx / 2f),
                            size = androidx.compose.ui.geometry.Size(widthPx, depthPx),
                            style = Stroke(width = 2f)
                        )

                        // Measurement uncertainty circle overlay
                        if (obj.measurementUncertaintyMeters > 0.05f) {
                            drawCircle(
                                color = Color.Yellow.copy(alpha = 0.2f),
                                radius = obj.measurementUncertaintyMeters * scale,
                                center = Offset(objX, objY)
                            )
                        }
                    }
                }
            }
        }
    }
}
