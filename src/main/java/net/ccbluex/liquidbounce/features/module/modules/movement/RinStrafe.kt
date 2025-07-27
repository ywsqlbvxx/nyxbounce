/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.utils.rotation.RotationUtils
import net.minecraft.util.MathHelper

object RinStrafe : Module("RinStrafe", Category.MOVEMENT) {

    val silentFixValue = boolean("Silent", true)

    var silentFix = false
    var doFix = false
    var isOverwrited = false

    fun getSilentFix(): Boolean {
        return silentFix
    }

    fun getDoFix(): Boolean {
        return doFix
    }

    fun onUpdate(event: UpdateEvent) {
        if (!isOverwrited) {
            silentFix = silentFixValue.get()
            doFix = state
        }
    }

    override fun onDisable() {
        doFix = false
    }

    fun applyForceStrafe(isSilent: Boolean, strafeEvent: StrafeEvent) {
        if (!doFix || strafeEvent.isCancelled) {
            return
        }
        silentFix = isSilent
        val player = mc.thePlayer ?: return
        val targetRotation = RotationUtils.targetRotation ?: return

        val yaw = targetRotation.yaw
        var modifiedStrafe = strafeEvent.strafe
        var modifiedForward = strafeEvent.forward
        var friction = strafeEvent.friction
        var factor = modifiedStrafe * modifiedStrafe + modifiedForward * modifiedForward

        var angleDiff = ((MathHelper.wrapAngleTo180_float(player.rotationYaw - yaw - 22.5f - 135.0f) + 180.0).toDouble() / 45.0).toInt()
        var calcYaw = if (isSilent) yaw + 45.0f * angleDiff else yaw

        var calcMoveDir = Math.max(Math.abs(modifiedStrafe), Math.abs(modifiedForward)).toFloat()
        calcMoveDir = calcMoveDir * calcMoveDir
        var calcMultiplier = MathHelper.sqrt_float(calcMoveDir / Math.min(1.0f, calcMoveDir * 2.0f))

        if (isSilent) {
            when (angleDiff) {
                1, 3, 5, 7, 9 -> {
                    if ((Math.abs(modifiedForward) > 0.005 || Math.abs(modifiedStrafe) > 0.005) && !(Math.abs(modifiedForward) > 0.005 && Math.abs(modifiedStrafe) > 0.005)) {
                        friction = friction / calcMultiplier
                    } else if (Math.abs(modifiedForward) > 0.005 && Math.abs(modifiedStrafe) > 0.005) {
                        friction = friction * calcMultiplier
                    }
                }
            }
        }

        if (factor >= 1.0E-4F) {
            factor = MathHelper.sqrt_float(factor)

            if (factor < 1.0F) {
                factor = 1.0F
            }

            factor = friction / factor
            modifiedStrafe *= factor
            modifiedForward *= factor

            val yawSin = MathHelper.sin((calcYaw * Math.PI / 180F).toFloat())
            val yawCos = MathHelper.cos((calcYaw * Math.PI / 180F).toFloat())

            if (!isSilent) {
                player.motionX += modifiedStrafe * yawCos - modifiedForward * yawSin
                player.motionZ += modifiedForward * yawCos + modifiedStrafe * yawSin
            }

            strafeEvent.strafe = modifiedStrafe
            strafeEvent.forward = modifiedForward
        }

        strafeEvent.cancelEvent()
    }

    fun updateOverwrite() {
        isOverwrited = false
        doFix = state
        silentFix = silentFixValue.get()
    }

    fun onStrafe(event: StrafeEvent) {
        applyForceStrafe(silentFix, event)
    }
}