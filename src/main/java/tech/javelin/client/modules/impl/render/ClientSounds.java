package tech.javelin.client.modules.impl.render;

import com.darkmagician6.eventapi.EventTarget;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import tech.javelin.base.events.impl.other.EventModuleToggle;
import tech.javelin.client.modules.api.Category;
import tech.javelin.client.modules.api.Module;
import tech.javelin.client.modules.api.ModuleAnnotation;
import tech.javelin.client.modules.api.setting.impl.BooleanSetting;
import tech.javelin.client.modules.api.setting.impl.ModeSetting;
import tech.javelin.client.modules.api.setting.impl.NumberSetting;

@ModuleAnnotation(
        name = "ClientSounds",
        category = Category.RENDER,
        description = "Звуки при включении/выключении модулей"
)
public class ClientSounds extends Module {
    public static final ClientSounds INSTANCE = new ClientSounds();
    private static final float LOUDNESS_MULTIPLIER = 1.35F;

    // Original sounds
    private static final SoundEvent ENABLE_SOUND = SoundEvent.of(Identifier.of("javelin", "enable"));
    private static final SoundEvent DISABLE_SOUND = SoundEvent.of(Identifier.of("javelin", "disable"));
    
    // New sound types
    private static final SoundEvent MODERN_ENABLE = SoundEvent.of(Identifier.of("javelin", "modern_enable"));
    private static final SoundEvent MODERN_DISABLE = SoundEvent.of(Identifier.of("javelin", "modern_disable"));
    private static final SoundEvent CLICK_SOUND = SoundEvent.of(Identifier.of("javelin", "click"));
    private static final SoundEvent HOVER_SOUND = SoundEvent.of(Identifier.of("javelin", "hover"));
    private static final SoundEvent ALERT_SOUND = SoundEvent.of(Identifier.of("javelin", "alert"));
    private static final SoundEvent SUCCESS_SOUND = SoundEvent.of(Identifier.of("javelin", "success"));
    private static final SoundEvent ERROR_SOUND = SoundEvent.of(Identifier.of("javelin", "error"));
    private static final SoundEvent POP_SOUND = SoundEvent.of(Identifier.of("javelin", "pop"));
    private static final SoundEvent BELL_SOUND = SoundEvent.of(Identifier.of("javelin", "bell"));

    public final NumberSetting volume = new NumberSetting("Громкость", 1.8F, 0.1F, 4.0F, 0.1F);
    public final NumberSetting pitch = new NumberSetting("Питч", 1.0F, 0.5F, 2.0F, 0.1F);
    public final ModeSetting soundMode = new ModeSetting("Режим звука", "Классический", "Классический", "Классик 2.0", "Современный", "Майнкрафт", "Поп", "Колокол");
    public final BooleanSetting combatSounds = new BooleanSetting("Звуки комбата", "Особые звуки для боевых модулей", true);
    public final BooleanSetting renderSounds = new BooleanSetting("Звуки рендера", "Особые звуки для визуальных модулей", false);
    public final BooleanSetting playOnClick = new BooleanSetting("Звук клика", "Звук при нажатии в GUI", true);
    public final BooleanSetting playOnHover = new BooleanSetting("Звук наведения", "Звук при наведении на элементы", false);

    private ClientSounds() {}

    @EventTarget
    private void onToggle(EventModuleToggle event) {
        if (mc.player == null || mc.world == null) return;
        if (event.getModule() == this) return;

        SoundEvent sound = getSoundForMode(event.isEnabled(), event.getModule());
        if (sound == null) return;
        
        float loudVolume = Math.min(4.0F, volume.getCurrent() * LOUDNESS_MULTIPLIER);
        float p = pitch.getCurrent();
        
        // Adjust pitch based on module category
        if (combatSounds.isEnabled() && event.getModule().getCategory() == Category.COMBAT) {
            p *= 1.2f;
            loudVolume *= 1.1f;
        } else if (renderSounds.isEnabled() && event.getModule().getCategory() == Category.RENDER) {
            p *= 0.8f;
        }
        
        mc.getSoundManager().play(
                PositionedSoundInstance.master(sound, Math.min(2.0F, p), loudVolume)
        );
    }
    
    private SoundEvent getSoundForMode(boolean enabled, Module module) {
        String mode = soundMode.get();
        
        switch (mode) {
            case "Классический":
                return enabled ? ENABLE_SOUND : DISABLE_SOUND;
            case "Классик 2.0":
                // Classic cheat-style sounds using vanilla Minecraft
                return enabled ?
                    SoundEvent.of(Identifier.of("minecraft", "entity.experience_orb.pickup")) :
                    SoundEvent.of(Identifier.of("minecraft", "entity.item.break"));
            case "Современный":
                return enabled ? MODERN_ENABLE : MODERN_DISABLE;
            case "Майнкрафт":
                // Use vanilla Minecraft sounds
                return enabled ? 
                    SoundEvent.of(Identifier.of("minecraft", "block.note_block.pling")) :
                    SoundEvent.of(Identifier.of("minecraft", "block.note_block.bass"));
            case "Поп":
                return POP_SOUND;
            case "Колокол":
                return enabled ? BELL_SOUND : POP_SOUND;
            default:
                return enabled ? ENABLE_SOUND : DISABLE_SOUND;
        }
    }
    
    public void playClickSound() {
        if (!isEnabled() || !playOnClick.isEnabled() || mc.player == null) return;
        mc.getSoundManager().play(
            PositionedSoundInstance.master(CLICK_SOUND, pitch.getCurrent() * 1.5f, volume.getCurrent())
        );
    }
    
    public void playHoverSound() {
        if (!isEnabled() || !playOnHover.isEnabled() || mc.player == null) return;
        mc.getSoundManager().play(
            PositionedSoundInstance.master(HOVER_SOUND, pitch.getCurrent() * 2.0f, volume.getCurrent() * 0.5f)
        );
    }
    
    public void playAlertSound() {
        if (!isEnabled() || mc.player == null) return;
        mc.getSoundManager().play(
            PositionedSoundInstance.master(ALERT_SOUND, 1.0f, Math.min(4.0F, volume.getCurrent() * 1.5f))
        );
    }
    
    public void playSuccessSound() {
        if (!isEnabled() || mc.player == null) return;
        mc.getSoundManager().play(
            PositionedSoundInstance.master(SUCCESS_SOUND, 1.2f, volume.getCurrent())
        );
    }
    
    public void playErrorSound() {
        if (!isEnabled() || mc.player == null) return;
        mc.getSoundManager().play(
            PositionedSoundInstance.master(ERROR_SOUND, 0.8f, volume.getCurrent())
        );
    }
}