package content.area.misthalin.varrock.digsite

import world.gregs.voidps.engine.Script
import world.gregs.voidps.engine.client.message
import world.gregs.voidps.engine.entity.character.move.tele
import world.gregs.voidps.engine.entity.obj.GameObjects
import world.gregs.voidps.engine.entity.obj.ObjectShape
import world.gregs.voidps.type.Tile

class DigsiteWinches : Script {
    init {
        worldSpawn {
            GameObjects.add(
                "digsite_rope_temple",
                Tile(3177, 5730, 0),
                ObjectShape.CENTRE_PIECE_STRAIGHT,
                collision = false,
            )
        }
        objectOperate("Operate", "digsite_winch_west") { (target) ->
            if (!get("digsite_winch_rope", false)) {
                message("You need to attach a rope to the winch first.")
                return@objectOperate
            }
            tele(3368, 9762, 0)
        }

        itemOnObjectOperate("rope", "digsite_winch_west") { (target) ->
            if (get("digsite_winch_rope", false)) {
                message("There is already a rope attached to the winch.")
                return@itemOnObjectOperate
            }
            set("digsite_winch_rope", true)
            message("You attach the rope to the winch.")
        }
    }
}
