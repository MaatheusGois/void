package world.gregs.voidps.network.login.protocol.decode

import kotlinx.io.Source
import kotlinx.io.readUByte
import world.gregs.voidps.network.client.Instruction
import world.gregs.voidps.network.client.instruction.ChatTypeChange
import world.gregs.voidps.network.login.protocol.Decoder

class ChatSetModeDecoder : Decoder(1) {

    @OptIn(ExperimentalUnsignedTypes::class)
    override suspend fun decode(packet: Source): Instruction {
        val x = packet.readUByte().toInt()
        return ChatTypeChange(x)
    }

}