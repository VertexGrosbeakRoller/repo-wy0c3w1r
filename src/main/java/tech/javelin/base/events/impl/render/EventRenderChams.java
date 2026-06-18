package tech.javelin.base.events.impl.render;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import tech.javelin.base.events.callables.EventCancellable;

public class EventRenderChams extends EventCancellable {
    private final LivingEntity entity;
    private final EntityModel<?> model;
    private final MatrixStack matrices;
    private final VertexConsumerProvider vertexConsumers;
    private final int light;
    private final float limbAngle;
    private final float limbDistance;
    private final float animationProgress;
    private final float headYaw;
    private final float headPitch;

    public EventRenderChams(LivingEntity entity, EntityModel<?> model, MatrixStack matrices,
                            VertexConsumerProvider vertexConsumers, int light,
                            float limbAngle, float limbDistance, float animationProgress,
                            float headYaw, float headPitch) {
        this.entity = entity;
        this.model = model;
        this.matrices = matrices;
        this.vertexConsumers = vertexConsumers;
        this.light = light;
        this.limbAngle = limbAngle;
        this.limbDistance = limbDistance;
        this.animationProgress = animationProgress;
        this.headYaw = headYaw;
        this.headPitch = headPitch;
    }

    public LivingEntity getEntity() {
        return entity;
    }

    public EntityModel<?> getModel() {
        return model;
    }

    public MatrixStack getMatrices() {
        return matrices;
    }

    public VertexConsumerProvider getVertexConsumers() {
        return vertexConsumers;
    }

    public int getLight() {
        return light;
    }

    public float getLimbAngle() {
        return limbAngle;
    }

    public float getLimbDistance() {
        return limbDistance;
    }

    public float getAnimationProgress() {
        return animationProgress;
    }

    public float getHeadYaw() {
        return headYaw;
    }

    public float getHeadPitch() {
        return headPitch;
    }
}
