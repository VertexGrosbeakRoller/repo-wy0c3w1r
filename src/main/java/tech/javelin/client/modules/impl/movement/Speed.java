package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventMoveInput;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.game.player.MovingUtil;
import tech.javelin.utility.math.StopWatch;

import java.util.Random;

@ModuleAnnotation(
   name = "Speed",
   category = Category.MOVEMENT,
   description = "Speed module with strafe and collision modes"
)
public class Speed extends Module {
    public static final Speed INSTANCE = new Speed();
    
    public final ModeSetting mode = new ModeSetting("Mode", 
        "Strafe", "Strafe", "ReallyWorld", "Collision1", "Collision2");
    
    private final NumberSetting strafeSpeed = new NumberSetting("Strafe Speed", 0.28F, 0.1F, 0.5F, 0.01F);
    private final NumberSetting collisionBoost = new NumberSetting("Collision Boost", 0.35F, 0.1F, 0.6F, 0.01F);
    private final BooleanSetting rotateOnCollide = new BooleanSetting("Rotate on Collide", "Применять силу ротации при столкновении", true);
    
    // Strafe state
    private int strafeStage = 0;
    private double lastDistance = 0;
    private double speedValue = 0;
    
    // Collision1 state
    private boolean wasColliding = false;
    private float rotationForce = 0;
    private StopWatch collisionTimer = new StopWatch();
    
    // Collision2 state
    private boolean wasInAir = false;
    private double fallSpeed = 0;
    private StopWatch landTimer = new StopWatch();
    
    private final Random rand = new Random();
    
    private Speed() {}

    @Override
    public void onEnable() {
        strafeStage = 4;
        lastDistance = 0;
        speedValue = 0;
        wasColliding = false;
        rotationForce = 0;
        wasInAir = false;
        fallSpeed = 0;
        collisionTimer.reset();
        landTimer.reset();
    }

    @Override
    public void onDisable() {
        if (mc.player != null) {
            mc.player.getAbilities().setWalkSpeed(0.1F);
        }
    }
    
    @EventTarget
    @Native
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        
        if (mode.is("Strafe")) {
            handleStrafeUpdate();
        } else if (mode.is("Collision1")) {
            handleCollision1Update();
        } else if (mode.is("Collision2")) {
            handleCollision2Update();
        } else if (mode.is("ReallyWorld")) {
            handleReallyWorldUpdate();
        }
    }
    
    @EventTarget
    @Native
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null || mc.world == null) return;
        
        if (mode.is("Strafe")) {
            if (!mc.player.isOnGround()) return;
            handleStrafeMove(event);
        } else if (mode.is("Collision1")) {
            handleCollision1Move(event);
        } else if (mode.is("Collision2")) {
            handleCollision2Move(event);
        } else if (mode.is("ReallyWorld")) {
            if (!mc.player.isOnGround()) return;
            handleReallyWorldMove(event);
        }
    }
    
    // ========== Strafe Mode ==========
    private void handleStrafeUpdate() {
        double currentSpeed = Math.sqrt(mc.player.getVelocity().x * mc.player.getVelocity().x 
                                       + mc.player.getVelocity().z * mc.player.getVelocity().z);
        lastDistance = currentSpeed;
    }
    
    private void handleStrafeMove(EventMoveInput event) {
        float forward = event.getForward();
        float strafe = event.getStrafe();
        
        if (forward == 0 && strafe == 0) {
            strafeStage = 4;
            return;
        }
        
        double baseSpeed = strafeSpeed.getCurrent();
        double acceleratedSpeed = baseSpeed * 2.149;
        
        if (strafeStage == 1) {
            speedValue = acceleratedSpeed;
        } else if (strafeStage == 2) {
            strafeStage = 3;
            speedValue = acceleratedSpeed - (lastDistance - baseSpeed) * 0.76;
        } else if (strafeStage == 3) {
            strafeStage = 4;
            if (mc.player.isOnGround()) {
                speedValue = lastDistance - (lastDistance / baseSpeed - 1.0) * 0.5;
            }
        } else {
            strafeStage = 1;
        }
        
        // Apply speed with direction
        double[] motion = MovingUtil.calculateDirection(speedValue);
        mc.player.setVelocity(motion[0], mc.player.getVelocity().y, motion[1]);
        
        // Strafe fix
        if (strafe != 0 && forward != 0) {
            event.setForward(0);
        }
    }
    
    // ========== Collision1 Mode ==========
    // Буст при столкновении с блоками/сущностями + микро-ротация
    private void handleCollision1Update() {
        // Проверяем столкновение с блоками (horizontal collision)
        boolean colliding = mc.player.horizontalCollision;
        
        // Также проверяем сущности
        if (!colliding) {
            Box playerBox = mc.player.getBoundingBox().expand(0.25);
            for (Entity entity : mc.world.getEntities()) {
                if (entity != mc.player && playerBox.intersects(entity.getBoundingBox())) {
                    colliding = true;
                    break;
                }
            }
        }
        
        // При столкновении даем буст
        if (colliding && !wasColliding) {
            collisionTimer.reset();
            if (rotateOnCollide.isEnabled()) {
                rotationForce = (rand.nextFloat() - 0.5f) * 90.0F;
            }
        }
        
        wasColliding = colliding;
        
        // Применяем микро-ротацию для обхода
        if (rotationForce != 0 && rotateOnCollide.isEnabled()) {
            mc.player.setYaw(mc.player.getYaw() + rotationForce * 0.05F);
            rotationForce *= 0.85F;
            if (Math.abs(rotationForce) < 0.5F) rotationForce = 0;
        }
    }
    
    private void handleCollision1Move(EventMoveInput event) {
        double baseSpeed = strafeSpeed.getCurrent();
        
        // Буст при столкновении активен 200мс
        double speed = wasColliding && collisionTimer.getElapsedTime() < 200 
            ? collisionBoost.getCurrent() * 1.2 
            : baseSpeed;
        
        if (event.getForward() != 0 || event.getStrafe() != 0) {
            double[] motion = MovingUtil.calculateDirection(speed);
            mc.player.setVelocity(motion[0], mc.player.getVelocity().y, motion[1]);
        }
    }
    
    // ========== Collision2 Mode ==========
    // Буст при касании земли + ускоренное падение
    private void handleCollision2Update() {
        boolean inAir = !mc.player.isOnGround();
        
        if (inAir) {
            // Ускоряем падение для быстрого приземления
            if (mc.player.getVelocity().y < -0.1) {
                Vec3d vel = mc.player.getVelocity();
                mc.player.setVelocity(vel.x, vel.y * 1.08, vel.z);
            }
            wasInAir = true;
        } else if (wasInAir) {
            // Приземление - старт буста
            landTimer.reset();
            wasInAir = false;
        }
    }
    
    private void handleCollision2Move(EventMoveInput event) {
        if (event.getForward() == 0 && event.getStrafe() == 0) return;
        
        double baseSpeed = strafeSpeed.getCurrent();
        long landTime = landTimer.getElapsedTime();
        
        double speed;
        if (landTime < 100) {
            // Первые 100мс - максимальный буст
            speed = collisionBoost.getCurrent() * 1.8;
        } else if (landTime < 300) {
            // До 300мс - постепенное снижение
            double t = (landTime - 100) / 200.0;
            speed = collisionBoost.getCurrent() * 1.8 * (1 - t) + baseSpeed * t;
        } else {
            speed = baseSpeed;
        }
        
        double[] motion = MovingUtil.calculateDirection(speed);
        mc.player.setVelocity(motion[0], mc.player.getVelocity().y, motion[1]);
    }
    
    // ========== ReallyWorld Mode ==========
    private void handleReallyWorldUpdate() {
        // Стандартная логика ReallyWorld
    }
    
    private void handleReallyWorldMove(EventMoveInput event) {
        // Простой strafe для ReallyWorld
        double speed = 0.26;
        if (event.getForward() != 0 || event.getStrafe() != 0) {
            double[] motion = MovingUtil.calculateDirection(speed);
            mc.player.setVelocity(motion[0], mc.player.getVelocity().y, motion[1]);
        }
    }
}

