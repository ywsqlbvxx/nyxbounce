/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodess.ncp

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.features.value.FloatValue
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class BlockdropFly : FlyMode("BlockDropNCP") {

    private val hSpeedValue = FloatValue("${valuePrefix}HorizontalSpeed", 1f, 0.1f, 5f)
    private val vSpeedValue = FloatValue("${valuePrefix}VerticalSpeed", 1f, 0.1f, 5f)

    private var startX = 0.0
    private var startY = 0.0
    private var startZ = 0.0
    private var startYaw = 0f
    private var startPitch = 0f

    override fun onEnable() {
        startX = mc.thePlayer.posX
        startY = mc.thePlayer.posY
        startZ = mc.thePlayer.posZ
        startYaw = mc.thePlayer.rotationYaw
        startPitch = mc.thePlayer.rotationPitch
    }

    override fun onUpdate(event: UpdateEvent) {
        // Reset player motion
        MovementUtils.resetMotion(true)

        // Vertical movement
        if (mc.gameSettings.keyBindJump.isKeyDown) mc.thePlayer.motionY = vSpeedValue.get().toDouble()
        if (mc.gameSettings.keyBindSneak.isKeyDown) mc.thePlayer.motionY -= vSpeedValue.get().toDouble()

        // Horizontal movement
        MovementUtils.strafe(hSpeedValue.get())

        // Send packets to server to keep position
        repeat(2) {
            PacketUtils.sendPacketNoEvent(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    startX,
                    startY,
                    startZ,
                    startYaw,
                    startPitch,
                    true
                )
            )
        }

        repeat(2) {
            PacketUtils.sendPacketNoEvent(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    mc.thePlayer.posX,
                    mc.thePlayer.posY,
                    mc.thePlayer.posZ,
                    startYaw,
                    startPitch,
                    false
                )
            )
        }
    }

    override fun onPacket(event: PacketEvent) {
        when (val packet = event.packet) {
            is C03PacketPlayer -> event.cancelEvent()
            is S08PacketPlayerPosLook -> {
                startX = packet.x
                startY = packet.y
                startZ = packet.z
                startYaw = packet.yaw
                startPitch = packet.pitch
                event.cancelEvent()
            }
        }
    }
}
