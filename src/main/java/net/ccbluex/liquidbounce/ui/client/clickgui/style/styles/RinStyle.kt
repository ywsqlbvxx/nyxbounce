/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.clickgui.style.styles

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.guiColor
import net.ccbluex.liquidbounce.features.module.modules.render.ClickGUI.scale
import net.ccbluex.liquidbounce.ui.client.clickgui.ClickGui.clamp
import net.ccbluex.liquidbounce.ui.client.clickgui.Panel
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ButtonElement
import net.ccbluex.liquidbounce.ui.client.clickgui.elements.ModuleElement
import net.ccbluex.liquidbounce.ui.client.clickgui.style.Style
import net.ccbluex.liquidbounce.ui.font.AWTFontRenderer.Companion.assumeNonVolatile
import net.ccbluex.liquidbounce.ui.font.Fonts.fontSemibold35
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawBorderedRect
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils.withAlpha
import net.minecraft.client.gui.ScaledResolution
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.awt.Color
import kotlin.math.roundToInt

@SideOnly(Side.CLIENT)
object RinStyle : Style() {

    private val backgroundColor = Color(0, 0, 0, 180)
    private val boxColor = Color(28, 28, 28, 220)
    private val accentColor = Color(0, 144, 255)

    override fun drawPanel(mouseX: Int, mouseY: Int, panel: Panel) {
        // Main panel background
        RenderUtils.drawBorderedRect(
            panel.x.toFloat(), panel.y.toFloat(),
            (panel.x + panel.width).toFloat(), (panel.y + panel.height).toFloat(),
            1F,
            accentColor.rgb,
            boxColor.rgb
        )

        // Panel header
        RenderUtils.drawRect(
            panel.x.toFloat(), panel.y.toFloat(),
            (panel.x + panel.width).toFloat(), (panel.y + 19).toFloat(),
            accentColor.withAlpha(150).rgb
        )

        // Draw panel name
        assumeNonVolatile {
            fontSemibold35.drawString(
                panel.name,
                (panel.x + 5).toFloat(),
                (panel.y + 7).toFloat(),
                Color.WHITE.rgb
            )
        }

        // Draw border line
        RenderUtils.drawRect(
            panel.x.toFloat(),
            (panel.y + 19).toFloat(),
            (panel.x + panel.width).toFloat(),
            (panel.y + 20).toFloat(),
            accentColor.rgb
        )

        // No arrow needed since we're using a box layout
    }

    override fun drawHoverText(mouseX: Int, mouseY: Int, text: String) {
        val lines = text.split("\n")
        val width = lines.maxOf { fontSemibold35.getStringWidth(it) } + 8
        val height = (lines.size * fontSemibold35.fontHeight) + 6

        // Draw hover text background
        RenderUtils.drawBorderedRect(
            mouseX.toFloat(), mouseY.toFloat(),
            (mouseX + width).toFloat(), (mouseY + height).toFloat(),
            1F,
            accentColor.rgb,
            boxColor.rgb
        )

        // Draw text
        assumeNonVolatile {
            lines.forEachIndexed { index, line ->
                fontSemibold35.drawString(
                    line,
                    (mouseX + 4).toFloat(),
                    (mouseY + 4 + index * fontSemibold35.fontHeight).toFloat(),
                    Color.WHITE.rgb,
                    true
                )
            }
        }
    }

    override fun drawButtonElement(mouseX: Int, mouseY: Int, buttonElement: ButtonElement) {
        val color = if (buttonElement.isHovered(mouseX, mouseY)) 
            getHoverColor(accentColor, 30) else boxColor.rgb

        // Draw button background
        RenderUtils.drawRect(
            buttonElement.x.toFloat(),
            buttonElement.y.toFloat(),
            (buttonElement.x + buttonElement.width).toFloat(),
            (buttonElement.y + buttonElement.height).toFloat(),
            color
        )

        // Draw button text
        assumeNonVolatile {
            fontSemibold35.drawString(
                buttonElement.displayName,
                (buttonElement.x + 5).toFloat(),
                (buttonElement.y + buttonElement.height / 2 - fontSemibold35.fontHeight / 2).toFloat(),
                buttonElement.color,
                true
            )
        }
    }

    override fun drawModuleElementAndClick(
        mouseX: Int,
        mouseY: Int,
        moduleElement: ModuleElement,
        mouseButton: Int?
    ): Boolean {
        val module = moduleElement.module
        val boxStartY = moduleElement.y
        val boxEndY = boxStartY + moduleElement.height

        // Draw module box background
        val moduleColor = if (module.isActive) accentColor.rgb else boxColor.rgb
        RenderUtils.drawRect(
            moduleElement.x.toFloat(),
            boxStartY.toFloat(),
            (moduleElement.x + moduleElement.width).toFloat(),
            boxEndY.toFloat(),
            moduleColor
        )

        // Draw module name
        assumeNonVolatile {
            fontSemibold35.drawString(
                moduleElement.displayName,
                (moduleElement.x + 5).toFloat(),
                (boxStartY + moduleElement.height / 2 - fontSemibold35.fontHeight / 2).toFloat(),
                Color.WHITE.rgb,
                true
            )
        }

        // Handle settings
        if (moduleElement.showSettings) {
            var settingsStartY = boxEndY
            moduleElement.settingsHeight = 0

            for (value in module.values) {
                when (value) {
                    is BoolValue -> drawBooleanElement(moduleElement, value, settingsStartY)
                    is FloatValue -> drawSliderElement(moduleElement, value, settingsStartY)
                    is IntegerValue -> drawSliderElement(moduleElement, value, settingsStartY)
                    is ListValue -> drawListElement(moduleElement, value, settingsStartY)
                    is TextValue -> drawTextElement(moduleElement, value, settingsStartY)
                    // Add more value types as needed
                }
                settingsStartY += 20
                moduleElement.settingsHeight += 20
            }
        }

        return mouseButton != null && moduleElement.isHovered(mouseX, mouseY)
    }

    private fun drawBooleanElement(moduleElement: ModuleElement, value: BoolValue, y: Int) {
        val toggleColor = if (value.get()) accentColor.rgb else Color.GRAY.rgb
        RenderUtils.drawRect(
            (moduleElement.x + 5).toFloat(),
            y.toFloat(),
            (moduleElement.x + moduleElement.width - 5).toFloat(),
            (y + 18).toFloat(),
            toggleColor
        )

        assumeNonVolatile {
            fontSemibold35.drawString(
                value.name,
                (moduleElement.x + 10).toFloat(),
                (y + 5).toFloat(),
                Color.WHITE.rgb,
                true
            )
        }
    }

    private fun drawSliderElement(moduleElement: ModuleElement, value: NumberValue<*>, y: Int) {
        val sliderWidth = moduleElement.width - 10
        val percentage = when (value) {
            is FloatValue -> (value.get() - value.minimum) / (value.maximum - value.minimum)
            is IntegerValue -> (value.get() - value.minimum).toFloat() / (value.maximum - value.minimum)
            else -> 0f
        }

        // Draw slider background
        RenderUtils.drawRect(
            (moduleElement.x + 5).toFloat(),
            y.toFloat(),
            (moduleElement.x + sliderWidth).toFloat(),
            (y + 18).toFloat(),
            boxColor.rgb
        )

        // Draw slider value
        RenderUtils.drawRect(
            (moduleElement.x + 5).toFloat(),
            y.toFloat(),
            (moduleElement.x + 5 + sliderWidth * percentage).toFloat(),
            (y + 18).toFloat(),
            accentColor.rgb
        )

        assumeNonVolatile {
            fontSemibold35.drawString(
                "${value.name}: ${value.get()}",
                (moduleElement.x + 10).toFloat(),
                (y + 5).toFloat(),
                Color.WHITE.rgb,
                true
            )
        }
    }

    private fun drawListElement(moduleElement: ModuleElement, value: ListValue, y: Int) {
        RenderUtils.drawRect(
            (moduleElement.x + 5).toFloat(),
            y.toFloat(),
            (moduleElement.x + moduleElement.width - 5).toFloat(),
            (y + 18).toFloat(),
            boxColor.rgb
        )

        assumeNonVolatile {
            fontSemibold35.drawString(
                "${value.name}: ${value.get()}",
                (moduleElement.x + 10).toFloat(),
                (y + 5).toFloat(),
                Color.WHITE.rgb,
                true
            )
        }
    }

    private fun drawTextElement(moduleElement: ModuleElement, value: TextValue, y: Int) {
        RenderUtils.drawRect(
            (moduleElement.x + 5).toFloat(),
            y.toFloat(),
            (moduleElement.x + moduleElement.width - 5).toFloat(),
            (y + 18).toFloat(),
            boxColor.rgb
        )

        assumeNonVolatile {
            fontSemibold35.drawString(
                "${value.name}: ${value.get()}",
                (moduleElement.x + 10).toFloat(),
                (y + 5).toFloat(),
                Color.WHITE.rgb,
                true
            )
        }
    }
}
