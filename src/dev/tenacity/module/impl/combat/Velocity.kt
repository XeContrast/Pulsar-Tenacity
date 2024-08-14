package dev.tenacity.module.impl.combat

import dev.tenacity.event.impl.game.WorldEvent
import dev.tenacity.event.impl.network.PacketReceiveEvent
import dev.tenacity.event.impl.network.PacketSendEvent
import dev.tenacity.event.impl.player.UpdateEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.settings.Setting
import dev.tenacity.module.settings.impl.BooleanSetting
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.module.settings.impl.NumberSetting
import dev.tenacity.ui.notifications.NotificationManager
import dev.tenacity.ui.notifications.NotificationType
import dev.tenacity.utils.misc.MathUtils.getRandomInRange
import dev.tenacity.utils.player.MovementUtils.isMoving
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S12PacketEntityVelocity
import net.minecraft.network.play.server.S19PacketEntityStatus
import net.minecraft.network.play.server.S27PacketExplosion

class Velocity : Module("Velocity", Category.COMBAT, "Reduces your knockback") {
    private val mode = ModeSetting("Mode", "Packet", "Packet","WatchDog", "Matrix", "Tick", "Stack", "C0F Cancel","IntaveReduce")
    private val horizontal = NumberSetting("Horizontal", 0.0, 100.0, 0.0, 1.0)
    private val vertical = NumberSetting("Vertical", 0.0, 100.0, 0.0, 1.0)
    private val chance = NumberSetting("Chance", 100.0, 100.0, 0.0, 1.0)
    private val onlyWhileMoving = BooleanSetting("Only while moving", false)
    private val staffCheck = BooleanSetting("Staff check", false)
    var jump = false
    private var lastDamageTimestamp: Long = 0
    private var lastAlertTimestamp: Long = 0
    private var cancel = false
    private var stack = 0
    var BoolTag = false
    init {
        Setting.addParent(mode, { m: ModeSetting -> m.`is`("Packet") }, horizontal, vertical, staffCheck)
        this.addSettings(mode, horizontal, vertical, chance, onlyWhileMoving, staffCheck)
    }

    override fun onPacketSendEvent(event: PacketSendEvent) {
        if (mode.`is`("C0F Cancel")) {
            if (event.packet is C0FPacketConfirmTransaction && mc.thePlayer.hurtTime > 0) {
                event.cancel()
            }
        }
    }

    override fun onPacketReceiveEvent(e: PacketReceiveEvent) {
        this.suffix = mode.mode
        if ((onlyWhileMoving.isEnabled && !isMoving) || (chance.value != 100.0 && getRandomInRange(
                0,
                100
            ) > chance.value)
        ) return
        val packet = e.packet
        when (mode.mode) {
            "Packet" -> if (packet is S12PacketEntityVelocity) {
                val s12 = e.packet as S12PacketEntityVelocity
                if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.entityId) {
                    if (cancel(e)) return
                    s12.motionX = (s12.motionX * (horizontal.value / 100.0)).toInt()
                    s12.motionZ = (s12.motionZ * (horizontal.value / 100.0)).toInt()
                    s12.motionY = (s12.motionY * (vertical.value / 100.0)).toInt()
                }
            } else if (packet is S27PacketExplosion) {
                if (cancel(e)) return
                val s27 = e.packet as S27PacketExplosion
                s27.motionX *= (horizontal.value / 100.0).toFloat()
                s27.motionZ *= (horizontal.value / 100.0).toFloat()
                s27.motionY *= (vertical.value / 100.0).toFloat()
            } else if (e.packet is S19PacketEntityStatus) {
                val s19 = e.packet as S19PacketEntityStatus
                if (mc.thePlayer != null && s19.entityId == mc.thePlayer.entityId && s19.opCode.toInt() == 2) {
                    lastDamageTimestamp = System.currentTimeMillis()
                }
            }
            "WatchDog" -> {
                if (packet is S12PacketEntityVelocity && packet.getEntityID() == mc.thePlayer.entityId) {
                    e.cancel()
                    mc.thePlayer.motionY = packet.getMotionY().toDouble() / 8000.0
                }
            }

            "C0F Cancel" -> {
                if (packet is S12PacketEntityVelocity) {
                    val s12 = e.packet as S12PacketEntityVelocity
                    if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.entityId) {
                        e.cancel()
                    }
                }
                if (packet is S27PacketExplosion) {
                    e.cancel()
                }
            }

            "Stack" -> {
                if (packet is S12PacketEntityVelocity) {
                    val s12 = packet
                    cancel = !cancel
                    if (cancel) {
                        e.cancel()
                    }
                }
                if (packet is S27PacketExplosion) {
                    e.cancel()
                }
            }

            "Matrix" -> if (packet is S12PacketEntityVelocity) {
                val s12 = e.packet as S12PacketEntityVelocity
                if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.entityId) {
                    s12.motionX = (s12.motionX * (5 / 100.0)).toInt()
                    s12.motionZ = (s12.motionZ * (5 / 100.0)).toInt()
                    s12.motionY = (s12.motionY * (100 / 100.0)).toInt()
                }
            }

            "Tick" -> if (packet is S12PacketEntityVelocity) {
                val s12 = e.packet as S12PacketEntityVelocity
                if (mc.thePlayer != null && s12.getEntityID() == mc.thePlayer.entityId && mc.thePlayer.ticksExisted % 3 == 0) {
                    s12.motionX = (s12.motionX * (5 / 100.0)).toInt()
                    s12.motionZ = (s12.motionZ * (5 / 100.0)).toInt()
                    s12.motionY = (s12.motionY * (100 / 100.0)).toInt()
                }
            }

        }
    }

    override fun onWorldEvent(event: WorldEvent) {
        stack = 0
    }

    private fun cancel(e: PacketReceiveEvent): Boolean {
        if (staffCheck.isEnabled && System.currentTimeMillis() - lastDamageTimestamp > 500) {
            if (System.currentTimeMillis() - lastAlertTimestamp > 250) {
                NotificationManager.post(NotificationType.WARNING, "Velocity", "Suspicious knockback detected!", 2f)
                lastAlertTimestamp = System.currentTimeMillis()
            }
            return true
        }
        if (horizontal.value == 0.0 && vertical.value == 0.0) {
            e.cancel()
            return true
        }
        return false
    }
    private fun onupdate(event: UpdateEvent){
        if (mc.thePlayer.hurtTime >= 1&&mc.thePlayer.onGround) {
            mc.gameSettings.keyBindJump.pressed = true
            jump = true
        }else if (jump){
            mc.gameSettings.keyBindJump.pressed = false
            jump = false
        }
        if (mode.`is`("IntaveReduce")) {
            if (mc.thePlayer.onGround) {
                BoolTag = true
                mc.thePlayer.motionX *= 0.66
                mc.thePlayer.motionZ *= 0.66
            }
        }else{
            BoolTag = false
        }
    }
}
