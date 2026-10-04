package focus;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FrameBytesTest {
    @TempDir
    Path dir;

    private List<Path> files(int count) throws IOException {
        Path[] out = new Path[count];
        for (int i = 0; i < count; i++)
            out[i] = Files.write(dir.resolve("curtain_" + i + ".png"), new byte[] {(byte) i, 1, 2});
        return List.of(out);
    }

    @Test
    void 다_올리기_전에도_파일에서_바로_읽어_준다() throws IOException {
        FrameBytes bytes = new FrameBytes(files(3));
        assertArrayEquals(new byte[] {2, 1, 2}, bytes.get(2));
        assertEquals(0, bytes.loadedCount());
    }

    @Test
    void 다_올리면_장_수와_전체_바이트를_센다() throws IOException {
        FrameBytes bytes = new FrameBytes(files(4));
        bytes.loadAll();
        assertEquals(4, bytes.loadedCount());
        assertEquals(12, bytes.loadedBytes());
        assertArrayEquals(new byte[] {3, 1, 2}, bytes.get(3));
    }

    @Test
    void 없는_파일은_빈_바이트로_돌려준다() throws IOException {
        FrameBytes bytes = new FrameBytes(List.of(dir.resolve("none.png")));
        bytes.loadAll();
        assertTrue(bytes.get(0).length == 0);
    }

    @Test
    void 올린_장도_올리기_전_장도_스트림으로_읽는다() throws IOException {
        FrameBytes bytes = new FrameBytes(files(2));
        assertArrayEquals(new byte[] {1, 1, 2}, bytes.stream(1).readAllBytes());
        bytes.loadAll();
        assertArrayEquals(new byte[] {1, 1, 2}, bytes.stream(1).readAllBytes());
        assertArrayEquals(new byte[] {1, 1, 2}, bytes.stream(1).readAllBytes());
    }
}