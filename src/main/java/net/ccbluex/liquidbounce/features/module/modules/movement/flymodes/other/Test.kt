package net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.other

import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.flymodes.FlyMode
import net.minecraft.network.play.server.S27PacketExplosion

class Test : FlyMode("Test") {
	private var velocityPacket = false

	override fun onEnable() {
		velocityPacket = false
	}

	override fun onUpdate(event: UpdateEvent) {
		if (velocityPacket) {
			val player = mc.thePlayer ?: return
			player.setPositionAndRotation(
				player.posX + 50,
				player.posY,
				player.posZ + 50,
				player.rotationYaw,
				player.rotationPitch
			)
			velocityPacket = false
		}
	}

	override fun onPacket(event: PacketEvent) {
		val packet = event.packet
		if (packet is S27PacketExplosion) {
			velocityPacket = true
		}
	}
}
