package dev.tenacity.module.impl.combat

import dev.tenacity.event.impl.player.AttackEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.utils.server.PacketUtils
import net.minecraft.network.play.client.C0BPacketEntityAction

class SuperKnockback :
    Module("SuperKnockback", Category.COMBAT, "Makes the player your attacking take extra knockback") {
    override fun onAttackEvent(event: AttackEvent) {
        if (event.targetEntity != null) {
            if (mc.thePlayer.isSprinting) PacketUtils.sendPacketNoEvent(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SPRINTING
                )
            )

            PacketUtils.sendPacketNoEvent(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.START_SPRINTING
                )
            )
            PacketUtils.sendPacketNoEvent(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.STOP_SPRINTING
                )
            )
            PacketUtils.sendPacketNoEvent(
                C0BPacketEntityAction(
                    mc.thePlayer,
                    C0BPacketEntityAction.Action.START_SPRINTING
                )
            )
        }
    }
}
