package tech.javelin.client.modules.impl.render;

import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(name = "HoldMyItems", category = Category.RENDER, description = "Custom first person item animations")
public class HoldMyItems extends Module {
    public static final HoldMyItems INSTANCE = new HoldMyItems();

    public final ModeSetting swingMode = new ModeSetting("Swing Mode", "Normal", "Normal", "Swipe", "HMI", "Block", "Old");
    public final NumberSetting swingSpeed = new NumberSetting("Swing Speed", 1.0f, 0.1f, 2.0f, 0.1f);
    public final NumberSetting swingPower = new NumberSetting("Swing Power", 80.0f, 30.0f, 150.0f, 5.0f);
    
    public final BooleanSetting hmiPhysics = new BooleanSetting("HMI Physics", true);
    public final NumberSetting physicsIntensity = new NumberSetting("Physics Intensity", 1.0f, 0.1f, 3.0f, 0.1f);
    public final NumberSetting bobbing = new NumberSetting("Bobbing", 0.5f, 0.0f, 2.0f, 0.1f);
    
    public final NumberSetting rightHandX = new NumberSetting("Right Hand X", 0.0f, -1.0f, 1.0f, 0.05f);
    public final NumberSetting rightHandY = new NumberSetting("Right Hand Y", 0.0f, -1.0f, 1.0f, 0.05f);
    public final NumberSetting rightHandZ = new NumberSetting("Right Hand Z", 0.0f, -1.0f, 1.0f, 0.05f);
    
    public final NumberSetting leftHandX = new NumberSetting("Left Hand X", 0.0f, -1.0f, 1.0f, 0.05f);
    public final NumberSetting leftHandY = new NumberSetting("Left Hand Y", 0.0f, -1.0f, 1.0f, 0.05f);
    public final NumberSetting leftHandZ = new NumberSetting("Left Hand Z", 0.0f, -1.0f, 1.0f, 0.05f);
    
    public final BooleanSetting item360 = new BooleanSetting("Item 360", false);
    public final NumberSetting rotationSpeed = new NumberSetting("Rotation Speed", 1.0f, 0.1f, 5.0f, 0.1f);
    public final BooleanSetting onlyAura = new BooleanSetting("Only Aura", false);
    public final BooleanSetting useForwardAttack = new BooleanSetting("Forward Attack", true);
    public final BooleanSetting useNormalAttack = new BooleanSetting("Normal Attack", false);
    public final BooleanSetting oldAnimations = new BooleanSetting("Old Animations", false);

    public float swingVelocityY = 0.0f;
    public float swingVelocityZ = 0.0f;
    public float swingVelocityX = 0.0f;
    public float swingAngleY = 0.0f;
    public float swingAngleX = 0.0f;
    public float climbBlend = 0.0f;
    public float directionalCrawlCount = 0.0f;
    public float inWaterCounter = 0.0f;
    public double deltaTime = 0.0;
    public float vertAngleY = 0.0f;
    public float previousRotation = 0.0f;
    public boolean physicsUpdatedThisFrame = false;
    private long lastFrameTime = System.currentTimeMillis();

    public boolean shouldUseCustomAnimation() {
        return isEnabled();
    }

    public void updateDeltaTime() {
        long currentTime = System.currentTimeMillis();
        deltaTime = Math.min((currentTime - lastFrameTime) / 1000.0, 0.05);
        lastFrameTime = currentTime;
        physicsUpdatedThisFrame = false;
    }
}
