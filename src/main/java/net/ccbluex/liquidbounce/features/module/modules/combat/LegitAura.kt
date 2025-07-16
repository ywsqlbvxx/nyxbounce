package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.util.MathHelper
import kotlin.random.Random

@ModuleInfo(name = "LegitAura", description = "A more legitimate-looking combat assistance.", category = ModuleCategory.COMBAT)
class LegitAura : Module() {

    // Targeting options
    private val rangeValue = FloatValue("Range", 3.5f, 1f, 4.2f)
    private val fovValue = FloatValue("FOV", 90f, 30f, 180f)
    private val priorityValue = FloatValue("Priority", 0f, 0f, 2f)

    // Aim options
    private val smoothAimValue = BoolValue("SmoothAim", true)
    private val aimSpeedValue = FloatValue("AimSpeed", 7f, 1f, 10f)
    private val randomizeAimValue = BoolValue("RandomizeAim", true)
    private val aimRandomValue = FloatValue("AimRandomization", 0.8f, 0.1f, 2f)
    
    // Click options
    private val minCpsValue = IntegerValue("MinCPS", 8, 4, 12)
    private val maxCpsValue = IntegerValue("MaxCPS", 12, 6, 16)
    private val randomClickValue = BoolValue("RandomizeClicks", true)
    private val jitterValue = FloatValue("Jitter", 0.3f, 0f, 1f)

    // Other options
    private val autoBlockValue = BoolValue("AutoBlock", false)
    private val keepSprintValue = BoolValue("KeepSprint", true)

    private var target: EntityLivingBase? = null
    private var currentRotation: Rotation? = null
    private var lastAttackTime = MSTimer()
    private var clickDelay = 0L

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Reset target if it's not valid anymore
        if (target != null && !isValidTarget(target!!)) {
            target = null
            currentRotation = null
        }

        // Find new target if needed
        if (target == null) {
            target = findTarget()
        }

        target?.let { currentTarget ->
            // Calculate aim
            val targetRotation = calculateTargetRotation(currentTarget)
            
            // Apply smooth aim if enabled
            if (smoothAimValue.get()) {
                currentRotation = smoothRotation(targetRotation)
            } else {
                currentRotation = targetRotation
            }

            // Apply rotations
            currentRotation?.let { rotation ->
                // Add jitter if enabled
                val jitterRotation = addJitter(rotation)
                RotationUtils.setTargetRotation(jitterRotation)

                // Attack logic
                if (canAttack()) {
                    // Pre-attack rotation check
                    if (isRotationMatch(jitterRotation, currentTarget)) {
                        // Attack
                        attackEntity(currentTarget)
                        
                        // Update click delay
                        updateClickDelay()
                    }
                }
            }
        }
    }

    private fun findTarget(): EntityLivingBase? {
        return mc.theWorld.loadedEntityList
            .filter { it is EntityLivingBase && isValidTarget(it) }
            .minByOrNull { 
                when(priorityValue.get().toInt()) {
                    0 -> mc.thePlayer.getDistanceToEntity(it) // Distance
                    1 -> it.health // Health
                    else -> -it.hurtResistantTime // Hurt time
                }
            } as EntityLivingBase?
    }

    private fun isValidTarget(entity: Entity): Boolean {
        if (entity is EntityLivingBase && EntityUtils.isSelected(entity, true)) {
            val distance = mc.thePlayer.getDistanceToEntity(entity)
            val angle = RotationUtils.getRotationDifference(entity)
            
            return distance <= rangeValue.get() && angle <= fovValue.get() / 2
        }
        return false
    }

    private fun calculateTargetRotation(target: EntityLivingBase): Rotation {
        var boundingBox = target.entityBoundingBox

        // Add randomization to aim point
        if (randomizeAimValue.get()) {
            val randomX = (Random.nextFloat() - 0.5f) * aimRandomValue.get()
            val randomY = (Random.nextFloat() - 0.5f) * aimRandomValue.get()
            val randomZ = (Random.nextFloat() - 0.5f) * aimRandomValue.get()
            
            boundingBox = boundingBox.expand(randomX.toDouble(), randomY.toDouble(), randomZ.toDouble())
        }

        return RotationUtils.toRotation(RotationUtils.getCenter(boundingBox), true)
    }

    private fun smoothRotation(targetRotation: Rotation): Rotation {
        val currentRotation = currentRotation ?: return targetRotation
        
        val speed = aimSpeedValue.get() * 0.15f
        
        val yawDiff = RotationUtils.getRotationDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDiff = RotationUtils.getRotationDifference(targetRotation.pitch, currentRotation.pitch)
        
        val smoothYaw = currentRotation.yaw + yawDiff * speed
        val smoothPitch = currentRotation.pitch + pitchDiff * speed
        
        return Rotation(smoothYaw, smoothPitch)
    }

    private fun addJitter(rotation: Rotation): Rotation {
        if (!jitterValue.get().equals(0f)) {
            val jitterAmount = jitterValue.get()
            val yawJitter = (Random.nextFloat() - 0.5f) * jitterAmount
            val pitchJitter = (Random.nextFloat() - 0.5f) * jitterAmount
            
            return Rotation(
                rotation.yaw + yawJitter,
                MathHelper.clamp_float(rotation.pitch + pitchJitter, -90f, 90f)
            )
        }
        return rotation
    }

    private fun canAttack(): Boolean {
        return lastAttackTime.hasTimePassed(clickDelay)
    }

    private fun isRotationMatch(rotation: Rotation, target: EntityLivingBase): Boolean {
        val diff = RotationUtils.getRotationDifference(rotation, target)
        return diff <= 20f // Allow some tolerance for more natural looking gameplay
    }

    private fun attackEntity(target: EntityLivingBase) {
        // Critical hit check
        val canCrit = mc.thePlayer.fallDistance > 0.0f && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder && !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.blindness) && mc.thePlayer.ridingEntity == null

        if (keepSprintValue.get()) {
            // Keep sprint
            mc.netHandler.addToSendQueue(net.minecraft.network.play.client.C0BPacketEntityAction(mc.thePlayer, net.minecraft.network.play.client.C0BPacketEntityAction.Action.STOP_SPRINTING))
        }

        // Attack
        mc.thePlayer.swingItem()
        mc.playerController.attackEntity(mc.thePlayer, target)

        if (keepSprintValue.get()) {
            // Resume sprint
            mc.netHandler.addToSendQueue(net.minecraft.network.play.client.C0BPacketEntityAction(mc.thePlayer, net.minecraft.network.play.client.C0BPacketEntityAction.Action.START_SPRINTING))
        }

        lastAttackTime.reset()
    }

    private fun updateClickDelay() {
        if (randomClickValue.get()) {
            // Random CPS within range
            val minDelay = 1000 / maxCpsValue.get()
            val maxDelay = 1000 / minCpsValue.get()
            clickDelay = Random.nextLong(minDelay, maxDelay)
        } else {
            // Fixed average CPS
            val averageCps = (minCpsValue.get() + maxCpsValue.get()) / 2
            clickDelay = 1000 / averageCps.toLong()
        }
    }

    override fun onDisable() {
        target = null
        currentRotation = null
    }
}
