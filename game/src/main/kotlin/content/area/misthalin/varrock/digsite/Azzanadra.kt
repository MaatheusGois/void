package content.area.misthalin.varrock.digsite

import content.entity.player.dialogue.Neutral
import content.entity.player.dialogue.type.npc
import content.entity.player.dialogue.type.player
import world.gregs.voidps.engine.Script

class Azzanadra : Script {
    init {
        npcOperate("Talk-to", "azzanadra") {
            npc<Neutral>("Greetings, adventurer. What brings you to this ancient place?")
            player<Neutral>("Who are you?")
            npc<Neutral>("I am Azzanadra, a servant of Zaros and guardian of this temple.")
            player<Neutral>("What happened to this temple?")
            npc<Neutral>("The temple was buried long ago. Its secrets must be treated with care.")
        }
    }
}
