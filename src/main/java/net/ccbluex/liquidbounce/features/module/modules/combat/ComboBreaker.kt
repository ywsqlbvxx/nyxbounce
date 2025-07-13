package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.extensions.component1
import net.ccbluex.liquidbounce.utils.extensions.component2
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.util.MathHelper
import kotlin.random.Random

object ComboBreaker : Module("ComboBreaker", ModuleCategory.COMBAT) {
    
    private val maxComboHits = IntegerValue("MaxComboHits", 3, 0..5)
    private val jumpChance = FloatValue("JumpChance", 0.3f, 0f..1f)
    private val sidewaysChance = FloatValue("SidewaysChance", 0.7f, 0f..1f)
    private val legitStrafe = BoolValue("LegitStrafe", true)
    private val randomTiming = BoolValue("RandomTiming", true)

    private var currentCombo = 0
    private var lastHurtTime = 0
    private var shouldEvade = false
    private var evadeTimer = 0
    private var lastEvadeDirection = 0

    @EventTarget
    fun onUpdate(event: UpdateEvent) { 
        val thePlayer = mc.thePlayer ?: return

        if (thePlayer.hurtTime == 0 && lastHurtTime > 0) {
            currentCombo = 0
            shouldEvade = false
            evadeTimer = 0
        }

        lastHurtTime = thePlayer.hurtTime

        if (thePlayer.hurtTime > 0 && !thePlayer.isDead) {
            if (thePlayer.hurtTime == 9) { 
                currentCombo++
                
                if (currentCombo >= maxComboHits) {
                    shouldEvade = true
                    evadeTimer = if (randomTiming) Random.nextInt(5, 10) else 7
                    
                    lastEvadeDirection = if (Random.nextFloat() < 0.5f) 1 else -1
                }
            }

            if (shouldEvade && evadeTimer > 0) {
                if (thePlayer.onGround && Random.nextFloat() < jumpChance.get()) {
                    thePlayer.jump()
                }

                if (Random.nextFloat() < sidewaysChance.get()) {
                    if (legitStrafe.get()) {
                        val strafeSpeed = thePlayer.motionX * thePlayer.motionX + thePlayer.motionZ * thePlayer.motionZ
                        val speed = MathHelper.sqrt_double(strafeSpeed) * 0.7f
                        thePlayer.motionX = -lastEvadeDirection * speed * Random.nextFloat()
                        thePlayer.motionZ = speed * (0.5f + Random.nextFloat() * 0.5f)
                    } else {
                        val speed = 0.2f
                        val yaw = thePlayer.rotationYaw * 0.017453292f
                        thePlayer.motionX -= MathHelper.sin(yaw) * speed
                        thePlayer.motionZ += MathHelper.cos(yaw) * speed
                    }
                }

                evadeTimer--
                if (evadeTimer <= 0) {
                    shouldEvade = false
                    currentCombo = 0
                }
            }
        }
    }
}