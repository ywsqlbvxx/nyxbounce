/*
 * RinBounce Hacked Client
 * A modern free open-source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements.target

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawGradientRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawHead
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedBorderRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRoundedRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.withClipping
import net.minecraft.client.renderer.GlStateManager.*
import net.minecraft.entity.EntityLivingBase
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.abs
import kotlin.math.max

class RinBounce(
    private val roundedRectRadius: Float = 6f,
    private val borderStrength: Float = 1.5f,
    private val backgroundColor: Color = Color(20, 20, 20, 180),
    private val healthBarColor1: Color = Color(144, 238, 144),
    private val healthBarColor2: Color = Color(173, 216, 230),
    private val roundHealthBarShape: Boolean = true,
    private val borderColor: Color = Color(35, 35, 35),
    private val textColor: Color = Color(255, 255, 255),
    private val titleFont: Any = Fonts.fontSemibold35,
    private val healthFont: Any = Fonts.fontRegular30,
    private val textShadow: Boolean = false
) : TargetStyle("RinBounce") {

    private var animatedHealthBarColor1 = healthBarColor1
    private var animatedHealthBarColor2 = healthBarColor2

    override fun drawTarget(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float, easingHurtTime: Float) {
        val width = (40f + (entity.name?.let { Fonts.fontSemibold40.getStringWidth(it) } ?: 0)).coerceAtLeast(118F)
        val height = 40f

        // Update animated colors
        val time = System.currentTimeMillis() / 1000.0
        val hueShift = (kotlin.math.sin(time * 0.5) * 0.1 + 0.1).toFloat()
        
        val hsbStart = Color.RGBtoHSB(healthBarColor1.red, healthBarColor1.green, healthBarColor1.blue, null)
        val hsbEnd = Color.RGBtoHSB(healthBarColor2.red, healthBarColor2.green, healthBarColor2.blue, null)
        
        animatedHealthBarColor1 = Color.getHSBColor(hsbStart[0] + hueShift, hsbStart[1], hsbStart[2])
        animatedHealthBarColor2 = Color.getHSBColor(hsbEnd[0] + hueShift, hsbEnd[1], hsbEnd[2])

        // Draw background with modern style border
        drawRoundedBorderRect(
            0F, 0F, width, height,
            borderStrength,
            backgroundColor.rgb,
            borderColor.rgb,
            roundedRectRadius
        )

        // Draw animated health bar
        val healthBarTop = 26F
        val healthBarHeight = 10F
        val healthBarStart = 38F
        val healthBarTotal = (width - 41F).coerceAtLeast(0F)
        val currentWidth = (easingHealth / maxHealth).coerceIn(0F, 1F) * healthBarTotal

        // Draw background bar with modern rounded style
        val backgroundBar = {
            drawRoundedRect(
                healthBarStart,
                healthBarTop,
                healthBarStart + healthBarTotal,
                healthBarTop + healthBarHeight,
                Color(35, 35, 35).rgb,
                roundedRectRadius - 2
            )
        }

        if (roundHealthBarShape) {
            backgroundBar()
        }

        // Draw animated gradient health bar
        withClipping(main = {
            if (roundHealthBarShape) {
                drawRoundedRect(
                    healthBarStart,
                    healthBarTop,
                    healthBarStart + currentWidth,
                    healthBarTop + healthBarHeight,
                    0,
                    roundedRectRadius - 2
                )
            } else {
                backgroundBar()
            }
        }, toClip = {
            drawGradientRect(
                healthBarStart.toInt(),
                healthBarTop.toInt(),
                healthBarStart.toInt() + currentWidth.toInt(),
                healthBarTop.toInt() + healthBarHeight.toInt(),
                animatedHealthBarColor1.rgb,
                animatedHealthBarColor2.rgb,
                0f
            )
        })

        // Draw health percentage with modern font
        val healthPercentage = (easingHealth / maxHealth * 100).toInt()
        val percentageText = "$healthPercentage%"
        val textWidth = (healthFont as net.ccbluex.liquidbounce.ui.font.GameFontRenderer).getStringWidth(percentageText)
        val calcX = healthBarStart + currentWidth - textWidth
        val textX = max(healthBarStart, calcX)
        val textY = healthBarTop - healthFont.fontHeight / 2 - 2F
        healthFont.drawString(percentageText, textX, textY, textColor.rgb, textShadow)

        // Draw player head with smooth hurt animation
        glPushMatrix()
        val scale = 1 - easingHurtTime / 10f
        val f1 = (0.7F..1F).lerpWith(scale)
        val color = ColorUtils.interpolateColor(Color.RED, Color.WHITE, scale)
        val centerX1 = (4..32).lerpWith(0.5F)
        val midY = (4f..28f).lerpWith(0.5F)

        glTranslatef(centerX1, midY, 0f)
        glScalef(f1, f1, f1)
        glTranslatef(-centerX1, -midY, 0f)

        val entityTexture = mc.renderManager.getEntityRenderObject<net.minecraft.entity.Entity>(entity)?.getEntityTexture(entity)
        if (entityTexture != null) {
            withClipping(main = {
                drawRoundedRect(4f, 4f, 32f, 32f, 0, roundedRectRadius)
            }, toClip = {
                drawHead(entityTexture, 4, 4, 8f, 8f, 8, 8, 28, 28, 64F, 64F, color)
            })
        }
        glPopMatrix()

        // Draw player name with modern font
        entity.name?.let {
            (titleFont as net.ccbluex.liquidbounce.ui.font.GameFontRenderer).drawString(
                it,
                healthBarStart,
                6F,
                textColor.rgb,
                textShadow
            )
        }
    }

    override fun getBorder(entity: EntityLivingBase, easingHealth: Float, maxHealth: Float): Border {
        val width = (40f + (entity.name?.let { Fonts.fontSemibold40.getStringWidth(it) } ?: 0)).coerceAtLeast(118F)
        return Border(0F, 0F, width, 40F)
    }

    private fun ClosedFloatingPointRange<Float>.lerpWith(alpha: Float): Float {
        return start + (endInclusive - start) * alpha
    }

    private fun IntRange.lerpWith(alpha: Float): Float {
        return first + (last - first) * alpha
    }
}
