package world.gregs.voidps.network.login.protocol.decode

import kotlinx.io.Source
import world.gregs.voidps.network.client.Instruction
import world.gregs.voidps.network.client.instruction.InteractNPC
import world.gregs.voidps.network.login.protocol.*

class NPCOption2Decoder : Decoder(3) {

    override suspend fun decode(packet: Source): Instruction {
        val run = packet.g1Alt1() == 1
        val npcIndex = packet.g2Alt2()
        return InteractNPC(npcIndex, 2)
    }

}