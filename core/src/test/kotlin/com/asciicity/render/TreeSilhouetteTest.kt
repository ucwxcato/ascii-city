package com.asciicity.render

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TreeSilhouetteTest {
    @Test
    fun `tree has a narrow trunk and open lower sides`() {
        assertEquals(TreeSilhouette.Part.TRUNK, TreeSilhouette.partAt(0.5, 1.0, 8.0))
        assertNull(TreeSilhouette.partAt(0.2, 1.0, 8.0))
    }

    @Test
    fun `canopy broadens through the middle and tapers at the crown`() {
        assertEquals(TreeSilhouette.Part.FOLIAGE, TreeSilhouette.partAt(0.1, 4.5, 8.0))
        assertEquals(TreeSilhouette.Part.FOLIAGE, TreeSilhouette.partAt(0.5, 7.7, 8.0))
        assertNull(TreeSilhouette.partAt(0.2, 7.7, 8.0))
    }

    @Test
    fun `pine has a low broad cone that narrows to a pointed crown`() {
        assertEquals(TreeSilhouette.Part.FOLIAGE, TreeSilhouette.partAt(0.08, 1.5, 10.0, pine = true))
        assertNull(TreeSilhouette.partAt(0.08, 8.5, 10.0, pine = true))
        assertEquals(TreeSilhouette.Part.FOLIAGE, TreeSilhouette.partAt(0.5, 9.8, 10.0, pine = true))
        assertEquals(TreeSilhouette.Part.TRUNK, TreeSilhouette.partAt(0.5, 0.5, 10.0, pine = true))
        assertNull(TreeSilhouette.partAt(0.2, 0.5, 10.0, pine = true))
    }
}
