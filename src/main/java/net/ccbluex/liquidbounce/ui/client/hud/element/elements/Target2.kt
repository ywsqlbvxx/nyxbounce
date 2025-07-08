/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements

import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.Element
import net.ccbluex.liquidbounce.ui.client.hud.element.ElementInfo
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawGradientRect
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.extensions.darker
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import java.awt.Color
import kotlin.math.abs

@ElementInfo(name = "Target2")
class Target2 : Element("Target2") {

    private val modeValue by listValue("Mode", arrayOf("Modern", "Legacy", "ModernRect"), "Modern")
    private val showStatusValue by boolean("ShowWinLoss", true)
    private val healthColorValue by boolean("HealthColor", false)

    private val theme1 by color("Theme1", Color(3, 65, 252))
    private val theme2 by color("Theme2", Color(3, 252, 236))

    private var fadeTimer: MSTimer? = null
    private var healthBarTimer: MSTimer? = null
    private var target: EntityLivingBase? = null
    private var lastAliveMS = 0L
    private var lastHealth = 0.0
    private var lastHealthBar = 0f

    private val backgroundColor = Color(0, 0, 0, 150)
    private val outlineColor = Color(0, 0, 0, 110)

    override fun drawElement(): Border {
        val scaledResolution = ScaledResolution(mc)
        val fontRenderer = mc.fontRendererObj

        if (!KillAura.handleEvents()) {
            target = null
            fadeTimer = null
            healthBarTimer = null
            return Border(0f, 0f, 120f, 36f)
        }

        var target = KillAura.target
        if (target == null || target !is EntityPlayer) {
            if (mc.currentScreen !is GuiChat) {
                this.target = null
                fadeTimer = null
                healthBarTimer = null
                return Border(0f, 0f, 120f, 36f)
            }
            target = mc.thePlayer
        }

        this.target = target

        if (KillAura.target != null) {
            lastAliveMS = System.currentTimeMillis()
            fadeTimer = null
        } else if (this.target != null) {
            if (System.currentTimeMillis() - lastAliveMS >= 400 && fadeTimer == null) {
                fadeTimer = MSTimer()
                fadeTimer!!.start()
            }
        }

        val padding = 8
        var playerInfo = target.displayName.formattedText
        val health = target.health / target.maxHealth
        
        if (target.isDead) {
            playerInfo = "§c§l✗ §r" + playerInfo
        }

        if (health != lastHealth) {
            healthBarTimer = MSTimer()
            healthBarTimer!!.start()
        }
        lastHealth = health

        playerInfo += " §r§7[§f${target.health.toInt()}§7/§f${target.maxHealth.toInt()}§7]"

        if (showStatusValue) {
            playerInfo += " " + (if (health <= mc.thePlayer.health / mc.thePlayer.maxHealth) "§aW" else "§cL")
        }

        val stringWidth = fontRenderer.getStringWidth(playerInfo) + padding
        val x = (scaledResolution.scaledWidth / 2 - stringWidth / 2)
        val y = (scaledResolution.scaledHeight / 2 + 15)
        
        val startX = x - padding
        val startY = y - padding
        val endX = x + stringWidth
        val endY = y + fontRenderer.FONT_HEIGHT + 7

        val alpha = if (fadeTimer == null) 255 else (255 - fadeTimer!!.elapsedTime.toFloat() / 400f * 255f).toInt().coerceIn(0, 255)
        
        if (alpha > 0) {
            when (modeValue.lowercase()) {
                "modern" -> {
                    drawRoundedRect(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY + 13f, 8f, backgroundColor.withAlpha(alpha))
                }
                "legacy" -> {
                    RenderUtils.drawRoundedGradientRect(
                        startX.toFloat(), startY.toFloat(), endX.toFloat(), endY + 13f, 
                        10f, outlineColor.withAlpha(alpha), 
                        theme1.withAlpha(alpha), theme2.withAlpha(alpha)
                    )
                }
                "modernrect" -> {
                    drawRect(startX, startY, endX, endY + 13, backgroundColor.withAlpha(alpha))
                }
            }

            // Health bar background
            val barStartX = startX + 6f
            val barEndX = endX - 6f
            val barY = endY.toFloat()
            drawRoundedRect(barStartX, barY, barEndX, barY + 5f, 4f, outlineColor.withAlpha(alpha))

            // Health bar
            val healthWidth = (barEndX - barStartX) * health
            val targetBar = barStartX + healthWidth

            if (abs(targetBar - lastHealthBar) > 0.01f && healthBarTimer != null) {
                val speed = if (modeValue.equals("modern", true)) 4f else 1f
                val diff = targetBar - lastHealthBar
                
                lastHealthBar = if (diff > 0) {
                    (lastHealthBar + healthBarTimer!!.elapsedTime.toFloat() / 400f * diff * speed).coerceAtMost(targetBar)
                } else {
                    (lastHealthBar - healthBarTimer!!.elapsedTime.toFloat() / 400f * abs(diff) * speed).coerceAtLeast(targetBar)
                }
            } else {
                lastHealthBar = targetBar
            }

            val barColor = if (healthColorValue) {
                ColorUtils.healthColor(target.health, target.maxHealth)
            } else {
                val blend = health.coerceIn(0.0, 1.0).toFloat()
                ColorUtils.blendColors(arrayOf(theme1, theme2), blend)
            }

            when (modeValue.lowercase()) {
                "modern", "legacy" -> {
                    drawRoundedRect(barStartX, barY, lastHealthBar, barY + 5f, 4f, barColor.darker(0.3f).withAlpha(alpha))
                    drawRoundedRect(barStartX, barY, targetBar, barY + 5f, 4f, barColor.withAlpha(alpha))
                }
                "modernrect" -> {
                    drawRect(barStartX.toInt(), barY.toInt(), lastHealthBar.toInt(), barY.toInt() + 5, 
                           barColor.darker(0.3f).withAlpha(alpha))
                    drawRect(barStartX.toInt(), barY.toInt(), targetBar.toInt(), barY.toInt() + 5,
                           barColor.withAlpha(alpha))
                }
            }

            Fonts.font35.drawString(playerInfo, x.toFloat(), y.toFloat(), Color(220, 220, 220, alpha).rgb, true)
        }

        return Border(startX.toFloat(), startY.toFloat(), endX.toFloat(), endY + 13f)
    }

    private fun Color.withAlpha(alpha: Int): Color {
        return Color(this.red, this.green, this.blue, alpha)
    }
}
