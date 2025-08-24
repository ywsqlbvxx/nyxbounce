/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.util.MathHelper

object StrafeFix : Module(name = "StrafeFix", category = ModuleCategory.MOVEMENT, spacedName = "Strafe Fix") {

    private val silentFixValue = BoolValue("Silent", true)

    var silentFix = false
    var doFix = false
    var isOverwrited = false

    override fun onUpdate(event: UpdateEvent) {
        if (!isOverwrited) {
            silentFix = silentFixValue.get()
            doFix = state
        }
    }

    override fun onDisable() {
        doFix = false
    }

    fun applyForceStrafe(isSilent: Boolean, runStrafeFix: Boolean) {
        silentFix = isSilent
        doFix = runStrafeFix
        isOverwrited = true
    }

    fun updateOverwrite() {
        isOverwrited = false
        doFix = state
        silentFix = silentFixValue.get()
    }

    fun runStrafeFixLoop(event: StrafeEvent) {
        if (!doFix || event.isCancelled) return

        val targetRotation = RotationUtils.targetRotation ?: return
        val yaw = if (silentFix) targetRotation.yaw else mc.thePlayer.rotationYaw
        var strafe = event.strafe
        var forward = event.forward
        var friction = event.friction

        val factor = strafe * strafe + forward * forward
        var angleDiff = ((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetRotation.yaw - 22.5f - 135.0f) + 180.0) / 45.0).toInt()
        val calcYaw = if (silentFix) yaw + 45.0f * angleDiff.toFloat() else yaw

        var calcMoveDir = Math.max(Math.abs(strafe), Math.abs(forward)).toFloat()
        calcMoveDir *= calcMoveDir
        val calcMultiplier = MathHelper.sqrt_float(calcMoveDir / Math.min(1.0f, calcMoveDir * 2.0f))

        if (silentFix) {
            when (angleDiff) {
                1, 3, 5, 7, 9 -> {
                    if ((Math.abs(forward) > 0.005 || Math.abs(strafe) > 0.005) &&
                        !(Math.abs(forward) > 0.005 && Math.abs(strafe) > 0.005)) {
                        friction /= calcMultiplier
                    } else if (Math.abs(forward) > 0.005 && Math.abs(strafe) > 0.005) {
                        friction *= calcMultiplier
                    }
                }
            }
        }

        if (factor >= 1.0E-4F) {
            var adjustedFactor = MathHelper.sqrt_float(factor)
            if (adjustedFactor < 1.0F) adjustedFactor = 1.0F
            adjustedFactor = friction / adjustedFactor

            strafe *= adjustedFactor
            forward *= adjustedFactor

            val yawSin = MathHelper.sin((calcYaw * Math.PI / 180F).toFloat())
            val yawCos = MathHelper.cos((calcYaw * Math.PI / 180F).toFloat())

            mc.thePlayer.motionX += strafe * yawCos - forward * yawSin
            mc.thePlayer.motionZ += forward * yawCos + strafe * yawSin
        }

        event.cancelEvent()
    }
}

