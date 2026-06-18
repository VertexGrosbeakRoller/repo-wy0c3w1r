package tech.javelin.client.modules.impl.render;

import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "FullBright", category = Category.RENDER, description = "Full brightness with gamma/night vision")
public class FullBright extends Module {
   public static final FullBright INSTANCE = new FullBright();
   
   public final ModeSetting mode = new ModeSetting("Mode", "Gamma", "NightVision", "Custom");
   public final NumberSetting brightness = new NumberSetting("Brightness", 15.0f, 1.0f, 20.0f, 1.0f);

   private FullBright() {}
}
