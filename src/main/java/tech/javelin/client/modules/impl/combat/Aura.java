package tech.javelin.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import net.minecraft.client.option.Perspective;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.AmbientEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.passive.FishEntity;
import net.minecraft.entity.passive.PassiveEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket.Mode;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.other.EventGameUpdate;
import tech.javelin.base.events.impl.other.EventTick;
import tech.javelin.base.events.impl.other.EventTickMovement;
import tech.javelin.base.events.impl.player.EventMoveInput;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.base.player.AttackUtil;
import tech.javelin.base.request.ScriptManager;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.MultiBooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.client.modules.impl.movement.AutoSprint;
import tech.javelin.utility.component.RotationComponent;
import tech.javelin.utility.game.player.MovingUtil;
import tech.javelin.utility.game.player.PlayerInventoryUtil;
import tech.javelin.utility.game.player.RaytracingUtil;
import tech.javelin.utility.game.player.rotation.Rotation;
import tech.javelin.utility.game.player.rotation.RotationUtil;
import tech.javelin.utility.game.rotation.NeuroRotationSystem;
import tech.javelin.utility.math.MultipointUtils;
import tech.javelin.utility.math.Timer;
import tech.javelin.utility.predict.PredictUtils;

@ModuleAnnotation(
        name = "Aura",
        category = Category.COMBAT,
        description = "Автоматически бьет цель"
)
public final class Aura extends Module {
   public static final Aura INSTANCE = new Aura();
   private final MultiBooleanSetting targetTypeSetting = MultiBooleanSetting.create("Атаковать", List.of("Игроков", "Мобов", "Животных", "Голых игроков", "Друзей"));
   public final ModeSetting rotationMode = new ModeSetting("Ротация", new String[0]);
   private final ModeSetting.Value hvh;
   private final ModeSetting.Value lonyJir;
   private final ModeSetting.Value cake;
   private final ModeSetting.Value legendsGrief;
   private final ModeSetting correction;
   private final ModeSetting.Value correctionFocus;
   private final ModeSetting.Value correctionGood;
   private final ModeSetting.Value correctionTarget;
   private final ModeSetting.Value correctionNone;
   private final NumberSetting distance;
   private final NumberSetting distanceRotation;
   private final BooleanSetting shieldBreak;
   private final BooleanSetting legitSwap;
   private final BooleanSetting raycastCheck;
   private final BooleanSetting predictOnElytra;
   public final NumberSetting predict;
   public final BooleanSetting critsOnlyWithSpace;
   private final BooleanSetting randomizeHits;
   private LivingEntity target;
   private float acceleration;
   private boolean isBack;
   private float funTimeYawVel;
   private float funTimeShakeStart = -1L;
   private float spookyCirclePhase;
   private float spookyCircleRadius;
   private float spookyCurrentSpeed;
   private float spookyJitterYaw;
   private float spookyJitterPitch;
   private float wellMineAcceleration;
   private boolean wellMineBack;
   private double wellMineOffX, wellMineOffY, wellMineOffZ;
   private float slothVelYaw, slothVelPitch;
   private float slothCurrentYaw, slothCurrentPitch;
   private float slothSmoothYaw, slothSmoothPitch;
   private float slothNoiseAngle;
   private float reallyWorldJitter;
   private float snapYawVel;
   private float snapPitchVel;
   private final Timer hurtTimer;
   private final java.util.Random random = new java.util.Random();
   private int randomDelay = 458;
   private final ScriptManager.ScriptTask script;
   private int lastSlot;
   public float lastYaw;
   public float lastPitch;

   private Aura() {
      this.hvh = new ModeSetting.Value(this.rotationMode, "Vanilla");
      this.lonyJir = (new ModeSetting.Value(this.rotationMode, "LonyGrief")).select();
      this.cake = (new ModeSetting.Value(this.rotationMode, "CakeWorld")).select();
      this.legendsGrief = (new ModeSetting.Value(this.rotationMode, "LegendsGrief")).select();
      new ModeSetting.Value(this.rotationMode, "FunTime");
      new ModeSetting.Value(this.rotationMode, "SpookyTime");
      new ModeSetting.Value(this.rotationMode, "ArtyGrief");
      new ModeSetting.Value(this.rotationMode, "WellMine");
      new ModeSetting.Value(this.rotationMode, "WhiteRise");
      new ModeSetting.Value(this.rotationMode, "Sloth");
      new ModeSetting.Value(this.rotationMode, "ReallyWorld");
      new ModeSetting.Value(this.rotationMode, "Snap");
      new ModeSetting.Value(this.rotationMode, "Neuro");
      new ModeSetting.Value(this.rotationMode, "Legit");
      this.correction = new ModeSetting("Коррекция", new String[0]);
      this.correctionFocus = new ModeSetting.Value(this.correction, "Фокус");
      this.correctionGood = (new ModeSetting.Value(this.correction, "Свободная")).select();
      this.correctionTarget = new ModeSetting.Value(this.correction, "Таргет");
      this.correctionNone = new ModeSetting.Value(this.correction, "Нет");
      this.distance = new NumberSetting("Дистанция", 3.0F, 0.5F, 6.0F, 0.1F, "Дистанция атаки");
      this.distanceRotation = new NumberSetting("Дистанция аима", 0.1F, 0.0F, 6.0F, 0.1F);
      this.shieldBreak = new BooleanSetting("Ломать щит", true);
      BooleanSetting var10005 = this.shieldBreak;
      Objects.requireNonNull(var10005);
      this.legitSwap = new BooleanSetting("Легитно ломать", true, var10005::isEnabled);
      this.raycastCheck = new BooleanSetting("Проверка на наведение", false);
      this.predictOnElytra = new BooleanSetting("Перегонять противника", true);
      this.predict = new NumberSetting("Насколько перегонять", 2.0F, 1.0F, 4.0F, 0.1F);
      this.critsOnlyWithSpace = new BooleanSetting("Только с пробелом", true);
      this.randomizeHits = new BooleanSetting("Рандомизация ударов", false);
      this.target = null;
      this.hurtTimer = new Timer();
      this.script = new ScriptManager.ScriptTask();
      this.lastSlot = -1;
   }

   @Native
   private void breakShieldAndAttack() {
      boolean wasSwapped = false;
      boolean wasSwappedInventory = false;
      int slotHotbar = PlayerInventoryUtil.find((List)List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE), 0, 8);
      int slotInventory = PlayerInventoryUtil.find((List)List.of(Items.WOODEN_AXE, Items.STONE_AXE, Items.IRON_AXE, Items.GOLDEN_AXE, Items.DIAMOND_AXE, Items.NETHERITE_AXE), 8, 35);
      if (slotHotbar != -1 && this.shieldBreak.isEnabled() && this.target.isBlocking()) {
         if (this.legitSwap.isEnabled()) {
            this.lastSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = slotHotbar;
         } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotHotbar));
         }

         wasSwapped = true;
      }

      if (slotHotbar == -1 && slotInventory != -1 && this.shieldBreak.isEnabled() && this.target.isBlocking()) {
         if (this.legitSwap.isEnabled()) {
            mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            this.lastSlot = mc.player.getInventory().selectedSlot;
            mc.player.getInventory().selectedSlot = 8;
         } else {
            mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(slotHotbar));
         }

         wasSwappedInventory = true;
      }

      mc.interactionManager.attackEntity(mc.player, this.target);
      mc.player.swingHand(Hand.MAIN_HAND);
      if (wasSwapped) {
         if (this.legitSwap.isEnabled()) {
            Javelin.getInstance().getScriptManager().addTask(this.script);
            this.script.schedule(EventUpdate.class, (eventUpdate) -> {
               mc.player.getInventory().selectedSlot = this.lastSlot;
               this.lastSlot = -1;
               return true;
            });
         } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
         }
      }

      if (wasSwappedInventory) {
         if (this.legitSwap.isEnabled()) {
            Javelin.getInstance().getScriptManager().addTask(this.script);
            this.script.schedule(EventUpdate.class, (eventUpdate) -> {
               mc.player.getInventory().selectedSlot = this.lastSlot;
               mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
               mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
               this.lastSlot = -1;
               return true;
            });
         } else {
            mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(mc.player.getInventory().selectedSlot));
            mc.interactionManager.clickSlot(0, slotInventory, 8, SlotActionType.SWAP, mc.player);
            mc.getNetworkHandler().sendPacket(new CloseHandledScreenC2SPacket(0));
         }
      }

   }

   @EventTarget
   public void onTick(EventTick e) {
      // Always capture player natural head movements for Legit rotation
      capturePlayerRotation();
      
      if (this.target == null || !this.isValid(this.target)) {
         this.target = this.updateTarget();
      }

      if (this.target != null) {
         if (this.isCanAttack() && this.hurtTimer.finished(this.randomDelay) && !this.target.isBlocking()) {
            if (mc.player.isSprinting() && !mc.player.isOnGround() && !mc.player.isSwimming()) {
               mc.player.setSprinting(false);
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
               if (!AutoSprint.INSTANCE.isEnabled()) {
                  mc.options.sprintKey.setPressed(false);
               }
            }

            mc.interactionManager.attackEntity(mc.player, this.target);
            mc.player.swingHand(Hand.MAIN_HAND);
            this.hurtTimer.reset();
         }

      }
   }

   @EventTarget
   @Native
   public void onTickMovement(EventTickMovement e) {
      if (this.target != null) {
         if (this.target.isBlocking() && this.hurtTimer.finished(50L)) {
            if (mc.player.isSprinting() && !mc.player.isOnGround() && !mc.player.isSwimming()) {
               mc.player.setSprinting(false);
               mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, Mode.STOP_SPRINTING));
               if (!AutoSprint.INSTANCE.isEnabled()) {
                  mc.options.sprintKey.setPressed(false);
               }
            }

            this.breakShieldAndAttack();
            this.hurtTimer.reset();
         }

      }
   }

   @EventTarget
   @Native
   public void eventRotate(EventGameUpdate e) {
      if (this.target != null) {
         Javelin.getInstance().getModuleManager().setAcceleration(0.0F);
         Box box = this.target.getBoundingBox();
         Vec3d eyes = mc.player.getEyePos();
         Vec3d point = this.hvh.isSelected() ? this.target.getBoundingBox().getCenter() : (this.legendsGrief.isSelected() ? MultipointUtils.getNearestPoint(this.target, (double)this.distance.getCurrent()) : MultipointUtils.getMultipoint(this.target, (double)this.distance.getCurrent()));
         if (this.target instanceof PlayerEntity && this.predictOnElytra.isEnabled() && mc.player.isGliding() && this.target.isGliding()) {
            point = PredictUtils.predict(this.target, this.target.getPos(), mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter()) > 8.0D ? 8.0F : this.predict.getCurrent());
         }

         Rotation angle = RotationUtil.fromVec3d(point.subtract(eyes));
         float deltaYaw;
         float deltaPitch;
         float smooth;
         float newYaw;
         if (this.rotationMode.is("FunTime")) {
            this.applyFunTimeRotation(angle);
            return;
         } else if (this.rotationMode.is("SpookyTime")) {
            this.applySpookyTimeRotation(angle, this.target);
            return;
         } else if (this.rotationMode.is("WellMine")) {
            this.applyWellMineRotation(this.target);
            return;
         } else if (this.rotationMode.is("WhiteRise")) {
            this.applyWhiteRiseRotation(angle);
            return;
         } else if (this.rotationMode.is("Sloth")) {
            this.applySlothRotation(angle, this.target, eyes);
            return;
         } else if (this.rotationMode.is("ReallyWorld")) {
            this.applyReallyWorldRotation(this.target);
            return;
         } else if (this.rotationMode.is("Snap")) {
            this.applySnapRotation(angle);
            return;
         } else if (this.rotationMode.is("ArtyGrief")) {
            this.applyArtyGriefRotation(angle);
            return;
         } else if (this.rotationMode.is("Neuro")) {
            this.applyNeuroRotation(angle);
            return;
         } else if (this.rotationMode.is("Legit")) {
            this.applyLegitRotation(angle);
            return;
         }
         if (this.hvh.isSelected()) {
            deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw);
            deltaPitch = angle.getPitch() - this.lastPitch;
            smooth = this.lastYaw + deltaYaw;
            newYaw = this.lastPitch + deltaPitch;
            smooth -= (smooth - this.lastYaw) % Rotation.gcd();
            newYaw -= (newYaw - this.lastPitch) % Rotation.gcd();
            Rotation smoothRot = new Rotation(smooth, newYaw);
            RotationComponent.update(new Rotation(smoothRot.getYaw(), smoothRot.getPitch()), 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);
            this.lastYaw = smoothRot.getYaw();
            this.lastPitch = smoothRot.getPitch();
         }

         Rotation smoothRot;
         float deltaYaw2;
         float deltaPitch2;
         float newPitch;
         if (this.lonyJir.isSelected()) {
            if (mc.player.isGliding()) {
               if (!this.isBack) {
                  this.acceleration += 0.005F;
                  if (this.acceleration >= 0.13F) {
                     this.isBack = true;
                  }
               } else {
                  if (this.acceleration >= -0.02F) {
                     this.acceleration -= 0.005F;
                  }

                  if (this.acceleration <= -0.02F) {
                     this.isBack = false;
                  }
               }
            } else if (!RaytracingUtil.rayTrace(mc.player.getRotationVector(), 1488.0D, this.target.getBoundingBox())) {
               this.acceleration += 0.0015F;
            } else if (this.acceleration > 0.0F) {
               this.acceleration -= 0.01F;
            }

            deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw);
            deltaPitch = angle.getPitch() - this.lastPitch;
            smooth = Math.max(this.acceleration, 0.0F);
            newYaw = this.lastYaw + deltaYaw * Math.min(Math.max(smooth, 0.0F), 1.0F);
            newPitch = this.lastPitch + deltaPitch * Math.min(Math.max(smooth / 2.0F, 0.0F), 1.0F);
            newYaw -= (newYaw - this.lastYaw) % Rotation.gcd();
            newPitch -= (newPitch - this.lastPitch) % Rotation.gcd();
            smoothRot = new Rotation(newYaw, newPitch);
            deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - this.lastYaw);
            deltaPitch2 = mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
            if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
               deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - 180.0F - this.lastYaw);
               deltaPitch2 = -mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
            }

            RotationComponent.update(new Rotation(smoothRot.getYaw(), smoothRot.getPitch()), 360.0F, 360.0F, !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, 0, 1, false);
            this.lastYaw = smoothRot.getYaw();
            this.lastPitch = smoothRot.getPitch();
         }

         if (this.legendsGrief.isSelected() || this.cake.isSelected()) {
            if (mc.player.isGliding() && this.target.isGliding()) {
               if (this.isBack) {
                  if (this.acceleration >= -0.02F) {
                     this.acceleration -= Math.abs(MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw)) > 80.0F ? 0.1F : 0.01F;
                  }

                  if (this.acceleration <= -0.02F) {
                     this.isBack = false;
                  }
               } else {
                  this.acceleration += 0.004F;
                  if (this.acceleration >= 0.17F || RaytracingUtil.rayTrace(mc.player.getRotationVector(), 1488.0D, box.offset(mc.player.isGliding() && this.target instanceof PlayerEntity && this.target.isGliding() ? PredictUtils.predict(this.target, this.target.getPos(), this.predict.getCurrent()) : Vec3d.ZERO))) {
                     this.isBack = true;
                  }
               }
            } else if (this.isBack) {
               if (this.acceleration >= -0.01F) {
                  this.acceleration -= Math.abs(MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw)) > 80.0F ? 0.1F : 0.01F;
               }

               if (this.acceleration <= -0.01F) {
                  this.isBack = false;
               }
            } else {
               this.acceleration += 0.004F;
               if (this.acceleration >= 0.18F || RaytracingUtil.rayTrace(mc.player.getRotationVector(), 999.0D, box.offset(mc.player.isGliding() && this.target instanceof PlayerEntity && this.target.isGliding() ? PredictUtils.predict(this.target, this.target.getPos(), this.predict.getCurrent()) : Vec3d.ZERO).expand(-0.5D))) {
                  this.isBack = true;
               }
            }

            deltaYaw = MathHelper.wrapDegrees(angle.getYaw() - this.lastYaw);
            deltaPitch = angle.getPitch() - this.lastPitch;
            smooth = Math.max(this.acceleration, 0.0F);
            newYaw = this.lastYaw + deltaYaw * Math.min(Math.max(smooth, 0.0F), 1.0F);
            newPitch = this.lastPitch + deltaPitch * Math.min(Math.max(smooth / 2.0F, 0.0F), 1.0F);
            newYaw -= (newYaw - this.lastYaw) % Rotation.gcd();
            newPitch -= (newPitch - this.lastPitch) % Rotation.gcd();
            smoothRot = new Rotation(newYaw, newPitch);
            deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - this.lastYaw);
            deltaPitch2 = mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
            if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
               deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - 180.0F - this.lastYaw);
               deltaPitch2 = -mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
            }

            RotationComponent.update(new Rotation(smoothRot.getYaw(), smoothRot.getPitch()), 360.0F, 360.0F, !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, 0, 1, false);
            this.lastYaw = smoothRot.getYaw();
            this.lastPitch = smoothRot.getPitch();
         }

      }
   }

   private boolean isCanAttack() {
      if (mc.player.getAttackCooldownProgress(0.5F) < 0.9F) {
         return false;
      } else if (!AttackUtil.canAttack()) {
         return false;
      } else if (this.target instanceof PlayerEntity && this.predictOnElytra.isEnabled() && mc.player.isGliding() && this.target.isGliding() && mc.player.getEyePos().distanceTo(PredictUtils.predict(this.target, this.target.getPos(), this.predict.getCurrent())) > 3.0D && mc.player.getEyePos().distanceTo(this.target.getBoundingBox().getCenter()) > 3.0D) {
         return false;
      } else if ((!mc.player.isGliding() || !this.target.isGliding()) && mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(this.target, (double)this.distance.getCurrent())) > (double)this.distance.getCurrent()) {
         return false;
      } else {
         return !this.raycastCheck.isEnabled() || RaytracingUtil.rayTrace(mc.player.getRotationVector(), (double)this.distance.getCurrent(), this.target.getBoundingBox()) || mc.targetedEntity != null || mc.player.isGliding() || this.target.isGliding();
      }
   }

   private LivingEntity updateTarget() {
      List<LivingEntity> targets = new ArrayList();
      Iterator var2 = mc.world.getEntities().iterator();

      while(var2.hasNext()) {
         Entity entity = (Entity)var2.next();
         if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            if (this.isValid(living)) {
               targets.add(living);
            }
         }
      }

      if (!targets.isEmpty() && this.isEnabled()) {
         targets.sort(Comparator.comparingDouble((entityx) -> {
            Rotation vec = Rotation.getRotations(entityx.getBoundingBox().getCenter());
            double dy = (double)Math.abs(MathHelper.wrapDegrees(vec.getYaw() - mc.player.getYaw()));
            double dp = (double)Math.abs(MathHelper.wrapDegrees(vec.getPitch() - mc.player.getPitch()));
            return dy + dp;
         }));
         return targets.isEmpty() ? null : (LivingEntity)targets.get(0);
      } else {
         return null;
      }
   }

   public boolean isValid(LivingEntity entity) {
      if (entity == mc.player) {
         return false;
      } else if (entity.isAlive() && !(entity.getHealth() <= 0.0F)) {
         if (mc.player.isAlive() && !(mc.player.getHealth() <= 0.0F)) {
            if (entity instanceof PlayerEntity) {
               PlayerEntity player = (PlayerEntity)entity;
               
               // Check if friend - skip unless "Друзей" is enabled
               if (Javelin.getInstance().getFriendManager().isFriend(entity.getName().getString())) {
                  if (!this.targetTypeSetting.isEnable("Друзей")) {
                     return false;
                  }
               }

               if (AntiBot.INSTANCE.isBot(player)) {
                  return false;
               }
               
               // Check for naked players (no armor)
               boolean hasArmor = !player.getInventory().getArmorStack(0).isEmpty() 
                  || !player.getInventory().getArmorStack(1).isEmpty()
                  || !player.getInventory().getArmorStack(2).isEmpty() 
                  || !player.getInventory().getArmorStack(3).isEmpty();
               
               boolean attackNaked = this.targetTypeSetting.isEnable("Голых игроков") && !hasArmor;
               boolean attackRegular = this.targetTypeSetting.isEnable("Игроков");
               
               // Attack if:
               // 1. Player is naked AND naked setting is enabled, OR
               // 2. Regular players setting is enabled (attacks all players regardless of armor)
               if (!attackNaked && !attackRegular) {
                  return false; // Neither condition met
               }
               // If we get here, we should attack this player
            }

            if (!(entity instanceof PassiveEntity) && !(entity instanceof FishEntity) || this.targetTypeSetting.isEnable("Животных") && !Javelin.getInstance().getServerHandler().isPvp()) {
               if (!(entity instanceof HostileEntity) && !(entity instanceof AmbientEntity) || this.targetTypeSetting.isEnable("Мобов") && !Javelin.getInstance().getServerHandler().isPvp()) {
                  if (mc.player.getEyePos().distanceTo(MultipointUtils.getNearestPoint(entity, (double)(this.distance.getCurrent() + this.distanceRotation.getCurrent()))) > (double)(mc.player.isGliding() ? 20.0F : this.distance.getCurrent() + this.distanceRotation.getCurrent())) {
                     return false;
                  } else {
                     return !(entity instanceof ArmorStandEntity);
                  }
               } else {
                  return false;
               }
            } else {
               return false;
            }
         } else {
            return false;
         }
      } else {
         return false;
      }
   }

   @EventTarget
   private void setCorrection(EventMoveInput eventMoveInput) {
      if (!this.correctionNone.isSelected() && this.target != null) {
         if (this.correctionFocus.isSelected()) {
            MovingUtil.fixMovementFocus(eventMoveInput, mc.player.getYaw());
         } else if (this.correctionTarget.isSelected()) {
            // Блокируем клавиши - принудительное движение к цели
            eventMoveInput.setForward(1.0F);
            eventMoveInput.setStrafe(0.0F);
            eventMoveInput.setJump(false);
            eventMoveInput.setSneak(false);
            mc.player.setSprinting(true);
            // Сбрасываем нажатия клавиш
            mc.options.forwardKey.setPressed(true);
            mc.options.backKey.setPressed(false);
            mc.options.leftKey.setPressed(false);
            mc.options.rightKey.setPressed(false);
         } else {
            MovingUtil.fixMovementFree(eventMoveInput);
         }

      }
   }

   private void applySnapRotation(Rotation targetAngle) {
      float speed = 0.35f;
      float yawDelta   = MathHelper.wrapDegrees(targetAngle.getYaw()   - this.lastYaw);
      float pitchDelta = targetAngle.getPitch() - this.lastPitch;
      float newYaw   = this.lastYaw   + yawDelta   * speed;
      float newPitch = this.lastPitch + pitchDelta * speed;
      newYaw   -= (newYaw   - this.lastYaw)   % Rotation.gcd();
      newPitch -= (newPitch - this.lastPitch) % Rotation.gcd();
      Rotation rot = new Rotation(newYaw, MathHelper.clamp(newPitch, -89f, 89f));
      RotationComponent.update(rot, 360f, 360f, 360f, 360f, 0, 1, false);
      this.lastYaw   = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   // FunTime rotation static state
   private static int funTimeHitCounter = 0;
   private static long funTimeLastHitTime = 0;
   private static boolean funTimeWasAttacking = false;

   private void applyFunTimeRotation(Rotation targetAngle) {
      boolean canAttack = mc.player.getAttackCooldownProgress(0.5F) >= 0.9F;
      long nowMs = System.currentTimeMillis();
      
      float currentYaw = this.lastYaw;
      float currentPitch = this.lastPitch;
      float targetYaw = targetAngle.getYaw();
      float targetPitch = targetAngle.getPitch();
      
      float deltaYaw = wrapTo180(targetYaw - currentYaw);
      float deltaPitch = wrapTo180(targetPitch - currentPitch);
      float total = (float) Math.hypot(deltaYaw, deltaPitch);

      // Защита от деления на ноль (NaN)
      if (total < 0.001f) {
         return;
      }

      // cap 130° по прямой
      float maxStepYaw = (Math.abs(deltaYaw) / total) * 130f;
      float maxStepPitch = (Math.abs(deltaPitch) / total) * 130f;

      float stepYaw = clamp(deltaYaw, -maxStepYaw, maxStepYaw);
      float stepPitch = clamp(deltaPitch, -maxStepPitch, maxStepPitch);

      float nextYaw = currentYaw + stepYaw;
      float nextPitch = currentPitch + stepPitch;

      // Детект реального удара (Rising Edge)
      boolean isNewHit = canAttack && !funTimeWasAttacking;
      if (isNewHit) {
         funTimeHitCounter++;
         funTimeLastHitTime = nowMs;
      }
      funTimeWasAttacking = canAttack; // Обновляем стейт на следующий тик

      if (canAttack) {
         // attack: сглаживание 0.85
         nextYaw = lerp(0.85f, currentYaw, nextYaw);
         nextPitch = lerp(0.85f, currentPitch, nextPitch);

         // Флик вниз каждый 86-й хит в окне 250 мс
         // Условие hitCounter > 0 предотвращает флик на самом первом ударе
         if (isNewHit && funTimeHitCounter % 86 == 0 && (nowMs - funTimeLastHitTime) < 250) {
            nextPitch = -90f; // Свинг должен быть вызван снаружи
         }
        
      } else {
         // idle shake
         long sinceLastHit = nowMs - funTimeLastHitTime;
        
         if (sinceLastHit >= 535) {
            // Лимит 45°, применяем джиттер
            float shakeYaw = (18f + (float) Math.random() * 10f) * (float) Math.sin(nowMs / 60.0);
            float shakePitch = (6f + (float) Math.random() * 10f) * (float) Math.cos(nowMs / 60.0);
          
            nextYaw = clamp(currentYaw + shakeYaw, currentYaw - 45f, currentYaw + 45f);
            nextPitch = clamp(currentPitch + shakePitch, currentPitch - 45f, currentPitch + 45f);
         } else {
            // Лимит 0° - жестко держим натуральный взгляд
            nextYaw = currentYaw;
            nextPitch = currentPitch;
         }
      }

      // Глобальный кламп pitch ДО GCD-снапа (иначе бан за illegal pitch)
      nextPitch = clamp(nextPitch, -89f, 90f);

      // GCD Snap
      float gcd = Rotation.gcd();
      nextYaw -= (nextYaw - currentYaw) % gcd;
      nextPitch -= (nextPitch - currentPitch) % gcd;

      Rotation rot = new Rotation(nextYaw, nextPitch);
      RotationComponent.update(rot, 360.0F, 360.0F, 360.0F, 360.0F, 0, 1, false);
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }
   
   private float wrapTo180(float v) {
      v %= 360;
      if (v >= 180) v -= 360;
      if (v < -180) v += 360;
      return v;
   }
   
   private float clamp(float v, float min, float max) {
      return Math.min(max, Math.max(min, v));
   }
   
   private float lerp(float t, float a, float b) {
      return a + t * (b - a);
   }

   private void applySpookyTimeRotation(Rotation targetAngle, LivingEntity entity) {
      // SpookyTime как LonyGrief с легкой рандомной тряской
      if (mc.player.isGliding()) {
         if (!this.isBack) {
            this.acceleration += 0.005F;
            if (this.acceleration >= 0.13F) {
               this.isBack = true;
            }
         } else {
            if (this.acceleration >= -0.02F) {
               this.acceleration -= 0.005F;
            }
            if (this.acceleration <= -0.02F) {
               this.isBack = false;
            }
         }
      } else if (!RaytracingUtil.rayTrace(mc.player.getRotationVector(), 1488.0D, this.target.getBoundingBox())) {
         this.acceleration += 0.0015F;
      } else if (this.acceleration > 0.0F) {
         this.acceleration -= 0.01F;
      }
      
      // Легкая тряска головы - рандомизированная
      float shakeYaw = (random.nextFloat() - 0.5f) * 1.5f; // ±0.75 градуса
      float shakePitch = (random.nextFloat() - 0.5f) * 1.0f; // ±0.5 градуса
      
      float deltaYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw);
      float deltaPitch = targetAngle.getPitch() - this.lastPitch;
      float smooth = Math.max(this.acceleration, 0.0F);
      
      float newYaw = this.lastYaw + deltaYaw * Math.min(Math.max(smooth, 0.0F), 1.0F) + shakeYaw;
      float newPitch = this.lastPitch + deltaPitch * Math.min(Math.max(smooth / 2.0F, 0.0F), 1.0F) + shakePitch;
      
      newYaw -= (newYaw - this.lastYaw) % Rotation.gcd();
      newPitch -= (newPitch - this.lastPitch) % Rotation.gcd();
      
      Rotation smoothRot = new Rotation(newYaw, newPitch);
      float deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - this.lastYaw);
      float deltaPitch2 = mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
      
      if (mc.options.getPerspective() == Perspective.THIRD_PERSON_FRONT) {
         deltaYaw2 = MathHelper.wrapDegrees(mc.gameRenderer.getCamera().getYaw() - 180.0F - this.lastYaw);
         deltaPitch2 = -mc.gameRenderer.getCamera().getPitch() - this.lastPitch;
      }
      
      RotationComponent.update(new Rotation(smoothRot.getYaw(), smoothRot.getPitch()), 360.0F, 360.0F, 
         !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, 
         !(Math.abs(deltaYaw2) > 3.0F) && !(Math.abs(deltaPitch2) > 3.0F) ? 360.0F : 0.0F, 0, 1, false);
      
      this.lastYaw = smoothRot.getYaw();
      this.lastPitch = smoothRot.getPitch();
   }

   private void applyWellMineRotation(LivingEntity target) {
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d center = target.getBoundingBox().getCenter().add(this.wellMineOffX, this.wellMineOffY, this.wellMineOffZ);
      Vec3d toTarget = center.subtract(eyePos);
      float centerYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
      float centerPitch = (float) (-Math.toDegrees(Math.atan2(toTarget.y, Math.hypot(toTarget.x, toTarget.z))));
      if (this.wellMineBack) {
         float slowdownSpeed = Math.abs(MathHelper.wrapDegrees(centerYaw - this.lastYaw)) > 80.0f ? 0.1f : 0.01f;
         this.wellMineAcceleration -= slowdownSpeed * (0.9f + (float) Math.random() * 0.2f);
         if (this.wellMineAcceleration <= -0.15f) {
            this.wellMineBack = false;
            this.wellMineOffX = (Math.random() - 0.5) * (target.getBoundingBox().maxX - target.getBoundingBox().minX) * 0.15;
            this.wellMineOffY = (Math.random() - 0.5) * (target.getBoundingBox().maxY - target.getBoundingBox().minY) * 0.15;
            this.wellMineOffZ = (Math.random() - 0.5) * (target.getBoundingBox().maxZ - target.getBoundingBox().minZ) * 0.15;
         }
      } else {
         float accelSpeed = 0.0082f + (float)(Math.random() * 0.002 - 0.001);
         this.wellMineAcceleration += accelSpeed;
         float threshold = 0.184f + (float)(Math.random() * 0.03 - 0.015);
         if (this.wellMineAcceleration >= threshold) {
            this.wellMineBack = true;
         }
      }
      float smooth = Math.max(this.wellMineAcceleration, 0.0f);
      float deltaYaw = MathHelper.wrapDegrees(centerYaw - this.lastYaw);
      float deltaPitch = centerPitch - this.lastPitch;
      float humanYawOffset = (float)(Math.sin((double) System.currentTimeMillis() * 0.001) * 0.04);
      float humanPitchOffset = (float)(Math.cos((double) System.currentTimeMillis() * 0.0015) * 0.025);
      float newYaw = this.lastYaw + deltaYaw * MathHelper.clamp(smooth * 1.12f, 0.0f, 1.0f) + humanYawOffset;
      float newPitch = this.lastPitch + deltaPitch * MathHelper.clamp(smooth / 1.88f, 0.0f, 1.0f) + humanPitchOffset;
      float gcd = Rotation.gcd();
      newYaw -= (newYaw - this.lastYaw) % gcd;
      newPitch -= (newPitch - this.lastPitch) % gcd;
      newPitch = MathHelper.clamp(newPitch, -89.0f, 89.0f);
      Rotation rot = new Rotation(newYaw, newPitch);
      RotationComponent.update(rot, 360.0f, 360.0f, 360.0f, 360.0f, 0, 1, false);
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   private void applyWhiteRiseRotation(Rotation targetAngle) {
      float yawDiff = Math.abs(MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw));
      boolean readyToAttack = mc.player.getAttackCooldownProgress(1.0f) > 0.9f;
      float jitterOffset = (float)(Math.sin(System.currentTimeMillis() * 0.00017) * 0.12 + (Math.random() * 0.08 - 0.04)) * 0.7f;
      if (!this.isBack) {
         float gain = 0.0055f;
         if (yawDiff > 60.0f) gain += 0.016f * 1.8f;
         else if (yawDiff > 30.0f) gain += 0.008f * 1.8f;
         else gain += 0.004f * 1.8f;
         if (readyToAttack) gain += 0.018f / 1.4f;
         this.acceleration += gain * (1.6f + jitterOffset);
         if (this.acceleration >= 0.22f) this.isBack = true;
      } else {
         float loss = readyToAttack ? 0.045f : 0.008f;
         this.acceleration -= loss * (2.1f + jitterOffset);
         if (this.acceleration <= -0.04f) this.isBack = false;
      }
      float smooth = MathHelper.clamp(this.acceleration, 0.0f, mc.player.isGliding() ? 0.38f : 0.26f);
      if (readyToAttack) smooth = Math.min(smooth + 0.1f, mc.player.isGliding() ? 0.46f : 0.34f);
      smooth += jitterOffset * 0.5f;
      float deltaYaw = MathHelper.clamp(MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw), -(mc.player.isGliding() ? 42.0f : (readyToAttack ? 28.0f : 20.0f)), mc.player.isGliding() ? 42.0f : (readyToAttack ? 28.0f : 20.0f));
      float deltaPitch = MathHelper.clamp(targetAngle.getPitch() - this.lastPitch, -(mc.player.isGliding() ? 12.0f : (readyToAttack ? 4.5f : 2.8f)), mc.player.isGliding() ? 12.0f : (readyToAttack ? 4.5f : 2.8f));
      float newYaw = this.lastYaw + deltaYaw * (smooth * (0.85f + jitterOffset * 0.4f));
      float newPitch = this.lastPitch + deltaPitch * (smooth * 0.28f);
      float gcd = Rotation.gcd();
      newYaw -= (newYaw - this.lastYaw) % gcd;
      newPitch -= (newPitch - this.lastPitch) % gcd;
      newPitch = MathHelper.clamp(newPitch, -89.0f, 89.0f);
      Rotation rot = new Rotation(newYaw, newPitch);
      RotationComponent.update(rot, 360.0f, 360.0f, 360.0f, 360.0f, 0, 1, false);
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   private float slothSpringInterp(float current, float target, float vel, float stiffness, float damping) {
      float diff = target - current;
      return vel + diff * stiffness - vel * damping;
   }

   private void applySlothRotation(Rotation targetAngle, LivingEntity entity, Vec3d eyePos) {
      float diffYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - this.slothCurrentYaw);
      float diffPitch = targetAngle.getPitch() - this.slothCurrentPitch;
      float totalDiff = (float) Math.sqrt(diffYaw * diffYaw + diffPitch * diffPitch);
      boolean flying = mc.player.isGliding();
      float speedMul = flying ? 0.45f : 1.0f;
      float stiffness = (0.038f + (float) Math.random() * 0.009f) * speedMul;
      float damping = 0.68f + 0.12f * (1.0f - speedMul);
      if (totalDiff > 32.0f) stiffness += 0.018f * speedMul;
      else if (totalDiff < 4.2f) stiffness *= 0.48f;
      float dist = (float) eyePos.distanceTo(entity.getBoundingBox().getCenter());
      stiffness += MathHelper.clamp((dist - 1.6f) / 7.5f, 0.0f, 0.045f) * speedMul;
      this.slothVelYaw = slothSpringInterp(this.slothCurrentYaw, this.slothCurrentYaw + diffYaw, this.slothVelYaw, stiffness, damping);
      this.slothVelPitch = slothSpringInterp(this.slothCurrentPitch, targetAngle.getPitch(), this.slothVelPitch, stiffness * 0.87f, damping);
      float maxVelYaw = 7.5f * speedMul;
      float maxVelPitch = 5.8f * speedMul;
      this.slothVelYaw = MathHelper.clamp(this.slothVelYaw, -maxVelYaw, maxVelYaw);
      this.slothVelPitch = MathHelper.clamp(this.slothVelPitch, -maxVelPitch, maxVelPitch);
      this.slothCurrentYaw += this.slothVelYaw;
      this.slothCurrentPitch += this.slothVelPitch;
      this.slothCurrentPitch = MathHelper.clamp(this.slothCurrentPitch, -89.0f, 89.0f);
      float smoothFactor = flying ? (0.3f + speedMul * 0.4f) : 0.85f;
      this.slothSmoothYaw = this.slothSmoothYaw + MathHelper.wrapDegrees(this.slothCurrentYaw - this.slothSmoothYaw) * smoothFactor;
      this.slothSmoothPitch = this.slothSmoothPitch + (this.slothCurrentPitch - this.slothSmoothPitch) * smoothFactor * 0.95f;
      this.slothNoiseAngle += 0.042f + (float)(Math.random() * 0.018);
      float noiseScale = MathHelper.clamp(dist / 4.5f, 0.25f, 1.0f) * 1.8f;
      float nYaw = ((float)Math.sin(this.slothNoiseAngle * 0.87) * 0.38f + (float)Math.sin(this.slothNoiseAngle * 1.43 + 0.75) * 0.28f) * noiseScale + (float)(Math.random() - 0.5) * noiseScale * 0.13f;
      float nPitch = ((float)Math.cos(this.slothNoiseAngle * 1.18 + 0.35) * 0.32f + (float)Math.cos(this.slothNoiseAngle * 1.76 + 1.42) * 0.23f) * noiseScale * 0.52f;
      float outY = this.slothSmoothYaw + nYaw;
      float outP = MathHelper.clamp(this.slothSmoothPitch + nPitch, -89.0f, 89.0f);
      float gcd = Rotation.gcd();
      outY -= (outY - this.lastYaw) % gcd;
      outP -= (outP - this.lastPitch) % gcd;
      Rotation rot = new Rotation(outY, outP);
      RotationComponent.update(rot, 360.0f, 360.0f, 360.0f, 360.0f, 0, 1, false);
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   private void applyReallyWorldRotation(LivingEntity target) {
      Vec3d eyePos = mc.player.getEyePos();
      Vec3d hipPos = new Vec3d(target.getX(), target.getY() + target.getBoundingBox().getLengthY() * 0.3, target.getZ());
      Vec3d toTarget = hipPos.subtract(eyePos);
      float wantYaw = (float) MathHelper.wrapDegrees(Math.toDegrees(Math.atan2(toTarget.z, toTarget.x)) - 90.0);
      float wantPitch = (float) -Math.toDegrees(Math.atan2(toTarget.y, toTarget.horizontalLength()));
      this.reallyWorldJitter += 0.038f;
      float shakeYaw = (float)(Math.sin(this.reallyWorldJitter * 1.3) * 1.8 + Math.sin(this.reallyWorldJitter * 2.7 + 0.5) * 0.9 + (Math.random() - 0.5) * 0.5);
      float shakePitch = (float)(Math.cos(this.reallyWorldJitter * 1.1 + 0.4) * 0.8 + Math.cos(this.reallyWorldJitter * 2.2 + 1.0) * 0.4 + (Math.random() - 0.5) * 0.25);
      float deltaYaw = MathHelper.wrapDegrees(wantYaw - this.lastYaw);
      float deltaPitch = wantPitch - this.lastPitch;
      float smooth = 0.85f;
      float newYaw = this.lastYaw + deltaYaw * smooth + shakeYaw;
      float newPitch = MathHelper.clamp(this.lastPitch + deltaPitch * smooth * 0.5f + shakePitch, -89.0f, 89.0f);
      float gcd = Rotation.gcd();
      newYaw -= (newYaw - this.lastYaw) % gcd;
      newPitch -= (newPitch - this.lastPitch) % gcd;
      Rotation rot = new Rotation(newYaw, newPitch);
      RotationComponent.update(rot, 360.0f, 360.0f, 360.0f, 360.0f, 0, 1, false);
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   private void applyArtyGriefRotation(Rotation targetAngle) {
      float deltaTime = 0.65f;
      boolean canAtk = mc.player.getAttackCooldownProgress(0.5F) >= 0.9F;
      float jitterAmount = canAtk ? 0.15f : 0.85f;
      float jitterYaw = (float)(Math.sin(System.currentTimeMillis() * 0.005) * jitterAmount);
      float jitterPitch = (float)(Math.cos(System.currentTimeMillis() * 0.007) * jitterAmount * 0.5f);
      float yawDelta = MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw + jitterYaw);
      float pitchDelta = targetAngle.getPitch() - this.lastPitch + jitterPitch;
      float speed = canAtk ? 0.55f : 0.08f;
      float newYaw = this.lastYaw + yawDelta * speed;
      float newPitch = this.lastPitch + pitchDelta * speed;
      newYaw -= (newYaw - this.lastYaw) % Rotation.gcd();
      newPitch -= (newPitch - this.lastPitch) % Rotation.gcd();
      Rotation rot = new Rotation(newYaw, MathHelper.clamp(newPitch, -89f, 89f));
      RotationComponent.update(rot, 360.0f, 360.0f, 360.0f, 360.0f, 0, 1, false);
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   // Neuro rotation - uses recorded datasets to generate human-like rotations
   private NeuroRotationSystem.GeneratedRotation currentNeuroRotation;
   private long lastNeuroUpdate = 0;
   
   private void applyNeuroRotation(Rotation targetAngle) {
      NeuroRotationSystem system = NeuroRotationSystem.getInstance();
      
      // Record sample if recording is active
      if (system.isRecording() && mc.player != null) {
         system.recordSample(
            mc.player.getYaw(),
            mc.player.getPitch(),
            mc.player.getX(),
            mc.player.getY(),
            mc.player.getZ(),
            mc.player.isOnGround(),
            mc.player.handSwinging
         );
      }
      
      // Get the currently loaded dataset (if any)
      NeuroRotationSystem.NeuroDataset dataset = system.getLastLoadedDataset();
      
      if (dataset == null || dataset.getPatterns().isEmpty()) {
         // Fallback to snap rotation if no dataset loaded
         applySnapRotation(targetAngle);
         return;
      }
      
      // Generate rotation based on recorded patterns
      long now = System.currentTimeMillis();
      if (now - lastNeuroUpdate > 50 || currentNeuroRotation == null) {
         currentNeuroRotation = system.generateRotation(
            dataset,
            targetAngle.getYaw(),
            targetAngle.getPitch(),
            this.lastYaw,
            this.lastPitch
         );
         lastNeuroUpdate = now;
      }
      
      if (currentNeuroRotation == null) {
         applySnapRotation(targetAngle);
         return;
      }
      
      // Apply generated rotation with smoothing
      float targetYaw = this.lastYaw + currentNeuroRotation.yawVariation;
      float targetPitch = this.lastPitch + currentNeuroRotation.pitchVariation;
      
      // Clamp pitch
      targetPitch = MathHelper.clamp(targetPitch, -89f, 89f);
      
      // Apply GCD snap
      targetYaw -= (targetYaw - this.lastYaw) % Rotation.gcd();
      targetPitch -= (targetPitch - this.lastPitch) % Rotation.gcd();
      
      Rotation rot = new Rotation(targetYaw, targetPitch);
      RotationComponent.update(rot, 360.0f, 360.0f, 360.0f, 360.0f, 0, 1, false);
      
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   // Legit rotation - replicates player's natural head shake
   private float playerYawHistory[] = new float[10];
   private float playerPitchHistory[] = new float[10];
   private int historyIndex = 0;
   private long lastCaptureTime = 0;
   
   private void capturePlayerRotation() {
      // Capture player's natural movements every 50ms
      long now = System.currentTimeMillis();
      if (now - lastCaptureTime < 50) return;
      lastCaptureTime = now;
      
      if (mc.player == null) return;
      
      playerYawHistory[historyIndex] = mc.player.getYaw();
      playerPitchHistory[historyIndex] = mc.player.getPitch();
      historyIndex = (historyIndex + 1) % 10;
   }
   
   private void applyLegitRotation(Rotation targetAngle) {
      // Use captured player natural movements
      float deltaYaw = MathHelper.wrapDegrees(targetAngle.getYaw() - this.lastYaw);
      float deltaPitch = targetAngle.getPitch() - this.lastPitch;
      
      // Get player's recent natural movement pattern
      float recentYawDelta = 0, recentPitchDelta = 0;
      int validSamples = 0;
      
      // Calculate average movement from last few samples
      for (int i = 1; i < 5 && i < 10; i++) {
         int idx = (historyIndex - i + 10) % 10;
         int prevIdx = (historyIndex - i - 1 + 10) % 10;
         
         float yawDiff = MathHelper.wrapDegrees(playerYawHistory[idx] - playerYawHistory[prevIdx]);
         float pitchDiff = playerPitchHistory[idx] - playerPitchHistory[prevIdx];
         
         // Only count meaningful movements (not the target-following rotations)
         if (Math.abs(yawDiff) < 15f && Math.abs(pitchDiff) < 10f) {
            recentYawDelta += yawDiff;
            recentPitchDelta += pitchDiff;
            validSamples++;
         }
      }
      
      if (validSamples > 0) {
         recentYawDelta /= validSamples;
         recentPitchDelta /= validSamples;
      }
      
      // Base rotation speed
      float speed = 0.25f;
      
      // Apply target rotation + player natural movement pattern
      float newYaw = this.lastYaw + deltaYaw * speed + recentYawDelta * 0.3f;
      float newPitch = this.lastPitch + deltaPitch * speed + recentPitchDelta * 0.3f;
      
      // Clamp
      newPitch = MathHelper.clamp(newPitch, -89f, 89f);
      
      // GCD snap for legit look
      float yawGCD = Rotation.gcd();
      newYaw = Math.round(newYaw / yawGCD) * yawGCD;
      newPitch = Math.round(newPitch / yawGCD) * yawGCD;
      
      Rotation rot = new Rotation(newYaw, newPitch);
      RotationComponent.update(rot, 360.0f, 360.0f, 360.0f, 360.0f, 0, 1, false);
      
      this.lastYaw = rot.getYaw();
      this.lastPitch = rot.getPitch();
   }

   public LivingEntity getTarget() {
      return this.isEnabled() ? this.target : null;
   }

   public void onEnable() {
      this.target = null;
      super.onEnable();
   }

   public void onDisable() {
      Javelin.getInstance().getModuleManager().setAcceleration(0.0F);
      super.onDisable();
   }
}
