package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import ru.nexusguard.protection.annotations.Native;
import tech.javelin.Javelin;
import tech.javelin.base.events.impl.render.EventHudRender;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;
import tech.javelin.utility.render.display.base.color.ColorRGBA;
import tech.javelin.utility.render.display.shader.DrawUtil;

import java.util.ArrayList;
import java.util.List;

@ModuleAnnotation(
        name = "Arrows",
        category = Category.RENDER,
        description = "Shows arrows towards players"
)
public final class Arrows extends Module {
    public static final Arrows INSTANCE = new Arrows();

    private final NumberSetting arrowsDistance = new NumberSetting("Distance", 60.0F, 20.0F, 150.0F, 5.0F);
    private final NumberSetting arrowSize      = new NumberSetting("Size", 12.0F, 8.0F, 50.0F, 0.5F);
    private final BooleanSetting onlyPlayers   = new BooleanSetting("Players only", true);
    private final BooleanSetting firstPerson   = new BooleanSetting("Show in F5", true);

    // Стрелка теперь использует текстуру Group 28
    private static final Identifier ARROW_TEXTURE = Javelin.id("icons/group28.png");

    private final List<ArrowData> arrowList = new ArrayList<>();

    private Arrows() {}

    @Override
    public void onDisable() {
        arrowList.clear();
        super.onDisable();
    }

    @EventTarget
    @Native
    public void onHudRender(EventHudRender event) {
        if (mc.player == null || mc.world == null) return;
        if (!firstPerson.isEnabled() && mc.options.getPerspective().isFirstPerson()) return;

        DrawContext context = event.getContext();
        float partialTicks = mc.getRenderTickCounter().getTickDelta(true);

        float size = arrowsDistance.getCurrent();
        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();
        float centerX = screenWidth / 2.0F;
        float centerY = screenHeight / 2.0F;

        // Обновляем список стрелок
        List<ArrowData> newList = new ArrayList<>();
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (!player.isAlive()) continue;

            double dist = mc.player.getPos().distanceTo(player.getPos());
            if (dist > 200.0) continue;

            boolean found = false;
            for (ArrowData ad : arrowList) {
                if (ad.player == player) {
                    ad.alpha = Math.min(1.0F, ad.alpha + 0.1F);
                    newList.add(ad);
                    found = true;
                    break;
                }
            }
            if (!found) {
                newList.add(new ArrowData(player, 0.0F));
            }
        }
        // Уменьшаем альфу для исчезнувших
        for (ArrowData ad : arrowList) {
            boolean stillThere = newList.stream().anyMatch(n -> n.player == ad.player);
            if (!stillThere) {
                ad.alpha -= 0.1F;
                if (ad.alpha > 0.0F) newList.add(ad);
            }
        }
        arrowList.clear();
        arrowList.addAll(newList);

        ColorRGBA themeColor = Javelin.getInstance().getThemeManager().getCurrentTheme().getColor();

        for (ArrowData arrow : arrowList) {
            if (arrow.alpha <= 0.01F) continue;

            double playerX = MathHelper.lerp(partialTicks, arrow.player.lastRenderX, arrow.player.getX())
                    - MathHelper.lerp(partialTicks, mc.player.lastRenderX, mc.player.getX());
            double playerZ = MathHelper.lerp(partialTicks, arrow.player.lastRenderZ, arrow.player.getZ())
                    - MathHelper.lerp(partialTicks, mc.player.lastRenderZ, mc.player.getZ());

            double cameraYaw = mc.gameRenderer.getCamera().getYaw();
            double cos = Math.cos(Math.toRadians(cameraYaw));
            double sin = Math.sin(Math.toRadians(cameraYaw));

            double rotY = -(playerZ * cos - playerX * sin);
            double rotX = -(playerX * cos + playerZ * sin);

            float angle = (float) (Math.atan2(rotY, rotX) * 180.0 / Math.PI);

            double x2 = size * arrow.alpha * Math.cos(Math.toRadians(angle)) + centerX;
            double y2 = size * arrow.alpha * Math.sin(Math.toRadians(angle)) + centerY;

            // Рисуем текстуру стрелки с цветом темы
            drawArrowTexture(context, (float) x2, (float) y2, angle, arrowSize.getCurrent() * arrow.alpha, themeColor.withAlpha((int)(arrow.alpha * 255)));
        }
    }

    private void drawArrowTexture(DrawContext context, float x, float y, float angle, float size, ColorRGBA color) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 0.0F);
        matrices.multiply(new org.joml.Quaternionf().fromAxisAngleDeg(0.0F, 0.0F, 1.0F, angle + 90.0F));

        float halfSize = size / 2.0F;
        DrawUtil.drawTexture(matrices, ARROW_TEXTURE, -halfSize, -halfSize, size, size, color);

        matrices.pop();
    }

    private static class ArrowData {
        PlayerEntity player;
        float alpha;

        ArrowData(PlayerEntity player, float alpha) {
            this.player = player;
            this.alpha = alpha;
        }
    }
}
