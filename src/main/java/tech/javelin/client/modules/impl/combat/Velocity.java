package tech.javelin.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.other.EventTick;
import tech.javelin.base.events.impl.server.EventPacket;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;

@ModuleAnnotation(
        name = "Velocity",
        category = Category.COMBAT,
        description = "Уменьшает или отменяет откат от урона"
)
public final class Velocity extends Module {
    public static final Velocity INSTANCE = new Velocity();

    private final ModeSetting mode = new ModeSetting("Режим", new String[0]);
    private final ModeSetting.Value modeCancel;
    private final ModeSetting.Value modeGrimSkip;
    private final ModeSetting.Value modeGrimCancel;
    private final ModeSetting.Value modeGrimCancel2;
    private final ModeSetting.Value modeGrimNew;
    private final ModeSetting.Value modeFuntime;

    private int skip;
    private boolean cancel;
    private boolean damaged;
    private boolean flag;
    private int ccCooldown;

    private Velocity() {
        this.modeCancel     = (new ModeSetting.Value(this.mode, "Cancel")).select();
        this.modeGrimSkip   = new ModeSetting.Value(this.mode, "GrimSkip");
        this.modeGrimCancel = new ModeSetting.Value(this.mode, "GrimCancel");
        this.modeGrimCancel2= new ModeSetting.Value(this.mode, "GrimCancel2");
        this.modeGrimNew    = new ModeSetting.Value(this.mode, "GrimNew");
        this.modeFuntime    = new ModeSetting.Value(this.mode, "Funtime");
    }

    @EventTarget
    @Native
    public void onPacket(EventPacket e) {
        if (mc.player == null) return;

        if (e.isReceive()) {
            if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket p
                    && p.getEntityId() != mc.player.getId()) return;

            if (this.modeCancel.isSelected()) {
                if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
                    e.setCancelled(true);
                }
            }

            if (this.modeGrimSkip.isSelected()) {
                if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
                    skip = 6;
                    e.setCancelled(true);
                }
            }

            if (this.modeGrimCancel.isSelected()) {
                if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket) {
                    e.setCancelled(true);
                    cancel = true;
                }
                if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                    skip = 3;
                }
            }

            if (this.modeGrimCancel2.isSelected()) {
                if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket wrapper) {
                    if (wrapper.getEntityId() != mc.player.getId() || skip < 0) return;
                    skip = 8;
                    e.setCancelled(true);
                }
                if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                    skip = -8;
                }
            }

            if (this.modeFuntime.isSelected()) {
                if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket p) {
                    if (skip >= 2) return;
                    if (p.getEntityId() != mc.player.getId()) return;
                    e.setCancelled(true);
                    damaged = true;
                }
                if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                    skip = 3;
                }
            }

            if (this.modeGrimNew.isSelected()) {
                if (ccCooldown > 0) {
                    ccCooldown--;
                } else {
                    if (e.getPacket() instanceof EntityVelocityUpdateS2CPacket wrapper
                            && wrapper.getEntityId() == mc.player.getId()) {
                        e.setCancelled(true);
                        flag = true;
                    }
                    if (e.getPacket() instanceof ExplosionS2CPacket) {
                        e.setCancelled(true);
                        flag = true;
                    }
                    if (e.getPacket() instanceof PlayerPositionLookS2CPacket) {
                        ccCooldown = 5;
                    }
                }
            }

        } else if (e.isSent()) {
            if (this.modeGrimSkip.isSelected()) {
                if (e.getPacket() instanceof PlayerMoveC2SPacket && skip > 0) {
                    skip--;
                    e.setCancelled(true);
                }
            }

            if (this.modeGrimCancel.isSelected()) {
                if (e.getPacket() instanceof PlayerMoveC2SPacket) {
                    skip--;
                    if (cancel) {
                        if (skip <= 0) {
                            BlockPos blockPos = BlockPos.ofFloored(mc.player.getPos());
                            mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                                    mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                                    mc.player.getYaw(), mc.player.getPitch(),
                                    mc.player.isOnGround(), false));
                            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                                    blockPos, Direction.UP));
                        }
                        cancel = false;
                    }
                }
            }

            if (this.modeGrimCancel2.isSelected()) {
                if (e.getPacket() instanceof PlayerMoveC2SPacket) {
                    if (skip > 1) {
                        skip--;
                        e.setCancelled(true);
                    } else if (skip < 0) {
                        skip++;
                    }
                }
            }
        }
    }

    @EventTarget
    @Native
    public void onTick(EventTick e) {
        if (mc.player == null) return;

        if (this.modeFuntime.isSelected()) {
            skip--;
            if (damaged) {
                BlockPos blockPos = BlockPos.ofFloored(mc.player.getPos());
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        mc.player.getYaw(), mc.player.getPitch(),
                        mc.player.isOnGround(), false));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                        blockPos, Direction.UP));
                damaged = false;
            }
        }

        if (this.modeGrimNew.isSelected() && flag) {
            if (ccCooldown <= 0) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(
                        mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        mc.player.getYaw(), mc.player.getPitch(),
                        mc.player.isOnGround(), false));
                mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                        PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK,
                        BlockPos.ofFloored(mc.player.getPos()), Direction.UP));
            }
            flag = false;
        }
    }

    @Override
    public void onEnable() {
        skip = 0;
        cancel = false;
        damaged = false;
        flag = false;
        ccCooldown = 0;
        super.onEnable();
    }
}
