package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.BlinkUtils
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement

object HMCBlinkFly : Module("HMCBlinkFly", Category.MOVEMENT) {

    val visibleLimitValue by int("VisibleLimit", 4, 0..10)
    val flySpeedValue by float("FlySpeed", 1.0f, 0.1f..3.0f)

    private var placedCount = 0

    init {
        EventManager.registerEventHook(PacketEvent::class.java, EventHook(this, false, 0) { event ->
            val packet = event.packet

            if (mc.thePlayer == null || mc.thePlayer!!.isDead) return@EventHook

            if (event.eventType == EventState.SEND) {
                when (packet) {
                    is C08PacketPlayerBlockPlacement -> {
                        if (placedCount < visibleLimitValue) {
                            placedCount++
                        } else {
                            BlinkUtils.blink(packet, event, true, false)
                        }
                    }
                }
            }

            if (event.eventType == EventState.RECEIVE) {
                if (isServerPacket(packet) && !isEntityMovementPacket(packet)) {
                    BlinkUtils.blink(packet, event, false, true)
                }
            }
        })

        EventManager.registerEventHook(MotionEvent::class.java, EventHook(this, false, 0) { event ->
            val thePlayer = mc.thePlayer ?: return@EventHook

            if (event.eventState == EventState.PRE) {
                val flySpeed = flySpeedValue

                if (thePlayer.onGround) {
                    thePlayer.motionY = flySpeed.toDouble()
                } else {
                    thePlayer.motionY = -0.05
                }

                thePlayer.motionX *= 1.0f + (flySpeed / 10f)
                thePlayer.motionZ *= 1.0f + (flySpeed / 10f)
            }
        })

        EventManager.registerEventHook(MotionEvent::class.java, EventHook(this, false, 0) { event ->
            if (event.eventState == EventState.POST) {
                val thePlayer = mc.thePlayer ?: return@EventHook
                if (thePlayer.isDead || thePlayer.ticksExisted <= 10) {
                    BlinkUtils.unblink()
                } else {
                    BlinkUtils.syncReceived()
                }
            }
        })
    }

    override fun onEnable() {
        placedCount = 0
    }

    override fun onDisable() {
        BlinkUtils.unblink()
    }

    override val tag: String
        get() = "V:${visibleLimitValue} S:${flySpeedValue}"

    private fun isServerPacket(packet: Any): Boolean {
        return packet.javaClass.simpleName.startsWith("S")
    }

    private fun isEntityMovementPacket(packet: Any): Boolean {
        return when (packet) {
            is net.minecraft.network.play.server.S14PacketEntity,
            is net.minecraft.network.play.server.S18PacketEntityTeleport,
            is net.minecraft.network.play.server.S19PacketEntityHeadLook,
            is net.minecraft.network.play.server.S0BPacketAnimation,
            is net.minecraft.network.play.server.S0CPacketSpawnPlayer,
            is net.minecraft.network.play.server.S1CPacketEntityMetadata -> true
            else -> {
                val name = packet.javaClass.simpleName
                name == "S15PacketEntityRelMove" ||
                        name == "S17PacketEntityLookMove" ||
                        name == "S16PacketEntityLook"
            }
        }
    }
}