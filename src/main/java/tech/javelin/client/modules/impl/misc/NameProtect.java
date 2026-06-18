package tech.javelin.client.modules.impl.misc;

import java.util.Collection;
import java.util.Iterator;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
   name = "NameProtect",
   category = Category.MISC,
   description = "Скрывает ник игрока"
)
public final class NameProtect extends Module {
   public static final NameProtect INSTANCE = new NameProtect();
   private final BooleanSetting hideFriends = new BooleanSetting("Скрыть друзей", false);
   public String customNick = "CopperHead.fun";

   private NameProtect() {
   }

   public void setCustomNick(String nick) {
      this.customNick = (nick == null || nick.isEmpty()) ? "CopperHead.fun" : nick;
   }

   @Native
   public static String getCustomName() {
      if (INSTANCE != null && INSTANCE.isEnabled() && mc.player != null) {
         return INSTANCE.customNick;
      }
      return mc.player != null ? mc.player.getNameForScoreboard() : "Player";
   }

   @Native
   public static String getCustomName(String originalName) {
      if (INSTANCE != null && INSTANCE.isEnabled() && mc.player != null) {
         String me = mc.player.getNameForScoreboard();
         String replacement = INSTANCE.customNick;
         if (originalName.contains(me)) {
            return originalName.replace(me, replacement);
         }
         if (INSTANCE.hideFriends.isEnabled()) {
            Collection<String> friends = Javelin.getInstance().getFriendManager().getItems();
            Iterator var5 = friends.iterator();
            while (var5.hasNext()) {
               String friend = (String) var5.next();
               if (originalName.contains(friend)) {
                  return originalName.replace(friend, replacement);
               }
            }
         }
         return originalName;
      }
      return originalName;
   }
}
