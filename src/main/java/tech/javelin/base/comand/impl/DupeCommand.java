package tech.javelin.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.comand.api.CommandAbstract;
import tech.javelin.utility.game.other.MessageUtil;

public class DupeCommand extends CommandAbstract {
   public DupeCommand() {
      super("dupe");
   }

   @Native
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      builder.then(arg("item", StringArgumentType.word()).suggests((context, suggestionsBuilder) -> {
         // Подсказки по всем предметам
         Registries.ITEM.stream().forEach(item -> {
            Identifier id = Registries.ITEM.getId(item);
            if (id.toString().contains(suggestionsBuilder.getRemaining())) {
               suggestionsBuilder.suggest(id.toString());
            }
         });
         return suggestionsBuilder.buildFuture();
      }).then(arg("count", IntegerArgumentType.integer(1, 64)).executes((context) -> {
         String itemName = context.getArgument("item", String.class);
         int count = context.getArgument("count", Integer.class);

         try {
            Identifier id = Identifier.of(itemName);
            Item item = Registries.ITEM.get(id);
            
            if (item == null) {
               MessageUtil.displayError("Item not found: " + itemName);
               return 0;
            }

            ItemStack stack = new ItemStack(item, count);
            
            if (mc.player != null) {
               mc.player.getInventory().insertStack(stack);
               MessageUtil.displayInfo("Duplicated " + count + "x " + itemName);
            }
            return 1;
         } catch (Exception e) {
            MessageUtil.displayError("Invalid item: " + itemName);
            return 0;
         }
      })));
   }
}
