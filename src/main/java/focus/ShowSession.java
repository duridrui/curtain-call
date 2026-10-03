package focus;

// 공연 상태: 공연 중인지, 시작, 종료 시각, 이번 공연의 통계
// 규칙: tick은 감지 스레드만 부름. start/requestEnd는 화면이나 메뉴가 부름
public class ShowSession {
    private volatile boolean running;                                   // 공연 중
    private volatile boolean endPending;                                // 종료 요청이 와서 마감을 기다림
    private volatile boolean finished;                                  // 마감 끝
    private volatile long startMillis;
    private volatile long endMillis;
    private volatile ShowLength length = ShowLength.minutes(ShowLength.QUICK_MINUTES.get(0));
    private volatile String topic = FocusTopic.OTHER;                   // 집중 주제
    private volatile ShowEnding ending = ShowEnding.COMPLETED;          // 끝난 이유
    private volatile DistractionStats stats = new DistractionStats();   // 이번 공연의 기록표
    private String lastKey = "(unknown)";                               // 마지막으로 적은 이름

    // 주제 없이 시작하면 기타로 시작
    public void start(long nowMillis, ShowLength length) {
        start(nowMillis, length, FocusTopic.OTHER);
    }

    // 새 공연 시작: 기록표를 새로 만들고 이전 공연 값을 되돌린 뒤 마지막에 공연 중으로 바꿈
    public void start(long nowMillis, ShowLength length, String topic) {
        stats = new DistractionStats();
        this.length = length;
        this.topic = FocusTopic.orOther(topic);
        ending = ShowEnding.COMPLETED;
        startMillis = nowMillis;
        endMillis = 0;
        endPending = false;
        finished = false;
        running = true;
    }

    // 이유 없이 종료하면 끝까지 본 것으로 처리
    public void requestEnd(long nowMillis) {
        requestEnd(nowMillis, ShowEnding.COMPLETED);
    }

    // 종료 요청: 먼저 온 요청의 시각과 이유만 적고, 딴짓 마감은 감지 스레드가 함
    public void requestEnd(long nowMillis, ShowEnding ending) {
        if (!running)
            return;
        this.ending = ending;
        endMillis = nowMillis;
        endPending = true;
        running = false;
    }

    // 감지 스레드만 부름: 공연 중이면 통계에 기록, 종료 직후 한 번 진행 중 딴짓 마감, 돌려주는 값은 커튼을 닫을지
    public boolean tick(String key, boolean distracting, long nowMillis) {
        if (running) {
            stats.update(key, distracting, nowMillis);
            lastKey = key;
            return distracting;
        }

        // 종료 직후 한 번만: 마지막 곳에서 종료 시각에 집중으로 돌아온 것으로 적어 진행 중 딴짓을 마감
        if (endPending) {
            stats.update(lastKey, false, endMillis);
            endPending = false;
            finished = true;
        }
        return false;
    }

    // 커튼콜을 띄울 차례면 true를 한 번만 돌려줌
    public boolean consumeFinished() {
        if (!finished)
            return false;
        finished = false;
        return true;
    }

    // 감지 스레드가 osascript를 돌릴 필요가 있는지: 공연 중이거나 마감이 남았을 때만
    public boolean needsDetection() {
        return running || endPending;
    }

    // 공연 길이를 다 채웠는지, 무제한이면 끝나지 않음
    public boolean isTimeUp(long nowMillis) {
        return !length.isUnlimited() && nowMillis - startMillis >= length.getMillis();
    }

    // 남은 시간, 지났거나 무제한이면 0
    public long remainingMillis(long nowMillis) {
        if (length.isUnlimited())
            return 0;
        return Math.max(0, length.getMillis() - (nowMillis - startMillis));
    }

    // 시작부터 지난 시간, 무제한 공연에서 남은 시간 대신 보여 줌
    public long elapsedMillis(long nowMillis) {
        return Math.max(0, nowMillis - startMillis);
    }

    // 무제한 공연인지
    public boolean isUnlimited() {
        return length.isUnlimited();
    }

    // 공연한 시간: 시작부터 종료 요청까지
    public long getShowMillis() {
        return Math.max(0, endMillis - startMillis);
    }

    // 집중한 시간: 공연한 시간에서 딴짓 시간을 뺌
    public long getFocusMillis() {
        return Math.max(0, getShowMillis() - stats.getTotalDistractionMillis());
    }

    // 다른 클래스가 공연 정보를 읽는 곳, 값은 start, requestEnd, tick으로만 바뀜
    public boolean isRunning() {
        return running;
    }

    public long getStartMillis() {
        return startMillis;
    }

    public long getEndMillis() {
        return endMillis;
    }

    public ShowLength getLength() {
        return length;
    }

    public String getTopic() {
        return topic;
    }

    public ShowEnding getEnding() {
        return ending;
    }

    public DistractionStats getStats() {
        return stats;
    }
}