package tech.javelin.client.hud.elements.component;

import java.util.Iterator;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.setting.Setting;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.utility.render.display.Keyboard;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

public class KeybindsComponent extends DraggableHudElement {
   private final Animation widthAnimation;
   private final Animation xLine;
   private final Animation alpha;

   private static final float BLUR_STRENGTH = 15.0f;
   private static final float CORNER_RADIUS = 2.25f;

   public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.xLine = new Animation(170L, Easing.SINE_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
   }

   private void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {

      DrawUtil.drawBlur(
              ctx.getMatrices(), x, y, width, height,
              BLUR_STRENGTH,
              BorderRadius.all(CORNER_RADIUS),
              new ColorRGBA(255, 255, 255, (int)(animation * 255))
      );

      ColorRGBA themeColor = theme.getColor();
      ColorRGBA backgroundColor = new ColorRGBA(
              (int) (Math.min(255, Math.max(0, themeColor.getRed() * 0.15f))),
              (int) (Math.min(255, Math.max(0, themeColor.getGreen() * 0.15f))),
              (int) (Math.min(255, Math.max(0, themeColor.getBlue() * 0.15f))),
              (int)(64 * animation)
      );

      DrawUtil.drawRoundedRect(
              ctx.getMatrices(), x, y, width, height,
              BorderRadius.all(CORNER_RADIUS),
              backgroundColor
      );
   }

   @Native
   public void render(CustomDrawContext ctx) {
      float posX = this.getX();
      float posY = this.getY();
      float defaultWidth = 53.0F;
      float height = 14.5F;
      boolean isFound = false;
      Iterator var7 = Javelin.getInstance().getModuleManager().getModules().iterator();

      while(var7.hasNext()) {
         Module module = (Module)var7.next();
         if (module.getName().equals("Menu") || module.getName().equals("DropDown")) {
            continue;
         }
         if (module.isEnabled() && module.getKeyCode() != -1) {
            this.alpha.update(1.0F);
            isFound = true;
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
               BooleanSetting boolSetting = (BooleanSetting) setting;
               if (boolSetting.isEnabled() && boolSetting.getKeyCode() != -1) {
                  this.alpha.update(1.0F);
                  isFound = true;
               }
            }
         }
      }

      if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
         this.alpha.update(0.0F);
      }

      if (mc.currentScreen instanceof ChatScreen) {
         this.alpha.update(1.0F);
      }

      if (this.alpha.getValue() < 0.01F) {
         this.width = 0.0F;
         this.height = 0.0F;
         return;
      }

      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();

      drawBlurBackground(ctx, posX, posY, this.widthAnimation.getValue(), 14.5F, theme, this.alpha.getValue());

      ctx.drawText(Fonts.ICONS.getFont(10.0F), "C", posX + 4.25F, posY + 5.5F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));

      ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), " |", posX + 15.0F, posY + 4.75F, new ColorRGBA(166, 166, 166, 255.0F * this.alpha.getValue()));

      ctx.drawText(Fonts.SEMIBOLD.getFont(7.5F), "Hotkeys", posX + 20.5F, posY + 4.75F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));

      posY += 14.5F + 1.0F; // Добавлен отступ после заголовка
      float bindWidth = 0.0F;
      Iterator var9 = Javelin.getInstance().getModuleManager().getModules().iterator();

      Module module;
      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getName().equals("Menu") || module.getName().equals("DropDown")) {
            continue;
         }
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            float localBindWidth = Fonts.SEMIBOLD.getWidth(Keyboard.getKeyName(module.getKeyCode()), 6.75F);
            if (localBindWidth > bindWidth) {
               bindWidth = localBindWidth;
            }
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
               BooleanSetting boolSetting = (BooleanSetting) setting;
               if (boolSetting.getAnimation().getValue() != 0.0F && boolSetting.getKeyCode() != -1) {
                  float localBindWidth = Fonts.SEMIBOLD.getWidth(Keyboard.getKeyName(boolSetting.getKeyCode()), 6.75F);
                  if (localBindWidth > bindWidth) {
                     bindWidth = localBindWidth;
                  }
               }
            }
         }
      }

      this.xLine.update(bindWidth + 10.0F);
      var9 = Javelin.getInstance().getModuleManager().getModules().iterator();

      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getName().equals("Menu") || module.getName().equals("DropDown")) {
            continue;
         }
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            height += 11.0F + 1.0F; // Добавлен отступ 1.5F
            String bind = Keyboard.getKeyName(module.getKeyCode());
            String moduleName = module.getName();
            float elementsWidth = Fonts.SEMIBOLD.getWidth(moduleName, 7.0F) + Fonts.SEMIBOLD.getWidth(bind, 6.75F) + 50.0F;

            float elementAlpha = module.getAnimation().getValue() * this.alpha.getValue();
            float elementY = posY + module.getAnimation().getValue() * 3.0F - 3.0F;

            drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

            float separatorX = posX + this.widthAnimation.getValue() - 6.0F - this.xLine.getValue();
            ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), "|", separatorX, elementY + 3.25F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));

            ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), moduleName, posX + 5.0F, elementY + 3.25F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

            ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), bind, posX + this.widthAnimation.getValue() - 3.0F - this.xLine.getValue() / 2.0F - Fonts.SEMIBOLD.getWidth(bind, 6.75F) / 2.0F, elementY + 3.25F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

            if (elementsWidth > defaultWidth) {
               defaultWidth = elementsWidth;
            }

            posY += (11.0F + 1.0F) * module.getAnimation().getValue(); // Добавлен отступ 1.5F
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
               BooleanSetting boolSetting = (BooleanSetting) setting;
               if (boolSetting.getAnimation().getValue() != 0.0F && boolSetting.getKeyCode() != -1) {
                  height += 11.0F + 1.0F; // Добавлен отступ 1.5F
                  String bind = Keyboard.getKeyName(boolSetting.getKeyCode());
                  String settingName = boolSetting.getName();
                  float elementsWidth = Fonts.SEMIBOLD.getWidth(settingName, 7.0F) + Fonts.SEMIBOLD.getWidth(bind, 6.75F) + 50.0F;

                  float elementAlpha = boolSetting.getAnimation().getValue() * this.alpha.getValue();
                  float elementY = posY + boolSetting.getAnimation().getValue() * 3.0F - 3.0F;

                  drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

                  float separatorX = posX + this.widthAnimation.getValue() - 6.0F - this.xLine.getValue();
                  ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), "|", separatorX, elementY + 3.25F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));

                  ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), settingName, posX + 5.0F, elementY + 3.25F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

                  ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), bind, posX + this.widthAnimation.getValue() - 3.0F - this.xLine.getValue() / 2.0F - Fonts.SEMIBOLD.getWidth(bind, 6.75F) / 2.0F, elementY + 3.25F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

                  if (elementsWidth > defaultWidth) {
                     defaultWidth = elementsWidth;
                  }

                  posY += (11.0F + 1.0F) * boolSetting.getAnimation().getValue(); // Добавлен отступ 1.5F
               }
            }
         }
      }

      this.widthAnimation.update(defaultWidth);
      this.width = this.widthAnimation.getValue();
      this.height = height;
   }
}
