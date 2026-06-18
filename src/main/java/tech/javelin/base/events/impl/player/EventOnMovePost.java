package tech.javelin.base.events.impl.player;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.math.Vec3d;
import tech.javelin.base.events.callables.EventCancellable;

@Getter
@Setter
public class EventOnMovePost extends EventCancellable {
    private float speed;
    private Vec3d movementInput;

    public EventOnMovePost(float speed, Vec3d movementInput) {
        this.speed = speed;
        this.movementInput = movementInput;
    }
}
