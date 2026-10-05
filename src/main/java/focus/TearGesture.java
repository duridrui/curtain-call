package focus;

final class TearGesture {
    static final double FULL_DISTANCE = 320;
    static final double COMPLETE_RATIO = 0.6;
    static final double FLING_SPEED = 0.11;
    static final double END_FOLLOW = 0.6;

    private boolean pressed;
    private double startX;
    private double lastX;
    private long lastTime;
    private double prevX;
    private long prevTime;
    private boolean hasPrev;

    static double progress(double distance) {
        return Math.max(0, Math.min(1, distance / FULL_DISTANCE));
    }

    static double shownOffset(double distance) {
        if (distance <= 0)
            return 0;
        if (distance <= FULL_DISTANCE)
            return distance * (1 - (1 - END_FOLLOW) * distance / FULL_DISTANCE);
        return FULL_DISTANCE * END_FOLLOW + (distance - FULL_DISTANCE) * (1 - 2 * (1 - END_FOLLOW));
    }

    void press(double x, long timeMillis) {
        pressed = true;
        startX = x;
        lastX = x;
        lastTime = timeMillis;
        hasPrev = false;
    }

    void drag(double x, long timeMillis) {
        if (!pressed)
            return;
        if (timeMillis == lastTime) {
            lastX = x;
            return;
        }
        prevX = lastX;
        prevTime = lastTime;
        hasPrev = true;
        lastX = x;
        lastTime = timeMillis;
    }

    boolean release(double x, long timeMillis) {
        if (!pressed)
            return false;
        drag(x, timeMillis);
        pressed = false;
        double distance = lastX - startX;
        double speed = hasPrev && lastTime > prevTime ? (lastX - prevX) / (lastTime - prevTime) : 0;
        return distance >= FULL_DISTANCE * COMPLETE_RATIO || speed > FLING_SPEED;
    }

    double distance() {
        return lastX - startX;
    }
}
