package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "Chams",
   category = Category.RENDER,
   description = "Рисует игроков моделькой как в Totem Angel"
)
public class Chams extends Module {
   public static final Chams INSTANCE = new Chams();

   private final NumberSetting alpha = new NumberSetting("Прозрачность", 0.8f, 0.1f, 1.0f, 0.05f);
   private final NumberSetting brightness = new NumberSetting("Яркость", 0.6f, 0.1f, 1.0f, 0.05f);
   private final NumberSetting lineWidth = new NumberSetting("Толщина линий", 1.0f, 0.1f, 5.0f, 0.05f);
   private final BooleanSetting throughWalls = new BooleanSetting("Сквозь стены", true);
   private final BooleanSetting glow = new BooleanSetting("Свечение", true);
   private final BooleanSetting fill = new BooleanSetting("Заливка", true);
   private final BooleanSetting wireframe = new BooleanSetting("Каркас", true);
   private final NumberSetting range = new NumberSetting("Дистанция", 64.0f, 8.0f, 128.0f, 4.0f);
   private final BooleanSetting fillInvis = new BooleanSetting("Невидимые", true);

   private Chams() {}

   @EventTarget
   @Native
   public void onRender(EventRender3D event) {
      if (mc.world == null || mc.player == null) return;

      float tickDelta = event.getPartialTicks();
      Vec3d cam = mc.gameRenderer.getCamera().getPos();
      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
      ColorRGBA themeColor = theme.getColor();
      int color = themeColor.getRGB();
      float baseAlpha = alpha.getCurrent();

      for (Entity entity : mc.world.getEntities()) {
         // Check range
         if (entity.squaredDistanceTo(mc.player) > range.getCurrent() * range.getCurrent()) continue;
         
         // Только игроки
         if (entity instanceof AbstractClientPlayerEntity player) {
            if (!player.isAlive()) continue;
            if (player == mc.player && mc.options.getPerspective() == Perspective.FIRST_PERSON) continue;
            
            // Check invisible
            if (player.isInvisible() && !fillInvis.isEnabled()) continue;
            
            renderChams(event.getMatrix(), player, cam, tickDelta, color, baseAlpha);
         }
      }
   }

   private void renderChams(MatrixStack stack, AbstractClientPlayerEntity player,
                            Vec3d cam, float tickDelta, int color, float baseAlpha) {
      float a = baseAlpha;
      float bright = brightness.getCurrent();
      float r = ((color >> 16) & 0xFF) / 255f * bright;
      float g = ((color >> 8)  & 0xFF) / 255f * bright;
      float b = (color & 0xFF)          / 255f * bright;

      double x = player.lastRenderX + (player.getX() - player.lastRenderX) * tickDelta;
      double y = player.lastRenderY + (player.getY() - player.lastRenderY) * tickDelta;
      double z = player.lastRenderZ + (player.getZ() - player.lastRenderZ) * tickDelta;

      float bodyYaw   = player.bodyYaw;
      float headYaw   = player.headYaw;
      float pitch     = player.getPitch();
      float netHeadYaw = headYaw - bodyYaw;

      float limbPos   = player.limbAnimator.getPos(tickDelta);
      float limbSpeed = player.limbAnimator.getSpeed(tickDelta);
      float swing     = MathHelper.sin(limbPos * 0.6662f) * 0.6f * limbSpeed;

      float swingProgress = player.getHandSwingProgress(tickDelta);
      boolean mainRight = player.getMainArm() == net.minecraft.util.Arm.RIGHT;
      float swingAngle = -(float)(Math.sin(Math.sqrt(swingProgress) * Math.PI) * 1.2f);
      float rightSwingX = swingAngle;
      float leftSwingX = swingAngle;

      boolean slim   = player.getSkinTextures().model() == net.minecraft.client.util.SkinTextures.Model.SLIM;
      boolean sneak  = player.isInSneakingPose();
      boolean elytra = player.isGliding();
      boolean swim   = player.isSwimming();

      stack.push();
      stack.translate(x - cam.x, y - cam.y, z - cam.z);
      stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180f - bodyYaw));

      if (elytra) {
         float ticks  = player.getGlidingTicks() + tickDelta;
         float factor = MathHelper.clamp(ticks * ticks / 100f, 0f, 1f);
         if (!player.isUsingRiptide())
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(factor * (-90f - pitch)));
      } else if (swim) {
         float swimPitch = player.isSubmergedInWater() ? -90f - pitch : -90f;
         stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(swimPitch));
         stack.translate(0, -1.0, 0.3);
      }

      stack.scale(-1f, -1f, 1f);
      stack.scale(0.9375f, 0.9375f, 0.9375f);
      stack.translate(0, -1.501, 0);

      if (sneak) stack.translate(0, 0.2, 0);

      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      if (throughWalls.isEnabled()) RenderSystem.disableDepthTest();

      float idleTime = (player.age + tickDelta) * 0.05f;

      // Render fill if enabled
      if (fill.isEnabled()) {
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         drawBody(stack, buf, swing, rightSwingX, leftSwingX, netHeadYaw, pitch, slim, sneak, swim, limbPos, limbSpeed, idleTime, r, g, b, a);
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }

      // Render wireframe if enabled
      if (wireframe.isEnabled()) {
         float lw = lineWidth.getCurrent();
         float la = MathHelper.clamp(a + 0.2f, 0f, 1f);
         float lr = MathHelper.clamp(r + 0.15f, 0f, 1f);
         float lg = MathHelper.clamp(g + 0.15f, 0f, 1f);
         float lb = MathHelper.clamp(b + 0.15f, 0f, 1f);

         GL11.glEnable(GL11.GL_LINE_SMOOTH);
         GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
         RenderSystem.lineWidth(lw);

         if (glow.isEnabled()) {
            RenderSystem.blendFuncSeparate(
               GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE,
               GlStateManager.SrcFactor.ONE,       GlStateManager.DstFactor.ZERO);
            // Simple glow with 3 layers
            for (int i = 3; i >= 1; i--) {
               float expand = i * 0.5f;
               float glowA  = MathHelper.clamp(la * (1f / (i + 1)) * 0.7f, 0f, 1f);
               BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
               drawBodyLines(stack, buf, swing, rightSwingX, leftSwingX, netHeadYaw, pitch, slim, sneak, swim, limbPos, limbSpeed, idleTime, lr, lg, lb, glowA, expand);
               BufferRenderer.drawWithGlobalProgram(buf.end());
            }
            RenderSystem.defaultBlendFunc();
         }

         BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
         drawBodyLines(stack, buf, swing, rightSwingX, leftSwingX, netHeadYaw, pitch, slim, sneak, swim, limbPos, limbSpeed, idleTime, lr, lg, lb, la, 0f);
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }

      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      if (throughWalls.isEnabled()) RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();

      stack.pop();
   }

   private void drawBody(MatrixStack m, BufferBuilder buf, float swing,
                         float rightSwingX, float leftSwingX,
                         float headYaw, float headPitch,
                         boolean slim, boolean sneak, boolean swim,
                         float limbPos, float limbSpeed, float idleTime,
                         float r, float g, float b, float a) {
      float u = 1f / 16f;
      float armW = slim ? 3 : 4;
      float armSwayZ = MathHelper.sin(idleTime) * 0.04f + 0.03f * limbSpeed;

      float swimPhase = limbPos * 0.6662f;
      float swimCycle = MathHelper.sin(swimPhase) * limbSpeed;
      float swimKick  = swim ? swimCycle * 0.4f : 0f;

      // Head
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(headYaw));
      m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));
      box(buf, m.peek().getPositionMatrix(), -4*u, -8*u, -4*u, 8*u, 8*u, 8*u, r, g, b, a);
      m.pop();

      // Body
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      if (swim) {
         m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(limbPos * 0.3331f) * 3f * limbSpeed));
      }
      box(buf, m.peek().getPositionMatrix(), -4*u, 0, -2*u, 8*u, 12*u, 4*u, r, g, b, a);
      m.pop();

      // Arms
      float swimArmX = swim ? swimCycle * 0.6f - (float)(Math.PI / 2f) : 0f;
      float rightArmX = swim ? swimArmX : swing;
      float leftArmX  = swim ? swimArmX : -swing;
      float swimSpread = swim ? MathHelper.clamp(swimCycle, 0f, 1f) * (float)(Math.PI / 4f) : 0f;
      float rightArmZ = swim ?  swimSpread : 0f;
      float leftArmZ  = swim ? -swimSpread : 0f;

      // Right Arm
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      m.translate(-4*u, 0, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(rightArmX + (swim ? 0 : rightSwingX)));
      m.multiply(RotationAxis.POSITIVE_Z.rotation(swim ? rightArmZ : armSwayZ));
      box(buf, m.peek().getPositionMatrix(), -armW*u, 0, -2*u, armW*u, 12*u, 4*u, r, g, b, a);
      m.pop();

      // Left Arm
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      m.translate(4*u, 0, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(leftArmX + (swim ? 0 : leftSwingX)));
      m.multiply(RotationAxis.POSITIVE_Z.rotation(swim ? leftArmZ : -armSwayZ));
      box(buf, m.peek().getPositionMatrix(), 0, 0, -2*u, armW*u, 12*u, 4*u, r, g, b, a);
      m.pop();

      // Right Leg
      m.push();
      m.translate(-2*u, 12*u, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(swim ? -swimKick : -swing));
      box(buf, m.peek().getPositionMatrix(), -2*u, 0, -2*u, 4*u, 12*u, 4*u, r, g, b, a);
      m.pop();

      // Left Leg
      m.push();
      m.translate(2*u, 12*u, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(swim ? swimKick : swing));
      box(buf, m.peek().getPositionMatrix(), -2*u, 0, -2*u, 4*u, 12*u, 4*u, r, g, b, a);
      m.pop();
   }

   private void drawBodyLines(MatrixStack m, BufferBuilder buf, float swing,
                              float rightSwingX, float leftSwingX,
                              float headYaw, float headPitch,
                              boolean slim, boolean sneak, boolean swim,
                              float limbPos, float limbSpeed, float idleTime,
                              float r, float g, float b, float a, float expand) {
      float u = 1f / 16f;
      float armW = slim ? 3 : 4;
      float armSwayZ = MathHelper.sin(idleTime) * 0.04f + 0.03f * limbSpeed;

      float swimPhase = limbPos * 0.6662f;
      float swimCycle = MathHelper.sin(swimPhase) * limbSpeed;
      float swimArmX = swim ? swimCycle * 0.6f - (float)(Math.PI / 2f) : 0f;
      float rightArmX = swim ? swimArmX : swing;
      float leftArmX  = swim ? swimArmX : -swing;
      float swimSpread = swim ? MathHelper.clamp(swimCycle, 0f, 1f) * (float)(Math.PI / 4f) : 0f;
      float rightArmZ = swim ?  swimSpread : 0f;
      float leftArmZ  = swim ? -swimSpread : 0f;
      float swimKick = swim ? swimCycle * 0.4f : 0f;
      float ex = expand * u;

      // Head
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      m.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(headYaw));
      m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(headPitch));
      boxLines(buf, m.peek().getPositionMatrix(), -4*u-ex, -8*u-ex, -4*u-ex, 8*u+ex*2, 8*u+ex*2, 8*u+ex*2, r, g, b, a);
      m.pop();

      // Body
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      if (swim) {
         m.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.sin(limbPos * 0.3331f) * 3f * limbSpeed));
      }
      boxLines(buf, m.peek().getPositionMatrix(), -4*u-ex, 0-ex, -2*u-ex, 8*u+ex*2, 12*u+ex*2, 4*u+ex*2, r, g, b, a);
      m.pop();

      // Right Arm
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      m.translate(-4*u, 0, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(rightArmX + (swim ? 0 : rightSwingX)));
      m.multiply(RotationAxis.POSITIVE_Z.rotation(swim ? rightArmZ : armSwayZ));
      boxLines(buf, m.peek().getPositionMatrix(), -armW*u-ex, 0-ex, -2*u-ex, armW*u+ex*2, 12*u+ex*2, 4*u+ex*2, r, g, b, a);
      m.pop();

      // Left Arm
      m.push();
      if (sneak) {
         m.translate(0, 12*u, 0);
         m.multiply(RotationAxis.POSITIVE_X.rotationDegrees(28.64f));
         m.translate(0, -12*u, 0);
      }
      m.translate(4*u, 0, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(leftArmX + (swim ? 0 : leftSwingX)));
      m.multiply(RotationAxis.POSITIVE_Z.rotation(swim ? leftArmZ : -armSwayZ));
      boxLines(buf, m.peek().getPositionMatrix(), 0-ex, 0-ex, -2*u-ex, armW*u+ex*2, 12*u+ex*2, 4*u+ex*2, r, g, b, a);
      m.pop();

      // Right Leg
      m.push();
      m.translate(-2*u, 12*u, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(swim ? -swimKick : -swing));
      boxLines(buf, m.peek().getPositionMatrix(), -2*u-ex, 0-ex, -2*u-ex, 4*u+ex*2, 12*u+ex*2, 4*u+ex*2, r, g, b, a);
      m.pop();

      // Left Leg
      m.push();
      m.translate(2*u, 12*u, 0);
      m.multiply(RotationAxis.POSITIVE_X.rotation(swim ? swimKick : swing));
      boxLines(buf, m.peek().getPositionMatrix(), -2*u-ex, 0-ex, -2*u-ex, 4*u+ex*2, 12*u+ex*2, 4*u+ex*2, r, g, b, a);
      m.pop();
   }

   private void box(BufferBuilder b, Matrix4f m,
                    float x, float y, float z, float sx, float sy, float sz,
                    float r, float g, float bl, float a) {
      float x2 = x+sx, y2 = y+sy, z2 = z+sz;
      // Front
      b.vertex(m, x,  y,  z2).color(r,g,bl,a); b.vertex(m, x2, y,  z2).color(r,g,bl,a);
      b.vertex(m, x2, y2, z2).color(r,g,bl,a); b.vertex(m, x,  y2, z2).color(r,g,bl,a);
      // Back
      b.vertex(m, x2, y,  z ).color(r,g,bl,a); b.vertex(m, x,  y,  z ).color(r,g,bl,a);
      b.vertex(m, x,  y2, z ).color(r,g,bl,a); b.vertex(m, x2, y2, z ).color(r,g,bl,a);
      // Left
      b.vertex(m, x,  y,  z ).color(r,g,bl,a); b.vertex(m, x,  y,  z2).color(r,g,bl,a);
      b.vertex(m, x,  y2, z2).color(r,g,bl,a); b.vertex(m, x,  y2, z ).color(r,g,bl,a);
      // Right
      b.vertex(m, x2, y,  z2).color(r,g,bl,a); b.vertex(m, x2, y,  z ).color(r,g,bl,a);
      b.vertex(m, x2, y2, z ).color(r,g,bl,a); b.vertex(m, x2, y2, z2).color(r,g,bl,a);
      // Top
      b.vertex(m, x,  y2, z2).color(r,g,bl,a); b.vertex(m, x2, y2, z2).color(r,g,bl,a);
      b.vertex(m, x2, y2, z ).color(r,g,bl,a); b.vertex(m, x,  y2, z ).color(r,g,bl,a);
      // Bottom
      b.vertex(m, x,  y,  z ).color(r,g,bl,a); b.vertex(m, x2, y,  z ).color(r,g,bl,a);
      b.vertex(m, x2, y,  z2).color(r,g,bl,a); b.vertex(m, x,  y,  z2).color(r,g,bl,a);
   }

   private void boxLines(BufferBuilder b, Matrix4f m,
                         float x, float y, float z, float sx, float sy, float sz,
                         float r, float g, float bl, float a) {
      float x2 = x+sx, y2 = y+sy, z2 = z+sz;
      // Bottom rectangle
      line(b,m, x,y,z,    x2,y,z,   r,g,bl,a); line(b,m, x2,y,z,   x2,y2,z,  r,g,bl,a);
      line(b,m, x2,y2,z,  x,y2,z,   r,g,bl,a); line(b,m, x,y2,z,   x,y,z,    r,g,bl,a);
      // Top rectangle
      line(b,m, x,y,z2,   x2,y,z2,  r,g,bl,a); line(b,m, x2,y,z2,  x2,y2,z2, r,g,bl,a);
      line(b,m, x2,y2,z2, x,y2,z2,  r,g,bl,a); line(b,m, x,y2,z2,  x,y,z2,   r,g,bl,a);
      // Vertical lines
      line(b,m, x,y,z,    x,y,z2,   r,g,bl,a); line(b,m, x2,y,z,   x2,y,z2,  r,g,bl,a);
      line(b,m, x2,y2,z,  x2,y2,z2, r,g,bl,a); line(b,m, x,y2,z,   x,y2,z2,  r,g,bl,a);
   }

   private void line(BufferBuilder b, Matrix4f m,
                     float x1, float y1, float z1, float x2, float y2, float z2,
                     float r, float g, float bl, float a) {
      float dx = x2-x1, dy = y2-y1, dz = z2-z1;
      float len = (float) Math.sqrt(dx*dx+dy*dy+dz*dz);
      if (len == 0) len = 1;
      b.vertex(m, x1,y1,z1).color(r,g,bl,a).normal(dx/len,dy/len,dz/len);
      b.vertex(m, x2,y2,z2).color(r,g,bl,a).normal(dx/len,dy/len,dz/len);
   }
   
   // Simplified rendering for mobs and animals - just a box
   private void renderChamsEntity(MatrixStack stack, Entity entity,
                                   Vec3d cam, float tickDelta, int color, float baseAlpha) {
      float a = baseAlpha;
      float bright = brightness.getCurrent();
      float r = ((color >> 16) & 0xFF) / 255f * bright;
      float g = ((color >> 8)  & 0xFF) / 255f * bright;
      float b = (color & 0xFF)          / 255f * bright;
      
      double x = entity.lastRenderX + (entity.getX() - entity.lastRenderX) * tickDelta;
      double y = entity.lastRenderY + (entity.getY() - entity.lastRenderY) * tickDelta;
      double z = entity.lastRenderZ + (entity.getZ() - entity.lastRenderZ) * tickDelta;
      
      float width = entity.getWidth();
      float height = entity.getHeight();
      
      stack.push();
      stack.translate(x - cam.x, y - cam.y + height/2, z - cam.z);
      
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      if (throughWalls.isEnabled()) RenderSystem.disableDepthTest();
      
      // Simple box for entity
      if (fill.isEnabled()) {
         RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
         BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
         box(buf, stack.peek().getPositionMatrix(), -width/2, -height/2, -width/2, width, height, width, r, g, b, a);
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }
      
      if (wireframe.isEnabled()) {
         float lw = lineWidth.getCurrent();
         float la = MathHelper.clamp(a + 0.2f, 0f, 1f);
         float lr = MathHelper.clamp(r + 0.15f, 0f, 1f);
         float lg = MathHelper.clamp(g + 0.15f, 0f, 1f);
         float lb = MathHelper.clamp(b + 0.15f, 0f, 1f);
         
         GL11.glEnable(GL11.GL_LINE_SMOOTH);
         RenderSystem.setShader(ShaderProgramKeys.RENDERTYPE_LINES);
         RenderSystem.lineWidth(lw);
         
         BufferBuilder buf = Tessellator.getInstance().begin(VertexFormat.DrawMode.LINES, VertexFormats.LINES);
         boxLines(buf, stack.peek().getPositionMatrix(), -width/2, -height/2, -width/2, width, height, width, lr, lg, lb, la);
         BufferRenderer.drawWithGlobalProgram(buf.end());
      }
      
      RenderSystem.defaultBlendFunc();
      RenderSystem.enableCull();
      if (throughWalls.isEnabled()) RenderSystem.enableDepthTest();
      RenderSystem.disableBlend();
      
      stack.pop();
   }
}
