/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.UpdateEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.InventoryUtils
import net.ccbluex.liquidbounce.utils.MovementUtils
import net.ccbluex.liquidbounce.utils.item.ItemUtils
import net.ccbluex.liquidbounce.utils.timing.MSTimer
import net.ccbluex.liquidbounce.utils.timing.TimeUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.IntegerValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.client.gui.inventory.GuiInventory
import net.minecraft.init.Blocks
import net.minecraft.item.*
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C09PacketHeldItemChange

object InvManager : Module(name = "InvManager", category = Category.PLAYER) {

    private val maxDelayValue = IntegerValue("MaxDelay", 600, 0, 1000)
    private val minDelayValue = IntegerValue("MinDelay", 400, 0, 1000)
    private val invOpenValue = BoolValue("InvOpen", false)
    private val simulateInventory = BoolValue("InvSpoof", true)
    private val noMoveValue = BoolValue("NoMove", false)
    private val hotbarValue = BoolValue("Hotbar", true)
    private val randomSlotValue = BoolValue("RandomSlot", false)
    private val sortValue = BoolValue("Sort", true)
    private val throwValue = BoolValue("Drop", true)
    private val itemDelayValue = IntegerValue("ItemDelay", 0, 0, 5000)

    private val items = arrayOf("None", "Sword", "Bow", "Pickaxe", "Axe", "Food", "Block", "Water", "Gapple", "Pearl", "Potion")
    private val sortSlot1Value = ListValue("SortSlot-1", items, "Sword")
    private val sortSlot2Value = ListValue("SortSlot-2", items, "Bow")
    private val sortSlot3Value = ListValue("SortSlot-3", items, "Pickaxe") 
    private val sortSlot4Value = ListValue("SortSlot-4", items, "Axe")
    private val sortSlot5Value = ListValue("SortSlot-5", items, "Block")
    private val sortSlot6Value = ListValue("SortSlot-6", items, "Food")
    private val sortSlot7Value = ListValue("SortSlot-7", items, "Water")
    private val sortSlot8Value = ListValue("SortSlot-8", items, "Gapple")
    private val sortSlot9Value = ListValue("SortSlot-9", items, "Pearl")

    private var delay = 0L
    private val timer = MSTimer()

    @EventTarget 
    fun onUpdate(event: UpdateEvent) {
        if (!timer.hasTimePassed(delay))
            return

        if (mc.currentScreen !is GuiInventory && invOpenValue.get())
            return

        if (noMoveValue.get() && MovementUtils.isMoving())
            return

        // Sort inventory
        if (sortValue.get()) {
            for (slot in 0..8) {
                val bestItem = findBestItem(slot)
                if (bestItem != -1 && bestItem != slot) {
                    mc.playerController.windowClick(0, bestItem + 36, slot, 2, mc.thePlayer)
                    timer.reset()
                    delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
                    return
                }
            }
        }

        // Throw useless items
        if (throwValue.get()) {
            val garbageItems = (9..44).filter { slot -> 
                val stack = mc.thePlayer.inventoryContainer.getSlot(slot).stack
                stack != null && !isUsefulItem(stack)
            }

            if (garbageItems.isNotEmpty()) {
                val slot = if (randomSlotValue.get()) {
                    garbageItems.random()
                } else {
                    garbageItems.first()
                }

                mc.playerController.windowClick(
                    mc.thePlayer.inventoryContainer.windowId,
                    slot,
                    1,
                    4,
                    mc.thePlayer
                )

                delay = TimeUtils.randomDelay(minDelayValue.get(), maxDelayValue.get())
                timer.reset()
                return
            }
        }
    }

    private fun findBestItem(targetSlot: Int): Int {
        val slotType = when (targetSlot) {
            0 -> sortSlot1Value.get()
            1 -> sortSlot2Value.get() 
            2 -> sortSlot3Value.get()
            3 -> sortSlot4Value.get()
            4 -> sortSlot5Value.get()
            5 -> sortSlot6Value.get()
            6 -> sortSlot7Value.get()
            7 -> sortSlot8Value.get()
            8 -> sortSlot9Value.get()
            else -> "None"
        }

        var bestSlot = -1
        var bestDamage = -1.0

        for (slot in 9..44) {
            val stack = mc.thePlayer.inventoryContainer.getSlot(slot).stack ?: continue
            
            val matches = when (slotType.toLowerCase()) {
                "sword" -> stack.item is ItemSword
                "bow" -> stack.item is ItemBow
                "pickaxe" -> stack.item is ItemPickaxe
                "axe" -> stack.item is ItemAxe
                "food" -> stack.item is ItemFood
                "block" -> stack.item is ItemBlock && !InventoryUtils.BLOCK_BLACKLIST.contains((stack.item as ItemBlock).block)
                "water" -> stack.item is ItemBucket && (stack.item as ItemBucket).isFull == Blocks.flowing_water
                "gapple" -> stack.item is ItemAppleGold
                "pearl" -> stack.item is ItemEnderPearl
                "potion" -> stack.item is ItemPotion && ItemPotion.isSplash(stack.itemDamage)
                else -> false
            }

            if (!matches) continue

            val damage = when (stack.item) {
                is ItemTool -> (stack.item as ItemTool).toolMaterial.harvestLevel.toDouble()
                is ItemSword -> (stack.item as ItemSword).damageVsEntity + ItemUtils.getWeaponEnchantFactor(stack)
                is ItemArmor -> (stack.item as ItemArmor).armorMaterial.getDamageReductionAmount(
                    (stack.item as ItemArmor).armorType
                ).toDouble()
                else -> 0.0
            }

            if (damage > bestDamage) {
                bestDamage = damage
                bestSlot = slot - 36
            }
        }

        return bestSlot
    }

    private fun isUsefulItem(stack: ItemStack): Boolean {
        val item = stack.item

        return when {
            item is ItemSword || item is ItemTool -> {
                val damage = when (item) {
                    is ItemSword -> item.damageVsEntity + ItemUtils.getWeaponEnchantFactor(stack)
                    is ItemTool -> item.toolMaterial.harvestLevel.toDouble()
                    else -> 0.0
                }

                val type = when (item) {
                    is ItemSword -> "sword"
                    is ItemPickaxe -> "pickaxe"
                    is ItemAxe -> "axe"
                    else -> ""
                }

                // Check if we want this type of item in hotbar
                for (i in 0..8) {
                    val slotType = when (i) {
                        0 -> sortSlot1Value.get()
                        1 -> sortSlot2Value.get()
                        2 -> sortSlot3Value.get()
                        3 -> sortSlot4Value.get()
                        4 -> sortSlot5Value.get()
                        5 -> sortSlot6Value.get()
                        6 -> sortSlot7Value.get()
                        7 -> sortSlot8Value.get()
                        8 -> sortSlot9Value.get()
                        else -> "none"
                    }
                    
                    if (slotType.equals(type, ignoreCase = true)) {
                        return true
                    }
                }

                false
            }
            item is ItemBow -> true
            item is ItemFood -> true
            item is ItemBlock -> !InventoryUtils.BLOCK_BLACKLIST.contains((item as ItemBlock).block) 
            item is ItemAppleGold -> true
            item is ItemEnderPearl -> true
            item is ItemPotion && ItemPotion.isSplash(stack.itemDamage) -> true
            else -> false
        }
    }
}
