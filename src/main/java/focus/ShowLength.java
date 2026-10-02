package focus;

import java.util.List;

// 공연 길이: 1분~24시간 또는 무제한. 빠른 선택은 60,90,120분 (눈금자 계산은 예매 화면 쪽 LengthRuler)
public final class ShowLength {
    static final int MIN_MINUTES = 1;
    static final int MAX_MINUTES = 24 * 60;
    static final List<Integer> QUICK_MINUTES = List.of(60, 90, 120);

    private final long millis;          // 공연 길이, 무제한이면 0
    private final boolean unlimited;

    private ShowLength(long millis, boolean unlimited) {
        this.millis = millis;
        this.unlimited = unlimited;
    }

    // 분 단위 길이. 1분보다 짧으면 1분, 24시간보다 길면 24시간으로 자름
    public static ShowLength minutes(int minutes) {
        int m = Math.max(MIN_MINUTES, Math.min(MAX_MINUTES, minutes));
        return new ShowLength(m * 60_000L, false);
    }

    public static ShowLength unlimited() {
        return new ShowLength(0, true);
    }

    // 공연 이어보기의 남은 시간처럼 초 단위까지 그대로 쓰는 길이
    public static ShowLength ofMillis(long millis) {
        return new ShowLength(Math.max(1000, millis), false);
    }

    public boolean isUnlimited() {
        return unlimited;
    }

    // 분 (무제한이면 0, 초는 버림)
    public int getMinutes() {
        return (int) (millis / 60_000);
    }

    public long getMillis() {
        return millis;
    }
}
