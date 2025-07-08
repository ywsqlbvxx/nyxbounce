package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce.moduleManager
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.settings.GameSettings
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object Velocity2 : Module("Velocity2", Category.COMBAT) {
    // Settings
    private val veloMode =
        ListValue("Mode", arrayOf("IntaveA", "IntaveB", "Jump", "Polar"), "IntaveA")
    private val intaveFactor = FloatValue("IntaveAFactor", 0.6f, 0f..1f)
    private val noFire = BoolValue("noFire", true)
    private val noFluid = BoolValue("NoFluid", true)
    private val noVehicle = BoolValue("NoVehicle", true)
    private val invCheck = BoolValue("InvCheck", true)
    private val onlyAura = BoolValue("OnlyAura", false)
    private val debugger = BoolValue("Debugger", false)

    // IntaveA
    private var hasReceivedVelocity = false
    private var intaveDamageTick = 0
    private var lastAttackTime = 0L
    private var intaveTick = 0

    // IntaveB
    private var blockVelocity = false
    private var isWorking = false
    private var hitable = false

    // Polar
    private var hurtTime = (8..9).random()

    // Movement
    private val forwardTimer = MSTimer()
    private val jumpTimer = MSTimer()
    private var jumped = false
    private var forward = false

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        if (cancelVelocity()) return
        var player = mc.thePlayer
        when (veloMode.get()) {
            "IntaveA" -> handleIntaveA()
            "IntaveB" -> handleIntaveB()
            "Jump" -> {

                if (player.hurtTime == 9) {
                    tryJump()
                }
            }
            "Polar" -> {
                if (player.hurtTime == hurtTime) {
                    tryJump()
                    hurtTime = getRandomInRange(8, 9).toInt()
                }
            }
        }

        updateMovementState()
    }

    @EventTarget
    fun onAttack(event: AttackEvent) {
        if (cancelVelocity()) return

        when (veloMode.get()) {
            "IntaveA" -> handleIntaveAAttack()
            "IntaveB" -> handleIntaveBAttack()
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        val packet = event.packet
        val player = mc.thePlayer ?: return

        when (veloMode.get()) {
            "IntaveB" -> {
                if (isEntityS12(player, packet)) {
                    isWorking = true
                }
            }

        if (packet is S12PacketEntityVelocity && isEntityS12(player, packet)) {
            hasReceivedVelocity = true
            if (packet !is S27PacketExplosion) {
                event.cancelEvent()
            }
        }
    }


    override fun onDisable() {
        resetKeyStates()
    }

    private fun handleIntaveAAttack() {
        var player = mc.thePlayer
        if (player == null || cancelVelocity()) return
        if (player.hurtTime == 9 && (System.currentTimeMillis() - lastAttackTime) <= 8000) {
            var f = intaveFactor.get()
            player.motionX *= f
            player.motionZ *= f
            if (debugger.get()) chat("Reduced")
        }
        lastAttackTime = System.currentTimeMillis()
    }

    private fun handleIntaveA() {
        if (hasReceivedVelocity) {
            intaveTick++
            if (mc.thePlayer.hurtTime == 2) {
                intaveDamageTick++
                if (mc.thePlayer.onGround && intaveTick % 2 == 0 && intaveDamageTick <= 10) {
                    tryJump()
                    intaveTick = 0
                } else {
                    intaveDamageTick = 0
                }
                hasReceivedVelocity = false
            }
        }
    }

    private fun handleIntaveB() {
        var player = mc.thePlayer
        if (player == null || cancelVelocity()) return
        var target = getClosestEntity()
        target?.let { hitable = isCrosshairOnEntity(it) }
        blockVelocity = true
        if (hitable) {
            if (player.hurtTime == 9 && !player.isBurning) {
                tryJump()
            }
            if (player.hurtTime > 0) {
                tryForward()
            }
        }
    }

    private fun handleIntaveBAttack() {
        if (hitable && mc.thePlayer.hurtTime > 0 && blockVelocity) {
            mc.thePlayer.isSprinting = false
            if (mc.thePlayer.hurtTime <= 6 && isWorking) {
                val yaw = MathHelper.wrapAngleTo180_float(RotationUtils.serverRotation.yaw)
                mc.thePlayer.motionX = -sin(yaw.toDouble()) * 0.02
                mc.thePlayer.motionZ = cos(yaw.toDouble()) * 0.02
                if (debugger.get()) chat("[Velocity] Reduced")
                isWorking = false
            }
            blockVelocity = false
        }
    }

    private fun processSavedPackets() {
        while (savePackets.isNotEmpty()) {
            val packet = savePackets.poll()
            mc.netHandler.addToSendQueue(packet)
        }
    }

    private fun isPlayerPacket(packet: S12PacketEntityVelocity): Boolean {
        return packet.entityID == mc.thePlayer.entityId
    }

    private fun getRandomInRange(min: Int, max: Int): Double =
        Random().nextDouble() * (max - min) + min

    private fun isEntityS12(entity: Entity, packet: Packet<*>): Boolean {
        return (packet as? S12PacketEntityVelocity)?.let { s12 ->
            s12.entityID == entity.entityId && !isFallMotion(s12)
        } == true
    }

    private fun isFallMotion(packet: S12PacketEntityVelocity): Boolean {
        val epsilon = 1e-5
        return (abs(packet.motionX / 8000.0) < epsilon) &&
               (abs(packet.motionY / 8000.0) < epsilon) &&
               (abs(packet.motionZ / 8000.0 + 0.078375) < epsilon)
    }

    private fun getClosestEntity(): EntityPlayer? {
        return mc.theWorld?.loadedEntityList
            ?.filterIsInstance<EntityPlayer>()
            ?.filter { it != mc.thePlayer && it.isEntityAlive }
            ?.minByOrNull { mc.thePlayer.getDistanceToEntity(it) }
    }

    private fun isCrosshairOnEntity(target: Entity): Boolean {
        val player = mc.thePlayer ?: return false
        val rotation = RotationUtils.serverRotation

        return target.entityBoundingBox.expand(
            target.collisionBorderSize.toDouble(),
            target.collisionBorderSize.toDouble(),
            target.collisionBorderSize.toDouble()
        ).let { bb ->
            val lookVec = getVectorForRotation(rotation.pitch, rotation.yaw)
            val eyesPos = player.getPositionEyes(1f)
            val reach = player.getDistanceToEntity(target) + 0.5
            val rayEnd = eyesPos.addVector(lookVec.xCoord * reach, lookVec.yCoord * reach, lookVec.zCoord * reach)
            bb.calculateIntercept(eyesPos, rayEnd) != null
        }
    }

    private fun cancelVelocity(): Boolean {
        val player = mc.thePlayer ?: return true

        return listOfNotNull(
            noFire.get() to player.isBurning.let { it as Boolean },
            noFluid.get() to (player.isInWater as Boolean
                            || player.isInLava as Boolean
                            || player.isInWeb as Boolean),
            noVehicle.get() to player.isRiding!!,
            invCheck.get() to (mc.currentScreen != null && mc.currentScreen !is GuiChat),
            moduleManager.getModule("KillAura")?.let {
                onlyAura.get() to !it.isActive
            } ?: (onlyAura.get() to false)  
        ).any { (condition, state) -> condition && state }
    }

    private fun tryJump() {
        mc.gameSettings.keyBindJump.pressed = true
        jumpTimer.reset()
        jumped = true
    }

    private fun tryForward() {
        mc.gameSettings.keyBindForward.pressed = true
        forwardTimer.reset()
        forward = true
    }

    private fun getVectorForRotation(pitch: Float, yaw: Float): Vec3 {
        val yawRad = Math.toRadians(-yaw.toDouble()) - Math.PI
        val pitchRad = Math.toRadians(-pitch.toDouble())

        return Vec3(
            sin(yawRad) * cos(pitchRad),
            -sin(pitchRad),
            cos(yawRad) * cos(pitchRad)
        )
    }

    private fun updateMovementState() {
        if (jumped && jumpTimer.hasTimePassed(100)) {
            mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
            jumped = false
        }
        if (forward && forwardTimer.hasTimePassed(100)) {
            mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
            forward = false
        }
    }

    private fun resetKeyStates() {
        mc.gameSettings.keyBindJump.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindJump)
        mc.gameSettings.keyBindForward.pressed = GameSettings.isKeyDown(mc.gameSettings.keyBindForward)
    }
    override val tag
        get() = veloMode.get()
}