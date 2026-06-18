package tech.javelin.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.EntityType;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.input.EventKey;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;

import java.util.UUID;

@ModuleAnnotation(
        name = "FakePlayer",
        category = Category.MISC,
        description = "Спавнит клона для тренировки"
)
public final class FakePlayer extends Module {
    public static final FakePlayer INSTANCE = new FakePlayer();

    private final BooleanSetting copyHealth = new BooleanSetting("Копировать здоровье", true);

    private AbstractClientPlayerEntity fakeEntity = null;

    private FakePlayer() {}

    @Override
    public void onDisable() {
        removeFake();
        super.onDisable();
    }

    @EventTarget
    @Native
    public void onKey(EventKey event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getKeyCode() != this.getKeyCode() || event.getAction() != 1) return;
        if (fakeEntity == null) spawnFake();
        else removeFake();
    }

    @Override
    public void onEnable() {
        if (mc.player != null && mc.world != null) spawnFake();
        super.onEnable();
    }

    private void spawnFake() {
        if (mc.world == null || mc.player == null) return;
        removeFake();

        ClientWorld world = mc.world;
        UUID uuid = UUID.randomUUID();
        GameProfile profile = new GameProfile(uuid, "FakePlayer");

        fakeEntity = new AbstractClientPlayerEntity(world, profile) {};
        Vec3d pos = mc.player.getPos();
        fakeEntity.setPosition(pos.x + 1.0, pos.y, pos.z);
        fakeEntity.setYaw(mc.player.getYaw());
        fakeEntity.setPitch(mc.player.getPitch());

        if (copyHealth.isEnabled()) {
            fakeEntity.setHealth(mc.player.getHealth());
        } else {
            fakeEntity.setHealth(20.0F);
        }

        world.addEntity(fakeEntity);
    }

    private void removeFake() {
        if (fakeEntity != null && mc.world != null) {
            mc.world.removeEntity(fakeEntity.getId(), net.minecraft.entity.Entity.RemovalReason.DISCARDED);
            fakeEntity = null;
        }
    }
}
