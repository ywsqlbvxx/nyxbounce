/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodess.FlyMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import kotlin.math.cos
import kotlin.math.sin

class NCPPacketFly : FlyMode("NCPPacket") {

    private val timerValue = FloatValue("${valuePrefix}Timer", 1.1f, 1.0f, 1.3f)
    private val speedValue = FloatValue("${valuePrefix}Speed", 0.28f, 0.27f, 0.29f)

    override fun onUpdate(event: UpdateEvent) {
        // Reset player motion before calculations
        MovementUtils.resetMotion(true)

        // Calculate movement vector based on yaw
        val yawRad = Math.toRadians(mc.thePlayer.rotationYaw.toDouble())
        val xMove = -sin(yawRad) * speedValue.get()
        val zMove = cos(yawRad) * speedValue.get()

        // Apply timer speed
        mc.timer.timerSpeed = timerValue.get()

        // Send packets to server
        mc.netHandler.addToSendQueue(
            C04PacketPlayerPosition(
                mc.thePlayer.posX + xMove,
                mc.thePlayer.motionY,
                mc.thePlayer.posZ + zMove,
                false
            )
        )
        mc.netHandler.addToSendQueue(
            C04PacketPlayerPosition(
                mc.thePlayer.posX + xMove,
                mc.thePlayer.motionY - 490.0,
                mc.thePlayer.posZ + zMove,
                true
            )
        )

        // Update local player position
        mc.thePlayer.posX += xMove
        mc.thePlayer.posZ += zMove
    }
}
