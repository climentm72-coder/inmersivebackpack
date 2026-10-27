package net.climent.inmersivebackpack.client;

public class SleepCinematicHandler {
    private static boolean isCinematicActive = false;
    private static float cinematicTicks = 0f;

    public static void startSleeping() {
        isCinematicActive = true;
        cinematicTicks = 0f;
    }

    public static void wakeUp() {
        isCinematicActive = false;
        cinematicTicks = 0f;
    }

    public static boolean isCinematicActive() {
        return isCinematicActive;
    }

    public static void tick() {
        if (isCinematicActive) {
            cinematicTicks += 0.05f;
        }
    }
}
