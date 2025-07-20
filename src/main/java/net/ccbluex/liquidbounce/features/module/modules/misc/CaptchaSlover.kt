package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.play.server.S34PacketMaps
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.Base64

object CaptchaSlover : Module("CaptchaSlover", Category.MISC) {
	private var hotbarJob: Job? = null
	private val mode by choices("Mode", arrayOf("3fmc"), "3fmc")

	private val ocrApiUrl = "https://60f254355a67.ngrok-free.app/ocr"

	private var lastMapData: ByteArray? = null

	val onPacket = handler<PacketEvent> { event ->
		if (mode != "3fmc") return@handler
		if (event.eventType != EventState.RECEIVE) return@handler
		val packet = event.packet
		if (packet is S34PacketMaps) {
			val mapData = try {
				val field = S34PacketMaps::class.java.getDeclaredField("mapDataBytes")
				field.isAccessible = true
				field.get(packet) as? ByteArray
			} catch (e: Exception) {
				null
			}
			if (mapData != null && (lastMapData == null || !mapData.contentEquals(lastMapData!!))) {
				lastMapData = mapData.copyOf()
				GlobalScope.launch(Dispatchers.IO) {
					try {
						val dataString = mapData.joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
						val json = "{" + "\"image\":$dataString" + "}"
						val url = URL(ocrApiUrl)
						val conn = url.openConnection() as HttpURLConnection
						conn.requestMethod = "POST"
						conn.setRequestProperty("Content-Type", "application/json")
						conn.doOutput = true
						conn.outputStream.use { it.write(json.toByteArray()) }
						val response = conn.inputStream.bufferedReader().readText()
						val regex = "\\"result\\"\\s*:\\s*\\"(.*?)\\"".toRegex()
						val result = regex.find(response)?.groupValues?.getOrNull(1)
						if (!result.isNullOrEmpty()) {
							withContext(Dispatchers.Main) {
								mc.thePlayer?.sendChatMessage(result)
							}
						}
					} catch (e: Exception) {
					}
				}
			}
		}
	}

	override fun onEnable() {
		super.onEnable()
		if (mode != "3fmc") return
		hotbarJob = GlobalScope.launch(Dispatchers.IO) {
			while (state) {
				try {
					val player = mc.thePlayer ?: continue
					val inventory = player.inventory ?: continue
					for (slot in 0..8) {
						val stack = inventory.getStackInSlot(slot) ?: continue
						val item = stack.item ?: continue
						if (item.unlocalizedName?.contains("filled_map") == true || item.unlocalizedName?.contains("map") == true) {
							val nbt = stack.tagCompound
							val mapData: ByteArray? = try {
								nbt?.getByteArray("data")
							} catch (e: Exception) { null }
							if (mapData != null && (lastMapData == null || !mapData.contentEquals(lastMapData!!))) {
								lastMapData = mapData.copyOf()
								val dataString = mapData.joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
								val json = "{" + "\"image\":$dataString" + "}"
								try {
									val url = URL(ocrApiUrl)
									val conn = url.openConnection() as HttpURLConnection
									conn.requestMethod = "POST"
									conn.setRequestProperty("Content-Type", "application/json")
									conn.doOutput = true
									conn.outputStream.use { it.write(json.toByteArray()) }
									val response = conn.inputStream.bufferedReader().readText()
									val regex = "\\"result\\"\\s*:\\s*\\"(.*?)\\"".toRegex()
									val result = regex.find(response)?.groupValues?.getOrNull(1)
									if (!result.isNullOrEmpty()) {
										withContext(Dispatchers.Main) {
											mc.thePlayer?.sendChatMessage(result)
										}
									}
								} catch (e: Exception) {}
							}
							break 
						}
					}
				} catch (e: Exception) {}
				delay(1200L) 
			}
		}
	}

	override fun onDisable() {
		super.onDisable()
		hotbarJob?.cancel()
		hotbarJob = null
	}
}
