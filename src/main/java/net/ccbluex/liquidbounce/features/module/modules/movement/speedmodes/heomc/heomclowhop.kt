/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.heomc

import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.MotionEvent
import net.ccbluex.liquidbounce.features.module.modules.movement.speedmodes.SpeedMode
import net.ccbluex.liquidbounce.utils.MovementUtils

class HeomcLowHop : SpeedMode("HeomcLowHop") {
    
    private var airTicks = 0

    override fun onMotion(event: MotionEvent) {
        if (event.eventState != EventState.PRE) return
        
        val player = mc.thePlayer

        if (MovementUtils.isMoving()) {
            if (player.onGround) {
                airTicks = 0
                
                // Ground speed boost
                if (MovementUtils.getSpeed() < 0.32) {
                    MovementUtils.strafe(0.37f)
                }
                
                // Jump with lower motion
                player.jump()
                player.motionY = 0.33
            } else {
                airTicks++

                // Air speed boosts
                when (airTicks) {
                    1 -> {
                        if (MovementUtils.getSpeed() < 0.20) {
                            MovementUtils.strafe(1.01f)
                        }
                    }
                    9 -> {
                        if (MovementUtils.getSpeed() < 0.29) {
                            MovementUtils.strafe(1.007f) 
                        }
                    }
                }

                // Additional boost when moving upwards in early air ticks
                if (player.motionY > 0 && airTicks <= 2 && MovementUtils.getSpeed() < 0.2) {
                    MovementUtils.strafe(1.02f)
                }
            }
        } else {
            // Reset motion when not moving
            player.motionX = 0.0
            player.motionZ = 0.0
        }
    }

    override fun onEnable() {
        airTicks = 0
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
    }
}
