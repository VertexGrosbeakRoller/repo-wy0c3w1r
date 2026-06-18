package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.render.EventRender3D;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.render.display.base.color.ColorRGBA;

@ModuleAnnotation(
   name = "Hands",
   category = Category.RENDER,
   description = "Эффекты на руках (статик/волна/обводка)"
)
public final class Hands extends Module {
   public static final Hands INSTANCE = new Hands();
   
   private final ModeSetting mode = new ModeSetting("Режим", "Статик", "Статик", "Волна", "Обводка");
   private final NumberSetting speed = new NumberSetting("Скорость", 1.0F, 0.1F, 5.0F, 0.1F);
   private final BooleanSetting useThemeColor = new BooleanSetting("Цвет темы", "Использовать цвет темы клиента", true);
   
   private Hands() {}
   
   public boolean isStaticMode() { return mode.is("Статик"); }
   public boolean isWaveMode() { return mode.is("Волна"); }
   public boolean isOutlineMode() { return mode.is("Обводка"); }
   
   public ColorRGBA getHandColor() {
      if (useThemeColor.isEnabled()) {
         Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
         return theme.getColor();
      }
      return new ColorRGBA(255, 255, 255, 200);
   }
   
   public float getColorPhase() {
      return (float)((System.currentTimeMillis() % 5000L) / 5000.0 * speed.getCurrent());
   }
   
   public int getWaveColor(float offset) {
      float phase = getColorPhase() + offset;
      ColorRGBA base = getHandColor();
      float r = base.getRed() / 255.0f;
      float g = base.getGreen() / 255.0f;
      float b = base.getBlue() / 255.0f;
      
      float wave = (float)(Math.sin(phase * Math.PI * 2) * 0.5 + 0.5);
      r = r * wave + (1 - r) * (1 - wave) * 0.3f;
      g = g * wave + (1 - g) * (1 - wave) * 0.3f;
      b = b * wave + (1 - b) * (1 - wave) * 0.3f;
      
      return new ColorRGBA(
         (int)(r * 255), (int)(g * 255), (int)(b * 255), 180
      ).getRGB();
   }
}
