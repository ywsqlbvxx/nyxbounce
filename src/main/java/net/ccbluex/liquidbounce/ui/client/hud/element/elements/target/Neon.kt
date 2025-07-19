/*
 * RinBounce Hacked Client
 * A modern free open-source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements.target

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import java.text.DecimalFormat
import kotlin.math.*

class Neon(
    private val glowEnabled: Boolean = true,
    private val neonColor: Color = Color(255, 50, 255),
    private val animationSpeed: Float = 0.2f
) : TargetStyle("Neon") {
    
    private val decimalFormat = DecimalFormat("0.0")
    private var easingHealth = 0f

    override fun drawTarget(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float, easingHurtTime: Float) {
        val width = 180F
        val height = 60F

        this.easingHealth += ((entity.health - this.easingHealth) * animationSpeed)

        // Background with glow
        RenderUtils.drawRoundedRect(0F, 0F, width, height, Color(10, 10, 10, 180).rgb, 8F)

        if(glowEnabled) {
            for(i in 1..3) {
                val glowSize = i * 2F
                RenderUtils.drawRoundedRect(
                    -glowSize, -glowSize,
                    width + glowSize, height + glowSize,
                    Color(neonColor.red, neonColor.green, neonColor.blue, 30/i).rgb,
                    8F + glowSize
                )
            }
        }

        // Head
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let {
            RenderUtils.drawHead(it.locationSkin, 8f, 8f, 44, 44, 1)
        }

        // Name
        Fonts.fontRegular40.drawString(entity.name, 60F, 12F, Color.WHITE.rgb)

        // Health bar
        val healthPercent = this.easingHealth / entity.maxHealth
        val barHeight = 12F
        val barX = 60F
        val barY = 28F

        // Background bar
        RenderUtils.drawRoundedRect(barX, barY, width - 20F, barY + barHeight, Color(30, 30, 30).rgb, 4F)

        // Neon health bar
        val gradientWidth = (width - 80F) * healthPercent
        RenderUtils.drawRoundedRect(
            barX, barY,
            barX + gradientWidth, barY + barHeight,
            neonColor.brighter().rgb,
            4F
        )

        // Health text
        val healthText = "${decimalFormat.format(this.easingHealth)} / ${entity.maxHealth}"
        Fonts.fontRegular35.drawString(
            healthText,
            width - Fonts.fontRegular35.getStringWidth(healthText) - 15F,
            barY + 2F,
            neonColor.brighter().rgb
        )

        // Distance
        val distance = mc.thePlayer.getDistanceToEntity(entity)
        val distanceText = "${decimalFormat.format(distance)}m"
        Fonts.fontRegular35.drawString(
            distanceText,
            barX,
            barY + barHeight + 5F,
            Color(200, 200, 200).rgb
        )
    }

    override fun getBorder(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float): Border {
        return Border(0F, 0F, 180F, 60F)
    }
}
