package tech.javelin.base.theme;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import lombok.Generated;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.base.color.ColorUtil;

public class ThemeManager {
   private Theme currentTheme;
   private final List<Theme> themes = new ArrayList();
   private final Theme defaultTheme = new Theme("White-Purple", (new Color(255, 255, 255, 255)).getRGB(), (new Color(128, 0, 255, 255)).getRGB());
   private int customColor1 = (new Color(255, 100, 50, 255)).getRGB();
   private int customColor2 = (new Color(20, 10, 5, 255)).getRGB();
   private Theme customTheme;

   public ThemeManager() {
      this.initThemes();
   }

   @Native
   private void initThemes() {
      if (this.currentTheme == null) {
         this.currentTheme = this.defaultTheme;
      }

      this.customTheme = new Theme("Custom", customColor1, customColor2);
      this.themes.addAll(List.of(new Theme[]{
         this.defaultTheme,
         new Theme("Yellow-Purple", (new Color(255, 255, 0, 255)).getRGB(), (new Color(128, 0, 255, 255)).getRGB()),
         new Theme("Blue-Yellow", (new Color(0, 150, 255, 255)).getRGB(), (new Color(255, 220, 0, 255)).getRGB()),
         new Theme("Red-Black", (new Color(220, 20, 60, 255)).getRGB(), (new Color(10, 10, 10, 255)).getRGB()),
         new Theme("Green-Orange", (new Color(0, 255, 128, 255)).getRGB(), (new Color(255, 140, 0, 255)).getRGB()),
         new Theme("Cyan-Pink", (new Color(0, 255, 255, 255)).getRGB(), (new Color(255, 20, 147, 255)).getRGB()),
         new Theme("White-Blue", (new Color(255, 255, 255, 255)).getRGB(), (new Color(30, 144, 255, 255)).getRGB()),
         new Theme("Gold-Purple", (new Color(255, 215, 0, 255)).getRGB(), (new Color(128, 0, 128, 255)).getRGB()),
         new Theme("Lime-Red", (new Color(50, 205, 50, 255)).getRGB(), (new Color(220, 20, 60, 255)).getRGB()),
         new Theme("Ice-Fire", (new Color(0, 255, 255, 255)).getRGB(), (new Color(255, 69, 0, 255)).getRGB()),
         new Theme("Silver-Gold", (new Color(192, 192, 192, 255)).getRGB(), (new Color(255, 215, 0, 255)).getRGB()),
         new Theme("Neon", (new Color(57, 255, 20, 255)).getRGB(), (new Color(255, 0, 255, 255)).getRGB()),
         new Theme("Ocean", (new Color(0, 105, 148, 255)).getRGB(), (new Color(0, 191, 255, 255)).getRGB()),
         new Theme("Sunset", (new Color(255, 94, 77, 255)).getRGB(), (new Color(255, 206, 84, 255)).getRGB()),
         new Theme("Forest", (new Color(34, 139, 34, 255)).getRGB(), (new Color(85, 107, 47, 255)).getRGB()),
         new Theme("Candy", (new Color(255, 105, 180, 255)).getRGB(), (new Color(0, 255, 255, 255)).getRGB()),
         new Theme("Dark", (new Color(45, 45, 45, 255)).getRGB(), (new Color(20, 20, 20, 255)).getRGB()),
         new Theme("Matrix", (new Color(0, 255, 0, 255)).getRGB(), (new Color(0, 50, 0, 255)).getRGB()),
         new Theme("Raspberry", (new Color(255, 0, 100, 255)).getRGB(), (new Color(128, 0, 128, 255)).getRGB()),
         new Theme("Crimson Edge", (new Color(220, 30, 50, 255)).getRGB(), (new Color(25, 5, 8, 255)).getRGB()),
         new Theme("Void Purple", (new Color(160, 0, 255, 255)).getRGB(), (new Color(15, 0, 30, 255)).getRGB()),
         new Theme("Arctic Ice", (new Color(0, 220, 255, 255)).getRGB(), (new Color(0, 15, 25, 255)).getRGB()),
         new Theme("Toxic Lime", (new Color(100, 255, 0, 255)).getRGB(), (new Color(5, 20, 0, 255)).getRGB()),
         new Theme("Solar Flare", (new Color(255, 140, 0, 255)).getRGB(), (new Color(30, 10, 0, 255)).getRGB()),
         new Theme("Sakura", (new Color(255, 130, 180, 255)).getRGB(), (new Color(30, 5, 15, 255)).getRGB()),
         new Theme("Deep Ocean", (new Color(0, 80, 220, 255)).getRGB(), (new Color(0, 5, 25, 255)).getRGB()),
         new Theme("Jade", (new Color(0, 200, 130, 255)).getRGB(), (new Color(0, 20, 12, 255)).getRGB()),
         new Theme("Blood Moon", (new Color(255, 40, 0, 255)).getRGB(), (new Color(20, 0, 0, 255)).getRGB()),
         new Theme("White", (new Color(230, 230, 230, 255)).getRGB(), (new Color(30, 30, 30, 255)).getRGB()),
         new Theme("Gold Rush", (new Color(255, 210, 0, 255)).getRGB(), (new Color(25, 18, 0, 255)).getRGB()),
         new Theme("Neon Pink", (new Color(255, 0, 180, 255)).getRGB(), (new Color(25, 0, 18, 255)).getRGB()),
         new Theme("Silver Storm", (new Color(180, 190, 210, 255)).getRGB(), (new Color(10, 12, 18, 255)).getRGB()),
         new Theme("Lava", (new Color(255, 80, 0, 255)).getRGB(), (new Color(30, 5, 0, 255)).getRGB()),
         new Theme("Midnight", (new Color(30, 30, 120, 255)).getRGB(), (new Color(5, 5, 20, 255)).getRGB()),
         new Theme("Emerald", (new Color(0, 230, 80, 255)).getRGB(), (new Color(0, 25, 8, 255)).getRGB()),
         new Theme("Lavender", (new Color(180, 100, 255, 255)).getRGB(), (new Color(15, 5, 30, 255)).getRGB()),
         new Theme("Frost", (new Color(180, 230, 255, 255)).getRGB(), (new Color(10, 18, 28, 255)).getRGB()),
         new Theme("Rose Gold", (new Color(255, 180, 160, 255)).getRGB(), (new Color(30, 12, 10, 255)).getRGB()),
         this.customTheme
      }));
   }

   public void setCustomColors(int color1, int color2) {
      this.customColor1 = color1;
      this.customColor2 = color2;
      if (this.customTheme != null) {
         this.customTheme.setColor1(color1);
         this.customTheme.setColor2(color2);
         this.customTheme.setFromColor1(color1);
         this.customTheme.setFromColor2(color2);
      }
   }

   public int getCustomColor1() { return customColor1; }
   public int getCustomColor2() { return customColor2; }
   public Theme getCustomTheme() { return customTheme; }

   public ColorRGBA getClientColor(int index) {
      return this.currentTheme == null ? new ColorRGBA(255, 255, 255, 255) : ColorUtil.gradient(3, index, this.currentTheme.getColor(), this.currentTheme.getSecondColor());
   }

   @Generated
   public Theme getCurrentTheme() {
      return this.currentTheme;
   }

   @Generated
   public List<Theme> getThemes() {
      return this.themes;
   }

   @Generated
   public Theme getDefaultTheme() {
      return this.defaultTheme;
   }

   @Generated
   public void setCurrentTheme(Theme currentTheme) {
      this.currentTheme = currentTheme;
   }

   @Generated
   public boolean equals(Object o) {
      if (o == this) {
         return true;
      } else if (!(o instanceof ThemeManager)) {
         return false;
      } else {
         ThemeManager other = (ThemeManager)o;
         if (!other.canEqual(this)) {
            return false;
         } else {
            label47: {
               Object this$currentTheme = this.getCurrentTheme();
               Object other$currentTheme = other.getCurrentTheme();
               if (this$currentTheme == null) {
                  if (other$currentTheme == null) {
                     break label47;
                  }
               } else if (this$currentTheme.equals(other$currentTheme)) {
                  break label47;
               }

               return false;
            }

            Object this$themes = this.getThemes();
            Object other$themes = other.getThemes();
            if (this$themes == null) {
               if (other$themes != null) {
                  return false;
               }
            } else if (!this$themes.equals(other$themes)) {
               return false;
            }

            Object this$defaultTheme = this.getDefaultTheme();
            Object other$defaultTheme = other.getDefaultTheme();
            if (this$defaultTheme == null) {
               if (other$defaultTheme != null) {
                  return false;
               }
            } else if (!this$defaultTheme.equals(other$defaultTheme)) {
               return false;
            }

            return true;
         }
      }
   }

   @Generated
   protected boolean canEqual(Object other) {
      return other instanceof ThemeManager;
   }

   @Generated
   public int hashCode() {
      int PRIME = 1;
      int result = 1;
      Object $currentTheme = this.getCurrentTheme();
      result = result * 59 + ($currentTheme == null ? 43 : $currentTheme.hashCode());
      Object $themes = this.getThemes();
      result = result * 59 + ($themes == null ? 43 : $themes.hashCode());
      Object $defaultTheme = this.getDefaultTheme();
      result = result * 59 + ($defaultTheme == null ? 43 : $defaultTheme.hashCode());
      return result;
   }

   @Generated
   public String toString() {
      String var10000 = String.valueOf(this.getCurrentTheme());
      return "ThemeManager(currentTheme=" + var10000 + ", themes=" + String.valueOf(this.getThemes()) + ", defaultTheme=" + String.valueOf(this.getDefaultTheme()) + ")";
   }
}
