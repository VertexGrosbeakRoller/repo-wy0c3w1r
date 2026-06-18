package tech.javelin.client.modules.impl.movement;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventMoveInput;
import tech.javelin.base.events.impl.player.EventMotion;
import tech.javelin.base.events.impl.player.EventOnMovePost;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.base.events.impl.server.EventPacket;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.utility.game.other.NetworkUtils;
import tech.javelin.utility.game.other.TimerManager;
import tech.javelin.utility.game.player.MovingUtil;
import tech.javelin.utility.math.StopWatch;

import java.util.Random;

@ModuleAnnotation(
   name = "Speed",
   category = Category.MOVEMENT,
   description = "Speed module with strafe and ReallyWorld modes"
)
public class Speed extends Module {
    public static final Speed INSTANCE = new Speed();
    
    public final ModeSetting mode = new ModeSetting("Mode", 
        "Strafe", "Strafe", "ReallyWorld", "Collision1", "Collision2");
    
    private int strafeStage = 0;
    private double lastDistance = 0;
    private double speedValue = 0;
    
    private boolean wasColliding = false;
    private float rotationForce = 0;
    private StopWatch collisionTimer = new StopWatch();
    
    private boolean wasInAir = false;
    private double fallSpeed = 0;
    private StopWatch landTimer = new StopWatch();
    
    private final Random rand = new Random();
    
    // ReallyWorld fields
    private int ticks = 0;
    private int groundTicks = 0;
    
    private Speed() {}

    @Override
    public void onEnable() {
        strafeStage = 0;
        lastDistance = 0;
        speedValue = 0;
        wasColliding = false;
        rotationForce = 0;
        wasInAir = false;
        fallSpeed = 0;
        ticks = 0;
        groundTicks = 0;
        collisionTimer.reset();
        landTimer.reset();
    }

    @Override
    public void onDisable() {
        TimerManager.resetTimer();
        if (mc.player != null) {
            mc.player.getAbilities().setWalkSpeed(0.1F);
        }
    }

    @EventTarget
    @Native
    public void onMovePost(EventOnMovePost event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("ReallyWorld")) return;
        
        TimerManager.setTimer(1.7F);

        if (ticks > 3) {
            double bst = 0.03;
            if (ticks % 2 == 0) {
                Vec3d v = mc.player.getVelocity();
                mc.player.setVelocity(v.x, v.y + 0.03, v.z);
                if (mc.player.isOnGround()) {
                    bst = 0.085;
                } else {
                    bst = 0.03;
                }
            }

            double yaw = Math.toRadians(MovingUtil.getdir());
            double xt = -Math.sin(yaw);
            double zt = Math.cos(yaw);
            if (MovingUtil.getdir() == -1.0F) {
                xt = 0.0;
                zt = 0.0;
            }
            Vec3d vel = mc.player.getVelocity();
            mc.player.setVelocity(vel.x + xt * bst, vel.y, vel.z + zt * bst);
        }

        ticks++;
    }

    @EventTarget
    @Native
    public void onMoveInput(EventMoveInput event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("ReallyWorld")) return;
        
        if (mc.player.verticalCollision) groundTicks++;
        else groundTicks = 0;

        if (groundTicks >= 1) mc.player.jump();
    }

    @EventTarget
    @Native
    public void onMotion(EventMotion event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("ReallyWorld")) return;
        
        if (ticks % 2 == 0) {
            TimerManager.setTimer(0.3F);
            NetworkUtils.sendSilentPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
        }
    }

    @EventTarget
    @Native
    public void onPacket(EventPacket event) {
        if (mc.player == null || mc.world == null) return;
        if (!mode.is("ReallyWorld")) return;
        
        if (event.getPacket() instanceof PlayerPositionLookS2CPacket) {
            if (ticks % 2 == 1) {
                ticks++;
            }
            TimerManager.setTimer(1.0F);
        }
    }
    
    @EventTarget
    @Native
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;
        
        boolean hasMovement = mc.player.input.movementForward != 0 || mc.player.input.movementSideways != 0;
        
        if (mode.is("Strafe")) {
            if (!hasMovement) {
                strafeStage = 0;
                return;
            }
            
            double xDist = mc.player.getX() - mc.player.prevX;
            double zDist = mc.player.getZ() - mc.player.prevZ;
            lastDistance = Math.sqrt(xDist * xDist + zDist * zDist);
            
            double baseSpeed = 0.28;
            
            if (mc.player.isOnGround()) {
                strafeStage = 2;
                speedValue = baseSpeed * 1.35;
                mc.player.jump();
            } else if (strafeStage == 2) {
                strafeStage = 3;
                speedValue = lastDistance - lastDistance / 159.0;
            } else {
                strafeStage = 0;
                speedValue = lastDistance - lastDistance / 159.0;
            }
            
            speedValue = Math.max(speedValue, baseSpeed);
            MovingUtil.setVelocity(speedValue);
            
        } else if (mode.is("Collision1")) {
            boolean colliding = mc.player.horizontalCollision;
            
            if (!colliding) {
                Box playerBox = mc.player.getBoundingBox().expand(0.25);
                for (Entity entity : mc.world.getEntities()) {
                    if (entity != mc.player && playerBox.intersects(entity.getBoundingBox())) {
                        colliding = true;
                        break;
                    }
                }
            }
            
            if (colliding && !wasColliding) {
                collisionTimer.reset();
                rotationForce = (rand.nextFloat() - 0.5f) * 90.0F;
            }
            
            wasColliding = colliding;
            
            if (rotationForce != 0) {
                mc.player.setYaw(mc.player.getYaw() + rotationForce * 0.05F);
                rotationForce *= 0.85F;
                if (Math.abs(rotationForce) < 0.5F) rotationForce = 0;
            }
            
            if (hasMovement) {
                double speed = wasColliding && collisionTimer.getElapsedTime() < 200 
                    ? 0.42 : 0.28;
                MovingUtil.setVelocity(speed);
            }
            
        } else if (mode.is("Collision2")) {
            boolean inAir = !mc.player.isOnGround();
            
            if (inAir) {
                if (mc.player.getVelocity().y < -0.1) {
                    Vec3d vel = mc.player.getVelocity();
                    mc.player.setVelocity(vel.x, vel.y * 1.08, vel.z);
                }
                wasInAir = true;
            } else if (wasInAir) {
                landTimer.reset();
                wasInAir = false;
            }
            
            if (hasMovement) {
                double baseSpeed = 0.28;
                long landTime = landTimer.getElapsedTime();
                
                double speed;
                if (landTime < 100) {
                    speed = 0.63;
                } else if (landTime < 300) {
                    double t = (landTime - 100) / 200.0;
                    speed = 0.63 * (1 - t) + baseSpeed * t;
                } else {
                    speed = baseSpeed;
                }
                MovingUtil.setVelocity(speed);
            }
        }
    }
}
