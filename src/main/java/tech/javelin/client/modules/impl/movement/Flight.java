package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;

@ModuleAnnotation(
        name = "Flight",
        category = Category.MOVEMENT,
        description = "Creative flight with adjustable speed"
)
public final class Flight extends Module {
    public static final Flight INSTANCE = new Flight();

    private final NumberSetting speed = new NumberSetting("Speed", 1.0F, 0.1F, 5.0F, 0.1F);
    private final BooleanSetting verticalBoost = new BooleanSetting("Vertical Boost", true);

    private Flight() {}

    @Override
    public void onEnable() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = true;
        }
        super.onEnable();
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.getAbilities().flying = false;
            mc.player.getAbilities().setFlySpeed(0.05F);
        }
        super.onDisable();
    }

    @EventTarget
    @Native
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        // Set creative flight speed
        float flySpeed = speed.getCurrent() * 0.05F;
        mc.player.getAbilities().setFlySpeed(flySpeed);
        mc.player.getAbilities().flying = true;

        // Handle vertical movement boost
        if (verticalBoost.isEnabled()) {
            Vec3d velocity = mc.player.getVelocity();
            double motionY = 0;

            if (mc.options.jumpKey.isPressed()) {
                motionY = speed.getCurrent() * 0.4;
            } else if (mc.options.sneakKey.isPressed()) {
                motionY = -speed.getCurrent() * 0.4;
            }

            if (motionY != 0) {
                mc.player.setVelocity(velocity.x, motionY, velocity.z);
            }
        }
    }
}
