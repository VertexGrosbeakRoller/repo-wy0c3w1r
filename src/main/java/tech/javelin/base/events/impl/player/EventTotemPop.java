package tech.javelin.base.events.impl.player;

import com.darkmagician6.eventapi.events.Event;
import lombok.Generated;
import net.minecraft.entity.player.PlayerEntity;

public final class EventTotemPop implements Event {
   private final PlayerEntity player;

   @Generated
   public PlayerEntity getPlayer() {
      return this.player;
   }

   @Generated
   public EventTotemPop(PlayerEntity player) {
      this.player = player;
   }
}
