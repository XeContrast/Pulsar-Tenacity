package dev.tenacity.module.impl.player

import dev.tenacity.event.impl.network.PacketSendEvent
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.event.impl.player.SlowDownEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.impl.combat.KillAura
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.utils.player.ChatUtil
import dev.tenacity.utils.player.MovementUtils.isMoving
import dev.tenacity.utils.server.PacketUtils
import net.minecraft.item.ItemBucketMilk
import net.minecraft.item.ItemPotion
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing

class NoSlow : Module("NoSlow", Category.PLAYER, "prevent item slowdown") {
    private val mode = ModeSetting("Mode", "Watchdog", "Vanilla", "NCP", "WatchDog")
    private var synced = false
    private var offGroundTicks = 0
    private var send = false

    init {
        this.addSettings(mode)
    }

    override fun onSlowDownEvent(event: SlowDownEvent) {
        val helditem = mc.thePlayer.heldItem
        if (mode.`is`("WatchDog")) {
            if (helditem.item is ItemPotion || helditem.item is ItemBucketMilk) {
                return
            }
        }
        event.cancel()
    }

    override fun onDisable() {
        offGroundTicks = 0
        send = false
    }

    override fun onMotionEvent(e: MotionEvent) {
        this.suffix = mode.mode
        when (mode.mode) {
            "WatchDog" -> {
                if (mc.thePlayer.isUsingItem || mc.thePlayer.isBlocking || KillAura.wasBlocking) {
                    val heldItem = mc.thePlayer.heldItem
                    if (e.isPre) {
                        // Food Only
                        if (heldItem.item is ItemPotion || heldItem.item is ItemBucketMilk) {
                            return
                        }
                        if (mc.thePlayer.onGround) {
                            offGroundTicks = 0
                        } else {
                            offGroundTicks++
                        }
                        if (offGroundTicks == 4 && send) {
                            send = false
                            PacketUtils.sendPacketNoEvent(
                                C08PacketPlayerBlockPlacement(
                                    BlockPos(-1, -1, -1),
                                    255, heldItem,
                                    0f, 0f, 0f
                                )
                            )
                        } else if (heldItem != null && mc.thePlayer.isUsingItem) {
                            e.y += 1E-14
                        }

                        if (getEmptySlot() != -1) {
                            if (mc.thePlayer.ticksExisted % 3 == 0) {
                                ChatUtil.print(true,"C08")
                                PacketUtils.sendPacket(
                                    C08PacketPlayerBlockPlacement(
                                        BlockPos(-1, -1, -1),
                                        1,
                                        null,
                                        0f,
                                        0f,
                                        0f
                                    )
                                )
                            }
                        }
                    }
                }
            }

            "NCP" -> if (isMoving && mc.thePlayer.isUsingItem) {
                if (e.isPre) {
                    PacketUtils.sendPacket(
                        C07PacketPlayerDigging(
                            C07PacketPlayerDigging.Action.RELEASE_USE_ITEM,
                            BlockPos.ORIGIN,
                            EnumFacing.DOWN
                        )
                    )
                } else {
                    PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.currentEquippedItem))
                }
            }
        }
    }

    override fun onPacketSendEvent(event: PacketSendEvent) {
        val helditem = mc.thePlayer.heldItem
        if (mode.`is`("WatchDog")) {
            if (event.packet is C08PacketPlayerBlockPlacement && !mc.thePlayer.isUsingItem) {
                val blockPlacement = event.packet as C08PacketPlayerBlockPlacement
                if (helditem != null && blockPlacement.placedBlockDirection == 255 && offGroundTicks < 2) {
                    if (mc.thePlayer.onGround) {
                        mc.thePlayer.jump()
                    }
                    send = true
                    event.cancel()
                }
            }
        }
    }
    private fun getEmptySlot(): Int {
        for (i in 1..44) {
            mc.thePlayer.inventoryContainer.getSlot(i).stack ?: return i
        }
        return -1
    }
}
