package dev.tenacity.module.impl.player

import dev.tenacity.event.impl.network.PacketSendEvent
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.settings.impl.NumberSetting
import dev.tenacity.utils.server.PacketUtils
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

class SpeedMine : Module("SpeedMine", Category.PLAYER, "mines blocks faster") {
    private val speed = NumberSetting("Speed", 1.4, 3.0, 1.0, 0.1)
    private var facing: EnumFacing? = null
    private var pos: BlockPos? = null
    private var boost = false
    private var damage = 0f

    init {
        this.addSettings(speed)
    }

    override fun onMotionEvent(e: MotionEvent) {
        if (e.isPre) {
            mc.playerController.blockHitDelay = 0
            if (pos != null && boost) {
                val blockState = mc.theWorld.getBlockState(pos) ?: return

                try {
                    damage += (blockState.block.getPlayerRelativeBlockHardness(mc.thePlayer) * speed.value).toFloat()
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    return
                }

                if (damage >= 1) {
                    try {
                        mc.theWorld.setBlockState(pos, Blocks.air.defaultState, 11)
                    } catch (ex: Exception) {
                        ex.printStackTrace()
                        return
                    }
                    PacketUtils.sendPacketNoEvent(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK,
                            pos,
                            facing
                        )
                    )
                    damage = 0f
                    boost = false
                }
            }
        }
    }

    override fun onPacketSendEvent(e: PacketSendEvent) {
        if (e.packet is C07PacketPlayerDigging) {
            val packet = e.packet as C07PacketPlayerDigging
            if (packet.status == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                boost = true
                pos = packet.position
                facing = packet.facing
                damage = 0f
            } else if ((packet.status == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) or (packet.status == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK)) {
                boost = false
                pos = null
                facing = null
            }
        }
    }
}
