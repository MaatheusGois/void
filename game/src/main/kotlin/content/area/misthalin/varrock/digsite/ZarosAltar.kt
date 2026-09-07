package content.area.misthalin.varrock.digsite

import content.skill.prayer.PrayerConfigs
import content.skill.prayer.getActivePrayerVarKey
import content.skill.prayer.isCurses
import world.gregs.voidps.engine.Script
import world.gregs.voidps.engine.client.message
import world.gregs.voidps.engine.entity.character.player.Player
import world.gregs.voidps.engine.entity.character.player.skill.Skill

class ZarosAltar : Script {
    init {
        objectOperate("Pray", "altar_of_zaros") {
            pray()
        }

        objectOperate("Pray-at", "altar_of_zaros") {
            pray()
        }

        objectOperate("Switch-prayer-book", "altar_of_zaros") {
            switchPrayerBook()
        }
    }

    private fun Player.pray() {
        levels.restore(Skill.Prayer)
        levels.boost(Skill.Prayer, multiplier = 0.15)
        anim("altar_pray")
        message("You recharge your Prayer points and feel the power of Zaros flow through you.")
    }

    private fun Player.switchPrayerBook() {
        clear(getActivePrayerVarKey())
        this[PrayerConfigs.PRAYERS] = if (isCurses()) "normal" else "curses"
        message(if (isCurses()) "You switch to the Ancient Curses." else "You switch to the normal prayers.")
    }
}
