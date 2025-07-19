package net.ccbluex.liquidbounce.features.module.modules.scriptbased

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render2DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.misc.AntiBot
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.utils.skid.moonlight.render.ColorUtils
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.gui.GuiChat
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*
import kotlin.math.*
import net.ccbluex.liquidbounce.utils.render.Stencil

object TargetHUD : Module("TargetHUD", Category.HUD, hideModule = false) {

    // General Settings
    private val hudStyle by ListValue(
        "Style",
        arrayOf(
            "Flux",
            "Arc",
            "Compact",
            "Moon4",
            "Southside",
            "Novoline",
            "戶籍",
            "Chill",
            "Myau",
            "RavenB4",
            "Naven",
            "Wave",
            "Pulse",
            "Neon"
        ),
        "Flux"
    )
    private val posX by intValue("PosX", 0, -400..400)
    private val posY by intValue("PosY", 0, -400..400)
    private val animSpeed by floatValue("AnimationSpeed", 0.1F, 0.01F..0.5F)

    // Moon4 Settings
    private val moon4BarColorR by intValue("Moon4-BarR", 70, 0..255) { hudStyle == "Moon4" }
    private val moon4BarColorG by intValue("Moon4-BarG", 130, 0..255) { hudStyle == "Moon4" }
    private val moon4BarColorB by intValue("Moon4-BarB", 255, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorR by intValue("Moon4-BGR", 30, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorG by intValue("Moon4-BGG", 30, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorB by intValue("Moon4-BGB", 30, 0..255) { hudStyle == "Moon4" }
    private val moon4BGColorA by intValue("Moon4-BGA", 180, 0..255) { hudStyle == "Moon4" }
    private val moon4AnimSpeed by intValue("Moon4-AnimSpeed", 4, 1..10) { hudStyle == "Moon4" }
    private var moon4EasingHealth = 0F

    // Neon Settings
    private val neonGlow by _boolean("Neon-Glow", true) { hudStyle == "Neon" }
    private val neonColor = Color(255, 50, 255)

    // Other existing settings...

    private val decimalFormat = DecimalFormat("0.0", DecimalFormatSymbols(Locale.ENGLISH))
    private var target: EntityLivingBase? = null
    private var lastTarget: EntityLivingBase? = null
    private var easingHealth = 0F
    private var hue = 0F
    private var slideIn = 0F

    @EventTarget
    fun onRender2D(event: Render2DEvent) {
        // Update target logic and animations...
        
        val sr = ScaledResolution(mc)
        val x = sr.scaledWidth / 2F + posX
        val y = sr.scaledHeight / 2F + posY

        when (hudStyle.lowercase(Locale.getDefault())) {
            "moon4" -> renderMoon4HUD(x, y)
            "neon" -> renderNeonHUD(x, y)
            // Other cases...
        }
    }

    private fun renderMoon4HUD(x: Float, y: Float) {
        val entity = target ?: lastTarget ?: return

        // Animate towards the current target's health, or towards 0 if no target.
        val currentHealth = target?.health ?: 0f
        moon4EasingHealth += ((currentHealth - moon4EasingHealth) / 2.0F.pow(10.0F - moon4AnimSpeed)) * deltaTime

        val mainColor = Color(moon4BarColorR, moon4BarColorG, moon4BarColorB)
        val bgColor = Color(moon4BGColorR, moon4BGColorG, moon4BGColorB, moon4BGColorA)

        val name = entity.name
        val healthInt = entity.health.toInt()
        val percentText = "${"$"}BOLD${healthInt}HP"

        val nameLength = (Fonts.fontSF40.getStringWidth(name)).coerceAtLeast(
            Fonts.fontSF35.getStringWidth(percentText)
        ).toFloat() + 20F

        val healthPercent = (entity.health / entity.maxHealth).coerceIn(0F, 1F)
        val barWidth = healthPercent * (nameLength - 2F)
        val animateThingy = (moon4EasingHealth.coerceIn(entity.health, entity.maxHealth) / entity.maxHealth) * (nameLength - 2F)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, slideIn)

        // Backgrounds
        RenderUtils.drawRoundedRect(-2F, -2F, 3F + nameLength + 36F, 2F + 36F, bgColor.rgb, 3f)
        RenderUtils.drawRoundedRect(-1F, -1F, 2F + nameLength + 36F, 1F + 36F, Color(0, 0, 0, 50).rgb, 3f)

        // Head with Stencil
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let { playerInfo ->
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.drawRoundedRect(1f, 0.5f, 1f + 35f, 0.5f + 35f, Color.WHITE.rgb, 7F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            Target().drawHead(playerInfo.locationSkin, 1, 0, 35, 35, Color.WHITE)
            Stencil.dispose()
        }

        // Text
        Fonts.fontSF40.drawString(name, 2F + 36F, 2F, -1)
        Fonts.fontSF35.drawString(percentText, 38F, 15F, Color.WHITE.rgb)

        // Health Bar
        RenderUtils.drawRoundedRect(37F, 23F, 37F + nameLength, 33f, Color(0, 0, 0, 100).rgb, 3f)
        if (moon4EasingHealth > entity.health) {
            RenderUtils.drawRoundedRect(38F, 24F, 38F + animateThingy, 32f, mainColor.darker().rgb, 3f)
        }
        RenderUtils.drawRoundedRect(38F, 24F, 38F + barWidth, 32f, mainColor.rgb, 3f)

        GlStateManager.popMatrix()
    }

    private fun renderNeonHUD(x: Float, y: Float) {
        val entity = target ?: return
        val width = 180F
        val height = 60F

        easingHealth = lerp(easingHealth, entity.health, animSpeed * 2)

        GlStateManager.pushMatrix()
        GlStateManager.translate(x, y, 0F)
        GlStateManager.scale(slideIn, slideIn, 1F)
        RenderUtils.drawRoundedRect(0F, 0F, width, height, Color(10, 10, 10, 180).rgb, 8F)

        if(neonGlow) {
            for(i in 1..3) {
                val glowSize = i * 2F
                RenderUtils.drawRoundedRect(-glowSize, -glowSize, width+glowSize, height+glowSize,
                    Color(neonColor.red, neonColor.green, neonColor.blue, 30/i).rgb, 8F + glowSize)
            }
        }

        // Head drawing
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            Target().drawHead(it.locationSkin, 8, 8, 44, 44, Color.WHITE)
        }

        // Name text
        Fonts.font40.drawString(entity.name, 60F, 12F, Color.WHITE.rgb)

        // Neon health bar
        val healthPercent = easingHealth / entity.maxHealth
        val barHeight = 12F
        val barX = 60F
        val barY = 28F

        // Background bar
        RenderUtils.drawRoundedRect(barX, barY, width - 20F, barY + barHeight, Color(30, 30, 30).rgb, 4F)

        // Foreground bar (neon effect)
        val gradientWidth = (width - 80F) * healthPercent
        RenderUtils.drawRoundedRect(barX, barY, barX + gradientWidth, barY + barHeight,
            neonColor.brighter().rgb, 4F)

        // Health value
        val healthText = "${decimalFormat.format(easingHealth)} / ${entity.maxHealth}"
        Fonts.font35.drawString(healthText, width - Fonts.font35.getStringWidth(healthText) - 15F, barY + 2F, neonColor.brighter().rgb)

        // Distance display
        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "${decimalFormat.format(distance)}m"
        Fonts.font35.drawString(distanceText, barX, barY + barHeight + 5F, Color(200, 200, 200).rgb)

        GlStateManager.popMatrix()
    }

    private fun lerp(start: Float, end: Float, speed: Float): Float = start + (end - start) * speed * (deltaTime / (1000F / 60F))

    // ... rest of your class implementation
}
