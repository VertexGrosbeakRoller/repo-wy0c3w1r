package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@ModuleAnnotation(
   name = "AncientDebris",
   category = Category.RENDER,
   description = "Обводка незеритовых обломков через стены"
)
public final class AncientDebris extends Module {
   public static final AncientDebris INSTANCE = new AncientDebris();
   
   private final NumberSetting radius = new NumberSetting("Радиус", 16, 4, 32, 1, "Блоки");
   
   private final List<BlockPos> foundBlocks = new ArrayList<>();
   private final Set<BlockPos> notifiedBlocks = new HashSet<>();
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
      RenderSystem.disableDepthTest();
      RenderSystem.depthMask(false);
      RenderSystem.lineWidth(2.0F);
      RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);
      
      Tessellator tessellator = Tessellator.getInstance();
      BufferBuilder buffer = tessellator.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);
      Matrix4f matrix = matrices.peek().getPositionMatrix();
      
      int r = 139, g = 90, b = 43, a = 255;
      
      for (BlockPos pos : foundBlocks) {
         float x = (float)(pos.getX() - camera.x);
         float y = (float)(pos.getY() - camera.y);
         float z = (float)(pos.getZ() - camera.z);
         
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
      }
      
      BufferRenderer.drawWithGlobalProgram(buffer.end());
      
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
               if (block == Blocks.ANCIENT_DEBRIS) {
                  foundBlocks.add(pos.toImmutable());
                  
                  // Chat notification for newly found blocks
                  if (!notifiedBlocks.contains(pos)) {
                     notifiedBlocks.add(pos.toImmutable());
                     mc.player.sendMessage(
                        Text.literal("§6[AncientDebris] §fОБНАРУЖЕН незеритовый обломок: §e" + pos.getX() + " " + pos.getY() + " " + pos.getZ()),
                        false
                     );
                  }
               }
            }
         }
      }
   }
   
   @Override
   public void onEnable() {
      scanTick = 0;
      foundBlocks.clear();
      notifiedBlocks.clear();
   }
   
   @Override
   public void onDisable() {
      foundBlocks.clear();
      notifiedBlocks.clear();
   }
}
