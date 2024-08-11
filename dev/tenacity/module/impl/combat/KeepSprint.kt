package dev.tenacity.module.impl.combat

import dev.tenacity.event.impl.player.KeepSprintEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module

class KeepSprint : Module("KeepSprint", Category.COMBAT, "Stops sprint reset after hitting") {
    override fun onKeepSprintEvent(event: KeepSprintEvent) {
        event.cancel()
    }
}
