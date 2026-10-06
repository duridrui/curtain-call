package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class CurtainQualityPaneTest {

    @Test
    void 화질팩_크기를_MB_GB로_적는다() {
        assertEquals("232MB", CurtainQualityPane.megabytes(231_785_371L));
        assertEquals("1.2GB", CurtainQualityPane.megabytes(1_195_523_837L));
        assertEquals("0MB", CurtainQualityPane.megabytes(0));
        assertEquals("1MB", CurtainQualityPane.megabytes(600_000));
    }
}
