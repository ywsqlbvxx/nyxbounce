package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.PacketUtils.sendPacket
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

object RemoveReduceDame : Module("RemoveReduceDame", Category.COMBAT) {
    private val delayMs by int("Delay", 40, 0..100)

    private var lastAttackTime = 0L
    private var blockReleasedAt = 0L

    init {
        handler<PacketEvent> { event ->
            val player = mc.thePlayer ?: return@handler
            val packet = event.packet

            if (packet is C02PacketUseEntity && packet.action == C02PacketUseEntity.Action.ATTACK) {
                val now = System.currentTimeMillis()

                if (now - lastAttackTime < delayMs) return@handler
                lastAttackTime = now

                if (player.isBlocking) {
                    sendPacket(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        ), false
                    )
                    blockReleasedAt = now
                }
            }
        }
    }
}
