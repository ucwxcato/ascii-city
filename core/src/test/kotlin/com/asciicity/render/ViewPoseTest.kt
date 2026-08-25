package com.asciicity.render

import com.asciicity.world.WorldPosition
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ViewPoseTest {
    @Test
    fun `yaw wraps pitch clamps and basis vectors remain orthogonal`() {
        val north = pose(yaw = 0.0)
        assertEquals(0.0, north.forwardX, 1e-12)
        assertEquals(1.0, north.forwardZ, 1e-12)
        assertEquals(1.0, north.rightX, 1e-12)
        assertEquals(0.0, north.rightZ, 1e-12)

        val wrapped = north.withLook(yawRadians = -PI / 2.0, pitchRadians = PI)
        assertEquals(PI * 1.5, wrapped.yawRadians, 1e-12)
        assertEquals(ViewPose.MAX_PITCH_RADIANS, wrapped.pitchRadians, 1e-12)
        assertEquals(0.0, wrapped.forwardX * wrapped.rightX + wrapped.forwardZ * wrapped.rightZ, 1e-12)
    }

    @Test
    fun `interpolation takes the short path through yaw wrap`() {
        val previous = pose(yaw = Math.toRadians(350.0))
        val current = pose(yaw = Math.toRadians(10.0)).copy(position = WorldPosition(3.0, 5.0))

        val middle = ViewPose.interpolate(previous, current, 0.5)

        assertTrue(middle.yawRadians < 1e-12 || middle.yawRadians > PI * 2.0 - 1e-12)
        assertEquals(WorldPosition(2.0, 3.5), middle.position)
    }

    private fun pose(yaw: Double) = ViewPose(
        position = WorldPosition(1.0, 2.0),
        eyeHeight = 0.6,
        yawRadians = yaw,
        pitchRadians = 0.0,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )
}
