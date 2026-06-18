package tech.javelin.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.client.network.PlayerListEntry;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.utility.math.Timer;

@ModuleAnnotation(
   name = "AntiBot",
   category = Category.COMBAT,
   description = "Обнаруживает и фильтрует ботов античита"
)
public final class AntiBot extends Module {
   public static final AntiBot INSTANCE = new AntiBot();
   private final Set<UUID> bots = new HashSet<>();
   private final Timer timer = new Timer();
   
   private final BooleanSetting tablistCheck = new BooleanSetting("Таблист", "Бот если нет в таблисте", true);
   private final BooleanSetting armorCheck = new BooleanSetting("Броня", "Проверка на кожаную/железную броню без чаров", true);
   private final BooleanSetting nameCheck = new BooleanSetting("Ник", "Проверка на подозрительный ник", true);
   private final BooleanSetting pingCheck = new BooleanSetting("Пинг", "Бот если пинг 0 или отрицательный", true);
   private final BooleanSetting entityIdCheck = new BooleanSetting("EntityID", "Бот если entityId отрицательный", true);
   private final BooleanSetting duplicateCheck = new BooleanSetting("Дубликат", "Бот если есть дубликат ника", true);

   private AntiBot() {
   }

   @EventTarget
   @Native
   public void onTick(EventUpdate event) {
      if (mc.player == null || mc.world == null) return;
      
      if (this.timer.finished(15000L) && !this.bots.isEmpty()) {
         this.bots.clear();
         this.timer.reset();
      }

      for (PlayerEntity player : mc.world.getPlayers()) {
         if (player == null || player == mc.player) continue;
         if (this.bots.contains(player.getUuid())) continue;
         
         int checks = 0;
         int totalChecks = 0;
         
         // Check 1: Not in tablist
         if (tablistCheck.isEnabled()) {
            totalChecks++;
            if (!isInTablist(player)) {
               checks++;
            }
         }
         
         // Check 2: Suspicious armor
         if (armorCheck.isEnabled()) {
            totalChecks++;
            if (hasSuspiciousArmor(player)) {
               checks++;
            }
         }
         
         // Check 3: Suspicious name (too short, random chars, NPC-like)
         if (nameCheck.isEnabled()) {
            totalChecks++;
            if (hasSuspiciousName(player)) {
               checks++;
            }
         }
         
         // Check 4: Zero or negative ping
         if (pingCheck.isEnabled()) {
            totalChecks++;
            PlayerListEntry entry = mc.getNetworkHandler() != null 
               ? mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) : null;
            if (entry != null && entry.getLatency() <= 0) {
               checks++;
            }
         }
         
         // Check 5: Negative entity ID
         if (entityIdCheck.isEnabled()) {
            totalChecks++;
            if (player.getId() < 0) {
               checks++;
            }
         }
         
         // Check 6: Duplicate name in world
         if (duplicateCheck.isEnabled()) {
            totalChecks++;
            int nameCount = 0;
            for (PlayerEntity other : mc.world.getPlayers()) {
               if (other != null && other.getNameForScoreboard().equals(player.getNameForScoreboard())) {
                  nameCount++;
               }
            }
            if (nameCount > 1) {
               checks++;
            }
         }
         
         // If at least 2 checks triggered, or tablist check alone triggered, mark as bot
         if (checks >= 2 || (tablistCheck.isEnabled() && !isInTablist(player))) {
            this.bots.add(player.getUuid());
         }
      }
   }
   
   private boolean isInTablist(PlayerEntity player) {
      if (mc.getNetworkHandler() == null) return true;
      return mc.getNetworkHandler().getPlayerListEntry(player.getUuid()) != null;
   }

   private boolean hasSuspiciousArmor(PlayerEntity entity) {
      return (getArmor(entity, 3).getItem() == Items.LEATHER_HELMET && isNotColored(entity, 3) && !getArmor(entity, 3).hasEnchantments())
          || (getArmor(entity, 2).getItem() == Items.LEATHER_CHESTPLATE && isNotColored(entity, 2) && !getArmor(entity, 2).hasEnchantments())
          || (getArmor(entity, 1).getItem() == Items.LEATHER_LEGGINGS && isNotColored(entity, 1) && !getArmor(entity, 1).hasEnchantments())
          || (getArmor(entity, 0).getItem() == Items.LEATHER_BOOTS && isNotColored(entity, 0) && !getArmor(entity, 0).hasEnchantments())
          || (getArmor(entity, 2).getItem() == Items.IRON_CHESTPLATE && !getArmor(entity, 2).hasEnchantments())
          || (getArmor(entity, 1).getItem() == Items.IRON_LEGGINGS && !getArmor(entity, 1).hasEnchantments());
   }
   
   private boolean hasSuspiciousName(PlayerEntity player) {
      String name = player.getNameForScoreboard();
      if (name == null || name.isEmpty()) return true;
      if (name.length() <= 2) return true;
      
      // Check for excessive random characters (no vowels = likely random)
      String lower = name.toLowerCase();
      long vowelCount = lower.chars().filter(c -> "aeiou".indexOf(c) >= 0).count();
      if (name.length() > 5 && vowelCount == 0) return true;
      
      // NPC-style names
      if (name.startsWith("NPC") || name.startsWith("Bot") || name.startsWith("CIT-")) return true;
      
      return false;
   }

   private ItemStack getArmor(PlayerEntity entity, int slot) {
      return entity.getInventory().getArmorStack(slot);
   }

   private boolean isNotColored(PlayerEntity entity, int slot) {
      return !getArmor(entity, slot).contains(DataComponentTypes.DYED_COLOR);
   }

   public void onEnable() {
      super.onEnable();
      this.bots.clear();
   }

   public void onDisable() {
      super.onDisable();
      this.bots.clear();
   }

   public boolean isBot(PlayerEntity player) {
      return this.bots.contains(player.getUuid());
   }
}
