package dev.tenacity.module.impl.movement

import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.settings.impl.ModeSetting

class Spider : Module("Spider", Category.MOVEMENT, "Climbs you up walls like a spider") {
    private val mode = ModeSetting("Mode", "Vanilla", "Vanilla", "Verus")

    init {
        addSettings(mode)
    }

    override fun onMotionEvent(event: MotionEvent) {
        this.suffix = mode.mode
        if (mc.thePlayer.isCollidedHorizontally) {
            if (!mc.thePlayer.onGround && mc.thePlayer.isCollidedVertically) return
            when (mode.mode) {
                "Vanilla" -> mc.thePlayer.jump()
                "Verus" -> if (mc.thePlayer.ticksExisted % 3 == 0) mc.thePlayer.motionY = 0.42
            }
        }
    }
}
