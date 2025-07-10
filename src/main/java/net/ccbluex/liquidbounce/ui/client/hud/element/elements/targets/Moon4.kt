/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.util.EnumChatFormatting.BOLD
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.pow

class Moon4(inst: Target) : TargetStyle("Moon4", inst) {

    private var easingHealth = 0f
    private val decimalFormat = java.text.DecimalFormat("##0")

    override fun drawTarget(entity: EntityPlayer) {
        updateAnim(entity.health)
        
        val mainColor = targetInstance.barColor
        val percent = entity.health.toInt()
        val nameLength = (Fonts.fontSFUI40.getStringWidth("$BOLD${entity.name}")).coerceAtLeast(
            Fonts.fontSFUI35.getStringWidth(
                "$BOLD${decimalFormat.format(percent)}"
            )
        ).toFloat() + 20F

        val barWidth = (entity.health / entity.maxHealth).coerceIn(0F, entity.maxHealth) * (nameLength - 2F)
        
        RenderUtils.drawRoundedRect(-2F, -2F, 3F + nameLength + 36F, 2F + 36F, 3f, targetInstance.bgColor.rgb)
        RenderUtils.drawRoundedRect(-1F, -1F, 2F + nameLength + 36F, 1F + 36F, 3f, Color(0, 0, 0, 50).rgb)

        // Draw head
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        RenderUtils.drawRoundedRect(1f, 0.5f, 36F, 35.5F, 7F, Color.WHITE.rgb)
        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)

        // Draw head (skin)
        RenderUtils.drawHead(entity.skin, 1, 0, 35, 35)

        Fonts.fontSFUI40.drawStringWithShadow("$BOLD${entity.name}", 2F + 36F, 2F, -1)
        RenderUtils.drawRoundedRect(37F, 23F, 37F + nameLength, 33f, 3f, Color(0, 0, 0, 100).rgb)

        easingHealth += ((entity.health - easingHealth) / 2.0F.pow(10.0F - targetInstance.fadeSpeed)) * RenderUtils.deltaTime
        val animateThingy = (easingHealth.coerceIn(entity.health, entity.maxHealth) / entity.maxHealth) * (nameLength - 2F)

        if (easingHealth > entity.health)
            RenderUtils.drawRoundedRect(38F, 24F, 38F + animateThingy, 32f, 3f, mainColor.darker().rgb)

        RenderUtils.drawRoundedRect(38F, 24f, 38F + barWidth, 32f, 3f, mainColor.rgb)
        Fonts.fontSFUI35.drawStringWithShadow("$BOLD${decimalFormat.format(percent)}HP", 38F, 15F, Color.WHITE.rgb)
    }

    override fun getBorder(entity: EntityPlayer?): Border? {
        if (entity == null) return null
        
        val percent = entity.health.toInt()
        val nameLength = (Fonts.fontSFUI40.getStringWidth("$BOLD${entity.name}")).coerceAtLeast(
            Fonts.fontSFUI35.getStringWidth(
                "$BOLD${decimalFormat.format(percent)}"
            )
        ).toFloat() + 18F
        
        return Border(-1F, -2F, nameLength + 40, 38F)
    }

    private fun updateAnim(health: Float) {
        if (easingHealth < 0 || easingHealth > health) {
            easingHealth = health
        }
    }
}
