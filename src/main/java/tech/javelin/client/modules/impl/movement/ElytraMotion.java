package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.client.modules.impl.combat.Aura;

@ModuleAnnotation(
   name = "ElytraMotion",
   category = Category.MOVEMENT,
   description = "Авто-фейерверк для элитр с бустом при застревании"
)
public class ElytraMotion extends Module {
   public static final ElytraMotion INSTANCE = new ElytraMotion();
   
   private final NumberSetting boostDelay = new NumberSetting("Задержка буста", 15, 5, 40, 1, "Тики между использованиями фейерверка");
   private final BooleanSetting autoEquip = new BooleanSetting("Авто-взять фейерверк", "Автоматически переключаться на слот с фейерверком", true);
   private final BooleanSetting autoStart = new BooleanSetting("Авто-старт", "Автоматически открывать элитру при падении", true);
   private final NumberSetting minFallDistance = new NumberSetting("Мин. высота падения", 4.0F, 2.0F, 10.0F, 0.5F, "Минимальная высота для открытия элитры");
   
   private int fireworkTick = 0;
   private int lastFireworkSlot = -1;
   private int originalSlot = -1;
   private boolean needsSwitchBack = false;
   private int glideTick = 0;
   private long lastFireworkTime = 0;
   private boolean wasInAir = false;
   private double lastY = 0;
   
   // Stuck detection
   private double lastPosX = 0, lastPosY = 0, lastPosZ = 0;
   private int stuckCheckTicks = 0;
   private int stuckCount = 0;

   private ElytraMotion() {}

   @EventTarget
   @Native
   public void onTick(EventUpdate e) {
      if (mc.player == null || mc.world == null) {
         resetState();
         return;
      }
      
      // Проверяем надет ли элитра
      if (!isWearingElytra()) {
         resetState();
         return;
      }
      
      // Обрабатываем авто-старт элитры
      if (autoStart.isEnabled()) {
         handleAutoStart();
      }
      
      // Проверяем летим ли мы
      boolean isGliding = mc.player.isGliding();
      if (isGliding) {
         glideTick++;
      } else {
         glideTick = 0;
         return; // Не летим - не используем фейерверки
      }
      
      // Нужно минимум 3 тика чтобы стабилизироваться в полете
      if (glideTick < 3) {
         return;
      }
      
      // Проверяем нужен ли буст
      boolean needBoost = checkNeedBoost();
      
      if (needBoost) {
         int fireworkSlot = findFireworkSlot();
         if (fireworkSlot != -1) {
            useFirework(fireworkSlot);
         }
      }
   }
   
   private boolean checkNeedBoost() {
      fireworkTick++;
      if (fireworkTick < boostDelay.getCurrent()) {
         return false;
      }
      
      Vec3d velocity = mc.player.getVelocity();
      
      // Буст если падаем
      if (velocity.y < -0.3) {
         fireworkTick = 0;
         return true;
      }
      
      // Буст если есть цель и не приближаемся к ней (застряли)
      Entity auraTarget = Aura.INSTANCE.getTarget();
      if (auraTarget != null && auraTarget.isAlive()) {
         double dist = mc.player.distanceTo(auraTarget);
         
         stuckCheckTicks++;
         if (stuckCheckTicks >= 10) { // Проверка каждые 10 тиков (0.5 сек)
            stuckCheckTicks = 0;
            
            // Проверяем приблизились ли мы
            double moved = Math.abs(mc.player.getX() - lastPosX) + Math.abs(mc.player.getZ() - lastPosZ);
            
            if (moved < 0.5) { // Не двигаемся - застряли
               stuckCount++;
               if (stuckCount >= 2) { // Застряли 2 проверки подряд
                  stuckCount = 0;
                  fireworkTick = 0;
                  return true;
               }
            } else {
               stuckCount = 0;
            }
            
            lastPosX = mc.player.getX();
            lastPosY = mc.player.getY();
            lastPosZ = mc.player.getZ();
         }
      }
      
      return false;
   }
   
   private void handleAutoStart() {
      // Проверяем что игрок в воздухе и падает
      if (!mc.player.isOnGround() && mc.player.getVelocity().y < -0.1) {
         // Проверяем расстояние падения
         if (!wasInAir) {
            lastY = mc.player.getY();
            wasInAir = true;
         }
         
         double fallDistance = lastY - mc.player.getY();
         
         // Если падение достаточно большое - открываем элитру
         if (fallDistance >= minFallDistance.getCurrent() && !mc.player.isGliding()) {
            // Нажимаем пробел для открытия элитры
            if (mc.player.getInventory().getStack(38).getItem() == Items.ELYTRA) {
               // Имитируем прыжок для открытия элитры
               mc.player.networkHandler.sendPacket(
                  new net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket(
                     mc.player, net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode.START_FALL_FLYING
                  )
               );
            }
         }
      } else {
         wasInAir = false;
         lastY = mc.player.getY();
      }
   }
   
   private boolean isWearingElytra() {
      return mc.player.getInventory().getStack(38).getItem() == Items.ELYTRA;
   }
   
   private int findFireworkSlot() {
      // Сначала проверяем хотбар (0-8)
      for (int i = 0; i <= 8; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }
      // Потом проверяем инвентарь
      for (int i = 9; i < 36; i++) {
         if (mc.player.getInventory().getStack(i).getItem() == Items.FIREWORK_ROCKET) {
            return i;
         }
      }
      return -1;
   }
   
   private void useFirework(int slot) {
      // Если фейерверк уже в руке - просто используем
      if (mc.player.getInventory().selectedSlot == slot) {
         mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
         return;
      }
      
      // Если авто-экипировка включена - переключаемся
      if (autoEquip.isEnabled()) {
         // Проверяем что слот в хотбаре
         if (slot >= 0 && slot <= 8) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            if (originalSlot == -1) {
               originalSlot = currentSlot;
            }
            
            // Переключаемся на фейерверк
            mc.player.getInventory().selectedSlot = slot;
            
            // Используем
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            
            // Возвращаемся на исходный слот в следующем тике
            needsSwitchBack = true;
            lastFireworkSlot = slot;
         }
      }
   }
   
   @EventTarget
   @Native
   public void onPostTick(EventUpdate e) {
      // Возвращаемся на исходный слот
      if (needsSwitchBack && originalSlot != -1) {
         mc.player.getInventory().selectedSlot = originalSlot;
         needsSwitchBack = false;
         originalSlot = -1;
      }
   }
   
   private void resetState() {
      fireworkTick = 0;
      originalSlot = -1;
      needsSwitchBack = false;
      lastFireworkSlot = -1;
      glideTick = 0;
      lastFireworkTime = 0;
      wasInAir = false;
      lastY = 0;
      lastPosX = lastPosY = lastPosZ = 0;
      stuckCheckTicks = 0;
      stuckCount = 0;
   }

   @Override
   public void onDisable() {
      super.onDisable();
      resetState();
   }
}
