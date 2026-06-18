package tech.javelin.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.option.KeyBinding;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.game.other.MessageUtil;

@ModuleAnnotation(
   name = "TapeMouse",
   description = "Автоматически кликает с заданной задержкой",
   category = Category.PLAYER
)
public final class TapeMouse extends Module {
   private final NumberSetting delaySetting = new NumberSetting("Задержка", 100, 10, 1000, 10);
   private final BooleanSetting leftClickSetting = new BooleanSetting("ЛКМ", true);
   private final BooleanSetting rightClickSetting = new BooleanSetting("ПКМ", false);
   
   public static final TapeMouse INSTANCE = new TapeMouse();
   
   private long lastLeftClick = 0;
   private long lastRightClick = 0;

   private TapeMouse() {}

   @EventTarget
   @Native
   public void onUpdate(EventUpdate e) {
      if (mc.player == null) return;
      
      long currentTime = System.currentTimeMillis();
      long delay = (long) delaySetting.getCurrent();
      
      // ЛКМ
      if (leftClickSetting.isEnabled() && currentTime - lastLeftClick >= delay) {
         KeyBinding.setKeyPressed(mc.options.attackKey.getDefaultKey(), true);
         KeyBinding.onKeyPressed(mc.options.attackKey.getDefaultKey());
         lastLeftClick = currentTime;
      }
      
      // ПКМ
      if (rightClickSetting.isEnabled() && currentTime - lastRightClick >= delay) {
         KeyBinding.setKeyPressed(mc.options.useKey.getDefaultKey(), true);
         KeyBinding.onKeyPressed(mc.options.useKey.getDefaultKey());
         lastRightClick = currentTime;
      }
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      // Сбрасываем клавиши при выключении
      if (mc.player != null) {
         KeyBinding.setKeyPressed(mc.options.attackKey.getDefaultKey(), false);
         KeyBinding.setKeyPressed(mc.options.useKey.getDefaultKey(), false);
      }
   }
}
