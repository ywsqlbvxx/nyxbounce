package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import kotlin.math.*
import java.util.*

object RemoveReduceDame : Module("RemoveReduceDame", Category.COMBAT) {

    private var targetYaw = 0f
    private var targetPitch = 0f
    private var shouldApplyRotation = false
    private val random = Random()

    init {
        handler<PacketEvent> { event ->
            val player = mc.thePlayer ?: return@handler
            val world = mc.theWorld ?: return@handler
            val packet = event.packet

            if (packet is C02PacketUseEntity && packet.action == C02PacketUseEntity.Action.ATTACK) {
                val target = packet.getEntityFromWorld(world) as? EntityLivingBase ?: return@handler

                val dx = target.posX - player.posX
                val dz = target.posZ - player.posZ
                val dy = (target.posY + target.eyeHeight) - (player.posY + player.eyeHeight)
                val dist = sqrt(dx * dx + dz * dz)

                val jitterYaw = (-0.5f + random.nextFloat() * 1f)
                val jitterPitch = (-0.3f + random.nextFloat() * 0.6f)

                targetYaw = Math.toDegrees(atan2(dz, dx)).toFloat() - 90f + jitterYaw
                targetPitch = -Math.toDegrees(atan2(dy, dist)).toFloat() + jitterPitch

                shouldApplyRotation = true
            }
        }

        handler<UpdateEvent> { event ->
            val player = mc.thePlayer ?: return@handler
            if (!shouldApplyRotation) return@handler

            player.rotationYaw = targetYaw
            player.rotationPitch = targetPitch

            player.rotationYawHead = targetYaw
            player.renderYawOffset = targetYaw

            shouldApplyRotation = false
        }
    }
}
