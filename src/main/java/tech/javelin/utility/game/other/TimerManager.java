package tech.javelin.utility.game.other;

import tech.javelin.utility.interfaces.IMinecraft;

public class TimerManager implements IMinecraft {
    private static float timerSpeed = 1.0F;

    public static void setTimer(float speed) {
        timerSpeed = speed;
    }

    public static void resetTimer() {
        timerSpeed = 1.0F;
    }

    public static float getTimerSpeed() {
        return timerSpeed;
    }
}
