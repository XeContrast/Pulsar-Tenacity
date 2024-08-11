package dev.tenacity.utils.player

import dev.tenacity.event.impl.player.MoveEvent
import dev.tenacity.event.impl.player.PlayerMoveUpdateEvent
import dev.tenacity.utils.Utils
import dev.tenacity.utils.Utils.mc
import dev.tenacity.utils.server.PacketUtils
import net.minecraft.entity.player.PlayerCapabilities
import net.minecraft.network.play.client.C13PacketPlayerAbilities
import net.minecraft.potion.Potion
import net.minecraft.util.MathHelper
import org.lwjgl.util.vector.Vector2f
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object MovementUtils : Utils {
    @JvmStatic
    val isMoving: Boolean
        get() {
            if (mc.thePlayer == null) {
                return false
            }
            return (mc.thePlayer.movementInput.moveForward != 0f || mc.thePlayer.movementInput.moveStrafe != 0f)
        }

    @JvmStatic
    fun getMoveYaw(yaw: Float): Float {
        var yaw = yaw
        val from = Vector2f(mc.thePlayer.lastTickPosX.toFloat(), mc.thePlayer.lastTickPosZ.toFloat())
        val to = Vector2f(mc.thePlayer.posX.toFloat(), mc.thePlayer.posZ.toFloat())
        val diff = Vector2f(to.x - from.x, to.y - from.y)

        val x = diff.x.toDouble()
        val z = diff.y.toDouble()
        if (x != 0.0 && z != 0.0) {
            yaw = Math.toDegrees((atan2(-x, z) + MathHelper.PI2) % MathHelper.PI2).toFloat()
        }
        return yaw
    }

    fun setSpeed(moveSpeed: Double, yaw: Float, strafe: Double, forward: Double) {
        var yaw = yaw
        var strafe = strafe
        var forward = forward
        if (forward != 0.0) {
            if (strafe > 0.0) {
                yaw += (if ((forward > 0.0)) -45 else 45).toFloat()
            } else if (strafe < 0.0) {
                yaw += (if ((forward > 0.0)) 45 else -45).toFloat()
            }
            strafe = 0.0
            if (forward > 0.0) {
                forward = 1.0
            } else if (forward < 0.0) {
                forward = -1.0
            }
        }
        if (strafe > 0.0) {
            strafe = 1.0
        } else if (strafe < 0.0) {
            strafe = -1.0
        }
        val mx = cos(Math.toRadians((yaw + 90.0f).toDouble()))
        val mz = sin(Math.toRadians((yaw + 90.0f).toDouble()))
        mc.thePlayer.motionX = forward * moveSpeed * mx + strafe * moveSpeed * mz
        mc.thePlayer.motionZ = forward * moveSpeed * mz - strafe * moveSpeed * mx
    }

    fun setSpeedHypixel(event: PlayerMoveUpdateEvent, moveSpeed: Float, strafeMotion: Float) {
        val remainder = 1f - strafeMotion
        if (mc.thePlayer.onGround) {
            setSpeed(moveSpeed.toDouble())
        } else {
            mc.thePlayer.motionX *= strafeMotion.toDouble()
            mc.thePlayer.motionZ *= strafeMotion.toDouble()
            event.friction = moveSpeed * remainder
        }
    }

    @JvmStatic
    fun setSpeed(moveSpeed: Double) {
        setSpeed(
            moveSpeed,
            mc.thePlayer.rotationYaw,
            mc.thePlayer.movementInput.moveStrafe.toDouble(),
            mc.thePlayer.movementInput.moveForward.toDouble()
        )
    }

    fun setSpeed(moveEvent: MoveEvent, moveSpeed: Double, yaw: Float, strafe: Double, forward: Double) {
        var yaw = yaw
        var strafe = strafe
        var forward = forward
        if (forward != 0.0) {
            if (strafe > 0.0) {
                yaw += (if ((forward > 0.0)) -45 else 45).toFloat()
            } else if (strafe < 0.0) {
                yaw += (if ((forward > 0.0)) 45 else -45).toFloat()
            }
            strafe = 0.0
            if (forward > 0.0) {
                forward = 1.0
            } else if (forward < 0.0) {
                forward = -1.0
            }
        }
        if (strafe > 0.0) {
            strafe = 1.0
        } else if (strafe < 0.0) {
            strafe = -1.0
        }
        val mx = cos(Math.toRadians((yaw + 90.0f).toDouble()))
        val mz = sin(Math.toRadians((yaw + 90.0f).toDouble()))
        moveEvent.x = forward * moveSpeed * mx + strafe * moveSpeed * mz
        moveEvent.z = forward * moveSpeed * mz - strafe * moveSpeed * mx
    }

    @JvmStatic
    fun setSpeed(moveEvent: MoveEvent, moveSpeed: Double) {
        setSpeed(
            moveEvent,
            moveSpeed,
            mc.thePlayer.rotationYaw,
            mc.thePlayer.movementInput.moveStrafe.toDouble(),
            mc.thePlayer.movementInput.moveForward.toDouble()
        )
    }

    @JvmStatic
    val baseMoveSpeed: Double
        get() {
            var baseSpeed = mc.thePlayer.capabilities.walkSpeed * 2.873
            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                baseSpeed /= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown).amplifier + 1)
            }
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier + 1)
            }
            return baseSpeed
        }

    fun sendFlyingCapabilities(isFlying: Boolean, allowFlying: Boolean) {
        val playerCapabilities = PlayerCapabilities()
        playerCapabilities.isFlying = isFlying
        playerCapabilities.allowFlying = allowFlying
        PacketUtils.sendPacketNoEvent(C13PacketPlayerAbilities(playerCapabilities))
    }

    val baseMoveSpeed2: Double
        get() {
            var baseSpeed =
                mc.thePlayer.capabilities.walkSpeed * (if (mc.thePlayer.isSprinting) 2.873 else 2.215)
            if (mc.thePlayer.isPotionActive(Potion.moveSlowdown)) {
                baseSpeed /= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSlowdown).amplifier + 1)
            }
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                baseSpeed *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier + 1)
            }
            return baseSpeed
        }
    val baseMoveSpeedStupid: Double
        get() {
            var sped = 0.2873
            if (mc.thePlayer.isPotionActive(Potion.moveSpeed)) {
                sped *= 1.0 + 0.2 * (mc.thePlayer.getActivePotionEffect(Potion.moveSpeed).amplifier + 1)
            }
            return sped
        }

    @JvmStatic
    fun isOnGround(height: Double): Boolean {
        return mc.theWorld.getCollidingBoundingBoxes(
            mc.thePlayer,
            mc.thePlayer.entityBoundingBox.offset(0.0, -height, 0.0)
        ).isNotEmpty()
    }

    @JvmStatic
    val speed: Float
        get() {
            if (mc.thePlayer == null || mc.theWorld == null) return 0F
            return sqrt(mc.thePlayer.motionX * mc.thePlayer.motionX + mc.thePlayer.motionZ * mc.thePlayer.motionZ).toFloat()
        }

    @JvmStatic
    val maxFallDist: Float
        get() = (mc.thePlayer.maxFallHeight + (if (mc.thePlayer.isPotionActive(
                Potion.jump
            )
        ) mc.thePlayer.getActivePotionEffect(Potion.jump)
            .amplifier + 1 else 0)).toFloat()

    @JvmStatic
    fun strafe(speed: Float) {
        if (!isMoving) return
        mc.thePlayer.motionX = -sin(direction) * speed
        mc.thePlayer.motionZ = cos(direction) * speed
    }

    @JvmStatic
    val direction: Double
        get() {
            var rotationYaw = mc.thePlayer.rotationYaw
            if (mc.thePlayer.movementInput.moveForward < 0f) rotationYaw += 180f
            var forward = 1f
            if (mc.thePlayer.movementInput.moveForward < 0f) forward = -0.5f else if (mc.thePlayer.movementInput.moveForward > 0f) forward = 0.5f
            if (mc.thePlayer.movementInput.moveStrafe > 0f) rotationYaw -= 90f * forward
            if (mc.thePlayer.movementInput.moveStrafe < 0f) rotationYaw += 90f * forward
            return Math.toRadians(rotationYaw.toDouble())
        }
    @JvmStatic
    fun predictedMotion(motion: Double, ticks: Int): Double {
        return predictedMotion2(motion, ticks)
    }
    private fun predictedMotion2(motion: Double, ticks: Int): Double {
        if (ticks == 0) return motion
        var predicted = motion

        for (i in 0 until ticks) {
            predicted = (predicted - 0.08) * 0.98f
        }

        return predicted
    }
    @JvmStatic
    fun resetTimer() {
        try {
            mc.timer.timerSpeed = 1.0f
        } catch (var1: NullPointerException) {
        }
    }
    @JvmStatic
    fun strafe() {
        strafe(speed)
    }
}
