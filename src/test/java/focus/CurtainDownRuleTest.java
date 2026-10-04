package focus;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class CurtainDownRuleTest {

    @Test
    void 다닫히면_한번만종료() {
        CurtainDownRule rule = new CurtainDownRule();
        assertFalse(rule.onTick(true, 29, true));
        assertTrue(rule.onTick(true, 30, true));
        assertFalse(rule.onTick(true, 31, true));
    }

    @Test
    void 닫히기전돌아오면_종료안함() {
        CurtainDownRule rule = new CurtainDownRule();
        assertFalse(rule.onTick(true, 29, true));
        assertFalse(rule.onTick(true, 0, false));       // 돌아오면 경과 초가 0으로
        assertFalse(rule.onTick(true, 29, true));
    }

    @Test
    void 공연끝나면_다시초기화() {
        CurtainDownRule rule = new CurtainDownRule();
        assertTrue(rule.onTick(true, 30, true));
        assertFalse(rule.onTick(false, 0, false));      // 공연이 끝난 사이
        assertTrue(rule.onTick(true, 30, true));
    }
}
