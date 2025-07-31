/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.checks.CheckManager
import net.ccbluex.liquidbounce.config.BoolValue
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.util.Vec3

object HeckerDetector : Module("HeckerDetector", Category.MISC) {

    private val debug = BoolValue("Debug", false)
    private val announcePlayers = BoolValue("AnnouncePlayers", true)
    private val checkYourself = BoolValue("CheckYourself", false)

    override fun onEnable() {
        CheckManager.cleanup()
    }

    override fun onDisable() {
        CheckManager.cleanup()
    }

    private val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        mc.theWorld?.playerEntities?.forEach { player ->
            if (player is EntityOtherPlayerMP && (player != thePlayer || checkYourself.get())) {
                CheckManager.update(player)
                
                if (announcePlayers.get()) {
                    val vl = CheckManager.getViolationLevel(player.name)
                    if (vl > 0) {
                        CheckManager.getAllChecks(player.name)?.forEach { check ->
                            if (check.wasFailed()) {
                                val message = if (debug.get())
                                    "§7[§9Debug§7] §f${player.name} §7- §c${check.name}: ${check.description()} (VL: $vl)"
                                else
                                    "§c[HeckerDetector] §f${player.name} §cmay be using ${check.name} (VL: $vl)"
                                chat(message)
                            }
                        }
                    }
                }
            }
        }
    }

    private val onPacket = handler<PacketEvent> { event ->
        if (!announcePlayers.get()) return@handler
        
        when (val packet = event.packet) {
            is S12PacketEntityVelocity -> {
                mc.theWorld?.getEntityByID(packet.entityID)?.let { entity ->
                    if (entity is EntityOtherPlayerMP) {
                        CheckManager.handleVelocityPacket(
                            entity,
                            packet.entityID,
                            Vec3(packet.motionX.toDouble() / 8000.0, packet.motionY.toDouble() / 8000.0, packet.motionZ.toDouble() / 8000.0)
                        )
                    }
                }
            }
            is S27PacketExplosion -> {
                mc.theWorld?.playerEntities?.forEach { player ->
                    if (player is EntityOtherPlayerMP) {
                        CheckManager.handleExplosion(
                            player, 
                            Vec3(packet.func_149149_c().toDouble(), packet.func_149144_d().toDouble(), packet.func_149147_e().toDouble())
                        )
                    }
                }
            }
        }
    }

    fun onCheckFlag(playerName: String, checkName: String, verbose: String) {
        if (debug.get()) {
            chat("§7[§9Debug§7] §f$playerName §7- §c$checkName: $verbose")
        } else {
            chat("§c[HeckerDetector] §f$playerName §c$verbose")
        }
    }
}
