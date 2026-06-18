package tech.javelin.utility.mixin.minecraft.entity.projectile;

import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import tech.javelin.client.modules.impl.combat.SuperBow;

@Mixin({PersistentProjectileEntity.class})
public class PersistentProjectileEntityMixin {
   
   @ModifyVariable(
      method = {"setVelocity"},
      at = @At("HEAD"),
      ordinal = 0,
      argsOnly = true
   )
   private double modifyVelocityX(double x) {
      SuperBow superBow = SuperBow.INSTANCE;
      if (superBow != null && superBow.isEnabled()) {
         return x * superBow.getVelocityMultiplier();
      }
      return x;
   }
   
   @ModifyVariable(
      method = {"setVelocity"},
      at = @At("HEAD"),
      ordinal = 1,
      argsOnly = true
   )
   private double modifyVelocityY(double y) {
      SuperBow superBow = SuperBow.INSTANCE;
      if (superBow != null && superBow.isEnabled()) {
         return y * superBow.getVelocityMultiplier();
      }
      return y;
   }
   
   @ModifyVariable(
      method = {"setVelocity"},
      at = @At("HEAD"),
      ordinal = 2,
      argsOnly = true
   )
   private double modifyVelocityZ(double z) {
      SuperBow superBow = SuperBow.INSTANCE;
      if (superBow != null && superBow.isEnabled()) {
         return z * superBow.getVelocityMultiplier();
      }
      return z;
   }
}
