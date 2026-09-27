package focus;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

class MainTest {
    List<String> allowed = List.of("Code", "Terminal");

    @Test
    void 허용앱은_딴짓아님() {
        assertFalse(Main.isDistracting("Code", allowed));
    }

    @Test
    void 비허용앱은_딴짓() {
        assertTrue(Main.isDistracting("Safari", allowed));
    }

    @Test
    void 모름은_딴짓아님() {
        assertFalse(Main.isDistracting(null, allowed));
        assertFalse(Main.isDistracting("(unknown)", allowed));
    }

    @Test
    void 자기자신은_딴짓아님() {
        assertFalse(Main.isDistracting("java", allowed));
    }
}