package tech.javelin.client.modules.impl.misc;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.screen.ChatScreen;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.input.EventKey;
import tech.javelin.base.events.impl.other.EventTick;
import tech.javelin.base.modules.ModuleManager;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.utility.game.other.MessageUtil;
import tech.javelin.utility.interfaces.IClient;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
   name = "UnHook",
   description = "ПОЛНОСТЬЮ скрывает клиент. Напишите 2 буквы в чате для восстановления",
   category = Category.MISC
)
public final class UnHook extends Module implements IClient {
   public static final UnHook INSTANCE = new UnHook();
   
   private boolean unhooked = false;
   private String chatBuffer = "";
   private long lastCharTime = 0;
   private boolean awaitingRestore = false;
   private String typedChars = "";
   private long typedTime = 0;
   
   // Список модулей которые были включены до UnHook
   private final List<Module> enabledModules = new ArrayList<>();
   private boolean wasInterfaceEnabled = false;
   
   private UnHook() {}
   
   @Override
   public void onEnable() {
      super.onEnable();
      activateUnHook();
   }
   
   @Override
   public void onDisable() {
      super.onDisable();
      // Не деактивируем здесь - только через чат!
   }
   
   @Native
   public void activateUnHook() {
      if (unhooked) return;
      unhooked = true;
      chatBuffer = "";
      typedChars = "";
      
      ModuleManager mm = Javelin.getInstance().getModuleManager();
      
      // Сохраняем и отключаем ВСЕ модули кроме себя
      enabledModules.clear();
      for (Module module : mm.getModules()) {
         if (module != this && module.isEnabled()) {
            enabledModules.add(module);
            module.setEnabled(false);
         }
      }
      
      // Сохраняем состояние Interface
      try {
         wasInterfaceEnabled = tech.javelin.client.modules.impl.render.Interface.INSTANCE.isEnabled();
         tech.javelin.client.modules.impl.render.Interface.INSTANCE.setEnabled(false);
      } catch (Exception ignored) {}
      
      MessageUtil.displayInfo("§c[UnHook] §fКлиент §cПОЛНОСТЬЮ§f скрыт. Введите §e2 буквы §fв чате.");
   }
   
   @Native
   public void deactivateUnHook() {
      if (!unhooked) return;
      unhooked = false;
      chatBuffer = "";
      typedChars = "";
      
      // Восстанавливаем все модули
      for (Module module : enabledModules) {
         if (module != null && !module.isEnabled()) {
            module.setEnabled(true);
         }
      }
      enabledModules.clear();
      
      // Восстанавливаем Interface
      try {
         if (wasInterfaceEnabled) {
            tech.javelin.client.modules.impl.render.Interface.INSTANCE.setEnabled(true);
         }
      } catch (Exception ignored) {}
      
      MessageUtil.displayInfo("§a[UnHook] §fКлиент восстановлен!");
   }
   
   @EventTarget
   @Native
   public void onKey(EventKey event) {
      if (!unhooked) return;
      if (!(mc.currentScreen instanceof ChatScreen)) return;
      
      int keyCode = event.getKeyCode();
      int action = event.getAction();
      
      if (action != 1) return; // Только нажатие
      
      // Буквы A-Z (65-90)
      if (keyCode >= 65 && keyCode <= 90) {
         char character = (char) ('a' + (keyCode - 65));
         chatBuffer += character;
         typedChars += character;
         lastCharTime = System.currentTimeMillis();
         typedTime = System.currentTimeMillis();
         
         // Восстановление после 2 букв
         if (chatBuffer.length() >= 2) {
            awaitingRestore = true;
            chatBuffer = "";
         }
      } else if (keyCode == 259) { // Backspace
         if (!chatBuffer.isEmpty()) chatBuffer = chatBuffer.substring(0, chatBuffer.length() - 1);
         if (!typedChars.isEmpty()) typedChars = typedChars.substring(0, typedChars.length() - 1);
      } else if (keyCode == 257 || keyCode == 335) { // Enter (оба варианта)
         chatBuffer = "";
         typedChars = "";
      }
   }
   
   @EventTarget
   @Native  
   public void onTick(EventTick event) {
      if (!unhooked) return;
      
      // Автоочистка чата через 5 сек - удаляем буквы чтобы никто не видел
      if (!typedChars.isEmpty()) {
         if (System.currentTimeMillis() - typedTime > 5000) {
            if (mc.currentScreen instanceof ChatScreen chat) {
               clearChatInput(chat);
            }
            typedChars = "";
         }
      }
      
      // Таймаут буфера
      if (System.currentTimeMillis() - lastCharTime > 3000) {
         chatBuffer = "";
      }
      
      // Восстановление клиента и отключение UnHook
      if (awaitingRestore) {
         deactivateUnHook();
         this.setEnabled(false);
         awaitingRestore = false;
      }
   }
   
   private void clearChatInput(ChatScreen chat) {
      try {
         java.lang.reflect.Field field = ChatScreen.class.getDeclaredField("chatField");
         field.setAccessible(true);
         Object chatField = field.get(chat);
         if (chatField instanceof net.minecraft.client.gui.widget.TextFieldWidget textField) {
            String current = textField.getText();
            // Удаляем только наши буквы из начала строки
            if (current.startsWith(typedChars)) {
               textField.setText(current.substring(typedChars.length()));
            } else {
               textField.setText("");
            }
         }
      } catch (Exception e) {
         // Fallback - backspace несколько раз
         for (int i = 0; i < typedChars.length(); i++) {
            mc.execute(() -> {
               if (mc.currentScreen instanceof ChatScreen) {
                  mc.keyboard.onKey(mc.getWindow().getHandle(), 259, 0, 1, 0);
               }
            });
         }
      }
   }
   
   public boolean isUnhooked() {
      return unhooked;
   }
   
   // Для совместимости с другими модулями
   public boolean shouldHideHud() {
      return unhooked;
   }
   
   public boolean shouldHideMessages() {
      return unhooked;
   }
}
