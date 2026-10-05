package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class LongPressTest {

    @Test
    void 누른시간을_600ms기준진행도로() {
        LongPress p = new LongPress();
        assertEquals(0.0, p.progress(1000));               // 누르기 전
        p.press(1000);
        assertEquals(0.0, p.progress(1000));
        assertEquals(0.45, p.progress(1270), 0.001);
        assertEquals(1.0, p.progress(5000));
    }

    @Test
    void 길게600ms채우면_한번만종료() {
        LongPress p = new LongPress();
        p.press(0);
        assertFalse(p.poll(599));
        assertTrue(p.poll(600));
        assertFalse(p.poll(700));                          // 이미 실행함
    }

    @Test
    void 일찍떼면_종료안하고처음부터() {
        LongPress p = new LongPress();
        p.press(0);
        p.release();
        assertFalse(p.poll(1000));
        assertEquals(0.0, p.progress(1000));
        p.press(2000);
        assertFalse(p.poll(2500));
        assertTrue(p.poll(2600));
    }

    @Test
    void 시계거꾸로가도_음수진행도없음() {
        LongPress p = new LongPress();
        p.press(1000);
        assertEquals(0.0, p.progress(900));
    }
}
