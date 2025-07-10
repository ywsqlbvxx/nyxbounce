package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.ui.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.ui.utils.extensions.isMoving
import net.ccbluex.liquidbounce.ui.utils.extensions.tryJump
import net.minecraft.util.MathHelper

object IntaveTest : SpeedMode("IntaveTest") {
    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || thePlayer.isInLiquid || thePlayer.isOnLadder || thePlayer.isRiding)
            return
        val moveYaw = Math.toDegrees(Math.atan2(thePlayer.motionZ, thePlayer.motionX)).toFloat() - 90f
        var yawDiff = MathHelper.wrapAngleTo180_float(thePlayer.rotationYaw - moveYaw).let { Math.abs(it) }

        if (thePlayer.onGround) {
            thePlayer.tryJump()
            if (yawDiff < 45 || yawDiff > 315) {
                mc.timer.timerSpeed = 0.9385f
                thePlayer.speedInAir = 0.0201f
            } else if (yawDiff > 135 && yawDiff < 225) {
                mc.timer.timerSpeed = 1.0f
                thePlayer.speedInAir = 0.02f
            } else {
                mc.timer.timerSpeed = 1.13f
                thePlayer.speedInAir = 0.021f
            }
        }

        if (thePlayer.fallDistance < 2.5) {
            if (thePlayer.fallDistance > 0.7) {
                if (thePlayer.ticksExisted % 3 == 0) { if (yawDiff < 45 || yawDiff > 315) {
                        mc.timer.timerSpeed = 1.925f
                    } else {
                        mc.timer.timerSpeed = 1.13f
                    }
                } else if (thePlayer.fallDistance < 1.25) {
                    if (yawDiff < 45 || yawDiff > 315) {
                        mc.timer.timerSpeed = 1.7975f
                    } else {
                        mc.timer.timerSpeed = 1.13f
                    }
                }
            }
            thePlayer.speedInAir = if (yawDiff < 45 || yawDiff > 315) 0.02f else 0.0195f
        }
    }
}