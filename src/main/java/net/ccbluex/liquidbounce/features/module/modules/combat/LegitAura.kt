package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.Rotation
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.FloatValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.MathHelper
import kotlin.random.Random

@ModuleInfo(name = "LegitAura", description = "A more legitimate-looking combat assistance.", category = ModuleCategory.COMBAT)
class LegitAura : Module("LegitAura", ModuleCategory.COMBAT) {

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
    private var blockStatus = false

    @EventTarget
    fun onMotion(event: MotionEvent) {
        if (event.eventState == EventState.PRE) {
            // Reset target if it's not valid anymore
            if (target != null && !isValidTarget(target!!)) {
                target = null
                currentRotation = null
                blockStatus = false
            }

            // Find new target if needed
            if (target == null) {
                target = findTarget()
            }

            target?.let { currentTarget ->
                // Calculate aim
                val targetRotation = calculateTargetRotation(currentTarget)
                
                // Apply smooth aim if enabled
                currentRotation = if (smoothAimValue.get()) {
                    smoothRotation(targetRotation)
                } else {
                    targetRotation
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

                    // AutoBlock
                    if (autoBlockValue.get() && mc.thePlayer.heldItem?.item is ItemSword) {
                        if (target != null) {
                            if (!blockStatus) {
                                mc.netHandler.addToSendQueue(C08PacketPlayerBlockPlacement(mc.thePlayer.inventory.getCurrentItem()))
                                blockStatus = true
                            }
                        } else if (blockStatus) {
                            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                            blockStatus = false
                        }
                    }
                }
            } ?: run {
                if (blockStatus) {
                    mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
                    blockStatus = false
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
                    0 -> mc.thePlayer.getDistanceToEntityBox(it) // Distance
                    1 -> (it as EntityLivingBase).health // Health
                    else -> -(it as EntityLivingBase).hurtResistantTime // Hurt time
                }
            } as EntityLivingBase?
    }

    private fun isValidTarget(entity: Entity): Boolean {
        if (entity is EntityLivingBase && EntityUtils.isSelected(entity, true)) {
            val distance = mc.thePlayer.getDistanceToEntityBox(entity)
            val angle = RotationUtils.getRotationDifference(entity)
            
            return distance <= rangeValue.get() && angle <= fovValue.get() * 0.5f
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
        val currentRot = currentRotation ?: return targetRotation
        
        val speed = aimSpeedValue.get() * 0.15f
        
        val yawDiff = getRotationDifference(targetRotation.yaw, currentRot.yaw)
        val pitchDiff = getRotationDifference(targetRotation.pitch, currentRot.pitch)
        
        val smoothYaw = currentRot.yaw + yawDiff * speed
        val smoothPitch = currentRot.pitch + pitchDiff * speed
        
        return Rotation(smoothYaw, MathHelper.clamp_float(smoothPitch, -90f, 90f))
    }

    private fun getRotationDifference(a: Float, b: Float): Float {
        var diff = (a - b) % 360f
        if (diff >= 180f) {
            diff -= 360f
        }
        if (diff < -180f) {
            diff += 360f
        }
        return diff
    }

    private fun addJitter(rotation: Rotation): Rotation {
        if (jitterValue.get() > 0f) {
            val jitterAmount = jitterValue.get()
            val yawJitter = (Math.random().toFloat() - 0.5f) * jitterAmount
            val pitchJitter = (Math.random().toFloat() - 0.5f) * jitterAmount
            
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
        val diff = RotationUtils.getRotationDifference(rotation, RotationUtils.toRotation(RotationUtils.getCenter(target.entityBoundingBox), true))
        return diff <= 20f // Allow some tolerance for more natural looking gameplay
    }

    private fun attackEntity(target: EntityLivingBase) {
        // Critical hit check
        val canCrit = mc.thePlayer.fallDistance > 0.0f && !mc.thePlayer.onGround && !mc.thePlayer.isOnLadder && !mc.thePlayer.isInWater && !mc.thePlayer.isPotionActive(net.minecraft.potion.Potion.blindness) && mc.thePlayer.ridingEntity == null

        if (keepSprintValue.get() && mc.thePlayer.isSprinting) {
            // Keep sprint
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.keyCode, true)
        }

        // Attack
        mc.thePlayer.swingItem()
        mc.playerController.attackEntity(mc.thePlayer, target)

        lastAttackTime.reset()
    }

    private fun updateClickDelay() {
        if (randomClickValue.get()) {
            // Random CPS within range
            val minDelay = 1000 / maxCpsValue.get()
            val maxDelay = 1000 / minCpsValue.get()
            clickDelay = (minDelay + (maxDelay - minDelay) * Math.random()).toLong()
        } else {
            // Fixed average CPS
            val averageCps = (minCpsValue.get() + maxCpsValue.get()) / 2
            clickDelay = (1000 / averageCps).toLong()
        }
    }

    override fun onDisable() {
        target = null
        currentRotation = null
        if (blockStatus) {
            mc.netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, EnumFacing.DOWN))
            blockStatus = false
        }
    }
}
