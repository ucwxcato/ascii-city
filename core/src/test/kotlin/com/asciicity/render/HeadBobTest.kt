package com.asciicity.render

import com.asciicity.world.WorldPosition
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class HeadBobTest {
    @Test
    fun `movement creates subtle bounded deterministic vertical motion`() {
        val first = HeadBob()
        val repeated = HeadBob()
        val firstOffsets = mutableListOf<Double>()
        val repeatedOffsets = mutableListOf<Double>()

        repeat(80) {
            first.update(distanceMoved = 0.05, deltaSeconds = 1.0 / 60.0)
            repeated.update(distanceMoved = 0.05, deltaSeconds = 1.0 / 60.0)
            firstOffsets += first.verticalOffset
            repeatedOffsets += repeated.verticalOffset
        }

        assertEquals(firstOffsets, repeatedOffsets)
        assertTrue(firstOffsets.any { it > 0.0 })
        assertTrue(firstOffsets.any { it < 0.0 })
        assertTrue(firstOffsets.all { abs(it) <= 0.015 + 1e-12 })
    }

    @Test
    fun `head bob changes only render eye height and settles after movement`() {
        val bob = HeadBob()
        val pose = testPose()

        bob.update(distanceMoved = 0.0, deltaSeconds = 1.0 / 60.0)
        assertSame(pose, bob.apply(pose))

        bob.update(distanceMoved = 0.2, deltaSeconds = 1.0 / 60.0)
        val rendered = bob.apply(pose)
        assertEquals(pose.position, rendered.position)
        assertEquals(pose.yawRadians, rendered.yawRadians)
        assertEquals(pose.pitchRadians, rendered.pitchRadians)
        assertEquals(pose.horizontalFovRadians, rendered.horizontalFovRadians)
        assertTrue(rendered.eyeHeight != pose.eyeHeight)

        repeat(20) { bob.update(distanceMoved = 0.0, deltaSeconds = 1.0 / 60.0) }
        assertEquals(0.0, bob.verticalOffset, 1e-12)
        bob.update(distanceMoved = 0.2, deltaSeconds = 1.0 / 60.0)
        bob.reset()
        assertSame(pose, bob.apply(pose))

        bob.update(distanceMoved = 0.2, deltaSeconds = 1.0 / 60.0)
        assertEquals(rendered.eyeHeight, bob.apply(pose).eyeHeight, 1e-12)
    }

    @Test
    fun `invalid head bob inputs fail early`() {
        assertFailsWith<IllegalArgumentException> { HeadBob(amplitude = -0.01) }
        assertFailsWith<IllegalArgumentException> { HeadBob(cycleDistance = 0.0) }
        assertFailsWith<IllegalArgumentException> { HeadBob(responsePerSecond = Double.NaN) }
        val bob = HeadBob()
        assertFailsWith<IllegalArgumentException> { bob.update(-0.1, 1.0 / 60.0) }
        assertFailsWith<IllegalArgumentException> { bob.update(0.1, Double.NaN) }
    }

    private fun testPose() = ViewPose(
        position = WorldPosition(4.5, 6.5),
        eyeHeight = 0.6,
        yawRadians = 0.25,
        pitchRadians = -0.1,
        horizontalFovRadians = ViewPose.DEFAULT_FOV_RADIANS,
    )
}
