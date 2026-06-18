package tech.javelin.client.modules.impl.combat;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import tech.javelin.base.events.impl.player.EventAttack;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;

import java.util.Iterator;

@ModuleAnnotation(
        name = "PacketCriticals",
        category = Category.COMBAT,
        description = "Бьет критами под эффект плавного падения / в паутине"
)
public class PacketCriticals extends Module {
    public static final PacketCriticals INSTANCE = new PacketCriticals();

    private final ModeSetting mode;
    private final ModeSetting.Value modeGrim;
    private final ModeSetting.Value modeMatrix;
    private final ModeSetting.Value modeLegit;
    private final ModeSetting.Value modeVanilla;

    private PacketCriticals() {
        mode = new ModeSetting("Обход", new String[0]);
        modeGrim = new ModeSetting.Value(mode, "Grim").select();
        modeMatrix = new ModeSetting.Value(mode, "Matrix");
        modeLegit = new ModeSetting.Value(mode, "Legit");
        modeVanilla = new ModeSetting.Value(mode, "Vanilla");
    }

    @EventTarget
    private void onAttack(EventAttack event) {
        if (mc.player == null || mc.world == null) return;
        if (!(event.getTarget() instanceof LivingEntity)) return;
        if (mc.player.getAttackCooldownProgress(0.5F) < 0.92F) return;
        if (mc.player.isSubmergedInWater() || mc.player.isInLava() || mc.player.isClimbing() || mc.player.hasVehicle()) return;

        if (isInWeb() || mc.player.hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            double x = mc.player.getX();
            double y = mc.player.getY();
            double z = mc.player.getZ();

            if (modeGrim.isSelected()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.03125, z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 1.0E-4, z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, false));
            } else if (modeMatrix.isSelected()) {
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.08, z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.02, z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, false));
            } else if (modeLegit.isSelected()) {
                // Legit - самый безопасный для всех анти-читов
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.01, z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, false));
            } else {
                // Vanilla
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.05, z, false, false));
                mc.player.networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, true, false));
            }
        }
    }

    private boolean isInWeb() {
        if (mc.player == null || mc.world == null) return false;
        Box box = mc.player.getBoundingBox();
        Iterator<BlockPos> iterator = BlockPos.iterate(
                MathHelper.floor(box.minX), MathHelper.floor(box.minY), MathHelper.floor(box.minZ),
                MathHelper.floor(box.maxX), MathHelper.floor(box.maxY), MathHelper.floor(box.maxZ)
        ).iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            if (mc.world.getBlockState(pos).isOf(Blocks.COBWEB)) {
                return true;
            }
        }
        return false;
    }
}
