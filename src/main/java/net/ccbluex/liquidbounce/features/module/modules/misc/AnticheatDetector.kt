/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 * @author RtxOP
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.server.S01PacketJoinGame
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.ccbluex.liquidbounce.utils.client.ServerUtils.remoteIp

object AnticheatDetector : Module("AnticheatDetector", Category.MISC) {
    private val debug by boolean("Debug", true)
    private val reduceSpam by boolean("ReduceSpam", false)
    
    private val actionNumbers = mutableListOf<Int>()
    private var check = false
    private var ticksPassed = 0
    

    private val detectVelocityModification by boolean("DetectVelocityModification", true)
    private val detectTeleportPatterns by boolean("DetectTeleportPatterns", true) 
    private val detectMovementFlags by boolean("DetectMovementFlags", true)
    
    private val velocityChecks = mutableListOf<Triple<Int, Int, Int>>()
    private val positionHistory = mutableListOf<Triple<Double, Double, Double>>()
    private val flagPatterns = mutableMapOf<String, Int>()
    
    private var lastVelocityTime = 0L
    private var lastPosLookTime = 0L
    private var detectedAnticheat: String? = null
    

    private val notificationCooldowns = mutableMapOf<String, Long>()
    private val cooldownPeriod = 8000L // 8 seconds
    private var lastPeriodicCheck = 0L
    private val periodicCheckInterval = 15000L // 15 seconds

    val onPacket = handler<PacketEvent> { event ->
        when (event.packet) {
            is S32PacketConfirmTransaction -> {
                if (check) handleTransaction(event.packet.actionNumber.toInt())
            }
            is S01PacketJoinGame -> {
                reset().also { check = true }
                detectedAnticheat = null
                debugMessage("Joined new world, starting detection...")
            }
            is S08PacketPlayerPosLook -> {
                if (detectTeleportPatterns && check) {
                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastPosLookTime
                    lastPosLookTime = currentTime
                    

                    val pos = Triple(event.packet.x, event.packet.y, event.packet.z)
                    positionHistory.add(pos)
                    if (positionHistory.size > 5) positionHistory.removeAt(0)
                    
                    debugMessage("Position teleport: X=${String.format("%.2f", pos.first)}, Y=${String.format("%.2f", pos.second)}, Z=${String.format("%.2f", pos.third)}, TimeDiff=${timeDiff}ms")
                    

                    if (timeDiff < 100 && positionHistory.size > 1) {
                        incrementFlagPattern("rapid_teleport")
                        debugMessage("Rapid teleport detected (${timeDiff}ms) - Pattern count: ${getFlagPattern("rapid_teleport")}")
                    }
                    

                    if (timeDiff < 500) {
                        incrementFlagPattern("flag_teleport")
                        debugMessage("Flag teleport pattern detected (${timeDiff}ms) - Pattern count: ${getFlagPattern("flag_teleport")}")
                        if (getFlagPattern("flag_teleport") > 3) {
                            if (remoteIp.lowercase().contains("mineplex")) {
                                debugMessage("Mineplex detection triggered by flag teleport pattern (${getFlagPattern("flag_teleport")} occurrences)")
                                notifyDetection("Mineplex AntiCheat")
                            } else {
                                debugMessage("AAC detection triggered by flag teleport pattern (${getFlagPattern("flag_teleport")} occurrences)")
                                notifyDetection("AAC")
                            }
                        }
                    }
                }
            }
            is S12PacketEntityVelocity -> {
                if (detectVelocityModification && check && event.packet.entityID == mc.thePlayer?.entityId) {
                    val currentTime = System.currentTimeMillis()
                    val timeDiff = currentTime - lastVelocityTime
                    lastVelocityTime = currentTime
                    

                    val velocity = Triple(event.packet.motionX, event.packet.motionY, event.packet.motionZ)
                    velocityChecks.add(velocity)
                    if (velocityChecks.size > 5) velocityChecks.removeAt(0)
                    
                    debugMessage("Velocity packet: X=${velocity.first}, Y=${velocity.second}, Z=${velocity.third}, TimeDiff=${timeDiff}ms")
                    

                    if (velocity.second > 8000 && velocity.first == 0 && velocity.third == 0) {
                        incrementFlagPattern("vertical_only_velocity")
                        debugMessage("Vertical-only velocity detected (Y=${velocity.second}) - Pattern count: ${getFlagPattern("vertical_only_velocity")}")
                        if (getFlagPattern("vertical_only_velocity") > 2) {
                            debugMessage("Hypixel Watchdog detection triggered by vertical velocity pattern (${getFlagPattern("vertical_only_velocity")} occurrences)")
                            notifyDetection("Hypixel Watchdog")
                        }
                    }
                    

                    if (velocity.first % 100 == 0 && velocity.third % 100 == 0) {
                        incrementFlagPattern("rounded_velocity")
                        debugMessage("Rounded velocity detected (X=${velocity.first}, Z=${velocity.third}) - Pattern count: ${getFlagPattern("rounded_velocity")}")
                        if (getFlagPattern("rounded_velocity") > 2) {
                            if (remoteIp.lowercase().contains("vulcan")) {
                                debugMessage("Vulcan detection triggered by rounded velocity pattern (${getFlagPattern("rounded_velocity")} occurrences)")
                                notifyDetection("Vulcan")
                            } else {
                                debugMessage("Matrix detection triggered by rounded velocity pattern (${getFlagPattern("rounded_velocity")} occurrences)")
                                notifyDetection("Matrix")
                            }
                        }
                    }
                }
            }
            is S27PacketExplosion -> {
                if (check && event.packet.field_149159_h == 0.0 && event.packet.func_149147_e() == 0.0) {
                    incrementFlagPattern("zero_explosion_motion")
                    debugMessage("Zero explosion motion detected - Pattern count: ${getFlagPattern("zero_explosion_motion")}")
                    if (getFlagPattern("zero_explosion_motion") > 1) {
                        debugMessage("Vulcan/Matrix detection triggered by zero explosion motion pattern (${getFlagPattern("zero_explosion_motion")} occurrences)")
                        notifyDetection("Vulcan/Matrix")
                    }
                }
            }
        }
        

        if (detectMovementFlags && check && event.eventType == EventState.SEND && event.packet is C03PacketPlayer) {
            val player = mc.thePlayer ?: return@handler
            

            if (player.motionY < -0.08 && player.fallDistance > 0.5f && !player.onGround && !player.capabilities.isFlying) {
                val packet = event.packet as C03PacketPlayer
                if (packet.onGround) {
                    incrementFlagPattern("ground_spoof")
                    debugMessage("Ground spoof detected (MotionY=${String.format("%.3f", player.motionY)}, FallDistance=${String.format("%.2f", player.fallDistance)}) - Pattern count: ${getFlagPattern("ground_spoof")}")
                    if (getFlagPattern("ground_spoof") > 3) {
                        debugMessage("AAC/NCP detection triggered by ground spoof pattern (${getFlagPattern("ground_spoof")} occurrences)")
                        notifyDetection("AAC/NCP")
                    }
                }
            }
        }
    }

    val onTick = handler<GameTickEvent> {
        if (check && ticksPassed++ > 40) {
            notifyDetection("None").also { reset() }
        }
        
        // Periodic anticheat checking when ReduceSpam is enabled
        if (reduceSpam && check) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPeriodicCheck > periodicCheckInterval) {
                lastPeriodicCheck = currentTime
                performPeriodicCheck()
            }
        }
    }

    private fun handleTransaction(action: Int) {
        actionNumbers.add(action)
        debugMessage("Transaction ID: $action")
        ticksPassed = 0
        if (actionNumbers.size >= 5) analyzeActionNumbers()
    }

    private fun analyzeActionNumbers() {
        val diffs = actionNumbers.windowed(2) { it[1] - it[0] }
        val first = actionNumbers.first()
        
        debugMessage("Analyzing transaction pattern: Numbers=${actionNumbers.joinToString()}, Diffs=${diffs.joinToString()}, First=$first")
        
        when {
            remoteIp.lowercase().equals("hypixel.net", true) -> {
                debugMessage("Hypixel server detected via IP match")
                notifyDetection("Watchdog")
            }
            
            diffs.all { it == diffs.first() } -> when (diffs.first()) {
                
                1 -> when (first) {
                    in -23772..-23762 -> {
                        debugMessage("Vulcan pattern detected: Consistent +1 diff with first=$first")
                        "Vulcan"
                    }
                    in 95..105, in -20005..-19995 -> {
                        debugMessage("Matrix pattern detected: Consistent +1 diff with first=$first")
                        "Matrix"
                    }
                    in -32773..-32762 -> {
                        debugMessage("Grizzly pattern detected: Consistent +1 diff with first=$first")
                        "Grizzly"
                    }
                    else -> {
                        debugMessage("Verus pattern detected: Consistent +1 diff with first=$first (default case)")
                        "Verus"
                    }
                }
                -1 -> when {
                    first in -8287..-8280 -> {
                        debugMessage("Errata pattern detected: Consistent -1 diff with first=$first")
                        "Errata"
                    }
                    first < -3000 -> {
                        debugMessage("Intave pattern detected: Consistent -1 diff with first=$first (< -3000)")
                        "Intave"
                    }
                    first in -5..0 -> {
                        debugMessage("Grim pattern detected: Consistent -1 diff with first=$first (-5 to 0)")
                        "Grim"
                    }
                    first in -3000..-2995 -> {
                        debugMessage("Karhu pattern detected: Consistent -1 diff with first=$first (-3000 to -2995)")
                        "Karhu"
                    }
                    else -> {
                        debugMessage("Polar pattern detected: Consistent -1 diff with first=$first (default case)")
                        "Polar"
                    }
                }
                else -> {
                    debugMessage("No consistent diff pattern matched (diff=${diffs.first()})")
                    null
                }
            }?.let { notifyDetection(it) }
            
            actionNumbers.take(2).let { it[0] == it[1] } 
                && actionNumbers.drop(2).windowed(2).all { it[1] - it[0] == 1 } -> {
                debugMessage("Verus special pattern detected: First two numbers equal (${actionNumbers[0]}), rest increment by 1")
                notifyDetection("Verus")
            }
            
            diffs.take(2).let { it[0] >= 100 && it[1] == -1 } 
                && diffs.drop(2).all { it == -1 } -> {
                debugMessage("Polar special pattern detected: First diff >= 100 (${diffs[0]}), second diff = -1, rest = -1")
                notifyDetection("Polar")
            }
            
            actionNumbers.first() < -3000 && actionNumbers.any { it == 0 } -> {
                debugMessage("Intave special pattern detected: First < -3000 (${actionNumbers.first()}) and contains 0")
                notifyDetection("Intave")
            }
            
            actionNumbers.take(3) == listOf(-30767, -30766, -25767) 
                && actionNumbers.drop(3).windowed(2).all { it[1] - it[0] == 1 } -> {
                debugMessage("Old Vulcan pattern detected: Specific sequence [-30767, -30766, -25767] followed by +1 increments")
                notifyDetection("Old Vulcan")
            }
                

            actionNumbers.all { it < 0 } && actionNumbers.any { it > -1000 } && actionNumbers.any { it < -5000 } -> {
                debugMessage("Spartan pattern detected: All negative, some > -1000, some < -5000")
                notifyDetection("Spartan")
            }
                
            actionNumbers.all { it > 0 } && actionNumbers.all { it < 500 } && diffs.any { it > 20 } && diffs.any { it < 5 } -> {
                debugMessage("Hawk pattern detected: All positive < 500, mixed diff sizes (>20 and <5)")
                notifyDetection("Hawk")
            }
                
            actionNumbers.windowed(3).any { it[0] + it[1] == it[2] * 2 } -> {
                debugMessage("Kauri pattern detected: Mathematical sequence found")
                notifyDetection("Kauri")
            }
            
            else -> {
                debugMessage("No known pattern matched - marking as Unknown")
                notifyDetection("Unknown").also { if (debug) logNumbers() }
            }
        }
        reset()
    }

    private fun notifyDetection(anticheat: String) {
        // Only apply spam reduction when ReduceSpam is enabled
        if (reduceSpam) {
            val currentTime = System.currentTimeMillis()
            val lastNotification = notificationCooldowns[anticheat] ?: 0L
            
            if (currentTime - lastNotification < cooldownPeriod) {
                debugMessage("Notification for '$anticheat' suppressed due to cooldown (${(cooldownPeriod - (currentTime - lastNotification)) / 1000}s remaining)")
                return
            }
            
            notificationCooldowns[anticheat] = currentTime
            debugMessage("Notification cooldown updated for '$anticheat'")
        }
        
        // When ReduceSpam is disabled, always send notifications immediately (allows spam)
        detectedAnticheat = anticheat
        notify(anticheat)
        debugMessage("Anticheat detection notification sent: $anticheat")
    }
    
    private fun performPeriodicCheck() {
        debugMessage("Performing periodic anticheat check...")
        
        // Check if we have enough data for analysis
        if (actionNumbers.size >= 3) {
            debugMessage("Periodic check: Found ${actionNumbers.size} transaction numbers for analysis")
            analyzeActionNumbers()
        }
        
        // Check flag patterns for potential detections
        flagPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "rapid_teleport" -> if (count > 2) {
                    debugMessage("Periodic check: Rapid teleport pattern detected ($count occurrences)")
                    notifyDetection("Movement AntiCheat")
                }
                "rounded_velocity" -> if (count > 1) {
                    debugMessage("Periodic check: Rounded velocity pattern detected ($count occurrences)")
                    notifyDetection("Velocity AntiCheat")
                }
                "ground_spoof" -> if (count > 2) {
                    debugMessage("Periodic check: Ground spoof pattern detected ($count occurrences)")
                    notifyDetection("Movement AntiCheat")
                }
            }
        }
        
        // Check for server-specific indicators
        if (remoteIp.lowercase().contains("hypixel")) {
            debugMessage("Periodic check: Hypixel server detected")
            notifyDetection("Watchdog")
        }
    }

    private fun notify(message: String) = hud.addNotification(
        Notification.informative(this, "Anticheat detected: $message", 3000L)
    )

    private fun incrementFlagPattern(pattern: String) {
        flagPatterns[pattern] = (flagPatterns[pattern] ?: 0) + 1
        debugMessage("Flag pattern '$pattern' incremented to ${flagPatterns[pattern]}")
    }
    
    private fun getFlagPattern(pattern: String): Int {
        return flagPatterns[pattern] ?: 0
    }

    private fun debugMessage(message: String) {
        if (debug) {
            chat("§7[§bAnticheatDetector§7] §f$message")
        }
    }

    private fun logNumbers() {
        debugMessage("Action Numbers: ${actionNumbers.joinToString()}")
        debugMessage("Differences: ${actionNumbers.windowed(2) { it[1] - it[0] }.joinToString()}")
        if (velocityChecks.isNotEmpty()) {
            debugMessage("Velocity Checks: ${velocityChecks.joinToString()}")
        }
        if (positionHistory.isNotEmpty()) {
            debugMessage("Position History: ${positionHistory.joinToString()}")
        }
        debugMessage("Flag Patterns: ${flagPatterns.entries.joinToString { "${it.key}=${it.value}" }}")
    }

    private fun reset() {
        actionNumbers.clear()
        ticksPassed = 0
        check = false
        
        // Clear old cooldowns (older than 30 seconds)
        if (reduceSpam) {
            val currentTime = System.currentTimeMillis()
            notificationCooldowns.entries.removeIf { (_, time) -> 
                currentTime - time > 30000L
            }
            debugMessage("Notification cooldowns cleaned up")
        }
    }

    override fun onEnable() = reset()
    
    override fun onDisable() {
        reset()
        notificationCooldowns.clear()
        flagPatterns.clear()
        velocityChecks.clear()
        positionHistory.clear()
        debugMessage("AnticheatDetector disabled, all data cleared")
    }
}
