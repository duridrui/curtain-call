package focus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

// 예매한 티켓: 공연 길이, 좌석, 티켓 번호, 입장 시각, 집중 주제, 이번 공연의 허용 앱과 사이트
public class Ticket {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final ShowLength length;
    private final String seat;
    private final String number;
    private final LocalDateTime startAt;
    private final String topic;                 // 집중 주제
    private final List<String> allowedApps;     // 이번 공연에서 딴짓이 아닌 앱
    private final List<String> allowedSites;    // 이번 공연에서 딴짓이 아닌 사이트

    public Ticket(ShowLength length, String seat, String number, LocalDateTime startAt, String topic,
            List<String> allowedApps, List<String> allowedSites) {
        this.length = length;
        this.seat = seat;
        this.number = number;
        this.startAt = startAt;
        this.topic = FocusTopic.orOther(topic);
        this.allowedApps = List.copyOf(allowedApps);
        this.allowedSites = List.copyOf(allowedSites);
    }

    // 허용 목록을 고르기 전 티켓: 기본 허용 목록
    public Ticket(ShowLength length, String seat, String number, LocalDateTime startAt, String topic) {
        this(length, seat, number, startAt, topic, AllowList.DEFAULT, SiteAllowList.DEFAULT);
    }

    // 티켓 번호: CC-월일-시분-좌석 (예: CC-1012-1902-C07)
    static String number(LocalDateTime at, int row, int col) {
        return String.format("CC-%02d%02d-%02d%02d-%c%02d",
            at.getMonthValue(), at.getDayOfMonth(), at.getHour(), at.getMinute(), (char) ('A' + row), col + 1);
    }

    // 공연 이어보기: 길이만 남은 시간으로 바꾼 같은 티켓
    public Ticket withLength(ShowLength next) {
        return new Ticket(next, seat, number, startAt, topic, allowedApps, allowedSites);
    }

    // 공연 시간대: "19:02 ~ 19:52", 무제한이면 "19:02 ~ 무제한"
    public String timeRange() {
        if (length.isUnlimited())
            return startAt.format(TIME) + " ~ 무제한";
        return startAt.format(TIME) + " ~ " + startAt.plusSeconds(length.getMillis() / 1000).format(TIME);
    }

    public ShowLength getLength() {
        return length;
    }

    public String getSeat() {
        return seat;
    }

    public String getNumber() {
        return number;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public String getTopic() {
        return topic;
    }

    public List<String> getAllowedApps() {
        return allowedApps;
    }

    public List<String> getAllowedSites() {
        return allowedSites;
    }
}
