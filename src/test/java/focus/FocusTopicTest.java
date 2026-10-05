package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.List;
import org.junit.jupiter.api.Test;

class FocusTopicTest {

    @Test
    void 기본_주제는_다섯_개와_기타() {
        assertEquals(List.of("공부", "과제", "코딩", "독서", "작업"), FocusTopic.PRESETS);
        assertEquals("기타", FocusTopic.OTHER);
    }

    @Test
    void 직접_입력은_앞뒤_공백을_지우고_가운데_공백은_하나로() {
        assertEquals("졸업 작품 발표", FocusTopic.normalize("  졸업   작품 발표 "));
    }

    @Test
    void 줄바꿈과_제어_문자는_공백으로_바꾼다() {
        assertEquals("보고서 쓰기", FocusTopic.normalize("보고서\n쓰기\t"));
        assertEquals("a b", FocusTopic.normalize("a\u0000b"));
    }

    @Test
    void 비어_있으면_주제가_아니다() {
        assertNull(FocusTopic.normalize(null));
        assertNull(FocusTopic.normalize("   "));
        assertNull(FocusTopic.normalize("\n\t"));
    }

    @Test
    void 이십_자까지만_받는다() {
        String twenty = "가".repeat(20);
        assertEquals(twenty, FocusTopic.normalize(twenty));
        assertNull(FocusTopic.normalize("가".repeat(21)));
        assertEquals(twenty, FocusTopic.normalize("  " + twenty + "  "));
    }

    @Test
    void 글자_수는_이모지도_한_글자로_센다() {
        String emoji = "📚".repeat(20);
        assertEquals(20, FocusTopic.length(emoji));
        assertEquals(emoji, FocusTopic.normalize(emoji));
    }

    @Test
    void 기타를_고르고_입력을_비우면_기타() {
        assertEquals("기타", FocusTopic.choose(FocusTopic.OTHER, ""));
        assertEquals("기타", FocusTopic.choose(FocusTopic.OTHER, null));
        assertEquals("영어 단어", FocusTopic.choose(FocusTopic.OTHER, " 영어 단어 "));
    }

    @Test
    void 기본_주제를_고르면_입력란은_무시한다() {
        assertEquals("코딩", FocusTopic.choose("코딩", "아무 글"));
    }

    @Test
    void 고르지_않았거나_입력이_너무_길면_선택이_아니다() {
        assertNull(FocusTopic.choose(null, ""));
        assertNull(FocusTopic.choose(FocusTopic.OTHER, "가".repeat(21)));
        assertNull(FocusTopic.choose("목록에 없는 주제", ""));
    }

    @Test
    void 저장된_주제가_없으면_기타로_읽는다() {
        assertEquals("기타", FocusTopic.orOther(null));
        assertEquals("기타", FocusTopic.orOther("  "));
        assertEquals("독서", FocusTopic.orOther("독서"));
        assertEquals("기타", FocusTopic.orOther("가".repeat(30)));
    }
}
