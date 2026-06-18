package tech.javelin.client.modules.impl.combat;

import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "SuperBow",
   category = Category.COMBAT,
   description = "Увеличивает скорость и урон стрел из лука"
)
public final class SuperBow extends Module {
   public static final SuperBow INSTANCE = new SuperBow();
   
   private final NumberSetting velocityMultiplier = new NumberSetting("Множитель скорости", 2.0F, 1.0F, 5.0F, 0.1F, "Насколько быстрее летит стрела");
   private final NumberSetting damageMultiplier = new NumberSetting("Множитель урона", 1.5F, 1.0F, 3.0F, 0.1F, "Насколько больше урона наносит стрела");
   private final BooleanSetting legit = new BooleanSetting("Легитный", "Меньше заметен для античитов", true);
   
   private SuperBow() {}
   
   public float getVelocityMultiplier() {
      if (!this.isEnabled()) return 1.0F;
      float multiplier = velocityMultiplier.getCurrent();
      if (legit.isEnabled()) {
         // В легитном режиме добавляем небольшую рандомизацию
         multiplier += (float)(Math.random() * 0.1 - 0.05);
      }
      return multiplier;
   }
   
   public float getDamageMultiplier() {
      if (!this.isEnabled()) return 1.0F;
      float multiplier = damageMultiplier.getCurrent();
      if (legit.isEnabled()) {
         // В легитном режиме меньший множитель урона
         multiplier = Math.min(multiplier, 1.5F);
      }
      return multiplier;
   }
   
   public boolean isLegit() {
      return legit.isEnabled();
   }
}
