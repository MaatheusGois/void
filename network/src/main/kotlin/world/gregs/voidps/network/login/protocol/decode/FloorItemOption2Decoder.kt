package world.gregs.voidps.network.login.protocol.decode

import kotlinx.io.Source
import world.gregs.voidps.network.client.Instruction
import world.gregs.voidps.network.client.instruction.InteractFloorItem
import world.gregs.voidps.network.login.protocol.*

class FloorItemOption2Decoder : Decoder(7) {

    override suspend fun decode(packet: Source): Instruction {
        val id = packet.g2Alt2()
        val run = packet.readBoolean()
        val y = packet.readShort().toInt()
        val x = packet.g2Alt1()
        return InteractFloorItem(id, x, y, 1)
    }

}