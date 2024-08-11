package dev.tenacity.utils.player

import dev.tenacity.utils.Utils
import dev.tenacity.utils.Utils.mc
import net.minecraft.block.*
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3

object BlockUtils : Utils {
    fun isValidBlock(pos: BlockPos?): Boolean {
        return isValidBlock(Utils.mc.theWorld.getBlockState(pos).block, false)
    }

    @JvmStatic
    fun getState(blockPos: BlockPos?): IBlockState = mc.theWorld.getBlockState(blockPos)
    @JvmStatic
    fun getBlockAtPos(pos: BlockPos?): Block {
        val blockState = Utils.mc.theWorld.getBlockState(pos)
        return blockState.block
    }
    @JvmStatic
    fun getBlock(vec3: Vec3?): Block? = getBlock(BlockPos(vec3))

    @JvmStatic
    fun getBlock(blockPos: BlockPos?): Block? = mc.theWorld?.getBlockState(blockPos)?.block
    @JvmStatic
    fun canBeClicked(blockPos: BlockPos?) = getBlock(blockPos)?.canCollideCheck(getState(blockPos), false) ?: false &&
            mc.theWorld.worldBorder.contains(blockPos)

    @JvmStatic
    fun isValidBlock(block: Block, placing: Boolean): Boolean {
        if (block is BlockCarpet
            || block is BlockSnow
            || block is BlockContainer
            || block is BlockBasePressurePlate
            || block.material.isLiquid
        ) {
            return false
        }
        if (placing && ((block is BlockSlab
                    || block is BlockStairs
                    || block is BlockLadder
                    || block is BlockStainedGlassPane
                    || block is BlockWall
                    || block is BlockWeb
                    || block is BlockCactus
                    || block is BlockFalling) || block === Blocks.glass_pane || block === Blocks.iron_bars)
        ) {
            return false
        }
        return (block.material.isSolid || !block.isTranslucent || block.isFullBlock)
    }

    val isInLiquid: Boolean
        get() {
            if (Utils.mc.thePlayer == null) return false
            for (x in MathHelper.floor_double(Utils.mc.thePlayer.entityBoundingBox.minX) until MathHelper.floor_double(
                Utils.mc.thePlayer.entityBoundingBox.maxX
            ) + 1) {
                for (z in MathHelper.floor_double(Utils.mc.thePlayer.entityBoundingBox.minZ) until MathHelper.floor_double(
                    Utils.mc.thePlayer.entityBoundingBox.maxZ
                ) + 1) {
                    val pos = BlockPos(x, Utils.mc.thePlayer.entityBoundingBox.minY.toInt(), z)
                    val block = Utils.mc.theWorld.getBlockState(pos).block
                    if (block != null && block !is BlockAir) {
                        return block is BlockLiquid
                    }
                }
            }
            return false
        }

    val isOnLiquid: Boolean
        get() {
            if (Utils.mc.thePlayer == null) return false
            var boundingBox = Utils.mc.thePlayer.entityBoundingBox
            if (boundingBox != null) {
                boundingBox = boundingBox.contract(0.01, 0.0, 0.01).offset(0.0, -0.01, 0.0)
                var onLiquid = false
                val y = boundingBox.minY.toInt()

                for (x in MathHelper.floor_double(boundingBox.minX) until MathHelper.floor_double(boundingBox.maxX + 1.0)) {
                    for (z in MathHelper.floor_double(boundingBox.minZ) until MathHelper.floor_double(boundingBox.maxZ + 1.0)) {
                        val block = Utils.mc.theWorld.getBlockState(BlockPos(x, y, z)).block
                        if (block !== Blocks.air) {
                            if (block !is BlockLiquid) return false
                            onLiquid = true
                        }
                    }
                }

                return onLiquid
            }
            return false
        }
}
