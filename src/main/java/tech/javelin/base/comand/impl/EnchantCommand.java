package tech.javelin.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.comand.api.CommandAbstract;
import tech.javelin.utility.game.other.MessageUtil;

public class EnchantCommand extends CommandAbstract {
   public EnchantCommand() {
      super("enchant");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(arg("enchantment", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
         // Подсказки по зачарованиям
         if (mc.world != null) {
            mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT).ifPresent(registry -> {
               registry.streamEntries().forEach(entry -> {
                  Identifier id = entry.registryKey().getValue();
                  if (id.toString().contains(suggestionsBuilder.getRemaining())) {
                     suggestionsBuilder.suggest(id.toString());
                  }
               });
            });
         }
         return suggestionsBuilder.buildFuture();
      }).then(arg("level", IntegerArgumentType.integer(1, 1000)).suggests((context, suggestionsBuilder) -> {
         // Подсказки уровней
         suggestionsBuilder.suggest("1");
         suggestionsBuilder.suggest("5");
         suggestionsBuilder.suggest("10");
         suggestionsBuilder.suggest("100");
         suggestionsBuilder.suggest("1000");
         return suggestionsBuilder.buildFuture();
      }).executes((context) -> {
         String enchantName = context.getArgument("enchantment", String.class);
         int level = context.getArgument("level", Integer.class);

         try {
            Identifier id = Identifier.of(enchantName);
            RegistryEntry<Enchantment> enchantmentEntry = null;
            if (mc.world != null) {
               enchantmentEntry = mc.world.getRegistryManager().getOptional(RegistryKeys.ENCHANTMENT)
                  .flatMap(registry -> registry.getEntry(id))
                  .orElse(null);
            }
            
            if (enchantmentEntry == null) {
               MessageUtil.displayError("Enchantment not found: " + enchantName);
               return 0;
            }

            if (mc.player == null) {
               MessageUtil.displayError("Player not found");
               return 0;
            }

            ItemStack mainHand = mc.player.getMainHandStack();
            if (mainHand.isEmpty()) {
               MessageUtil.displayError("Hold an item in main hand");
               return 0;
            }

            // Добавляем зачарование
            mainHand.addEnchantment(enchantmentEntry, level);
            MessageUtil.displayInfo("Enchanted " + mainHand.getItem().getName().getString() + " with " + enchantName + " " + level);
            return 1;
         } catch (Exception e) {
            MessageUtil.displayError("Invalid enchantment: " + enchantName);
            return 0;
         }
      })));
   }
}
