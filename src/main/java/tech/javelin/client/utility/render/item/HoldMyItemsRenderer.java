package tech.javelin.client.utility.render.item;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.item.consume.UseAction;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import tech.javelin.client.modules.impl.combat.Aura;
import tech.javelin.client.modules.impl.render.HoldMyItems;

public class HoldMyItemsRenderer {
    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final HoldMyItems hmi = HoldMyItems.INSTANCE;
    private static final double MAX_DELTA = 0.05;
    private static final double ANIMATION_SPEED = 30.0;

    public static void renderFirstPersonItem(MatrixStack matrices, float tickDelta, Arm arm, float equipProgress, float swingProgress, ItemStack stack) {
        if (!hmi.isEnabled() || !hmi.shouldUseCustomAnimation()) return;
        hmi.updateDeltaTime();
        if (hmi.swingMode.is("HMI") && shouldUseHMI(stack)) {
            renderHMIStyle(matrices, arm, equipProgress, swingProgress, stack, tickDelta);
        } else if (hmi.swingMode.is("Swipe")) {
            renderSwipeStyle(matrices, arm, swingProgress);
        } else if (hmi.swingMode.is("Old")) {
            renderOldStyle(matrices, arm, swingProgress);
        }
        if (hmi.item360.isEnabled()) {
            float angle = (System.currentTimeMillis() / 4L % 360L) * hmi.rotationSpeed.getCurrent();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(angle));
        }
    }

    private static boolean shouldUseHMI(ItemStack stack) {
        if (stack.isEmpty()) return true;
        return !(stack.getItem() instanceof FilledMapItem) && !(stack.getItem() instanceof CrossbowItem);
    }

    private static void renderHMIStyle(MatrixStack matrices, Arm arm, float equipProgress, float swingProgress, ItemStack stack, float tickDelta) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        PlayerEntity player = mc.player;
        if (player == null) return;
        float handDirection = arm == Arm.RIGHT ? 1.0f : -1.0f;
        float swingRot = getSwingRot(swingProgress);
        float swing = ease(MathHelper.sin(swingProgress * (float) Math.PI));
        updateSwingPhysics(player, direction, swingProgress);
        matrices.translate(direction * 1.0, -equipProgress * 0.3, 0.3);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(45.0f * direction));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-40.0f * direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f));
        matrices.scale(0.9f, 0.9f, 0.9f);
        applyEnvironment(matrices, player, arm, stack, swingProgress, tickDelta);
        applyItemPose(matrices, player, arm, stack, swingProgress);
        applySwing(matrices, player, arm, stack, swingProgress);
    }

    private static void renderSwipeStyle(MatrixStack matrices, Arm arm, float swingProgress) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * (45.0f + f * -20.0f)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * f1 * -20.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f1 * -hmi.swingPower.getCurrent()));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -45.0f));
    }

    private static void renderOldStyle(MatrixStack matrices, Arm arm, float swingProgress) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        float f = MathHelper.sin(swingProgress * swingProgress * (float) Math.PI);
        float f1 = MathHelper.sin(MathHelper.sqrt(swingProgress) * (float) Math.PI);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * (45.0f + f * -20.0f)));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(direction * f1 * -20.0f));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(f1 * -80.0f));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(direction * -45.0f));
    }

    private static void updateSwingPhysics(PlayerEntity player, int direction, float swingProgress) {
        if (hmi.physicsUpdatedThisFrame) return;
        double currentSpeed = player.getVelocity().length();
        double dt = hmi.deltaTime * ANIMATION_SPEED;
        hmi.swingVelocityZ += (float) (((direction > 0 ? ((currentSpeed * -15.0) - hmi.swingVelocityZ) : ((currentSpeed * 15.0) - hmi.swingVelocityZ)) * 0.1) * dt);
        hmi.physicsUpdatedThisFrame = true;
    }

    private static float getSwingRot(float swingProgress) {
        if (swingProgress < 0.6f) return MathHelper.sin(MathHelper.clamp(swingProgress, 0.0f, 0.12506f) * 12.56f);
        return MathHelper.sin(MathHelper.clamp(swingProgress, 0.62532f, 0.75038f) * 12.56f);
    }

    private static float ease(float value) {
        float c1 = 1.70158f, c2 = c1 * 1.525f;
        if (value < 0.5f) {
            float doubled = 2.0f * value;
            return doubled * doubled * ((c2 + 1.0f) * doubled - c2) * 0.5f;
        }
        float shifted = 2.0f * value - 2.0f;
        return (shifted * shifted * ((c2 + 1.0f) * shifted + c2) + 2.0f) * 0.5f;
    }

    private static void applyEnvironment(MatrixStack matrices, PlayerEntity player, Arm arm, ItemStack stack, float swingProgress, float tickDelta) {
        float yaw = MathHelper.lerp(tickDelta, player.prevYaw, player.getYaw());
        double radians = Math.toRadians(yaw);
        double forwardX = -Math.sin(radians), forwardZ = Math.cos(radians);
        double motionX = player.getX() - player.prevX, motionZ = player.getZ() - player.prevZ;
        double dotProduct = motionX * forwardX + motionZ * forwardZ;
        double crossProduct = motionX * forwardZ - motionZ * forwardX;
        boolean crawling = isCrawling(player), climbing = isClimbing(player), elytraFlying = player.isGliding();
        double dt = hmi.deltaTime * ANIMATION_SPEED;
        float handDirection = arm == Arm.RIGHT ? 1.0f : -1.0f;
        if (elytraFlying) { hmi.climbBlend = 0.0f; hmi.inWaterCounter = 0.0f; }
        if (!hmi.physicsUpdatedThisFrame) {
            double motionLen = Math.sqrt(motionX * motionX + motionZ * motionZ);
            if (motionLen >= 0.08) {
                double clampedSpeed = Math.min(motionLen, 0.22), clampedDot = MathHelper.clamp(dotProduct, -0.22, 0.22);
                hmi.directionalCrawlCount += (float) (0.1 * clampedDot * 4.0 * dt);
            }
            float motionYNormalized = player.isOnGround() ? 0.0f : (float) MathHelper.clamp(player.getVelocity().y, -0.42, 0.42);
            hmi.vertAngleY += (float) (motionYNormalized * 0.015 * dt);
            hmi.vertAngleY = (float) (hmi.vertAngleY * Math.pow(0.88, dt));
            hmi.physicsUpdatedThisFrame = true;
        }
        if ((crawling || climbing) && !player.isUsingItem() && swingProgress == 0.0f) {
            hmi.climbBlend = (float) Math.min(1.0, hmi.climbBlend + 0.1 * dt);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-20.0f * hmi.climbBlend));
        } else { hmi.climbBlend = (float) (hmi.climbBlend * Math.pow(0.88, dt)); }
        if (swingProgress == 0.0f) {
            float pitch = player.getPitch();
            matrices.translate(handDirection > 0.0f ? pitch / 650.0f * hmi.climbBlend * -1.0f : pitch / 650.0f * hmi.climbBlend, 0.0f, 0.0f);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(pitch * hmi.climbBlend));
        }
        if (isLantern(stack)) matrices.translate(0.0f, 0.1f, player.getPitch() / 80.0f * hmi.climbBlend);
        if (player.isSwimming() && swingProgress == 0.0f) {
            double distance = (player.age + tickDelta) * 0.2;
            double handRotation = Math.sin(distance) * 1.5;
            double smoothRotation = handRotation * 0.8 + hmi.previousRotation * 0.2;
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (arm == Arm.RIGHT ? smoothRotation : -smoothRotation)));
            matrices.translate(0.0f, 0.0f, (float) (smoothRotation * 0.2));
            hmi.previousRotation = (float) smoothRotation;
        }
    }

    private static void applyItemPose(MatrixStack matrices, PlayerEntity player, Arm arm, ItemStack stack, float swingProgress) {
        int direction = arm == Arm.RIGHT ? 1 : -1;
        matrices.translate(-0.3 * direction, 0.65, -0.1);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-65.0f * direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(10.0f));
        if (stack.getItem() instanceof BlockItem && !(stack.getItem() instanceof BucketItem)) {
            if (isLantern(stack)) { applyLanternPose(matrices, player, arm, swingProgress); return; }
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(25.0f * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(5.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0f * direction));
            matrices.translate(0.2 * direction, 0.2, 0.05);
            return;
        }
        if (isSmallItem(stack)) {
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(5.0f * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0f));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0f * direction));
            matrices.translate(0.0, -0.05, -0.1);
            matrices.scale(0.7f, 0.7f, 0.7f);
            return;
        }
        if (stack.getUseAction() == UseAction.BLOCK) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(160.0f * direction));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-60.0f * direction));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-70.0f));
            matrices.scale(0.75f, 0.75f, 0.75f);
            matrices.translate(0.15 * direction, 0.35, -0.15);
            matrices.translate(0.17 * direction, 0.0, 0.3);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-90.0f * direction));
            return;
        }
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(75.0f * direction));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(70.0f));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(45.0f * direction));
        matrices.scale(1.2f, 1.2f, 1.2f);
    }

    private static void applyLanternPose(MatrixStack matrices, PlayerEntity player, Arm arm, float swingProgress) {
        float dt = (float) (hmi.deltaTime * ANIMATION_SPEED);
        int direction = arm == Arm.RIGHT ? 1 : -1;
        float yawDelta = player.prevHeadYaw - player.headYaw, pitchDelta = player.prevPitch - player.getPitch();
        hmi.swingVelocityY += yawDelta * 0.015f * dt;
        hmi.swingVelocityX += pitchDelta * 0.015f * dt;
        hmi.swingVelocityY -= 0.1f * hmi.swingAngleY * dt;
        hmi.swingVelocityX -= 0.1f * hmi.swingAngleX * dt;
        hmi.swingVelocityY = (float) (hmi.swingVelocityY * Math.pow(0.88, dt));
        hmi.swingVelocityX = (float) (hmi.swingVelocityX * Math.pow(0.88, dt));
        hmi.swingAngleY += hmi.swingVelocityY * dt;
        hmi.swingAngleX += hmi.swingVelocityX * dt;
        matrices.translate(0.0, 0.0, -0.1);
        matrices.multiply(RotationAxis.NEGATIVE_Y.rotationDegrees(35.0f * direction + hmi.swingAngleY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(15.0f + hmi.swingAngleX));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(75.0f * direction + hmi.swingVelocityZ));
        matrices.translate(0.3 * direction, -0.35, 0.0);
        matrices.translate(0.0, 0.0, 0.1);
        matrices.scale(1.5f, 1.5f, 1.5f);
    }

    private static void applySwing(MatrixStack matrices, PlayerEntity player, Arm arm, ItemStack stack, float swingProgress) {
        boolean mainHand = arm == Arm.RIGHT;
        float ll = mainHand ? 1.0f : -1.0f;
        float swingRot = getSwingRot(swingProgress);
        float swing = ease(MathHelper.sin(swingProgress * (float) Math.PI));
        boolean hasAuraTarget = hmi.onlyAura.isEnabled() ? (Aura.INSTANCE != null && Aura.INSTANCE.getTarget() != null && Aura.INSTANCE.getTarget().isAlive()) : true;
        boolean forwardAttack = hmi.useForwardAttack.isEnabled() && hasAuraTarget;
        boolean normalAttack = hmi.useNormalAttack.isEnabled() && hasAuraTarget;
        if (stack.getItem() instanceof SwordItem && forwardAttack) {
            matrices.translate(0.12 * ll * swingRot, 0.04 * swingRot, -0.95 * swing);
            matrices.translate(0.02 * ll * swing, 0.10 * swing, -0.10 * swingRot);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(8.0f * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-14.0f * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-18.0f * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(32.0f * swing));
            return;
        }
        if (stack.getItem() instanceof SwordItem && normalAttack) {
            applyGenericSwing(matrices, ll, swingRot, swing);
            return;
        }
        if (isWeapon(stack) && !isShovel(stack)) {
            matrices.translate(0.8 * ll * swingRot, 0.3 * swingRot, -0.5 * swing);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0f * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-20.0f * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-70.0f * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees((stack.getItem() instanceof SwordItem ? 40.0f : 30.0f) * swing));
            return;
        }
        if (isShovel(stack)) {
            matrices.translate(0.0, 0.15 * swingRot, -0.25 * swingRot);
            matrices.translate(0.0, 0.0, -0.2 * swing);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(15.0f * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-35.0f * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(30.0f * swing));
            return;
        }
        if (isTool(stack)) {
            matrices.translate(0.1 * ll * swingRot, 0.1 * swingRot, -0.5 * swing);
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swingRot));
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-20.0f * swingRot * ll));
            matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
            return;
        }
        applyGenericSwing(matrices, ll, swingRot, swing);
    }

    private static void applyGenericSwing(MatrixStack matrices, float direction, float swingRot, float swing) {
        matrices.translate(0.1 * direction * swingRot, 0.1 * swingRot, -0.1 * swing);
        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(-30.0f * swingRot));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-10.0f * swingRot * direction));
        matrices.multiply(RotationAxis.NEGATIVE_X.rotationDegrees(40.0f * swing));
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(10.0f * swing * direction));
    }

    private static boolean isCrawling(PlayerEntity player) { return player.isInSwimmingPose() && !player.isSwimming(); }
    private static boolean isClimbing(PlayerEntity player) { return player.isClimbing() && !player.isOnGround() && Math.abs(player.getVelocity().y) > 0.0; }
    private static boolean isWeapon(ItemStack stack) { return stack.getItem() instanceof SwordItem || stack.getItem() instanceof AxeItem; }
    private static boolean isTool(ItemStack stack) { return stack.getItem() instanceof MiningToolItem || stack.getItem() instanceof TridentItem; }
    private static boolean isShovel(ItemStack stack) { return stack.getItem() instanceof ShovelItem; }
    private static boolean isLantern(ItemStack stack) { return stack.getItem() == Items.LANTERN || stack.getItem() == Items.SOUL_LANTERN; }
    private static boolean isSmallItem(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem) return false;
        if (isTool(stack)) return false;
        if (isWeapon(stack)) return false;
        return !(stack.getItem() instanceof FishingRodItem) && !(stack.getItem() instanceof BucketItem)
                && stack.getUseAction() != UseAction.BOW && stack.getUseAction() != UseAction.SPEAR && stack.getUseAction() != UseAction.BLOCK;
    }
}