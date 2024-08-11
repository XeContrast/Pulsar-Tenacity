package dev.tenacity.module.impl.movement

import dev.tenacity.Tenacity
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.impl.player.NoSlow
import dev.tenacity.module.settings.impl.BooleanSetting

class Sprint : Module("Sprint", Category.MOVEMENT, "Sprints automatically") {
    private val omniSprint = BooleanSetting("Omni Sprint", false)

    init {
        this.addSettings(omniSprint)
    }

    override fun onMotionEvent(event: MotionEvent) {
        if (Tenacity.INSTANCE.moduleCollection[Scaffold::class.java].isEnabled && (!Scaffold.sprint.isEnabled || Scaffold.isDownwards)) {
            mc.gameSettings.keyBindSprint.pressed = false
            mc.thePlayer.isSprinting = false
            return
        }
        if (omniSprint.isEnabled) {
            mc.thePlayer.isSprinting = true
        } else {
            if (mc.thePlayer.isUsingItem) {
                if (mc.thePlayer.moveForward > 0 && (Tenacity.INSTANCE.isEnabled(
                        NoSlow::class.java
                    ) || !mc.thePlayer.isUsingItem) && !mc.thePlayer.isSneaking && !mc.thePlayer.isCollidedHorizontally && mc.thePlayer.foodStats.foodLevel > 6
                ) {
                    mc.thePlayer.isSprinting = true
                }
            } else {
                mc.gameSettings.keyBindSprint.pressed = true
            }
        }
    }

    override fun onDisable() {
        mc.thePlayer.isSprinting = false
        mc.gameSettings.keyBindSprint.pressed = false
        super.onDisable()
    }
}
