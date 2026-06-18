package tech.javelin.client.modules.impl.player;

import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.TextSetting;

@ModuleAnnotation(
   name = "GeneratorProxy",
   category = Category.PLAYER,
   description = "Настройки прокси для подключения к серверу"
)
public final class GeneratorProxy extends Module {
   public static final GeneratorProxy INSTANCE = new GeneratorProxy();

   public final ModeSetting proxyType = new ModeSetting("Тип прокси", new String[]{"SOCKS4", "SOCKS5", "HTTP"});
   public final TextSetting proxyIp = new TextSetting("IP прокси", "127.0.0.1");
   public final TextSetting proxyPort = new TextSetting("Порт прокси", "1080");
   public final BooleanSetting authEnabled = new BooleanSetting("Авторизация", false);
   public final TextSetting username = new TextSetting("Логин", "", () -> authEnabled.isEnabled());
   public final TextSetting password = new TextSetting("Пароль", "", () -> authEnabled.isEnabled());
   public final BooleanSetting showRealIp = new BooleanSetting("Показывать реальный IP", false);

   private GeneratorProxy() {
   }

   public String getProxyAddress() {
      return proxyIp.get() + ":" + proxyPort.get();
   }
}
