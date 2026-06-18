package tech.javelin.client.hud.elements.component;

import com.mojang.authlib.GameProfile;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import lombok.Generated;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.GameMode;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

public class StaffComponent extends DraggableHudElement {
   private final Map<String, StaffComponent.StaffModule> modules = new LinkedHashMap();
   private final Set<String> staffPrefix = Set.of(new String[]{"helper", "ᴀдмин", "moder", "staff", "admin", "curator", "стажёр", "сотрудник", "помощник", "админ", "модер", "ꔗ", "ꔥ", "ꔡ", "ꔳ"});
   private final Map<String, Identifier> skinTextureCache = new HashMap();
   private long lastStaffUpdate = 0L;
   private long lastSkinCacheClear = 0L;
   private final Set<String> currentStaffKeys = new HashSet();
   private final Animation widthAnimation;
   private final Animation alpha;

   private static final float blurStrength = 15.0f;
   private static final float cornerRadius = 2.25f;

   public StaffComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.widthAnimation = new Animation(200L, Easing.CUBIC_OUT);
      this.alpha = new Animation(200L, Easing.CUBIC_OUT);
   }

   private void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {
      DrawUtil.drawBlur(
              ctx.getMatrices(), x, y, width, height,
              blurStrength,
              BorderRadius.all(cornerRadius),
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
              BorderRadius.all(cornerRadius),
              backgroundColor
      );
   }

   @Native
   public void render(CustomDrawContext ctx) {
      long currentTime = System.currentTimeMillis();
      if (currentTime - this.lastStaffUpdate > 50L && mc.getNetworkHandler() != null) {
         this.updateStaffList();
         this.lastStaffUpdate = currentTime;
      }

      if (currentTime - this.lastSkinCacheClear > 30000L) {
         this.skinTextureCache.clear();
         this.lastSkinCacheClear = currentTime;
      }

      this.modules.entrySet().removeIf((entry) -> {
         return ((StaffComponent.StaffModule)entry.getValue()).isDelete();
      });
      float posX = this.getX();
      float posY = this.getY();
      float defaultWidth = 61.5F;
      float height = 14.5F;
      boolean isFound = false;
      Iterator var9 = this.modules.entrySet().iterator();

      while(var9.hasNext()) {
         Entry<String, StaffComponent.StaffModule> module = (Entry)var9.next();
         ((StaffComponent.StaffModule)module.getValue()).animation.update(this.currentStaffKeys.contains(module.getKey()));
         if (((StaffComponent.StaffModule)module.getValue()).animation.getValue() != 0.0F) {
            this.alpha.update(1.0F);
            isFound = true;
         }
      }

      if (!isFound && !(mc.currentScreen instanceof ChatScreen)) {
         this.alpha.update(0.0F);
      }

      if (mc.currentScreen instanceof ChatScreen) {
         this.alpha.update(1.0F);
      }

      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();

      drawBlurBackground(ctx, posX, posY, this.widthAnimation.getValue(), 14.5F, theme, this.alpha.getValue());

      ctx.drawText(Fonts.NURIKI.getFont(9F), "O", posX + 4F, posY + 5.5F, theme.getColor().withAlpha(255.0F * this.alpha.getValue()));

      ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), "|", posX + 15.0F, posY + 4.75F, new ColorRGBA(166, 166, 166, 255.0F * this.alpha.getValue()));

      ctx.drawText(Fonts.SEMIBOLD.getFont(7.5F), "Staff online", posX + 18.5F, posY + 4.75F, (new ColorRGBA(-1)).withAlpha(255.0F * this.alpha.getValue()));
      posY += 14.5F + 1.0F; // Добавлен отступ после заголовка
      Iterator var16 = this.modules.entrySet().iterator();

      while(var16.hasNext()) {
         Entry<String, StaffComponent.StaffModule> module = (Entry)var16.next();
         if (((StaffComponent.StaffModule)module.getValue()).animation.getValue() != 0.0F) {
            height += 11.0F + 1.0F; // Добавлен отступ
            Identifier skinTexture = (Identifier)this.skinTextureCache.get(((StaffComponent.StaffModule)module.getValue()).name);
            if (skinTexture == null && mc.getNetworkHandler() != null) {
               PlayerListEntry player = (PlayerListEntry)mc.getNetworkHandler().getPlayerList().stream().filter((p) -> {
                  return p.getProfile() != null && ((StaffComponent.StaffModule)module.getValue()).name.equals(p.getProfile().getName());
               }).findFirst().orElse(null);
               if (player != null && player.getSkinTextures() != null) {
                  skinTexture = player.getSkinTextures().texture();
                  this.skinTextureCache.put(((StaffComponent.StaffModule)module.getValue()).name, skinTexture);
               }
            }

            if (skinTexture == null) {
               skinTexture = DefaultSkinHelper.getSteve().texture();
            }

            Text prefix = ((StaffComponent.StaffModule)module.getValue()).displayNameText;
            float elementsWidth = Fonts.SEMIBOLD.getWidth(prefix.getString(), 7.0F) + 28.0F;

            float elementAlpha = ((StaffComponent.StaffModule)module.getValue()).animation.getValue() * this.alpha.getValue();

            float elementY = posY + ((StaffComponent.StaffModule)module.getValue()).animation.getValue() * 3.0F - 3.0F;
            drawBlurBackground(ctx, posX, elementY, this.widthAnimation.getValue(), 11.0F, theme, elementAlpha);

            ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), "|", posX + this.widthAnimation.getValue() - 10.5F, elementY + 3.25F, new ColorRGBA(166, 166, 166, 255.0F * elementAlpha));

            ColorRGBA statusColor;
            if (((StaffComponent.StaffModule)module.getValue()).status == StaffComponent.Status.SPEC) {
               statusColor = new ColorRGBA(255, 32, 32, 255.0F * elementAlpha);
            } else if (((StaffComponent.StaffModule)module.getValue()).status == StaffComponent.Status.VANISHED) {
               statusColor = new ColorRGBA(255, 220, 0, 255.0F * elementAlpha);
            } else {
               statusColor = new ColorRGBA(32, 255, 32, 255.0F * elementAlpha);
            }
            DrawUtil.drawRoundedRect(ctx.getMatrices(), posX + this.widthAnimation.getValue() - 7.5F, elementY + 3.5F, 4.0F, 4.0F, BorderRadius.all(2.0F), statusColor);

            DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTexture, posX + 2.5F, elementY + 2.25F, 6.5F, BorderRadius.all(2.0F), ColorRGBA.WHITE.withAlpha(((StaffComponent.StaffModule)module.getValue()).animation.getValue() * 255.0F));

            ctx.drawText(Fonts.SEMIBOLD.getFont(7.0F), prefix, posX + 11.5F, elementY + 3.25F, ((StaffComponent.StaffModule)module.getValue()).animation.getValue() * 255.0F * this.alpha.getValue());
            if (elementsWidth > defaultWidth) {
               defaultWidth = elementsWidth;
            }

            posY += (11.0F + 1.0F) * ((StaffComponent.StaffModule)module.getValue()).animation.getValue(); // Добавлен отступ
         }
      }

      this.widthAnimation.update(defaultWidth);
      this.width = this.widthAnimation.getValue();
      this.height = height;
   }

   private void updateStaffList() {
      if (mc.getNetworkHandler() != null) {
         this.currentStaffKeys.clear();
         Iterator var1 = mc.getNetworkHandler().getPlayerList().iterator();

         while(true) {
            PlayerListEntry entry;
            Text displayName;
            String display;
            String name;
            String prefix;
            do {
               do {
                  do {
                     GameProfile profile;
                     do {
                        do {
                           if (!var1.hasNext()) {
                              return;
                           }

                           entry = (PlayerListEntry)var1.next();
                           profile = entry.getProfile();
                           displayName = entry.getDisplayName();
                        } while(displayName == null);
                     } while(profile == null);

                     display = displayName.getString();
                     name = profile.getName();
                     prefix = display.replace(name, "").trim();
                     String var10000 = prefix.replaceAll("ꔗ", String.valueOf(Formatting.BLUE) + "MODER").replaceAll("ꔥ", String.valueOf(Formatting.BLUE) + "ST.MODER").replaceAll("ꔡ", String.valueOf(Formatting.LIGHT_PURPLE) + "MODER+").replaceAll("ꔀ", String.valueOf(Formatting.GRAY) + "PLAYER").replaceAll("ꔉ", String.valueOf(Formatting.YELLOW) + "HELPER").replaceAll("◆", "@").replaceAll("┃", "|").replaceAll("ꔳ", String.valueOf(Formatting.AQUA) + "ML.ADMIN");
                     String var10002 = String.valueOf(Formatting.RED);
                     prefix = var10000.replaceAll("ꔅ", var10002 + "Y" + String.valueOf(Formatting.WHITE) + "T").replaceAll("ꔂ", String.valueOf(Formatting.BLUE) + "D.MODER").replaceAll("ꕠ", String.valueOf(Formatting.YELLOW) + "D.HELPER").replaceAll("ꕄ", String.valueOf(Formatting.RED) + "DRACULA").replaceAll("ꔖ", String.valueOf(Formatting.AQUA) + "OVERLORD").replaceAll("ꕈ", String.valueOf(Formatting.GREEN) + "COBRA").replaceAll("ꔨ", String.valueOf(Formatting.LIGHT_PURPLE) + "DRAGON").replaceAll("ꔤ", String.valueOf(Formatting.RED) + "IMPERATOR").replaceAll("ꔠ", String.valueOf(Formatting.GOLD) + "MAGISTER").replaceAll("ꔄ", String.valueOf(Formatting.BLUE) + "HERO").replaceAll("ꔒ", String.valueOf(Formatting.GREEN) + "AVENGER").replaceAll("ꕒ", String.valueOf(Formatting.WHITE) + "RABBIT").replaceAll("ꔈ", String.valueOf(Formatting.YELLOW) + "TITAN").replaceAll("ꕀ", String.valueOf(Formatting.DARK_GREEN) + "HYDRA").replaceAll("ꔶ", String.valueOf(Formatting.GOLD) + "TIGER").replaceAll("ꔲ", String.valueOf(Formatting.DARK_PURPLE) + "BULL").replaceAll("ꕖ", String.valueOf(Formatting.BLACK) + "BUNNY").replaceAll("ꕗꕘ", String.valueOf(Formatting.YELLOW) + "SPONSOR").replaceAll("\ud83d\udd25", "@").replaceAll("ᴀ", "A").replaceAll("ʙ", "B").replaceAll("ᴄ", "C").replaceAll("ᴅ", "D").replaceAll("ᴇ", "E").replaceAll("ғ", "F").replaceAll("ɢ", "G").replaceAll("ʜ", "H").replaceAll("ɪ", "I").replaceAll("ᴊ", "J").replaceAll("ᴋ", "K").replaceAll("ʟ", "L").replaceAll("ᴍ", "M").replaceAll("ɴ", "N").replaceAll("ꜱ", "S").replaceAll("ᴏ", "O").replaceAll("ᴘ", "P").replaceAll("ǫ", "Q").replaceAll("ʀ", "R").replaceAll("ᴛ", "T").replaceAll("ᴜ", "U").replaceAll("ᴠ", "V").replaceAll("ᴡ", "W").replaceAll("ꜰ", "F").replaceAll("ʏ", "Y").replaceAll("ᴢ", "Z");
                  } while(prefix.length() < 2);
               } while(!this.containsAnyKeyword(prefix));
            } while(Javelin.getInstance().getServerHandler().getServer().equals("LonyGrief") && (prefix.contains("D.ADMIN") || prefix.contains("sTAFF")));

            StaffComponent.Status status = entry.getGameMode() == GameMode.SPECTATOR ? StaffComponent.Status.VANISHED : StaffComponent.Status.NONE;
            final Text finalDisplayName = displayName;
            final String finalDisplay = display;
            final String finalName = name;
            final StaffComponent.Status finalStatus = status;
            this.modules.computeIfAbsent(display, (k) -> {
               return new StaffComponent.StaffModule(this, finalDisplayName, finalDisplay, finalName, finalStatus);
            });
            this.currentStaffKeys.add(display);
         }
      }
   }

   public boolean containsAnyKeyword(String text) {
      String lower = text.toLowerCase(Locale.US);
      Iterator var3 = this.staffPrefix.iterator();

      String keyword;
      do {
         if (!var3.hasNext()) {
            return false;
         }

         keyword = (String)var3.next();
      } while(!lower.contains(keyword));

      return true;
   }

   private class StaffModule {
      private final Animation animation;
      private final Animation animationColor;
      private final Text displayNameText;
      private final String key;
      private final String name;
      private final StaffComponent.Status status;
      private final long appearTime;

      public StaffModule(final StaffComponent param1, Text displayNameText, String key, String name, StaffComponent.Status status) {
         this.animation = new Animation(250L, 0.01F, Easing.CUBIC_OUT);
         this.animationColor = new Animation(200L, Easing.QUAD_IN_OUT);
         this.displayNameText = displayNameText;
         this.key = key;
         this.name = name;
         this.status = status;
         this.appearTime = System.currentTimeMillis();
      }

      public boolean isDelete() {
         return this.animation.getValue() == 0.0F;
      }
   }

   public static enum Status {
      NONE,
      VANISHED,
      SPEC;

      private static StaffComponent.Status[] $values() {
         return new StaffComponent.Status[]{NONE, VANISHED, SPEC};
      }
   }

   public static class Staff {
      private Text prefix;
      private String name;
      private boolean isSpec;
      private StaffComponent.Status status;

      @Generated
      public Text getPrefix() {
         return this.prefix;
      }

      @Generated
      public String getName() {
         return this.name;
      }

      @Generated
      public boolean isSpec() {
         return this.isSpec;
      }

      @Generated
      public StaffComponent.Status getStatus() {
         return this.status;
      }

      @Generated
      public void setPrefix(Text prefix) {
         this.prefix = prefix;
      }

      @Generated
      public void setName(String name) {
         this.name = name;
      }

      @Generated
      public void setSpec(boolean isSpec) {
         this.isSpec = isSpec;
      }

      @Generated
      public void setStatus(StaffComponent.Status status) {
         this.status = status;
      }

      @Generated
      public boolean equals(Object o) {
         if (o == this) {
            return true;
         } else if (!(o instanceof StaffComponent.Staff)) {
            return false;
         } else {
            StaffComponent.Staff other = (StaffComponent.Staff)o;
            if (!other.canEqual(this)) {
               return false;
            } else if (this.isSpec() != other.isSpec()) {
               return false;
            } else {
               label49: {
                  Object this$prefix = this.getPrefix();
                  Object other$prefix = other.getPrefix();
                  if (this$prefix == null) {
                     if (other$prefix == null) {
                        break label49;
                     }
                  } else if (this$prefix.equals(other$prefix)) {
                     break label49;
                  }

                  return false;
               }

               Object this$name = this.getName();
               Object other$name = other.getName();
               if (this$name == null) {
                  if (other$name != null) {
                     return false;
                  }
               } else if (!this$name.equals(other$name)) {
                  return false;
               }

               Object this$status = this.getStatus();
               Object other$status = other.getStatus();
               if (this$status == null) {
                  if (other$status != null) {
                     return false;
                  }
               } else if (!this$status.equals(other$status)) {
                  return false;
               }

               return true;
            }
         }
      }

      @Generated
      protected boolean canEqual(Object other) {
         return other instanceof StaffComponent.Staff;
      }

      @Generated
      public int hashCode() {
         int PRIME = 1;
         int result = 1; result = result * 59 + (this.isSpec() ? 79 : 97);
         Object $prefix = this.getPrefix();
         result = result * 59 + ($prefix == null ? 43 : $prefix.hashCode());
         Object $name = this.getName();
         result = result * 59 + ($name == null ? 43 : $name.hashCode());
         Object $status = this.getStatus();
         result = result * 59 + ($status == null ? 43 : $status.hashCode());
         return result;
      }

      @Generated
      public String toString() {
         String var10000 = String.valueOf(this.getPrefix());
         return "StaffComponent.Staff(prefix=" + var10000 + ", name=" + this.getName() + ", isSpec=" + this.isSpec() + ", status=" + String.valueOf(this.getStatus()) + ")";
      }

      @Generated
      public Staff(Text prefix, String name, boolean isSpec, StaffComponent.Status status) {
         this.prefix = prefix;
         this.name = name;
         this.isSpec = isSpec;
         this.status = status;
      }
   }
}
