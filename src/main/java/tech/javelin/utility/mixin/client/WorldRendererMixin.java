package tech.javelin.utility.mixin.client;

import net.minecraft.client.render.WorldRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import tech.javelin.client.modules.impl.render.CustomSky;
import tech.javelin.client.modules.impl.render.Removals;

@Mixin({WorldRenderer.class})
public class WorldRendererMixin {
   
   @Inject(
      method = {"renderSky"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderSky(CallbackInfo ci) {
      if (CustomSky.INSTANCE.isEnabled()) {
         CustomSky.INSTANCE.renderSkyShader();
         ci.cancel();
      }
   }
   
   @Inject(
      method = {"renderWeather"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void onRenderWeather(CallbackInfo ci) {
      if (Removals.INSTANCE.isRemoveRain()) {
         ci.cancel();
      }
   }
}
