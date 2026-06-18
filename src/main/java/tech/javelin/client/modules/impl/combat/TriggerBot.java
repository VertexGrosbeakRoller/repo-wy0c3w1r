package tech.javelin.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.other.EventTick;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "TriggerBot",
   category = Category.COMBAT,
   description = "Авто-удар при наведении на цель (как Aura но только вручную)"
)
public final class TriggerBot extends Module {
   public static final TriggerBot INSTANCE = new TriggerBot();
   
   private final BooleanSetting players = new BooleanSetting("Игроки", true);
   private final BooleanSetting mobs = new BooleanSetting("Мобы", true);
   private final BooleanSetting critsOnly = new BooleanSetting("Только криты", false);
   private final NumberSetting range = new NumberSetting("Дистанция", 3.0F, 1.0F, 6.0F, 0.1F);
   
   private long lastAttack = 0;
   
   private TriggerBot() {}
   
   @EventTarget
   @Native
   public void onTick(EventTick event) {
      if (mc.player == null || mc.world == null || mc.interactionManager == null) return;
      
      // Проверяем что смотрим на сущность
      HitResult hit = mc.crosshairTarget;
      if (hit == null || hit.getType() != HitResult.Type.ENTITY) return;
      
      Entity target = ((EntityHitResult) hit).getEntity();
      if (!(target instanceof LivingEntity)) return;
      
      LivingEntity living = (LivingEntity) target;
      
      // Проверки
      if (!living.isAlive() || living == mc.player) return;
      if (mc.player.squaredDistanceTo(target) > range.getCurrent() * range.getCurrent()) return;
      
      // Тип цели
      if (target instanceof PlayerEntity) {
         if (!players.isEnabled()) return;
         String name = ((PlayerEntity) target).getName().getString();
         if (Javelin.getInstance().getFriendManager().isFriend(name)) return;
      } else if (target instanceof net.minecraft.entity.mob.HostileEntity) {
         if (!mobs.isEnabled()) return;
      } else {
         return; // Животные и прочее - нет
      }
      
      // Всегда уважаем кулдаун атаки
      if (mc.player.getAttackCooldownProgress(0.0F) < 0.95F) return;
      
      // Задержка между ударами
      long now = System.currentTimeMillis();
      if (now - lastAttack < 150) return;
      
      // Криты
      if (critsOnly.isEnabled()) {
         if (!canCrit()) return;
      }
      
      // Атака
      mc.interactionManager.attackEntity(mc.player, target);
      mc.player.swingHand(Hand.MAIN_HAND);
      lastAttack = now;
   }
   
   private boolean canCrit() {
      return !mc.player.isOnGround() 
         && mc.player.getVelocity().y < 0
         && !mc.player.isTouchingWater() 
         && !mc.player.isInLava()
         && !mc.player.hasVehicle();
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      lastAttack = 0;
   }
}
