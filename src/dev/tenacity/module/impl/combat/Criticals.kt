package dev.tenacity.module.impl.combat

import dev.tenacity.Tenacity
import dev.tenacity.event.impl.network.PacketSendEvent
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.impl.movement.Flight
import dev.tenacity.module.impl.movement.Step
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.module.settings.impl.NumberSetting
import dev.tenacity.utils.server.PacketUtils
import dev.tenacity.utils.time.TimerUtil
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C18PacketSpectate
import java.util.*
import java.util.concurrent.ThreadLocalRandom

@Suppress("unused")
class Criticals : Module("Criticals", Category.COMBAT, "Crit attacks") {
    private var stage = false
    private val offset = 0.0
    private var groundTicks = 0
    private val mode = ModeSetting("Mode", "Watchdog", "Watchdog", "Packet", "Dev", "Verus","BlocksMC")
    private val bmcmode = ModeSetting("BlocksMCMode","Motion","Motion","Packet")
    private val watchdogMode = ModeSetting("Watchdog Mode", "Packet", "Packet", "Edit")
    private val delay = NumberSetting("Delay", 1.0, 20.0, 0.0, 1.0)
    private val timer = TimerUtil()

    init {
        delay.addParent(mode) { m: ModeSetting -> !(m.`is`("Verus") || (m.`is`("Watchdog") && watchdogMode.`is`("Edit"))) }
        watchdogMode.addParent(mode) { m: ModeSetting -> m.`is`("Watchdog") }
        bmcmode.addParent(mode) { m : ModeSetting -> m.`is`("BlocksMC")}
        this.addSettings(mode, watchdogMode,bmcmode, delay)
    }

    override fun onPacketSendEvent(e: PacketSendEvent) {
    }

    override fun onMotionEvent(e: MotionEvent) {
        this.suffix = mode.mode
        when (mode.mode) {
            "Watchdog" -> {
                if (watchdogMode.`is`("Packet")) {
                    if (KillAura.attacking && e.isOnGround && !Step.isStepping) {
                        if (KillAura.target != null && KillAura.target!!.hurtTime >= delay.value.toInt()) {
                            for (offset in doubleArrayOf(0.06, 0.01)) {
                                mc.thePlayer.sendQueue.addToSendQueue(
                                    C04PacketPlayerPosition(
                                        mc.thePlayer.posX,
                                        mc.thePlayer.posY + offset + (Math.random() * 0.001),
                                        mc.thePlayer.posZ,
                                        false
                                    )
                                )
                            }
                        }
                    }
                }
                if (e.isPre && watchdogMode.`is`("Edit") && !Tenacity.INSTANCE.isEnabled(Flight::class.java) && !Step.isStepping && KillAura.attacking) {
                    if (e.isOnGround) {
                        groundTicks++
                        if (groundTicks > 2) {
                            stage = !stage
                            e.y = e.y + (if (stage) 0.015 else 0.01) - Math.random() * 0.0001
                            e.isOnGround = false
                        }
                    } else {
                        groundTicks = 0
                    }
                }
            }

            "BlocksMC" -> {
                if (KillAura.attacking && !Step.isStepping) {
                    if (KillAura.target != null && KillAura.target!!.hurtTime >= delay.value.toInt()) {
                        when (bmcmode.mode) {
                            "Motion" -> {
                                mc.thePlayer.sendQueue.addToSendQueue(
                                    C04PacketPlayerPosition(
                                        mc.thePlayer.posX,
                                        mc.thePlayer.posY + 0.001091981,
                                        mc.thePlayer.posZ,
                                        true
                                    )
                                )
                                mc.thePlayer.sendQueue.addToSendQueue(
                                    C04PacketPlayerPosition(
                                        mc.thePlayer.posX,
                                        mc.thePlayer.posY,
                                        mc.thePlayer.posZ,
                                        false
                                    )
                                )
                            }

                            "Packet" -> {
                                if (mc.thePlayer.ticksExisted % 4 == 0) {
                                    mc.thePlayer.sendQueue.addToSendQueue(
                                        C04PacketPlayerPosition(
                                            mc.thePlayer.posX,
                                            mc.thePlayer.posY + 0.0011,
                                            mc.thePlayer.posZ,
                                            true
                                        )
                                    )
                                    mc.thePlayer.sendQueue.addToSendQueue(
                                        C04PacketPlayerPosition(
                                            mc.thePlayer.posX,
                                            mc.thePlayer.posY,
                                            mc.thePlayer.posZ,
                                            false
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }

            "Packet" -> if (mc.objectMouseOver.entityHit != null && mc.thePlayer.onGround) {
                if (mc.objectMouseOver.entityHit.hurtResistantTime > delay.value.toInt()) {
                    for (offset in doubleArrayOf(0.006253453, 0.002253453, 0.001253453)) {
                        mc.thePlayer.sendQueue.addToSendQueue(
                            C04PacketPlayerPosition(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY + offset,
                                mc.thePlayer.posZ,
                                false
                            )
                        )
                    }
                }
            }

            "Dev" -> if (mc.objectMouseOver.entityHit != null && mc.thePlayer.onGround) {
                if (mc.objectMouseOver.entityHit.hurtResistantTime > delay.value.toInt()) {
                    for (offset in doubleArrayOf(0.06253453, 0.02253453, 0.001253453, 0.0001135346)) {
                        mc.thePlayer.sendQueue.addToSendQueue(
                            C04PacketPlayerPosition(
                                mc.thePlayer.posX,
                                mc.thePlayer.posY + offset,
                                mc.thePlayer.posZ,
                                false
                            )
                        )
                        PacketUtils.sendPacketNoEvent(C18PacketSpectate(UUID.randomUUID()))
                    }
                }
            }

            "Verus" -> if (KillAura.attacking && KillAura.target != null && e.isOnGround) {
                when (KillAura.target!!.hurtResistantTime) {
                    17, 19 -> {
                        e.isOnGround = false
                        e.y += ThreadLocalRandom.current().nextDouble(0.001, 0.0011)
                    }

                    18, 20 -> {
                        e.isOnGround = false
                        e.y += 0.03 + ThreadLocalRandom.current().nextDouble(0.001, 0.0011)
                    }
                }
            }
        }
    }
}
