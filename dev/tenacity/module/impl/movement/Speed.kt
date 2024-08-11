package dev.tenacity.module.impl.movement

import dev.tenacity.Tenacity
import dev.tenacity.event.impl.network.PacketReceiveEvent
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.event.impl.player.MoveEvent
import dev.tenacity.event.impl.player.PlayerMoveUpdateEvent
import dev.tenacity.event.impl.player.UpdateEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.impl.combat.TargetStrafe
import dev.tenacity.module.settings.impl.BooleanSetting
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.module.settings.impl.NumberSetting
import dev.tenacity.ui.notifications.NotificationManager
import dev.tenacity.ui.notifications.NotificationType
import dev.tenacity.utils.player.MovementUtils
import dev.tenacity.utils.player.MovementUtils.baseMoveSpeed
import dev.tenacity.utils.player.MovementUtils.isMoving
import dev.tenacity.utils.player.MovementUtils.setSpeed
import dev.tenacity.utils.player.MovementUtils.strafe
import dev.tenacity.utils.server.PacketUtils
import dev.tenacity.utils.time.TimerUtil
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import java.util.concurrent.ThreadLocalRandom
import kotlin.math.hypot
import kotlin.math.max

@Suppress("unused")
class Speed : Module("Speed", Category.MOVEMENT, "Makes you go faster") {
    private val mode = ModeSetting(
        "Mode",
        "WatchDog",
        "WatchDog",
        "Strafe",
        "Matrix",
        "HurtTime",
        "Vanilla",
        "BHop",
        "Verus",
        "Viper",
        "Vulcan",
        "Zonecraft",
        "Heatseeker",
        "Mineland"
    )
    private val watchdogMode = ModeSetting("Watchdog Mode", "Hop", "Hop", "Dev", "Low Hop", "Ground", "New")
    private val verusMode = ModeSetting("Verus Mode", "Normal", "Low", "Normal")
    private val viperMode = ModeSetting("Viper Mode", "Normal", "High", "Normal")
    private val autoDisable = BooleanSetting("Auto Disable", false)
    private val groundSpeed = NumberSetting("Ground Speed", 2.0, 5.0, 1.0, 0.1)
    private val timer = NumberSetting("Timer", 1.0, 5.0, 1.0, 0.1)
    private val vanillaSpeed = NumberSetting("Speed", 1.0, 10.0, 1.0, 0.1)

    private val timerUtil = TimerUtil()
    private val r = ThreadLocalRandom.current().nextFloat()
    private var speed = 0.0
    private var lastDist = 0.0
    private val speedChangingDirection = 0f
    private var stage = 0
    private var strafe = false
    private var wasOnGround = false
    private val setTimer = true
    private var moveSpeed = 0.0
    private var inAirTicks = 0

    init {
        watchdogMode.addParent(mode) { modeSetting: ModeSetting -> modeSetting.`is`("Watchdog") }
        verusMode.addParent(mode) { modeSetting: ModeSetting -> modeSetting.`is`("Verus") }
        viperMode.addParent(mode) { modeSetting: ModeSetting -> modeSetting.`is`("Viper") }
        groundSpeed.addParent(watchdogMode) { modeSetting: ModeSetting -> modeSetting.`is`("Ground") && mode.`is`("WatchDog") }
        vanillaSpeed.addParent(mode) { modeSetting: ModeSetting -> modeSetting.`is`("Vanilla") || modeSetting.`is`("BHop") }
        this.addSettings(mode, vanillaSpeed, watchdogMode, verusMode, viperMode, autoDisable, groundSpeed, timer)
    }

    override fun onUpdateEvent(event: UpdateEvent?) {
        if (mode.`is`("WatchDog")) {
            if (watchdogMode.`is`("New")) {
                if (mc.thePlayer.onGround && MovementUtils.isMoving) {
                    mc.thePlayer.jump()
                    if (!mc.thePlayer.isUsingItem) {
                        strafe(0.4f)
                    } else {
                        strafe()
                    }
                }
            }
        }
    }

    override fun onMotionEvent(e: MotionEvent) {
        this.suffix = mode.mode
        if (setTimer) {
            mc.timer.timerSpeed = timer.value.toFloat()
        }

        val distX = e.x - mc.thePlayer.prevPosX
        val distZ = e.z - mc.thePlayer.prevPosZ
        lastDist = hypot(distX, distZ)

        when (mode.mode) {
            "WatchDog" -> when (watchdogMode.mode) {
                "Hop", "Low Hop", "Dev" -> if (e.isPre) {
                    if (isMoving && mc.thePlayer.fallDistance < 1) {
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump()
                        }
                    }
                }
                "New" -> {}
            }

            "Heatseeker" -> if (e.isPre) {
                if (mc.thePlayer.onGround) {
                    if (timerUtil.hasTimeElapsed(300, true)) {
                        strafe = !strafe
                    }
                    if (strafe) {
                        setSpeed(1.5)
                    }
                }
            }

            "Mineland" -> if (e.isPre) {
                stage++
                if (stage == 1) mc.thePlayer.motionY = 0.2

                if (mc.thePlayer.onGround && stage > 1) setSpeed(0.5)

                if (stage % 14 == 0) stage = 0
            }

            "Vulcan" -> if (e.isPre) {
                if (mc.thePlayer.onGround) {
                    if (isMoving) {
                        mc.thePlayer.jump()
                        setSpeed(baseMoveSpeed * 1.6)
                        inAirTicks = 0
                    }
                } else {
                    inAirTicks++
                    if (inAirTicks == 1) setSpeed(baseMoveSpeed * 1.16)
                }
            }

            "Zonecraft" -> if (e.isPre) {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                    setSpeed(baseMoveSpeed * 1.8)
                    stage = 0
                } else {
                    if (stage == 0 && !mc.thePlayer.isCollidedHorizontally) mc.thePlayer.motionY = -0.4
                    stage++
                }
            }

            "Matrix" -> if (isMoving) {
                if (mc.thePlayer.onGround && mc.thePlayer.motionY < 0.003) {
                    mc.thePlayer.jump()
                    mc.timer.timerSpeed = 1.0f
                }
                if (mc.thePlayer.motionY > 0.003) {
                    mc.thePlayer.motionX *= speed
                    mc.thePlayer.motionZ *= speed
                    mc.timer.timerSpeed = 1.05f
                }
                speed = 1.0012
            }

            "HurtTime" -> if (isMoving) {
                if (mc.thePlayer.hurtTime <= 0) {
                    mc.thePlayer.motionX *= 1.001
                    mc.thePlayer.motionZ *= 1.001
                } else {
                    mc.thePlayer.motionX *= 1.0294
                    mc.thePlayer.motionZ *= 1.0294
                }
                if (mc.thePlayer.onGround && mc.thePlayer.motionY < 0.003) {
                    mc.thePlayer.jump()
                }
            }

            "Vanilla" -> if (isMoving) {
                setSpeed(vanillaSpeed.value / 4)
            }

            "BHop" -> if (isMoving) {
                setSpeed(vanillaSpeed.value / 4)
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                }
            }

            "Verus" -> when (verusMode.mode) {
                "Low" -> if (e.isPre) {
                    if (isMoving) {
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump()
                            wasOnGround = true
                        } else if (wasOnGround) {
                            if (!mc.thePlayer.isCollidedHorizontally) {
                                mc.thePlayer.motionY = -0.0784000015258789
                            }
                            wasOnGround = false
                        }
                        setSpeed(0.33)
                    } else {
                        mc.thePlayer.motionZ = 0.0
                        mc.thePlayer.motionX = mc.thePlayer.motionZ
                    }
                }

                "Normal" -> if (e.isPre) {
                    if (isMoving) {
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.jump()
                            setSpeed(0.48)
                        } else {
                            setSpeed(MovementUtils.speed.toDouble())
                        }
                    } else {
                        setSpeed(0.0)
                    }
                }
            }

            "Viper" -> {
                when (viperMode.mode) {
                    "High" -> if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.7
                    }

                    "Normal" -> if (mc.thePlayer.onGround) {
                        mc.thePlayer.motionY = 0.42
                    }
                }
                setSpeed(baseMoveSpeed * 1.2)
            }

            "Strafe" -> if (e.isPre && isMoving) {
                if (mc.thePlayer.onGround) {
                    mc.thePlayer.jump()
                } else {
                    setSpeed(MovementUtils.speed.toDouble())
                }
            }
        }
    }

    override fun onMoveEvent(e: MoveEvent) {
        if (mode.`is`("Watchdog")) {
            when (watchdogMode.mode) {
                "Ground" -> {
                    strafe = !strafe
                    if (mc.thePlayer.onGround && isMoving && mc.theWorld.getBlockState(
                            BlockPos(
                                mc.thePlayer.posX + e.x,
                                mc.thePlayer.posY,
                                mc.thePlayer.posZ + e.z
                            )
                        ).block === Blocks.air && !mc.thePlayer.isCollidedHorizontally && !Step.isStepping
                    ) {
                        if (strafe || groundSpeed.value >= 1.6) PacketUtils.sendPacket(
                            C04PacketPlayerPosition(
                                mc.thePlayer.posX + e.x,
                                mc.thePlayer.posY,
                                mc.thePlayer.posZ + e.z,
                                true
                            )
                        )
                        e.setSpeed(baseMoveSpeed * groundSpeed.value)
                    }
                }

                "Low Hop" -> if (isMoving) {
                    if (mc.thePlayer.onGround) inAirTicks = 0
                    else inAirTicks++
                    if (inAirTicks == 5) e.y = (-0.19).also { mc.thePlayer.motionY = it }
                }
            }
        }
        TargetStrafe.strafe(e)
    }

    override fun onPlayerMoveUpdateEvent(e: PlayerMoveUpdateEvent) {
        if (mode.`is`("Watchdog") && (watchdogMode.`is`("Hop") || watchdogMode.`is`("Dev") || watchdogMode.`is`("Low Hop")) && mc.thePlayer.fallDistance < 1 && !mc.thePlayer.isPotionActive(
                Potion.jump
            )
        ) {
            if (isMoving) {
                when (watchdogMode.mode) {
                    "Low Hop", "Hop" -> {
                        if (mc.thePlayer.onGround) speed = 1.5
                        speed -= 0.025
                        e.applyMotion(baseMoveSpeed * speed, 0.55f)
                    }

                    "Dev" -> {
                        if (mc.thePlayer.onGround) {
                            moveSpeed = baseMoveSpeed * 2.1475 * 0.76
                            wasOnGround = true
                        } else if (wasOnGround) {
                            moveSpeed = lastDist - 0.81999 * (lastDist - baseMoveSpeed)
                            moveSpeed *= 1 / 0.91
                            wasOnGround = false
                        } else {
                            moveSpeed -= if (TargetStrafe.canStrafe()) lastDist / 100.0 else lastDist / 150.0
                        }
                        speed = if (mc.thePlayer.isInWater || mc.thePlayer.isInLava) {
                            baseMoveSpeed * 0.25
                        } else {
                            max(moveSpeed, baseMoveSpeed)
                        }
                        e.applyMotion(speed, 0.6f)
                    }
                }
            } else {
                e.applyMotion(0.0, 0f)
            }
        }
    }

    override fun onPacketReceiveEvent(e: PacketReceiveEvent) {
        if (e.packet is S08PacketPlayerPosLook && autoDisable.isEnabled) {
            NotificationManager.post(
                NotificationType.WARNING, "Flag Detector",
                "Speed disabled due to " +
                        (if (mc.thePlayer == null || mc.thePlayer.ticksExisted < 5
                        ) "world change"
                        else "lagback"), 1.5f
            )
            this.toggleSilent()
        }
    }

    fun shouldPreventJumping(): Boolean {
        return Tenacity.INSTANCE.isEnabled(Speed::class.java) && isMoving && !(mode.`is`("Watchdog") && watchdogMode.`is`(
            "Ground"
        ))
    }

    override fun onEnable() {
        speed = 1.5
        timerUtil.reset()
        if (mc.thePlayer != null) {
            wasOnGround = mc.thePlayer.onGround
        }
        inAirTicks = 0
        moveSpeed = 0.0
        stage = 0
        super.onEnable()
    }

    override fun onDisable() {
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }
}
