package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.deltaTime
import net.ccbluex.liquidbounce.value.*
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.player.EntityPlayer
import org.lwjgl.opengl.GL11.*
import java.awt.Color
import kotlin.math.sin

object Bounce : Module("Bounce", Category.RENDER) {

    // General Settings
    private val colorMode by ListValue("ColorMode", arrayOf("Rainbow", "Custom", "Health"), "Rainbow")
    private val customRed by intValue("Red", 255, 0..255) { colorMode == "Custom" }
    private val customGreen by intValue("Green", 255, 0..255) { colorMode == "Custom" }
    private val customBlue by intValue("Blue", 255, 0..255) { colorMode == "Custom" }
    private val customAlpha by intValue("Alpha", 180, 0..255) { colorMode == "Custom" }
    
    // Animation Settings
    private val bounceSpeed by floatValue("BounceSpeed", 1f, 0.1f..5f)
    private val bounceHeight by floatValue("BounceHeight", 1f, 0.1f..3f)
    private val radius by floatValue("Radius", 0.5f, 0.1f..2f)
    private val lineWidth by floatValue("LineWidth", 2f, 0.1f..5f)
    private val filled by _boolean("Filled", true)
    
    // Target Settings
    private val showSelf by _boolean("ShowSelf", true)
    private val showTeam by _boolean("ShowTeam", true)
    private val showTarget by _boolean("ShowKillAuraTarget", true)

    private var hue = 0f

    @EventTarget
    fun onRender3D(event: Render3DEvent) {
        // Update rainbow hue
        hue += 0.001f * deltaTime
        if (hue > 1f) hue = 0f

        for (entity in mc.theWorld.loadedEntityList) {
            if (entity !is EntityLivingBase || entity.isDead || (!showSelf && entity == mc.thePlayer)) continue
            if (entity !is EntityPlayer) continue

            // Calculate bounce offset using sine wave
            val bounceOffset = sin((System.currentTimeMillis() % 1000).toDouble() / (1000.0 / (bounceSpeed * Math.PI))) * bounceHeight

            // Determine color based on mode
            val color = when (colorMode) {
                "Rainbow" -> Color.getHSBColor(hue + (entity.entityId * 0.001f), 0.7f, 1.0f)
                "Custom" -> Color(customRed, customGreen, customBlue, customAlpha)
                "Health" -> {
                    val health = entity.health
                    val maxHealth = entity.maxHealth
                    val fraction = health / maxHealth
                    val red = ((1 - fraction) * 255).toInt().coerceIn(0, 255)
                    val green = (fraction * 255).toInt().coerceIn(0, 255)
                    Color(red, green, 0, customAlpha)
                }
                else -> Color.WHITE
            }

            // Draw the bouncing circle
            RenderUtils.withTryCatch {
                glPushMatrix()
                
                // Enable required OpenGL states
                glDisable(GL_TEXTURE_2D)
                glEnable(GL_BLEND)
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                glEnable(GL_LINE_SMOOTH)
                glLineWidth(lineWidth)
                
                // Get entity position
                val renderManager = mc.renderManager
                val x = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * event.partialTicks - renderManager.renderPosX
                val y = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * event.partialTicks - renderManager.renderPosY + bounceOffset
                val z = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * event.partialTicks - renderManager.renderPosZ
                
                // Set color with alpha
                glColor4f(color.red / 255f, color.green / 255f, color.blue / 255f, color.alpha / 255f)
                
                // Draw circle
                glBegin(if (filled) GL_POLYGON else GL_LINE_LOOP)
                for (i in 0..360 step 5) {
                    val angle = i * Math.PI / 180
                    glVertex3d(x + sin(angle) * radius, y, z + cos(angle) * radius)
                }
                glEnd()
                
                // Restore OpenGL states
                glEnable(GL_TEXTURE_2D)
                glDisable(GL_LINE_SMOOTH)
                glDisable(GL_BLEND)
                glPopMatrix()
            }
        }
    }

    override val tag get() = colorMode
}
