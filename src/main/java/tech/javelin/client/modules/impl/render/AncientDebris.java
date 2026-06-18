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
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.render.display.base.color.ColorRGBA;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
   name = "AncientDebris",
   category = Category.RENDER,
   description = "Подсвечивает незеритовые обломки через стены"
)
public final class AncientDebris extends Module {
   public static final AncientDebris INSTANCE = new AncientDebris();
   
   private final NumberSetting radius = new NumberSetting("Радиус", 16, 4, 32, 1, "Блоки");
   private final BooleanSetting showDiamonds = new BooleanSetting("Алмазы", "Подсвечивать алмазную руду", false);
   private final BooleanSetting showEmeralds = new BooleanSetting("Изумруды", "Подсвечивать изумрудную руду", false);
   private final BooleanSetting showGold = new BooleanSetting("Золото", "Подсвечивать золотую руду", false);
   private final BooleanSetting showIron = new BooleanSetting("Железо", "Подсвечивать железную руду", false);
   
   private final List<BlockPos> foundBlocks = new ArrayList<>();
   private int scanTick = 0;
   
   private AncientDebris() {}
   
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
      RenderSystem.depthMask(false);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      
      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
      
      for (BlockPos pos : foundBlocks) {
         ColorRGBA color = getBlockColor(mc.world.getBlockState(pos).getBlock());
         if (color == null) continue;
         
         double x = pos.getX() - camera.x;
         double y = pos.getY() - camera.y;
         double z = pos.getZ() - camera.z;
         
         drawBox(matrices, (float) x, (float) y, (float) z, color);
      }
      
      RenderSystem.depthMask(true);
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
               
               if (isTargetBlock(block)) {
                  foundBlocks.add(pos);
               }
            }
         }
      }
   }
   
   private boolean isTargetBlock(Block block) {
      if (block == Blocks.ANCIENT_DEBRIS) return true;
      if (showDiamonds.isEnabled() && (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE)) return true;
      if (showEmeralds.isEnabled() && (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE)) return true;
      if (showGold.isEnabled() && (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE)) return true;
      if (showIron.isEnabled() && (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE)) return true;
      return false;
   }
   
   private ColorRGBA getBlockColor(Block block) {
      if (block == Blocks.ANCIENT_DEBRIS) return new ColorRGBA(139, 90, 43, 160);
      if (block == Blocks.DIAMOND_ORE || block == Blocks.DEEPSLATE_DIAMOND_ORE) return new ColorRGBA(80, 220, 255, 160);
      if (block == Blocks.EMERALD_ORE || block == Blocks.DEEPSLATE_EMERALD_ORE) return new ColorRGBA(80, 255, 80, 160);
      if (block == Blocks.GOLD_ORE || block == Blocks.DEEPSLATE_GOLD_ORE || block == Blocks.NETHER_GOLD_ORE) return new ColorRGBA(255, 215, 0, 160);
      if (block == Blocks.IRON_ORE || block == Blocks.DEEPSLATE_IRON_ORE) return new ColorRGBA(200, 180, 160, 160);
      return null;
   }
   
   private void drawBox(MatrixStack matrices, float x, float y, float z, ColorRGBA color) {
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      Tessellator tessellator = Tessellator.getInstance();
      
      int r = color.getRed(), g = color.getGreen(), b = color.getBlue(), a = color.getAlpha();
      
      // Draw filled box faces
      BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
      
      // Bottom
      buffer.vertex(matrix, x, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y, z + 1).color(r, g, b, a);
      // Top
      buffer.vertex(matrix, x, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z).color(r, g, b, a);
      // North
      buffer.vertex(matrix, x, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z).color(r, g, b, a);
      // South
      buffer.vertex(matrix, x, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z + 1).color(r, g, b, a);
      // East
      buffer.vertex(matrix, x + 1, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x + 1, y, z + 1).color(r, g, b, a);
      // West
      buffer.vertex(matrix, x, y, z).color(r, g, b, a);
      buffer.vertex(matrix, x, y, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z + 1).color(r, g, b, a);
      buffer.vertex(matrix, x, y + 1, z).color(r, g, b, a);
      
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
}
