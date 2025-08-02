/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.test

import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.utils.movement.MovementUtils
import kotlin.random.Random

object GrimTest : FlyMode("GrimTest") {

    override fun onEnable() {
        mc.thePlayer.capabilities.isFlying = true
    }

    override fun onDisable() {
        mc.thePlayer.capabilities.isFlying = false
        mc.thePlayer.motionY = 0.0
    }

    override fun onMove(event: MoveEvent) {
        val thePlayer = mc.thePlayer ?: return
        val tick = thePlayer.ticksExisted

        val randomOffset = if (tick % 2 == 0) {
            Random.nextDouble(1.0E-10, 1.0E-5)
        } else {
            -Random.nextDouble(1.0E-10, 1.0E-5)
        }

        event.y += 1.0E-5 + randomOffset

        mc.thePlayer.motionY = 0.0
    }

    override fun onUpdate() {
        mc.thePlayer?.let {
            MovementUtils.setSpeed(MovementUtils.getBaseSpeed(), Math.random() / 2000.0)
        }
    }
}