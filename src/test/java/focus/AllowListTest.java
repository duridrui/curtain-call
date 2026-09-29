package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AllowListTest {

    @TempDir Path dir;

    @Test
    void 파일없으면_기본목록() {
        Path file = dir.resolve("없는 파일.txt");
        assertEquals(AllowList.DEFAULT, AllowList.load(file));
    }

    @Test
    void 공백_빈줄_중복_정리() {
        List<String> messy = List.of(" Code", "", "Code", "Safari");
        assertEquals(List.of("Code", "Safari"), AllowList.clean(messy));
    }

    @Test
    void 저장하고_다시읽으면_같다() throws Exception {
        Path file = dir.resolve("allowed-apps.txt");

        AllowList first = new AllowList(file);
        first.replace(List.of("Code", "Notion"));

        AllowList second = new AllowList(file);
        assertEquals(List.of("Code", "Notion"), second.get());
    }

    @Test
    void 넘겨준_목록을_나중에_고쳐도_영향없음() throws Exception {
        AllowList list = new AllowList(dir.resolve("allowed-apps.txt"));
        ArrayList<String> fromWindow = new ArrayList<>(List.of("Code"));
        list.replace(fromWindow);
        fromWindow.add("YouTube");
        assertEquals(List.of("Code"), list.get());
    }

    @Test
    void 기본목록이면_iTerm2는_딴짓아님() {
        assertEquals(false, Main.isDistracting("iTerm2", AllowList.DEFAULT));
    }
}