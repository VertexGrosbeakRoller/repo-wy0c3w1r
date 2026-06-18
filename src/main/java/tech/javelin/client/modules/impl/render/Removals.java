package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import java.util.List;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.render.EventCamera;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.MultiBooleanSetting;

@ModuleAnnotation(
   name = "Removals",
   category = Category.RENDER,
   description = "Убирает лишние элементы с экрана"
)
public final class Removals extends Module {
   public static final Removals INSTANCE = new Removals();
   
   // Основные настройки (19/24 с фото)
   private final MultiBooleanSetting removeSettings = MultiBooleanSetting.create("Применять на", List.of(
       "Тряска камеры",
       "Скорборд", 
       "Удочка на экране",
       "Босс-бар",
       "Частицы разрушения",
       "Дождь",
       "Камера клип",
       "Тени",
       "Дым",
       "Снесение тотема",
       "Виньетка",
       "Стрелы в игроке",
       "Голограммы",
       "Эффект здоровья",
       "Трава",
       "Плохие эффекты",
       "Свечение игроков",
       "Игроки",
       "Размытие под водой",
       "Лава",
       "Огонь",
       "Тайтлы",
       "Взрыв кристалла",
       "Локи"
   ));
   
   // Уменьшить звук (0/5 с фото)
   private final MultiBooleanSetting soundSettings = MultiBooleanSetting.create("Уменьшить звук", List.of(
       "Трезубец",
       "Появление визера",
       "Открытие энд-портала",
       "Музыкальные пластинки",
       "Битве пузырьков опыта"
   ));

   @Native
   public boolean isRemoveFire() {
      return this.isEnabled() && this.removeSettings.isEnable("Огонь");
   }

   @Native
   public boolean isRemoveBadEffect() {
      return this.isEnabled() && this.removeSettings.isEnable("Плохие эффекты");
   }
   
   @Native
   public boolean isRemoveCameraShake() {
      return this.isEnabled() && this.removeSettings.isEnable("Тряска камеры");
   }
   
   @Native
   public boolean isRemoveScoreboard() {
      return this.isEnabled() && this.removeSettings.isEnable("Скорборд");
   }
   
   @Native
   public boolean isRemoveFishingRod() {
      return this.isEnabled() && this.removeSettings.isEnable("Удочка на экране");
   }
   
   @Native
   public boolean isRemoveBossBar() {
      return this.isEnabled() && this.removeSettings.isEnable("Босс-бар");
   }
   
   @Native
   public boolean isRemoveDestructionParticles() {
      return this.isEnabled() && this.removeSettings.isEnable("Частицы разрушения");
   }
   
   @Native
   public boolean isRemoveRain() {
      return this.isEnabled() && this.removeSettings.isEnable("Дождь");
   }
   
   @Native
   public boolean isRemoveShadows() {
      return this.isEnabled() && this.removeSettings.isEnable("Тени");
   }
   
   @Native
   public boolean isRemoveSmoke() {
      return this.isEnabled() && this.removeSettings.isEnable("Дым");
   }
   
   @Native
   public boolean isRemoveTotemPop() {
      return this.isEnabled() && this.removeSettings.isEnable("Снесение тотема");
   }
   
   @Native
   public boolean isRemoveVignette() {
      return this.isEnabled() && this.removeSettings.isEnable("Виньетка");
   }
   
   @Native
   public boolean isRemoveArrowsInPlayer() {
      return this.isEnabled() && this.removeSettings.isEnable("Стрелы в игроке");
   }
   
   @Native
   public boolean isRemoveHolograms() {
      return this.isEnabled() && this.removeSettings.isEnable("Голограммы");
   }
   
   @Native
   public boolean isRemoveHealthEffect() {
      return this.isEnabled() && this.removeSettings.isEnable("Эффект здоровья");
   }
   
   @Native
   public boolean isRemoveGrass() {
      return this.isEnabled() && this.removeSettings.isEnable("Трава");
   }
   
   @Native
   public boolean isRemovePlayerGlow() {
      return this.isEnabled() && this.removeSettings.isEnable("Свечение игроков");
   }
   
   @Native
   public boolean isRemovePlayers() {
      return this.isEnabled() && this.removeSettings.isEnable("Игроки");
   }
   
   @Native
   public boolean isRemoveUnderwaterBlur() {
      return this.isEnabled() && this.removeSettings.isEnable("Размытие под водой");
   }
   
   @Native
   public boolean isRemoveLava() {
      return this.isEnabled() && this.removeSettings.isEnable("Лава");
   }
   
   @Native
   public boolean isRemoveTitles() {
      return this.isEnabled() && this.removeSettings.isEnable("Тайтлы");
   }
   
   @Native
   public boolean isRemoveCrystalExplosion() {
      return this.isEnabled() && this.removeSettings.isEnable("Взрыв кристалла");
   }
   
   @Native
   public boolean isRemoveLocks() {
      return this.isEnabled() && this.removeSettings.isEnable("Локи");
   }
   
   // Sound settings
   @Native
   public boolean isReduceTridentSound() {
      return this.isEnabled() && this.soundSettings.isEnable("Трезубец");
   }
   
   @Native
   public boolean isReduceWitherSpawnSound() {
      return this.isEnabled() && this.soundSettings.isEnable("Появление визера");
   }
   
   @Native
   public boolean isReduceEndPortalSound() {
      return this.isEnabled() && this.soundSettings.isEnable("Открытие энд-портала");
   }
   
   @Native
   public boolean isReduceMusicDiscSound() {
      return this.isEnabled() && this.soundSettings.isEnable("Музыкальные пластинки");
   }
   
   @Native
   public boolean isReduceXPSound() {
      return this.isEnabled() && this.soundSettings.isEnable("Битве пузырьков опыта");
   }

   @EventTarget
   @Native
   private void onCamera(EventCamera e) {
      e.setCameraClip(this.removeSettings.isEnable("Камера клип"));
      e.cancel();
   }
}
