package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import java.util.Objects;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.util.Hand;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventSlowWalking;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.utility.game.player.PlayerIntersectionUtil;

@ModuleAnnotation(
   name = "NoSlow",
   category = Category.MOVEMENT,
   description = "Убирает замедление во время еды/использования предметов"
)
public final class NoSlow extends Module {
   public static final NoSlow INSTANCE = new NoSlow();
   private final ModeSetting mode = new ModeSetting("Мод", new String[0]);
   private final ModeSetting.Value grimNew;
   private final ModeSetting.Value grimOld;
   private final ModeSetting.Value vanilla;
   private final ModeSetting.Value switchSlot;
   private final ModeSetting.Value noPacket;
   private BooleanSetting sprint;
   private int ticks;

   private NoSlow() {
      this.grimNew = new ModeSetting.Value(this.mode, "Grim New");
      this.grimOld = (new ModeSetting.Value(this.mode, "Grim Old")).select();
      this.vanilla = new ModeSetting.Value(this.mode, "Vanilla");
      this.switchSlot = new ModeSetting.Value(this.mode, "SwitchSlot");
      this.noPacket = new ModeSetting.Value(this.mode, "NoPacket");
      ModeSetting.Value var10005 = this.grimOld;
      Objects.requireNonNull(var10005);
      this.sprint = new BooleanSetting("Спринт", true, var10005::isSelected);
      this.ticks = 0;
   }

   @EventTarget
   @Native
   public void onItemUse(EventSlowWalking e) {
      if (mc.player == null) return;
      
      if (this.grimNew.isSelected() && mc.player.getItemUseTime() % 2 == 0) {
         e.setCancelled(true);
      }

      if (this.grimOld.isSelected()) {
         Hand hand = mc.player.getActiveHand();
         if (this.sprint.isEnabled()) {
            mc.player.setSprinting(mc.player.canSprint() && mc.player.isWalking() && !mc.player.isBlind() && !mc.player.isGliding() && (!mc.player.shouldSlowDown() || mc.player.isSubmergedInWater()));
         }
         PlayerIntersectionUtil.useItem(hand.equals(Hand.MAIN_HAND) ? Hand.OFF_HAND : Hand.MAIN_HAND);
         e.setCancelled(true);
      }
      
      if (this.vanilla.isSelected()) {
         e.setCancelled(true);
      }
      
      if (this.switchSlot.isSelected()) {
         if (mc.player.isUsingItem() && mc.player.getItemUseTime() > 0) {
            int currentSlot = mc.player.getInventory().selectedSlot;
            int switchTo = (currentSlot + 1) % 9;
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(switchTo));
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(currentSlot));
            e.setCancelled(true);
         }
      }
      
      if (this.noPacket.isSelected()) {
         e.setCancelled(true);
      }
   }

   @EventTarget
   public void update(EventUpdate tickEvent) {
      if (mc.player == null) return;
      if (!mc.player.isUsingItem() || !mc.player.isOnGround()) {
         this.ticks = 0;
      }
   }
}
