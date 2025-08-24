/*
 * LiquidBounce+ Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/WYSI-Foundation/LiquidBouncePlus/
 * 
 * This code belongs to WYSI-Foundation. Please give credits when using this in your repository.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.MoveEvent
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.event.Render3DEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.modules.combat.KillAura
import net.ccbluex.liquidbounce.features.module.modules.movement.Fly
import net.ccbluex.liquidbounce.features.module.modules.movement.Speed
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.RotationUtils
import net.ccbluex.liquidbounce.utils.render.ColorUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.value.*
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.util.MathHelper
import org.lwjgl.opengl.GL11
import java.awt.Color
import java.util.*

object TargetStrafe : Module("TargetStrafe", category = ModuleCategory.MOVEMENT, spacedName = "Target Strafe") {

    private val radiusMode = ListValue("StrafeMode", arrayOf("TrueRadius", "Simple", "Behind"), "Behind")
    val radius = FloatValue("Radius", 2.0f, 0.1f, 4.0f) { !grim.get() }
    val customSpeed = BoolValue("CustomSpeed", false)
    val speedValue = FloatValue("Speed", 0.3f, 0.1f, 0.5f)
    private val render = BoolValue("Render", true)
    private val alwaysRender = BoolValue("Always-Render", true) { render.get() }
    private val modeValue = ListValue("KeyMode", arrayOf("Jump", "None"), "None")
    private val colorType = ListValue("Color", arrayOf("Custom", "Dynamic", "Rainbow", "Rainbow2", "Sky", "Fade", "Mixer"), "Custom")
    private val redValue = IntegerValue("Red", 255, 0, 255)
    private val greenValue = IntegerValue("Green", 255, 0, 255)
    private val blueValue = IntegerValue("Blue", 255, 0, 255)
    private val safewalk = BoolValue("SafeWalk", true)
    val thirdPerson = BoolValue("ThirdPerson", true)
    val always = BoolValue("Always", false)
    val onground = BoolValue("Ground", false)
    val air = BoolValue("Air", false)
    val grim = BoolValue("Grim", false)
    private val accuracyValue = IntegerValue("Accuracy", 0, 0, 59)
    private val thicknessValue = FloatValue("Thickness", 1F, 0.1F, 5F)
    private val mixerSecondsValue = IntegerValue("Mixer-Seconds", 2, 1, 10)
    private val outLine = BoolValue("Outline", true)
    private val saturationValue = FloatValue("Saturation", 0.7F, 0F, 1F)
    private val brightnessValue = FloatValue("Brightness", 1F, 0F, 1F)

    private val killAura = LiquidBounce.moduleManager.getModule(KillAura::class.java) as KillAura
    private val speed = LiquidBounce.moduleManager.getModule(Speed::class.java) as Speed
    private val fly = LiquidBounce.moduleManager.getModule(Fly::class.java) as Fly

    var direction: Int = 1
    var lastView: Int = 0
    var hasChangedThirdPerson: Boolean = true

    val cansize: Float
        get() = when (radiusMode.get().lowercase(Locale.getDefault())) {
            "simple" -> 45f / mc.thePlayer!!.getDistance(killAura.target!!.posX, mc.thePlayer!!.posY, killAura.target!!.posZ).toFloat()
            else -> 45f
        }

    val Enemydistance: Double
        get() = mc.thePlayer!!.getDistance(killAura.target!!.posX, mc.thePlayer!!.posY, killAura.target!!.posZ)

    val algorithm: Float
        get() = Math.max(
            Enemydistance - if (grim.get()) 0.8f else radius.get(),
            Enemydistance - (Enemydistance - if (grim.get()) 0.8f else radius.get() / (if (grim.get()) 0.8f else radius.get() * 2))
        ).toFloat()

    override fun onEnable() {
        hasChangedThirdPerson = true
        lastView = mc.gameSettings.thirdPersonView
    }

    fun onMotion(event: MotionEvent) {
        if (thirdPerson.get()) {
            if (canStrafe) {
                if (hasChangedThirdPerson) lastView = mc.gameSettings.thirdPersonView
                mc.gameSettings.thirdPersonView = 1
                hasChangedThirdPerson = false
            } else if (!hasChangedThirdPerson) {
                mc.gameSettings.thirdPersonView = lastView
                hasChangedThirdPerson = true
            }
        }

        if (event.eventState == net.ccbluex.liquidbounce.event.EventState.PRE) {
            if (mc.thePlayer.isCollidedHorizontally)
                direction = -direction
            if (mc.gameSettings.keyBindLeft.pressed) direction = 1
            if (mc.gameSettings.keyBindRight.pressed) direction = -1
        }
    }

    fun onMove(event: MoveEvent) {
        if (!canStrafe) return

        if (grim.get() && mc.thePlayer.getDistanceSqToEntity(killAura.target!!) < 1.25f) {
            strafe(event, MovementUtils.getSpeed(event.x, event.z))
        } else {
            strafe(event, MovementUtils.getSpeed(event.x, event.z))
        }

        if (safewalk.get() && checkVoid()) {
            event.isSafeWalk = true
        }
    }

    fun strafe(event: MoveEvent, moveSpeed: Double) {
        val target = killAura.target ?: return
        val rotYaw = RotationUtils.getRotationsEntity(target).yaw

        when (radiusMode.get()) {
            "TrueRadius", "Simple" -> {
                val dist = mc.thePlayer.getDistanceToEntity(target)
                val offset = if (dist <= radius.get()) 0.0 else 1.0
                setSpeed(event, if (customSpeed.get()) speedValue.get().toDouble() else moveSpeed, rotYaw, direction, offset)
            }
            "Behind" -> {
                val xPos = target.posX + -Math.sin(Math.toRadians(target.rotationYaw.toDouble())) * -2
                val zPos = target.posZ + Math.cos(Math.toRadians(target.rotationYaw.toDouble())) * -2
                val yaw1 = RotationUtils.getRotations1(xPos, target.posY, zPos)[0].toDouble()
                val speedFactor = if (customSpeed.get()) speedValue.get().toDouble() else moveSpeed
                event.x = -speedFactor * MathHelper.sin(Math.toRadians(yaw1).toFloat())
                event.z = speedFactor * MathHelper.cos(Math.toRadians(yaw1).toFloat())
            }
        }
    }

    private fun setSpeed(event: MoveEvent, moveSpeed: Double, pseudoYaw: Float, pseudoStrafe: Int, pseudoForward: Double) {
        var yaw = pseudoYaw
        var forward = pseudoForward
        var strafe = pseudoStrafe
        var strafe2 = 0f

        if (forward != 0.0) {
            if (strafe > 0.0) {
                if (radiusMode.get().lowercase(Locale.getDefault()) == "trueradius")
                    yaw += if (forward > 0.0) -cansize else cansize
                strafe2 += if (forward > 0.0) -45 / algorithm else 45 / algorithm
            } else if (strafe < 0.0) {
                if (radiusMode.get().lowercase(Locale.getDefault()) == "trueradius")
                    yaw += if (forward > 0.0) cansize else -cansize
                strafe2 += if (forward > 0.0) 45 / algorithm else -45 / algorithm
            }
            strafe = 0
            forward = if (forward > 0.0) 1.0 else -1.0
        }
        strafe = when {
            strafe > 0.0 -> 1
            strafe < 0.0 -> -1
            else -> 0
        }

        val mx = Math.cos(Math.toRadians(yaw + 90.0 + strafe2))
        val mz = Math.sin(Math.toRadians(yaw + 90.0 + strafe2))
        event.x = forward * moveSpeed * mx + strafe * moveSpeed * mz
        event.z = forward * moveSpeed * mz - strafe * moveSpeed * mx
    }

    val keyMode: Boolean
        get() = when (modeValue.get().lowercase(Locale.getDefault())) {
            "jump" -> mc.gameSettings.keyBindJump.isKeyDown
            "none" -> mc.thePlayer.movementInput.moveStrafe != 0f || mc.thePlayer.movementInput.moveForward != 0f
            else -> false
        }

    val canStrafe: Boolean
        get() = state && (speed.state || fly.state || always.get()) &&
                ((onground.get() && mc.thePlayer.onGround) || (!mc.thePlayer.onGround && air.get())) &&
                killAura.state && killAura.target != null && !mc.thePlayer.isSneaking && keyMode

    private fun checkVoid(): Boolean {
        for (x in -1..0) for (z in -1..0) if (isVoid(x, z)) return true
        return false
    }

    private fun isVoid(X: Int, Z: Int): Boolean {
        if (mc.thePlayer.posY < 0.0) return true
        var off = 0
        while (off < mc.thePlayer.posY.toInt() + 2) {
            val bb = mc.thePlayer.entityBoundingBox.offset(X.toDouble(), (-off).toDouble(), Z.toDouble())
            if (mc.theWorld!!.getCollidingBoundingBoxes(mc.thePlayer, bb).isEmpty()) {
                off += 2
                continue
            }
            return false
        }
        return true
    }

    fun onRender3D(event: Render3DEvent) {
        val target = killAura.target ?: return
        if (!(canStrafe || alwaysRender.get()) || !render.get()) return

        GL11.glPushMatrix()
        GL11.glTranslated(
            target.lastTickPosX + (target.posX - target.lastTickPosX) * mc.timer.renderPartialTicks - mc.renderManager.renderPosX,
            target.lastTickPosY + (target.posY - target.lastTickPosY) * mc.timer.renderPartialTicks - mc.renderManager.renderPosY,
            target.lastTickPosZ + (target.posZ - target.lastTickPosZ) * mc.timer.renderPartialTicks - mc.renderManager.renderPosZ
        )
        GL11.glEnable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_LINE_SMOOTH)
        GL11.glDisable(GL11.GL_TEXTURE_2D)
        GL11.glDisable(GL11.GL_DEPTH_TEST)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
        GL11.glRotatef(90F, 1F, 0F, 0F)

        if (outLine.get()) {
            GL11.glLineWidth(thicknessValue.get() + 1.25F)
            GL11.glColor3f(0F, 0F, 0F)
            GL11.glBegin(GL11.GL_LINE_LOOP)
            for (i in 0..360 step 60 - accuracyValue.get()) {
                GL11.glVertex2f(Math.cos(i * Math.PI / 180.0).toFloat() * radius.get(),
                    Math.sin(i * Math.PI / 180.0).toFloat() * radius.get())
            }
            GL11.glEnd()
        }

        val rainbow2 = ColorUtils.LiquidSlowly(System.nanoTime(), 0, saturationValue.get(), brightnessValue.get())
        val sky = RenderUtils.skyRainbow(0, saturationValue.get(), brightnessValue.get())
        val fade = ColorUtils.fade(Color(redValue.get(), greenValue.get(), blueValue.get()), 0, 100)
        val mixer = fade // simplified; replace with ColorMixer if needed

        GL11.glLineWidth(thicknessValue.get())
        GL11.glBegin(GL11.GL_LINE_LOOP)
        for (i in 0..360 step 60 - accuracyValue.get()) {
            when (colorType.get()) {
                "Custom" -> GL11.glColor3f(redValue.get() / 255f, greenValue.get() / 255f, blueValue.get() / 255f)
                "Dynamic" -> GL11.glColor3f(redValue.get() / 255f, greenValue.get() / 255f, blueValue.get() / 255f)
                "Rainbow" -> {
                    val rainbow = Color(RenderUtils.getNormalRainbow(i, saturationValue.get(), brightnessValue.get()))
                    GL11.glColor3f(rainbow.red / 255f, rainbow.green / 255f, rainbow.blue / 255f)
                }
                "Rainbow2" -> GL11.glColor3f(rainbow2!!.red / 255f, rainbow2.green / 255f, rainbow2.blue / 255f)
                "Sky" -> GL11.glColor3f(sky.red / 255f, sky.green / 255f, sky.blue / 255f)
                "Mixer" -> GL11.glColor3f(mixer.red / 255f, mixer.green / 255f, mixer.blue / 255f)
                else -> GL11.glColor3f(fade.red / 255f, fade.green / 255f, fade.blue / 255f)
            }
            GL11.glVertex2f(Math.cos(i * Math.PI / 180.0).toFloat() * radius.get(),
                Math.sin(i * Math.PI / 180.0).toFloat() * radius.get())
        }
        GL11.glEnd()

        GL11.glDisable(GL11.GL_BLEND)
        GL11.glEnable(GL11.GL_TEXTURE_2D)
        GL11.glEnable(GL11.GL_DEPTH_TEST)
        GL11.glDisable(GL11.GL_LINE_SMOOTH)
        GL11.glPopMatrix()
        GlStateManager.resetColor()
    }
}
