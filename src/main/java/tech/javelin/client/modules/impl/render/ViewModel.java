package tech.javelin.client.modules.impl.render;

import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
   name = "ViewModel",
   category = Category.RENDER,
   description = "Настройка позиции"
)
public final class ViewModel extends Module {
   public static final ViewModel INSTANCE = new ViewModel();
   public final NumberSetting leftX = new NumberSetting("Позиция левой руки X", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftY = new NumberSetting("Позиция левой руки Y", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftZ = new NumberSetting("Позиция левой руки Z", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting leftScale = new NumberSetting("Размер левой руки", 1.0F, 0.5F, 1.5F, 0.05F);
   public final NumberSetting rightX = new NumberSetting("Позиция правой руки X", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightY = new NumberSetting("Позиция правой руки Y", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightZ = new NumberSetting("Позиция правой руки Z", 0.0F, -1.0F, 1.0F, 0.1F);
   public final NumberSetting rightScale = new NumberSetting("Размер правой руки", 1.0F, 0.5F, 1.5F, 0.05F);

   private ViewModel() {
   }

   public void applyHandScale(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.scale(this.rightScale.getCurrent(), this.rightScale.getCurrent(), this.rightScale.getCurrent());
         } else {
            matrices.scale(this.leftScale.getCurrent(), this.leftScale.getCurrent(), this.leftScale.getCurrent());
         }
      } else {
         matrices.scale(1.0F, 1.0F, 1.0F);
      }

   }

   public void applyHandPosition(MatrixStack matrices, Arm arm) {
      if (this.isEnabled()) {
         if (arm == Arm.RIGHT) {
            matrices.translate(this.rightX.getCurrent(), this.rightY.getCurrent(), this.rightZ.getCurrent());
         } else {
            matrices.translate(-this.leftX.getCurrent(), this.leftY.getCurrent(), this.leftZ.getCurrent());
         }
      } else {
         matrices.translate(0.0F, 0.0F, 0.0F);
      }

   }
}
