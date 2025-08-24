/*
 * FDPClient Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge by LiquidBounce.
 * https://github.com/SkidderMC/FDPClient/
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.StrafeEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.minecraft.util.MathHelper

object StrafeFix : Module(
    name = "StrafeFix",
    category = Category.MOVEMENT,
    spacedName = "Strafe Fix"
) {

    private val silentFixValue = BoolValue("Silent", true)

    private var silentFix = false
    private var doFix = false
    private var isOverwrited = false

    @EventTarget
    private fun onUpdate(event: UpdateEvent) {
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

    fun runStrafeFixLoop(isSilent: Boolean, event: StrafeEvent) {
        if (!doFix || event.isCancelled) return

        val targetYaw = RotationUtils.targetRotation?.yaw ?: return
        var strafe = event.strafe
        var forward = event.forward
        var friction = event.friction
        var factor = strafe * strafe + forward * forward

        val angleDiff = (((MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw - targetYaw - 22.5f - 135f) + 180.0) / 45.0).toInt())
        val calcYaw = if (isSilent) targetYaw + 45f * angleDiff else targetYaw

        var calcMoveDir = maxOf(Math.abs(strafe), Math.abs(forward))
        calcMoveDir *= calcMoveDir
        val calcMultiplier = MathHelper.sqrt_float(calcMoveDir / minOf(1.0f, calcMoveDir * 2.0f))

        if (isSilent) {
            when (angleDiff) {
                1, 3, 5, 7, 9 -> {
                    if ((Math.abs(forward) > 0.005f || Math.abs(strafe) > 0.005f) &&
                        !(Math.abs(forward) > 0.005f && Math.abs(strafe) > 0.005f)
                    ) {
                        friction /= calcMultiplier
                    } else if (Math.abs(forward) > 0.005f && Math.abs(strafe) > 0.005f) {
                        friction *= calcMultiplier
                    }
                }
            }
        }

        if (factor >= 1.0E-4F) {
            factor = MathHelper.sqrt_float(factor)
            factor = if (factor < 1.0F) 1.0F else factor
            factor = friction / factor

            strafe *= factor
            forward *= factor

            val yawSin = MathHelper.sin((calcYaw * Math.PI / 180F).toFloat())
            val yawCos = MathHelper.cos((calcYaw * Math.PI / 180F).toFloat())

            mc.thePlayer.motionX += strafe * yawCos - forward * yawSin
            mc.thePlayer.motionZ += forward * yawCos + strafe * yawSin
        }

        event.cancelEvent()
    }
}
