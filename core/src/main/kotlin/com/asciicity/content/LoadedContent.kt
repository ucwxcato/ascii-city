package com.asciicity.content

import com.asciicity.entity.Npc
import com.asciicity.entity.AmbientActor
import com.asciicity.ambient.AmbientEventEmitter
import com.asciicity.render.AtmosphericEmitter
import com.asciicity.render.RenderPalette
import com.asciicity.render.PresentationTuning
import com.asciicity.render.FacadeSign
import com.asciicity.traffic.TrafficCar
import com.asciicity.world.GameWorld

data class LoadedContent(
    val palette: RenderPalette,
    val presentation: PresentationTuning,
    val materials: MaterialRegistry,
    val cells: CellRegistry,
    val sprites: AsciiSpriteRegistry,
    val dialogues: DialogueRegistry,
    val metadata: MapMetadata,
    val world: GameWorld,
    val npcs: List<Npc>,
    val trafficCars: List<TrafficCar> = emptyList(),
    val facadeSigns: List<FacadeSign> = emptyList(),
    val atmosphericEmitters: List<AtmosphericEmitter> = emptyList(),
    val ambientEventEmitters: List<AmbientEventEmitter> = emptyList(),
    val ambientActors: List<AmbientActor> = emptyList(),
)
