package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class HistoryStoreTest {

    @TempDir
    Path dir;

    ShowRecord record(String id, long distraction) {
        return new ShowRecord(id, "단막극", 25, "A열 1번", 1_500_000, distraction, 2,
            "youtube.com", Map.of("youtube.com", 2, "Discord", 1));
    }

    @Test
    void 저장한_기록을_그대로_읽는다() throws IOException {
        HistoryStore store = new HistoryStore(dir.resolve("history"));
        ShowRecord saved = record("20261001-190200", 300_000);
        store.save(saved);
        List<ShowRecord> all = store.list();
        assertEquals(1, all.size());
        ShowRecord read = all.get(0);
        assertEquals(saved.getId(), read.getId());
        assertEquals("A열 1번", read.getSeat());
        assertEquals(300_000, read.getDistractionMillis());
        assertEquals(Map.of("youtube.com", 2, "Discord", 1), read.getCounts());
    }

    @Test
    void 목록은_최근_공연이_먼저() throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save(record("20261001-190200", 1));
        store.save(record("20261003-090000", 2));
        store.save(record("20261002-120000", 3));
        List<String> ids = store.list().stream().map(ShowRecord::getId).toList();
        assertEquals(List.of("20261003-090000", "20261002-120000", "20261001-190200"), ids);
    }

    @Test
    void 직전_공연을_찾는다() throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save(record("20261001-190200", 1));
        store.save(record("20261002-120000", 2));
        assertEquals("20261001-190200", store.previousOf("20261002-120000").getId());
        assertNull(store.previousOf("20261001-190200"));
    }

    @Test
    void 폴더가_없으면_빈_목록() {
        assertTrue(new HistoryStore(dir.resolve("none")).list().isEmpty());
    }

    @Test
    void 망가진_파일은_건너뛴다() throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save(record("20261001-190200", 1));
        Files.writeString(dir.resolve("20261002-000000.properties"), "showMillis=abc\n");
        assertEquals(1, store.list().size());
    }

    @Test
    void 이름에_구분_기호가_있어도_곳별_횟수를_지킨다() throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save(new ShowRecord("20261001-190200", "연극", 50, "B열 2번", 10, 5, 1,
            "a=b;c", Map.of("a=b;c", 1)));
        assertEquals(Map.of("a=b;c", 1), store.list().get(0).getCounts());
    }

    @Test
    void 주제와_종료_이유를_저장하고_읽는다() throws IOException {
        HistoryStore store = new HistoryStore(dir);
        store.save(new ShowRecord("20261002-190200", "연극", 50, "B열 2번", 40_000, 30_000, 1,
            "youtube.com", Map.of("youtube.com", 1), "졸업 작품=발표", ShowEnding.CURTAIN_DOWN));
        ShowRecord read = store.list().get(0);
        assertEquals("졸업 작품=발표", read.getTopic());
        assertEquals(ShowEnding.CURTAIN_DOWN, read.getEnding());
    }

    @Test
    void 주제가_없는_예전_기록은_기타와_정상_종료로_읽는다() throws IOException {
        Files.writeString(dir.resolve("20260930-210000.properties"),
            "title=\\uB2E8\\uB9C9\\uADF9\nplannedMinutes=25\nseat=A\nshowMillis=1500000\ndistractionMillis=420000\nswitchCount=4\n");
        ShowRecord read = new HistoryStore(dir).list().get(0);
        assertEquals("단막극", read.getTitle());
        assertEquals("기타", read.getTopic());
        assertEquals(ShowEnding.COMPLETED, read.getEnding());
        assertEquals(1_500_000, read.getShowMillis());
    }

    @Test
    void 알_수_없는_종료_이유와_빈_주제도_읽는다() throws IOException {
        Files.writeString(dir.resolve("20260930-210000.properties"),
            "showMillis=10\ndistractionMillis=0\nswitchCount=0\nending=SOMETHING\ntopic=\n");
        ShowRecord read = new HistoryStore(dir).list().get(0);
        assertEquals(ShowEnding.COMPLETED, read.getEnding());
        assertEquals("기타", read.getTopic());
    }

    @Test
    void 예전_생성자로_만든_기록은_기타와_정상_종료() {
        ShowRecord r = record("20261001-190200", 1);
        assertEquals("기타", r.getTopic());
        assertEquals(ShowEnding.COMPLETED, r.getEnding());
    }

    @Test
    void 막_수와_무제한_공연을_저장하고_읽는다() throws IOException {
        HistoryStore store = new HistoryStore(dir);
        ShowRecord r = ShowRecord.mergeAct(
            new ShowRecord("20261002-190200", "무제한", 0, "B열 2번", 40_000, 30_000, 1, null, Map.of(), "코딩", ShowEnding.CURTAIN_DOWN),
            new ShowRecord("20261002-191000", "무제한", 0, "B열 2번", 60_000, 0, 0, null, Map.of(), "코딩", ShowEnding.COMPLETED));
        store.save(r);
        ShowRecord read = store.list().get(0);
        assertEquals(2, read.getActs());
        assertEquals(0, read.getPlannedMinutes());
        assertEquals(100_000, read.getShowMillis());
    }

    @Test
    void 막_수가_없는_예전_기록은_1막() throws IOException {
        Files.writeString(dir.resolve("20260930-210000.properties"), "showMillis=10\ndistractionMillis=0\nswitchCount=0\n");
        assertEquals(1, new HistoryStore(dir).list().get(0).getActs());
    }
}