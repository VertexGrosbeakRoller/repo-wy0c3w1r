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
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.player.EventTotemPop;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.render.display.base.color.ColorRGBA;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

@ModuleAnnotation(
   name = "Particles",
   category = Category.RENDER,
   description = "Кастомные партиклы с физикой"
)
public final class Particles extends Module {
   // Known particle textures from assets/javelin/Particle/
   private static final String[] KNOWN_TEXTURES = {
      "Default", "glow", "dollar", "star", "heart", "crown", "sparkle", "snowflake", "skull", "Copper"
   };
   
   public static final Particles INSTANCE = new Particles();
   
   // Настройки появления
   private final BooleanSetting onEnderPearl = new BooleanSetting("Эндер перл", "Партиклы за эндер перлом", true);
   private final BooleanSetting onTrident = new BooleanSetting("Трезубец", "Партиклы за трезубцем", true);
   private final BooleanSetting onCrit = new BooleanSetting("Крит", "Партиклы при крите", true);
   private final BooleanSetting onWalk = new BooleanSetting("Ходьба", "Партиклы при ходьбе", true);
   private final BooleanSetting onTotem = new BooleanSetting("Тотем", "Партиклы при тотеме", true);
   
   // Настройки партиклей
   private final NumberSetting particleSize = new NumberSetting("Размер", 1.5F, 0.5F, 5.0F, 0.1F);
   private final NumberSetting particleCount = new NumberSetting("Количество", 20, 5, 50, 1);
   private final NumberSetting particleLife = new NumberSetting("Время жизни", 30, 10, 100, 5, "Тики");
   private final BooleanSetting useThemeColor = new BooleanSetting("Цвет темы", "Брать цвет из темы клиента", true);
   private final NumberSetting gravity = new NumberSetting("Гравитация", 0.05F, 0.0F, 0.2F, 0.01F);
   
   // Particle texture selection from assets/javelin/Particle
   private final ModeSetting particleTexture;
   
   private final List<CustomParticle> particles = new ArrayList<>();
   private final Random random = new Random();
   private final Map<String, Identifier> textureCache = new HashMap<>();
   private double lastX, lastY, lastZ;
   private int walkTicks = 0;
   
   private Particles() {
      this.particleTexture = new ModeSetting("Текстура", KNOWN_TEXTURES);
   }
   
   public String getSelectedTexture() {
      return particleTexture.get();
   }
   
   private Identifier getTextureIdentifier(String textureName) {
      if (textureName.equals("Default")) {
         return null;
      }
      
      if (textureCache.containsKey(textureName)) {
         return textureCache.get(textureName);
      }
      
      // Resources are at javelin:Particle/<name>.png
      Identifier id = Identifier.of("javelin", "particle/" + textureName.toLowerCase());
      textureCache.put(textureName, id);
      return id;
   }
   
   @Override
   public void onEnable() {
      particles.clear();
      if (mc.player != null) {
         lastX = mc.player.getX();
         lastY = mc.player.getY();
         lastZ = mc.player.getZ();
      }
   }
   
   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null) return;
      
      // Обновляем партиклы
      Iterator<CustomParticle> iterator = particles.iterator();
      while (iterator.hasNext()) {
         CustomParticle p = iterator.next();
         p.update();
         if (p.isDead()) {
            iterator.remove();
         }
      }
      
      // Партиклы при ходьбе
      if (onWalk.isEnabled() && mc.player.isOnGround()) {
         double dx = mc.player.getX() - lastX;
         double dz = mc.player.getZ() - lastZ;
         double dist = Math.sqrt(dx * dx + dz * dz);
         
         if (dist > 0.1) {
            walkTicks++;
            if (walkTicks % 3 == 0) {
               spawnParticles(mc.player.getX(), mc.player.getY(), mc.player.getZ(), (int) particleCount.getCurrent() / 4);
            }
         }
      }
      
      lastX = mc.player.getX();
      lastY = mc.player.getY();
      lastZ = mc.player.getZ();
      
      // Партиклы за эндер перлами
      if (onEnderPearl.isEnabled()) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EnderPearlEntity) {
               EnderPearlEntity pearl = (EnderPearlEntity) entity;
               if (random.nextInt(3) == 0) {
                  spawnParticles(pearl.getX(), pearl.getY(), pearl.getZ(), 1);
               }
            }
         }
      }
      
      // Партиклы за трезубцами
      if (onTrident.isEnabled()) {
         for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof TridentEntity) {
               TridentEntity trident = (TridentEntity) entity;
               if (random.nextInt(2) == 0) {
                  spawnParticles(trident.getX(), trident.getY(), trident.getZ(), 2);
               }
            }
         }
      }
   }
   
   @EventTarget
   @Native
   public void onTotemPop(EventTotemPop event) {
      if (onTotem.isEnabled()) {
         spawnParticles(event.getPlayer().getX(), event.getPlayer().getY() + 1, event.getPlayer().getZ(), (int) particleCount.getCurrent() * 2);
      }
   }
   
   // Вызывается вручную при крите
   public void spawnCritParticles(double x, double y, double z) {
      if (isEnabled() && onCrit.isEnabled()) {
         spawnParticles(x, y + 1, z, (int) particleCount.getCurrent() / 2);
      }
   }
   
   @EventTarget
   public void onRender3D(EventRender3D event) {
      if (particles.isEmpty() || mc.player == null) return;
      
      String selectedTexture = particleTexture.get();
      Identifier textureId = getTextureIdentifier(selectedTexture);
      
      MatrixStack matrices = event.getMatrix();
      Vec3d camera = mc.gameRenderer.getCamera().getPos();
      
      matrices.push();
      
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.depthMask(false);
      
      Tessellator tessellator = Tessellator.getInstance();
      
      if (textureId != null) {
         // Render with texture
         RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
         RenderSystem.setShaderTexture(0, textureId);
         BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
         
         for (CustomParticle particle : particles) {
            double x = particle.x - camera.x;
            double y = particle.y - camera.y;
            double z = particle.z - camera.z;
            
            float alpha = particle.getAlpha();
            float size = particle.size;
            ColorRGBA color = particle.color;
            int colorInt = color.withAlpha((int)(alpha * 255)).getRGB();
            
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            // Billboarding - particles always face camera
            buffer.vertex(matrix, (float)(x - size), (float)(y - size), (float)z).texture(0, 1).color(colorInt);
            buffer.vertex(matrix, (float)(x + size), (float)(y - size), (float)z).texture(1, 1).color(colorInt);
            buffer.vertex(matrix, (float)(x + size), (float)(y + size), (float)z).texture(1, 0).color(colorInt);
            buffer.vertex(matrix, (float)(x - size), (float)(y + size), (float)z).texture(0, 0).color(colorInt);
         }
         
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      } else {
         // Render with colors (Default mode)
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         
         for (CustomParticle particle : particles) {
            double x = particle.x - camera.x;
            double y = particle.y - camera.y;
            double z = particle.z - camera.z;
            
            float alpha = particle.getAlpha();
            float size = particle.size;
            ColorRGBA color = particle.color;
            
            Matrix4f matrix = matrices.peek().getPositionMatrix();
            buffer.vertex(matrix, (float)(x - size), (float)(y - size), (float)z).color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
            buffer.vertex(matrix, (float)(x + size), (float)(y - size), (float)z).color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
            buffer.vertex(matrix, (float)(x + size), (float)(y + size), (float)z).color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
            buffer.vertex(matrix, (float)(x - size), (float)(y + size), (float)z).color(color.getRed(), color.getGreen(), color.getBlue(), (int)(alpha * 255));
         }
         
         BufferRenderer.drawWithGlobalProgram(buffer.end());
      }
      
      RenderSystem.depthMask(true);
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      matrices.pop();
   }
   
   private void spawnParticles(double x, double y, double z, int count) {
      ColorRGBA baseColor;
      if (useThemeColor.isEnabled()) {
         baseColor = Javelin.getInstance().getThemeManager().getCurrentTheme().getColor();
      } else {
         baseColor = new ColorRGBA(255, 255, 255, 200);
      }
      
      for (int i = 0; i < count; i++) {
         double px = x + (random.nextFloat() - 0.5) * 0.5;
         double py = y + (random.nextFloat() - 0.5) * 0.5;
         double pz = z + (random.nextFloat() - 0.5) * 0.5;
         
         double vx = (random.nextFloat() - 0.5) * 0.2;
         double vy = (random.nextFloat() - 0.5) * 0.2 + 0.1;
         double vz = (random.nextFloat() - 0.5) * 0.2;
         
         ColorRGBA color = new ColorRGBA(
            Math.min(255, baseColor.getRed() + random.nextInt(40) - 20),
            Math.min(255, baseColor.getGreen() + random.nextInt(40) - 20),
            Math.min(255, baseColor.getBlue() + random.nextInt(40) - 20),
            200
         );
         
         particles.add(new CustomParticle(px, py, pz, vx, vy, vz, particleSize.getCurrent(), color, (int) particleLife.getCurrent(), gravity.getCurrent()));
      }
   }
   
   private static class CustomParticle {
      double x, y, z;
      double vx, vy, vz;
      float size;
      ColorRGBA color;
      int age;
      int maxAge;
      double gravity;
      
      CustomParticle(double x, double y, double z, double vx, double vy, double vz, float size, ColorRGBA color, int maxAge, double gravity) {
         this.x = x;
         this.y = y;
         this.z = z;
         this.vx = vx;
         this.vy = vy;
         this.vz = vz;
         this.size = size / 10.0f;
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
