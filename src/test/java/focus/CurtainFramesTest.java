package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CurtainFramesTest {

    @TempDir
    Path dir;

    @Test
    void 닫힌정도를_장번호로바꿈() {
        assertEquals(0, CurtainFrames.indexFor(0, 61));
        assertEquals(30, CurtainFrames.indexFor(0.5, 61));
        assertEquals(60, CurtainFrames.indexFor(1, 61));
        assertEquals(0, CurtainFrames.indexFor(-0.3, 61));
        assertEquals(60, CurtainFrames.indexFor(1.7, 61));
    }

    @Test
    void curtain번호PNG만_번호순으로셈() throws IOException {
        for (String name : new String[] {"curtain_02.png", "curtain_00.png", "curtain_01.png", "readme.txt", "other.png"})
            Files.writeString(dir.resolve(name), "x");
        CurtainFrames frames = CurtainFrames.fromFolder(dir);
        assertEquals(3, frames.count());
        assertEquals(dir.resolve("curtain_02.png"), frames.path(2));
    }

    @Test
    void 그림없으면_사진커튼() {
        assertNull(CurtainFrames.fromFolder(dir.resolve("none")));
        assertNull(CurtainFrames.fromFolder(dir));      // 빈 폴더
    }

    // Blender 렌더 (닫힘 curtain_000~899, 막 오름 open_00~89)

    @Test
    void 접두사주면_그이름PNG만셈() throws IOException {
        for (String name : new String[] {"open_01.png", "open_00.png", "curtain_00.png"})
            Files.writeString(dir.resolve(name), "x");
        CurtainFrames open = CurtainFrames.fromFolder(dir, "open_");
        assertEquals(2, open.count());
        assertEquals(dir.resolve("open_01.png"), open.path(1));
    }

    @Test
    void 다닫혀머물면_마지막앞장() {
        // 899장은 가운데 밑단이 반투명하게 벌어져 있어서 다 닫힌 채로는 898장을 보인다
        assertEquals(898, CurtainFrames.closedIndex(1.0, 900));
        assertEquals(898, CurtainFrames.closedIndex(1.4, 900));
        assertEquals(450, CurtainFrames.closedIndex(0.5, 900));      // 닫히는 중에는 indexFor와 같다
        assertEquals(0, CurtainFrames.closedIndex(0, 900));
    }

    @Test
    void 막오름은_흐른시간으로고름() {
        assertEquals(0, CurtainFrames.openIndexAt(0, 3.0, 90));
        assertEquals(0, CurtainFrames.openIndexAt(-1, 3.0, 90));
        assertEquals(1, CurtainFrames.openIndexAt(1 / 30.0 + 1e-9, 3.0, 90));
        assertEquals(45, CurtainFrames.openIndexAt(1.5, 3.0, 90));
        assertEquals(89, CurtainFrames.openIndexAt(2.999, 3.0, 90));
        assertEquals(89, CurtainFrames.openIndexAt(9, 3.0, 90));    // 끝난 뒤에도 마지막 장
    }

    private Path frames(String side, String prefix, int count) throws IOException {
        Path d = Files.createDirectories(dir.resolve(side));
        for (int i = 0; i < count; i++)
            Files.writeString(d.resolve(String.format("%s%03d.png", prefix, i)), "x");
        return d;
    }

    @Test
    void 네폴더다있으면_Blender커튼() throws IOException {
        frames("left", "curtain_", 5);
        frames("right", "curtain_", 5);
        frames("open_left", "open_", 3);
        frames("open_right", "open_", 3);
        BlenderCurtain b = BlenderCurtain.load(dir);
        assertNotNull(b);
        assertEquals(5, b.left().count());
        assertEquals(3, b.openRight().count());
    }

    @Test
    void 폴더빠지거나장수다르면_사진커튼() throws IOException {
        assertNull(BlenderCurtain.load(dir.resolve("none")));
        frames("left", "curtain_", 5);
        frames("right", "curtain_", 5);
        frames("open_left", "open_", 3);
        assertNull(BlenderCurtain.load(dir));                       // open_right 없음
        frames("open_right", "open_", 2);
        assertNull(BlenderCurtain.load(dir));                       // 막 오름 왼쪽 3장, 오른쪽 2장
    }

    @Test
    void 다시열림은_30fps일정미리정함() {
        // 1.2초에 900장을 훑는 다시 열림은 펄스마다 목표가 바뀌어 늦게 뜬 장이 생겼다. 시작할 때 일정을 정해 미리 풀어 둔다
        int[] linear = CurtainFrames.reopenSchedule(1.0, 1.2, 30, 900, t -> t);
        assertEquals(37, linear.length);                 // 0초 ~ 1.2초, 1/30초마다
        assertEquals(898, linear[0]);                    // 다 닫힌 채 머문 장에서 시작
        assertEquals(450, linear[18]);                   // 0.6초: 반
        assertEquals(0, linear[36]);                     // 끝: 다 열림
        int[] eased = CurtainFrames.reopenSchedule(0.5, 1.2, 30, 900, t -> t * t);
        assertEquals(450, eased[0]);
        assertEquals(446, eased[3]);                     // 천천히 시작: 0.1초에 0.5 * (1 - 0.0833 * 0.0833), 446장
    }
}
