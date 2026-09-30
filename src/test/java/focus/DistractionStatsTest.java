package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;

class DistractionStatsTest {

    @Test
    void 딴짓이_이어져도_한번만_센다() {
        DistractionStats stats = new DistractionStats();
        stats.update("Code", false, 0);              // 집중
        stats.update("Safari", true, 1000);          // 딴짓 시작
        stats.update("Safari", true, 2000);          // 딴짓 계속
        stats.update("Safari", true, 3000);          // 딴짓 계속
        assertEquals(1, stats.getSwitchCount());
    }

    @Test
    void 복귀하면_딴짓_시간이_누적된다() {
        DistractionStats stats = new DistractionStats();
        stats.update("Safari", true, 1000);          // 딴짓 시작
        stats.update("Code", false, 6000);           // 5초 뒤 복귀
        stats.update("Safari", true, 10000);         // 다시 딴짓
        stats.update("Code", false, 12000);          // 2초 뒤 복귀
        assertEquals(7000, stats.getTotalDistractionMillis());
    }

    @Test
    void 딴짓_중_장소를_옮기면_곳별로_따로_잰다() {
        DistractionStats stats = new DistractionStats();
        stats.update("youtube.com", true, 0);          // 유튜브에서 딴짓 시작
        stats.update("instagram.com", true, 10000);    // 10초 뒤 인스타로 이동
        stats.update("Code", false, 15000);            // 5초 뒤 복귀
        assertEquals(1, stats.getSwitchCount());                                 // 막이 내린 건 1번
        assertEquals(10000L, stats.getAppDistractionMillis().get("youtube.com")); // 유튜브 10초
        assertEquals(5000L, stats.getAppDistractionMillis().get("instagram.com")); // 인스타 5초
        assertEquals(1, stats.getAppSwitchCounts().get("instagram.com"));        // 인스타도 1번 간 걸로 셈
    }

    @Test
    void 동점이면_이름순으로_앞선_곳이_1등() {
        DistractionStats stats = new DistractionStats();
        stats.update("Safari", true, 1000);      // Safari 1번
        stats.update("Code", false, 2000);
        stats.update("Discord", true, 3000);     // Discord 1번 -> 동점
        stats.update("Code", false, 4000);
        assertEquals("Discord", stats.getMostVisitedApp());   // D가 S보다 앞
    }

    @Test
    void 곳별_횟수는_고칠_수_없는_복사본() {
        DistractionStats stats = new DistractionStats();
        stats.update("Safari", true, 1000);
        assertThrows(UnsupportedOperationException.class,
                () -> stats.getAppSwitchCounts().put("Safari", 99));   // 고치려 하면 에러가 나야 통과
    }
}