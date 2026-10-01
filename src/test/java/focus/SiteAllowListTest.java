package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import org.junit.jupiter.api.Test;

class SiteAllowListTest {

    List<String> allowed = List.of("daegu.ac.kr", "kmooc.kr");   // 테스트에 쓸 허용 목록

    @Test
    void 하위주소도_허용() {
        assertTrue(SiteAllowList.isAllowedHost("daegu.ac.kr", allowed));
        assertTrue(SiteAllowList.isAllowedHost("lms.daegu.ac.kr", allowed));
    }

    @Test
    void 비슷한이름은_허용안함() {
        assertFalse(SiteAllowList.isAllowedHost("notdaegu.ac.kr", allowed));
    }

    @Test
    void 웹주소는_호스트만() {
        assertEquals("lms.kmooc.kr", SiteAllowList.hostOf("https://LMS.KMOOC.kr/course/1"));   // 소문자로 바뀜
    }

    @Test
    void 웹주소아니면_null() {
        assertNull(SiteAllowList.hostOf("chrome://newtab/"));      // 크롬 새 탭
        assertNull(SiteAllowList.hostOf("about:blank"));           // 빈 페이지
        assertNull(SiteAllowList.hostOf("이상한 주소 %%"));          // 잘못된 주소
    }

    @Test
    void 허용안된사이트는_딴짓() {
        assertTrue(SiteAllowList.isDistractingSite("www.youtube.com", allowed));
        assertFalse(SiteAllowList.isDistractingSite("lms.daegu.ac.kr", allowed));
        assertFalse(SiteAllowList.isDistractingSite(null, allowed));        // 주소를 모르면 딴짓 아님
    }

    @Test
    void 사이트이름은_www와m을뗌() {
        assertEquals("youtube.com", SiteAllowList.siteKey("www.youtube.com"));
        assertEquals("youtube.com", SiteAllowList.siteKey("m.youtube.com"));
    }

    @Test
    void 정리하면_소문자_중복제거() {
        assertEquals(List.of("kmooc.kr"), SiteAllowList.clean(List.of("  KMOOC.kr ", "", "kmooc.kr")));
    }
}