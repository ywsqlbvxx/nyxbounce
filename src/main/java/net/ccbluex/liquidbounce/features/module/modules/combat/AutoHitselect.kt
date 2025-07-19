/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MathHelper

object AutoHitselect : Module("AutoHitselect", Category.COMBAT) {

    private val maxWaitTime by float("MaxWaitTime", 500f, 100f..1000f)
    private val range by float("Range", 8f, 1f..8f)
    private val maxAngle by float("MaxAngle", 120f, 30f..180f)
    private val resetTime by float("ResetTime", 250f, 100f..500f)
    private val clickDelay by float("ClickDelay", 100f, 50f..200f)
    private val distance by float("Distance", 3f, 1f..6f)
    private val wTap by boolean("WTap", true)
    private val debug by boolean("Debug", false)

    private var blockClicking = false
    private var isWTapping = false
    private var shouldClick = false
    private var startedCombo = false
    private var waitTimerReset = false
    private var target: EntityLivingBase? = null

    private val resetTimer = MSTimer()
    private val clickTimer = MSTimer()
    private val maxWaitTimer = MSTimer()

    override fun onDisable() {
        reset()
        if (wTap) {
            mc.gameSettings.keyBindForward.pressed = true
        }
    }

    val onAttack = handler<AttackEvent> { event ->
        this.target = event.targetEntity as? EntityLivingBase
    }

    val onUpdate = handler<UpdateEvent> {
        val target = this.target ?: EntityUtils.getClosestEntityInRange(range) ?: run {
            reset()
            return@handler
        }

        val calcYaw = (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, 
                                      mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0).toFloat()
        val diffX = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - target.rotationYawHead))

        if (diffX > maxAngle) {
            reset()
            return@handler
        }

        val playerHurt = mc.thePlayer.hurtTime > 0
        val targetHurt = target.hurtTime > 0

        if (!playerHurt && !targetHurt) {
            if (resetTimer.hasTimePassed(resetTime.toLong())) {
                startedCombo = false
                clickTimer.reset()
                shouldClick = false
            }
        } else {
            resetTimer.reset()
        }

        if (mc.thePlayer.getDistanceToEntityBox(target) < distance) {
            if (!waitTimerReset) {
                maxWaitTimer.reset()
                waitTimerReset = true
            }
        } else {
            waitTimerReset = false
        }

        if (!playerHurt && !targetHurt && maxWaitTimer.hasTimePassed(maxWaitTime.toLong())) {
            clickTimer.time = Long.MAX_VALUE
            shouldClick = true
            startedCombo = true
        }

        if (!startedCombo) {
            if (playerHurt && !targetHurt) {
                if (clickTimer.hasTimePassed(clickDelay.toLong())) {
                    shouldClick = true
                    startedCombo = true
                    if (wTap) isWTapping = true
                } else {
                    blockClicking = true
                    return@handler
                }
            } else {
                blockClicking = true
                return@handler
            }
        }

        blockClicking = !shouldClick && (!playerHurt || !targetHurt)

        if (debug) {
            chat("§8[§9§lAuto Hit Select§8] §7blockClicking=$blockClicking shouldClick=$shouldClick startedCombo=$startedCombo")
        }
        }
    }

    val onMotion = handler<MotionEvent> {
        if (isWTapping && wTap) {
            mc.gameSettings.keyBindForward.pressed = false
            isWTapping = false
        }
    }

    private fun reset() {
        blockClicking = false
        startedCombo = false
        shouldClick = false
        target = null
        isWTapping = false
    }
}
}
