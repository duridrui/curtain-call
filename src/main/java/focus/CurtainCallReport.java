package focus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

// 커튼콜(공연 결과)에 보여 줄 숫자 계산
public class CurtainCallReport {

    // 밀리초를 "45초" / "12분 5초" / "1시간 2분" 꼴의 글로
    static String formatDuration(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long rest = seconds % 60;
        if (hours > 0)
            return hours + "시간 " + minutes + "분";       // 1시간 넘으면 초는 뺌
        if (minutes > 0)
            return minutes + "분 " + rest + "초";
        return rest + "초";
    }

    // 집중 비율(%) = (공연 시간 - 딴짓 시간) / 공연 시간. 0~100 사이로 자름
    static int focusPercent(long showMillis, long distractionMillis) {
        if (showMillis <= 0)
            return 0;
        long focus = Math.max(0, showMillis - distractionMillis);
        return (int) Math.min(100, Math.round(focus * 100.0 / showMillis));
    }

    // 횟수 많은 순 상위 n개, 동점이면 이름 사전순 (getMostVisitedApp과 같은 규칙)
    static List<Map.Entry<String, Integer>> topEntries(Map<String, Integer> counts, int n) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(counts.entrySet());    // 불변 Map은 바로 정렬할 수 없어 옮겨 담음
        list.sort(Comparator.comparing((Map.Entry<String, Integer> e) -> e.getValue()).reversed()
            .thenComparing(Map.Entry::getKey));
        return list.subList(0, Math.min(n, list.size()));
    }

    // 기록 이름 "20261001-190200"을 "10월 1일 19:02"로, 형식이 다르면 그대로
    static String formatWhen(String id) {
        if (id == null || !id.matches("\\d{8}-\\d{6}"))
            return id;
        int month = Integer.parseInt(id.substring(4, 6));
        int day = Integer.parseInt(id.substring(6, 8));
        return month + "월 " + day + "일 " + id.substring(9, 11) + ":" + id.substring(11, 13);
    }

    // 끝난 공연(마감까지 끝난 ShowSession)을 저장할 기록으로 바꿈
    static ShowRecord toRecord(String id, ShowSession show, String seat) {
        DistractionStats stats = show.getStats();
        return new ShowRecord(id, lengthText(show.getLength()), show.getLength().getMinutes(), seat,
            show.getShowMillis(), stats.getTotalDistractionMillis(), stats.getSwitchCount(),
            stats.getMostVisitedApp(), stats.getAppSwitchCounts(), show.getTopic(), show.getEnding());
    }

    // 직전 공연과 비교한 문장들. 직전 공연이 없으면 안내 한 줄
    static List<String> compare(ShowRecord previous, ShowRecord current) {
        if (previous == null)
            return List.of("첫 공연이라 비교할 기록이 없습니다.");
        List<String> lines = new ArrayList<>();
        int before = focusPercent(previous.getShowMillis(), previous.getDistractionMillis());
        int now = focusPercent(current.getShowMillis(), current.getDistractionMillis());
        lines.add("집중 비율 " + now + "% (지난 공연 " + before + "%" + change(now - before, "%p", "올랐어요", "내려갔어요") + ")");
        int switchDiff = current.getSwitchCount() - previous.getSwitchCount();
        lines.add("막이 내린 횟수 " + current.getSwitchCount() + "번 (지난 공연" + countChange(switchDiff) + ")");
        lines.add("인터미션 " + formatDuration(current.getDistractionMillis())
            + " (지난 공연 " + formatDuration(previous.getDistractionMillis()) + ")");
        return lines;
    }

    static String change(int diff, String unit, String up, String down) {
        if (diff == 0)
            return "와 같아요";
        return "보다 " + Math.abs(diff) + unit + " " + (diff > 0 ? up : down);
    }

    static String countChange(int diff) {
        if (diff == 0)
            return "과 같아요";
        return "보다 " + Math.abs(diff) + "번 " + (diff > 0 ? "늘었어요" : "줄었어요");
    }

    // 공연 길이 글: 무제한, 이어보기처럼 초가 남은 길이는 초까지, 나머지는 시간과 분
    private static String lengthText(ShowLength length) {
        if (length.isUnlimited())
            return "무제한";
        if (length.getMillis() % 60_000 != 0)
            return formatDuration(length.getMillis());
        int h = length.getMinutes() / 60, m = length.getMinutes() % 60;
        if (h == 0)
            return m + "분";
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }
}
