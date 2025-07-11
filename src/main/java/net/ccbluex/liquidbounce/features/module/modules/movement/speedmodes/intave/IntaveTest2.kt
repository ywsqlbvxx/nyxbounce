package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.intave

import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.extensions.isInLiquid
import net.ccbluex.liquidbounce.utils.extensions.isMoving
import net.ccbluex.liquidbounce.utils.extensions.tryJump
import net.minecraft.util.MathHelper
import kotlin.random.Random

object IntaveTest2 : SpeedMode("IntaveTest2") {
    private var lastRandomTick = 0
    private var randomTimer = 1.0f
    private var randomSpeedInAir = 0.02f

    override fun onUpdate() {
        val thePlayer = mc.thePlayer ?: return

        if (!thePlayer.isMoving || thePlayer.isInLiquid || thePlayer.isOnLadder || thePlayer.isRiding)
            return

        val moveYaw = Math.toDegrees(Math.atan2(thePlayer.motionZ, thePlayer.motionX)).toFloat() - 90f
        val yawDiff = MathHelper.wrapAngleTo180_float(thePlayer.rotationYaw - moveYaw).let { Math.abs(it) }

        val deltaY = Math.abs(thePlayer.posY - thePlayer.prevPosY)
        if (yawDiff > 10f || deltaY > 0.001f) {
            mc.timer.timerSpeed = 1.0f
            thePlayer.speedInAir = 0.02f
            return
        }

        val now = thePlayer.ticksExisted
        if (now - lastRandomTick > Random.nextInt(7, 14)) {
            randomTimer = when {
                Random.nextBoolean() -> 1.0f + Random.nextFloat() * 0.03f
                else -> 0.97f + Random.nextFloat() * 0.03f
            }
            randomSpeedInAir = 0.0198f + Random.nextFloat() * 0.0006f
            lastRandomTick = now
        }

        if (thePlayer.onGround) {
            thePlayer.tryJump()
            mc.timer.timerSpeed = 0.9385f * randomTimer
            thePlayer.speedInAir = 0.0201f * (randomSpeedInAir / 0.02f)
        }

        if (thePlayer.fallDistance < 2.5) {
            if (thePlayer.fallDistance > 0.7) {
                if (thePlayer.ticksExisted % 3 == 0) {
                    mc.timer.timerSpeed = 1.925f * randomTimer
                } else if (thePlayer.fallDistance < 1.25) {
                    mc.timer.timerSpeed = 1.7975f * randomTimer
                }
            }
            thePlayer.speedInAir = 0.02f * (randomSpeedInAir / 0.02f)
        }
    }
}