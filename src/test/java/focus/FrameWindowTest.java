package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

// 그릴 장보다 몇 장 앞서 디코드해 두는 창. 디코드는 다른 스레드에서 하므로 테스트는 손으로 돌리는 실행기를 씀
class FrameWindowTest {

    // 맡긴 일을 쌓아 두었다가 runAll()에서 한꺼번에 돌림 (디코드가 아직 안 끝난 상태를 만들 때)
    static final class ManualExecutor implements Executor {
        final ArrayDeque<Runnable> queue = new ArrayDeque<>();
        public void execute(Runnable r) { queue.add(r); }
        void runAll() { while (!queue.isEmpty()) queue.poll().run(); }
    }

    @Test
    void 움직이는방향으로_몇장앞까지디코드() {
        List<Integer> decoded = new ArrayList<>();
        FrameWindow<String> w = new FrameWindow<>(100, 3, 0, i -> { decoded.add(i); return "장" + i; }, Runnable::run, i -> { });
        w.aim(10, 1);
        assertEquals(List.of(10, 11, 12, 13), w.held());
        assertEquals("장12", w.get(12));
        w.aim(20, -1);                           // 다시 열림: 앞 장 쪽으로
        assertEquals(List.of(17, 18, 19, 20), w.held());
    }

    @Test
    void 빨리움직이면_건너뛰며고름() {
        FrameWindow<String> w = new FrameWindow<>(900, 3, 0, i -> "장" + i, Runnable::run, i -> { });
        w.aim(500, -3);                          // 한 번에 3장씩 줄어듦
        assertEquals(List.of(491, 494, 497, 500), w.held());
    }

    @Test
    void 끝안넘고_고정장은안놓음() {
        FrameWindow<String> w = new FrameWindow<>(90, 3, 0, i -> "장" + i, Runnable::run, i -> { });
        w.pin(0, 1);
        w.aim(88, 1);
        assertEquals(List.of(0, 1, 88, 89), w.held());
        w.release();                                   // 움직임이 끝나면 고정한 장만 남김
        assertEquals(List.of(0, 1), w.held());
    }

    @Test
    void 디코드안끝나면null_끝나면알림() {
        ManualExecutor ex = new ManualExecutor();
        List<Integer> ready = new ArrayList<>();
        FrameWindow<String> w = new FrameWindow<>(100, 1, 0, i -> "장" + i, ex, ready::add);
        w.aim(5, 1);
        assertNull(w.get(5));
        ex.runAll();
        assertEquals("장5", w.get(5));
        assertEquals(List.of(5, 6), ready);
    }

    @Test
    void 지난장은_가까운순으로여분만남김() {
        ManualExecutor ex = new ManualExecutor();
        FrameWindow<String> w = new FrameWindow<>(900, 0, 4, i -> "장" + i, ex, i -> { });
        w.aim(300, -1);
        ex.runAll();
        w.aim(296, -1);                                     // 296은 아직 디코드 중, 300은 여분 칸에 남음
        assertEquals(List.of(296, 300), w.held());
        assertEquals("장300", w.get(300));            // 빨리 움직일 때 296 대신 보일 수 있는 장
        assertNull(w.get(296));
    }

    @Test
    void 디코드실패하면_없는장() {
        FrameWindow<String> w = new FrameWindow<>(10, 0, 0, i -> { throw new IllegalStateException("깨진 PNG"); }, Runnable::run, i -> { });
        w.aim(3, 1);
        assertNull(w.get(3));
    }

    @Test
    void 놓은장은_대기중이어도디코드안함() {
        // 측정: 다시 열림 때 놓은 장 327개가 대기열에서 그대로 디코드돼 막 오름 장이 3초 동안 밀렸다
        ManualExecutor ex = new ManualExecutor();
        List<Integer> decoded = new ArrayList<>();
        FrameWindow<String> w = new FrameWindow<>(900, 2, 0, i -> { decoded.add(i); return "장" + i; }, ex, i -> { });
        w.aim(800, -60);
        w.aim(100, 1);                           // 800, 740, 680은 놓음
        ex.runAll();
        assertEquals(List.of(100, 101, 102), decoded);
    }

    @Test
    void 다풀린장놓으면_버퍼돌려줌() {
        List<String> recycled = new ArrayList<>();
        FrameWindow<String> w = new FrameWindow<>(100, 1, 0, i -> "장" + i, Runnable::run, i -> { }, recycled::add);
        w.aim(10, 1);
        w.aim(50, 1);
        assertEquals(java.util.Set.of("장10", "장11"), new java.util.HashSet<>(recycled));   // 놓는 순서는 상관없음
    }

    @Test
    void 정해둔장목록을_그대로미리디코드() {
        FrameWindow<String> w = new FrameWindow<>(900, 0, 1, i -> "장" + i, Runnable::run, i -> { });
        w.aim(500, 1);
        w.aimList(List.of(498, 470, 400));                      // 다시 열림 일정: 간격이 고르지 않아도 됨
        assertEquals(List.of(400, 470, 498, 500), w.held());    // 500은 여분 1칸에 남음
    }
}
