package tech.javelin.client.modules.impl.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.opengl.GL11;
import com.darkmagician6.eventapi.EventTarget;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ColorSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "FireFly", category = Category.RENDER, description = "Fireflies particles in sky like Ghosts 2")
public class FireFly extends Module {
    public static final FireFly INSTANCE = new FireFly();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting mode = new ModeSetting("Mode", "Ghost2", "Classic", "Random");
    public final NumberSetting count = new NumberSetting("Count", 50.0f, 10.0f, 200.0f, 10.0f);
    public final NumberSetting range = new NumberSetting("Range", 100.0f, 20.0f, 200.0f, 10.0f);
    public final NumberSetting speed = new NumberSetting("Speed", 0.5f, 0.1f, 2.0f, 0.1f);
    public final NumberSetting height = new NumberSetting("Height", 80.0f, 30.0f, 150.0f, 5.0f);

    public final ColorSetting color = new ColorSetting("Color", new ColorRGBA(255, 255, 100, 200));
    public final NumberSetting size = new NumberSetting("Size", 2.0f, 0.5f, 5.0f, 0.5f);
    public final BooleanSetting glow = new BooleanSetting("Glow", true);
    public final NumberSetting glowRadius = new NumberSetting("Glow Radius", 3.0f, 1.0f, 10.0f, 0.5f);

    public final BooleanSetting followPlayer = new BooleanSetting("Follow Player", true);
    public final BooleanSetting connectLines = new BooleanSetting("Connect Lines", true);
    public final NumberSetting lineWidth = new NumberSetting("Line Width", 1.0f, 0.5f, 3.0f, 0.5f);
    public final NumberSetting maxLineDist = new NumberSetting("Max Line Distance", 20.0f, 5.0f, 50.0f, 5.0f);

    private final List<FireflyParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastUpdate = 0;

    private static class FireflyParticle {
        double x, y, z;
        double prevX, prevY, prevZ;
        float size;
        float alpha;
        float phase;
        float speedVal;
        ColorRGBA color;

        FireflyParticle(double x, double y, double z, float size, ColorRGBA color, float speed) {
            this.x = x; this.y = y; this.z = z;
            this.prevX = x; this.prevY = y; this.prevZ = z;
            this.size = size;
            this.alpha = 1.0f;
            this.phase = (float) Math.random() * MathHelper.PI * 2;
            this.color = color;
            this.speedVal = speed;
        }
    }

    @Override
    public void onEnable() {
        particles.clear();
        lastUpdate = System.currentTimeMillis();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdate) / 1000.0f;
        lastUpdate = currentTime;

        updateParticles(deltaTime);
        if (particles.size() < count.getCurrent()) {
            spawnParticles();
        }
        renderParticles(event.getMatrix());
    }

    private void updateParticles(float deltaTime) {
        PlayerEntity player = mc.player;
        float time = (System.currentTimeMillis() % 10000) / 1000.0f;

        for (FireflyParticle particle : particles) {
            particle.prevX = particle.x;
            particle.prevY = particle.y;
            particle.prevZ = particle.z;

            float moveSpeed = speed.getCurrent() * deltaTime * 10;

            if (mode.is("Ghost2")) {
                particle.phase += deltaTime * 0.5f;
                particle.x += MathHelper.sin(particle.phase) * moveSpeed * 0.3f;
                particle.z += MathHelper.cos(particle.phase * 0.7f) * moveSpeed * 0.3f;
                particle.y += MathHelper.sin(particle.phase * 0.5f) * moveSpeed * 0.15f;
                particle.alpha = 0.6f + 0.4f * MathHelper.sin(time * 2 + particle.phase);
            } else if (mode.is("Classic")) {
                particle.x += (random.nextFloat() - 0.5f) * moveSpeed;
                particle.y += (random.nextFloat() - 0.5f) * moveSpeed * 0.3f;
                particle.z += (random.nextFloat() - 0.5f) * moveSpeed;
            } else {
                particle.phase += deltaTime;
                particle.x += MathHelper.sin(particle.phase * 1.3f) * moveSpeed;
                particle.z += MathHelper.cos(particle.phase * 0.9f) * moveSpeed;
                particle.y += MathHelper.sin(particle.phase * 0.6f) * moveSpeed * 0.5f;
            }

            if (followPlayer.isEnabled()) {
                double dx = particle.x - player.getX();
                double dz = particle.z - player.getZ();
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist > range.getCurrent()) {
                    particle.x = player.getX() + (random.nextFloat() - 0.5f) * range.getCurrent();
                    particle.z = player.getZ() + (random.nextFloat() - 0.5f) * range.getCurrent();
                }
                double targetY = player.getY() + height.getCurrent();
                if (particle.y < targetY - 20 || particle.y > targetY + 20) {
                    particle.y = targetY + (random.nextFloat() - 0.5f) * 20;
                }
            }
        }

        particles.removeIf(p -> {
            if (!followPlayer.isEnabled()) {
                double dx = p.x - mc.player.getX();
                double dy = p.y - mc.player.getY();
                double dz = p.z - mc.player.getZ();
                return Math.sqrt(dx * dx + dy * dy + dz * dz) > range.getCurrent() * 1.5;
            }
            return false;
        });
    }

    private void spawnParticles() {
        PlayerEntity player = mc.player;
        if (player == null) return;

        int toSpawn = (int) (count.getCurrent() - particles.size());
        for (int i = 0; i < toSpawn && i < 5; i++) {
            double x = player.getX() + (random.nextFloat() - 0.5f) * range.getCurrent();
            double z = player.getZ() + (random.nextFloat() - 0.5f) * range.getCurrent();
            double y = player.getY() + height.getCurrent() + (random.nextFloat() - 0.5f) * 20;

            ColorRGBA c = color.getColor();
            ColorRGBA particleColor = new ColorRGBA(
                c.getRed() + random.nextInt(20) - 10,
                c.getGreen() + random.nextInt(20) - 10,
                c.getBlue() + random.nextInt(20) - 10,
                c.getAlpha()
            );
            particles.add(new FireflyParticle(x, y, z, size.getCurrent(), particleColor, speed.getCurrent()));
        }
    }

    private void renderParticles(MatrixStack matrices) {
        if (particles.isEmpty()) return;
        Vec3d camera = mc.gameRenderer.getCamera().getPos();

        matrices.push();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
        
        // Setup for lines
        if (connectLines.isEnabled()) {
            RenderSystem.lineWidth(lineWidth.getCurrent());
            
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
            
            for (int i = 0; i < particles.size(); i++) {
                for (int j = i + 1; j < particles.size(); j++) {
                    FireflyParticle p1 = particles.get(i);
                    FireflyParticle p2 = particles.get(j);
                    double dx = p1.x - p2.x;
                    double dy = p1.y - p2.y;
                    double dz = p1.z - p2.z;
                    double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
                    if (dist < maxLineDist.getCurrent()) {
                        float alpha = 1.0f - (float) (dist / maxLineDist.getCurrent());
                        int lineAlpha = (int) (alpha * 100);
                        
                        buffer.vertex((float)(p1.x - camera.x), (float)(p1.y - camera.y), (float)(p1.z - camera.z))
                              .color(p1.color.getRed(), p1.color.getGreen(), p1.color.getBlue(), lineAlpha);
                        buffer.vertex((float)(p2.x - camera.x), (float)(p2.y - camera.y), (float)(p2.z - camera.z))
                              .color(p2.color.getRed(), p2.color.getGreen(), p2.color.getBlue(), lineAlpha);
                    }
                }
            }
            
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }

        // Draw particles as small quads
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        
        for (FireflyParticle particle : particles) {
            double x = particle.x - camera.x;
            double y = particle.y - camera.y;
            double z = particle.z - camera.z;
            int alpha = (int) (particle.alpha * particle.color.getAlpha());
            float pSize = particle.size / 10.0f;
            
            int r = particle.color.getRed();
            int g = particle.color.getGreen();
            int b = particle.color.getBlue();
            
            buffer.vertex((float)(x - pSize), (float)(y - pSize), (float)z).color(r, g, b, alpha);
            buffer.vertex((float)(x + pSize), (float)(y - pSize), (float)z).color(r, g, b, alpha);
            buffer.vertex((float)(x + pSize), (float)(y + pSize), (float)z).color(r, g, b, alpha);
            buffer.vertex((float)(x - pSize), (float)(y + pSize), (float)z).color(r, g, b, alpha);
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
