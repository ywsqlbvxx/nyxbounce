package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.AttackEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.Minecraft
import kotlin.random.Random

object ComboBreaker : Module("ComboBreaker", ModuleCategory.COMBAT) {
    private val maxComboHits by IntegerValue("MaxComboHits", 3, 0..5)
    private val jumpChance by FloatValue("JumpChance", 0.3f, 0f..1f)
    private val sidewaysChance by FloatValue("SidewaysChance", 0.7f, 0f..1f)
    private val legitStrafe by BoolValue("LegitStrafe", true)
    private val randomTiming by BoolValue("RandomTiming", true)

    private var currentCombo = 0
    private var lastHurtTime = 0
    private var shouldEvade = false
    private var evadeTimer = 0
    private var lastEvadeDirection = 0

    val onUpdate = handler<UpdateEvent> { 
        val thePlayer = mc.thePlayer ?: return@handler

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
                if (thePlayer.onGround && Random.nextFloat() < jumpChance) {
                    thePlayer.jump()
                }

                if (Random.nextFloat() < sidewaysChance) {
                    if (legitStrafe) {
                        val strafeSpeed = MovementUtils.getSpeed() * 0.7f
                        thePlayer.motionX = -lastEvadeDirection * strafeSpeed * Random.nextFloat()
                        thePlayer.motionZ = strafeSpeed * (0.5f + Random.nextFloat() * 0.5f)
                    } else {
                        MovementUtils.strafe(0.2f)
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