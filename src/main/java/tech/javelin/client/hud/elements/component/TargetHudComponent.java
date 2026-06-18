package tech.javelin.client.hud.elements.component;

import java.util.Iterator;
import java.util.List;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.animations.base.Animation;
import tech.javelin.base.animations.base.Easing;
import tech.javelin.base.font.Font;
import tech.javelin.base.font.Fonts;
import tech.javelin.base.font.MsdfRenderer;
import tech.javelin.base.theme.Theme;
import tech.javelin.client.hud.elements.draggable.DraggableHudElement;
import tech.javelin.client.modules.impl.combat.Aura;
import tech.javelin.client.modules.impl.misc.NameProtect;
import tech.javelin.client.modules.impl.misc.ScoreboardHealth;
import tech.javelin.utility.game.player.PlayerIntersectionUtil;
import tech.javelin.utility.mixin.accessors.DrawContextAccessor;
import tech.javelin.utility.render.display.StencilUtil;
import tech.javelin.utility.render.display.base.BorderRadius;
import tech.javelin.utility.render.display.base.CustomDrawContext;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

public class TargetHudComponent extends DraggableHudElement {
   private final Animation healthAnimation;
   private final Animation outdatedHealthAnimation;
   private final Animation gappleAnimation;
   private final Animation toggleAnimation;
   private final Animation toggleAnimationMetanoise;
   private LivingEntity target;

   private final float blurStrength = 15.0f;
   private final float cornerRadius = 5.0f;

   public TargetHudComponent(String name, float initialX, float initialY, float windowWidth, float windowHeight, float offsetX, float offsetY, DraggableHudElement.Align align) {
      super(name, initialX, initialY, windowWidth, windowHeight, offsetX, offsetY, align);
      this.healthAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.outdatedHealthAnimation = new Animation(650L, Easing.CUBIC_OUT);
      this.gappleAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.toggleAnimation = new Animation(250L, Easing.CUBIC_OUT);
      this.toggleAnimationMetanoise = new Animation(1850L, Easing.CUBIC_OUT);
   }


   private void drawBlurBackground(CustomDrawContext ctx, float x, float y, float width, float height, Theme theme, float animation) {

      DrawUtil.drawBlur(
              ctx.getMatrices(), x, y, width, height,
              blurStrength,
              BorderRadius.all(cornerRadius),
              new ColorRGBA(255, 255, 255, (int)(animation * 255))
      );


      ColorRGBA themeColor = theme.getColor();
      ColorRGBA backgroundColor = new ColorRGBA(
              (int) (Math.min(255, Math.max(0, themeColor.getRed() * 0.25f))),
              (int) (Math.min(255, Math.max(0, themeColor.getGreen() * 0.25f))),
              (int) (Math.min(255, Math.max(0, themeColor.getBlue() * 0.25f))),
              (int)(64 * animation)
      );

      DrawUtil.drawRoundedRect(
              ctx.getMatrices(), x, y, width, height,
              BorderRadius.all(cornerRadius),
              backgroundColor
      );
   }

   @Native
   public void render(CustomDrawContext ctx) {
      Aura aura = Aura.INSTANCE;
      LivingEntity target = mc.currentScreen instanceof ChatScreen ? mc.player : aura.getTarget();
      this.setTarget((LivingEntity)target);
      if (this.target != null && this.toggleAnimation.getValue() > 0.0F) {
         this.renderTargetHud(ctx, this.target, this.toggleAnimation.getValue());
      }
   }

   @Native
   private void renderTargetHud(CustomDrawContext ctx, LivingEntity target, float animation) {
      float posX = this.x;
      float posY = this.y;
      float width = 100.5F;
      float height = 40.0F;
      Theme theme = Javelin.getInstance().getThemeManager().getCurrentTheme();
      float hp = ScoreboardHealth.INSTANCE.isEnabled() ? PlayerIntersectionUtil.getHealth(target) : target.getHealth();
      this.healthAnimation.update(hp / target.getMaxHealth());
      if (this.outdatedHealthAnimation.getValue() < this.healthAnimation.getValue()) {
         this.outdatedHealthAnimation.setValue(this.healthAnimation.getValue());
         this.outdatedHealthAnimation.setStartValue(this.healthAnimation.getValue());
      } else {
         this.outdatedHealthAnimation.update(hp / target.getMaxHealth());
      }

      this.gappleAnimation.update(target.getAbsorptionAmount() / target.getMaxHealth());


      drawBlurBackground(ctx, posX, posY, width, height, theme, animation);


      Identifier skinTextures = null;
      Iterator var11 = mc.getNetworkHandler().getPlayerList().iterator();

      while(var11.hasNext()) {
         PlayerListEntry playerListEntry = (PlayerListEntry)var11.next();
         if (playerListEntry.getProfile().getName().equals(target.getNameForScoreboard())) {
            skinTextures = playerListEntry.getSkinTextures().texture();
         }
      }

      if (skinTextures == null) {
         skinTextures = DefaultSkinHelper.getSteve().texture();
      }

      float headSize = 32.0F;
      DrawUtil.drawPlayerHeadWithRoundedShader(ctx.getMatrices(), skinTextures, posX + 4.0F, posY + 4.0F, headSize,
              BorderRadius.all(3.0F), ColorRGBA.WHITE.withAlpha(animation * 255.0F));


      String playerName = target == mc.player ? NameProtect.getCustomName() : target.getNameForScoreboard();
      if (playerName.length() > 11) {
         playerName = playerName.substring(0, 7) + "...";
      }
      ctx.drawText(Fonts.SEMIBOLD.getFont(8.0F), playerName, posX + headSize + 8.0F, posY + 6.5F,
              ColorRGBA.WHITE.withAlpha(animation * 255.0F));


      String hpText = "HP: " + String.format("%.1f", hp).replace(",", ".") +
              (target.getAbsorptionAmount() > 0.0F ? " (" + String.format("%.1f", target.getAbsorptionAmount()).replace(",", ".") + ")" : "");
      ctx.drawText(Fonts.SEMIBOLD.getFont(6.5F), hpText, posX + headSize + 8.0F, posY + 17.5F,
              ColorRGBA.WHITE.withAlpha(animation * 255.0F));


      float barX = posX + headSize + 7.2F;
      float barY = posY + 27.8F;
      float barWidth = width - headSize - 12.0F;
      float barHeight = 7.4F;


      DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, barWidth, barHeight, BorderRadius.all(3.0F),
              new ColorRGBA(50, 50, 50, (int)(animation * 150)));


      float healthWidth = MathHelper.clamp(barWidth * this.healthAnimation.getValue(), 0.0F, barWidth);
      if (healthWidth > 0) {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, healthWidth, barHeight, BorderRadius.all(3.0F),
                 theme.getSecondColor().withAlpha((int)(animation * 255)),
                 theme.getSecondColor().withAlpha((int)(animation * 255)),
                 theme.getColor().withAlpha((int)(animation * 255)),
                 theme.getColor().withAlpha((int)(animation * 255)));
      }

      float absorptionWidth = MathHelper.clamp(barWidth * this.gappleAnimation.getValue(), 0.0F, barWidth);
      if (absorptionWidth > 0) {
         DrawUtil.drawRoundedRect(ctx.getMatrices(), barX, barY, absorptionWidth, barHeight, BorderRadius.all(3.0F),
                 new ColorRGBA(255, 220, 0, (int)(animation * 255)));
      }

      if (target instanceof PlayerEntity) {
         this.drawArmor(ctx, (PlayerEntity)target, posX + width - 65.0F, posY - 12.0F, 0.0F, 0.0F, 0.0F);
      }

      this.width = width;
      this.height = height;
   }

   private void drawArmor(CustomDrawContext ctx, PlayerEntity player, float posX, float posY, float headSize, float padding, float fontSize) {
      float boxSizeItem = 10.0F;
      float paddingItem = 0.0F;
      float iconX = posX + (5.0F - this.toggleAnimation.getValue() * 5.0F);
      float iconY = posY + 1.0F + (5.0F - this.toggleAnimation.getValue() * 5.0F);
      List<ItemStack> armor = player.getInventory().armor;
      ItemStack[] items = new ItemStack[]{player.getMainHandStack(), player.getOffHandStack(), (ItemStack)armor.get(3), (ItemStack)armor.get(2), (ItemStack)armor.get(1), (ItemStack)armor.get(0)};
      Font font = Fonts.MEDIUM.getFont(5.0F);
      ItemStack[] var15 = items;
      int var16 = items.length;

      for(int var17 = 0; var17 < var16; ++var17) {
         ItemStack stack = var15[var17];
         if (!stack.isEmpty()) {
            ctx.getMatrices().push();
            ctx.getMatrices().translate((double)iconX + ((double)boxSizeItem - 9.6D) / 2.0D, (double)iconY + ((double)boxSizeItem - 9.6D) / 2.0D, 0.0D);
            ctx.getMatrices().scale(0.6F * this.toggleAnimation.getValue(), 0.6F * this.toggleAnimation.getValue(), 0.6F * this.toggleAnimation.getValue());
            ctx.drawItem(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawItemBar(stack, 0, 0);
            ((DrawContextAccessor)ctx).callDrawCooldownProgress(stack, 0, 0);
            ctx.getMatrices().pop();
            iconX += boxSizeItem + paddingItem;
         }
      }
   }

   public void setTarget(LivingEntity target) {
      if (target == null) {
         this.toggleAnimation.update(0.0F);
         this.toggleAnimationMetanoise.update(0.0F);
         this.toggleAnimationMetanoise.setDuration(2200L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         if (this.toggleAnimationMetanoise.getValue() == 0.0F) {
            this.target = null;
         }
      } else {
         this.target = target;
         this.toggleAnimationMetanoise.update(1.0F);
         this.toggleAnimationMetanoise.setDuration(1300L);
         this.toggleAnimationMetanoise.setEasing(Easing.CIRC_OUT);
         this.toggleAnimation.update(1.0F);
      }

   }
}