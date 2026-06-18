package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import com.darkmagician6.eventapi.events.Event;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector3d;
import tech.javelin.base.events.impl.player.EventMotion;
import tech.javelin.base.events.impl.player.EventMoveInput;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.utility.math.MathUtil;
import tech.javelin.utility.math.StopWatch;

import java.util.concurrent.ThreadLocalRandom;

@ModuleAnnotation(
        name = "GrimGlide",
        category = Category.MOVEMENT,
        description = "Ускорение на элитре без фейерверков (Grim Bypass)"
)
public class GrimGlide extends Module {
    private final StopWatch ticks = new StopWatch();
    private int ticksTwo = 0;

    public static GrimGlide INSTANCE = new GrimGlide();

    private GrimGlide() {
    }

    @EventTarget
    private void onEvent(EventMoveInput event) {
        if (mc.player == null || mc.world == null || !mc.player.isGliding()) return;

        ++ticksTwo;
        Vec3d pos = mc.player.getPos();
        float yaw = mc.player.getYaw();
        double forward = mc.player.age % 2 == 0 ? 0.087D : 0.09D;
        double dx = -Math.sin(Math.toRadians(yaw)) * forward;
        double dz = Math.cos(Math.toRadians(yaw)) * forward;

        mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
        ticks.reset();

        if (ticks.getElapsedTime() >= 40) {
            double yVel = mc.player.getVelocity().y + 0.00600000075995922D;
            mc.player.setVelocity(
                    dx * (double) ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F),
                    yVel,
                    dz * (double) ThreadLocalRandom.current().nextFloat(1.001F, 1.0021F)
            );
        }
    }

    @Override
    public void onDisable() {
        ticks.reset();
        ticksTwo = 0;
        super.onDisable();
    }
}