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


    override fun onEnable() {
    }

    override fun onDisable() {
        doFix = false
    }

    fun onUpdate(event: UpdateEvent) {
        if (!isOverwrited) {
            silentFix = silentFixValue.get()
            doFix = state
        }
    }
    fun applyForceStrafe(isSilent: Boolean, runStrafeFix: Boolean) {
        silentFix = isSilent
        doFix = runStrafeFix
        isOverwrited = true
    }
    fun updateOverwrite() {
        isOverwrited = false
        doFix = state
        silentFix = silentFixVaule.get()
    }
    fun runStrafeFixLoop(isSilent: Boolean, event: StrafeEvent) {
        if (!doFix || event.isCancelled) {
            return
        }
        silentFix = isSilent
        val player = mc.thePlayer ?: return
        val targetRotation = RotationUtils.targetRotation ?: return

        val yaw = targetRotation.yaw
        var strafe = event.strafe
        var forward = event.forward
        var friction = event.friction
        var factor = strafe * strafe + forward * forward

        var angleDiff = ((MathHelper.wrapAngleTo180_float(player.rotationYaw - yaw - 22.5f - 135.0f) + 180.0).toDouble() / 45.0).toInt()
        var calcYaw = if (isSilent) yaw + 45.0f * angleDiff else yaw

        var calcMoveDir = Math.max(Math.abs(strafe), Math.abs(forward)).toFloat()
        calcMoveDir = calcMoveDir * calcMoveDir
        var calcMultiplier = MathHelper.sqrt_float(calcMoveDir / Math.min(1.0f, calcMoveDir * 2.0f))

        if (isSilent) {
            when (angleDiff) {
                1, 3, 5, 7, 9 -> {
                    if ((Math.abs(forward) > 0.005 || Math.abs(strafe) > 0.005) && !(Math.abs(forward) > 0.005 && Math.abs(strafe) > 0.005)) {
                        friction = friction / calcMultiplier
                    } else if (Math.abs(forward) > 0.005 && Math.abs(strafe) > 0.005) {
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
            strafe *= factor
            forward *= factor

            val yawSin = MathHelper.sin((calcYaw * Math.PI / 180F).toFloat())
            val yawCos = MathHelper.cos((calcYaw * Math.PI / 180F).toFloat())

            
            player.motionX += strafe * yawCos - forward * yawSin
            player.motionZ += forward * yawCos + strafe * yawSin
        }

        event.cancelEvent()
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