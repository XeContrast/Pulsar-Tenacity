package dev.tenacity.module.impl.render

import dev.tenacity.Tenacity
import dev.tenacity.event.impl.game.WorldEvent
import dev.tenacity.event.impl.render.Render2DEvent
import dev.tenacity.event.impl.render.RenderChestEvent
import dev.tenacity.event.impl.render.RenderModelEvent
import dev.tenacity.module.Category
import dev.tenacity.module.Module
import dev.tenacity.module.settings.ParentAttribute
import dev.tenacity.module.settings.impl.*
import dev.tenacity.utils.animations.Animation
import dev.tenacity.utils.animations.impl.DecelerateAnimation
import dev.tenacity.utils.misc.MathUtils.calculateGaussianValue
import dev.tenacity.utils.render.*
import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.shader.Framebuffer
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.monster.EntityMob
import net.minecraft.entity.monster.EntitySlime
import net.minecraft.entity.passive.EntityAnimal
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntityChest
import org.lwjgl.BufferUtils
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL13
import org.lwjgl.opengl.GL14
import org.lwjgl.opengl.GL20
import java.awt.Color
import kotlin.math.pow
import kotlin.math.sin

class GlowESP : Module("GlowESP", Category.RENDER, "ESP that glows on players") {
    private val kawaseGlow = BooleanSetting("Kawase Glow", false)
    private val colorMode = ModeSetting("Color Mode", "Sync", "Sync", "Random", "Custom")
    private val validEntities = MultipleBoolSetting(
        "Entities",
        BooleanSetting("Players", true),
        BooleanSetting("Animals", true),
        BooleanSetting("Mobs", true),
        BooleanSetting("Chests", true)
    )
    private val playerColor = ColorSetting("Player Color", Tenacity.INSTANCE.clientColor)
    private val animalColor = ColorSetting("Animal Color", Tenacity.INSTANCE.alternateClientColor)
    private val mobColor = ColorSetting("Mob Color", Color.RED)
    private val chestColor = ColorSetting("Chest Color", Color.GREEN)
    private val hurtTimeColor = ColorSetting("Hurt Time Color", Color.RED)
    private val radius = NumberSetting("Radius", 4.0, 20.0, 2.0, 2.0)
    private val iterationsSetting = NumberSetting("Iterations", 4.0, 10.0, 2.0, 1.0)
    private val offsetSetting = NumberSetting("Offset", 4.0, 10.0, 2.0, 1.0)
    private val exposure = NumberSetting("Exposure", 2.2, 3.5, .5, .1)
    private val seperate = BooleanSetting("Seperate Texture", false)

    private val chamsShader = ShaderUtil("chams")
    private val outlineShader = ShaderUtil("Tenacity/Shaders/outline.frag")
    private val glowShader = ShaderUtil("glow")
    private val kawaseGlowShader = ShaderUtil("kawaseDownBloom")
    private val kawaseGlowShader2 = ShaderUtil("kawaseUpGlow")

    var framebuffer: Framebuffer? = null
    var outlineFrameBuffer: Framebuffer? = null

    var glowFrameBuffer: Framebuffer? = null
    private val entities: MutableList<Entity> = ArrayList()
    private val entityColorMap: MutableMap<Any, Color?> = HashMap()

    init {
        playerColor.addParent(colorMode) { modeSetting: ModeSetting ->
            modeSetting.`is`("Custom") && validEntities.getSetting(
                "Players"
            ).isEnabled
        }
        animalColor.addParent(colorMode) { modeSetting: ModeSetting ->
            modeSetting.`is`("Custom") && validEntities.getSetting(
                "Animals"
            ).isEnabled
        }
        mobColor.addParent(colorMode) { modeSetting: ModeSetting ->
            modeSetting.`is`("Custom") && validEntities.getSetting(
                "Mobs"
            ).isEnabled
        }
        chestColor.addParent(colorMode) { modeSetting: ModeSetting ->
            modeSetting.`is`("Custom") && validEntities.getSetting(
                "Chests"
            ).isEnabled
        }

        radius.addParent(kawaseGlow, ParentAttribute.BOOLEAN_CONDITION.negate())
        iterationsSetting.addParent(kawaseGlow, ParentAttribute.BOOLEAN_CONDITION)
        offsetSetting.addParent(kawaseGlow, ParentAttribute.BOOLEAN_CONDITION)
        addSettings(
            kawaseGlow,
            colorMode,
            validEntities,
            playerColor,
            animalColor,
            mobColor,
            chestColor,
            hurtTimeColor,
            iterationsSetting,
            offsetSetting,
            radius,
            exposure,
            seperate
        )
    }

    override fun onWorldEvent(event: WorldEvent) {
        entityColorMap.clear()
    }

    override fun onEnable() {
        super.onEnable()
        entityColorMap.clear()
        fadeIn = DecelerateAnimation(250, 1.0)
    }

    fun createFrameBuffers() {
        framebuffer = RenderUtil.createFrameBuffer(framebuffer)
        outlineFrameBuffer = RenderUtil.createFrameBuffer(outlineFrameBuffer)
    }

    override fun onRenderChestEvent(e: RenderChestEvent) {
        if (validEntities.getSetting("Chests").isEnabled && framebuffer != null) {
            framebuffer!!.bindFramebuffer(false)
            chamsShader.init()
            chamsShader.setUniformi("textureIn", 0)
            val color = getColor(e.entity)

            RenderUtil.resetColor()
            chamsShader.setUniformf("color", color!!.red / 255f, color.green / 255f, color.blue / 255f, 1f)
            e.drawChest()
            chamsShader.unload()

            mc.framebuffer.bindFramebuffer(false)
        }
    }

    override fun onRenderModelEvent(e: RenderModelEvent) {
        if (e.isPost && framebuffer != null) {
            if (!entities.contains(e.entity)) return
            framebuffer!!.bindFramebuffer(false)
            chamsShader.init()
            chamsShader.setUniformi("textureIn", 0)
            val color = getColor(e.entity)

            // TODO: Fix gradient
            chamsShader.setUniformf("color", color!!.red / 255f, color.green / 255f, color.blue / 255f, 1f)
            RenderUtil.resetColor()
            GlStateManager.enableCull()
            renderGlint = false
            e.drawModel()

            //Needed to add the other layers to the entity
            e.drawLayers()
            renderGlint = true
            GlStateManager.disableCull()

            chamsShader.unload()


            mc.framebuffer.bindFramebuffer(false)
        }
    }

    override fun onRender2DEvent(e: Render2DEvent) {
        createFrameBuffers()
        collectEntities()

        val sr = ScaledResolution(mc)
        if (framebuffer != null && outlineFrameBuffer != null && (validEntities.getSetting("Chests").isEnabled || entities.size > 0)) {
            RenderUtil.setAlphaLimit(0f)
            GLUtil.startBlend()


            /*        RenderUtil.bindTexture(framebuffer.framebufferTexture);
            ShaderUtil.drawQuads();
            framebuffer.framebufferClear();
            mc.getFramebuffer().bindFramebuffer(false);


            if(true) return;*/
            outlineFrameBuffer!!.framebufferClear()
            outlineFrameBuffer!!.bindFramebuffer(false)
            outlineShader.init()
            setupOutlineUniforms(0f, 1f)
            RenderUtil.bindTexture(framebuffer!!.framebufferTexture)
            ShaderUtil.drawQuads()
            outlineShader.init()
            setupOutlineUniforms(1f, 0f)
            RenderUtil.bindTexture(framebuffer!!.framebufferTexture)
            ShaderUtil.drawQuads()
            outlineShader.unload()
            outlineFrameBuffer!!.unbindFramebuffer()


            if (kawaseGlow.isEnabled) {
                val offset = offsetSetting.value.toInt()
                val iterations = 3

                if (framebufferList.isEmpty() || currentIterations != iterations || (framebuffer!!.framebufferWidth != mc.displayWidth || framebuffer!!.framebufferHeight != mc.displayHeight)) {
                    initFramebuffers(iterations.toFloat())
                    currentIterations = iterations
                }
                RenderUtil.setAlphaLimit(0f)

                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE)

                GL11.glClearColor(0f, 0f, 0f, 0f)
                renderFBO(
                    framebufferList[1],
                    outlineFrameBuffer!!.framebufferTexture,
                    kawaseGlowShader,
                    offset.toFloat()
                )

                //Downsample
                for (i in 1 until iterations) {
                    renderFBO(
                        framebufferList[i + 1],
                        framebufferList[i].framebufferTexture,
                        kawaseGlowShader,
                        offset.toFloat()
                    )
                }

                //Upsample
                for (i in iterations downTo 2) {
                    renderFBO(
                        framebufferList[i - 1],
                        framebufferList[i].framebufferTexture,
                        kawaseGlowShader2,
                        offset.toFloat()
                    )
                }

                val lastBuffer = framebufferList[0]
                lastBuffer.framebufferClear()
                lastBuffer.bindFramebuffer(false)
                kawaseGlowShader2.init()
                kawaseGlowShader2.setUniformf("offset", offset.toFloat(), offset.toFloat())
                kawaseGlowShader2.setUniformi("inTexture", 0)
                kawaseGlowShader2.setUniformi("check", if (seperate.isEnabled) 1 else 0)
                kawaseGlowShader2.setUniformf("lastPass", 1f)
                kawaseGlowShader2.setUniformf("exposure", (exposure.value.toFloat() * fadeIn!!.output.toFloat()))
                kawaseGlowShader2.setUniformi("textureToCheck", 16)
                kawaseGlowShader2.setUniformf(
                    "halfpixel",
                    1.0f / lastBuffer.framebufferWidth,
                    1.0f / lastBuffer.framebufferHeight
                )
                kawaseGlowShader2.setUniformf(
                    "iResolution",
                    lastBuffer.framebufferWidth.toFloat(),
                    lastBuffer.framebufferHeight.toFloat()
                )
                GL13.glActiveTexture(GL13.GL_TEXTURE16)
                RenderUtil.bindTexture(framebuffer!!.framebufferTexture)
                GL13.glActiveTexture(GL13.GL_TEXTURE0)
                RenderUtil.bindTexture(framebufferList[1].framebufferTexture)

                ShaderUtil.drawQuads()
                kawaseGlowShader2.unload()

                GL11.glClearColor(0f, 0f, 0f, 0f)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                framebuffer!!.framebufferClear()
                RenderUtil.resetColor()
                mc.framebuffer.bindFramebuffer(true)
                RenderUtil.bindTexture(framebufferList[0].framebufferTexture)
                ShaderUtil.drawQuads()
                RenderUtil.setAlphaLimit(0f)
                GlStateManager.bindTexture(0)
            } else {
                if (framebufferList.isNotEmpty()) {
                    for (framebuffer in framebufferList) {
                        framebuffer.deleteFramebuffer()
                    }
                    glowFrameBuffer = null
                    framebufferList.clear()
                }

                glowFrameBuffer = RenderUtil.createFrameBuffer(glowFrameBuffer)

                GL11.glClearColor(0f, 0f, 0f, 0f)
                glowFrameBuffer!!.framebufferClear()
                glowFrameBuffer!!.bindFramebuffer(false)
                glowShader.init()
                setupGlowUniforms(1f, 0f)
                RenderUtil.bindTexture(outlineFrameBuffer!!.framebufferTexture)
                GL11.glBlendFunc(GL11.GL_ONE, GL11.GL_ONE)
                ShaderUtil.drawQuads()
                glowShader.unload()

                mc.framebuffer.bindFramebuffer(false)

                GL11.glClearColor(0f, 0f, 0f, 0f)
                glowShader.init()
                setupGlowUniforms(0f, 1f)
                if (seperate.isEnabled) {
                    GL13.glActiveTexture(GL13.GL_TEXTURE16)
                    RenderUtil.bindTexture(framebuffer!!.framebufferTexture)
                }
                GL13.glActiveTexture(GL13.GL_TEXTURE0)
                RenderUtil.bindTexture(glowFrameBuffer!!.framebufferTexture)
                GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)
                ShaderUtil.drawQuads()
                glowShader.unload()

                framebuffer!!.framebufferClear()
                mc.framebuffer.bindFramebuffer(false)
            }
        }
    }

    private fun initFramebuffers(iterations: Float) {
        for (framebuffer in framebufferList) {
            framebuffer.deleteFramebuffer()
        }
        framebufferList.clear()

        //Have to make the framebuffer null so that it does not try to delete a framebuffer that has already been deleted
        framebufferList.add(RenderUtil.createFrameBuffer(null).also { glowFrameBuffer = it })


        var i = 1
        while (i <= iterations) {
            val currentBuffer = Framebuffer(
                (mc.displayWidth / 2.0.pow(i.toDouble())).toInt(),
                (mc.displayHeight / 2.0.pow(i.toDouble())).toInt(),
                true
            )
            currentBuffer.setFramebufferFilter(GL11.GL_LINEAR)

            GlStateManager.bindTexture(currentBuffer.framebufferTexture)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL14.GL_MIRRORED_REPEAT)
            GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL14.GL_MIRRORED_REPEAT)
            GlStateManager.bindTexture(0)

            framebufferList.add(currentBuffer)
            i++
        }
    }

    fun setupGlowUniforms(dir1: Float, dir2: Float) {
        glowShader.setUniformi("texture", 0)
        if (seperate.isEnabled) {
            glowShader.setUniformi("textureToCheck", 16)
        }
        glowShader.setUniformf("radius", radius.value.toFloat())
        glowShader.setUniformf("texelSize", 1.0f / mc.displayWidth, 1.0f / mc.displayHeight)
        glowShader.setUniformf("direction", dir1, dir2)
        glowShader.setUniformf("exposure", (exposure.value.toFloat() * fadeIn!!.output.toFloat()))
        glowShader.setUniformi("avoidTexture", if (seperate.isEnabled) 1 else 0)

        val buffer = BufferUtils.createFloatBuffer(256)
        for (i in 1..radius.value.toFloat().toInt()) {
            buffer.put(calculateGaussianValue(i.toFloat(), radius.value.toFloat() / 2))
        }
        buffer.rewind()

        GL20.glUniform1(glowShader.getUniform("weights"), buffer)
    }


    fun setupOutlineUniforms(dir1: Float, dir2: Float) {
        outlineShader.setUniformi("textureIn", 0)
        val iterations =
            if (kawaseGlow.isEnabled) (iterationsSetting.value.toFloat() * 2f) else radius.value.toFloat() / 1.5f
        outlineShader.setUniformf("radius", iterations)
        outlineShader.setUniformf("texelSize", 1.0f / mc.displayWidth, 1.0f / mc.displayHeight)
        outlineShader.setUniformf("direction", dir1, dir2)
    }


    private fun getColor(entity: Any): Color? {
        var color = Color.WHITE
        when (colorMode.mode) {
            "Custom" -> {
                if (entity is EntityPlayer) {
                    color = playerColor.color
                }
                if (entity is EntityMob || entity is EntitySlime) {
                    color = mobColor.color
                }
                if (entity is EntityAnimal) {
                    color = animalColor.color
                }
                if (entity is TileEntityChest) {
                    color = chestColor.color
                }
            }

            "Sync" -> {
                val colors = HUDMod.getClientColors()
                color = if (HUDMod.isRainbowTheme()) {
                    colors.first
                } else {
                    ColorUtil.interpolateColorsBackAndForth(15, 0, colors.first, colors.second, false)
                }
            }

            "Random" -> if (entityColorMap.containsKey(entity)) {
                color = entityColorMap[entity]
            } else {
                color = ColorUtil.getRandomColor()
                entityColorMap[entity] = color
            }
        }
        if (entity is EntityLivingBase) {
            if (entity.hurtTime > 0) {
                //We use a the first part of the sine wave to make the color more red as the entity gets hurt and animate it back to normal
                color = ColorUtil.interpolateColorC(
                    color, hurtTimeColor.color, sin(entity.hurtTime * (18 * Math.PI / 180))
                        .toFloat()
                )
            }
        }

        return color
    }

    fun collectEntities() {
        entities.clear()
        for (entity in mc.theWorld.getLoadedEntityList()) {
            if (!ESPUtil.isInView(entity)) continue
            if (entity === mc.thePlayer && mc.gameSettings.thirdPersonView == 0) continue
            if (entity is EntityAnimal && validEntities.getSetting("animals").isEnabled) {
                entities.add(entity)
            }

            if (entity is EntityPlayer && validEntities.getSetting("players").isEnabled) {
                entities.add(entity)
            }

            if ((entity is EntityMob || entity is EntitySlime) && validEntities.getSetting("mobs").isEnabled) {
                entities.add(entity)
            }
        }
    }


    companion object {
        var renderNameTags: Boolean = true
        @JvmField
        var renderGlint: Boolean = true

        @JvmField
        var fadeIn: Animation? = null

        private var currentIterations = 0

        private val framebufferList: MutableList<Framebuffer> = ArrayList()

        private fun renderFBO(framebuffer: Framebuffer, framebufferTexture: Int, shader: ShaderUtil, offset: Float) {
            framebuffer.framebufferClear()
            framebuffer.bindFramebuffer(false)
            shader.init()
            RenderUtil.bindTexture(framebufferTexture)
            shader.setUniformf("offset", offset, offset)
            shader.setUniformi("inTexture", 0)
            shader.setUniformi("check", 0)
            shader.setUniformf("lastPass", 0f)
            shader.setUniformf("halfpixel", 1.0f / framebuffer.framebufferWidth, 1.0f / framebuffer.framebufferHeight)
            shader.setUniformf(
                "iResolution",
                framebuffer.framebufferWidth.toFloat(),
                framebuffer.framebufferHeight.toFloat()
            )

            ShaderUtil.drawQuads()
            shader.unload()
        }
    }
}
