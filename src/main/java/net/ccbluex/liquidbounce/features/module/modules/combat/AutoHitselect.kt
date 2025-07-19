/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MathHelper

object AutoHitselect : Module("AutoHitselect", Category.COMBAT) {

    var maxWaitTime = 500L
    var range = 8f
    var maxAngle = 120f
    var resetTime = 250L
    var clickDelay = 100L
    var distance = 3f
    var wTap = true
    var debug = false

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

    fun onAttack(event: AttackEvent) {
        if (event.targetEntity !is EntityLivingBase) return
        target = event.targetEntity as EntityLivingBase
    }

    fun onUpdate() {

        val thePlayer = mc.thePlayer ?: return
        val target = target ?: run {
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
            clickTimer.reset()
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

    fun onMotion() {
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
