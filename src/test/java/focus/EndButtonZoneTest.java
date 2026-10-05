package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class EndButtonZoneTest {

    @Test
    void 왼쪽아래56에_52px영역만받음() {
        EndButtonZone z = EndButtonZone.of(0, 30, 1680, 1020);
        assertEquals(52, z.getWidth());
        assertEquals(52, z.getHeight());
        assertEquals(56 - 4, z.getX());
        assertEquals(30 + 1020 - 56 - 44 - 4, z.getY());
    }

    @Test
    void 보조모니터도_그화면기준() {
        EndButtonZone z = EndButtonZone.of(-1440, 0, 1440, 900);
        assertEquals(-1440 + 52, z.getX());
        assertEquals(900 - 104, z.getY());
    }

    @Test
    void 영역안만받고_밖은통과() {
        EndButtonZone z = EndButtonZone.of(0, 0, 1440, 900);
        assertTrue(z.contains(z.getX(), z.getY()));
        assertTrue(z.contains(z.getX() + 51.9, z.getY() + 51.9));
        assertFalse(z.contains(z.getX() + 52, z.getY()));
        assertFalse(z.contains(z.getX() - 0.1, z.getY() + 10));
        assertFalse(z.contains(720, 450));                 // 화면 가운데는 통과
    }

    @Test
    void 버튼중심은_영역가운데() {
        EndButtonZone z = EndButtonZone.of(0, 0, 1440, 900);
        assertEquals(56 + 22, z.getX() + z.getWidth() / 2);
        assertEquals(900 - 56 - 22, z.getY() + z.getHeight() / 2);
    }
}
