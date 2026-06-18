package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "HighJump",
   category = Category.MOVEMENT,
   description = "Подбрасывает при открытии шалкер-бокса рядом"
)
public final class HighJump extends Module {
   public static final HighJump INSTANCE = new HighJump();
   
   private final NumberSetting jumpHeight = new NumberSetting("Высота прыжка", 2.0F, 1.0F, 3.0F, 0.05F);
   
   private HighJump() {}

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null) return;
      
      // Scan nearby block entities for opening shulker boxes
      BlockPos playerPos = mc.player.getBlockPos();
      int scanRadius = 2;
      
      for (int x = -scanRadius; x <= scanRadius; x++) {
         for (int y = -2; y <= 2; y++) {
            for (int z = -scanRadius; z <= scanRadius; z++) {
               BlockPos pos = playerPos.add(x, y, z);
               BlockEntity be = mc.world.getBlockEntity(pos);
               if (!(be instanceof ShulkerBoxBlockEntity shulker)) continue;
               
               // Check distance
               double distX = mc.player.getX() - (pos.getX() + 0.5);
               double distZ = mc.player.getZ() - (pos.getZ() + 0.5);
               double horizDist = Math.sqrt(distX * distX + distZ * distZ);
               double distY = Math.abs(mc.player.getY() - (pos.getY() + 0.5));
               
               // Max Y distance depends on current Y velocity (already launched = allow bigger range)
               double maxDistY = mc.player.getVelocity().y > 1.0 ? 30.0 : 2.0;
               
               if (horizDist > 1.0 || distY > maxDistY) continue;
               
               // Check if shulker is opening (animation progress > 0)
               float progress = shulker.getAnimationProgress(1.0F);
               if (progress <= 0.0F) continue;
               
               // Apply upward velocity
               Vec3d vel = mc.player.getVelocity();
               mc.player.setVelocity(vel.x, jumpHeight.getCurrent(), vel.z);
               
               // Close screen if open
               if (mc.currentScreen != null) {
                  mc.player.closeHandledScreen();
               }
               return;
            }
         }
      }
   }
   
   @Override
   public void onEnable() {
   }
}
