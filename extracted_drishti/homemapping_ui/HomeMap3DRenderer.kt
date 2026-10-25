package com.example.drishti.homemapping.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
import com.example.drishti.homemapping.model.Point3D
import com.example.drishti.homemapping.model.RoomBoundary
import com.example.drishti.ui.theme.IntelligentBlue
import kotlin.math.cos
import kotlin.math.sin

data class Camera3DState(
    var yawDeg: Float = 35f,   // Orbit rotation angle around Y axis
    var pitchDeg: Float = 30f, // Tilt angle
    var zoomScale: Float = 80f  // Scale factor
)

@Composable
fun HomeMap3DView(
    map: HomeMap,
    selectedRoomId: String? = null,
    selectedObjectId: String? = null,
    onRoomSelected: (RoomBoundary) -> Unit = {},
    onObjectSelected: (HouseholdObject) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var cameraState by remember { mutableStateOf(Camera3DState()) }
    var panOffset by remember { mutableStateOf(Offset.Zero) }

    fun project3DTo2D(point: Point3D, canvasCenter: Offset): Offset {
        val yawRad = Math.toRadians(cameraState.yawDeg.toDouble())
        val pitchRad = Math.toRadians(cameraState.pitchDeg.toDouble())

        val cosYaw = cos(yawRad).toFloat()
        val sinYaw = sin(yawRad).toFloat()
        val cosPitch = cos(pitchRad).toFloat()
        val sinPitch = sin(pitchRad).toFloat()

        // 1. Rotate around Y axis (yaw)
        val x1 = point.x * cosYaw - point.z * sinYaw
        val z1 = point.x * sinYaw + point.z * cosYaw
        val y1 = point.y

        // 2. Rotate around X axis (pitch)
        val x2 = x1
        val y2 = y1 * cosPitch - z1 * sinPitch

        // 3. Project to screen
        val screenX = canvasCenter.x + panOffset.x + (x2 * cameraState.zoomScale)
        val screenY = canvasCenter.y + panOffset.y - (y2 * cameraState.zoomScale)

        return Offset(screenX, screenY)
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    cameraState = cameraState.copy(
                        yawDeg = cameraState.yawDeg + dragAmount.x * 0.4f,
                        pitchDeg = (cameraState.pitchDeg - dragAmount.y * 0.4f).coerceIn(10f, 80f)
                    )
                }
            }
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    cameraState = cameraState.copy(
                        zoomScale = (cameraState.zoomScale * zoom).coerceIn(30f, 250f)
                    )
                    panOffset += pan
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasCenter = Offset(size.width / 2f, size.height / 2f)

            map.floors.forEach { floor ->
                floor.rooms.forEach { room ->
                    val isRoomSelected = room.roomId == selectedRoomId
                    val wallHeight = 2.4f // 2.4 meters 3D wall height
                    val wallColor = if (isRoomSelected) IntelligentBlue.copy(alpha = 0.35f) else Color(0xFF64748B).copy(alpha = 0.25f)
                    val edgeColor = if (isRoomSelected) IntelligentBlue else Color(0xFF334155)

                    if (room.polygonPoints.size >= 3) {
                        // 1. Draw 3D Floor Base Polygon
                        val floorPath = Path().apply {
                            val first2D = project3DTo2D(room.polygonPoints.first(), canvasCenter)
                            moveTo(first2D.x, first2D.y)
                            for (i in 1 until room.polygonPoints.size) {
                                val p2D = project3DTo2D(room.polygonPoints[i], canvasCenter)
                                lineTo(p2D.x, p2D.y)
                            }
                            close()
                        }
                        drawPath(path = floorPath, color = wallColor)

                        // 2. Draw Extruded 3D Walls & Top Polygon Wireframe
                        for (i in room.polygonPoints.indices) {
                            val pBottom = room.polygonPoints[i]
                            val pNextBottom = room.polygonPoints[(i + 1) % room.polygonPoints.size]

                            val pTop = pBottom.copy(y = wallHeight)
                            val pNextTop = pNextBottom.copy(y = wallHeight)

                            val b1 = project3DTo2D(pBottom, canvasCenter)
                            val b2 = project3DTo2D(pNextBottom, canvasCenter)
                            val t1 = project3DTo2D(pTop, canvasCenter)
                            val t2 = project3DTo2D(pNextTop, canvasCenter)

                            // Vertical Wall Polygon
                            val wallPath = Path().apply {
                                moveTo(b1.x, b1.y)
                                lineTo(b2.x, b2.y)
                                lineTo(t2.x, t2.y)
                                lineTo(t1.x, t1.y)
                                close()
                            }
                            drawPath(path = wallPath, color = wallColor)
                            drawPath(path = wallPath, color = edgeColor, style = Stroke(width = 2f))
                        }
                    }

                    // 3. Draw 3D Household Object Bounding Cubes
                    room.objects.forEach { obj ->
                        val isObjSelected = obj.objectId == selectedObjectId
                        val color = if (isObjSelected) Color(0xFFEF4444) else Color(0xFF2563EB)

                        val halfW = obj.dimensions.width / 2f
                        val halfD = obj.dimensions.depth / 2f
                        val objH = obj.dimensions.height

                        val posX = obj.position.x
                        val posZ = obj.position.z

                        // 8 vertices of 3D Bounding Box
                        val b0 = project3DTo2D(Point3D(posX - halfW, 0f, posZ - halfD), canvasCenter)
                        val b1 = project3DTo2D(Point3D(posX + halfW, 0f, posZ - halfD), canvasCenter)
                        val b2 = project3DTo2D(Point3D(posX + halfW, 0f, posZ + halfD), canvasCenter)
                        val b3 = project3DTo2D(Point3D(posX - halfW, 0f, posZ + halfD), canvasCenter)

                        val t0 = project3DTo2D(Point3D(posX - halfW, objH, posZ - halfD), canvasCenter)
                        val t1 = project3DTo2D(Point3D(posX + halfW, objH, posZ - halfD), canvasCenter)
                        val t2 = project3DTo2D(Point3D(posX + halfW, objH, posZ + halfD), canvasCenter)
                        val t3 = project3DTo2D(Point3D(posX - halfW, objH, posZ + halfD), canvasCenter)

                        val objPath = Path().apply {
                            moveTo(b0.x, b0.y); lineTo(b1.x, b1.y); lineTo(b2.x, b2.y); lineTo(b3.x, b3.y); close()
                            moveTo(t0.x, t0.y); lineTo(t1.x, t1.y); lineTo(t2.x, t2.y); lineTo(t3.x, t3.y); close()
                            moveTo(b0.x, b0.y); lineTo(t0.x, t0.y)
                            moveTo(b1.x, b1.y); lineTo(t1.x, t1.y)
                            moveTo(b2.x, b2.y); lineTo(t2.x, t2.y)
                            moveTo(b3.x, b3.y); lineTo(t3.x, t3.y)
                        }

                        drawPath(path = objPath, color = color.copy(alpha = 0.5f))
                        drawPath(path = objPath, color = color, style = Stroke(width = 3f))
                    }
                }
            }
        }
    }
}
