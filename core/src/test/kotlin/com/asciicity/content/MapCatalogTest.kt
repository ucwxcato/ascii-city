package com.asciicity.content

import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MapCatalogTest {
    @Test
    fun `playable map rotation loads every map and wraps to the default`() {
        assertEquals(MapCatalog.DEFAULT_MAP_ID, MapCatalog.playableMapIds.first())
        assertTrue(MapCatalog.playableMapIds.size > 1)
        assertEquals(MapCatalog.playableMapIds.size, MapCatalog.playableMapIds.toSet().size)

        for ((index, mapId) in MapCatalog.playableMapIds.withIndex()) {
            val content = ContentLoader().load(Path.of("."), mapId)
            assertEquals(mapId, content.world.mapId)
            assertEquals(
                MapCatalog.playableMapIds[(index + 1) % MapCatalog.playableMapIds.size],
                MapCatalog.nextAfter(mapId),
            )
        }
    }

    @Test
    fun `themed maps retain their authored semantic landmarks`() {
        val loadedWaterPark = ContentLoader().load(Path.of("."), "water-park")
        val waterPark = ContentVerifier.verify(loadedWaterPark)
        assertEquals(2, loadedWaterPark.metadata.version)
        val ceiling = requireNotNull(loadedWaterPark.metadata.ceiling)
        assertEquals(7.0, ceiling.height)
        assertEquals("ceiling.water-park", ceiling.materialId)
        assertEquals(140, waterPark.cellCounts.getValue("interior.water-park-wall"))
        assertTrue(waterPark.cellCounts.getValue("park.pond") > 100)
        assertTrue(waterPark.cellCounts.getValue("nature.bridge") > 20)
        assertTrue(waterPark.cellCounts.getValue("park.fountain") > 0)

        val natureReserve = ContentVerifier.verify(ContentLoader().load(Path.of("."), "nature-reserve"))
        assertTrue(natureReserve.cellCounts.getValue("nature.pine") > 10)
        assertTrue(natureReserve.cellCounts.getValue("nature.waterfall") > 0)
        assertTrue(natureReserve.cellCounts.getValue("nature.tent") > 0)

        val loadedCasino = ContentLoader().load(Path.of("."), "casino")
        val casino = ContentVerifier.verify(loadedCasino)
        assertEquals(1, loadedCasino.metadata.version)
        assertEquals("ceiling.casino", requireNotNull(loadedCasino.metadata.ceiling).materialId)
        assertEquals(140, casino.cellCounts.getValue("interior.casino-wall"))
        assertEquals(156, casino.cellCounts.getValue("interior.casino-slot"))
        assertEquals(16, casino.cellCounts.getValue("interior.casino-table"))
        assertEquals(6, casino.cellCounts.getValue("interior.casino-light-column"))
        assertEquals(1, casino.entityTypeCounts.getValue("terminal"))
        assertEquals(6, casino.entityTypeCounts.getValue("lamp"))
        val casinoExit = requireNotNull(loadedCasino.world.entity(com.asciicity.world.EntityId("door.casino-exit")))
        assertEquals("city-alpha", casinoExit.destinationMapId)
        assertEquals(com.asciicity.world.WorldPosition(26.5, 5.8), casinoExit.destinationPosition)
        assertEquals(180.0, casinoExit.destinationYawDegrees)
        assertEquals(1.5, casinoExit.collisionRadius)
        assertTrue(casino.reachableCellCount > 800)

    }
}
