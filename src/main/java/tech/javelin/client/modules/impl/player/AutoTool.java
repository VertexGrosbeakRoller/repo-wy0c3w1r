package tech.javelin.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.*;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
   name = "AutoTool",
   category = Category.PLAYER,
   description = "Умный выбор предмета: меч для атаки, зелье/гэпл при низком хп"
)
public final class AutoTool extends Module {
   public static final AutoTool INSTANCE = new AutoTool();
   private int previousSlot = -1;
   private boolean switchedForAttack = false;

   private AutoTool() {
   }

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
      if (mc.player.isCreative()) return;
      
      float health = mc.player.getHealth();
      
      // If looking at entity and left clicking — switch to sword
      if (mc.crosshairTarget instanceof EntityHitResult && mc.options.attackKey.isPressed()) {
         int swordSlot = findSword();
         if (swordSlot != -1 && mc.player.getInventory().selectedSlot != swordSlot) {
            if (!switchedForAttack) {
               previousSlot = mc.player.getInventory().selectedSlot;
               switchedForAttack = true;
            }
            mc.player.getInventory().selectedSlot = swordSlot;
         }
         return;
      }
      
      // If right clicking and low HP — switch to healing item
      if (mc.options.useKey.isPressed() && health <= 10.0F) {
         int healSlot = findHealingItem();
         if (healSlot != -1) {
            mc.player.getInventory().selectedSlot = healSlot;
            return;
         }
      }
      
      // If right clicking and high HP — switch to any potion
      if (mc.options.useKey.isPressed() && health > 10.0F) {
         int potionSlot = findAnyPotion();
         if (potionSlot != -1) {
            mc.player.getInventory().selectedSlot = potionSlot;
            return;
         }
      }
      
      // Block mining — switch to best tool
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
      
      // Return to previous slot when done attacking
      if (switchedForAttack && !mc.options.attackKey.isPressed()) {
         if (previousSlot != -1) {
            mc.player.getInventory().selectedSlot = previousSlot;
         }
         previousSlot = -1;
         switchedForAttack = false;
      }
      
      // Return to previous slot when done mining
      if (!mc.interactionManager.isBreakingBlock() && previousSlot != -1 && !switchedForAttack) {
         mc.player.getInventory().selectedSlot = previousSlot;
         previousSlot = -1;
      }
   }

   private int findSword() {
      int bestSlot = -1;
      float bestDamage = 0;
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof SwordItem sword) {
            float damage = sword.getComponents().get(DataComponentTypes.ATTRIBUTE_MODIFIERS) != null ? 1.0F : 0;
            // Prefer higher tier swords
            if (stack.getItem() == Items.NETHERITE_SWORD) damage = 8;
            else if (stack.getItem() == Items.DIAMOND_SWORD) damage = 7;
            else if (stack.getItem() == Items.IRON_SWORD) damage = 6;
            else if (stack.getItem() == Items.STONE_SWORD) damage = 5;
            else if (stack.getItem() == Items.GOLDEN_SWORD) damage = 4;
            else if (stack.getItem() == Items.WOODEN_SWORD) damage = 3;
            else damage = 2;
            if (damage > bestDamage) {
               bestDamage = damage;
               bestSlot = i;
            }
         }
      }
      return bestSlot;
   }

   private int findHealingItem() {
      // First look for golden apple
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() == Items.GOLDEN_APPLE || stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE) {
            return i;
         }
      }
      // Then look for healing/regen potion
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof PotionItem) {
            PotionContentsComponent contents = stack.get(DataComponentTypes.POTION_CONTENTS);
            if (contents != null) {
               boolean hasHealing = false;
               for (var e : contents.getEffects()) {
                  if (e.getEffectType() == StatusEffects.INSTANT_HEALTH ||
                      e.getEffectType() == StatusEffects.REGENERATION) {
                     hasHealing = true;
                     break;
                  }
               }
               if (hasHealing) return i;
            }
         }
      }
      return -1;
   }

   private int findAnyPotion() {
      for (int i = 0; i < 9; i++) {
         ItemStack stack = mc.player.getInventory().getStack(i);
         if (stack.getItem() instanceof PotionItem) {
            return i;
         }
      }
      return -1;
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
      this.switchedForAttack = false;
      super.onDisable();
   }
}
