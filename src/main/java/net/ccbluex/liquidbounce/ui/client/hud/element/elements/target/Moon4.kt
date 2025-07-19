/*
 * RinBounce Hacked Client
 * A modern free open-source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements.target

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.Stencil
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.*

class Moon4(
    private val barColorR: Int = 70,
    private val barColorG: Int = 130,
    private val barColorB: Int = 255,
    private val bgColorR: Int = 30,
    private val bgColorG: Int = 30,
    private val bgColorB: Int = 30,
    private val bgColorA: Int = 180,
    private val animSpeed: Int = 4
) : TargetStyle("Moon4") {
    
    companion object {
        private val WHITE_TEXT = Color(255, 255, 255)
        private val BLACK_BG = Color(0, 0, 0, 50)
    }

    private var easingHealth = 0f

    override fun drawTarget(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float, easingHurtTime: Float) {
        val mainColor = Color(barColorR, barColorG, barColorB)
        val bgColor = Color(bgColorR, bgColorG, bgColorB, bgColorA)

        val name = entity.name
        val healthInt = entity.health.toInt()
        val percentText = "§l${healthInt}HP"

        val nameLength = (Fonts.fontSF40.getStringWidth(name)).coerceAtLeast(
            Fonts.fontSF35.getStringWidth(percentText)
        ).toFloat() + 20F

        val healthPercent = (entity.health / entity.maxHealth).coerceIn(0F, 1F)
        val barWidth = healthPercent * (nameLength - 2F)
        
        this.easingHealth += ((entity.health - this.easingHealth) / 2.0F.pow(10.0F - animSpeed))
        val animatedWidth = (this.easingHealth.coerceIn(entity.health, entity.maxHealth) / entity.maxHealth) * (nameLength - 2F)

        // Backgrounds
        RenderUtils.drawRoundedRect(-2F, -2F, 3F + nameLength + 36F, 2F + 36F, bgColor.rgb, 3f)
        RenderUtils.drawRoundedRect(-1F, -1F, 2F + nameLength + 36F, 1F + 36F, BLACK_BG.rgb, 3f)

        // Head with Stencil
        mc.netHandler.getPlayerInfo(entity.uniqueID)?.let { playerInfo ->
            Stencil.write(false)
            glDisable(GL_TEXTURE_2D)
            glEnable(GL_BLEND)
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
            RenderUtils.drawRoundedRect(1f, 0.5f, 1f + 35f, 0.5f + 35f, WHITE_TEXT.rgb, 7F)
            glDisable(GL_BLEND)
            glEnable(GL_TEXTURE_2D)
            Stencil.erase(true)
            RenderUtils.drawHead(playerInfo.locationSkin, 1, 0, 35, 35, 1f)
            Stencil.dispose()
        }

        // Text
        Fonts.fontSF40.drawString(name, 2F + 36F, 2F, -1)
        Fonts.fontSF35.drawString(percentText, 38F, 15F, WHITE_TEXT.rgb)

        // Health Bar
        RenderUtils.drawRoundedRect(37F, 23F, 37F + nameLength, 33f, Color(0, 0, 0, 100).rgb, 3f)
        if (this.easingHealth > entity.health) {
            RenderUtils.drawRoundedRect(38F, 24F, 38F + animatedWidth, 32f, mainColor.darker().rgb, 3f)
        }
        RenderUtils.drawRoundedRect(38F, 24F, 38F + barWidth, 32f, mainColor.rgb, 3f)
    }

    override fun getBorder(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float): Border {
        val nameLength = (40f + (entity.name?.let { Fonts.fontSF40.getStringWidth(it) } ?: 0)).coerceAtLeast(118F)
        return Border(0F, 0F, nameLength + 40F, 40F)
    }
}
