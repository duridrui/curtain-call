package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ShowSessionTest {

    // 1분 공연: 시작 후 60000ms에 끝나고, 1ms 전에는 아직 안 끝남
    @Test
    void 시간다되면_끝남() {
        ShowSession show = new ShowSession();
        show.start(0, ShowLength.minutes(1));
        assertFalse(show.isTimeUp(59_999));
        assertTrue(show.isTimeUp(60_000));
        assertEquals(15_000, show.remainingMillis(45_000));
        assertEquals(0, show.remainingMillis(70_000));
    }

    // 무제한 공연: 24시간이 지나도 시간으로는 끝나지 않고 남은 시간은 0
    @Test
    void 무제한은_안끝남() {
        ShowSession show = new ShowSession();
        show.start(0, ShowLength.unlimited());
        assertFalse(show.isTimeUp(24 * 60 * 60 * 1000L));
        assertEquals(0, show.remainingMillis(1000));
        assertEquals(5000, show.elapsedMillis(5000));
    }

    // 유튜브를 보던 중 종료: 다음 tick 한 번에 종료 시각까지 딴짓으로 마감
    @Test
    void 종료하면_딴짓마감() {
        ShowSession show = new ShowSession();
        show.start(0, ShowLength.minutes(1));
        show.tick("Code", false, 500);
        assertTrue(show.tick("youtube.com", true, 1000));
        show.requestEnd(5000);
        assertFalse(show.tick("youtube.com", true, 6000));
        assertEquals(1, show.getStats().getSwitchCount());
        assertEquals(4000, show.getStats().getTotalDistractionMillis());
        assertTrue(show.consumeFinished());
        assertFalse(show.consumeFinished());
    }

    // 두 번째 공연: 새 기록표로 바뀌고 첫 공연 기록표는 그대로 남음
    @Test
    void 새공연은_새기록표() {
        ShowSession show = new ShowSession();
        show.start(0, ShowLength.minutes(1));
        show.tick("youtube.com", true, 1000);
        show.requestEnd(2000);
        show.tick("youtube.com", true, 3000);
        DistractionStats old = show.getStats();

        show.start(10_000, ShowLength.minutes(25));
        assertNotSame(old, show.getStats());
        assertEquals(0, show.getStats().getSwitchCount());
        assertEquals(1, old.getSwitchCount());
    }
}