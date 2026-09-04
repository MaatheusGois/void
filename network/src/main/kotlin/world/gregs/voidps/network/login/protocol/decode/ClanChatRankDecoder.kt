package world.gregs.voidps.network.login.protocol.decode

import kotlinx.io.Source
import world.gregs.voidps.network.client.Instruction
import world.gregs.voidps.network.client.instruction.ClanChatRank
import world.gregs.voidps.network.login.protocol.Decoder
import world.gregs.voidps.network.login.protocol.g1Alt3
import world.gregs.voidps.network.login.protocol.readString

class ClanChatRankDecoder : Decoder(BYTE) {

    override suspend fun decode(packet: Source): Instruction {
        val rank = packet.g1Alt3()
        val name = packet.readString()
        return ClanChatRank(name, rank)
    }

}