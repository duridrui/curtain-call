package focus;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

// 딴짓 통계(전환 횟수, 누적 시간, 앱별 횟수와 시간)를 모으는 곳
// update는 감지 스레드만, 다른 스레드는 get
public class DistractionStats {
    private String currentApp;                     // 지금 딴짓 중인 곳, 집중 중이면 null
    private long distractionStartMillis;           // 딴짓 시작 시각
    private volatile int switchCount;              // 집중 -> 딴짓 전환 횟수
    private volatile long totalDistractionMillis;  // 끝난 딴짓 시간의 합

    private final Map<String, Integer> appCounts = new HashMap<>();      // 이름별 횟수
    private final Map<String, Long> appMillis = new HashMap<>();         // 이름별 시간
    private volatile Map<String, Integer> appCountsView = Map.of();      // appCounts를 수정 불가 Map으로 복사한 것
    private volatile Map<String, Long> appMillisView = Map.of();         // appMillis를 수정 불가 Map으로 복사한 것
    private volatile String mostVisitedApp;                              // 가장 자주 간 곳, 아직 없으면 null

    // 감지 스레드가 1초마다 부름 : 지금 앱 이름, 지금 판정, 지금 시각
    public void update(String app, boolean distracting, long nowMillis) {
        String next = distracting ? app : null;                         // 지금 딴짓 중인 곳, 집중이면 null
        if (Objects.equals(next, currentApp))
            return;                                                     // 직전과 같으면 할 일 없음

        if (currentApp != null) {                                       // 직전에 딴짓 중이었으면 -> 그 딴짓 마감
            long spent = nowMillis - distractionStartMillis;            // 이번 딴짓에 머문 시간

            totalDistractionMillis += spent;                            // 전체 누적에 더하기
            appMillis.merge(currentApp, spent, Long::sum);              // 그 곳 시간 칸에 더하기
            appMillisView = Map.copyOf(appMillis);                      // appMillis를 다시 복사해 appMillisView 교체
        }
        if (next != null) {                                             // 지금 딴짓 중이면 -> 새 딴짓 시작
            if (currentApp == null)                                     // 집중에서 넘어왔을 때만
                switchCount++;                                          // 전환 횟수 +1
            distractionStartMillis = nowMillis;                         // 시작 시각 기록

            appCounts.merge(next, 1, Integer::sum);                     // 그 곳 횟수 +1
            appCountsView = Map.copyOf(appCounts);                      // appCounts를 다시 복사해 appCountsView 교체

            int count = appCounts.get(next);                            // 이 곳의 지금 횟수
            int leaderCount = mostVisitedApp == null ? 0 : appCounts.get(mostVisitedApp);               // 지금 1등의 횟수, 없으면 0
            if (count > leaderCount || (count == leaderCount && next.compareTo(mostVisitedApp) < 0))    // 횟수가 더 많거나, 같으면 이름순으로 앞선 곳
                mostVisitedApp = next;
        }
        currentApp = next;  // 다음 1초를 위해 지금 상태 기억
    }

    public int getSwitchCount() {                           // 집중 -> 딴짓 전환 횟수
        return switchCount;
    }

    public long getTotalDistractionMillis() {               // 전체 딴짓 시간(밀리초)
        return totalDistractionMillis;
    }

    public Map<String, Integer> getAppSwitchCounts() {      // 곳별 횟수
        return appCountsView;
    }

    public Map<String, Long> getAppDistractionMillis() {    // 곳별 시간
        return appMillisView;
    }

    public String getMostVisitedApp() {                     // 가장 자주 간 곳, 없으면 null
        return mostVisitedApp;
    }
}
