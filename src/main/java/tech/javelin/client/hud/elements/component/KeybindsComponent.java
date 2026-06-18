package tech.javelin.client.hud.elements.component;

import java.util.Iterator;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.util.Identifier;
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

   private static final float CORNER_RADIUS = 2.25f;
   private static final Identifier KEYBINDS_ICON = Identifier.of("javelin", "hudicons/keybinds.png");

   // User-configurable via Interface module settings
   private float blurStrength = 12.0f;
   private float bgOpacity = 0.85f;

   public KeybindsComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.xLine = new Animation(170L, Easing.SINE_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
   }

   public void setBlurStrength(float strength) { this.blurStrength = strength; }
   public void setBgOpacity(float opacity) { this.bgOpacity = opacity; }

   private void drawSolidBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {
      if (blurStrength > 0) {
         DrawUtil.drawBlur(
                 ctx.getMatrices(), x, y, width, height,
                 blurStrength,
                 BorderRadius.all(CORNER_RADIUS),
                 new ColorRGBA(255, 255, 255, (int)(animation * 255))
         );
      }

      // Solid dark background
      ColorRGBA backgroundColor = new ColorRGBA(15, 15, 15, (int)(bgOpacity * 255 * animation));

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

      drawSolidBackground(ctx, posX, posY, this.widthAnimation.getValue(), 14.5F, theme, this.alpha.getValue());

      // Icon from hudicons/keybinds.png (slightly smaller and higher)
      ctx.drawTexture(KEYBINDS_ICON, posX + 3, posY + 3.5F, 8, 8, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));

      // Title without separator
      ctx.drawText(Fonts.SEMIBOLD.getFont(7.5F), "Hotkeys", posX + 14.5F, posY + 4.75F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));

      posY += 14.5F + 1.0F;
      float bindWidth = 0.0F;
      Iterator var9 = Javelin.getInstance().getModuleManager().getModules().iterator();

      Module module;
      while(var9.hasNext()) {
         module = (Module)var9.next();
         if (module.getName().equals("Menu") || module.getName().equals("DropDown")) {
            continue;
         }
         if (module.getAnimation().getValue() != 0.0F && module.getKeyCode() != -1) {
            String bindText = "[" + Keyboard.getKeyName(module.getKeyCode()) + "]";
            float localBindWidth = Fonts.SEMIBOLD.getWidth(bindText, 6.75F);
            if (localBindWidth > bindWidth) {
               bindWidth = localBindWidth;
            }
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
               BooleanSetting boolSetting = (BooleanSetting) setting;
               if (boolSetting.getAnimation().getValue() != 0.0F && boolSetting.getKeyCode() != -1) {
                  String bindText = "[" + Keyboard.getKeyName(boolSetting.getKeyCode()) + "]";
                  float localBindWidth = Fonts.SEMIBOLD.getWidth(bindText, 6.75F);
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
            height += 11.0F + 1.0F;
            String bind = "[" + Keyboard.getKeyName(module.getKeyCode()) + "]";
            String moduleName = module.getName();
            float elementsWidth = Fonts.SEMIBOLD.getWidth(moduleName, 7.0F) + Fonts.SEMIBOLD.getWidth(bind, 6.75F) + 45.0F;

            float elementAlpha = module.getAnimation().getValue() * this.alpha.getValue();
            float elementY = posY + module.getAnimation().getValue() * 3.0F - 3.0F;

            drawSolidBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

            // Module name on left, bind in brackets on right — no separator
            ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), moduleName, posX + 5.0F, elementY + 3.25F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

            ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), bind, posX + this.widthAnimation.getValue() - 3.0F - this.xLine.getValue() / 2.0F - Fonts.SEMIBOLD.getWidth(bind, 6.75F) / 2.0F, elementY + 3.25F, theme.getColor().withAlpha(elementAlpha * 255.0F));

            if (elementsWidth > defaultWidth) {
               defaultWidth = elementsWidth;
            }

            posY += (11.0F + 1.0F) * module.getAnimation().getValue();
         }

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting) {
               BooleanSetting boolSetting = (BooleanSetting) setting;
               if (boolSetting.getAnimation().getValue() != 0.0F && boolSetting.getKeyCode() != -1) {
                  height += 11.0F + 1.0F;
                  String bind = "[" + Keyboard.getKeyName(boolSetting.getKeyCode()) + "]";
                  String settingName = boolSetting.getName();
                  float elementsWidth = Fonts.SEMIBOLD.getWidth(settingName, 7.0F) + Fonts.SEMIBOLD.getWidth(bind, 6.75F) + 45.0F;

                  float elementAlpha = boolSetting.getAnimation().getValue() * this.alpha.getValue();
                  float elementY = posY + boolSetting.getAnimation().getValue() * 3.0F - 3.0F;

                  drawSolidBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

                  ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), settingName, posX + 5.0F, elementY + 3.25F, (new ColorRGBA(-1)).withAlpha(elementAlpha * 255.0F));

                  ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), bind, posX + this.widthAnimation.getValue() - 3.0F - this.xLine.getValue() / 2.0F - Fonts.SEMIBOLD.getWidth(bind, 6.75F) / 2.0F, elementY + 3.25F, theme.getColor().withAlpha(elementAlpha * 255.0F));

                  if (elementsWidth > defaultWidth) {
                     defaultWidth = elementsWidth;
                  }

                  posY += (11.0F + 1.0F) * boolSetting.getAnimation().getValue();
               }
            }
         }
      }

      this.widthAnimation.update(defaultWidth);
      this.width = this.widthAnimation.getValue();
      this.height = height;
   }
}
