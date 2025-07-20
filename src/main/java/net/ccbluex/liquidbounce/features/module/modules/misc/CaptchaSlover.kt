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
	private val mode by choices("Mode", arrayOf("3fmc"), "3fmc")

	private val ocrApiUrl = "https://60f254355a67.ngrok-free.app/ocr"

	private var lastMapData: ByteArray? = null

	val onPacket = handler<PacketEvent> { event ->
		if (mode != "3fmc") return@handler
		if (event.eventType != EventState.RECEIVE) return@handler
		val packet = event.packet
		if (packet is S34PacketMaps) {
			val mapData = packet.mapData
			if (mapData != null && !mapData.contentEquals(lastMapData)) {
				lastMapData = mapData
				GlobalScope.launch(Dispatchers.IO) {
					try {
						// Convert byte array to JSON array string
						val dataString = mapData.joinToString(",", prefix = "[", postfix = "]") { it.toUByte().toString() }
						val json = "{" + "\"image\":$dataString" + "}"
						val url = URL(ocrApiUrl)
						val conn = url.openConnection() as HttpURLConnection
						conn.requestMethod = "POST"
						conn.setRequestProperty("Content-Type", "application/json")
						conn.doOutput = true
						conn.outputStream.use { it.write(json.toByteArray()) }
						val response = conn.inputStream.bufferedReader().readText()
						val result = Regex("\\"result\\"\s*:\s*\\"(.*?)\\"").find(response)?.groupValues?.getOrNull(1)
						if (!result.isNullOrBlank()) {
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
}
