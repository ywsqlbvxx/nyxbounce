package net.ccbluex.liquidbounce.utils

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.Packet
import net.minecraft.util.Vec3

object BlinkUtils {
    private val packetBuffer = mutableListOf<Packet<*>>()
    private val positions = mutableListOf<Vec3>()
    private var isBlinking = false
    private var packetsReceived = 0

    fun blink(packet: Packet<*>, event: PacketEvent, shouldCancel: Boolean, countAsReceived: Boolean) {
        if (shouldCancel) {
            event.cancelEvent()
            packetBuffer.add(packet)
            isBlinking = true
        }
        if (countAsReceived) {
            packetsReceived++
        }
    }

    fun unblink() {
        packetBuffer.forEach { sendPacket(it) }
        packetBuffer.clear()
        positions.clear()
        packetsReceived = 0
        isBlinking = false
    }

    fun syncReceived() {
        if (!isBlinking) return
        packetsReceived = 0
    }
}
