/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.kotlin.RandomUtils
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.item.ItemStack
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import kotlin.random.Random

object FastPlace : Module("FastPlace", Category.WORLD) {

    // === CORE SETTINGS ===
    private val mode by choices("Mode", arrayOf("Vanilla", "Legit", "Bypass", "Custom"), "Legit")
    val speed by int("Speed", 0, 0..4) { mode == "Vanilla" }

    // === DELAY SETTINGS ===
    private val minDelay by int("MinDelay", 0, 0..20) { mode in arrayOf("Legit", "Bypass", "Custom") }
    private val maxDelay by int("MaxDelay", 2, 0..20) { mode in arrayOf("Legit", "Bypass", "Custom") }
    private val customDelay by int("CustomDelay", 1, 0..20) { mode == "Custom" }

    // === ANTI-DETECTION ===
    private val humanization by boolean("Humanization", true) { mode != "Vanilla" }
    private val randomization by boolean("Randomization", true) { mode != "Vanilla" }
    private val adaptiveDelay by boolean("AdaptiveDelay", true) { mode == "Bypass" }
    private val burstProtection by boolean("BurstProtection", true) { mode != "Vanilla" }

    // === COMPATIBILITY ===
    val onlyBlocks by boolean("OnlyBlocks", true)
    val facingBlocks by boolean("OnlyWhenFacingBlocks", true)
    private val serverMode by choices("ServerMode", arrayOf("Universal", "Hypixel", "NCP", "AAC", "Spartan"), "Universal")
    private val respectCooldown by boolean("RespectCooldown", true) { mode != "Vanilla" }

    // === PERFORMANCE ===
    private val smartPlacement by boolean("SmartPlacement", true)
    private val predictiveDelay by boolean("PredictiveDelay", false) { mode == "Bypass" }
    private val lagCompensation by boolean("LagCompensation", true) { mode == "Bypass" }

    // === BURST SETTINGS ===
    private val burstLimit by int("BurstLimit", 3, 1..10) { burstProtection }
    private val burstCooldown by int("BurstCooldown", 100, 50..500) { burstProtection }

    // === INTERNAL STATE ===
    private val delayTimer = TickTimer()
    private var lastPlaceTime = 0L
    private var placementCount = 0
    private var burstCount = 0
    private var lastBurstTime = 0L
    private var adaptiveDelayMultiplier = 1.0
    private var lastServerLag = 0L

    // === STATISTICS ===
    private var totalPlacements = 0
    private var averageDelay = 0.0
    private var lastPlacementTimes = mutableListOf<Long>()

    init {

        delayTimer.reset()
    }

    override fun onEnable() {

        delayTimer.reset()
        placementCount = 0
        burstCount = 0
        lastBurstTime = 0L
        adaptiveDelayMultiplier = 1.0
        lastPlacementTimes.clear()
    }

    val onUpdate = handler<UpdateEvent> {
        val player = mc.thePlayer ?: return@handler
        val world = mc.theWorld ?: return@handler

        updateStatistics()


        if (burstProtection) {
            handleBurstProtection()
        }

        if (adaptiveDelay && mode == "Bypass") {
            updateAdaptiveDelay()
        }


        if (lagCompensation && mode == "Bypass") {
            updateLagCompensation()
        }
    }

    /**
     * Check if we can place a block right now
     */
    fun canPlace(): Boolean {
        val player = mc.thePlayer ?: return false

        if (!isHoldingPlaceableItem()) return false

        if (facingBlocks && !isFacingPlaceableBlock()) return false

        // Check cooldown
        if (respectCooldown && player.itemInUseCount > 0) return false

        if (burstProtection && !canBurst()) return false


        return when (mode) {
            "Vanilla" -> true // No delay restrictions in vanilla mode
            "Legit" -> checkLegitDelay()
            "Bypass" -> checkBypassDelay()
            "Custom" -> checkCustomDelay()
            else -> false
        }
    }

    /**
     * Get the current delay that should be applied
     */
    fun getCurrentDelay(): Int {
        val baseDelay = when (mode) {
            "Vanilla" -> 0
            "Legit" -> if (randomization) RandomUtils.nextInt(minDelay, maxDelay + 1) else minDelay
            "Bypass" -> calculateBypassDelay()
            "Custom" -> customDelay
            else -> 0
        }

        // Apply humanization
        return if (humanization && mode != "Vanilla") {
            applyHumanization(baseDelay)
        } else {
            baseDelay
        }
    }

    // === HELPER METHODS ===

    private fun isHoldingPlaceableItem(): Boolean {
        val player = mc.thePlayer ?: return false
        val heldItem = player.heldItem ?: return false

        return if (onlyBlocks) {
            heldItem.item is ItemBlock
        } else {
            heldItem.item is ItemBlock || heldItem.item.javaClass.simpleName.contains("Item")
        }
    }

    private fun isFacingPlaceableBlock(): Boolean {
        val player = mc.thePlayer ?: return false
        val world = mc.theWorld ?: return false

        val rayTrace = player.rayTrace(4.5, 1.0f)
        if (rayTrace?.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return false

        val blockPos = rayTrace.blockPos ?: return false
        val block = world.getBlockState(blockPos).block

        return block != Blocks.air && block.isFullBlock
    }

    private fun canBurst(): Boolean {
        if (!burstProtection) return true

        val currentTime = System.currentTimeMillis()


        if (currentTime - lastBurstTime > burstCooldown) {
            burstCount = 0
        }

        return burstCount < burstLimit
    }

    private fun checkLegitDelay(): Boolean {
        val currentTime = System.currentTimeMillis()
        val requiredDelay = getCurrentDelay() * 50L // Convert ticks to milliseconds

        return currentTime - lastPlaceTime >= requiredDelay
    }

    private fun checkBypassDelay(): Boolean {
        val currentTime = System.currentTimeMillis()
        val requiredDelay = (getCurrentDelay() * adaptiveDelayMultiplier * 50L).toLong()

        return currentTime - lastPlaceTime >= requiredDelay
    }

    private fun checkCustomDelay(): Boolean {
        val currentTime = System.currentTimeMillis()
        val requiredDelay = customDelay * 50L

        return currentTime - lastPlaceTime >= requiredDelay
    }

    private fun calculateBypassDelay(): Int {
        return when (serverMode) {
            "Hypixel" -> if (randomization) RandomUtils.nextInt(1, 3) else 1
            "NCP" -> if (randomization) RandomUtils.nextInt(0, 2) else 0
            "AAC" -> if (randomization) RandomUtils.nextInt(1, 4) else 2
            "Spartan" -> if (randomization) RandomUtils.nextInt(2, 5) else 3
            else -> if (randomization) RandomUtils.nextInt(minDelay, maxDelay + 1) else minDelay
        }
    }

    private fun applyHumanization(baseDelay: Int): Int {
        if (!humanization) return baseDelay


        val variance = (baseDelay * 0.2).toInt().coerceAtLeast(1)
        val randomOffset = RandomUtils.nextInt(-variance, variance + 1)


        val hesitationChance = 0.05 // 5% chance
        val hesitationDelay = if (Random.nextDouble() < hesitationChance) {
            RandomUtils.nextInt(5, 15)
        } else 0

        return (baseDelay + randomOffset + hesitationDelay).coerceAtLeast(0)
    }

    private fun handleBurstProtection() {
        val currentTime = System.currentTimeMillis()


        if (currentTime - lastBurstTime > burstCooldown) {
            burstCount = 0
        }
    }

    private fun updateAdaptiveDelay() {

        val currentTime = System.currentTimeMillis()


        if (lastPlacementTimes.size >= 5) {
            val recentPlacements = lastPlacementTimes.takeLast(5)
            val averageInterval = recentPlacements.zipWithNext { a, b -> b - a }.average()

            if (averageInterval < 100) { // Less than 100ms between placements
                adaptiveDelayMultiplier = (adaptiveDelayMultiplier * 1.1).coerceAtMost(3.0)
            } else if (averageInterval > 300) { // More than 300ms between placements
                adaptiveDelayMultiplier = (adaptiveDelayMultiplier * 0.95).coerceAtLeast(0.5)
            }
        }
    }

    private fun updateLagCompensation() {

        val ping = mc.netHandler?.getPlayerInfo(mc.thePlayer?.uniqueID)?.responseTime ?: 0

        if (ping > 100) {
            adaptiveDelayMultiplier = (adaptiveDelayMultiplier * 1.05).coerceAtMost(2.0)
        } else if (ping < 50) {
            adaptiveDelayMultiplier = (adaptiveDelayMultiplier * 0.98).coerceAtLeast(0.7)
        }
    }

    private fun updateStatistics() {

        if (lastPlacementTimes.size > 20) {
            lastPlacementTimes = lastPlacementTimes.takeLast(20).toMutableList()
        }


        if (lastPlacementTimes.size >= 2) {
            val intervals = lastPlacementTimes.zipWithNext { a, b -> b - a }
            averageDelay = intervals.average()
        }
    }

    /**
     * Called when a block is placed - updates internal state
     */
    fun onBlockPlaced() {
        val currentTime = System.currentTimeMillis()

        lastPlaceTime = currentTime
        lastPlacementTimes.add(currentTime)
        totalPlacements++
        placementCount++


        if (burstProtection) {
            burstCount++
            lastBurstTime = currentTime
        }

        // Reset delay timer
        delayTimer.reset()
    }

    // === PUBLIC API ===

    /**
     * Get the tag to display on the module
     */
    override val tag: String
        get() = when (mode) {
            "Vanilla" -> "S$speed"
            "Custom" -> "C$customDelay"
            else -> "$mode ${if (randomization) "${minDelay}-${maxDelay}" else "$minDelay"}ms"
        }
}
