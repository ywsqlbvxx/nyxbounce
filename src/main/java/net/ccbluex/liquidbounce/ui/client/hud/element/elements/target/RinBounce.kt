/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements.target

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.entity.EntityLivingBase
import net.minecraft.client.renderer.GlStateManager.*
import java.awt.Color
import net.ccbluex.liquidbounce.ui.font.GameFontRenderer
import org.lwjgl.opengl.GL11

class RinBounce(
    private val roundedRectRadius: Float,
    private val borderStrength: Float,
    private val backgroundColor: Color,
    private val healthBarColor1: Color,
    private val healthBarColor2: Color,
    private val roundHealthBarShape: Boolean,
    private val borderColor: Color,
    private val textColor: Color,
    private val titleFont: GameFontRenderer,
    private val healthFont: GameFontRenderer,
    private val textShadow: Boolean
) : TargetStyle("RinBounce") {

    override fun drawTarget(entity: EntityLivingBase) {
        val width = (40f + titleFont.getStringWidth(entity.name ?: "")).coerceAtLeast(118F)
        val height = 36F

        // Draw background with border
        RenderUtils.drawRoundedRect(1F, 1F, width - 1F, height - 1F, roundedRectRadius, backgroundColor.rgb)
        RenderUtils.drawRoundedRect(0F, 0F, width, height, roundedRectRadius, borderColor.rgb)
        
        // Draw head with border
        GL11.glColor4f(1f, 1f, 1f, 1f)
        RenderUtils.drawRoundedRect(2F, 2F, 34F, 34F, 4f, borderColor.rgb)
        RenderUtils.drawHead(entity.skin, 3f, 3f, 30, 30)

        // Draw name with custom font
        titleFont.drawString(
            "§l" + (entity.name ?: ""),
            38F, 
            4F,
            textColor.rgb,
            textShadow
        )

        // Health bar background with rounded corners
        RenderUtils.drawRoundedRect(37F, 22F, width - 2F, 33F, 3f, Color(0, 0, 0, 70).rgb)

        // Health bar with gradient
        val barWidth = (entity.health / entity.maxHealth) * (width - 39F)
        RenderUtils.drawRoundedRect(38F, 23F, 38F + barWidth, 32F, if (roundHealthBarShape) 3f else 0f, healthBarColor1.rgb)

        if (entity.hurtTime > 0) {
            RenderUtils.drawRoundedRect(38F, 23F, 38F + barWidth, 32F, if (roundHealthBarShape) 3f else 0f, healthBarColor2.rgb)
        }

        // Health text with bold and custom font
        healthFont.drawString(
            "§l${entity.health.toInt()} HP",
            38F,
            14F,
            Color.WHITE.rgb,
            textShadow
        )

        resetColor()
    }

    override fun getBorder(entity: EntityLivingBase): Border {
        val width = (40f + titleFont.getStringWidth(entity.name ?: "")).coerceAtLeast(118F)
        return Border(0F, 0F, width, 36F)
    }
}
