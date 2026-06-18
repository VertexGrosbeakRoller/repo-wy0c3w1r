package tech.javelin.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.network.packet.s2c.play.GameMessageS2CPacket;
import net.minecraft.screen.slot.SlotActionType;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.other.EventTick;
import tech.javelin.base.events.impl.server.EventPacket;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@ModuleAnnotation(
        name = "AutoDuel",
        category = Category.MISC,
        description = "Автоматически кидает и принимает дуэль"
)
public final class AutoDuel extends Module {
    public static final AutoDuel INSTANCE = new AutoDuel();

    private final ModeSetting mode = new ModeSetting("Режим", "Шары",
            "Щит", "Шипы", "Лук", "Тотемы", "Нодебафф", "Шары", "Классик", "Читер", "Незер");

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");

    private final List<String> sent  = new ArrayList<>();
    private boolean inDuel           = false;
    private long    lastDuelTime     = 0;
    private long    lastClearTime    = 0;
    private long    lastPickTime     = 0;
    private long    lastSetTime      = 0;

    private AutoDuel() {}

    @Override
    public void onEnable() {
        sent.clear();
        inDuel = false;
        lastDuelTime  = System.currentTimeMillis();
        lastClearTime = System.currentTimeMillis();
        super.onEnable();
    }

    @Override
    public void onDisable() {
        sent.clear();
        inDuel = false;
        super.onDisable();
    }

    @EventTarget
    @Native
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null || inDuel) return;

        long now = System.currentTimeMillis();

        if (now - lastClearTime >= 30000) {
            sent.clear();
            lastClearTime = now;
        }

        if (now - lastDuelTime >= 1000) {
            sendDuel();
            lastDuelTime = now;
        }

        handleGui(now);
    }

    @EventTarget
    @Native
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;
        if (!e.isReceive()) return;
        if (!(e.getPacket() instanceof GameMessageS2CPacket p)) return;
        String msg = p.content().getString().toLowerCase();
        if (msg.contains("поединок начался") || msg.contains("начало") && msg.contains("через") ||
                msg.contains("во время поединка")) {
            inDuel = true;
            toggle();
        }
    }

    private void sendDuel() {
        for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
            String name = entry.getProfile().getName();
            if (!NAME_PATTERN.matcher(name).matches()) continue;
            if (name.equals(mc.player.getName().getString())) continue;
            if (sent.contains(name)) continue;
            mc.getNetworkHandler().sendChatCommand("duel " + name);
            sent.add(name);
            break;
        }
    }

    private void handleGui(long now) {
        if (!(mc.currentScreen instanceof GenericContainerScreen s)) return;
        int    id    = s.getScreenHandler().syncId;
        String title = s.getTitle().getString();

        if (title.contains("Выбор набора") && now - lastPickTime >= 150) {
            mc.interactionManager.clickSlot(id, getModeSlot(), 0, SlotActionType.QUICK_MOVE, mc.player);
            lastPickTime = now;
        } else if (title.contains("Настройка поединка") && now - lastSetTime >= 150) {
            mc.interactionManager.clickSlot(id, 0, 0, SlotActionType.QUICK_MOVE, mc.player);
            lastSetTime = now;
        }
    }

    private int getModeSlot() {
        if (mode.is("Щит"))     return 0;
        if (mode.is("Шипы"))    return 1;
        if (mode.is("Лук"))     return 2;
        if (mode.is("Тотемы"))  return 3;
        if (mode.is("Нодебафф")) return 4;
        if (mode.is("Шары"))    return 5;
        if (mode.is("Классик")) return 6;
        if (mode.is("Читер"))   return 7;
        if (mode.is("Незер"))   return 8;
        return 5;
    }
}
