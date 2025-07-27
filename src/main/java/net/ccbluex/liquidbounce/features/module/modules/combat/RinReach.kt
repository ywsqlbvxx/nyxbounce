/*
 * RinBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/rattermc/rinbounce69
 */
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.attack.EntityUtils
import net.ccbluex.liquidbounce.utils.attack.CombatCheck
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.config.*
import net.minecraft.client.entity.EntityOtherPlayerMP
import net.minecraft.entity.EntityLivingBase
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.world.WorldSettings

object RinReach : Module("RinReach", Category.COMBAT) {

    private val modeValue by ListValue("Mode", arrayOf("RinIntave", "RinFakePlayer"), "RinFakePlayer")
    private val aura by BoolValue("Aura", false)
    private val pulseDelayValue by IntValue("PulseDelay", 200, 50..500) 
    private val intaveTestHurtTimeValue by IntValue("Packets", 5, 0..30) { modeValue == "RinIntave" }

    private var fakePlayer: EntityOtherPlayerMP? = null
    private var currentTarget: EntityLivingBase? = null
    private var shown = false
    private val pulseTimer = MSTimer()

    override fun onEnable() {
    }

    override fun onDisable() {
        removeFakePlayer()
    }

    private fun removeFakePlayer() {
        fakePlayer?.let {
            currentTarget = null
            mc.theWorld?.removeEntity(it)
            fakePlayer = null
            shown = false
        }
    }

    private fun attackEntity(entity: EntityLivingBase) {
        mc.thePlayer?.run {
            swingItem()
            mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, C02PacketUseEntity.Action.ATTACK))
            if (mc.playerController.currentGameType != WorldSettings.GameType.SPECTATOR) {
                attackTargetEntityWithCurrentItem(entity)
            }
        }
    }

    private fun createFakePlayer(target: EntityLivingBase) {
        val world = mc.theWorld ?: return
        val playerInfo = mc.netHandler.getPlayerInfo(target.uniqueID) ?: return
        val faker = EntityOtherPlayerMP(world, playerInfo.gameProfile).apply {
            rotationYawHead = target.rotationYawHead
            renderYawOffset = target.renderYawOffset
            copyLocationAndAnglesFrom(target)
            health = target.health
            (0..4).forEach { index ->
                target.getEquipmentInSlot(index)?.let { setCurrentItemOrArmor(index, it) }
            }
        }
        world.addEntityToWorld(-1337, faker)
        fakePlayer = faker
        shown = true
    }

    fun onAttack(event: AttackEvent) {
        val target = event.targetEntity as? EntityLivingBase ?: return
        CombatCheck.setTarget(target)

        when (modeValue) {
            "RinIntave", "RinFakePlayer" -> {
                if (fakePlayer == null) {
                    currentTarget = target
                    createFakePlayer(target)
                } else if (event.targetEntity == fakePlayer) {
                    currentTarget?.let { attackEntity(it) }
                    event.cancelEvent()
                } else {
                    removeFakePlayer()
                    currentTarget = target
                    createFakePlayer(target)
                }
            }
        }
    }

    fun onUpdate(event: UpdateEvent) {
        CombatCheck.updateCombatState()
        if (mc.thePlayer == null || currentTarget == null || !CombatCheck.inCombat) {
            removeFakePlayer()
            return
        }

        if (aura.get() && !LiquidBounce.moduleManager[KillAura::class.java]!!.state) {
            removeFakePlayer()
            return
        }

        when (modeValue) {
            "RinIntave" -> {
                fakePlayer?.let { faker ->
                    currentTarget?.let { target ->
                        if (!EntityUtils.isRendered(faker) || target.isDead || !EntityUtils.isRendered(target)) {
                            removeFakePlayer()
                        } else {
                            faker.health = target.health
                            (0..4).forEach { index ->
                                target.getEquipmentInSlot(index)?.let { faker.setCurrentItemOrArmor(index, it) }
                            }
                            if (mc.thePlayer.ticksExisted % intaveTestHurtTimeValue.get() == 0) {
                                faker.rotationYawHead = target.rotationYawHead
                                faker.renderYawOffset = target.renderYawOffset
                                faker.copyLocationAndAnglesFrom(target)
                                pulseTimer.reset()
                            }
                        }
                    }
                }

                if (!shown && currentTarget != null && mc.netHandler.getPlayerInfo(currentTarget?.uniqueID)?.gameProfile != null) {
                    createFakePlayer(currentTarget!!)
                }
            }
            "RinFakePlayer" -> {
                fakePlayer?.let { faker ->
                    currentTarget?.let { target ->
                        if (!EntityUtils.isRendered(faker) || target.isDead || !EntityUtils.isRendered(target)) {
                            removeFakePlayer()
                        } else {
                            faker.health = target.health
                            (0..4).forEach { index ->
                                target.getEquipmentInSlot(index)?.let { faker.setCurrentItemOrArmor(index, it) }
                            }
                            if (pulseTimer.hasTimePassed(pulseDelayValue.get().toLong())) {
                                faker.rotationYawHead = target.rotationYawHead
                                faker.renderYawOffset = target.renderYawOffset
                                faker.copyLocationAndAnglesFrom(target)
                                pulseTimer.reset()
                            }
                        }
                    }
                }

                if (!shown && currentTarget != null && mc.netHandler.getPlayerInfo(currentTarget?.uniqueID)?.gameProfile != null) {
                    createFakePlayer(currentTarget!!)
                }
            }
        }
    }
}
