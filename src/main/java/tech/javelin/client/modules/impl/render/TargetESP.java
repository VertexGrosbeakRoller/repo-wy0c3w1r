package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.impl.combat.Aura;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(
        name = "TargetESP",
        category = Category.RENDER,
        description = "Выделяет цель"
)
public class TargetESP extends Module {
    public static final TargetESP INSTANCE = new TargetESP();
    private static final Identifier GLOW_TEXTURE = Identifier.of("javelin", "icons/glow.png");
    private final ModeSetting mode = new ModeSetting("Мод", new String[]{
            "Маркер", "Призраки", "Призраки 1", "Призраки 2", "Круг", "Призрачные орбиты", "Кристаллы", "yougame"
    });
    private final Animation animation;
    private final Animation animation2;
    private Entity lastTarget;
    private boolean textureLoaded;
    private float rotationAngle;
    private float rotationSpeed;
    private boolean isReversing;
    private float animationNurik;
    private long currentTime;
    private final long timestamp4;
    private long timestamp5;
    private float value23;

    private static final int ORBIT_PARTICLE_COUNT = 3;
    private static final float ORBIT_BASE_RADIUS = 0.4f;
    private static final float ORBIT_BASE_MUL = 0.1f;
    private static final float ORBIT_SPEED = 15.0f;
    private static final int ORBIT_TRAIL_LENGTH = 40;

    private static final float[] SCALE_CACHE = new float[101];

    static {
        for (int k = 0; k <= 100; k++) {
            SCALE_CACHE[k] = Math.max(0.28f * (k / 100f), 0.15f);
        }
    }

    private final Vec3d[] orbitPositions = new Vec3d[ORBIT_PARTICLE_COUNT];
    private final Vec3d[] orbitMotions = new Vec3d[ORBIT_PARTICLE_COUNT];
    private final List<Vec3d>[] orbitTrails = new List[ORBIT_PARTICLE_COUNT];
    private float movingAngle = 0;
    private long lastOrbitTime = 0;
    private final Animation orbitShrinkAnim = new Animation(300L, Easing.CUBIC_OUT);

    private float crystalMoving = 0;

    public TargetESP() {
        this.animation = new Animation(400L, Easing.CUBIC_OUT);
        this.animation2 = new Animation(250L, Easing.CUBIC_OUT);
        this.lastTarget = null;
        this.textureLoaded = false;
        this.rotationAngle = 0.0F;
        this.rotationSpeed = 0.0F;
        this.isReversing = false;
        this.animationNurik = 0.0F;
        this.currentTime = System.currentTimeMillis();
        this.timestamp4 = System.currentTimeMillis();
        this.timestamp5 = System.nanoTime();
        this.value23 = 0.0F;

        for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
            this.orbitTrails[i] = new ArrayList<>();
            this.orbitMotions[i] = Vec3d.ZERO;
        }
    }

    @EventTarget
    private void onRenderWorldLast(EventRender3D e) {
        Entity target = Aura.INSTANCE.getTarget();
        if (target != null) {
            if (this.lastTarget != target) {
                for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
                    orbitPositions[i] = null;
                    orbitMotions[i] = Vec3d.ZERO;
                    orbitTrails[i].clear();
                }
            }
            this.lastTarget = target;
            this.animation.update(true);
            this.animation2.update(true);
        } else {
            this.animation.update(false);
            this.animation2.update(false);
            if (this.animation.getValue() == 0.0F) {
                this.lastTarget = null;
            }
        }

        if (this.lastTarget != null && this.animation.getValue() > 0.01F) {
            String currentMode = this.mode.getValue().toString();
            switch (currentMode) {
                case "Маркер" -> this.renderMarker(e);
                case "Призраки" -> this.drawSpiritsTrack(e);
                case "Призраки 1" -> this.drawSpirits(e);
                case "Призраки 2" -> this.renderNursultan(e);
                case "Круг" -> this.drawCircle(e);
                case "Призрачные орбиты" -> this.drawGhostOrbits(e);
                case "Кристаллы" -> this.renderCrystals(e);
                case "yougame" -> this.renderYouGame(e);
            }
        }
    }

    private void renderYouGame(EventRender3D e) {
        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double partialTicks = e.getPartialTicks();
        Camera camera = mc.gameRenderer.getCamera();

        double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, partialTicks) - camPos.x;
        double yBase = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, partialTicks) - camPos.y;
        double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, partialTicks) - camPos.z;

        float time = (float) (System.currentTimeMillis() - this.timestamp4) / 1000.0F;
        float alphaGlobal = this.animation2.getValue();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();

        float rotSpeedBase = time * 9F;
        float vertSpeedBase = time * 3.25F;
        float entityHeight = this.lastTarget.getHeight();

        for (int i = 0; i < 3; i++) {
            float angleOffset = i * (float)(Math.PI * 2.0 / 3.0);

            for (int j = 0; j < 10; j++) {
                float trailDelay = j * 0.05F;
                float currentAngle = rotSpeedBase + angleOffset - (j * 0.1F);

                float radius = 0.725F;
                double posX = Math.cos(currentAngle) * radius;
                double posZ = Math.sin(currentAngle) * radius;

                double verticalOscillation = (Math.sin(vertSpeedBase - trailDelay) * 0.5 + 0.5) * (entityHeight * 0.975);

                matrices.push();
                matrices.translate(x + posX, yBase + verticalOscillation, z + posZ);
                matrices.multiply(camera.getRotation());
                Matrix4f matrix = matrices.peek().getPositionMatrix();

                float size = (0.30F - (j * 0.015F)) * alphaGlobal;
                if (size < 0.03F) size = 0.03F;

                float tailFade = 1.35F - (j / 13.5F);
                int alpha = (int) (150 * tailFade * alphaGlobal);
                int color = theme.getColor().withAlpha(alpha).getRGB();

                buffer.vertex(matrix, -size, -size, 0).texture(0, 0).color(color);
                buffer.vertex(matrix, size, -size, 0).texture(1, 0).color(color);
                buffer.vertex(matrix, size, size, 0).texture(1, 1).color(color);
                buffer.vertex(matrix, -size, size, 0).texture(0, 1).color(color);

                matrices.pop();
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        RenderSystem.defaultBlendFunc();
    }

    private void renderMarker(EventRender3D e) {
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double tickDelta = (double) e.getPartialTicks();
        MatrixStack matrices = e.getMatrix();
        double x = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderX, this.lastTarget.getX());
        double y = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderY, this.lastTarget.getY()) + (double) this.lastTarget.getHeight() / 2.0D;
        double z = MathHelper.lerp(tickDelta, this.lastTarget.lastRenderZ, this.lastTarget.getZ());
        matrices.push();
        matrices.translate(x - camPos.x, y - camPos.y, z - camPos.z);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-mc.gameRenderer.getCamera().getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(mc.gameRenderer.getCamera().getPitch()));
        float scale = 0.15F * this.animation.getValue();
        matrices.scale(-scale, -scale, scale);
        this.updateRotation();
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(this.rotationAngle));

        Identifier textureId = Identifier.of("javelin", "icons/marker.png");
        float alpha = this.animation.getValue();
        float size = 12.0F;
        Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA color = theme.getColor().withAlpha((int) (alpha * 255.0F));
        DrawUtil.drawTexture(matrices, textureId, 0.0F - size / 2.0F, 0.0F - size / 2.0F, size, size, color);
        matrices.pop();
    }

    @Native
    private void updateRotation() {
        if (!this.isReversing) {
            this.rotationSpeed += 0.01F;
            if ((double) this.rotationSpeed > 2.3D) {
                this.rotationSpeed = 2.3F;
                this.isReversing = true;
            }
        } else {
            this.rotationSpeed -= 0.01F;
            if ((double) this.rotationSpeed < -2.3D) {
                this.rotationSpeed = -2.3F;
                this.isReversing = false;
            }
        }
        this.rotationAngle += this.rotationSpeed;
        this.rotationAngle %= 360.0F;
    }

    public static double interpolate(double current, double old, double scale) {
        return old + (current - old) * scale;
    }

    private void drawSpirits(EventRender3D e) {
        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();

        double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double) e.getPartialTicks()) - camPos.x;
        double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double) e.getPartialTicks()) - camPos.y + (double) this.lastTarget.getHeight() / 2.0;
        double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double) e.getPartialTicks()) - camPos.z;

        float hurtTime = 0.0F;
        if (this.lastTarget instanceof LivingEntity living) {
            hurtTime = ((float) living.hurtTime - (living.hurtTime != 0 ? e.getPartialTicks() : 0.0F)) / 10.0F;
        }

        float animValue = -0.15F * this.animation2.getValue() + 0.65F;
        long time = (long) ((float) (System.currentTimeMillis() - this.timestamp4) / 2.0F);
        long nanoTime = System.nanoTime();
        float deltaTime = (float) (nanoTime - this.timestamp5) / 2000000.0F;
        this.timestamp5 = nanoTime;
        this.value23 += hurtTime * deltaTime;

        matrices.push();
        matrices.translate(x, y, z);
        matrices.scale(1.5F, 1.5F, 1.5F);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (int layer = 0; layer < 3; layer++) {
            for (int i = 0; i < 14; i++) {
                matrices.push();
                float progress = (float) i / 13.0F;
                float size = (0.55F * (1.0F - progress) + 0.2F * progress) * this.animation2.getValue();
                double angle = (double) (0.2F * ((float) time + this.value23 - (float) i * 7.0F) / 15.0F);

                boolean firstHalf = progress < 0.5F;
                float wave = firstHalf ? progress * 2.0F : (1.0F - progress) * 2.0F;
                double amplitude = Math.sin((double) wave * Math.PI) * 2.0;

                Random random = new Random((long) i * 12345L);
                double offsetX = (random.nextDouble() - 0.5) * amplitude;
                double offsetY = (random.nextDouble() - 0.5) * amplitude;
                double offsetZ = (random.nextDouble() - 0.5) * amplitude;

                double posX = -Math.sin(angle) * (double) animValue;
                double posZ = -Math.cos(angle) * (double) animValue;

                switch (layer) {
                    case 0 -> matrices.translate(posX + (offsetX * (double) this.animation2.getValue() - offsetX), posZ + ((double) i * 0.02) + (offsetY * (double) this.animation2.getValue() - offsetY), -posZ + (offsetZ * (double) this.animation2.getValue() - offsetZ));
                    case 1 -> matrices.translate(-posX + (offsetX * (double) this.animation2.getValue() - offsetX), posX - ((double) i * 0.02) + (offsetY * (double) this.animation2.getValue() - offsetY), -posZ + (offsetZ * (double) this.animation2.getValue() - offsetZ));
                    case 2 -> matrices.translate(-posX + (offsetX * (double) this.animation2.getValue() - offsetX), -posX + (offsetY * (double) this.animation2.getValue() - offsetY), posZ + (offsetZ * (double) this.animation2.getValue() - offsetZ));
                }

                float pSize = size * 0.5F;
                Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
                int color = theme.getColor().withAlpha((int) (600.0F * this.animation2.getValue())).getRGB();

                matrices.multiply(mc.gameRenderer.getCamera().getRotation());
                buffer.vertex(matrices.peek().getPositionMatrix(), -pSize, -pSize, 0).texture(1, 1).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), pSize, -pSize, 0).texture(0, 1).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), pSize, pSize, 0).texture(0, 0).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), -pSize, pSize, 0).texture(1, 0).color(color);
                matrices.pop();
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        RenderSystem.enableCull();
        matrices.pop();
    }

    private void renderNursultan(EventRender3D e) {
        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double) e.getPartialTicks()) - camPos.x;
        double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double) e.getPartialTicks()) - camPos.y + (double) this.lastTarget.getHeight() / 2.0;
        double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double) e.getPartialTicks()) - camPos.z;

        float time = (float) (System.currentTimeMillis() - this.timestamp4) / 1100.0F;
        float rotation = time * 360.0F;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int layer = 0; layer < 4; layer++) {
            float layerOffset = (float) (layer - 1) * 0.4F;
            for (float i = 0.0F; i < 130.0F; i++) {
                float angle = rotation + i + layerOffset * 360.0F;
                double radians = Math.toRadians(-angle);
                float size = (0.5F * (i / 140.0F)) * this.animation2.getValue();
                Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
                int color = theme.getColor().withAlpha((int) (600.0 * this.animation2.getValue() * MathHelper.clamp(size, 0, 1))).getRGB();

                matrices.push();
                matrices.translate(x, y + Math.sin(radians + 2.0) * (double) layerOffset, z);
                matrices.multiply(mc.gameRenderer.getCamera().getRotation());
                float hs = size / 2.0F;
                buffer.vertex(matrices.peek().getPositionMatrix(), (float) Math.cos(radians)*0.5f - hs, -hs, (float) Math.sin(radians)*0.5f).texture(0, 1).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), (float) Math.cos(radians)*0.5f + hs, -hs, (float) Math.sin(radians)*0.5f).texture(1, 1).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), (float) Math.cos(radians)*0.5f + hs, hs, (float) Math.sin(radians)*0.5f).texture(1, 0).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), (float) Math.cos(radians)*0.5f - hs, hs, (float) Math.sin(radians)*0.5f).texture(0, 0).color(color);
                matrices.pop();
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void drawCircle(EventRender3D e) {
        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double x = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, (double) e.getPartialTicks()) - camPos.x;
        double y = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, (double) e.getPartialTicks()) - camPos.y;
        double z = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, (double) e.getPartialTicks()) - camPos.z;

        float height = this.lastTarget.getHeight();
        float progress = (float) ((System.currentTimeMillis() % 1500) / 750.0);
        if (progress > 1.0f) progress = 2.0f - progress;
        progress = MathHelper.clamp(progress, 0, 1);

        matrices.push();
        matrices.translate(x, y + (height * progress), z);

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < 60; i++) {
            float angle = (float) (i * 6 * Math.PI / 180.0);
            float size = 0.3F * this.animation2.getValue();
            Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
            int color = theme.getColor().withAlpha((int)(400 * this.animation2.getValue())).getRGB();
            matrices.push();
            matrices.translate(Math.cos(angle) * 0.7, 0, Math.sin(angle) * 0.7);
            matrices.multiply(mc.gameRenderer.getCamera().getRotation());
            buffer.vertex(matrices.peek().getPositionMatrix(), -size, -size, 0).texture(0, 0).color(color);
            buffer.vertex(matrices.peek().getPositionMatrix(), size, -size, 0).texture(1, 0).color(color);
            buffer.vertex(matrices.peek().getPositionMatrix(), size, size, 0).texture(1, 1).color(color);
            buffer.vertex(matrices.peek().getPositionMatrix(), -size, size, 0).texture(0, 1).color(color);
            matrices.pop();
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private void drawSpiritsTrack(EventRender3D event3D) {
        Aura aura = Aura.INSTANCE;
        this.animation2.update(aura.getTarget() != null && aura.isEnabled());
        if (this.animation2.getValue() == 0) return;
        if (aura.getTarget() == null && this.lastTarget == null) return;

        MatrixStack e = event3D.getMatrix();
        Entity target = aura.getTarget() != null ? aura.getTarget() : this.lastTarget;

        long currentTimeMs = System.currentTimeMillis();
        this.animationNurik += (float) (currentTimeMs - this.currentTime) / 120.0F;
        this.currentTime = currentTimeMs;

        double x = interpolate(target.getX(), target.lastRenderX, event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().x;
        double y = interpolate(target.getY(), target.lastRenderY, event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().y;
        double z = interpolate(target.getZ(), target.lastRenderZ, event3D.getPartialTicks()) - mc.gameRenderer.getCamera().getPos().z;

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(770, 1, 0, 1);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (int i = 0; i < 9; i += 3) {
            for (int j = 0; j < 12; ++j) {
                float f2 = this.animationNurik + (float) j * 0.1F;
                Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
                int color = theme.getColor().withAlpha((int) (this.animation2.getValue() * 600.0F)).getRGB();
                e.push();
                e.translate(x + (0.8 * Math.sin(f2 + i*i)), y + 0.5 + (0.3 * Math.sin(this.animationNurik + j * 0.2)) + (0.2 * i), z + (0.8 * Math.cos(f2 - i*i)));
                e.scale(this.animation2.getValue() * (0.1F + j / 100.0F), this.animation2.getValue() * (0.1F + j / 100.0F), 1);
                e.multiply(mc.gameRenderer.getCamera().getRotation());
                buffer.vertex(e.peek().getPositionMatrix(), -0.5f, -0.5f, 0).texture(0, 0).color(color);
                buffer.vertex(e.peek().getPositionMatrix(), 0.5f, -0.5f, 0).texture(1, 0).color(color);
                buffer.vertex(e.peek().getPositionMatrix(), 0.5f, 0.5f, 0).texture(1, 1).color(color);
                buffer.vertex(e.peek().getPositionMatrix(), -0.5f, 0.5f, 0).texture(0, 1).color(color);
                e.pop();
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void drawGhostOrbits(EventRender3D e) {
        if (this.lastTarget == null) return;
        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        Camera camera = mc.gameRenderer.getCamera();

        double tx = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, e.getPartialTicks());
        double ty = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, e.getPartialTicks()) + this.lastTarget.getHeight()/2.0;
        double tz = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, e.getPartialTicks());

        movingAngle += (System.currentTimeMillis() - lastOrbitTime) * 0.5f;
        lastOrbitTime = System.currentTimeMillis();

        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, GLOW_TEXTURE);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(770, 1);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();

        for (int i = 0; i < ORBIT_PARTICLE_COUNT; i++) {
            float angle = movingAngle + (i * 120);
            double rad = Math.toRadians(angle);
            double ox = Math.cos(rad) * 0.7;
            double oz = Math.sin(rad) * 0.7;
            double oy = Math.sin(rad * 0.5) * 0.3;

            Vec3d pos = new Vec3d(tx + ox, ty + oy, tz + oz);
            orbitTrails[i].add(0, pos);
            if(orbitTrails[i].size() > ORBIT_TRAIL_LENGTH) orbitTrails[i].remove(ORBIT_TRAIL_LENGTH);

            for (int j = 0; j < orbitTrails[i].size(); j++) {
                Vec3d p = orbitTrails[i].get(j);
                float f = 1.0f - (float)j/ORBIT_TRAIL_LENGTH;
                float size = 0.3f * f * this.animation2.getValue();
                int color = theme.getColor().withAlpha((int)(200 * f * this.animation2.getValue())).getRGB();
                matrices.push();
                matrices.translate(p.x - camPos.x, p.y - camPos.y, p.z - camPos.z);
                matrices.multiply(camera.getRotation());
                buffer.vertex(matrices.peek().getPositionMatrix(), -size, -size, 0).texture(0, 0).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), size, -size, 0).texture(1, 0).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), size, size, 0).texture(1, 1).color(color);
                buffer.vertex(matrices.peek().getPositionMatrix(), -size, size, 0).texture(0, 1).color(color);
                matrices.pop();
            }
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(true);
    }

    private void renderCrystals(EventRender3D e) {
        float alpha = this.animation2.getValue();
        if (alpha <= 0.0F || this.lastTarget == null) return;

        MatrixStack matrices = e.getMatrix();
        Vec3d camPos = mc.gameRenderer.getCamera().getPos();
        double tx = interpolate(this.lastTarget.getX(), this.lastTarget.lastRenderX, e.getPartialTicks()) - camPos.x;
        double ty = interpolate(this.lastTarget.getY(), this.lastTarget.lastRenderY, e.getPartialTicks()) - camPos.y;
        double tz = interpolate(this.lastTarget.getZ(), this.lastTarget.lastRenderZ, e.getPartialTicks()) - camPos.z;

        crystalMoving += 1.0f;
        Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
        ColorRGBA c = theme.getColor();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder cb = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
        matrices.push();
        matrices.translate(tx, ty + this.lastTarget.getHeight()*0.5, tz);

        for (int i = 0; i < 360; i += 45) {
            double rad = Math.toRadians(i + crystalMoving);
            matrices.push();
            matrices.translate(Math.cos(rad)*0.8, Math.sin(crystalMoving*0.05 + i)*0.2, Math.sin(rad)*0.8);
            Matrix4f m = matrices.peek().getPositionMatrix();
            int color = c.withAlpha((int)(200 * alpha)).getRGB();
            cb.vertex(m, 0, 0.2f, 0).color(color);
            cb.vertex(m, -0.1f, 0, -0.1f).color(color);
            cb.vertex(m, 0.1f, 0, -0.1f).color(color);
            cb.vertex(m, 0, -0.2f, 0).color(color);
            cb.vertex(m, 0.1f, 0, -0.1f).color(color);
            cb.vertex(m, -0.1f, 0, -0.1f).color(color);
            matrices.pop();
        }
        BufferRenderer.drawWithGlobalProgram(cb.end());
        matrices.pop();
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
    }

    private ColorRGBA blendColors(ColorRGBA c1, ColorRGBA c2, float ratio) {
        int r = (int) (c1.getRed() * (1 - ratio) + c2.getRed() * ratio);
        int g = (int) (c1.getGreen() * (1 - ratio) + c2.getGreen() * ratio);
        int b = (int) (c1.getBlue() * (1 - ratio) + c2.getBlue() * ratio);
        int a = (int) (c1.getAlpha() * (1 - ratio) + c2.getAlpha() * ratio);
        return new ColorRGBA(r, g, b, a);
    }
}