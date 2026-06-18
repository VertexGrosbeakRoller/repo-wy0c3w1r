package tech.javelin.base.modules;

import com.darkmagician6.eventapi.EventManager;
import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.client.option.Perspective;
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket;
import net.minecraft.util.math.MathHelper;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.input.EventKey;
import tech.javelin.base.events.impl.other.EventGameUpdate;
import tech.javelin.base.events.impl.render.EventHudRender;
import tech.javelin.base.events.impl.server.EventPacket;
import tech.javelin.base.macro.Macro;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.setting.Setting;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.javelin.client.modules.impl.combat.AntiBot;
import tech.javelin.client.modules.impl.combat.AutoExplosion;
import tech.javelin.client.modules.impl.combat.Velocity;
import tech.javelin.client.modules.impl.combat.Aura;
import tech.javelin.client.modules.impl.combat.AutoSwap;
import tech.javelin.client.modules.impl.combat.AutoTotem;
import tech.javelin.client.modules.impl.combat.ClickPearl;
import tech.javelin.client.modules.impl.misc.AHHelper;
import tech.javelin.client.modules.impl.misc.AutoAccept;
import tech.javelin.client.modules.impl.misc.AutoDuel;
import tech.javelin.client.modules.impl.misc.AutoRespawn;
import tech.javelin.client.modules.impl.misc.FakePlayer;
import tech.javelin.client.modules.impl.misc.ClickAction;
import tech.javelin.client.modules.impl.misc.ElytraHelper;
import tech.javelin.client.modules.impl.misc.FreeCam;
import tech.javelin.client.modules.impl.misc.ItemScroller;
import tech.javelin.client.modules.impl.misc.CommandBind;
import tech.javelin.client.modules.impl.misc.DeathCoord;
import tech.javelin.client.modules.impl.misc.NameProtect;
import tech.javelin.client.modules.impl.misc.NoInteract;
import tech.javelin.client.modules.impl.misc.ScoreboardHealth;
import tech.javelin.client.modules.impl.misc.FriendCord;
import tech.javelin.client.modules.impl.misc.ServerHelper;
import tech.javelin.client.modules.impl.movement.AirStuck;
import tech.javelin.client.modules.impl.movement.AutoSprint;
import tech.javelin.client.modules.impl.movement.ElytraAccelerate;
import tech.javelin.client.modules.impl.movement.ElytraBooster;
import tech.javelin.client.modules.impl.movement.ElytraMotion;
import tech.javelin.client.modules.impl.movement.ElytraRecast;
import tech.javelin.client.modules.impl.movement.GuiWalk;
import tech.javelin.client.modules.impl.movement.NoSlow;
import tech.javelin.client.modules.impl.movement.NoWeb;
import tech.javelin.client.modules.impl.movement.Speed;
import tech.javelin.client.modules.impl.movement.Flight;
import tech.javelin.client.modules.impl.player.AutoArmor;
import tech.javelin.client.modules.impl.player.AutoLoot;
import tech.javelin.client.modules.impl.player.AutoTool;
import tech.javelin.client.modules.impl.player.Blink;
import tech.javelin.client.modules.impl.player.FastBreak;
import tech.javelin.client.modules.impl.player.NoDelay;
import tech.javelin.client.modules.impl.player.NoPush;
import tech.javelin.client.modules.impl.render.AntiInvisible;
import tech.javelin.client.modules.impl.render.Crosshair;
import tech.javelin.client.modules.impl.render.CustomFog;
import tech.javelin.client.modules.impl.render.EntityESP;
import tech.javelin.client.modules.impl.render.FullBright;
import tech.javelin.client.modules.impl.render.Interface;
import tech.javelin.client.modules.impl.render.Menu;
import tech.javelin.client.modules.impl.render.Removals;
import tech.javelin.client.modules.impl.render.Predictions;
import tech.javelin.client.modules.impl.render.SwingAnimation;
import tech.javelin.client.modules.impl.render.TargetESP;
import tech.javelin.client.modules.impl.render.ViewModel;
import tech.javelin.client.modules.impl.render.WorldTime;
import tech.javelin.client.modules.impl.render.Arrows;
import tech.javelin.client.modules.impl.render.MusicHud;
import tech.javelin.client.modules.impl.render.Wings;
import tech.javelin.client.modules.impl.render.LineGlyphes;
import tech.javelin.client.modules.impl.render.Chams;
import tech.javelin.client.modules.impl.render.HoldMyItems;
import tech.javelin.client.modules.impl.render.FireFly;
import tech.javelin.client.modules.impl.render.Particles;
import tech.javelin.client.modules.impl.render.WorldParticles;
import tech.javelin.client.modules.impl.render.ClientSounds;
import tech.javelin.client.modules.impl.combat.PacketCriticals;
import tech.javelin.client.modules.impl.combat.SuperBow;
import tech.javelin.client.modules.impl.combat.TriggerBot;
import tech.javelin.client.screens.menu.MenuScreen;
import tech.javelin.utility.component.RotationComponent;
import tech.javelin.utility.game.player.rotation.Rotation;
import tech.javelin.utility.interfaces.IMinecraft;

public final class ModuleManager implements IMinecraft {
   private final List<Module> modules = new ArrayList<>();
   private boolean isBack;
   private boolean isRotated;
   private float acceleration;

   private long lastKeyPressTime = 0;
   private int lastKeyCode = -1;
   private static final long DEBOUNCE_THRESHOLD_MS = 200;

   public ModuleManager() {
      init();
      EventManager.register(this);
   }

   private void init() {
      registerCombat();
      registerMovement();
      registerRender();
      registerPlayer();
      registerMisc();
   }

   private void registerCombat() {
      registerModule(AntiBot.INSTANCE);
      registerModule(Velocity.INSTANCE);
      registerModule(AutoExplosion.INSTANCE);
      registerModule(Aura.INSTANCE);
      registerModule(AutoSwap.INSTANCE);
      registerModule(AutoTotem.INSTANCE);
      registerModule(ClickPearl.INSTANCE);
      registerModule(PacketCriticals.INSTANCE);
      registerModule(SuperBow.INSTANCE);
      registerModule(TriggerBot.INSTANCE);
   }

   private void registerMovement() {
      registerModule(AutoSprint.INSTANCE);
      registerModule(ElytraBooster.INSTANCE);
      registerModule(ElytraRecast.INSTANCE);
      registerModule(GuiWalk.INSTANCE);
      registerModule(NoSlow.INSTANCE);
      registerModule(Speed.INSTANCE);
      registerModule(Flight.INSTANCE);
      registerModule(AirStuck.INSTANCE);
      registerModule(ElytraMotion.INSTANCE);
      registerModule(NoWeb.INSTANCE);
   }

   private void registerRender() {
      registerModule(Interface.INSTANCE);
      registerModule(AntiInvisible.INSTANCE);
      registerModule(Menu.INSTANCE);
      registerModule(Removals.INSTANCE);
      registerModule(Predictions.INSTANCE);
      registerModule(SwingAnimation.INSTANCE);
      registerModule(Crosshair.INSTANCE);
      registerModule(ViewModel.INSTANCE);
      registerModule(CustomFog.INSTANCE);
      registerModule(WorldTime.INSTANCE);
      registerModule(EntityESP.INSTANCE);
      registerModule(TargetESP.INSTANCE);
      registerModule(Wings.INSTANCE);
      registerModule(MusicHud.INSTANCE);
      registerModule(Arrows.INSTANCE);
      registerModule(LineGlyphes.INSTANCE);
      registerModule(Chams.INSTANCE);
      registerModule(tech.javelin.client.modules.impl.render.CustomSky.INSTANCE);
      registerModule(FullBright.INSTANCE);
      registerModule(HoldMyItems.INSTANCE);
      registerModule(FireFly.INSTANCE);
      registerModule(Particles.INSTANCE);
      registerModule(WorldParticles.INSTANCE);
      registerModule(ClientSounds.INSTANCE);
   }

   private void registerPlayer() {
      registerModule(AutoTool.INSTANCE);
      registerModule(AutoArmor.INSTANCE);
      registerModule(AutoLoot.INSTANCE);
      registerModule(Blink.INSTANCE);
      registerModule(NoDelay.INSTANCE);
      registerModule(FastBreak.INSTANCE);
      registerModule(NoPush.INSTANCE);
   }

   private void registerMisc() {
      registerModule(ServerHelper.INSTANCE);
      registerModule(ElytraHelper.INSTANCE);
      registerModule(ItemScroller.INSTANCE);
      registerModule(ClickAction.INSTANCE);
      registerModule(FreeCam.INSTANCE);
      registerModule(AHHelper.INSTANCE);
      registerModule(NoInteract.INSTANCE);
      registerModule(AutoAccept.INSTANCE);
      registerModule(AutoRespawn.INSTANCE);
      registerModule(NameProtect.INSTANCE);
      registerModule(DeathCoord.INSTANCE);
      registerModule(CommandBind.INSTANCE);
      registerModule(ScoreboardHealth.INSTANCE);
      registerModule(ElytraAccelerate.INSTANCE);
      registerModule(AutoDuel.INSTANCE);
      registerModule(FakePlayer.INSTANCE);
      registerModule(FriendCord.INSTANCE);
   }

   private void registerModule(Module module) {
      modules.add(module);
   }

   public Module getModule(String name) {
      return modules.stream()
              .filter(module -> module.getName().equalsIgnoreCase(name))
              .findFirst()
              .orElse(null);
   }

   public Set<Module> getActiveModules() {
      Set<Module> active = new HashSet<>();
      for (Module module : modules) {
         if (module.isEnabled()) {
            active.add(module);
         }
      }
      return active;
   }

   @EventTarget
   public void onKey(EventKey event) {
      if (mc.currentScreen == null && event.getAction() == 1) {
         int keyCode = event.getKeyCode();
         long currentTime = System.currentTimeMillis();
         
         if (keyCode == lastKeyCode && (currentTime - lastKeyPressTime) < DEBOUNCE_THRESHOLD_MS) {
            return;
         }
         
         lastKeyCode = keyCode;
         lastKeyPressTime = currentTime;
         
         boolean isCommandBind = CommandBind.INSTANCE.isEnabled()
               && CommandBind.INSTANCE.getBinds().stream().anyMatch(e -> e.keyCode == keyCode);
         if (!isCommandBind) {
            for (Module module : modules) {
               if (module.getKeyCode() == keyCode && module.getKeyCode() != -1) {
                  module.toggle();
               }
            }
         }

         for (Macro macro : Javelin.getInstance().getMacroManager().getItems()) {
            if (keyCode == macro.getBind()) {
               mc.getNetworkHandler().sendChatMessage(macro.getText());
            }
         }
      }
   }

   @EventTarget
   public void onRender(EventHudRender e) {
      Javelin.getInstance().getThemeManager().getCurrentTheme().getAnimation().update(1.0F);

      for (Module module : modules) {
         module.getAnimation().update(module.isEnabled());

         for (Setting setting : module.getSettings()) {
            if (setting instanceof BooleanSetting booleanSetting) {
               booleanSetting.getAnimation().update(booleanSetting.isEnabled());
            } else if (setting instanceof ModeSetting modeSetting) {
               for (ModeSetting.Value value : modeSetting.getValues()) {
                  value.getAnimation().update(value.isSelected());
               }
            } else if (setting instanceof MultiBooleanSetting multiBooleanSetting) {
               for (MultiBooleanSetting.Value value : multiBooleanSetting.getBooleanSettings()) {
                  value.getAnimation().update(value.isEnabled());
               }
            }
         }
      }

      MenuScreen menuScreen = Javelin.getInstance().getMenuScreen();
      if (menuScreen.needToClose) {
         if (menuScreen.savedRunnable != null) {
            menuScreen.savedRunnable.run();
         }

         if (menuScreen.openAnimationMetanoise.getValue() <= 0.27F) {
            menuScreen.savedRunnable = null;
            menuScreen.needToClose = false;
            menuScreen.openAnimationMetanoise.setValue(0.0F);
            menuScreen.openAnimationMetanoise.setStartValue(0.0F);
         }
      }
   }

   @EventTarget
   private void onPacket(EventPacket e) {
      if (e.getPacket() instanceof CloseScreenS2CPacket && mc.currentScreen instanceof MenuScreen) {
         e.cancel();
      }
   }

   @EventTarget
   private void onGameUpdate(EventGameUpdate e) {
      if (mc.player == null) return;

      if (!Aura.INSTANCE.isEnabled() || Aura.INSTANCE.getTarget() == null) {
         float cameraYaw = mc.gameRenderer.getCamera().getYaw();
         float cameraPitch = mc.gameRenderer.getCamera().getPitch();

         if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            Aura.INSTANCE.lastYaw = cameraYaw - 180.0F;
            Aura.INSTANCE.lastPitch = -cameraPitch;
         } else {
            Aura.INSTANCE.lastYaw = cameraYaw;
            Aura.INSTANCE.lastPitch = cameraPitch;
         }

         if (Aura.INSTANCE.rotationMode.is("Vanilla")) {
            return;
         }

         Rotation current = new Rotation(mc.player.getYaw(), mc.player.getPitch());
         float deltaYaw = MathHelper.wrapDegrees(cameraYaw - current.getYaw());
         float deltaPitch = cameraPitch - current.getPitch();

         if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
            deltaYaw = MathHelper.wrapDegrees(cameraYaw - 180.0F - current.getYaw());
            deltaPitch = -cameraPitch - current.getPitch();
         }

         acceleration += 0.0024F;
         float smooth = MathHelper.clamp(acceleration, 0.0F, 1.0F);
         float newYaw = current.getYaw() + deltaYaw * smooth;
         float newPitch = current.getPitch() + deltaPitch * (smooth / 2.0F);

         Rotation smoothRot = new Rotation(newYaw, newPitch);
         RotationComponent.update(smoothRot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 2, false);
      }
   }


   public List<Module> getModules() {
      return modules;
   }

   public boolean isBack() {
      return isBack;
   }

   public boolean isRotated() {
      return isRotated;
   }

   public float getAcceleration() {
      return acceleration;
   }

   public void setBack(boolean isBack) {
      this.isBack = isBack;
   }

   public void setRotated(boolean isRotated) {
      this.isRotated = isRotated;
   }

   public void setAcceleration(float acceleration) {
      this.acceleration = acceleration;
   }
}
