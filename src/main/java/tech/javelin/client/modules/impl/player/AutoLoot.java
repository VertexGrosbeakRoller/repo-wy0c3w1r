package tech.javelin.client.modules.impl.player;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.base.events.impl.player.EventUpdate;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.impl.combat.Aura;
import tech.javelin.utility.game.player.PlayerInventoryUtil;
import tech.javelin.utility.math.Timer;

@ModuleAnnotation(
        name = "AutoLoot",
        category = Category.PLAYER,
        description = "После убийства цели Aura автоматически складывает добро в шалкер"
)
public final class AutoLoot extends Module {
    public static final AutoLoot INSTANCE = new AutoLoot();

    private final ModeSetting mode = new ModeSetting("Режим", "Тихий (пакеты)", "Правый клик");
    private final BooleanSetting keepArmor  = new BooleanSetting("Не складывать броню",  true);
    private final BooleanSetting keepWeapon = new BooleanSetting("Не складывать оружие", true);
    private final BooleanSetting keepFood   = new BooleanSetting("Не складывать еду",    false);

    private LivingEntity lastTarget = null;
    private boolean triggered       = false;
    private boolean shulkerOpen     = false;
    private final Timer waitTimer   = new Timer();
    private int lootPhase = 0;

    private AutoLoot() {}

    @EventTarget
    @Native
    public void onUpdate(EventUpdate event) {
        if (mc.player == null || mc.world == null) return;

        LivingEntity target = Aura.INSTANCE.getTarget();

        if (target != null && target.isAlive()) {
            lastTarget = target;
            triggered = false;
            lootPhase = 0;
        }

        if (!triggered && lastTarget != null && !lastTarget.isAlive()
                && mc.player.distanceTo(lastTarget) < 6.0) {
            triggered = true;
            lootPhase = 1;
            waitTimer.reset();
        }

        if (!triggered || lootPhase == 0) return;

        if (!waitTimer.finished(300L)) return;

        switch (lootPhase) {
            case 1 -> {
                Slot shulkerSlot = findShulkerInInventory();
                if (shulkerSlot == null) { lootPhase = 0; triggered = false; return; }

                if (mode.is("Тихий (пакеты)")) {
                    doSilentLoot(shulkerSlot);
                } else {
                    doRightClickLoot(shulkerSlot);
                }
                lootPhase = 0;
                triggered = false;
            }
        }
    }

    private void doSilentLoot(Slot shulkerSlot) {
        ClientPlayerEntity p = mc.player;
        int shulkerInvSlot = shulkerSlot.id;

        for (int i = 9; i < 45; i++) {
            Slot slot = p.currentScreenHandler.getSlot(i);
            if (slot == null || slot.getStack().isEmpty()) continue;
            if (slot == shulkerSlot) continue;
            if (shouldKeep(slot.getStack())) continue;

            PlayerInventoryUtil.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
        }
    }

    private void doRightClickLoot(Slot shulkerSlot) {
        ClientPlayerEntity p = mc.player;
        int prevSlot = p.getInventory().selectedSlot;

        p.getInventory().selectedSlot = toHotbarSlot(shulkerSlot.id);
        p.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(p.getInventory().selectedSlot));

        BlockPos pos = p.getBlockPos();
        mc.interactionManager.interactBlock(p, Hand.MAIN_HAND,
                new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false));

        waitTimer.reset();

        for (int i = 9; i < 45; i++) {
            Slot slot = p.currentScreenHandler.getSlot(i);
            if (slot == null || slot.getStack().isEmpty()) continue;
            if (slot == shulkerSlot) continue;
            if (shouldKeep(slot.getStack())) continue;
            PlayerInventoryUtil.clickSlot(slot, 0, SlotActionType.QUICK_MOVE, false);
        }

        p.networkHandler.sendPacket(new CloseHandledScreenC2SPacket(p.currentScreenHandler.syncId));
        p.getInventory().selectedSlot = prevSlot;
        p.networkHandler.sendPacket(new net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket(prevSlot));
    }

    private boolean shouldKeep(ItemStack stack) {
        if (stack.isEmpty()) return true;
        Item item = stack.getItem();
        if (keepArmor.isEnabled() && item instanceof ArmorItem) return true;
        if (keepWeapon.isEnabled() && (item instanceof SwordItem || item instanceof AxeItem
                || item instanceof TridentItem || item instanceof BowItem
                || item instanceof CrossbowItem)) return true;
        if (keepFood.isEnabled() && stack.contains(net.minecraft.component.DataComponentTypes.FOOD)) return true;
        return false;
    }

    private Slot findShulkerInInventory() {
        for (int i = 9; i < 45; i++) {
            Slot slot = mc.player.currentScreenHandler.getSlot(i);
            if (slot != null && !slot.getStack().isEmpty()
                    && slot.getStack().getItem() instanceof BlockItem bi
                    && bi.getBlock() instanceof net.minecraft.block.ShulkerBoxBlock) {
                return slot;
            }
        }
        return null;
    }

    private int toHotbarSlot(int invSlot) {
        if (invSlot >= 36 && invSlot <= 44) return invSlot - 36;
        return mc.player.getInventory().selectedSlot;
    }
}
