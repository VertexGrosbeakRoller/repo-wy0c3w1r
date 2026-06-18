package tech.javelin.client.command.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import tech.javelin.base.comand.api.CommandAbstract;
import tech.javelin.client.modules.impl.combat.NeuroRotation;
import tech.javelin.utility.game.other.MessageUtil;

public class NeuroCommand extends CommandAbstract {
   public NeuroCommand() {
      super("neuro");
   }

   @Override
   public void execute(LiteralArgumentBuilder<CommandSource> builder) {
      // .neuro rec <name> - start recording
      builder.then(literal("rec").then(arg("name", StringArgumentType.word()).executes(context -> {
         String name = context.getArgument("name", String.class);
         NeuroRotation.INSTANCE.startRecording(name);
         return 1;
      })).executes(context -> {
         NeuroRotation.INSTANCE.startRecording("default");
         return 1;
      }));
      
      // .neuro stop - stop recording
      builder.then(literal("stop").executes(context -> {
         NeuroRotation.INSTANCE.stopRecording();
         return 1;
      }));
      
      // .neuro save <name> - save dataset
      builder.then(literal("save").then(arg("name", StringArgumentType.word()).executes(context -> {
         String name = context.getArgument("name", String.class);
         NeuroRotation.INSTANCE.saveDataset(name);
         return 1;
      })).executes(context -> {
         NeuroRotation.INSTANCE.saveDataset(null);
         return 1;
      }));
      
      // .neuro load <name> - load dataset
      builder.then(literal("load").then(arg("name", StringArgumentType.word()).executes(context -> {
         String name = context.getArgument("name", String.class);
         NeuroRotation.INSTANCE.loadDataset(name);
         return 1;
      })));
      
      // .neuro play <name> - play dataset
      builder.then(literal("play").then(arg("name", StringArgumentType.word()).executes(context -> {
         String name = context.getArgument("name", String.class);
         NeuroRotation.INSTANCE.play(name);
         return 1;
      })).executes(context -> {
         NeuroRotation.INSTANCE.play(null);
         return 1;
      }));
      
      // .neuro di/dir - list datasets (like .cfg di)
      builder.then(literal("di").executes(context -> {
         NeuroRotation.INSTANCE.listDatasets();
         return 1;
      }));
      builder.then(literal("dir").executes(context -> {
         NeuroRotation.INSTANCE.listDatasets();
         return 1;
      }));
      
      // .neuro del <name> - delete dataset
      builder.then(literal("del").then(arg("name", StringArgumentType.word()).executes(context -> {
         String name = context.getArgument("name", String.class);
         NeuroRotation.INSTANCE.deleteDataset(name);
         return 1;
      })));
      
      // .neuro status - show current status
      builder.then(literal("status").executes(context -> {
         String status = "§a[Neuro] §fСтатус:\n" +
            "§7Текущий: §f" + NeuroRotation.INSTANCE.getCurrentDataset() + "\n" +
            "§7Запись: §f" + (NeuroRotation.INSTANCE.isRecording() ? "§aДА" : "§7нет") + "\n" +
            "§7Воспроизведение: §f" + (NeuroRotation.INSTANCE.isPlaying() ? "§aДА" : "§7нет");
         MessageUtil.displayInfo(status);
         return 1;
      }));
      
      // Default - show help
      builder.executes(context -> {
         String help = "§a[Neuro] §fКоманды:\n" +
            "§7.neuro rec [name] §f- начать запись\n" +
            "§7.neuro stop §f- остановить запись\n" +
            "§7.neuro save [name] §f- сохранить\n" +
            "§7.neuro load <name> §f- загрузить\n" +
            "§7.neuro play [name] §f- воспроизвести\n" +
            "§7.neuro di §f- список датасетов\n" +
            "§7.neuro del <name> §f- удалить\n" +
            "§7.neuro status §f- статус";
         MessageUtil.displayInfo(help);
         return 1;
      });
   }
}
