package dev.tenacity.utils.player

import dev.tenacity.Tenacity
import dev.tenacity.module.impl.movement.Scaffold
import dev.tenacity.module.impl.movement.Speed
import dev.tenacity.utils.Utils
import dev.tenacity.utils.Utils.mc
import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.init.Blocks
import net.minecraft.item.ItemBlock
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraft.util.Vec3
import kotlin.math.max
import kotlin.math.min

object ScaffoldUtils : Utils {
    val yLevel: Double
        get() {
            if (!Scaffold.keepY.isEnabled || Scaffold.keepYMode.`is`("Speed toggled") && !Tenacity.INSTANCE.isEnabled(
                    Speed::class.java
                ) || Scaffold.keepYMode.`is`("WatchDog") && mc.thePlayer.motionY >= 0.2
            ) {
                return mc.thePlayer.posY - 1.0
            }
            return if (mc.thePlayer.posY - 1.0 >= Scaffold.keepYCoord && max(
                    mc.thePlayer.posY,
                    Scaffold.keepYCoord
                ) - min(
                    mc.thePlayer.posY, Scaffold.keepYCoord
                ) <= 3.0 && !mc.gameSettings.keyBindJump.isKeyDown
            ) Scaffold.keepYCoord
            else mc.thePlayer.posY - 1.0
        }

    val blockInfo: BlockCache?
        get() {
            val belowBlockPos =
                BlockPos(
                    mc.thePlayer.posX,
                    if (!Scaffold.keepYMode.`is`("WatchDog")) {
                        yLevel - (if (Scaffold.isDownwards) 1 else 0)
                    } else {
                        yLevel + if (mc.thePlayer.motionY == 0.4) 1 else 0
                    }, mc.thePlayer.posZ)
            if (mc.theWorld.getBlockState(belowBlockPos).block is BlockAir) {
                for (x in 0..3) {
                    for (z in 0..3) {
                        var i = 1
                        while (i > -3) {
                            val blockPos = belowBlockPos.add(x * i, 0, z * i)
                            if (mc.theWorld.getBlockState(blockPos).block is BlockAir) {
                                for (direction in EnumFacing.entries) {
                                    val block = blockPos.offset(direction)
                                    val material = mc.theWorld.getBlockState(block).block.material
                                    if (material.isSolid && !material.isLiquid) {
                                        return BlockCache(block, direction.opposite)
                                    }
                                }
                            }
                            i -= 2
                        }
                    }
                }
            }
            return null
        }

    val blockSlot: Int
        get() {
            for (i in 0..8) {
                val itemStack = mc.thePlayer.inventory.mainInventory[i]
                if (itemStack != null && itemStack.item is ItemBlock && itemStack.stackSize > 0) {
                    val itemBlock = itemStack.item as ItemBlock
                    if (isBlockValid(itemBlock.block)) {
                        return i
                    }
                }
            }
            return -1
        }

    val blockCount: Int
        get() {
            var count = 0
            for (i in 0..8) {
                val itemStack = mc.thePlayer.inventory.mainInventory[i]
                if (itemStack != null && itemStack.item is ItemBlock && itemStack.stackSize > 0) {
                    val itemBlock = itemStack.item as ItemBlock
                    if (isBlockValid(itemBlock.block)) {
                        count += itemStack.stackSize
                    }
                }
            }
            return count
        }

    private fun isBlockValid(block: Block): Boolean {
        return (block.isFullBlock || block === Blocks.glass) && (
                block !== Blocks.sand) && (
                block !== Blocks.gravel) && (
                block !== Blocks.dispenser) && (
                block !== Blocks.command_block) && (
                block !== Blocks.noteblock) && (
                block !== Blocks.furnace) && (
                block !== Blocks.crafting_table) && (
                block !== Blocks.tnt) && (
                block !== Blocks.dropper) && (
                block !== Blocks.beacon)
    }

    fun getHypixelVec3(data: BlockCache): Vec3 {
        val pos = data.position
        val face = data.facing
        var x = pos.x.toDouble() + 0.5
        var y = pos.y.toDouble() + 0.5
        var z = pos.z.toDouble() + 0.5
        if (face != EnumFacing.UP && face != EnumFacing.DOWN) {
            y += 0.5
        } else {
            x += 0.3
            z += 0.3
        }
        if (face == EnumFacing.WEST || face == EnumFacing.EAST) {
            z += 0.15
        }
        if (face == EnumFacing.SOUTH || face == EnumFacing.NORTH) {
            x += 0.15
        }
        return Vec3(x, y, z)
    }

    class BlockCache(val position: BlockPos, val facing: EnumFacing)
}
