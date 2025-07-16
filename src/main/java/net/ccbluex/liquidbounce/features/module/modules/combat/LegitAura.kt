/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold
import net.ccbluex.liquidbounce.features.module.modules.player.Scaffold2
import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.ui.client.gui.colortheme.ClientTheme
import net.ccbluex.liquidbounce.utils.extensions.*
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils
import net.ccbluex.liquidbounce.utils.EntityUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.Rotation
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.item.ItemSword
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.WorldSettings

object LegitAura : Module("LegitAura", Category.COMBAT) {

    // General settings
    private val range by float("Range", 3.5f, 1f..6f)
    private val fov by int("FOV", 90, 1..180)
    private val maxCPS by int("MaxCPS", 12, 1..20)
    private val minCPS by int("MinCPS", 8, 1..20)
    private val autoBlock by boolean("AutoBlock", false)
    private val onlyWeapon by boolean("OnlyWeapon", true)
    private val throughWalls by boolean("ThroughWalls", false)
    private val swing by boolean("Swing", true)
    private val rotationMode by choices("Rotation", arrayOf("LockView", "None"), "LockView")

    // Internal variables
    private var target: EntityLivingBase? = null
    private val clickTimer = MSTimer()
    private var currentClickDelay = 0L

    override fun onDisable() {
        target = null
        if (autoBlock) {
            stopBlocking()
        }
    }

    override fun onEnable() {
        target = null
        updateClickDelay()
    }

    @EventTarget
    fun onUpdate(event: UpdateEvent) {
        // Get new target or clear current one
        target = getClosestTarget()

        if (target == null || shouldStopAttacking()) {
            if (autoBlock) {
                stopBlocking()
            }
            return
        }

        // Handle rotations
        if (rotationMode == "LockView") {
            val (_, rotation) = RotationUtils.lockView(
                target!!.hitBox,
                true,
                false
            ) ?: return

            rotation.toPlayer(mc.thePlayer)
        }

        // Attack target
        if (canAttack()) {
            if (autoBlock && mc.thePlayer.isBlocking) {
                stopBlocking()
            }

            // Attack
            if (mc.objectMouseOver?.entityHit == target) {
                attackTarget()
            }

            // Start blocking again if needed
            if (autoBlock && mc.thePlayer.heldItem?.item is ItemSword) {
                startBlocking()
            }
        }
    }

    @EventTarget 
    fun onRender3D(event: Render3DEvent) {
        target?.let {
            RenderUtils.drawEntityBox(
                it, 
                ClientTheme.getColorWithAlpha(1, 70),
                false,
                true,
                0f
            )
            GlStateManager.resetColor()
        }
    }

    private fun getClosestTarget(): EntityLivingBase? {
        return mc.theWorld.loadedEntityList
            .filter { 
                it is EntityLivingBase &&
                EntityUtils.isSelected(it, true) &&
                mc.thePlayer.canEntityBeSeen(it) &&
                mc.thePlayer.getDistanceToEntityBox(it) <= range &&
                RotationUtils.getRotationDifference(it) <= fov
            }
            .minByOrNull { mc.thePlayer.getDistanceToEntityBox(it) } as EntityLivingBase?
    }

    private fun shouldStopAttacking(): Boolean {
        return onlyWeapon && !mc.thePlayer.isHoldingSword() || 
               (Scaffold.handleEvents() || Scaffold2.handleEvents()) ||
               mc.thePlayer.isSpectator()
    }

    private fun canAttack(): Boolean {
        return clickTimer.hasTimePassed(currentClickDelay)
    }

    private fun attackTarget() {
        // Reset timer
        clickTimer.reset()
        updateClickDelay()

        // Send attack packet
        val target = target ?: return
        mc.netHandler.addToSendQueue(C02PacketUseEntity(target, C02PacketUseEntity.Action.ATTACK))

        if (swing) {
            mc.thePlayer.swingItem()
        }
        
        if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
            mc.thePlayer.attackTargetEntityWithCurrentItem(target)
        }
    }

    private fun updateClickDelay() {
        currentClickDelay = TimeUtils.randomClickDelay(minCPS, maxCPS)
    }

    private fun startBlocking() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, true)
    }

    private fun stopBlocking() {
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.keyCode, false)
    }

    private fun Entity.isHoldingSword() = mc.thePlayer.heldItem?.item is ItemSword

    override val tag: String
        get() = "%.1f".format(range)
}
