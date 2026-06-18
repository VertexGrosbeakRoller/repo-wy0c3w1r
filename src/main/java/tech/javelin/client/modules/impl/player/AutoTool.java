package tech.javelin.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Block;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "AutoTool",
   category = Category.PLAYER,
   description = "Автоматически выбирает лучший инструмент для копания блока"
)
public final class AutoTool extends Module {
   public static final AutoTool INSTANCE = new AutoTool();
   private int previousSlot = -1;

   private AutoTool() {
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
      if (mc.player.isCreative()) return;
      
      if (mc.interactionManager.isBreakingBlock()) {
         if (previousSlot == -1) {
            previousSlot = mc.player.getInventory().selectedSlot;
         }
         int toolSlot = findOptimalTool();
         if (toolSlot != -1) {
            mc.player.getInventory().selectedSlot = toolSlot;
         }
         return;
      }
      
      // Return to previous slot when done mining
      if (previousSlot != -1) {
         mc.player.getInventory().selectedSlot = previousSlot;
         previousSlot = -1;
      }
   }

   private int findOptimalTool() {
      HitResult hit = mc.crosshairTarget;
      if (hit instanceof BlockHitResult blockHit) {
         Block block = mc.world.getBlockState(blockHit.getBlockPos()).getBlock();
         int bestSlot = -1;
         float bestSpeed = 1.0F;
         for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.isDamageable() && stack.getMaxDamage() - stack.getDamage() <= 5) {
               continue;
            }
            float speed = stack.getMiningSpeedMultiplier(block.getDefaultState());
            if (speed > bestSpeed) {
               bestSpeed = speed;
               bestSlot = i;
            }
         }
         return bestSlot;
      }
      return -1;
   }

   public void onDisable() {
      this.previousSlot = -1;
      super.onDisable();
   }
}
