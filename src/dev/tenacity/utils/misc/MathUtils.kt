package dev.tenacity.utils.misc

import net.minecraft.client.Minecraft
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import java.math.BigDecimal
import java.math.RoundingMode
import java.security.SecureRandom
import java.text.DecimalFormat
import java.util.Random
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

operator fun Vec3.plus(vec: Vec3): Vec3 = add(vec)
fun Float.toRadians() = this * 0.017453292f
fun Float.toRadiansD() = toRadians().toDouble()
fun Float.toDegrees() = this * 57.29578f
fun Float.toDegreesD() = toDegrees().toDouble()

fun Double.toRadians() = this * 0.017453292
fun Double.toRadiansF() = toRadians().toFloat()
fun Double.toDegrees() = this * 57.295779513
fun Double.toDegreesF() = toDegrees().toFloat()

object MathUtils {
    @JvmField
    val DF_0: DecimalFormat = DecimalFormat("0")
    val DF_1: DecimalFormat = DecimalFormat("0.0")
    val DF_2: DecimalFormat = DecimalFormat("0.00")
    val DF_1D: DecimalFormat = DecimalFormat("0.#")
    val DF_2D: DecimalFormat = DecimalFormat("0.##")

    val secureRandom: SecureRandom = SecureRandom()

    @JvmStatic
    fun getRandomInRange(min: Int, max: Int): Int {
        return ((Math.random() * (max - min)) + min).toInt()
    }

    fun yawPos(value: Double): DoubleArray {
        return yawPos(Minecraft.getMinecraft().thePlayer.rotationYaw * MathHelper.deg2Rad, value)
    }

    fun yawPos(yaw: Float, value: Double): DoubleArray {
        return doubleArrayOf(-MathHelper.sin(yaw) * value, MathHelper.cos(yaw) * value)
    }

    fun getRandomInRange(min: Float, max: Float): Float {
        val random = SecureRandom()
        return random.nextFloat() * (max - min) + min
    }

    @JvmStatic
    fun getRandomInRange(min: Double, max: Double): Double {
        val random = SecureRandom()
        return if (min == max) min else random.nextDouble() * (max - min) + min
    }

    fun getRandomNumberUsingNextInt(min: Int, max: Int): Int {
        val random = Random()
        return random.nextInt(max - min) + min
    }

    fun lerp(old: Double, newVal: Double, amount: Double): Double {
        return (1.0 - amount) * old + amount * newVal
    }

    @JvmStatic
    fun interpolate(oldValue: Double, newValue: Double, interpolationValue: Double): Double {
        return (oldValue + (newValue - oldValue) * interpolationValue)
    }

    @JvmStatic
    fun interpolateFloat(oldValue: Float, newValue: Float, interpolationValue: Double): Float {
        return interpolate(
            oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat()
                .toDouble()
        ).toFloat()
    }

    @JvmStatic
    fun interpolateInt(oldValue: Int, newValue: Int, interpolationValue: Double): Int {
        return interpolate(
            oldValue.toDouble(), newValue.toDouble(), interpolationValue.toFloat()
                .toDouble()
        ).toInt()
    }

    @JvmStatic
    fun calculateGaussianValue(x: Float, sigma: Float): Float {
        val output = 1.0 / sqrt(2.0 * Math.PI * (sigma * sigma))
        return (output * exp(-(x * x) / (2.0 * (sigma * sigma)))).toFloat()
    }

    @JvmStatic
    fun roundToHalf(d: Double): Double {
        return Math.round(d * 2) / 2.0
    }

    @JvmStatic
    fun round(num: Double, increment: Double): Double {
        var bd = BigDecimal(num)
        bd = (bd.setScale(increment.toInt(), RoundingMode.HALF_UP))
        return bd.toDouble()
    }

    @JvmStatic
    fun round(value: Double, places: Int): Double {
        require(places >= 0)
        var bd = BigDecimal(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toDouble()
    }

    fun round(value: String?, places: Int): String {
        require(places >= 0)
        var bd = BigDecimal(value)
        bd = bd.stripTrailingZeros()
        bd = bd.setScale(places, RoundingMode.HALF_UP)
        return bd.toString()
    }

    @JvmStatic
    fun getRandomFloat(max: Float, min: Float): Float {
        val random = SecureRandom()
        return random.nextFloat() * (max - min) + min
    }


    fun getNumberOfDecimalPlace(value: Double): Int {
        val bigDecimal = BigDecimal(value)
        return max(0.0, bigDecimal.stripTrailingZeros().scale().toDouble()).toInt()
    }
}
