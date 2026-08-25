package com.asciicity.render

class WorldRenderStats {
    var rayCount: Int = 0
        internal set
    var ddaSteps: Int = 0
        internal set
    var wallHits: Int = 0
        internal set
    var ceilingWrites: Int = 0
        internal set
    var entitySpriteWrites: Int = 0
        internal set
    var trafficSpriteWrites: Int = 0
        internal set
    var terminalSpriteWrites: Int = 0
        internal set
    var atmosphericWrites: Int = 0
        internal set
    var ambientEventWrites: Int = 0
        internal set
    var ambientActorSpriteWrites: Int = 0
        internal set
    var fireflyWrites: Int = 0
        internal set
    var localizedLightWrites: Int = 0
        internal set
    var filledGlyphCount: Int = 0
        internal set

    internal fun reset() {
        rayCount = 0
        ddaSteps = 0
        wallHits = 0
        ceilingWrites = 0
        entitySpriteWrites = 0
        trafficSpriteWrites = 0
        terminalSpriteWrites = 0
        atmosphericWrites = 0
        ambientEventWrites = 0
        ambientActorSpriteWrites = 0
        fireflyWrites = 0
        localizedLightWrites = 0
        filledGlyphCount = 0
    }
}
