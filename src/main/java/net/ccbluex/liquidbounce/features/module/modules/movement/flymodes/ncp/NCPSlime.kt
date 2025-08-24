package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.ncp

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.NotifyType
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.rayTraceWithCustomRotation
import net.ccbluex.liquidbounce.utils.timer.MSTimer
import net.ccbluex.liquidbounce.features.value.BoolValue
import net.ccbluex.liquidbounce.features.value.ListValue
import net.minecraft.block.BlockAir
import net.minecraft.item.ItemBlock
import net.minecraft.network.Packet
import net.minecraft.network.play.INetHandlerPlayServer
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import java.util.*

class NCPSlimeFly : FlyMode("NCPSlime") {

    private val timerBoostValue = BoolValue("${valuePrefix}DoTimer", true)
    private val swingModeValue = ListValue("${valuePrefix}SwingMode", arrayOf("Normal","Packet"), "Normal")

    private var stage = Stage.WAITING
    private var ticks = 0
    private var packets = 0
    private var firstLaunch = true
    private var needReset = false
    private var vanillaBypass = 0
    private var test = 1.0

    private val timer = MSTimer()
    private val packetBuffer = LinkedList<Packet<INetHandlerPlayServer>>()

    override fun onEnable() {
        test = 1.0
        needReset = false
        firstLaunch = true
        vanillaBypass = 0
        packets = 0
        ticks = 0
        packetBuffer.clear()
        timer.reset()

        stage = if (mc.thePlayer.onGround) {
            mc.thePlayer.jump()
            Stage.WAITING
        } else Stage.INFFLYING
    }

    override fun onWorld(event: WorldEvent) {
        packetBuffer.clear()
        timer.reset()
    }

    override fun onPacket(event: PacketEvent) {
        if (stage == Stage.WAITING) return
        val packet = event.packet

        when (packet) {
            is C03PacketPlayer -> {
                packet.onGround = true
                packetBuffer.add(packet)
                event.cancelEvent()
            }
            is S12PacketEntityVelocity -> {
                if (mc.thePlayer == null || mc.theWorld?.getEntityByID(packet.entityID) != mc.thePlayer) return
                event.cancelEvent()
            }
        }
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1.0f
        packetBuffer.forEach { PacketUtils.sendPacketNoEvent(it) }
        packetBuffer.clear()
    }

    override fun onUpdate(event: UpdateEvent) {
        if (timer.hasTimePassed((Math.random() * 1000).toLong())) {
            timer.reset()
            packetBuffer.forEach { PacketUtils.sendPacketNoEvent(it) }
            packetBuffer.clear()
        }

        when (stage) {
            Stage.WAITING -> handleWaitingStage()
            Stage.FLYING, Stage.INFFLYING -> handleFlyingStage()
        }
    }

    private fun handleWaitingStage() {
        if (mc.thePlayer.posY < fly.launchY + 0.8) return

        RotationUtils.setTargetRotation(Rotation(mc.thePlayer.rotationYaw, 90f))
        val trace = mc.thePlayer.rayTraceWithCustomRotation(4.5, mc.thePlayer.rotationYaw, 90f)
        trace ?: return

        if (mc.thePlayer.onGround) {
            if (trace.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return
            val blockPos = trace.blockPos
            val enumFacing = trace.sideHit
            if (mc.playerController.onPlayerDamageBlock(blockPos, enumFacing)) stage = Stage.FLYING
            mc.thePlayer.motionY = 0.0
        } else {
            placeSlimeBlock(trace)
        }
    }

    private fun placeSlimeBlock(trace: MovingObjectPosition) {
        var slot = -1
        for (i in 0..8) {
            val stack = mc.thePlayer.inventory.getStackInSlot(i)
            if (stack?.item is ItemBlock) {
                slot = PlayerUtils.findSlimeBlock() ?: -1
                break
            }
        }

        if (slot == -1) {
            fly.state = false
            LiquidBounce.hud.addNotification(
                Notification(
                    "NCPSlimeFly",
                    "You need a slime block to use this fly",
                    NotifyType.ERROR,
                    1000
                )
            )
            return
        }

        val oldSlot = mc.thePlayer.inventory.currentItem
        mc.thePlayer.inventory.currentItem = slot

        val blockPos = trace.blockPos
        val enumFacing = trace.sideHit
        val hitVec: Vec3 = trace.hitVec

        if (mc.playerController.onPlayerRightClick(mc.thePlayer, mc.theWorld, mc.thePlayer.heldItem, blockPos, enumFacing, hitVec)) {
            when (swingModeValue.get().lowercase(Locale.getDefault())) {
                "normal" -> mc.thePlayer.swingItem()
                "packet" -> mc.netHandler.addToSendQueue(C0APacketAnimation())
            }
        }

        mc.thePlayer.inventory.currentItem = oldSlot
    }

    private fun handleFlyingStage() {
        if (timerBoostValue.get()) {
            ticks++
            mc.timer.timerSpeed = when (ticks) {
                in 1..10 -> 2f
                in 10..15 -> 0.4f
                else -> {
                    ticks = 0
                    0.6f
                }
            }
        } else mc.timer.timerSpeed = 1.0f
    }

    override fun onBlockBB(event: BlockBBEvent) {
        if (event.block !is BlockAir) return
        val topY = when (stage) {
            Stage.WAITING -> fly.launchY
            Stage.FLYING -> fly.launchY
            Stage.INFFLYING -> mc.thePlayer.posY
        }
        if (event.y <= topY) {
            event.boundingBox = AxisAlignedBB.fromBounds(event.x.toDouble(), event.y.toDouble(), event.z.toDouble(), event.x + 1.0, fly.launchY, event.z + 1.0)
        }
    }

    override fun onJump(event: JumpEvent) {
        if (stage != Stage.WAITING) event.cancelEvent()
    }

    override fun onStep(event: StepEvent) {
        if (stage != Stage.WAITING) event.stepHeight = 0.0f
    }

    enum class Stage {
        WAITING,
        FLYING,
        INFFLYING
    }
}

