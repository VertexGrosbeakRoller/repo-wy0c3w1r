package tech.javelin.base.events.impl.player;

import lombok.Generated;
import net.minecraft.util.PlayerInput;
import tech.javelin.base.events.callables.EventCancellable;

public class EventMoveInput extends EventCancellable {
   private PlayerInput input;
   private float forward;
   private float strafe;
   private boolean jump;
   private boolean sneak;

   @Generated
   public PlayerInput getInput() {
      return this.input;
   }

   @Generated
   public float getForward() {
      return this.forward;
   }

   @Generated
   public float getStrafe() {
      return this.strafe;
   }

   @Generated
   public boolean getJump() {
      return this.jump;
   }

   @Generated
   public boolean getSneak() {
      return this.sneak;
   }

   @Generated
   public void setInput(PlayerInput input) {
      this.input = input;
   }

   @Generated
   public void setForward(float forward) {
      this.forward = forward;
   }

   @Generated
   public void setStrafe(float strafe) {
      this.strafe = strafe;
   }

   @Generated
   public void setJump(boolean jump) {
      this.jump = jump;
   }

   @Generated
   public void setSneak(boolean sneak) {
      this.sneak = sneak;
   }

   @Generated
   public EventMoveInput(PlayerInput input, float forward, float strafe) {
      this.input = input;
      this.forward = forward;
      this.strafe = strafe;
      this.jump = input != null ? input.jump() : false;
      this.sneak = input != null ? input.sneak() : false;
   }
}
