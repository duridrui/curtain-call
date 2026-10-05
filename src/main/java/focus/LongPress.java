package focus;

// 공연 종료 버튼의 길게 누르기: 600ms를 계속 누르고 있으면 한 번 실행
final class LongPress {
    static final long HOLD_MILLIS = 600;

    private boolean pressed;
    private boolean fired;      // 이번 누름에서 이미 실행했는지
    private long startMillis;

    void press(long nowMillis) {
        pressed = true;
        fired = false;
        startMillis = nowMillis;
    }

    void release() {
        pressed = false;
    }

    // 링이 찬 정도 0~1. 누르고 있지 않으면 0
    double progress(long nowMillis) {
        if (!pressed)
            return 0;
        return Math.max(0, Math.min(1, (nowMillis - startMillis) / (double) HOLD_MILLIS));
    }

    // 600ms를 채운 첫 순간에만 true
    boolean poll(long nowMillis) {
        if (!pressed || fired || progress(nowMillis) < 1)
            return false;
        fired = true;
        return true;
    }
}
