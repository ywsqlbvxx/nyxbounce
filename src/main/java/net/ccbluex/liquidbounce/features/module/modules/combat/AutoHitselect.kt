package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MathHelper
import org.lwjgl.input.Keyboard

@ModuleInfo(name = "AutoHitselect", description = "Automatically hit selects in order to start combos.", category = ModuleCategory.COMBAT)
class AutoHitselect : Module() {

    private val maxWaitTimeValue = FloatValue("MaxWaitTime", 500f, 100f, 1000f)
    private val rangeValue = FloatValue("Range", 8f, 1f, 8f)
    private val maxAngleValue = FloatValue("MaxAngle", 120f, 30f, 180f)
    private val resetTimeValue = FloatValue("ResetTime", 250f, 100f, 500f)
    private val clickDelayValue = FloatValue("ClickDelay", 100f, 50f, 200f)
    private val distanceValue = FloatValue("Distance", 3f, 1f, 6f)
    private val wTapValue = BoolValue("WTap", true)
    private val debugValue = BoolValue("Debug", false)

    private var blockClicking = false
    private var wTap = false
    private var shouldClick = false
    private var startedCombo = false
    private var waitTimerReset = false
    private var target: EntityLivingBase? = null

    private val resetTimer = MSTimer()
    private val clickTimer = MSTimer()
    private val maxWaitTimer = MSTimer()

    override fun onEnable() {
        blockClicking = false
        startedCombo = false
        shouldClick = false
        target = null
    }

    override fun onDisable() {
        blockClicking = false
        startedCombo = false
        shouldClick = false
        target = null
        if (wTapValue.get()) {
            mc.gameSettings.keyBindForward.pressed = true
        }
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        val target = event.targetEntity as? EntityLivingBase ?: return
        this.target = target
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        val target = this.target ?: EntityUtils.getClosestEntityInRange(rangeValue.get()) ?: run {
            reset()
            return
        }

        val calcYaw = (MathHelper.atan2(mc.thePlayer.posZ - target.posZ, 
                                      mc.thePlayer.posX - target.posX) * 180.0 / Math.PI - 90.0).toFloat()
        val diffX = Math.abs(MathHelper.wrapAngleTo180_float(calcYaw - target.rotationYawHead))

        if (diffX > maxAngleValue.get()) {
            reset()
            return
        }

        val playerHurt = mc.thePlayer.hurtTime > 0
        val targetHurt = target.hurtTime > 0

        if (!playerHurt && !targetHurt) {
            if (resetTimer.hasTimePassed(resetTimeValue.get().toLong())) {
                startedCombo = false
                clickTimer.reset()
                shouldClick = false
            }
        } else {
            resetTimer.reset()
        }

        if (mc.thePlayer.getDistanceToEntityBox(target) < distanceValue.get()) {
            if (!waitTimerReset) {
                maxWaitTimer.reset()
                waitTimerReset = true
            }
        } else {
            waitTimerReset = false
        }

        if (!playerHurt && !targetHurt && maxWaitTimer.hasTimePassed(maxWaitTimeValue.get().toLong())) {
            clickTimer.time = Long.MAX_VALUE
            shouldClick = true
            startedCombo = true
        }

        if (!startedCombo) {
            if (playerHurt && !targetHurt) {
                if (clickTimer.hasTimePassed(clickDelayValue.get().toLong())) {
                    shouldClick = true
                    startedCombo = true
                    if (wTapValue.get()) wTap = true
                } else {
                    blockClicking = true
                    return
                }
            } else {
                blockClicking = true
                return
            }
        }

        blockClicking = !shouldClick && (!playerHurt || !targetHurt)

        if (debugValue.get()) {
            mc.thePlayer.addChatMessage("§8[§9§lAuto Hit Select§8] §7blockClicking=$blockClicking shouldClick=$shouldClick startedCombo=$startedCombo")
        }
    }

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (wTap && wTapValue.get()) {
            mc.gameSettings.keyBindForward.pressed = false
            wTap = false
        }
    }

    private fun reset() {
        blockClicking = false
        startedCombo = false
        shouldClick = false
        target = null
    }
}
}
