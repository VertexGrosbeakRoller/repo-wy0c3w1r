package tech.javelin.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.other.EventTick;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
        name = "DeathCoord",
        category = Category.MISC,
        description = "Показывает координаты смерти"
)
public final class DeathCoord extends Module {
    public static final DeathCoord INSTANCE = new DeathCoord();

    private final BooleanSetting showInChat = new BooleanSetting("Показывать в чате", true);

    private Vec3d deathPos = null;
    private String deathDimension = null;
    private boolean wasDead = false;

    private DeathCoord() {}

    @EventTarget
    @Native
    public void onTick(EventTick e) {
        if (mc.player == null || mc.world == null) return;

        boolean isDead = mc.player.isDead() || mc.player.getHealth() <= 0.0F;

        if (isDead && !wasDead) {
            deathPos = mc.player.getPos();
            deathDimension = mc.world.getRegistryKey().getValue().toString();
            if (showInChat.isEnabled() && mc.player.networkHandler != null) {
                String msg = String.format("§c[DeathCoord]§r Смерть: X=%.1f Y=%.1f Z=%.1f (§e%s§r)",
                        deathPos.x, deathPos.y, deathPos.z,
                        formatDimension(deathDimension));
                mc.inGameHud.getChatHud().addMessage(net.minecraft.text.Text.literal(msg));
            }
        }

        wasDead = isDead;
    }

    private String formatDimension(String dim) {
        if (dim == null) return "?";
        if (dim.contains("overworld")) return "Мир";
        if (dim.contains("nether")) return "Ад";
        if (dim.contains("end")) return "Край";
        return dim;
    }

    public Vec3d getDeathPos() { return deathPos; }
    public String getDeathDimension() { return deathDimension; }

    @Override
    public void onDisable() {
        wasDead = false;
        super.onDisable();
    }
}
