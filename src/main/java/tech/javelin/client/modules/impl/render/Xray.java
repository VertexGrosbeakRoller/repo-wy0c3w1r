package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
   name = "Xray",
   category = Category.RENDER,
   description = "Подсвечивает руды через стены"
)
public final class Xray extends Module {
   public static final Xray INSTANCE = new Xray();
   
   private final NumberSetting radius = new NumberSetting("Радиус", 16, 4, 32, 1, "Блоки");
   private final BooleanSetting showDiamonds = new BooleanSetting("Алмазы", true);
   private final BooleanSetting showEmeralds = new BooleanSetting("Изумруды", true);
   private final BooleanSetting showGold = new BooleanSetting("Золото", true);
   private final BooleanSetting showIron = new BooleanSetting("Железо", false);
   private final BooleanSetting showRedstone = new BooleanSetting("Редстоун", false);
   private final BooleanSetting showLapis = new BooleanSetting("Лазурит", false);
   private final BooleanSetting showCopper = new BooleanSetting("Медь", false);
   
   private final List<OreEntry> foundBlocks = new ArrayList<>();
   private int scanTick = 0;
   
   private Xray() {}
   
   @EventTarget
   @Native
   public void onRender3D(EventRender3D event) {
      if (mc.player == null || mc.world == null) return;
      
      scanTick++;
      if (scanTick >= 20) {
         scanTick = 0;
         scanBlocks();
      }
      
      if (foundBlocks.isEmpty()) return;
      
      MatrixStack matrices = event.getMatrix();
      Vec3d camera = mc.gameRenderer.getCamera().getPos();
      
      matrices.push();
      
      RenderSystem.enableBlend();
      RenderSystem.defaultBlendFunc();
      RenderSystem.disableCull();
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.lineWidth(2.0F);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      
      for (OreEntry entry : foundBlocks) {
         double x = entry.pos.getX() - camera.x;
         double y = entry.pos.getY() - camera.y;
         double z = entry.pos.getZ() - camera.z;
         
         drawOutline(matrices, (float) x, (float) y, (float) z, entry.r, entry.g, entry.b);
      }
      
      RenderSystem.depthMask(true);
      RenderSystem.enableDepthTest();
      RenderSystem.enableCull();
      RenderSystem.disableBlend();
      
      matrices.pop();
   }
   
   private void scanBlocks() {
      foundBlocks.clear();
      if (mc.player == null || mc.world == null) return;
      
      int r = (int) radius.getCurrent();
      BlockPos playerPos = mc.player.getBlockPos();
      
      for (int x = -r; x <= r; x++) {
         for (int y = -r; y <= r; y++) {
            for (int z = -r; z <= r; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               Block block = mc.world.getBlockState(pos).getBlock();
               int[] color = getOreColor(block);
               if (color != null) {
                  foundBlocks.add(new OreEntry(pos, color[0], color[1], color[2]));
               }
            }
         }
      }
   }
   
   private int[] getOreColor(Block block) {
      if (showDiamonds.isEnabled() && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE))
         return new int[]{80, 220, 255};
      if (showEmeralds.isEnabled() && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE))
         return new int[]{80, 255, 80};
      if (showGold.isEnabled() && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE))
         return new int[]{255, 215, 0};
      if (showIron.isEnabled() && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE))
         return new int[]{200, 180, 160};
      if (showRedstone.isEnabled() && (block == Blocks.REDSTONE_ORE || block == Blocks.DEEPSLATE_REDSTONE_ORE))
         return new int[]{255, 50, 50};
      if (showLapis.isEnabled() && (block == Blocks.LAPIS_ORE || block == Blocks.DEEPSLATE_LAPIS_ORE))
         return new int[]{50, 50, 255};
      if (showCopper.isEnabled() && (block == Blocks.COPPER_ORE || block == Blocks.DEEPSLATE_COPPER_ORE))
         return new int[]{200, 120, 50};
      return null;
   }
   
   private void drawOutline(MatrixStack matrices, float x, float y, float z, int r, int g, int b) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      
      int a = 255;
      
      BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      
      // Bottom
      buffer.vertex(matrix, x, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y, z).color(r, g, b, a);
      // Top
      buffer.vertex(matrix, x, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z).color(r, g, b, a);
      // Verticals
      buffer.vertex(matrix, x, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z + 1).color(r, g, b, a);
      
      BufferRenderer.drawWithGlobalProgram(buffer.end());
   }
   
   @Override
   public void onEnable() {
      scanTick = 0;
      foundBlocks.clear();
   }
   
   @Override
   public void onDisable() {
      foundBlocks.clear();
   }
   
   private static class OreEntry {
      final BlockPos pos;
      final int r, g, b;
      OreEntry(BlockPos pos, int r, int g, int b) {
         this.pos = pos;
         this.r = r;
         this.g = g;
         this.b = b;
      }
   }
}
