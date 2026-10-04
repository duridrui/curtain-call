package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CurtainPacksTest {

    @TempDir
    Path tmp;

    private Path packZip(String name, int closed, int open, String... extraEntries) throws IOException {
        Path zip = tmp.resolve(name);
        try (ZipOutputStream z = new ZipOutputStream(Files.newOutputStream(zip))) {
            for (String side : new String[] {"left", "right"})
                for (int i = 0; i < closed; i++)
                    put(z, String.format("%s/curtain_%03d.png", side, i));
            for (String side : new String[] {"open_left", "open_right"})
                for (int i = 0; i < open; i++)
                    put(z, String.format("%s/open_%02d.png", side, i));
            for (String e : extraEntries)
                put(z, e);
        }
        return zip;
    }

    private static void put(ZipOutputStream z, String name) throws IOException {
        z.putNextEntry(new ZipEntry(name));
        z.write(("png:" + name).getBytes());
        z.closeEntry();
    }

    private static String sha(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private PackManifest manifest(Path zip, String tier, int closed, int open, String sha) throws IOException {
        return PackManifest.parse("{\"version\": 1, \"packs\": [{\"tier\": \"" + tier + "\", \"file\": \"" + zip.getFileName()
            + "\", \"bytes\": " + Files.size(zip) + ", \"sha256\": \"" + sha + "\", \"width\": 480, \"height\": 540, \"closed\": "
            + closed + ", \"open\": " + open + ", \"files\": " + (2 * closed + 2 * open) + "}]}");
    }

    @Test
    void 화질_등급은_저_중_고이고_모르는_값은_저화질() {
        assertEquals(List.of("low", "medium", "high"), List.of(CurtainQuality.LOW.id, CurtainQuality.MEDIUM.id, CurtainQuality.HIGH.id));
        assertTrue(CurtainQuality.LOW.bundled);
        assertFalse(CurtainQuality.MEDIUM.bundled);
        assertEquals(CurtainQuality.LOW, CurtainQuality.fromId("ultra"));
        assertEquals(CurtainQuality.LOW, CurtainQuality.fromId(null));
        assertEquals(CurtainQuality.HIGH, CurtainQuality.fromId("high"));
    }

    @Test
    void manifest를_읽는다() {
        PackManifest m = PackManifest.parse("{\"version\": 1, \"packs\": [{\"tier\": \"medium\", \"file\": \"curtain-medium.zip\", "
            + "\"bytes\": 231553329, \"sha256\": \"ab12\", \"width\": 480, \"height\": 540, \"closed\": 900, \"open\": 90, \"files\": 1980}]}");
        PackManifest.Pack p = m.pack(CurtainQuality.MEDIUM);
        assertEquals("curtain-medium.zip", p.file());
        assertEquals(231553329L, p.bytes());
        assertEquals(900, p.closed());
        assertEquals(null, m.pack(CurtainQuality.HIGH));
    }

    @Test
    void zip_경로_탈출은_막는다() throws Exception {
        Path zip = packZip("evil.zip", 1, 1, "../../escaped.png");
        Path dest = tmp.resolve("dest/inner");
        IOException e = assertThrows(IOException.class, () -> SafeUnzip.extract(zip, dest));
        assertTrue(e.getMessage().contains("경로"), e.getMessage());
        assertFalse(Files.exists(tmp.resolve("escaped.png")));
    }
}
