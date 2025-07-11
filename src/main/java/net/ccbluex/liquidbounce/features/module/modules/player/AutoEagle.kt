/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.block.block
import net.ccbluex.liquidbounce.utils.timing.TickTimer
import net.minecraft.client.settings.GameSettings
import net.minecraft.init.Blocks.air
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3

object AutoEagle : Module("AutoEagle", Category.PLAYER) {

    private val maxSneakTime by intRange("MaxSneakTime", 1..5, 0..20)
    private val distanceCheck by float("Distance", 1f, 0f..3f)
    private val onlyWhenLookingDown by boolean("OnlyLookDown", false)
    private val lookDownThreshold by float("LookDownAngle", 45f, 0f..90f) { onlyWhenLookingDown }
    private val autoDisable by boolean("AutoDisable", false)

    private val sneakTimer = TickTimer()

    val onUpdate = handler<UpdateEvent> {
        val thePlayer = mc.thePlayer ?: return@handler

        // Don't interfere if player is manually sneaking
        if (GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            if (autoDisable) state = false
            return@handler
        }

        if (!thePlayer.onGround) {
            if (sneakTimer.hasTimePassed(maxSneakTime.random())) {
                mc.gameSettings.keyBindSneak.pressed = false
                sneakTimer.reset()
            }
            return@handler
        }

        // Check blocks around player
        val playerVec = Vec3(thePlayer.posX, thePlayer.posY, thePlayer.posZ)
        val shouldSneak = arrayOf(
            BlockPos(playerVec.xCoord, playerVec.yCoord - 1.0, playerVec.zCoord),
            BlockPos(playerVec.xCoord + distanceCheck, playerVec.yCoord - 1.0, playerVec.zCoord),
            BlockPos(playerVec.xCoord - distanceCheck, playerVec.yCoord - 1.0, playerVec.zCoord),
            BlockPos(playerVec.xCoord, playerVec.yCoord - 1.0, playerVec.zCoord + distanceCheck),
            BlockPos(playerVec.xCoord, playerVec.yCoord - 1.0, playerVec.zCoord - distanceCheck)
        ).any { it.block == air }

        if (shouldSneak) {
            val lookCheck = !onlyWhenLookingDown || thePlayer.rotationPitch >= lookDownThreshold
            mc.gameSettings.keyBindSneak.pressed = lookCheck && !GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        } else {
            if (sneakTimer.hasTimePassed(maxSneakTime.random())) {
                mc.gameSettings.keyBindSneak.pressed = false
                sneakTimer.reset()
            } else sneakTimer.update()
        }
    }

    override fun onDisable() {
        sneakTimer.reset()

        if (!GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)) {
            mc.gameSettings.keyBindSneak.pressed = false
        }
    }
}
