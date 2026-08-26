package com.asciicity.render

import com.badlogic.gdx.math.Vector3
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertTrue

class VehicleWheelTransformTest {
    @Test
    fun `positive forward roll moves the tyre contact point backward against travel`() {
        val contactPoint = Vector3(0f, -1f, 0f)
            .mul(VehicleWheelTransform.rotation(steeringRadians = 0.0, rollRadians = PI * 0.5))

        // A car travelling along +Z needs its bottom tyre surface moving toward -Z at contact.
        assertTrue(contactPoint.z < -0.99f)
    }

    @Test
    fun `front steering yaws the wheel before its roll`() {
        val wheelForward = Vector3(0f, 0f, 1f)
            .mul(VehicleWheelTransform.rotation(steeringRadians = PI * 0.5, rollRadians = 0.0))

        assertTrue(wheelForward.x > 0.99f)
    }

    @Test
    fun `vehicle display steering is opposite the wheel node yaw convention`() {
        val simulationSteerRadians = PI * 0.5
        val displayedForward = Vector3(0f, 0f, 1f)
            .mul(VehicleWheelTransform.frontWheelRotation(simulationSteerRadians, rollRadians = 0.0))

        assertTrue(displayedForward.x < -0.99f)
    }
}
