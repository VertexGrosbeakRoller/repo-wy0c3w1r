package tech.javelin.base.comand.impl;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.comand.api.CommandAbstract;
import tech.javelin.utility.game.other.MessageUtil;
import tech.javelin.utility.game.rotation.NeuroRotationSystem;

import java.util.List;

public class NeuroCommand extends CommandAbstract {
   
   public NeuroCommand() {
      super("neuro");
   }
   
   @Native
   @Override
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      // .neuro recode <name>
      builder.then(literal("recode").then(arg("name", StringArgumentType.word()).executes(context -> {
         String name = context.getArgument("name", String.class);
         NeuroRotationSystem.getInstance().startRecording(name);
         return 1;
      })));
      
      // .neuro stop
      builder.then(literal("stop").executes(context -> {
         NeuroRotationSystem.getInstance().stopRecording();
         return 1;
      }));
      
      // .neuro load <name>
      builder.then(literal("load").then(arg("name", StringArgumentType.word())
         .suggests((context, suggestionsBuilder) -> {
            List<String> datasets = NeuroRotationSystem.getInstance().getAvailableDatasets();
            for (String dataset : datasets) {
               suggestionsBuilder.suggest(dataset);
            }
            return suggestionsBuilder.buildFuture();
         })
         .executes(context -> {
            String name = context.getArgument("name", String.class);
            NeuroRotationSystem.getInstance().loadDataset(name);
            return 1;
         })
      ));
      
      // .neuro list
      builder.then(literal("list").executes(context -> {
         List<String> datasets = NeuroRotationSystem.getInstance().getAvailableDatasets();
         if (datasets.isEmpty()) {
            MessageUtil.displayInfo("§c[Neuro] §fНет сохраненных датасетов");
         } else {
            MessageUtil.displayInfo("§a[Neuro] §fДоступные датасеты:");
            for (String name : datasets) {
               MessageUtil.displayInfo("  §7- §e" + name);
            }
         }
         return 1;
      }));
      
      // .neuro status
      builder.then(literal("status").executes(context -> {
         NeuroRotationSystem system = NeuroRotationSystem.getInstance();
         if (system.isRecording()) {
            MessageUtil.displayInfo("§a[Neuro] §fИдет запись: §e" + system.getCurrentRecordingName());
         } else {
            MessageUtil.displayInfo("§a[Neuro] §fЗапись не активна");
         }
         return 1;
      }));
   }
}
