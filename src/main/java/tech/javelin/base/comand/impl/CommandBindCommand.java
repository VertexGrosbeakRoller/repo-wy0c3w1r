package tech.javelin.base.comand.impl;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.command.CommandSource;
import net.minecraft.util.Formatting;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.comand.api.CommandAbstract;
import tech.javelin.client.modules.impl.misc.CommandBind;
import tech.javelin.utility.game.other.MessageUtil;

public class CommandBindCommand extends CommandAbstract {
    public CommandBindCommand() {
        super("commandbind");
    }

    @Native
    public void execute(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(
            literal("add")
                .then(arg("command", StringArgumentType.greedyString())
                    .then(arg("key", IntegerArgumentType.integer(0))
                        .executes(ctx -> {
                            String command = StringArgumentType.getString(ctx, "command");
                            int key = IntegerArgumentType.getInteger(ctx, "key");
                            CommandBind.INSTANCE.addBind(command, key);
                            MessageUtil.displayInfo(Formatting.GRAY + "Добавлен биндинг: " + Formatting.WHITE + command + Formatting.GRAY + " -> клавиша " + Formatting.WHITE + key);
                            return 1;
                        })
                    )
                )
        );
        builder.then(
            literal("remove")
                .then(arg("key", IntegerArgumentType.integer(0))
                    .executes(ctx -> {
                        int key = IntegerArgumentType.getInteger(ctx, "key");
                        CommandBind.INSTANCE.removeBind(key);
                        MessageUtil.displayInfo(Formatting.GRAY + "Биндинг на клавишу " + Formatting.WHITE + key + Formatting.GRAY + " удалён");
                        return 1;
                    })
                )
        );
        builder.then(
            literal("list")
                .executes(ctx -> {
                    if (CommandBind.INSTANCE.getBinds().isEmpty()) {
                        MessageUtil.displayInfo(Formatting.GRAY + "Биндингов нет");
                    } else {
                        CommandBind.INSTANCE.getBinds().forEach(e ->
                            MessageUtil.displayInfo(Formatting.GRAY + "Клавиша " + Formatting.WHITE + e.keyCode + Formatting.GRAY + " -> " + Formatting.WHITE + e.command)
                        );
                    }
                    return 1;
                })
        );
    }
}
