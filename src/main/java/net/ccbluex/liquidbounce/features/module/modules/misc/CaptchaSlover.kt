package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.io.post
import net.minecraft.item.ItemMap
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NBTTagCompound
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody
import org.json.JSONObject

class CaptchaSlover : Module("CaptchaSlover", Category.MISC) {
	private var lastMapData: String? = null
	private var fetchJob: Job? = null


	override fun onEnable() {
		super.onEnable()
		fetchJob = CoroutineScope(Dispatchers.Default).launch {
			while (isActive) {
				withContext(Dispatchers.Main) {
					if (isOn3fmc()) {
						solveCaptchaIfNeeded()
					}
				}
				delay(1000)
			}
		}
	}

	override fun onDisable() {
		super.onDisable()
		fetchJob?.cancel()
		fetchJob = null
	}

	private fun isOn3fmc(): Boolean {
		val server = MinecraftInstance.mc.currentServerData?.serverIP ?: return false
		return server.contains("3fmc.com", ignoreCase = true)
	}

	private fun solveCaptchaIfNeeded() {
		val player = MinecraftInstance.mc.thePlayer ?: return
		val stack: ItemStack? = player.inventory.mainInventory.getOrNull(0)
		if (stack != null && stack.item is ItemMap) {
			println("[CaptchaSlover] Debug: fetching map...")
			val nbt: NBTTagCompound? = stack.tagCompound
			val mapData: ByteArray? = nbt?.getByteArray("data")
			if (mapData != null) {
				val dataString = mapData.joinToString(",")
				if (dataString == lastMapData) return
				lastMapData = dataString
				println("[CaptchaSlover] Debug: sent to api ...")
				CoroutineScope(Dispatchers.IO).launch {
					try {
						val json = JSONObject()
						json.put("image", dataString)
						val body = RequestBody.create("application/json".toMediaTypeOrNull(), json.toString())
						val response = HttpClient.post("https://60f254355a67.ngrok-free.app/ocr", body)
						val responseBody = response.body?.string()
						val result = JSONObject(responseBody).optString("result", "")
						if (result.isNotBlank()) {
							withContext(Dispatchers.Main) {
								MinecraftInstance.mc.thePlayer.sendChatMessage(result)
							}
						}
					} catch (e: Exception) {
					}
				}
			}
		}
	}
}
