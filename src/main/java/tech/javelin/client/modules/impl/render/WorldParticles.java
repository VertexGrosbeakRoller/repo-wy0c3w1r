package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

@ModuleAnnotation(
   name = "WorldParticles",
   category = Category.RENDER,
   description = "Частицы в мире: кубы, треугольники, прямоугольники"
)
public class WorldParticles extends Module {
   public static final WorldParticles INSTANCE = new WorldParticles();
   
   // Spawn settings
   private final BooleanSetting onWalk = new BooleanSetting("При ходьбе", true);
   private final BooleanSetting onJump = new BooleanSetting("При прыжке", true);
   private final BooleanSetting onSprint = new BooleanSetting("При беге", true);
   private final NumberSetting spawnRate = new NumberSetting("Частота", 3, 1, 10, 1);
   
   // Particle settings
   private final ModeSetting shape = new ModeSetting("Форма", "Куб", "Куб", "Треугольник", "Прямоугольник", "Кристалл");
   private final NumberSetting particleSize = new NumberSetting("Размер", 0.3f, 0.1f, 1.0f, 0.05f);
   private final NumberSetting particleLife = new NumberSetting("Время жизни", 40, 10, 100, 5);
   private final NumberSetting particleCount = new NumberSetting("Количество", 5, 1, 20, 1);
   private final NumberSetting gravity = new NumberSetting("Гравитация", 0.02f, 0.0f, 0.1f, 0.01f);
   private final NumberSetting glow = new NumberSetting("Свечение", 2.0f, 0.0f, 5.0f, 0.5f);
   
   // Appearance
   private final BooleanSetting useThemeColor = new BooleanSetting("Цвет темы", true);
   private final BooleanSetting rainbow = new BooleanSetting("Радуга", false);
   private final BooleanSetting rotate = new BooleanSetting("Вращение", true);
   private final NumberSetting rotationSpeed = new NumberSetting("Скорость вращения", 2.0f, 0.5f, 5.0f, 0.5f);
   
   private final List<WorldParticle> particles = new ArrayList<>();
   private final Random random = new Random();
   private int tickCounter = 0;
   private double lastX, lastY, lastZ;
   private boolean wasOnGround = true;
   private boolean wasSprinting = false;
   
   private WorldParticles() {}
   
   @Override
   public void onEnable() {
      particles.clear();
      tickCounter = 0;
      if (mc.player != null) {
         lastX = mc.player.getX();
         lastY = mc.player.getY();
         lastZ = mc.player.getZ();
      }
   }
   
   @EventTarget
   @Native
   public void onRender3D(EventRender3D event) {
      if (mc.player == null || mc.world == null) return;
      
      float tickDelta = event.getPartialTicks();
      tickCounter++;
      
      // Spawn particles based on conditions
      if (tickCounter % (11 - spawnRate.getCurrent()) == 0) {
         checkAndSpawnParticles();
      }
      
      // Update and render particles
      if (particles.isEmpty()) return;
      
      MatrixStack matrices = event.getMatrix();
      Vec3d camera = mc.gameRenderer.getCamera().getPos();
      
      matrices.push();
      GL11.glDisable(GL11.GL_DEPTH_TEST);
      GL11.glEnable(GL11.GL_BLEND);
      GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
      
      Iterator<WorldParticle> iterator = particles.iterator();
      while (iterator.hasNext()) {
         WorldParticle p = iterator.next();
         p.update();
         if (p.isDead()) {
            iterator.remove();
         } else {
            renderParticle(matrices, camera, p, tickDelta);
         }
      }
      
      GL11.glEnable(GL11.GL_DEPTH_TEST);
      matrices.pop();
      
      // Update state
      lastX = mc.player.getX();
      lastY = mc.player.getY();
      lastZ = mc.player.getZ();
      wasOnGround = mc.player.isOnGround();
      wasSprinting = mc.player.isSprinting();
   }
   
   private void checkAndSpawnParticles() {
      boolean shouldSpawn = false;
      
      // Walk check
      if (onWalk.isEnabled() && mc.player.isOnGround()) {
         double dx = mc.player.getX() - lastX;
         double dz = mc.player.getZ() - lastZ;
         if (Math.sqrt(dx*dx + dz*dz) > 0.05) {
            shouldSpawn = true;
         }
      }
      
      // Jump check
      if (onJump.isEnabled() && !mc.player.isOnGround() && wasOnGround) {
         shouldSpawn = true;
      }
      
      // Sprint check
      if (onSprint.isEnabled() && mc.player.isSprinting() && !wasSprinting) {
         shouldSpawn = true;
      }
      
      if (shouldSpawn) {
         spawnParticlesAt(mc.player.getX(), mc.player.getY(), mc.player.getZ());
      }
   }
   
   private void spawnParticlesAt(double x, double y, double z) {
      ColorRGBA baseColor;
      if (useThemeColor.isEnabled()) {
         baseColor = Javelin.getInstance().getThemeManager().getCurrentTheme().getColor();
      } else {
         baseColor = new ColorRGBA(100, 200, 255, 200);
      }
      
      int count = (int) particleCount.getCurrent();
      for (int i = 0; i < count; i++) {
         double px = x + (random.nextDouble() - 0.5) * 0.8;
         double py = y + random.nextDouble() * 0.5;
         double pz = z + (random.nextDouble() - 0.5) * 0.8;
         
         double vx = (random.nextDouble() - 0.5) * 0.1;
         double vy = random.nextDouble() * 0.1;
         double vz = (random.nextDouble() - 0.5) * 0.1;
         
         ColorRGBA color;
         if (rainbow.isEnabled()) {
            float hue = random.nextFloat();
            int rgb = java.awt.Color.HSBtoRGB(hue, 1.0f, 1.0f);
            color = new ColorRGBA((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, 200);
         } else {
            color = new ColorRGBA(
               Math.min(255, baseColor.getRed() + random.nextInt(60) - 30),
               Math.min(255, baseColor.getGreen() + random.nextInt(60) - 30),
               Math.min(255, baseColor.getBlue() + random.nextInt(60) - 30),
               200
            );
         }
         
         particles.add(new WorldParticle(
            px, py, pz, vx, vy, vz,
            particleSize.getCurrent(), color,
            (int) particleLife.getCurrent(), gravity.getCurrent()
         ));
      }
   }
   
   private void renderParticle(MatrixStack matrices, Vec3d camera, WorldParticle p, float tickDelta) {
      double x = p.x - camera.x;
      double y = p.y - camera.y;
      double z = p.z - camera.z;
      float size = p.size * (1.0f - (float)p.age / (float)p.maxAge);
      
      matrices.push();
      matrices.translate(x, y, z);
      
      if (rotate.isEnabled()) {
         float rot = (p.age + tickDelta) * rotationSpeed.getCurrent();
         org.joml.Quaternionf q1 = new org.joml.Quaternionf().fromAxisAngleDeg(1, 0, 0, rot);
         org.joml.Quaternionf q2 = new org.joml.Quaternionf().fromAxisAngleDeg(0, 1, 0, rot * 0.7f);
         matrices.multiply(q1);
         matrices.multiply(q2);
      }
      
      String shapeType = shape.get();
      switch (shapeType) {
         case "Куб":
            renderCube(matrices, size, p.color, p.getAlpha());
            break;
         case "Треугольник":
            renderTriangle(matrices, size, p.color, p.getAlpha());
            break;
         case "Прямоугольник":
            renderRectangle(matrices, size, p.color, p.getAlpha());
            break;
         case "Кристалл":
            renderCrystal(matrices, size, p.color, p.getAlpha());
            break;
      }
      
      matrices.pop();
   }
   
   private void renderCube(MatrixStack matrices, float size, ColorRGBA color, float alpha) {
      float s = size / 2;
      int col = color.withAlpha((int)(alpha * 180)).getRGB();
      int lineCol = color.withAlpha((int)(alpha * 255)).getRGB();
      
      // Fill
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      
      Matrix4f m = matrices.peek().getPositionMatrix();
      // Front
      vertex(buf, m, -s, -s, s, col); vertex(buf, m, s, -s, s, col);
      vertex(buf, m, s, s, s, col); vertex(buf, m, -s, s, s, col);
      // Back
      vertex(buf, m, s, -s, -s, col); vertex(buf, m, -s, -s, -s, col);
      vertex(buf, m, -s, s, -s, col); vertex(buf, m, s, s, -s, col);
      // Top
      vertex(buf, m, -s, s, s, col); vertex(buf, m, s, s, s, col);
      vertex(buf, m, s, s, -s, col); vertex(buf, m, -s, s, -s, col);
      // Bottom
      vertex(buf, m, -s, -s, -s, col); vertex(buf, m, s, -s, -s, col);
      vertex(buf, m, s, -s, s, col); vertex(buf, m, -s, -s, s, col);
      // Left
      vertex(buf, m, -s, -s, -s, col); vertex(buf, m, -s, -s, s, col);
      vertex(buf, m, -s, s, s, col); vertex(buf, m, -s, s, -s, col);
      // Right
      vertex(buf, m, s, -s, s, col); vertex(buf, m, s, -s, -s, col);
      vertex(buf, m, s, s, -s, col); vertex(buf, m, s, s, s, col);
      
      BufferRenderer.drawWithGlobalProgram(buf.end());
      
      // Wireframe
      buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
      // Edges
      line(buf, m, -s, -s, -s, s, -s, -s, lineCol);
      line(buf, m, s, -s, -s, s, s, -s, lineCol);
      line(buf, m, s, s, -s, -s, s, -s, lineCol);
      line(buf, m, -s, s, -s, -s, -s, -s, lineCol);
      line(buf, m, -s, -s, s, s, -s, s, lineCol);
      line(buf, m, s, -s, s, s, s, s, lineCol);
      line(buf, m, s, s, s, -s, s, s, lineCol);
      line(buf, m, -s, s, s, -s, -s, s, lineCol);
      line(buf, m, -s, -s, -s, -s, -s, s, lineCol);
      line(buf, m, s, -s, -s, s, -s, s, lineCol);
      line(buf, m, s, s, -s, s, s, s, lineCol);
      line(buf, m, -s, s, -s, -s, s, s, lineCol);
      
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }
   
   private void renderTriangle(MatrixStack matrices, float size, ColorRGBA color, float alpha) {
      float s = size;
      int col = color.withAlpha((int)(alpha * 180)).getRGB();
      int lineCol = color.withAlpha((int)(alpha * 255)).getRGB();
      
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLES, VertexFormats.POSITION_COLOR);
      
      Matrix4f m = matrices.peek().getPositionMatrix();
      // Pyramid
      vertex(buf, m, 0, s, 0, col);
      vertex(buf, m, -s, -s, s, col);
      vertex(buf, m, s, -s, s, col);
      
      vertex(buf, m, 0, s, 0, col);
      vertex(buf, m, s, -s, s, col);
      vertex(buf, m, s, -s, -s, col);
      
      vertex(buf, m, 0, s, 0, col);
      vertex(buf, m, s, -s, -s, col);
      vertex(buf, m, -s, -s, -s, col);
      
      vertex(buf, m, 0, s, 0, col);
      vertex(buf, m, -s, -s, -s, col);
      vertex(buf, m, -s, -s, s, col);
      
      BufferRenderer.drawWithGlobalProgram(buf.end());
      
      // Edges
      buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
      line(buf, m, 0, s, 0, -s, -s, s, lineCol);
      line(buf, m, 0, s, 0, s, -s, s, lineCol);
      line(buf, m, 0, s, 0, s, -s, -s, lineCol);
      line(buf, m, 0, s, 0, -s, -s, -s, lineCol);
      line(buf, m, -s, -s, s, s, -s, s, lineCol);
      line(buf, m, s, -s, s, s, -s, -s, lineCol);
      line(buf, m, s, -s, -s, -s, -s, -s, lineCol);
      line(buf, m, -s, -s, -s, -s, -s, s, lineCol);
      
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }
   
   private void renderRectangle(MatrixStack matrices, float size, ColorRGBA color, float alpha) {
      float w = size;
      float h = size * 1.5f;
      float d = size * 0.5f;
      int col = color.withAlpha((int)(alpha * 180)).getRGB();
      int lineCol = color.withAlpha((int)(alpha * 255)).getRGB();
      
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      
      Matrix4f m = matrices.peek().getPositionMatrix();
      // Sides
      vertex(buf, m, -w, -h, d, col); vertex(buf, m, w, -h, d, col);
      vertex(buf, m, w, h, d, col); vertex(buf, m, -w, h, d, col);
      
      vertex(buf, m, w, -h, -d, col); vertex(buf, m, -w, -h, -d, col);
      vertex(buf, m, -w, h, -d, col); vertex(buf, m, w, h, -d, col);
      
      BufferRenderer.drawWithGlobalProgram(buf.end());
      
      // Edges
      buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
      line(buf, m, -w, -h, d, w, -h, d, lineCol);
      line(buf, m, w, -h, d, w, h, d, lineCol);
      line(buf, m, w, h, d, -w, h, d, lineCol);
      line(buf, m, -w, h, d, -w, -h, d, lineCol);
      line(buf, m, -w, -h, -d, w, -h, -d, lineCol);
      line(buf, m, w, -h, -d, w, h, -d, lineCol);
      line(buf, m, w, h, -d, -w, h, -d, lineCol);
      line(buf, m, -w, h, -d, -w, -h, -d, lineCol);
      line(buf, m, -w, -h, d, -w, -h, -d, lineCol);
      line(buf, m, w, -h, d, w, -h, -d, lineCol);
      line(buf, m, w, h, d, w, h, -d, lineCol);
      line(buf, m, -w, h, d, -w, h, -d, lineCol);
      
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }
   
   private void renderCrystal(MatrixStack matrices, float size, ColorRGBA color, float alpha) {
      float s = size;
      int col = color.withAlpha((int)(alpha * 200)).getRGB();
      
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.POSITION_COLOR);
      
      Matrix4f m = matrices.peek().getPositionMatrix();
      // Diamond shape with cross
      line(buf, m, 0, -s, 0, s, 0, 0, col);
      line(buf, m, s, 0, 0, 0, s, 0, col);
      line(buf, m, 0, s, 0, -s, 0, 0, col);
      line(buf, m, -s, 0, 0, 0, -s, 0, col);
      line(buf, m, 0, -s, 0, 0, 0, s, col);
      line(buf, m, 0, 0, s, 0, s, 0, col);
      line(buf, m, 0, s, 0, 0, 0, -s, col);
      line(buf, m, 0, 0, -s, 0, -s, 0, col);
      // Inner cross
      line(buf, m, -s*0.5f, 0, -s*0.5f, s*0.5f, 0, s*0.5f, col);
      line(buf, m, s*0.5f, 0, -s*0.5f, -s*0.5f, 0, s*0.5f, col);
      
      BufferRenderer.drawWithGlobalProgram(buf.end());
   }
   
   private void vertex(BufferBuilder b, Matrix4f m, float x, float y, float z, int color) {
      float r = ((color >> 16) & 0xFF) / 255f;
      float g = ((color >> 8) & 0xFF) / 255f;
      float bl = (color & 0xFF) / 255f;
      float a = ((color >> 24) & 0xFF) / 255f;
      b.vertex(m, x, y, z).color(r, g, bl, a);
   }
   
   private void line(BufferBuilder b, Matrix4f m, float x1, float y1, float z1, float x2, float y2, float z2, int color) {
      vertex(b, m, x1, y1, z1, color);
      vertex(b, m, x2, y2, z2, color);
   }
   
   private static class WorldParticle {
      double x, y, z;
      double vx, vy, vz;
      float size;
      ColorRGBA color;
      int age;
      int maxAge;
      double gravity;
      
      WorldParticle(double x, double y, double z, double vx, double vy, double vz, 
                    float size, ColorRGBA color, int maxAge, double gravity) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.size = size;
         this.color = color;
         this.age = 0;
         this.maxAge = maxAge;
         this.gravity = gravity;
      }
      
      void update() {
         x += vx;
         y += vy;
         z += vz;
         vy -= gravity;
         age++;
      }
      
      boolean isDead() {
         return age >= maxAge;
      }
      
      float getAlpha() {
         return 1.0f - ((float) age / (float) maxAge);
      }
   }
}
