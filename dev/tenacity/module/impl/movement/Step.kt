package dev.tenacity.module.impl.movement

import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.event.impl.player.StepConfirmEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.module.settings.impl.NumberSetting
import dev.tenacity.utils.time.TimerUtil
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition

@Suppress("unused")
class Step : Module("Step", Category.MOVEMENT, "step up blocks") {
    private val mode = ModeSetting("Mode", "Vanilla", "Vanilla", "NCP", "Full Jump Packets")

    private val height = NumberSetting("Height", 1.0, 10.0, 1.0, 0.5)

    private val timer = NumberSetting("Timer", 1.0, 2.0, 0.1, 0.1)

    private var hasStepped = false

    private val timerUtil = TimerUtil()

    init {
        this.addSettings(mode, height, timer)
    }

    override fun onMotionEvent(event: MotionEvent) {
        this.suffix = mode.mode
        if (mc.thePlayer.onGround) {
            if (mc.thePlayer.stepHeight != height.value.toFloat()) mc.thePlayer.stepHeight = height.value.toFloat()
        } else {
            if (mc.thePlayer.stepHeight != 0.625f) mc.thePlayer.stepHeight = 0.625f
        }
        if (timerUtil.hasTimeElapsed(20) && hasStepped) {
            mc.timer.timerSpeed = 1f
            hasStepped = false
            isStepping = false
        }
    }

    override fun onStepConfirmEvent(event: StepConfirmEvent) {
        val diffY = mc.thePlayer.entityBoundingBox.minY - mc.thePlayer.posY
        if (diffY > 0.625f && diffY <= 1.5f && mc.thePlayer.onGround) {
            mc.timer.timerSpeed = timer.value.toFloat()
            timerUtil.reset()
            hasStepped = true
            isStepping = true
            when (mode.mode) {
                "NCP" -> for (offset in doubleArrayOf(
                    0.41999998688698,
                    0.7531999805212
                )) mc.thePlayer.sendQueue.addToSendQueue(
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ, false
                    )
                )

                "Full Jump Packets" -> for (offset in doubleArrayOf(
                    0.41999998688698,
                    0.7531999805212,
                    1.00133597911214,
                    1.16610926093821,
                    1.24918707874468,
                    1.24918707874468,
                    1.1707870772188,
                    1.0155550727022
                )) mc.thePlayer.sendQueue.addToSendQueue(
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX, mc.thePlayer.posY + offset, mc.thePlayer.posZ, false
                    )
                )
            }
        }
    }

    override fun onDisable() {
        mc.thePlayer.stepHeight = 0.625f
        super.onDisable()
    }

    companion object {
        var isStepping: Boolean = false
    }
}
