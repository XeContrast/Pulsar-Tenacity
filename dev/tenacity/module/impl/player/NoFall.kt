package dev.tenacity.module.impl.player

import dev.tenacity.event.impl.player.BoundingBoxEvent
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.settings.impl.BooleanSetting
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.utils.player.MovementUtils
import dev.tenacity.utils.server.PacketUtils
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.AxisAlignedBB
import kotlin.math.max

@Suppress("unused")
class NoFall : Module("NoFall", Category.PLAYER, "prevents fall damage") {
    private val mode = ModeSetting("Mode", "Vanilla", "Vanilla", "Packet", "Verus", "WatchDog")
    private val watchdogmode = ModeSetting("WatchDogMode","Motion","Motion","Position")
    private val prediction = BooleanSetting("Prediction",false)
    private val dist = 0.0
    private val doNofall = false
    private val lastFallDistance = 0.0
    private val c04 = false
    private var jump = false
    private var ticks = false
    private var fall = 0

    init {
        this.addSettings(mode,watchdogmode,prediction)
        watchdogmode.addParent(mode) {modeSetting : ModeSetting -> modeSetting.`is`("WatchDog")}
        prediction.addParent(mode) {modeSetting : ModeSetting -> modeSetting.`is`("WatchDog")}
    }

    override fun onDisable() {
        fall = 0
        ticks = false
        mc.timer.timerSpeed = 1f
        jump = false
    }

    override fun onMotionEvent(event: MotionEvent) {
        this.suffix = mode.mode
        if (event.isPre) {
            if (mode.`is`("WatchDog")) {
                //分割
                if (mc.thePlayer.onGround) {
                    fall = 0
                } else {
                    if (prediction.isEnabled) {
                        fall -= MovementUtils.predictedMotion(mc.thePlayer.motionY, 0).toInt()
                    }
                    when (watchdogmode.mode) {
                        "Position" -> {
                            fall += max(mc.thePlayer.lastTickPosY - event.y, 0.0).toInt()
                        }

                        "Motion" -> {
                            fall += max(-mc.thePlayer.motionY, 0.0).toInt()
                        }
                    }
                    if (fall > 2.5) {
                        mc.timer.timerSpeed = 0.5f
                        fall = 0
                        mc.netHandler.addToSendQueue(C03PacketPlayer(true))
                        ticks = true
                    } else {
                        if (ticks) {
                            MovementUtils.resetTimer()
                            ticks = false
                        }
                    }
                }
            }
            if (mc.thePlayer.fallDistance > 3.0 && isBlockUnder) {
                when (mode.mode) {
                    "Vanilla" -> event.isOnGround = true
                    "Packet" -> PacketUtils.sendPacket(C03PacketPlayer(true))
                }
                mc.thePlayer.fallDistance = 0f
            }
        }
    }

    override fun onBoundingBoxEvent(event: BoundingBoxEvent) {
        if (mode.`is`("Verus") && mc.thePlayer.fallDistance > 2) {
            val axisAlignedBB = AxisAlignedBB.fromBounds(-5.0, -1.0, -5.0, 5.0, 1.0, 5.0)
                .offset(event.blockPos.x.toDouble(), event.blockPos.y.toDouble(), event.blockPos.z.toDouble())
            event.boundingBox = axisAlignedBB
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
