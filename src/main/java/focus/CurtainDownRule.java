package focus;

// 막이 다 닫히면(딴짓 30초 누적) 공연을 중도 종료하는 규칙. 화면 스레드의 1초 타이머만 부른다.
// 막이 닫히는 규칙 자체는 그대로 쓰고, 다 닫힌 첫 1초에 한 번만 알린다.
final class CurtainDownRule {
    static final long CURTAIN_CALL_DELAY_MILLIS = 1500;     // 막이 내린 안내를 보여 준 뒤 커튼콜로 넘어가는 시간

    private boolean fired;                                  // 이번 공연에서 이미 끝내라고 알렸는지

    // 공연 중 막이 다 닫혔으면 true를 한 번만 돌려준다. 공연이 끝나면 다음 공연을 위해 초기화
    boolean onTick(boolean running, long elapsedSeconds, boolean distracting) {
        if (!running) {
            fired = false;
            return false;
        }
        if (fired || !CurtainOverlay.isFullyClosed(elapsedSeconds, distracting))
            return false;
        fired = true;
        return true;
    }

    // 막이 닫힌 시각부터 1500ms가 지나도록 남은 시간, 이미 지났으면 0
    static long curtainCallDelay(long closedAtMillis, long nowMillis) {
        return Math.max(0, CURTAIN_CALL_DELAY_MILLIS - (nowMillis - closedAtMillis));
    }
}
