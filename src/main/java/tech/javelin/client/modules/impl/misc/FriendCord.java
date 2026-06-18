package tech.javelin.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.input.EventKey;
import tech.javelin.base.filemanager.impl.FriendManager;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.KeySetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
   name = "FriendCord",
   description = "Отправляет координаты всем друзьям по бинду",
   category = Category.MISC
)
public final class FriendCord extends Module {
   private final KeySetting sendBind = new KeySetting("Отправить координаты");
   private final ModeSetting format = new ModeSetting("Формат", new String[]{"/msg %s %s", "/tell %s %s", "/w %s %s", "/f msg %s %s"});
   private final BooleanSetting includeDimension = new BooleanSetting("Указывать измерение", true);
   private final BooleanSetting showNotification = new BooleanSetting("Показывать уведомление", true);
   
   public static final FriendCord INSTANCE = new FriendCord();
   
   private int messageIndex = 0;
   private int currentDelay = 0;
   private static final int MESSAGE_DELAY = 3; // Тики между сообщениями
   
   private FriendCord() {
      this.format.setValue(this.format.getValues().get(0));
   }
   
   @EventTarget
   @Native
   public void onKey(EventKey e) {
      if (e.isKeyDown(this.sendBind.getKeyCode())) {
         sendCoordsToFriends();
      }
   }
   
   private void sendCoordsToFriends() {
      FriendManager friendManager = Javelin.getInstance().getFriendManager();
      if (friendManager == null || friendManager.getItems().isEmpty()) {
         return;
      }
      
      if (mc.player == null || mc.world == null) {
         return;
      }
      
      int x = (int) mc.player.getX();
      int y = (int) mc.player.getY();
      int z = (int) mc.player.getZ();
      
      String dimension = "";
      if (includeDimension.isEnabled()) {
         String worldKey = mc.world.getRegistryKey().getValue().getPath();
         switch (worldKey) {
            case "overworld":
               dimension = " [Overworld]";
               break;
            case "the_nether":
               dimension = " [Nether]";
               break;
            case "the_end":
               dimension = " [End]";
               break;
            default:
               dimension = " [" + worldKey + "]";
         }
      }
      
      String coords = String.format("X: %d Y: %d Z: %d%s", x, y, z, dimension);
      String msgFormat = format.get();
      
      int index = 1;
      for (String friendName : friendManager.getItems()) {
         String message = String.format(msgFormat, friendName, coords + " (" + index + ")");
         
         // Отправляем сообщение с задержкой
         sendDelayedMessage(message, index * MESSAGE_DELAY);
         index++;
      }
      
      if (showNotification.isEnabled()) {
         // Можно добавить уведомление в чат игроку
         mc.player.sendMessage(net.minecraft.text.Text.of("§a[FriendCord] §fОтправлено координат: §a" + (index - 1) + " §fдрузьям"), true);
      }
   }
   
   private void sendDelayedMessage(String message, int delayTicks) {
      new Thread(() -> {
         try {
            // Задержка в тиках (50мс * delayTicks)
            Thread.sleep(delayTicks * 50L);
            
            if (mc.player != null && mc.player.networkHandler != null) {
               mc.player.networkHandler.sendChatMessage(message);
            }
         } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
         }
      }).start();
   }
}
