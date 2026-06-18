package tech.javelin.utility.mixin.minecraft.entity.projectile;

import net.minecraft.entity.projectile.ArrowEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tech.javelin.client.modules.impl.combat.SuperBow;

@Mixin({ArrowEntity.class})
public class ArrowEntityMixin {
   
   @ModifyVariable(
      method = {"onHit"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private float modifyDamage(float damage) {
      SuperBow superBow = SuperBow.INSTANCE;
      if (superBow != null && superBow.isEnabled()) {
         return damage * superBow.getDamageMultiplier();
      }
      return damage;
   }
}
