package tech.javelin.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.input.EventKey;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;

@ModuleAnnotation(
        name = "CommandBind",
        category = Category.MISC,
        description = "Биндит команды/текст на клавиши. Используй .commandbind <команда> <клавиша>"
)
public final class CommandBind extends Module {
    public static final CommandBind INSTANCE = new CommandBind();

    private final List<Entry> binds = new ArrayList<>();

    private CommandBind() {}

    public void addBind(String command, int keyCode) {
        binds.removeIf(e -> e.keyCode == keyCode);
        binds.add(new Entry(command, keyCode));
        if (mc.player != null) {
            mc.inGameHud.getChatHud().addMessage(
                net.minecraft.text.Text.literal("§a[CommandBind]§r Забиндено: §e" + command + " §7-> §bклавиша " + keyCode));
        }
    }

    public void removeBind(int keyCode) {
        binds.removeIf(e -> e.keyCode == keyCode);
    }

    public List<Entry> getBinds() {
        return binds;
    }

    @EventTarget
    @Native
    public void onKey(EventKey event) {
        if (mc.player == null || mc.currentScreen != null) return;
        if (event.getAction() != 1) return;
        for (Entry entry : binds) {
            if (entry.keyCode == event.getKeyCode()) {
                String cmd = entry.command;
                if (cmd.startsWith("/")) {
                    mc.player.networkHandler.sendChatCommand(cmd.substring(1));
                } else {
                    mc.player.networkHandler.sendChatMessage(cmd);
                }
            }
        }
    }

    public static class Entry {
        public final String command;
        public final int keyCode;
        Entry(String command, int keyCode) {
            this.command = command;
            this.keyCode = keyCode;
        }
    }
}
