package tech.javelin.client.modules.impl.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(name = "FireFly", category = Category.RENDER, description = "Светлячки вокруг игрока")
public class FireFly extends Module {
    public static final FireFly INSTANCE = new FireFly();
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public final ModeSetting mode = new ModeSetting("Mode", "Ghost2", "Classic", "Random");
    public final NumberSetting count = new NumberSetting("Count", 30.0f, 5.0f, 100.0f, 5.0f);
    public final NumberSetting range = new NumberSetting("Range", 15.0f, 5.0f, 50.0f, 5.0f);
    public final NumberSetting speed = new NumberSetting("Speed", 0.5f, 0.1f, 2.0f, 0.1f);
    public final NumberSetting size = new NumberSetting("Size", 0.15f, 0.05f, 0.5f, 0.05f);
    public final ColorSetting color = new ColorSetting("Color", new ColorRGBA(255, 255, 100, 200));
    public final BooleanSetting glow = new BooleanSetting("Glow", true);

    private final List<FireflyParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastUpdate = 0;

    private static class FireflyParticle {
        double x, y, z;
        float alpha;
        float phase;
        ColorRGBA color;

        FireflyParticle(double x, double y, double z, ColorRGBA color) {
            this.x = x; this.y = y; this.z = z;
            this.alpha = 0.5f + (float) Math.random() * 0.5f;
            this.phase = (float) (Math.random() * Math.PI * 2);
            this.color = color;
        }
    }

    @Override
    public void onEnable() {
        particles.clear();
        lastUpdate = System.currentTimeMillis();
    }

    @Override
    public void onDisable() {
        particles.clear();
    }

    @EventTarget
    public void onRender3D(EventRender3D event) {
        if (mc.world == null || mc.player == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdate) / 1000.0f, 0.1f);
        lastUpdate = currentTime;

        updateParticles(deltaTime);
        
        while (particles.size() < (int) count.getCurrent()) {
            spawnParticle();
        }
        
        if (!particles.isEmpty()) {
            renderParticles(event.getMatrix());
        }
    }

    private void updateParticles(float deltaTime) {
        float time = (System.currentTimeMillis() % 10000) / 1000.0f;
        float moveSpeed = speed.getCurrent() * deltaTime * 5;
        float rangeVal = range.getCurrent();

        Iterator<FireflyParticle> it = particles.iterator();
        while (it.hasNext()) {
            FireflyParticle p = it.next();
            p.phase += deltaTime * (0.5f + random.nextFloat() * 0.5f);

            if (mode.is("Ghost2")) {
                p.x += MathHelper.sin(p.phase) * moveSpeed * 0.3f;
                p.z += MathHelper.cos(p.phase * 0.7f) * moveSpeed * 0.3f;
                p.y += MathHelper.sin(p.phase * 0.5f) * moveSpeed * 0.1f;
                p.alpha = 0.5f + 0.5f * MathHelper.sin(time * 2 + p.phase);
            } else if (mode.is("Classic")) {
                p.x += (random.nextFloat() - 0.5f) * moveSpeed;
                p.y += (random.nextFloat() - 0.5f) * moveSpeed * 0.3f;
                p.z += (random.nextFloat() - 0.5f) * moveSpeed;
                p.alpha = 0.4f + 0.6f * (float) Math.abs(Math.sin(time + p.phase));
            } else {
                p.x += MathHelper.sin(p.phase * 1.3f) * moveSpeed;
                p.z += MathHelper.cos(p.phase * 0.9f) * moveSpeed;
                p.y += MathHelper.sin(p.phase * 0.6f) * moveSpeed * 0.5f;
                p.alpha = 0.3f + 0.7f * random.nextFloat();
            }

            // Remove if too far from player
            double dx = p.x - mc.player.getX();
            double dy = p.y - mc.player.getY();
            double dz = p.z - mc.player.getZ();
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > rangeVal * 1.5) {
                it.remove();
            }
        }
    }

    private void spawnParticle() {
        PlayerEntity player = mc.player;
        if (player == null) return;

        float r = range.getCurrent();
        double x = player.getX() + (random.nextFloat() - 0.5f) * r * 2;
        double y = player.getY() + 1 + random.nextFloat() * 5; // 1-6 blocks above player
        double z = player.getZ() + (random.nextFloat() - 0.5f) * r * 2;

        ColorRGBA c = color.getColor();
        ColorRGBA pColor = new ColorRGBA(
            Math.max(0, Math.min(255, c.getRed() + random.nextInt(30) - 15)),
            Math.max(0, Math.min(255, c.getGreen() + random.nextInt(30) - 15)),
            Math.max(0, Math.min(255, c.getBlue() + random.nextInt(30) - 15)),
            c.getAlpha()
        );
        particles.add(new FireflyParticle(x, y, z, pColor));
    }

    private void renderParticles(MatrixStack matrices) {
        Vec3d camera = mc.gameRenderer.getCamera().getPos();
        float camYaw = mc.gameRenderer.getCamera().getYaw();
        float camPitch = mc.gameRenderer.getCamera().getPitch();

        // Calculate billboard right/up vectors from camera rotation
        float yawRad = (float) Math.toRadians(-camYaw);
        float pitchRad = (float) Math.toRadians(-camPitch);
        float cosYaw = MathHelper.cos(yawRad);
        float sinYaw = MathHelper.sin(yawRad);
        float cosPitch = MathHelper.cos(pitchRad);
        float sinPitch = MathHelper.sin(pitchRad);
        
        // Right vector (perpendicular to camera look direction in horizontal plane)
        float rx = cosYaw, ry = 0, rz = sinYaw;
        // Up vector (perpendicular to both look and right)
        float ux = sinYaw * sinPitch, uy = cosPitch, uz = -cosYaw * sinPitch;

        matrices.push();
        
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableCull();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float pSize = size.getCurrent();
        
        for (FireflyParticle particle : particles) {
            float x = (float) (particle.x - camera.x);
            float y = (float) (particle.y - camera.y);
            float z = (float) (particle.z - camera.z);
            int alpha = (int) (particle.alpha * particle.color.getAlpha());
            int cr = particle.color.getRed();
            int cg = particle.color.getGreen();
            int cb = particle.color.getBlue();
            
            // Camera-facing billboard quad
            float s = pSize;
            buffer.vertex(matrix, x - rx*s - ux*s, y - ry*s - uy*s, z - rz*s - uz*s).color(cr, cg, cb, alpha);
            buffer.vertex(matrix, x + rx*s - ux*s, y + ry*s - uy*s, z + rz*s - uz*s).color(cr, cg, cb, alpha);
            buffer.vertex(matrix, x + rx*s + ux*s, y + ry*s + uy*s, z + rz*s + uz*s).color(cr, cg, cb, alpha);
            buffer.vertex(matrix, x - rx*s + ux*s, y - ry*s + uy*s, z - rz*s + uz*s).color(cr, cg, cb, alpha);
            
            // Glow effect - larger semi-transparent quad
            if (glow.isEnabled()) {
                float gs = pSize * 2.5f;
                int ga = alpha / 4;
                buffer.vertex(matrix, x - rx*gs - ux*gs, y - ry*gs - uy*gs, z - rz*gs - uz*gs).color(cr, cg, cb, ga);
                buffer.vertex(matrix, x + rx*gs - ux*gs, y + ry*gs - uy*gs, z + rz*gs - uz*gs).color(cr, cg, cb, ga);
                buffer.vertex(matrix, x + rx*gs + ux*gs, y + ry*gs + uy*gs, z + rz*gs + uz*gs).color(cr, cg, cb, ga);
                buffer.vertex(matrix, x - rx*gs + ux*gs, y - ry*gs + uy*gs, z - rz*gs + uz*gs).color(cr, cg, cb, ga);
            }
        }
        
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.enableCull();
        RenderSystem.disableBlend();
        matrices.pop();
    }
}
