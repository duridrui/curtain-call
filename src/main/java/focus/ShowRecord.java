package focus;

import java.util.HashMap;
import java.util.Map;

// 끝난 공연 한 편의 관람 기록 (파일에 저장하고 다시 읽는 단위)
public class ShowRecord {
    private final String id;                    // 공연 시작 시각 "yyyyMMdd-HHmmss", 파일 이름으로도 씀
    private final String title;                 // 공연 길이 이름 ("1시간 30분", "무제한"). 예전 기록은 공연 종류 이름
    private final int plannedMinutes;           // 예매한 공연 길이(분), 무제한이면 0
    private final String seat;                  // 좌석 이름
    private final long showMillis;              // 실제로 공연한 시간
    private final long distractionMillis;       // 딴짓(인터미션) 시간
    private final int switchCount;              // 막이 내린 횟수 (집중 -> 딴짓 전환)
    private final String mostVisited;           // 딴짓으로 가장 많이 넘어간 앱,사이트, 딴짓이 없으면 null
    private final Map<String, Integer> counts;  // 딴짓으로 넘어간 앱,사이트별 횟수 (예: "Discord"=3)
    private final String topic;                 // 집중 주제, 주제가 없던 예전 기록은 '기타'
    private final ShowEnding ending;            // 끝난 이유 (정상 종료 / 막이 다 닫혀 중도 종료)
    private final int acts;                     // 막 수: 공연 이어보기를 하면 2막, 3막으로 늘어남

    // 주제와 끝난 이유가 없는 예전 기록: 주제 '기타', 관람 완료
    public ShowRecord(String id, String title, int plannedMinutes, String seat, long showMillis,
            long distractionMillis, int switchCount, String mostVisited, Map<String, Integer> counts) {
        this(id, title, plannedMinutes, seat, showMillis, distractionMillis, switchCount, mostVisited, counts,
            FocusTopic.OTHER, ShowEnding.COMPLETED);
    }

    // 이어보기 없이 끝난 공연: 막 수 1
    public ShowRecord(String id, String title, int plannedMinutes, String seat, long showMillis,
            long distractionMillis, int switchCount, String mostVisited, Map<String, Integer> counts,
            String topic, ShowEnding ending) {
        this(id, title, plannedMinutes, seat, showMillis, distractionMillis, switchCount, mostVisited, counts,
            topic, ending, 1);
    }

    // 필드에 값을 넣는 생성자: 주제가 비면 '기타', 끝난 이유가 없으면 관람 완료, 막 수는 최소 1
    public ShowRecord(String id, String title, int plannedMinutes, String seat, long showMillis,
            long distractionMillis, int switchCount, String mostVisited, Map<String, Integer> counts,
            String topic, ShowEnding ending, int acts) {
        this.id = id;
        this.title = title;
        this.plannedMinutes = plannedMinutes;
        this.seat = seat;
        this.showMillis = showMillis;
        this.distractionMillis = distractionMillis;
        this.switchCount = switchCount;
        this.mostVisited = mostVisited;
        this.counts = Map.copyOf(counts);
        this.topic = FocusTopic.orOther(topic);
        this.ending = ending == null ? ShowEnding.COMPLETED : ending;
        this.acts = Math.max(1, acts);
    }

    // 공연 이어보기: 앞 막 기록에 다음 막을 더해 같은 기록(같은 id)으로. 한 번이라도 막이 다 닫혔으면 중도 종료 표시
    static ShowRecord mergeAct(ShowRecord earlier, ShowRecord next) {
        Map<String, Integer> counts = new HashMap<>(earlier.counts);
        next.counts.forEach((name, n) -> counts.merge(name, n, Integer::sum));
        boolean down = earlier.ending == ShowEnding.CURTAIN_DOWN || next.ending == ShowEnding.CURTAIN_DOWN;
        return new ShowRecord(earlier.id, earlier.title, earlier.plannedMinutes, earlier.seat,
            earlier.showMillis + next.showMillis, earlier.distractionMillis + next.distractionMillis,
            earlier.switchCount + next.switchCount, mostVisited(counts), counts,
            earlier.topic, down ? ShowEnding.CURTAIN_DOWN : ShowEnding.COMPLETED, earlier.acts + next.acts);
    }

    // 횟수가 가장 많은 앱,사이트를 고름. 횟수가 같으면 이름이 알파벳 순으로 앞선 쪽. 딴짓이 없으면 null
    static String mostVisited(Map<String, Integer> counts) {
        String best = null;
        for (Map.Entry<String, Integer> e : counts.entrySet()) {
            if (best == null)
                best = e.getKey();
            else {
                int diff = e.getValue() - counts.get(best);
                if (diff > 0 || (diff == 0 && e.getKey().compareTo(best) < 0))
                    best = e.getKey();
            }
        }
        return best;
    }

    // 이어볼 때 남은 길이: 예매한 길이 - 지금까지 본 시간. 무제한은 무제한, 남은 시간이 없으면 null
    static ShowLength remainingAfter(ShowRecord record) {
        if (record.plannedMinutes == 0)
            return ShowLength.unlimited();
        long rest = record.plannedMinutes * 60_000L - record.showMillis;
        return rest < 1000 ? null : ShowLength.ofMillis(rest);
    }

    public String getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public int getPlannedMinutes() {
        return plannedMinutes;
    }

    public String getSeat() {
        return seat;
    }

    public long getShowMillis() {
        return showMillis;
    }

    public long getDistractionMillis() {
        return distractionMillis;
    }

    public long getFocusMillis() {
        return Math.max(0, showMillis - distractionMillis);
    }

    public int getSwitchCount() {
        return switchCount;
    }

    public String getMostVisited() {
        return mostVisited;
    }

    public Map<String, Integer> getCounts() {
        return counts;
    }

    public String getTopic() {
        return topic;
    }

    public ShowEnding getEnding() {
        return ending;
    }

    public int getActs() {
        return acts;
    }
}
