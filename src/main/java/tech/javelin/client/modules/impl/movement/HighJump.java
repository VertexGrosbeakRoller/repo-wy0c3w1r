package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "HighJump",
   category = Category.MOVEMENT,
   description = "Увеличивает высоту прыжка"
)
public final class HighJump extends Module {
   public static final HighJump INSTANCE = new HighJump();
   
   private final NumberSetting jumpHeight = new NumberSetting("Высота", 1.5F, 0.5F, 5.0F, 0.1F);
   
   private boolean wasOnGround = false;
   
   private HighJump() {}

   @EventTarget
   @Native
   public void onUpdate(EventUpdate event) {
      if (mc.player == null || mc.world == null) return;
      
      boolean onGround = mc.player.isOnGround();
      
      if (wasOnGround && !onGround && mc.player.getVelocity().y > 0) {
         double baseJumpVelocity = 0.42;
         double multiplier = jumpHeight.getCurrent();
         mc.player.setVelocity(
            mc.player.getVelocity().x,
            baseJumpVelocity * multiplier,
            mc.player.getVelocity().z
         );
      }
      
      wasOnGround = onGround;
   }
   
   @Override
   public void onEnable() {
      wasOnGround = false;
   }
}
