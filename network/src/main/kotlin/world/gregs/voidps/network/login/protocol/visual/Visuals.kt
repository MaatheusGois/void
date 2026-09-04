package world.gregs.voidps.network.login.protocol.visual

import world.gregs.voidps.network.login.protocol.visual.update.*

abstract class Visuals {

    var flag: Int = 0
        private set

    var walkStep: Int = -1
    var runStep: Int = -1
    var moved: Boolean = false
    var tele: Boolean = false

    val animation = Animation()
    val graphics = Array(4) { Graphic() }
    val colourOverlay = ColourOverlay()
    val exactMovement = ExactMovement()
    val timeBar = TimeBar()
    val face = Face()
    val watch = Watch()
    val say = Say()
    val hits = Hits()

    fun flag(mask: Int) {
        flag = flag or mask
    }

    fun flagged(mask: Int): Boolean = flag and mask != 0

    open fun reset() {
        walkStep = -1
        runStep = -1
        moved = false
        tele = false
        flag = 0
        animation.reset()
        graphics.forEach { it.reset() }
        exactMovement.reset()
        colourOverlay.reset()
        hits.reset()
        face.reset()
        watch.reset()
        say.reset()
        timeBar.reset()
    }
}
