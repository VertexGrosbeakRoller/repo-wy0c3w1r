package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "HighJump",
   category = Category.MOVEMENT,
   description = "Усиливает подъём от левитации шалкера"
)
public final class HighJump extends Module {
   public static final HighJump INSTANCE = new HighJump();
   
   private final NumberSetting jumpHeight = new NumberSetting("Множитель", 2.0F, 1.0F, 5.0F, 0.1F);
   
   private boolean hadLevitation = false;
   
   private HighJump() {}

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null) return;
      
      boolean hasLevitation = mc.player.hasStatusEffect(StatusEffects.LEVITATION);
      
      if (hasLevitation) {
         Vec3d vel = mc.player.getVelocity();
         if (vel.y > 0) {
            double multiplier = jumpHeight.getCurrent();
            mc.player.setVelocity(vel.x, vel.y * multiplier, vel.z);
         }
         hadLevitation = true;
      } else if (hadLevitation) {
         hadLevitation = false;
      }
   }
   
   @Override
   public void onEnable() {
      hadLevitation = false;
   }
}
