/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 * @author RtxOP
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.GameTickEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S01PacketJoinGame
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.network.Packet
import net.ccbluex.liquidbounce.utils.client.ServerUtils.remoteIp
import kotlin.math.abs
import kotlin.math.sqrt

object AnticheatDetector : Module("AnticheatDetector", Category.MISC) {
    private val debug by boolean("Debug", true)
    private val reduceSpam by boolean("ReduceSpam", true)
    private val spamDelaySeconds by float("SpamDelaySeconds", 8.0f, 1.0f..30.0f) { reduceSpam }

    private val detectVelocityModification by boolean("DetectVelocityModification", true)
    private val detectTeleportPatterns by boolean("DetectTeleportPatterns", true)
    private val detectMovementFlags by boolean("DetectMovementFlags", true)
    private val usePacketIDs by boolean("UsePacketIDs", true)
    private val detectAdvancedAnticheats by boolean("DetectAdvancedAnticheats", true)

    // Enhanced detection options
    private val detectACCPatterns by boolean("DetectACC", true)
    private val detectVulcanPatterns by boolean("DetectVulcan", true)
    private val detectMatrixPatterns by boolean("DetectMatrix", true)
    private val detectKarhuPatterns by boolean("DetectKarhu", true)
    private val detectSpartanPatterns by boolean("DetectSpartan", true)
    private val detectNegativePatterns by boolean("DetectNegative", true)
    private val detectThemisPatterns by boolean("DetectThemis", true)
    private val detectVerusPatterns by boolean("DetectVerus", true)

    private val enableAdaptiveSpamControl by boolean("AdaptiveSpamControl", true) { reduceSpam }
    private val maxNotificationsPerMinute by int("MaxNotificationsPerMinute", 10, 1..50) { enableAdaptiveSpamControl && reduceSpam }
    private val quietMode by boolean("QuietMode", false)

    // Packet ID constants for better compatibility
    private object PacketIDs {
        const val S01_JOIN_GAME = 0x01
        const val S08_PLAYER_POS_LOOK = 0x08
        const val S12_ENTITY_VELOCITY = 0x12
        const val S27_EXPLOSION = 0x27
        const val S32_CONFIRM_TRANSACTION = 0x32
        const val C03_PLAYER = 0x03
    }


    private val velocityChecks = mutableListOf<Triple<Int, Int, Int>>()
    private val positionHistory = mutableListOf<Triple<Double, Double, Double>>()
    private val flagPatterns = mutableMapOf<String, Int>()
    private val velocityTimings = mutableListOf<Long>()
    private val transactionTimings = mutableListOf<Long>()

    private val actionNumbers = mutableListOf<Int>()
    private var check = false
    private var ticksPassed = 0

    private var lastVelocityTime = 0L
    private var lastPosLookTime = 0L

    private var detectedAnticheat: String? = null

    private val notificationCooldowns = mutableMapOf<String, Long>()
    private var lastPeriodicCheck = 0L
    private const val PERIODIC_CHECK_INTERVAL = 15000L

    // Enhanced pattern tracking
    private val grimPatterns = mutableMapOf<String, Int>()
    private val intavePatterns = mutableMapOf<String, Int>()
    private val vulcanPatterns = mutableMapOf<String, Int>()
    private val kauraPatterns = mutableMapOf<String, Int>()
    private val sparkyPatterns = mutableMapOf<String, Int>()
    private val themisPatterns = mutableMapOf<String, Int>()
    private val negativityPatterns = mutableMapOf<String, Int>()

    // Additional anticheat patterns
    private val accPatterns = mutableMapOf<String, Int>()
    private val matrixPatterns = mutableMapOf<String, Int>()
    private val karhuPatterns = mutableMapOf<String, Int>()
    private val spartanPatterns = mutableMapOf<String, Int>()
    private val verusPatterns = mutableMapOf<String, Int>()

    private var consecutiveZeroVelocities = 0

    // Enhanced notification tracking
    private var notificationCount = 0
    private var lastNotificationMinute = System.currentTimeMillis()

    // Packet ID tracking for enhanced detection
    private val packetIdSequence = mutableListOf<Int>()
    private val packetTimings = mutableMapOf<Int, MutableList<Long>>()


    val onPacket = handler<PacketEvent> { event ->
        if (usePacketIDs) {
            handlePacketByID(event)
        } else {
            handlePacketByType(event)
        }
    }

    private fun handlePacketByID(event: PacketEvent) {
        val packetId = getPacketID(event.packet)
        if (packetId != -1) {
            trackPacketSequence(packetId)

            when (packetId) {
                PacketIDs.S32_CONFIRM_TRANSACTION -> {
                    if (check && event.packet is S32PacketConfirmTransaction) {
                        val currentTime = System.currentTimeMillis()
                        transactionTimings.add(currentTime)
                        if (transactionTimings.size > 10) transactionTimings.removeAt(0)

                        handleTransaction(event.packet.actionNumber.toInt())

                        // Enhanced anticheat detection for transactions
                        detectACCPatterns(event.packet)

                        analyzeTransactionTiming()
                        analyzeAdvancedPatterns(packetId, currentTime)
                    }
                }
                PacketIDs.S01_JOIN_GAME -> {
                    reset().also { check = true }
                    detectedAnticheat = null
                    debugMessage("Joined new world, starting enhanced packet ID detection...")
                }
                PacketIDs.S08_PLAYER_POS_LOOK -> {
                    if (detectTeleportPatterns && check && event.packet is S08PacketPlayerPosLook) {
                        handleTeleportPattern(event.packet)

                        // Enhanced anticheat detection for teleports
                        detectVulcanPatterns(event.packet)

                        analyzeAdvancedPatterns(packetId, System.currentTimeMillis())
                    }
                }
                PacketIDs.S12_ENTITY_VELOCITY -> {
                    if (detectVelocityModification && check && event.packet is S12PacketEntityVelocity
                        && event.packet.entityID == mc.thePlayer?.entityId) {
                        handleVelocityPattern(event.packet)

                        // Enhanced anticheat detection
                        detectACCPatterns(event.packet)
                        detectVulcanPatterns(event.packet)
                        detectMatrixPatterns(event.packet)
                        detectKarhuPatterns(event.packet)
                        detectSpartanPatterns(event.packet)
                        detectVerusPatterns(event.packet)

                        analyzeAdvancedPatterns(packetId, System.currentTimeMillis())
                    }
                }
                PacketIDs.S27_EXPLOSION -> {
                    if (check && event.packet is S27PacketExplosion) {
                        handleExplosionPattern(event.packet)

                        // Enhanced anticheat detection for explosions
                        detectMatrixPatterns(event.packet)

                        analyzeAdvancedPatterns(packetId, System.currentTimeMillis())
                    }
                }
                PacketIDs.C03_PLAYER -> {
                    if (detectMovementFlags && check && event.eventType == EventState.SEND
                        && event.packet is C03PacketPlayer) {
                        handleMovementFlags(event.packet)
                    }
                }
            }
        }
    }

    private fun handlePacketByType(event: PacketEvent) {
        when (event.packet) {
            is S32PacketConfirmTransaction -> {
                if (check) {
                    val currentTime = System.currentTimeMillis()
                    transactionTimings.add(currentTime)
                    if (transactionTimings.size > 10) transactionTimings.removeAt(0)

                    handleTransaction(event.packet.actionNumber.toInt())
                    analyzeTransactionTiming()
                }
            }
            is S01PacketJoinGame -> {
                reset().also { check = true }
                detectedAnticheat = null
                debugMessage("Joined new world, starting enhanced detection...")
            }
            is S08PacketPlayerPosLook -> {
                if (detectTeleportPatterns && check) {
                    handleTeleportPattern(event.packet)
                }
            }
            is S12PacketEntityVelocity -> {
                if (detectVelocityModification && check && event.packet.entityID == mc.thePlayer?.entityId) {
                    handleVelocityPattern(event.packet)
                }
            }
            is S27PacketExplosion -> {
                if (check) handleExplosionPattern(event.packet)
            }
        }

        if (detectMovementFlags && check && event.eventType == EventState.SEND && event.packet is C03PacketPlayer) {
            handleMovementFlags(event.packet)
        }
    }

    private fun getPacketID(packet: Packet<*>): Int {
        return when (packet) {
            is S01PacketJoinGame -> PacketIDs.S01_JOIN_GAME
            is S08PacketPlayerPosLook -> PacketIDs.S08_PLAYER_POS_LOOK
            is S12PacketEntityVelocity -> PacketIDs.S12_ENTITY_VELOCITY
            is S27PacketExplosion -> PacketIDs.S27_EXPLOSION
            is S32PacketConfirmTransaction -> PacketIDs.S32_CONFIRM_TRANSACTION
            is C03PacketPlayer -> PacketIDs.C03_PLAYER
            else -> -1
        }
    }

    private fun trackPacketSequence(packetId: Int) {
        packetIdSequence.add(packetId)
        if (packetIdSequence.size > 20) packetIdSequence.removeAt(0)

        val currentTime = System.currentTimeMillis()
        packetTimings.getOrPut(packetId) { mutableListOf() }.add(currentTime)
        packetTimings[packetId]?.let { timings ->
            if (timings.size > 10) timings.removeAt(0)
        }
    }

    private fun analyzeAdvancedPatterns(packetId: Int, timestamp: Long) {
        if (!detectAdvancedAnticheats) return

        // Kaura AC detection - specific packet timing patterns
        analyzeKauraPatterns(packetId, timestamp)

        // Sparky AC detection - packet sequence analysis
        analyzeSparkyPatterns()

        // Themis AC detection - advanced timing analysis
        analyzeThemisPatterns(packetId, timestamp)

        // Negativity AC detection - packet frequency analysis
        analyzeNegativityPatterns(packetId, timestamp)
    }

    private fun analyzeKauraPatterns(packetId: Int, timestamp: Long) {
        if (packetId == PacketIDs.S32_CONFIRM_TRANSACTION) {
            packetTimings[packetId]?.let { timings ->
                if (timings.size >= 5) {
                    val intervals = timings.windowed(2) { it[1] - it[0] }
                    // Kaura has very specific 20ms intervals
                    if (intervals.all { it in 18..22 } && intervals.size >= 4) {
                        incrementPattern(kauraPatterns, "kaura_precise_timing")
                        debugMessage("Kaura AC precise timing pattern detected")
                        if (getPattern(kauraPatterns, "kaura_precise_timing") > 3) {
                            notifyDetection("Kaura AC")
                        }
                    }
                }
            }
        }
    }

    private fun analyzeSparkyPatterns() {
        if (packetIdSequence.size >= 8) {
            val recentSequence = packetIdSequence.takeLast(8)
            // Sparky has specific packet ordering patterns
            if (recentSequence.count { it == PacketIDs.S32_CONFIRM_TRANSACTION } >= 6 &&
                recentSequence.count { it == PacketIDs.S08_PLAYER_POS_LOOK } >= 1) {
                incrementPattern(sparkyPatterns, "sparky_sequence_pattern")
                debugMessage("Sparky AC sequence pattern detected")
                if (getPattern(sparkyPatterns, "sparky_sequence_pattern") > 2) {
                    notifyDetection("Sparky AC")
                }
            }
        }
    }

    private fun analyzeThemisPatterns(packetId: Int, timestamp: Long) {
        if (packetId == PacketIDs.S08_PLAYER_POS_LOOK) {
            packetTimings[packetId]?.let { timings ->
                if (timings.size >= 3) {
                    val intervals = timings.windowed(2) { it[1] - it[0] }
                    // Themis has irregular but consistent patterns
                    if (intervals.any { it in 45..55 } && intervals.any { it in 95..105 }) {
                        incrementPattern(themisPatterns, "themis_mixed_timing")
                        debugMessage("Themis AC mixed timing pattern detected")
                        if (getPattern(themisPatterns, "themis_mixed_timing") > 2) {
                            notifyDetection("Themis AC")
                        }
                    }
                }
            }
        }
    }

    private fun analyzeNegativityPatterns(packetId: Int, timestamp: Long) {
        if (packetId == PacketIDs.S32_CONFIRM_TRANSACTION) {
            packetTimings[packetId]?.let { timings ->
                if (timings.size >= 10) {
                    val recentTimings = timings.takeLast(10)
                    val totalTime = recentTimings.last() - recentTimings.first()
                    val frequency = recentTimings.size.toDouble() / (totalTime / 1000.0)

                    // Negativity sends packets at very high frequency
                    if (frequency > 25.0) {
                        incrementPattern(negativityPatterns, "negativity_high_frequency")
                        debugMessage("Negativity AC high frequency pattern detected (${String.format("%.1f", frequency)} Hz)")
                        if (getPattern(negativityPatterns, "negativity_high_frequency") > 1) {
                            notifyDetection("Negativity AC")
                        }
                    }
                }
            }
        }
    }

    private fun handleTeleportPattern(packet: S08PacketPlayerPosLook) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastPosLookTime
        lastPosLookTime = currentTime
        
        val pos = Triple(packet.x, packet.y, packet.z)
        positionHistory.add(pos)
        if (positionHistory.size > 8) positionHistory.removeAt(0)
        
        debugMessage("Position teleport: X=${String.format("%.2f", pos.first)}, Y=${String.format("%.2f", pos.second)}, Z=${String.format("%.2f", pos.third)}, TimeDiff=${timeDiff}ms")
        
        // Enhanced Grim detection - specific teleport patterns
        if (timeDiff in 45..55 && positionHistory.size > 2) {
            incrementPattern(grimPatterns, "grim_precise_teleport")
            debugMessage("Grim precise teleport timing detected (${timeDiff}ms)")
            if (getPattern(grimPatterns, "grim_precise_teleport") > 2) {
                notifyDetection("Grim AC")
            }
        }
        
        // Enhanced Intave detection - irregular teleport intervals
        if (timeDiff < 30 || (timeDiff > 150 && timeDiff < 200)) {
            incrementPattern(intavePatterns, "intave_irregular_teleport")
            debugMessage("Intave irregular teleport pattern detected (${timeDiff}ms)")
            if (getPattern(intavePatterns, "intave_irregular_teleport") > 3) {
                notifyDetection("Intave")
            }
        }

        // Better Intave teleport patterns
        if (timeDiff in 35..45 && positionHistory.size > 3) {
            val recentPositions = positionHistory.takeLast(4)
            val distances = recentPositions.windowed(2) {
                val dx = it[1].first - it[0].first
                val dy = it[1].second - it[0].second
                val dz = it[1].third - it[0].third
                kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)
            }
            if (distances.any { it > 5.0 } && distances.any { it < 0.5 }) {
                incrementPattern(intavePatterns, "intave_mixed_teleport_distance")
                debugMessage("Intave mixed teleport distance pattern detected")
                if (getPattern(intavePatterns, "intave_mixed_teleport_distance") > 2) {
                    notifyDetection("Intave")
                }
            }
        }

        // Better Intave: specific timing sequences
        if (positionHistory.size >= 6) {
            val recentTimes = mutableListOf<Long>()
            repeat(6) { recentTimes.add(System.currentTimeMillis() - (it * 50)) }
            val intervals = recentTimes.windowed(2) { it[1] - it[0] }
            if (intervals.count { abs(it) in 40..60 } >= 3 && intervals.any { abs(it) > 100 }) {
                incrementPattern(intavePatterns, "intave_sequence_timing")
                debugMessage("Intave sequence timing pattern detected")
                if (getPattern(intavePatterns, "intave_sequence_timing") > 1) {
                    notifyDetection("Intave")
                }
            }
        }
        
        // Enhanced Vulcan detection - consistent teleport spacing
        if (timeDiff in 95..105 && positionHistory.size > 3) {
            val distances = positionHistory.windowed(2) { 
                val dx = it[1].first - it[0].first
                val dy = it[1].second - it[0].second
                val dz = it[1].third - it[0].third
                kotlin.math.sqrt(dx*dx + dy*dy + dz*dz)
            }
            if (distances.all { it < 0.1 }) {
                incrementPattern(vulcanPatterns, "vulcan_micro_teleport")
                debugMessage("Vulcan micro-teleport pattern detected")
                if (getPattern(vulcanPatterns, "vulcan_micro_teleport") > 2) {
                    notifyDetection("Vulcan")
                }
            }
        }
        
        // Original patterns with improvements
        if (timeDiff < 100 && positionHistory.size > 1) {
            incrementFlagPattern("rapid_teleport")
            if (getFlagPattern("rapid_teleport") > 4) {
                if (remoteIp.contains("mineplex", ignoreCase = true)) {
                    notifyDetection("Mineplex AntiCheat")
                } else {
                    notifyDetection("AAC")
                }
            }
        }
    }

    private fun handleVelocityPattern(packet: S12PacketEntityVelocity) {
        val currentTime = System.currentTimeMillis()
        val timeDiff = currentTime - lastVelocityTime
        lastVelocityTime = currentTime
        
        velocityTimings.add(currentTime)
        if (velocityTimings.size > 8) velocityTimings.removeAt(0)
        
        val velocity = Triple(packet.motionX, packet.motionY, packet.motionZ)
        velocityChecks.add(velocity)
        if (velocityChecks.size > 8) velocityChecks.removeAt(0)
        
        debugMessage("Velocity packet: X=${velocity.first}, Y=${velocity.second}, Z=${velocity.third}, TimeDiff=${timeDiff}ms")
        
        // Enhanced Grim VC detection
        if (velocity.first == 0 && velocity.third == 0 && velocity.second != 0) {
            consecutiveZeroVelocities++
            if (consecutiveZeroVelocities > 2) {
                incrementPattern(grimPatterns, "grim_vc_zero_horizontal")
                debugMessage("Grim VC zero horizontal velocity pattern detected")
                if (getPattern(grimPatterns, "grim_vc_zero_horizontal") > 1) {
                    notifyDetection("Grim VC")
                }
            }
        } else {
            consecutiveZeroVelocities = 0
        }
        
        // Enhanced Intave velocity detection
        if (abs(velocity.first) % 1000 == 0 && abs(velocity.third) % 1000 == 0 && velocity.second > 0) {
            incrementPattern(intavePatterns, "intave_rounded_velocity")
            debugMessage("Intave rounded velocity pattern detected")
            if (getPattern(intavePatterns, "intave_rounded_velocity") > 1) {
                notifyDetection("Intave")
            }
        }

        // Better Intave velocity patterns
        if (velocity.first == 0 && velocity.third == 0 && velocity.second % 500 == 0) {
            incrementPattern(intavePatterns, "intave_vertical_only_rounded")
            debugMessage("Intave vertical-only rounded velocity detected")
            if (getPattern(intavePatterns, "intave_vertical_only_rounded") > 1) {
                notifyDetection("Intave")
            }
        }

        // Better Intave: specific velocity ranges
        if ((abs(velocity.first) in 8000..12000 || abs(velocity.third) in 8000..12000) && velocity.second < 0) {
            incrementPattern(intavePatterns, "intave_specific_range")
            debugMessage("Intave specific velocity range detected")
            if (getPattern(intavePatterns, "intave_specific_range") > 1) {
                notifyDetection("Intave")
            }
        }

        // Better Intave: irregular velocity timing
        if (velocityTimings.size >= 4) {
            val intervals = velocityTimings.windowed(2) { it[1] - it[0] }
            if (intervals.any { it < 15 } && intervals.any { it > 200 } && intervals.any { it in 45..55 }) {
                incrementPattern(intavePatterns, "intave_mixed_velocity_timing")
                debugMessage("Intave mixed velocity timing detected")
                if (getPattern(intavePatterns, "intave_mixed_velocity_timing") > 2) {
                    notifyDetection("Intave")
                }
            }
        }
        
        // Enhanced Vulcan velocity detection
        if (velocity.first % 100 == 0 && velocity.third % 100 == 0 && velocity.second % 100 == 0) {
            incrementPattern(vulcanPatterns, "vulcan_all_rounded")
            debugMessage("Vulcan all-rounded velocity pattern detected")
            if (getPattern(vulcanPatterns, "vulcan_all_rounded") > 1) {
                notifyDetection("Vulcan")
            }
        }
        
        // Velocity timing analysis for Grim
        if (velocityTimings.size >= 3) {
            val intervals = velocityTimings.windowed(2) { it[1] - it[0] }
            if (intervals.all { it in 48..52 }) {
                incrementPattern(grimPatterns, "grim_precise_timing")
                debugMessage("Grim precise velocity timing detected")
                if (getPattern(grimPatterns, "grim_precise_timing") > 2) {
                    notifyDetection("Grim AC")
                }
            }
        }
        
        // Original patterns
        if (velocity.second > 8000 && velocity.first == 0 && velocity.third == 0) {
            incrementFlagPattern("vertical_only_velocity")
            if (getFlagPattern("vertical_only_velocity") > 2) {
                notifyDetection("Hypixel Watchdog")
            }
        }
    }

    private fun handleExplosionPattern(packet: S27PacketExplosion) {
        if (packet.field_149152_f == 0.0f && packet.field_149159_h == 0.0f) {
            incrementFlagPattern("zero_explosion_motion")
            
            // Enhanced detection for specific anticheats
            if (getPattern(grimPatterns, "grim_vc_zero_horizontal") > 0) {
                incrementPattern(grimPatterns, "grim_zero_explosion")
                if (getPattern(grimPatterns, "grim_zero_explosion") > 0) {
                    notifyDetection("Grim VC")
                }
            } else if (getFlagPattern("zero_explosion_motion") > 1) {
                notifyDetection("Vulcan/Matrix")
            }
        }
    }

    private fun handleMovementFlags(packet: C03PacketPlayer) {
        val player = mc.thePlayer ?: return
        
        if (player.motionY < -0.08 && player.fallDistance > 0.5f && !player.onGround && !player.capabilities.isFlying) {
            if (packet.onGround) {
                incrementFlagPattern("ground_spoof")
                
                // Enhanced Intave ground spoof detection
                if (player.fallDistance > 2.0f) {
                    incrementPattern(intavePatterns, "intave_ground_spoof")
                    if (getPattern(intavePatterns, "intave_ground_spoof") > 2) {
                        notifyDetection("Intave")
                    }
                }
                
                if (getFlagPattern("ground_spoof") > 3) {
                    notifyDetection("AAC/NCP")
                }
            }
        }
    }

    // Enhanced ACC detection patterns
    private fun detectACCPatterns(packet: Packet<*>) {
        if (!detectACCPatterns) return

        when (packet) {
            is S12PacketEntityVelocity -> {
                // ACC has specific velocity modification patterns
                val velocity = Triple(packet.motionX, packet.motionY, packet.motionZ)

                // ACC often reduces velocity by specific percentages
                if (abs(velocity.first) in 1000..3000 && abs(velocity.third) in 1000..3000) {
                    incrementPattern(accPatterns, "acc_reduced_velocity")
                    if (getPattern(accPatterns, "acc_reduced_velocity") > 2) {
                        notifyDetection("ACC")
                    }
                }

                // ACC velocity timing patterns
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastVelocityTime in 45..55) {
                    incrementPattern(accPatterns, "acc_timing_pattern")
                    if (getPattern(accPatterns, "acc_timing_pattern") > 3) {
                        notifyDetection("ACC")
                    }
                }
            }

            is S32PacketConfirmTransaction -> {
                // ACC transaction patterns
                if (packet.actionNumber.toInt() in 1..100) {
                    incrementPattern(accPatterns, "acc_transaction_range")
                    if (getPattern(accPatterns, "acc_transaction_range") > 5) {
                        notifyDetection("ACC")
                    }
                }
            }
        }
    }

    // Enhanced Vulcan detection patterns
    private fun detectVulcanPatterns(packet: Packet<*>) {
        if (!detectVulcanPatterns) return

        when (packet) {
            is S12PacketEntityVelocity -> {
                val velocity = Triple(packet.motionX, packet.motionY, packet.motionZ)

                // Vulcan specific velocity patterns
                if (velocity.first == 0 && velocity.third == 0 && velocity.second in 2000..4000) {
                    incrementPattern(vulcanPatterns, "vulcan_vertical_only")
                    if (getPattern(vulcanPatterns, "vulcan_vertical_only") > 2) {
                        notifyDetection("Vulcan")
                    }
                }

                // Vulcan velocity reduction patterns
                if (abs(velocity.first) < 1000 && abs(velocity.third) < 1000 && velocity.second > 0) {
                    incrementPattern(vulcanPatterns, "vulcan_reduced_horizontal")
                    if (getPattern(vulcanPatterns, "vulcan_reduced_horizontal") > 3) {
                        notifyDetection("Vulcan")
                    }
                }
            }

            is S08PacketPlayerPosLook -> {
                // Vulcan teleport patterns
                val currentTime = System.currentTimeMillis()
                if (currentTime - lastPosLookTime < 100) {
                    incrementPattern(vulcanPatterns, "vulcan_rapid_teleport")
                    if (getPattern(vulcanPatterns, "vulcan_rapid_teleport") > 1) {
                        notifyDetection("Vulcan")
                    }
                }
                lastPosLookTime = currentTime
            }
        }
    }

    // Enhanced Matrix detection patterns
    private fun detectMatrixPatterns(packet: Packet<*>) {
        if (!detectMatrixPatterns) return

        when (packet) {
            is S12PacketEntityVelocity -> {
                val velocity = Triple(packet.motionX, packet.motionY, packet.motionZ)

                // Matrix velocity patterns
                if (velocity.first % 100 == 0 && velocity.third % 100 == 0) {
                    incrementPattern(matrixPatterns, "matrix_rounded_velocity")
                    if (getPattern(matrixPatterns, "matrix_rounded_velocity") > 2) {
                        notifyDetection("Matrix")
                    }
                }

                // Matrix specific velocity ranges
                if (abs(velocity.first) in 500..1500 && abs(velocity.third) in 500..1500) {
                    incrementPattern(matrixPatterns, "matrix_velocity_range")
                    if (getPattern(matrixPatterns, "matrix_velocity_range") > 3) {
                        notifyDetection("Matrix")
                    }
                }
            }

            is S27PacketExplosion -> {
                // Matrix explosion handling
                if (packet.field_149152_f == 0.0f && packet.field_149159_h == 0.0f) {
                    incrementPattern(matrixPatterns, "matrix_zero_explosion")
                    if (getPattern(matrixPatterns, "matrix_zero_explosion") > 1) {
                        notifyDetection("Matrix")
                    }
                }
            }
        }
    }

    // Enhanced Karhu detection patterns
    private fun detectKarhuPatterns(packet: Packet<*>) {
        if (!detectKarhuPatterns) return

        when (packet) {
            is S12PacketEntityVelocity -> {
                val velocity = Triple(packet.motionX, packet.motionY, packet.motionZ)

                // Karhu velocity patterns
                if (velocity.first != 0 && velocity.third != 0 && velocity.second == 0) {
                    incrementPattern(karhuPatterns, "karhu_horizontal_only")
                    if (getPattern(karhuPatterns, "karhu_horizontal_only") > 2) {
                        notifyDetection("Karhu")
                    }
                }

                // Karhu specific velocity modifications
                if (abs(velocity.first) in 2000..6000 && abs(velocity.third) in 2000..6000) {
                    incrementPattern(karhuPatterns, "karhu_velocity_range")
                    if (getPattern(karhuPatterns, "karhu_velocity_range") > 3) {
                        notifyDetection("Karhu")
                    }
                }
            }
        }
    }

    // Enhanced Spartan detection patterns
    private fun detectSpartanPatterns(packet: Packet<*>) {
        if (!detectSpartanPatterns) return

        when (packet) {
            is S12PacketEntityVelocity -> {
                val velocity = Triple(packet.motionX, packet.motionY, packet.motionZ)

                // Spartan velocity patterns
                if (velocity.first == velocity.third && velocity.first != 0) {
                    incrementPattern(spartanPatterns, "spartan_equal_horizontal")
                    if (getPattern(spartanPatterns, "spartan_equal_horizontal") > 2) {
                        notifyDetection("Spartan")
                    }
                }

                // Spartan specific velocity values
                if (abs(velocity.first) == 4000 && abs(velocity.third) == 4000) {
                    incrementPattern(spartanPatterns, "spartan_4000_velocity")
                    if (getPattern(spartanPatterns, "spartan_4000_velocity") > 1) {
                        notifyDetection("Spartan")
                    }
                }
            }
        }
    }

    // Enhanced Verus detection patterns
    private fun detectVerusPatterns(packet: Packet<*>) {
        if (!detectVerusPatterns) return

        when (packet) {
            is S12PacketEntityVelocity -> {
                val velocity = Triple(packet.motionX, packet.motionY, packet.motionZ)

                // Verus velocity patterns
                if (velocity.first == 0 && velocity.third == 0 && velocity.second > 5000) {
                    incrementPattern(verusPatterns, "verus_high_vertical")
                    if (getPattern(verusPatterns, "verus_high_vertical") > 1) {
                        notifyDetection("Verus")
                    }
                }

                // Verus specific velocity modifications
                if (abs(velocity.first) < 500 && abs(velocity.third) < 500 && velocity.second > 0) {
                    incrementPattern(verusPatterns, "verus_low_horizontal")
                    if (getPattern(verusPatterns, "verus_low_horizontal") > 3) {
                        notifyDetection("Verus")
                    }
                }
            }
        }
    }

    private fun analyzeTransactionTiming() {
        if (transactionTimings.size >= 5) {
            val intervals = transactionTimings.windowed(2) { it[1] - it[0] }
            val avgInterval = intervals.average()
            
            // Grim has very consistent transaction timing
            if (intervals.all { abs(it - avgInterval) < 2 } && avgInterval in 48.0..52.0) {
                incrementPattern(grimPatterns, "grim_consistent_timing")
                debugMessage("Grim consistent transaction timing detected (avg: ${String.format("%.1f", avgInterval)}ms)")
                if (getPattern(grimPatterns, "grim_consistent_timing") > 3) {
                    notifyDetection("Grim AC")
                }
            }
            
            // Intave has irregular timing patterns
            if (intervals.any { it < 20 } && intervals.any { it > 100 }) {
                incrementPattern(intavePatterns, "intave_irregular_timing")
                debugMessage("Intave irregular transaction timing detected")
                if (getPattern(intavePatterns, "intave_irregular_timing") > 2) {
                    notifyDetection("Intave")
                }
            }
        }
    }

    val onTick = handler<GameTickEvent> {
        if (check && ticksPassed++ > 40) {
            notifyDetection("None").also { reset() }
        }
        
        if (reduceSpam && check) {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastPeriodicCheck > PERIODIC_CHECK_INTERVAL) {
                lastPeriodicCheck = currentTime
                performPeriodicCheck()
            }
        }
    }

    private fun handleTransaction(action: Int) {
        actionNumbers.add(action).also { if (debug) chat("ID: $action") }
        ticksPassed = 0
        if (actionNumbers.size >= 5) analyzeActionNumbers()
    }

    private fun analyzeActionNumbers() {
        val diffs = actionNumbers.windowed(2) { it[1] - it[0] }
        val first = actionNumbers.first()
        
        when {
            // Server-specific detection rules
            remoteIp.lowercase().contains("3fmc", true) || remoteIp.lowercase().contains("luckyvn", true) -> {
                debugMessage("3FMC/LuckyVN server detected - forcing Intave detection")
                notifyDetection("Intave")
            }

            remoteIp.lowercase().contains("heromc", true) -> {
                debugMessage("HeroMC server detected - dual anticheat system")
                notifyDetection("Vulcan + Grim VC")
            }

            remoteIp.lowercase().equals("hypixel.net", true) -> {
                debugMessage("Hypixel server detected via IP match")
                notifyDetection("Watchdog")
            }

            // Enhanced Grim detection
            isGrimPattern(diffs, first) -> {
                debugMessage("Enhanced Grim pattern detected")
                notifyDetection("Grim AC")
            }

            // Enhanced Intave detection
            isIntavePattern(diffs, first) -> {
                debugMessage("Enhanced Intave pattern detected")
                notifyDetection("Intave")
            }

            // Enhanced Vulcan detection
            isVulcanPattern(diffs, first) -> {
                debugMessage("Enhanced Vulcan pattern detected")
                notifyDetection("Vulcan")
            }
            
            // Original pattern analysis (simplified)
            diffs.all { it == diffs.first() } -> when (diffs.first()) {
                1 -> when (first) {
                    in -23772..-23762 -> notifyDetection("Vulcan")
                    in 95..105, in -20005..-19995 -> notifyDetection("Matrix")
                    in -32773..-32762 -> notifyDetection("Grizzly")
                    else -> notifyDetection("Verus")
                }
                -1 -> when {
                    first in -8287..-8280 -> notifyDetection("Errata")
                    first < -3000 -> notifyDetection("Intave")
                    first in -5..0 -> notifyDetection("Grim")
                    first in -3000..-2995 -> notifyDetection("Karhu")
                    else -> notifyDetection("Polar")
                }
                else -> null
            }
            
            else -> {
                debugMessage("No known pattern matched - marking as Unknown")
                notifyDetection("Unknown").also { if (debug) logNumbers() }
            }
        }
        reset()
    }

    private fun isGrimPattern(diffs: List<Int>, first: Int): Boolean {
        // Grim specific patterns
        return when {
            // Grim VC pattern: consistent -1 with specific first values
            diffs.all { it == -1 } && first in -10..5 -> true
            // Grim AC pattern: mixed increments with specific ranges
            actionNumbers.all { it in -100..100 } && diffs.any { it == 1 } && diffs.any { it == -1 } -> true
            // Grim timing pattern: numbers close to zero with small variations
            actionNumbers.all { abs(it) < 50 } && diffs.all { abs(it) <= 2 } -> true
            else -> false
        }
    }

    private fun isIntavePattern(diffs: List<Int>, first: Int): Boolean {
        // Enhanced Intave specific patterns
        return when {
            // Intave pattern: large negative numbers with irregular diffs
            first < -5000 && diffs.any { abs(it) > 1000 } -> true
            // Intave pattern: contains both very small and very large numbers
            actionNumbers.any { it > 1000 } && actionNumbers.any { it < -1000 } && actionNumbers.any { abs(it) < 10 } -> true
            // Intave pattern: specific sequence variations
            actionNumbers.size >= 4 && actionNumbers.take(2).all { it < -2000 } && actionNumbers.drop(2).all { it > -100 } -> true
            // Enhanced Intave pattern: alternating large negative and small positive
            actionNumbers.size >= 6 && actionNumbers.windowed(2).any { (a, b) -> a < -3000 && b in -50..50 } -> true
            // Enhanced Intave pattern: specific negative ranges with irregular jumps
            first in -8000..-6000 && diffs.any { it > 5000 } && diffs.any { it < -2000 } -> true
            // Enhanced Intave pattern: consistent negative base with random spikes
            actionNumbers.count { it < -1000 } >= 3 && actionNumbers.any { it > 500 } -> true
            // Enhanced Intave pattern: specific modulo patterns
            actionNumbers.any { it % 1337 == 0 } || actionNumbers.any { it % 2048 == 0 } -> true
            else -> false
        }
    }

    private fun isVulcanPattern(diffs: List<Int>, first: Int): Boolean {
        // Vulcan specific patterns
        return when {
            // Enhanced Vulcan pattern: specific ranges with consistent increments
            first in -25000..-20000 && diffs.all { it == 1 } -> true
            // Vulcan pattern: rounded numbers (multiples of 100)
            actionNumbers.all { it % 100 == 0 } && diffs.all { it > 0 } -> true
            // Vulcan pattern: specific negative range with small positive diffs
            actionNumbers.all { it in -30000..-20000 } && diffs.all { it in 1..5 } -> true
            else -> false
        }
    }

    private fun incrementPattern(patternMap: MutableMap<String, Int>, pattern: String) {
        patternMap[pattern] = (patternMap[pattern] ?: 0) + 1
        debugMessage("Pattern '$pattern' incremented to ${patternMap[pattern]}")
    }
    
    private fun getPattern(patternMap: Map<String, Int>, pattern: String): Int {
        return patternMap[pattern] ?: 0
    }

    private fun notifyDetection(anticheat: String) {
        if (quietMode) {
            debugMessage("Quiet mode: Detection suppressed for '$anticheat'")
            return
        }

        if (reduceSpam) {
            val currentTime = System.currentTimeMillis()
            val lastNotification = notificationCooldowns[anticheat] ?: 0L
            val cooldownPeriod = (spamDelaySeconds * 1000).toLong()

            if (currentTime - lastNotification < cooldownPeriod) {
                debugMessage("Notification for '$anticheat' suppressed due to cooldown (${spamDelaySeconds}s)")
                return
            }

            // Enhanced adaptive spam control
            if (enableAdaptiveSpamControl) {
                // Reset notification count every minute
                if (currentTime - lastNotificationMinute > 60000) {
                    notificationCount = 0
                    lastNotificationMinute = currentTime
                }

                // Check if we've exceeded the per-minute limit
                if (notificationCount >= maxNotificationsPerMinute) {
                    debugMessage("Adaptive spam control: Max notifications per minute reached ($maxNotificationsPerMinute)")
                    return
                }

                notificationCount++
            }

            notificationCooldowns[anticheat] = currentTime
        }
        
        detectedAnticheat = anticheat
        notify(anticheat)
        debugMessage("Anticheat detection notification sent: $anticheat")
    }
    
    private fun performPeriodicCheck() {
        debugMessage("Performing enhanced periodic anticheat check...")
        
        if (actionNumbers.size >= 3) {
            analyzeActionNumbers()
        }
        
        // Enhanced pattern checking
        grimPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "grim_precise_teleport", "grim_vc_zero_horizontal", "grim_consistent_timing" -> {
                    if (count > 1) notifyDetection("Grim AC")
                }
            }
        }
        
        intavePatterns.forEach { (pattern, count) ->
            when (pattern) {
                "intave_irregular_teleport", "intave_rounded_velocity", "intave_ground_spoof",
                "intave_vertical_only_rounded", "intave_specific_range", "intave_mixed_velocity_timing",
                "intave_mixed_teleport_distance", "intave_sequence_timing" -> {
                    if (count > 1) notifyDetection("Intave")
                }
            }
        }
        
        vulcanPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "vulcan_micro_teleport", "vulcan_all_rounded", "vulcan_vertical_only",
                "vulcan_reduced_horizontal", "vulcan_rapid_teleport" -> {
                    if (count > 1) notifyDetection("Vulcan")
                }
            }
        }

        // Enhanced anticheat pattern checking
        accPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "acc_reduced_velocity", "acc_timing_pattern", "acc_transaction_range" -> {
                    if (count > 2) notifyDetection("ACC")
                }
            }
        }

        matrixPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "matrix_rounded_velocity", "matrix_velocity_range", "matrix_zero_explosion" -> {
                    if (count > 1) notifyDetection("Matrix")
                }
            }
        }

        karhuPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "karhu_horizontal_only", "karhu_velocity_range" -> {
                    if (count > 2) notifyDetection("Karhu")
                }
            }
        }

        spartanPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "spartan_equal_horizontal", "spartan_4000_velocity" -> {
                    if (count > 1) notifyDetection("Spartan")
                }
            }
        }

        verusPatterns.forEach { (pattern, count) ->
            when (pattern) {
                "verus_high_vertical", "verus_low_horizontal" -> {
                    if (count > 1) notifyDetection("Verus")
                }
            }
        }

        // Advanced anticheat pattern checking
        if (detectAdvancedAnticheats) {
            kauraPatterns.forEach { (pattern, count) ->
                when (pattern) {
                    "kaura_precise_timing" -> {
                        if (count > 2) notifyDetection("Kaura AC")
                    }
                }
            }

            sparkyPatterns.forEach { (pattern, count) ->
                when (pattern) {
                    "sparky_sequence_pattern" -> {
                        if (count > 1) notifyDetection("Sparky AC")
                    }
                }
            }

            themisPatterns.forEach { (pattern, count) ->
                when (pattern) {
                    "themis_mixed_timing" -> {
                        if (count > 1) notifyDetection("Themis AC")
                    }
                }
            }

            negativityPatterns.forEach { (pattern, count) ->
                when (pattern) {
                    "negativity_high_frequency" -> {
                        if (count > 0) notifyDetection("Negativity AC")
                    }
                }
            }
        }

        // Server-specific periodic checks
        if (remoteIp.contains("3fmc", ignoreCase = true) || remoteIp.contains("luckyvn", ignoreCase = true)) {
            notifyDetection("Intave")
        } else if (remoteIp.contains("heromc", ignoreCase = true)) {
            notifyDetection("Vulcan + Grim VC")
        } else if (remoteIp.contains("hypixel", ignoreCase = true)) {
            notifyDetection("Watchdog")
        }
    }

    private fun notify(message: String) = hud.addNotification(
        Notification.informative(this, "Anticheat detected: $message", 3000L)
    )

    private fun incrementFlagPattern(pattern: String) {
        flagPatterns[pattern] = (flagPatterns[pattern] ?: 0) + 1
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
        debugMessage("Enhanced Patterns - Grim: ${grimPatterns.entries.joinToString()}")
        debugMessage("Enhanced Patterns - Intave: ${intavePatterns.entries.joinToString()}")
        debugMessage("Enhanced Patterns - Vulcan: ${vulcanPatterns.entries.joinToString()}")
        debugMessage("Advanced Patterns - Kaura: ${kauraPatterns.entries.joinToString()}")
        debugMessage("Advanced Patterns - Sparky: ${sparkyPatterns.entries.joinToString()}")
        debugMessage("Advanced Patterns - Themis: ${themisPatterns.entries.joinToString()}")
        debugMessage("Advanced Patterns - Negativity: ${negativityPatterns.entries.joinToString()}")
        debugMessage("Packet ID Sequence: ${packetIdSequence.takeLast(10).joinToString()}")
    }

    private fun reset() {
        actionNumbers.clear()
        ticksPassed = 0
        check = false
        consecutiveZeroVelocities = 0

        // Clear all pattern maps
        grimPatterns.clear()
        intavePatterns.clear()
        vulcanPatterns.clear()
        kauraPatterns.clear()
        sparkyPatterns.clear()
        themisPatterns.clear()
        negativityPatterns.clear()
        accPatterns.clear()
        matrixPatterns.clear()
        karhuPatterns.clear()
        spartanPatterns.clear()
        verusPatterns.clear()

        // Clear tracking data
        velocityChecks.clear()
        positionHistory.clear()
        flagPatterns.clear()
        velocityTimings.clear()
        transactionTimings.clear()
        packetIdSequence.clear()
        packetTimings.clear()

        if (reduceSpam) {
            val currentTime = System.currentTimeMillis()
            val cleanupThreshold = (spamDelaySeconds * 4 * 1000).toLong() // Keep entries for 4x the spam delay
            notificationCooldowns.entries.removeIf { (_, time) ->
                currentTime - time > cleanupThreshold
            }
        }

        debugMessage("AnticheatDetector reset - all patterns and tracking data cleared")
    }

    // Enhanced utility methods for better anticheat detection
    private fun analyzePacketFrequency() {
        val currentTime = System.currentTimeMillis()
        packetTimings.forEach { (packetId, timings) ->
            if (timings.size >= 5) {
                val intervals = timings.windowed(2) { it[1] - it[0] }
                val avgInterval = intervals.average()
                val variance = intervals.map { (it - avgInterval) * (it - avgInterval) }.average()

                // Detect suspiciously consistent timing (possible anticheat)
                if (variance < 1.0 && avgInterval in 45.0..55.0) {
                    when (packetId) {
                        PacketIDs.S32_CONFIRM_TRANSACTION -> {
                            incrementPattern(grimPatterns, "grim_consistent_transaction_timing")
                            if (getPattern(grimPatterns, "grim_consistent_transaction_timing") > 3) {
                                notifyDetection("Grim AC")
                            }
                        }
                        PacketIDs.S12_ENTITY_VELOCITY -> {
                            incrementPattern(vulcanPatterns, "vulcan_consistent_velocity_timing")
                            if (getPattern(vulcanPatterns, "vulcan_consistent_velocity_timing") > 2) {
                                notifyDetection("Vulcan")
                            }
                        }
                    }
                }
            }

            // Clean old timings (keep only last 10)
            if (timings.size > 10) {
                timings.removeAt(0)
            }
        }
    }

    private fun detectAdvancedPatterns() {
        // Cross-pattern analysis for more accurate detection
        val grimScore = grimPatterns.values.sum()
        val intaveScore = intavePatterns.values.sum()
        val vulcanScore = vulcanPatterns.values.sum()
        val accScore = accPatterns.values.sum()
        val matrixScore = matrixPatterns.values.sum()

        debugMessage("Pattern scores - Grim: $grimScore, Intave: $intaveScore, Vulcan: $vulcanScore, ACC: $accScore, Matrix: $matrixScore")

        // Multi-pattern detection for higher confidence
        when {
            grimScore >= 5 && vulcanScore >= 3 -> notifyDetection("Grim AC + Vulcan Hybrid")
            intaveScore >= 4 && accScore >= 3 -> notifyDetection("Intave + ACC Hybrid")
            matrixScore >= 3 && vulcanScore >= 2 -> notifyDetection("Matrix + Vulcan Hybrid")
            grimScore >= 8 -> notifyDetection("Grim AC (High Confidence)")
            intaveScore >= 6 -> notifyDetection("Intave (High Confidence)")
            vulcanScore >= 5 -> notifyDetection("Vulcan (High Confidence)")
            accScore >= 5 -> notifyDetection("ACC (High Confidence)")
            matrixScore >= 4 -> notifyDetection("Matrix (High Confidence)")
        }
    }

    private fun analyzeVelocitySequence() {
        if (velocityChecks.size >= 5) {
            val horizontalVelocities = velocityChecks.map { sqrt((it.first * it.first + it.third * it.third).toDouble()) }
            val verticalVelocities = velocityChecks.map { it.second.toDouble() }

            // Detect patterns in velocity sequences
            val horizontalPattern = horizontalVelocities.windowed(3) { window ->
                window[0] > window[1] && window[1] > window[2] // Decreasing pattern
            }.count { it }

            val verticalPattern = verticalVelocities.windowed(3) { window ->
                window.all { it == window[0] } // Consistent pattern
            }.count { it }

            if (horizontalPattern >= 2) {
                incrementPattern(grimPatterns, "grim_decreasing_velocity_sequence")
                if (getPattern(grimPatterns, "grim_decreasing_velocity_sequence") > 1) {
                    notifyDetection("Grim AC")
                }
            }

            if (verticalPattern >= 2) {
                incrementPattern(vulcanPatterns, "vulcan_consistent_vertical_sequence")
                if (getPattern(vulcanPatterns, "vulcan_consistent_vertical_sequence") > 1) {
                    notifyDetection("Vulcan")
                }
            }
        }
    }

    private fun detectServerSpecificPatterns() {
        val serverIP = mc.currentServerData?.serverIP?.lowercase() ?: return

        // Server-specific anticheat detection
        when {
            serverIP.contains("hypixel") -> {
                // Hypixel Watchdog patterns
                if (velocityChecks.any { it.first == 0 && it.third == 0 && it.second > 0 }) {
                    incrementPattern(grimPatterns, "hypixel_watchdog_vertical_only")
                    if (getPattern(grimPatterns, "hypixel_watchdog_vertical_only") > 2) {
                        notifyDetection("Hypixel Watchdog")
                    }
                }
            }
            serverIP.contains("minemen") -> {
                // Minemen anticheat patterns
                if (actionNumbers.any { it in -1000..-500 }) {
                    incrementPattern(accPatterns, "minemen_acc_pattern")
                    if (getPattern(accPatterns, "minemen_acc_pattern") > 2) {
                        notifyDetection("Minemen ACC")
                    }
                }
            }
            serverIP.contains("pvpland") || serverIP.contains("pvp.land") -> {
                // PvPLand Vulcan patterns
                if (velocityChecks.any { abs(it.first) < 1000 && abs(it.third) < 1000 }) {
                    incrementPattern(vulcanPatterns, "pvpland_vulcan_pattern")
                    if (getPattern(vulcanPatterns, "pvpland_vulcan_pattern") > 2) {
                        notifyDetection("PvPLand Vulcan")
                    }
                }
            }
        }
    }

    // Debug command for manual testing
    fun printDetectionReport() {
        if (debug) {
            debugMessage("=== AnticheatDetector Report ===")
            debugMessage("Detected Anticheat: ${detectedAnticheat ?: "None"}")
            debugMessage("Pattern counts - Grim: ${grimPatterns.values.sum()}, Intave: ${intavePatterns.values.sum()}")
            debugMessage("Vulcan: ${vulcanPatterns.values.sum()}, ACC: ${accPatterns.values.sum()}")
            debugMessage("Matrix: ${matrixPatterns.values.sum()}, Karhu: ${karhuPatterns.values.sum()}")
            debugMessage("Packets analyzed - Velocity: ${velocityChecks.size}, Transactions: ${transactionTimings.size}")
        }
    }

    override fun onEnable() = reset()
    
    override fun onDisable() {
        reset()
        notificationCooldowns.clear()
        flagPatterns.clear()
        grimPatterns.clear()
        intavePatterns.clear()
        vulcanPatterns.clear()
        kauraPatterns.clear()
        sparkyPatterns.clear()
        themisPatterns.clear()
        negativityPatterns.clear()
        velocityChecks.clear()
        positionHistory.clear()
        velocityTimings.clear()
        transactionTimings.clear()
        packetIdSequence.clear()
        packetTimings.clear()
        debugMessage("Enhanced AnticheatDetector with packet ID support disabled, all data cleared")
    }
}