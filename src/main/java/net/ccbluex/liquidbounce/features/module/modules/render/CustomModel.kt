// remove female lol
package net.ccbluex.liquidbounce.features.module.modules.render

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module

object CustomModel : Module("CustomModel", Category.VISUAL) {
    val mode by choices("Mode", arrayOf("Imposter", "Rabbit", "Freddy", "None"), "Imposter")

    val rotatePlayer by  boolean("RotatePlayer", false)

    override val tag: String
        get() = mode
}