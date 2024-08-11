package dev.tenacity.module.impl.movement

import dev.tenacity.event.impl.game.TickEvent
import dev.tenacity.event.impl.network.PacketSendEvent
import dev.tenacity.event.impl.player.BlockPlaceableEvent
import dev.tenacity.event.impl.player.MotionEvent
import dev.tenacity.event.impl.player.SafeWalkEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.settings.ParentAttribute
import dev.tenacity.module.settings.impl.BooleanSetting
import dev.tenacity.module.settings.impl.ModeSetting
import dev.tenacity.module.settings.impl.NumberSetting
import dev.tenacity.utils.animations.Animation
import dev.tenacity.utils.animations.Direction
import dev.tenacity.utils.animations.impl.DecelerateAnimation
import dev.tenacity.utils.misc.MathUtils
import dev.tenacity.utils.player.MovementUtils
import dev.tenacity.utils.player.MovementUtils.baseMoveSpeed
import dev.tenacity.utils.player.MovementUtils.getMoveYaw
import dev.tenacity.utils.player.MovementUtils.isMoving
import dev.tenacity.utils.player.MovementUtils.setSpeed
import dev.tenacity.utils.player.RotationUtils
import dev.tenacity.utils.player.ScaffoldUtils
import dev.tenacity.utils.player.ScaffoldUtils.BlockCache
import dev.tenacity.utils.render.ColorUtil
import dev.tenacity.utils.render.RenderUtil
import dev.tenacity.utils.render.RoundedUtil
import dev.tenacity.utils.server.PacketUtils
import dev.tenacity.utils.time.TimerUtil
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.settings.GameSettings
import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C09PacketHeldItemChange
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.potion.Potion
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.MouseFilter
import org.lwjgl.opengl.GL11
import java.awt.Color
import kotlin.math.abs
import kotlin.math.floor

class Scaffold : Module("Scaffold", Category.MOVEMENT, "Automatically places blocks under you") {
    private val countMode = ModeSetting("Block Counter", "Tenacity", "None", "Tenacity", "Basic", "Polar")
    private val rotations = BooleanSetting("Rotations", true)
    private val rotationMode =
        ModeSetting("Rotation Mode", "Watchdog", "Watchdog", "NCP", "Back", "45", "Enum", "Down", "0")
    private val placeType = ModeSetting("Place Type", "Post", "Pre", "Post", "Legit", "Dynamic")

    //public static NumberSetting extend = new NumberSetting("Extend", 0, 6, 0, 0.05);
    private val timer = NumberSetting("Timer", 1.0, 5.0, 0.1, 0.1)
    private val sneak = BooleanSetting("Sneak", false)
    private val towerTimer = NumberSetting("Tower Timer Boost", 1.2, 5.0, 0.1, 0.1)
    private val swing = BooleanSetting("Swing", true)
    private val autoJump = BooleanSetting("Auto Jump", false)
    private val hideJump = BooleanSetting("Hide Jump", false)
    private val baseSpeed = BooleanSetting("Base Speed", false)
    private var blockCache: BlockCache? = null
    private var lastBlockCache: BlockCache? = null
    private var y = 0f
    private var speed = 0f
    private val pitchMouseFilter = MouseFilter()
    private val delayTimer = TimerUtil()
    private val timerUtil = TimerUtil()
    private val shouldSendPacket = false
    private val shouldTower = false
    private var firstJump = false
    private var pre = false
    private var jumpTimer = 0
    private var slot = 0
    private var prevSlot = 0
    private var cachedRots = FloatArray(2)
    // WATCHDOG
    private var wdTick = 0
    private var wdSpoof = false

    private val anim: Animation = DecelerateAnimation(250, 1.0)

    init {
        this.addSettings(
            countMode,
            rotations,
            rotationMode,
            placeType,
            keepYMode,
            sprintMode,
            towerMode,
            swingMode,
            delay,
            timer,
            auto3rdPerson,
            speedSlowdown,
            speedSlowdownAmount,
            itemSpoof,
            downwards,
            safewalk,
            sprint,
            sneak,
            tower,
            towerTimer,
            swing,
            autoJump,
            hideJump,
            baseSpeed,
            keepY
        )
        rotationMode.addParent(rotations, ParentAttribute.BOOLEAN_CONDITION)
        sprintMode.addParent(sprint, ParentAttribute.BOOLEAN_CONDITION)
        towerMode.addParent(tower, ParentAttribute.BOOLEAN_CONDITION)
        swingMode.addParent(swing, ParentAttribute.BOOLEAN_CONDITION)
        towerTimer.addParent(tower, ParentAttribute.BOOLEAN_CONDITION)
        keepYMode.addParent(keepY, ParentAttribute.BOOLEAN_CONDITION)
        hideJump.addParent(autoJump, ParentAttribute.BOOLEAN_CONDITION)
        speedSlowdownAmount.addParent(speedSlowdown, ParentAttribute.BOOLEAN_CONDITION)
    }

    override fun onMotionEvent(e: MotionEvent) {
        isDownwards = downwards.isEnabled && GameSettings.isKeyDown(mc.gameSettings.keyBindSneak)
        // Timer Stuff
        if (!mc.gameSettings.keyBindJump.isKeyDown) {
            mc.timer.timerSpeed = timer.value.toFloat()
        } else {
            mc.timer.timerSpeed = if (tower.isEnabled) towerTimer.value.toFloat() else 1F
        }

        if (e.isPre) {
            if (towerMode.`is`("WatchDog") && tower.isEnabled && mc.gameSettings.keyBindJump.isKeyDown) {
                    if (mc.thePlayer.onGround) {
                        wdTick = 0
                        mc.thePlayer.motionY = 0.42
                        mc.thePlayer.motionZ *= 1.05
                        mc.thePlayer.motionX *= 1.05
                    } else if (mc.thePlayer.motionY > -0.0784000015258789) {
                        val n = Math.round(mc.thePlayer.posY % 1.0 * 100.0).toInt()
                        when (n) {
                            42 -> {
                                mc.thePlayer.motionY = 0.33
                            }

                            75 -> {
                                mc.thePlayer.motionY = 0.25
                                wdSpoof = true
                            }
                            0 -> {
                                mc.thePlayer.motionY = 0.42
                                wdSpoof = true
                            }

                        }
                    }
                } else {
                wdTick = 0
            }
            // Auto Jump
            if (baseSpeed.isEnabled) {
                setSpeed(baseMoveSpeed * 0.7)
            }
            if (autoJump.isEnabled && mc.thePlayer.onGround && isMoving && !mc.gameSettings.keyBindJump.isKeyDown) {
                mc.thePlayer.jump()
            }

            if (sprint.isEnabled && sprintMode.`is`("Watchdog") && mc.thePlayer.onGround && isMoving && !mc.gameSettings.keyBindJump.isKeyDown && !isDownwards && mc.thePlayer.isSprinting) {
                val offset = MathUtils.yawPos(mc.thePlayer.direction, (MovementUtils.speed / 2).toDouble())
                mc.thePlayer.sendQueue.addToSendQueue(
                    C04PacketPlayerPosition(
                        mc.thePlayer.posX - offset[0],
                        mc.thePlayer.posY,
                        mc.thePlayer.posZ - offset[1],
                        true
                    )
                )
            }

            // Rotations
            if (rotations.isEnabled) {
                var rotations = floatArrayOf(0f, 0f)
                when (rotationMode.mode) {
                    "Watchdog" -> {
                        rotations = floatArrayOf(getMoveYaw(e.yaw) - 180, y)
                        e.setRotations(rotations[0], rotations[1])
                    }

                    "NCP" -> {
                        val prevYaw = cachedRots[0]
                        if ((ScaffoldUtils.blockInfo.also { blockCache = it }) == null) {
                            blockCache = lastBlockCache
                        }
                        if (blockCache != null && (mc.thePlayer.ticksExisted % 3 == 0
                                    || mc.theWorld.getBlockState(
                                BlockPos(
                                    e.x,
                                    ScaffoldUtils.yLevel,
                                    e.z
                                )
                            ).block === Blocks.air)
                        ) {
                            cachedRots = RotationUtils.getRotations(blockCache!!.position, blockCache!!.facing)
                        }
                        if ((mc.thePlayer.onGround || (isMoving && tower.isEnabled && mc.gameSettings.keyBindJump.isKeyDown)) && abs(
                                (cachedRots[0] - prevYaw).toDouble()
                            ) >= 90
                        ) {
                            cachedRots[0] = getMoveYaw(e.yaw) - 180
                        }
                        rotations = cachedRots
                        e.setRotations(rotations[0], rotations[1])
                    }

                    "Back" -> {
                        rotations = floatArrayOf(getMoveYaw(e.yaw) - 180, 77f)
                        e.setRotations(rotations[0], rotations[1])
                    }

                    "Down" -> e.pitch = 90f
                    "45" -> {
                        var `val`: Float
                        if (isMoving) {
                            val f = getMoveYaw(e.yaw) - 180
                            val numbers = floatArrayOf(-135f, -90f, -45f, 0f, 45f, 90f, 135f, 180f)
                            var lastDiff = 999f
                            `val` = f
                            for (v in numbers) {
                                val diff = abs((v - f).toDouble()).toFloat()
                                if (diff < lastDiff) {
                                    lastDiff = diff
                                    `val` = v
                                }
                            }
                        } else {
                            `val` = rotations[0]
                        }
                        rotations = floatArrayOf(
                            (`val` + MathHelper.wrapAngleTo180_float(mc.thePlayer.prevRotationYawHead)) / 2.0f,
                            (77 + MathHelper.wrapAngleTo180_float(mc.thePlayer.prevRotationPitchHead)) / 2.0f
                        )
                        e.setRotations(rotations[0], rotations[1])
                    }

                    "Enum" -> if (lastBlockCache != null) {
                        val yaw = RotationUtils.getEnumRotations(lastBlockCache!!.facing)
                        e.setRotations(yaw, 77f)
                    } else {
                        e.setRotations(mc.thePlayer.rotationYaw + 180, 77f)
                    }

                    "0" -> e.setRotations(0f, 0f)
                }
                RotationUtils.setVisualRotations(e)
            }

            // Speed 2 Slowdown
            if (speedSlowdown.isEnabled && mc.thePlayer.isPotionActive(Potion.moveSpeed) && !mc.gameSettings.keyBindJump.isKeyDown && mc.thePlayer.onGround) {
                setSpeed(speedSlowdownAmount.value)
            }

            if (sneak.isEnabled) KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)

            // Save ground Y level for keep Y
            if (mc.thePlayer.onGround) {
                keepYCoord = floor(mc.thePlayer.posY - 1.0)
            }

            if (tower.isEnabled && mc.gameSettings.keyBindJump.isKeyDown) {
                val centerX = floor(e.x) + 0.5
                val centerZ = floor(e.z) + 0.5
                when (towerMode.mode) {
                    "Vanilla" -> mc.thePlayer.motionY = 0.42
                    "Verus" -> if (mc.thePlayer.ticksExisted % 2 == 0) mc.thePlayer.motionY = 0.42
                    "Watchdog" -> {}
                    "NCP" -> if (!isMoving || MovementUtils.speed < 0.16) {
                        if (mc.thePlayer.onGround) {
                            mc.thePlayer.motionY = 0.42
                        } else if (mc.thePlayer.motionY < 0.23) {
                            mc.thePlayer.setPosition(
                                mc.thePlayer.posX, mc.thePlayer.posY.toInt()
                                    .toDouble(), mc.thePlayer.posZ
                            )
                            mc.thePlayer.motionY = 0.42
                        }
                    }
                }
            }

            // Setting Block Cache
            blockCache = ScaffoldUtils.blockInfo
            if (blockCache != null) {
                lastBlockCache = ScaffoldUtils.blockInfo
            } else {
                return
            }

            if (mc.thePlayer.ticksExisted % 4 == 0) {
                pre = true
            }

            // Placing Blocks (Pre)
            if (placeType.`is`("Pre") || (placeType.`is`("Dynamic") && pre)) {
                if (place()) {
                    pre = false
                }
            }
        } else {
            // Setting Item Slot
            if (!itemSpoof.isEnabled) {
                mc.thePlayer.inventory.currentItem = slot
            }

            // Placing Blocks (Post)
            if (placeType.`is`("Post") || (placeType.`is`("Dynamic") && !pre)) {
                place()
            }

            pre = false
        }
    }

    private fun place(): Boolean {
        val slot = ScaffoldUtils.blockSlot
        if (blockCache == null || lastBlockCache == null || slot == -1) return false

        if (this.slot != slot) {
            this.slot = slot
            PacketUtils.sendPacketNoEvent(C09PacketHeldItemChange(this.slot))
        }

        var placed = false
        if (delayTimer.hasTimeElapsed(delay.value * 1000)) {
            firstJump = false
            if (mc.playerController.onPlayerRightClick(
                    mc.thePlayer, mc.theWorld,
                    mc.thePlayer.inventory.getStackInSlot(this.slot),
                    lastBlockCache!!.position, lastBlockCache!!.facing,
                    ScaffoldUtils.getHypixelVec3(lastBlockCache!!)
                )
            ) {
                placed = true
                y = MathUtils.getRandomInRange(79.5f, 83.5f)
                if (swing.isEnabled) {
                    if (swingMode.`is`("Client")) {
                        mc.thePlayer.swingItem()
                    } else {
                        PacketUtils.sendPacket(C0APacketAnimation())
                    }
                }
            }
            delayTimer.reset()
            blockCache = null
        }
        return placed
    }

    override fun onBlockPlaceable(event: BlockPlaceableEvent) {
        if (placeType.`is`("Legit")) {
            place()
        }
    }

    override fun onTickEvent(event: TickEvent) {
        if (mc.thePlayer == null) return
        if (hideJump.isEnabled && !mc.gameSettings.keyBindJump.isKeyDown && isMoving && !mc.thePlayer.onGround && autoJump.isEnabled) {
            mc.thePlayer.posY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY
            mc.thePlayer.lastTickPosY -= mc.thePlayer.posY - mc.thePlayer.lastTickPosY
            mc.thePlayer.cameraPitch = 0.1f
            mc.thePlayer.cameraYaw = mc.thePlayer.cameraPitch
        }
        if (downwards.isEnabled) {
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, false)
            mc.thePlayer.movementInput.sneak = false
        }
    }

    override fun onDisable() {
        if (mc.thePlayer != null) {
            if (!itemSpoof.isEnabled) mc.thePlayer.inventory.currentItem = prevSlot
            if (slot != mc.thePlayer.inventory.currentItem && itemSpoof.isEnabled) PacketUtils.sendPacketNoEvent(
                C09PacketHeldItemChange(
                    mc.thePlayer.inventory.currentItem
                )
            )

            if (auto3rdPerson.isEnabled) {
                mc.gameSettings.thirdPersonView = 0
            }
            if (mc.thePlayer.isSneaking && sneak.isEnabled) KeyBinding.setKeyBindState(
                mc.gameSettings.keyBindSneak.keyCode, GameSettings.isKeyDown(
                    mc.gameSettings.keyBindSneak
                )
            )
        }
        mc.timer.timerSpeed = 1f
        super.onDisable()
    }

    override fun onEnable() {
        lastBlockCache = null
        if (mc.thePlayer != null) {
            prevSlot = mc.thePlayer.inventory.currentItem
            slot = mc.thePlayer.inventory.currentItem
            if (mc.thePlayer.isSprinting && sprint.isEnabled && sprintMode.`is`("Cancel")) {
                PacketUtils.sendPacketNoEvent(
                    C0BPacketEntityAction(
                        mc.thePlayer,
                        C0BPacketEntityAction.Action.STOP_SPRINTING
                    )
                )
            }
            if (auto3rdPerson.isEnabled) {
                mc.gameSettings.thirdPersonView = 1
            }
        }
        firstJump = true
        speed = 1.1f
        timerUtil.reset()
        jumpTimer = 0
        y = 80f
        super.onEnable()
    }

    fun renderCounterBlur() {
        if (!enabled && anim.isDone) return
        val slot = ScaffoldUtils.blockSlot
        val heldItem = if (slot == -1) null else mc.thePlayer.inventory.mainInventory[slot]
        val count = if (slot == -1) 0 else ScaffoldUtils.blockCount
        val countStr = count.toString()
        val fr = mc.fontRendererObj
        val sr = ScaledResolution(mc)
        var color: Int
        val x: Float
        val y: Float
        val str = countStr + " block" + (if (count != 1) "s" else "")
        val output = anim.output.toFloat()
        when (countMode.mode) {
            "Tenacity" -> {
                val blockWH = (if (heldItem != null) 15 else -2).toFloat()
                val spacing = 3
                val text = "§l" + countStr + "§r block" + (if (count != 1) "s" else "")
                val textWidth = tenacityFont18.getStringWidth(text)

                val totalWidth = ((textWidth + blockWH + spacing) + 6) * output
                x = sr.scaledWidth / 2f - (totalWidth / 2f)
                y = sr.scaledHeight - (sr.scaledHeight / 2f - 20)
                val height = 20f
                RenderUtil.scissorStart(x - 1.5, y - 1.5, (totalWidth + 3).toDouble(), (height + 3).toDouble())

                RoundedUtil.drawRound(x, y, totalWidth, height, 5f, Color.BLACK)
                RenderUtil.scissorEnd()
            }

            "Basic" -> {
                x = sr.scaledWidth / 2f - fr.getStringWidth(str) / 2f + 1
                y = sr.scaledHeight / 2f + 10
                RenderUtil.scaleStart(sr.scaledWidth / 2.0f, y + fr.FONT_HEIGHT / 2.0f, output)
                fr.drawStringWithShadow(str, x, y, 0x000000)
                RenderUtil.scaleEnd()
            }

            "Polar" -> {
                x = sr.scaledWidth / 2f - fr.getStringWidth(countStr) / 2f + (if (heldItem != null) 6 else 1)
                y = sr.scaledHeight / 2f + 10

                GlStateManager.pushMatrix()
                RenderUtil.fixBlendIssues()
                GL11.glTranslatef(x + (if (heldItem == null) 1 else 0), y, 1f)
                GL11.glScaled(
                    anim.output.toFloat().toDouble(), anim.output.toFloat().toDouble(), 1.0
                )
                GL11.glTranslatef(-x - (if (heldItem == null) 1 else 0), -y, 1f)

                fr.drawOutlinedString(countStr, x, y, ColorUtil.applyOpacity(0x000000, output), true)

                if (heldItem != null) {
                    val scale = 0.7
                    GlStateManager.color(1f, 1f, 1f, 1f)
                    GlStateManager.scale(scale, scale, scale)
                    RenderHelper.enableGUIStandardItemLighting()
                    mc.renderItem.renderItemAndEffectIntoGUI(
                        heldItem,
                        ((sr.scaledWidth / 2f - fr.getStringWidth(countStr) / 2f - 7) / scale).toInt(),
                        ((sr.scaledHeight / 2f + 8.5f) / scale).toInt()
                    )
                    RenderHelper.disableStandardItemLighting()
                }
                GlStateManager.popMatrix()
            }
        }
    }

    fun renderCounter() {
        anim.setDirection(if (enabled) Direction.FORWARDS else Direction.BACKWARDS)
        if (!enabled && anim.isDone) return
        val slot = ScaffoldUtils.blockSlot
        val heldItem = if (slot == -1) null else mc.thePlayer.inventory.mainInventory[slot]
        val count = if (slot == -1) 0 else ScaffoldUtils.blockCount
        val countStr = count.toString()
        val fr = mc.fontRendererObj
        val sr = ScaledResolution(mc)
        val color: Int
        val x: Float
        val y: Float
        val str = countStr + " block" + (if (count != 1) "s" else "")
        val output = anim.output.toFloat()
        when (countMode.mode) {
            "Tenacity" -> {
                val blockWH = (if (heldItem != null) 15 else -2).toFloat()
                val spacing = 3
                val text = "§l" + countStr + "§r block" + (if (count != 1) "s" else "")
                val textWidth = tenacityFont18.getStringWidth(text)

                val totalWidth = ((textWidth + blockWH + spacing) + 6) * output
                x = sr.scaledWidth / 2f - (totalWidth / 2f)
                y = sr.scaledHeight - (sr.scaledHeight / 2f - 20)
                val height = 20f
                RenderUtil.scissorStart(x - 1.5, y - 1.5, (totalWidth + 3).toDouble(), (height + 3).toDouble())

                RoundedUtil.drawRound(x, y, totalWidth, height, 5f, ColorUtil.tripleColor(20, .45f))

                tenacityFont18.drawString(
                    text,
                    x + 3 + blockWH + spacing,
                    y + tenacityFont18.getMiddleOfBox(height) + .5f,
                    -1
                )

                if (heldItem != null) {
                    RenderHelper.enableGUIStandardItemLighting()
                    mc.renderItem.renderItemAndEffectIntoGUI(heldItem, x.toInt() + 3, (y + 10 - (blockWH / 2)).toInt())
                    RenderHelper.disableStandardItemLighting()
                }
                RenderUtil.scissorEnd()
            }

            "Basic" -> {
                x = sr.scaledWidth / 2f - fr.getStringWidth(str) / 2f + 1
                y = sr.scaledHeight / 2f + 10
                RenderUtil.scaleStart(sr.scaledWidth / 2.0f, y + fr.FONT_HEIGHT / 2.0f, output)
                fr.drawStringWithShadow(str, x, y, -1)
                RenderUtil.scaleEnd()
            }

            "Polar" -> {
                color = if (count < 24) -0xaaab else if (count < 128) -0xab else -0xaa00ab
                x = sr.scaledWidth / 2f - fr.getStringWidth(countStr) / 2f + (if (heldItem != null) 6 else 1)
                y = sr.scaledHeight / 2f + 10

                GlStateManager.pushMatrix()
                RenderUtil.fixBlendIssues()
                GL11.glTranslatef(x + (if (heldItem == null) 1 else 0), y, 1f)
                GL11.glScaled(
                    anim.output.toFloat().toDouble(), anim.output.toFloat().toDouble(), 1.0
                )
                GL11.glTranslatef(-x - (if (heldItem == null) 1 else 0), -y, 1f)

                fr.drawOutlinedString(countStr, x, y, ColorUtil.applyOpacity(color, output), true)

                if (heldItem != null) {
                    val scale = 0.7
                    GlStateManager.color(1f, 1f, 1f, 1f)
                    GlStateManager.scale(scale, scale, scale)
                    RenderHelper.enableGUIStandardItemLighting()
                    mc.renderItem.renderItemAndEffectIntoGUI(
                        heldItem,
                        ((sr.scaledWidth / 2f - fr.getStringWidth(countStr) / 2f - 7) / scale).toInt(),
                        ((sr.scaledHeight / 2f + 8.5f) / scale).toInt()
                    )
                    RenderHelper.disableStandardItemLighting()
                }
                GlStateManager.popMatrix()
            }
        }
    }

    override fun onPacketSendEvent(e: PacketSendEvent) {
        val packet = e.packet
        if (tower.isEnabled && towerMode.`is`("WatchDog")) {
                if (packet is C03PacketPlayer) {
                    if (wdSpoof) {
                        packet.onGround = true
                        wdSpoof = false
                    }
                }
        }
        if (e.packet is C0BPacketEntityAction && (e.packet as C0BPacketEntityAction).action == C0BPacketEntityAction.Action.START_SPRINTING && sprint.isEnabled && sprintMode.`is`(
                "Cancel"
            )
        ) {
            e.cancel()
        }
        if (e.packet is C09PacketHeldItemChange && itemSpoof.isEnabled) {
            e.cancel()
        }
    }

    override fun onSafeWalkEvent(event: SafeWalkEvent) {
        if ((safewalk.isEnabled && !isDownwards) || ScaffoldUtils.blockCount == 0) {
            event.isSafe = true
        }
    }

    companion object {
        @JvmField
        var keepYMode: ModeSetting = ModeSetting("Keep Y Mode", "Always", "Always", "Speed toggled","WatchDog")
        var sprintMode: ModeSetting = ModeSetting("Sprint Mode", "Vanilla", "Vanilla", "Watchdog", "Cancel")
        var towerMode: ModeSetting = ModeSetting("Tower Mode", "Watchdog", "Vanilla", "NCP", "Watchdog", "Verus")
        var swingMode: ModeSetting = ModeSetting("Swing Mode", "Client", "Client", "Silent")
        var delay: NumberSetting = NumberSetting("Delay", 0.0, 2.0, 0.0, 0.05)
        val auto3rdPerson: BooleanSetting = BooleanSetting("Auto 3rd Person", false)
        val speedSlowdown: BooleanSetting = BooleanSetting("Speed Slowdown", true)
        val speedSlowdownAmount: NumberSetting = NumberSetting("Slowdown Amount", 0.1, 0.2, 0.01, 0.01)
        val itemSpoof: BooleanSetting = BooleanSetting("Item Spoof", false)
        val downwards: BooleanSetting = BooleanSetting("Downwards", false)
        val safewalk: BooleanSetting = BooleanSetting("Safewalk", false)
        val sprint: BooleanSetting = BooleanSetting("Sprint", false)
        val tower: BooleanSetting = BooleanSetting("Tower", false)
        @JvmField
        var keepY: BooleanSetting = BooleanSetting("Keep Y", false)
        @JvmField
        var keepYCoord: Double = 0.0
        @JvmField
        var isDownwards = false
    }
}
