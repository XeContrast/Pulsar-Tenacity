package dev.tenacity.utils.player

import com.google.common.base.Predicate
import com.google.common.base.Predicates
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.utils.Utils
import dev.tenacity.utils.misc.MathUtils.getRandomFloat
import dev.tenacity.utils.player.ScaffoldUtils.BlockCache
import dev.tenacity.utils.Rotation
import net.minecraft.client.Minecraft
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.item.EntityItemFrame
import net.minecraft.entity.projectile.EntitySnowball
import net.minecraft.util.*
import store.intent.intentguard.annotation.Exclude
import store.intent.intentguard.annotation.Strategy
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

object RotationUtils : Utils {
    @JvmField
    var serverRotation: Rotation? = Rotation(0f, 0f)
    /*
     * Sets the player's head rotations to the given yaw and pitch (visual-only).
     */
    @JvmStatic
    @Exclude(Strategy.NAME_REMAPPING)
    fun setVisualRotations(yaw: Float, pitch: Float) {
        Utils.mc.thePlayer.renderYawOffset = yaw
        Utils.mc.thePlayer.rotationYawHead = Utils.mc.thePlayer.renderYawOffset
        Utils.mc.thePlayer.rotationPitchHead = pitch
    }

    fun setVisualRotations(rotations: FloatArray) {
        setVisualRotations(rotations[0], rotations[1])
    }

    fun setVisualRotations(e: MotionEvent) {
        setVisualRotations(e.yaw, e.pitch)
    }

    fun clampRotation(): Float {
        var rotationYaw = Minecraft.getMinecraft().thePlayer.rotationYaw
        var n = 1.0f
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward < 0.0f) {
            rotationYaw += 180.0f
            n = -0.5f
        } else if (Minecraft.getMinecraft().thePlayer.movementInput.moveForward > 0.0f) {
            n = 0.5f
        }
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe > 0.0f) {
            rotationYaw -= 90.0f * n
        }
        if (Minecraft.getMinecraft().thePlayer.movementInput.moveStrafe < 0.0f) {
            rotationYaw += 90.0f * n
        }
        return rotationYaw * 0.017453292f
    }

    val sensitivityMultiplier: Float
        get() {
            val SENSITIVITY = Minecraft.getMinecraft().gameSettings.mouseSensitivity * 0.6f + 0.2f
            return (SENSITIVITY * SENSITIVITY * SENSITIVITY * 8.0f) * 0.15f
        }

    fun smoothRotation(from: Float, to: Float, speed: Float): Float {
        var f = MathHelper.wrapAngleTo180_float(to - from)

        if (f > speed) {
            f = speed
        }

        if (f < -speed) {
            f = -speed
        }

        return from + f
    }

    fun getFacingRotations(blockCache: BlockCache): FloatArray {
        val d1: Double =
            blockCache.position.x + 0.5 - Utils.mc.thePlayer.posX + blockCache.facing.frontOffsetX / 2.0
        val d2: Double =
            blockCache.position.z + 0.5 - Utils.mc.thePlayer.posZ + blockCache.facing.frontOffsetZ / 2.0
        val d3: Double = Utils.mc.thePlayer.posY + Utils.mc.thePlayer.eyeHeight - (blockCache.position.y)
        val d4 = MathHelper.sqrt_double(d1 * d1 + d2 * d2).toDouble()
        var f1 = (atan2(d2, d1) * 180.0 / Math.PI).toFloat() - 90.0f
        val f2 = (atan2(d3, d4) * 180.0 / Math.PI).toFloat()
        if (f1 < 0.0f) {
            f1 += 360.0f
        }
        return floatArrayOf(f1, f2)
    }

    fun getRotations(blockPos: BlockPos, enumFacing: EnumFacing): FloatArray {
        val d = blockPos.x.toDouble() + 0.5 - Utils.mc.thePlayer.posX + enumFacing.frontOffsetX.toDouble() * 0.25
        val d2 = blockPos.z.toDouble() + 0.5 - Utils.mc.thePlayer.posZ + enumFacing.frontOffsetZ.toDouble() * 0.25
        val d3 =
            Utils.mc.thePlayer.posY + Utils.mc.thePlayer.eyeHeight.toDouble() - blockPos.y - enumFacing.frontOffsetY.toDouble() * 0.25
        val d4 = MathHelper.sqrt_double(d * d + d2 * d2).toDouble()
        val f = (atan2(d2, d) * 180.0 / Math.PI).toFloat() - 90.0f
        val f2 = (atan2(d3, d4) * 180.0 / Math.PI).toFloat()
        return floatArrayOf(MathHelper.wrapAngleTo180_float(f), f2)
    }

    @JvmStatic
    fun getRotationsNeeded(entity: Entity?): FloatArray? {
        if (entity == null) {
            return null
        }
        val mc = Minecraft.getMinecraft()
        val xSize = entity.posX - mc.thePlayer.posX
        val ySize = entity.posY + entity.eyeHeight / 2 - (mc.thePlayer.posY + mc.thePlayer.eyeHeight)
        val zSize = entity.posZ - mc.thePlayer.posZ
        val theta = MathHelper.sqrt_double(xSize * xSize + zSize * zSize).toDouble()
        val yaw = (atan2(zSize, xSize) * 180 / Math.PI).toFloat() - 90
        val pitch = (-(atan2(ySize, theta) * 180 / Math.PI)).toFloat()
        return floatArrayOf(
            (mc.thePlayer.rotationYaw + MathHelper.wrapAngleTo180_float(yaw - mc.thePlayer.rotationYaw)) % 360,
            (mc.thePlayer.rotationPitch + MathHelper.wrapAngleTo180_float(pitch - mc.thePlayer.rotationPitch)) % 360.0f
        )
    }

    @JvmStatic
    fun getFacingRotations2(paramInt1: Int, d: Double, paramInt3: Int): FloatArray? {
        val localEntityPig = EntitySnowball(Minecraft.getMinecraft().theWorld)
        localEntityPig.posX = paramInt1 + 0.5
        localEntityPig.posY = d + 0.5
        localEntityPig.posZ = paramInt3 + 0.5
        return getRotationsNeeded(localEntityPig)
    }

    fun getEnumRotations(facing: EnumFacing): Float {
        var yaw = 0f
        if (facing == EnumFacing.NORTH) {
            yaw = 0f
        }
        if (facing == EnumFacing.EAST) {
            yaw = 90f
        }
        if (facing == EnumFacing.WEST) {
            yaw = -90f
        }
        if (facing == EnumFacing.SOUTH) {
            yaw = 180f
        }
        return yaw
    }

    @JvmStatic
    fun getYaw(to: Vec3): Float {
        val x = (to.xCoord - Utils.mc.thePlayer.posX).toFloat()
        val z = (to.zCoord - Utils.mc.thePlayer.posZ).toFloat()
        val var1 = (StrictMath.atan2(z.toDouble(), x.toDouble()) * 180.0 / StrictMath.PI).toFloat() - 90.0f
        val rotationYaw = Utils.mc.thePlayer.rotationYaw
        return rotationYaw + MathHelper.wrapAngleTo180_float(var1 - rotationYaw)
    }

    @JvmStatic
    fun getVecRotations(yaw: Float, pitch: Float): Vec3 {
        val d = cos(Math.toRadians(-yaw.toDouble()) - Math.PI)
        val d1 = sin(Math.toRadians(-yaw.toDouble()) - Math.PI)
        val d2 = -cos(Math.toRadians(-pitch.toDouble()))
        val d3 = sin(Math.toRadians(-pitch.toDouble()))
        return Vec3(d1 * d2, d3, d * d2)
    }

    @JvmStatic
    fun getRotations(posX: Double, posY: Double, posZ: Double): FloatArray {
        val x = posX - Utils.mc.thePlayer.posX
        val z = posZ - Utils.mc.thePlayer.posZ
        val y = posY - (Utils.mc.thePlayer.eyeHeight + Utils.mc.thePlayer.posY)
        val d3 = MathHelper.sqrt_double(x * x + z * z).toDouble()
        val yaw = (MathHelper.atan2(z, x) * 180.0 / Math.PI).toFloat() - 90.0f
        val pitch = (-(MathHelper.atan2(y, d3) * 180.0 / Math.PI)).toFloat()
        return floatArrayOf(yaw, pitch)
    }

    @JvmStatic
    fun getSmoothRotations(entity: EntityLivingBase): FloatArray {
        val f1 = Utils.mc.gameSettings.mouseSensitivity * 0.6f + 0.2f
        val fac = f1 * f1 * f1 * 256.0f

        val x = entity.posX - Utils.mc.thePlayer.posX
        val z = entity.posZ - Utils.mc.thePlayer.posZ
        val y = (entity.posY + entity.eyeHeight
                - (Utils.mc.thePlayer.entityBoundingBox.minY
                + (Utils.mc.thePlayer.entityBoundingBox.maxY
                - Utils.mc.thePlayer.entityBoundingBox.minY)))

        val d3 = MathHelper.sqrt_double(x * x + z * z).toDouble()
        var yaw = (MathHelper.atan2(z, x) * 180.0 / Math.PI).toFloat() - 90.0f
        var pitch = (-(MathHelper.atan2(y, d3) * 180.0 / Math.PI)).toFloat()
        yaw = smoothRotation(Utils.mc.thePlayer.prevRotationYawHead, yaw, fac * getRandomFloat(0.9f, 1f))
        pitch = smoothRotation(Utils.mc.thePlayer.prevRotationPitchHead, pitch, fac * getRandomFloat(0.7f, 1f))

        return floatArrayOf(yaw, pitch)
    }

    @JvmStatic
    fun isMouseOver(yaw: Float, pitch: Float, target: Entity, range: Float): Boolean {
        val partialTicks = Utils.mc.timer.renderPartialTicks
        val entity = Utils.mc.renderViewEntity
        var objectMouseOver: MovingObjectPosition?
        var mcPointedEntity: Entity? = null

        if (entity != null && Utils.mc.theWorld != null) {
            Utils.mc.mcProfiler.startSection("pick")
            val d0 = Utils.mc.playerController.blockReachDistance.toDouble()
            objectMouseOver = entity.rayTrace(d0, partialTicks)
            var d1 = d0
            val vec3 = entity.getPositionEyes(partialTicks)
            val flag = d0 > range.toDouble()

            if (objectMouseOver != null) {
                d1 = objectMouseOver.hitVec.distanceTo(vec3)
            }

            val vec31 = Utils.mc.thePlayer.getVectorForRotation(pitch, yaw)
            val vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0)
            var pointedEntity: Entity? = null
            var vec33: Vec3? = null
            val f = 1.0f
            val list = Utils.mc.theWorld.getEntitiesInAABBexcluding(
                entity,
                entity.entityBoundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0)
                    .expand(f.toDouble(), f.toDouble(), f.toDouble()),
                Predicates.and(EntitySelectors.NOT_SPECTATING, Predicate { obj: Entity? -> obj!!.canBeCollidedWith() })
            )
            var d2 = d1

            for (entity1 in list) {
                val f1 = entity1.collisionBorderSize
                val axisalignedbb = entity1.entityBoundingBox.expand(f1.toDouble(), f1.toDouble(), f1.toDouble())
                val movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32)

                if (axisalignedbb.isVecInside(vec3)) {
                    if (d2 >= 0.0) {
                        pointedEntity = entity1
                        vec33 = if (movingobjectposition == null) vec3 else movingobjectposition.hitVec
                        d2 = 0.0
                    }
                } else if (movingobjectposition != null) {
                    val d3 = vec3.distanceTo(movingobjectposition.hitVec)

                    if (d3 < d2 || d2 == 0.0) {
                        pointedEntity = entity1
                        vec33 = movingobjectposition.hitVec
                        d2 = d3
                    }
                }
            }

            if (pointedEntity != null && flag && vec3.distanceTo(vec33) > range.toDouble()) {
                pointedEntity = null
                objectMouseOver =
                    MovingObjectPosition(MovingObjectPosition.MovingObjectType.MISS, vec33, null, BlockPos(vec33))
            }

            if (pointedEntity != null && (d2 < d1 || objectMouseOver == null)) {
                if (pointedEntity is EntityLivingBase || pointedEntity is EntityItemFrame) {
                    mcPointedEntity = pointedEntity
                }
            }

            Utils.mc.mcProfiler.endSection()

            return mcPointedEntity === target
        }

        return false
    }
    fun limitAngleChange(currentRotation: Rotation, targetRotation: Rotation, turnSpeed: Float): Rotation {
        val yawDifference = getAngleDifference(targetRotation.yaw, currentRotation.yaw)
        val pitchDifference = getAngleDifference(targetRotation.pitch, currentRotation.pitch)

        return Rotation(
            currentRotation.yaw + if (yawDifference > turnSpeed) turnSpeed else yawDifference.coerceAtLeast(-turnSpeed),
            currentRotation.pitch + if (pitchDifference > turnSpeed) turnSpeed else pitchDifference.coerceAtLeast(-turnSpeed)
        )
    }
    fun limitAngleChange(
        currentRotation: Rotation,
        targetRotation: Rotation?,
        horizontalSpeed: Float,
        verticalSpeed: Float
    ): Rotation {
        val yawDifference = targetRotation?.let { getAngleDifference(it.yaw, currentRotation.yaw) }
        val pitchDifference = targetRotation?.let { getAngleDifference(it.pitch, currentRotation.pitch) }
        return Rotation(
            currentRotation.yaw + (if (yawDifference!! > horizontalSpeed) horizontalSpeed else max(
                yawDifference,
                -horizontalSpeed
            )),
            currentRotation.pitch + (if (pitchDifference!! > verticalSpeed) verticalSpeed else max(
                pitchDifference,
                -verticalSpeed
            ))
        )
    }
    @JvmStatic
    fun getAngleDifference(a: Float, b: Float): Float {
        return ((((a - b) % 360f) + 540f) % 360f) - 180f
    }
}
