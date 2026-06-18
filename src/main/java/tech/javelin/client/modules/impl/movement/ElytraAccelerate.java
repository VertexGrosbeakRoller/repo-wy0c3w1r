package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import java.util.concurrent.ThreadLocalRandom;
import net.minecraft.util.math.Vec3d;
import tech.javelin.base.events.impl.player.EventMotion;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.math.Timer;

@ModuleAnnotation(
   name = "GrimGlide",
   category = Category.MOVEMENT,
   description = "Ускорение на элитре без флагов"
)
public class ElytraAccelerate extends Module {
   public static final ElytraAccelerate INSTANCE = new ElytraAccelerate();

   private final NumberSetting maxSpeed  = new NumberSetting("Макс скорость", 48.0F, 10.0F, 120.0F, 1.0F);
   private final NumberSetting upSpeed   = new NumberSetting("Скорость вверх", 0.06F, 0.01F, 0.3F,  0.01F);
   private final NumberSetting downSpeed = new NumberSetting("Скорость вниз", 0.08F, 0.01F, 0.3F,  0.01F);

   private final Timer ticks = new Timer();
   private int ticksTwo = 0;

   @EventTarget
   public void onEvent(EventMotion event) {
      if (mc.player == null || mc.world == null || !mc.player.isGliding()) return;

      ++this.ticksTwo;
      Vec3d pos = mc.player.getPos();
      float yaw = mc.player.getYaw();

      double forward = 0.087D;
      double motion = Math.hypot(mc.player.prevX - mc.player.getX(), mc.player.prevZ - mc.player.getZ()) * 20.0D;

      float valuePidor = maxSpeed.getCurrent();
      if (motion >= (double) valuePidor) {
         forward = 0.0D;
      }

      double dx = -Math.sin(Math.toRadians((double) yaw)) * forward;
      double dz =  Math.cos(Math.toRadians((double) yaw)) * forward;

      double vy = mc.player.getVelocity().y - 0.019999999552965164D;

      if (mc.options.jumpKey.isPressed()) {
         vy = upSpeed.getCurrent();
      } else if (mc.options.sneakKey.isPressed()) {
         vy = -downSpeed.getCurrent();
      }

      float rMin = 1.1F, rMax = 1.21F;
      mc.player.setVelocity(
         dx * (double) ThreadLocalRandom.current().nextFloat(rMin, rMax),
         vy,
         dz * (double) ThreadLocalRandom.current().nextFloat(rMin, rMax)
      );

      if (this.ticks.finished(50L)) {
         mc.player.setPosition(pos.getX() + dx, pos.getY(), pos.getZ() + dz);
         this.ticks.reset();
      }

      mc.player.setVelocity(
         dx * (double) ThreadLocalRandom.current().nextFloat(rMin, rMax),
         mc.player.getVelocity().y + 0.01600000075995922D,
         dz * (double) ThreadLocalRandom.current().nextFloat(rMin, rMax)
      );
   }
}
