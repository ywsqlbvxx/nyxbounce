/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements.target

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.EntityLivingBase
import java.awt.Color
import kotlin.math.pow
import org.lwjgl.opengl.GL11
import net.ccbluex.liquidbounce.utils.extensions.getDistanceToEntityBox
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.renderer.GlStateManager.*

class RinBounce(
    roundedRectRadius: Float,
    borderStrength: Float,
    backgroundColor: Color,
    healthBarColor1: Color,
    healthBarColor2: Color,
    roundHealthBarShape: Boolean,
    borderColor: Color,
    textColor: Color,
    titleFont: GameFontRenderer,
    healthFont: GameFontRenderer,
    textShadow: Boolean
) : TargetStyle(roundedRectRadius, borderStrength, backgroundColor, healthBarColor1, healthBarColor2, roundHealthBarShape, borderColor, textColor, titleFont, healthFont, textShadow) {

    override fun drawTarget(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float, easingHurtTime: Float, alpha: Double) {
        val width = (40f + titleFont.getStringWidth(entity.name ?: "")).coerceAtLeast(118F)
        val height = 36F

        // Background
        drawRoundedRect(0F, 0F, width, height, roundedRectRadius, backgroundColor.rgb)
        
        // Draw head
        Stencil.write(false)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.fastRoundedRect(1f, 0.5f, 35F, 35.5F, 7F)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        Stencil.erase(true)
        RenderUtils.drawHead(entity, 1, 0, 34, 34)
        Stencil.dispose()

        // Draw name
        titleFont.drawStringWithShadow(
            "§l${entity.name}",
            38F,
            3F,
            textColor.rgb
        )

        // Health bar background
        drawRoundedRect(37F, 23F, width - 2F, 33F, 3f, Color(0, 0, 0, 100).rgb)

        // Health bar
        val barWidth = (easingHealth / maxHealth) * (width - 39F)
        drawRoundedRect(38F, 24F, 38F + barWidth, 32F, 3f, healthBarColor1.rgb)

        // Health text
        healthFont.drawStringWithShadow(
            "§l${easingHealth.toInt()}HP",
            38F,
            15F,
            Color.WHITE.rgb
        )

        GlStateManager.resetColor()
        glColor4f(1f, 1f, 1f, 1f)
    }

    override fun getBorder(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float): Border? {
        val width = (40f + titleFont.getStringWidth(entity.name ?: "")).coerceAtLeast(118F)
        return Border(0F, 0F, width, 36F)
    }
}
