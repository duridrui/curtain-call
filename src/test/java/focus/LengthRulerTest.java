package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

class LengthRulerTest {
    @Test
    void 일분미만은_1분() {
        assertEquals(1, LengthRuler.snap(0));
    }

    @Test
    void 하루초과는_24시간() {
        assertEquals(1440, LengthRuler.snap(5000));
    }

    @Test
    void 구간마다_단위다름() {
        assertEquals(60, LengthRuler.snap(61));
        assertEquals(195, LengthRuler.snap(200));
    }

    @Test
    void 눈금끝은_24시간() {
        assertEquals(1440, LengthRuler.minutesAt(1.0));
    }
}