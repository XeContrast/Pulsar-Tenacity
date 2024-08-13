package dev.tenacity.module.impl.player

import dev.tenacity.Tenacity
import dev.tenacity.event.impl.network.PacketSendEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.impl.movement.Speed
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.module.settings.impl.NumberSetting
import dev.tenacity.utils.server.PacketUtils
import dev.tenacity.utils.time.TimerUtil
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import java.util.function.Consumer

class AntiVoid : Module("AntiVoid", Category.PLAYER, "saves you from the void") {
    private val mode = ModeSetting("Mode", "Watchdog", "Watchdog")
    private val fallDist = NumberSetting("Fall Distance", 3.0, 20.0, 1.0, 0.5)
    private val timer = TimerUtil()
    private val reset = false
    private var lastGroundY = 0.0

    private val packets: MutableList<Packet<*>> = ArrayList()

    init {
        this.addSettings(mode, fallDist)
    }

    override fun onPacketSendEvent(event: PacketSendEvent) {
        if (mode.`is`("Watchdog") && !Tenacity.INSTANCE.moduleCollection.getModule(Speed::class.java).isEnabled) {
            if (event.packet is C03PacketPlayer) {
                if (!isBlockUnder) {
                    if (mc.thePlayer.fallDistance < fallDist.value) {
                        event.cancel()
                        packets.add(event.packet)
                    } else {
                        if (packets.isNotEmpty()) {
                            for (packet in packets) {
                                val c03 = packet as C03PacketPlayer
                                c03.setY(lastGroundY)
                                PacketUtils.sendPacketNoEvent(packet)
                            }
                            packets.clear()
                        }
                    }
                } else {
                    lastGroundY = mc.thePlayer.posY
                    if (packets.isNotEmpty()) {
                        packets.forEach(Consumer { packet: Packet<*>? -> PacketUtils.sendPacketNoEvent(packet) })
                        packets.clear()
                    }
                }
            }
        }
    }

    private val isBlockUnder: Boolean
        get() {
            if (mc.thePlayer.posY < 0) return false
            var offset = 0
            while (offset < mc.thePlayer.posY.toInt() + 2) {
                val bb = mc.thePlayer.entityBoundingBox.offset(0.0, -offset.toDouble(), 0.0)
                if (mc.theWorld.getCollidingBoundingBoxes(mc.thePlayer, bb).isNotEmpty()) {
                    return true
                }
                offset += 2
            }
            return false
        }
}
