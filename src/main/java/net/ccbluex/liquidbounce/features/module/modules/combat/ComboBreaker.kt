package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.Event
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.util.MathHelper
import kotlin.random.Random

object ComboBreaker : Module("ComboBreaker", Category.COMBAT) {
    
    private val maxComboHits by IntegerValue("MaxComboHits", 3, 0, 5)
    private val jumpChance by FloatValue("JumpChance", 0.3f, 0f, 1f)
    private val sidewaysChance by FloatValue("SidewaysChance", 0.7f, 0f, 1f)
    private val legitStrafe by BoolValue("LegitStrafe", true)
    private val randomTiming by BoolValue("RandomTiming", true)

    private var currentCombo = 0
    private var lastHurtTime = 0
    private var shouldEvade = false
    private var evadeTimer = 0
    private var lastEvadeDirection = 0

    override fun onEnable() {
        currentCombo = 0
        shouldEvade = false
        evadeTimer = 0
    }

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return

        // Reset combo when not being hit
        if (thePlayer.hurtTime == 0 && lastHurtTime > 0) {
            currentCombo = 0
            shouldEvade = false
            evadeTimer = 0
        }

        lastHurtTime = thePlayer.hurtTime

        if (thePlayer.hurtTime > 0 && !thePlayer.isDead) {
            // Track combo hits
            if (thePlayer.hurtTime == 9) { 
                currentCombo++
                
                // Start evading if combo exceeds limit
                if (currentCombo >= maxComboHits) {
                    shouldEvade = true
                    evadeTimer = if (randomTiming) Random.nextInt(5, 10) else 7
                    lastEvadeDirection = if (Random.nextFloat() < 0.5f) 1 else -1
                }
            }

            // Execute evasive movement
            if (shouldEvade && evadeTimer > 0) {
                // Random jumps
                if (thePlayer.onGround && Random.nextFloat() < jumpChance) {
                    thePlayer.jump()
                }

                // Sideways movement
                if (Random.nextFloat() < sidewaysChance) {
                    if (legitStrafe) {
                        // More natural strafing based on current motion
                        val strafeSpeed = thePlayer.motionX * thePlayer.motionX + thePlayer.motionZ * thePlayer.motionZ
                        val speed = MathHelper.sqrt_double(strafeSpeed) * 0.7
                        thePlayer.motionX = (-lastEvadeDirection * speed * Random.nextFloat()).toDouble()
                        thePlayer.motionZ = (speed * (0.5 + Random.nextFloat() * 0.5)).toDouble()
                    } else {
                        // Basic sideways movement
                        val speed = 0.2
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
    
    override fun handleEvents(): Boolean {
        return true
    }
}