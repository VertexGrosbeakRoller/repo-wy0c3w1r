package tech.javelin.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.c2s.common.ResourcePackStatusC2SPacket;
import net.minecraft.util.math.random.Random;
import ru.nexusguard.protection.annotations.Native;
import net.minecraft.text.Text;
import tech.javelin.base.events.impl.input.EventKey;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.KeySetting;
import tech.javelin.utility.game.other.MessageUtil;

@ModuleAnnotation(
   name = "ProxyGenerator",
   description = "Генерирует прокси и меняет IP по нажатию клавиши",
   category = Category.MISC
)
public final class ProxyGenerator extends Module {
   private final KeySetting changeIpBind = new KeySetting("Сменить IP", 69); // E key by default
   public static final ProxyGenerator INSTANCE = new ProxyGenerator();
   
   private final String[] proxyHosts = {
      "proxy1.example.com", "proxy2.example.com", "proxy3.example.com",
      "185.209.12", "194.32.15", "103.21.45", "91.203.89"
   };
   private final int[] proxyPorts = {8080, 3128, 1080, 8081, 4145};

   private ProxyGenerator() {}

   @EventTarget
   @Native
   public void onKey(EventKey e) {
      if (e.isKeyDown(this.changeIpBind.getKeyCode())) {
         changeProxy();
      }
   }
   
   private void changeProxy() {
      if (mc.player == null || mc.getNetworkHandler() == null) return;
      
      // 1. Сначала выходим с сервера
      mc.player.networkHandler.sendPacket(new ResourcePackStatusC2SPacket(mc.player.getUuid(), ResourcePackStatusC2SPacket.Status.DECLINED));
      mc.getNetworkHandler().getConnection().disconnect(net.minecraft.text.Text.of("[ProxyGenerator] Смена IP..."));
      
      // 2. Генерируем новый прокси
      String newProxy = generateRandomProxy();
      
      // 3. Выводим сообщение
      MessageUtil.displayInfo("§a[ProxyGenerator] §fIP изменен на: §e" + newProxy);
   }
   
   private String generateRandomProxy() {
      Random random = Random.create();
      String host = proxyHosts[random.nextInt(proxyHosts.length)];
      int port = proxyPorts[random.nextInt(proxyPorts.length)];
      
      // Если хост это паттерн IP, генерируем полный IP
      if (host.contains(".")) {
         String[] parts = host.split("\\.");
         if (parts.length == 3) {
            return parts[0] + "." + parts[1] + "." + parts[2] + "." + random.nextInt(256) + ":" + port;
         }
      }
      
      return host + ":" + port;
   }
}
