/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.hud.element.elements.targets

import net.ccbluex.liquidbounce.ui.client.hud.element.Border
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Target
import net.ccbluex.liquidbounce.utils.client.MinecraftInstance
import net.minecraft.entity.player.EntityPlayer

abstract class TargetStyle(
    val name: String, 
    val targetInstance: Target
) {

    protected var easingHealth = 0F
    protected val decimalFormat2 = java.text.DecimalFormat("##0")

    /**
     * Draw target
     */
    abstract fun drawTarget(entity: EntityPlayer)

    /**
     * Get border of element
     */
    open fun getBorder(entity: EntityPlayer?): Border? {
        return null
    }

    /**
     * Update health animation value
     */
    protected fun updateAnim(health: Float) {
        if (easingHealth < 0 || easingHealth > health)
            easingHealth = health
    }
}
