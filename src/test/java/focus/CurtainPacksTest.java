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

    private CurtainPacks packs(PackManifest manifest, String baseUrl) {
        return new CurtainPacks(tmp.resolve("home"), baseUrl, tmp.resolve("bundled-low"), manifest);
    }

    private String dirUrl() {
        return tmp.toUri().toString();
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
    void 처음엔_저화질이고_고른_화질은_저장된다() {
        CurtainPacks p = packs(PackManifest.parse("{\"packs\": []}"), dirUrl());
        assertEquals(CurtainQuality.LOW, p.chosen());
        p.choose(CurtainQuality.HIGH);
        assertEquals(CurtainQuality.HIGH, packs(PackManifest.parse("{\"packs\": []}"), dirUrl()).chosen());
    }

    @Test
    void 받기에_성공하면_설치하고_그_화질을_고른다() throws Exception {
        Path zip = packZip("curtain-medium.zip", 4, 2);
        CurtainPacks p = packs(manifest(zip, "medium", 4, 2, sha(zip)), dirUrl());
        List<Long> progress = new ArrayList<>();
        CurtainPacks.Result r = p.download(CurtainQuality.MEDIUM, progress::add);
        assertTrue(r.ok(), r.message());
        assertTrue(p.installed(CurtainQuality.MEDIUM));
        assertEquals(CurtainQuality.MEDIUM, p.chosen());
        CurtainPacks.Active a = p.active();
        assertEquals(CurtainQuality.MEDIUM, a.quality());
        assertEquals(tmp.resolve("home/curtain/medium"), a.root());
        assertEquals(Files.size(zip), progress.get(progress.size() - 1));
        assertFalse(Files.exists(tmp.resolve("home/curtain/medium.zip.part")));
    }

    @Test
    void sha256이_다르면_설치하지_않고_저화질_그대로() throws Exception {
        Path zip = packZip("curtain-medium.zip", 4, 2);
        CurtainPacks p = packs(manifest(zip, "medium", 4, 2, "0".repeat(64)), dirUrl());
        CurtainPacks.Result r = p.download(CurtainQuality.MEDIUM, b -> { });
        assertFalse(r.ok());
        assertTrue(r.message().contains("확인"), r.message());
        assertFalse(p.installed(CurtainQuality.MEDIUM));
        assertEquals(CurtainQuality.LOW, p.chosen());
        assertEquals(CurtainQuality.LOW, p.active().quality());
        try (var left = Files.list(tmp.resolve("home/curtain"))) {
            assertEquals(List.of(), left.toList());
        }
    }

    @Test
    void 받다가_끊기면_설치하지_않는다() throws Exception {
        Path zip = packZip("curtain-medium.zip", 4, 2);
        byte[] all = Files.readAllBytes(zip);
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", ex -> {
            ex.sendResponseHeaders(200, all.length);
            try (OutputStream out = ex.getResponseBody()) {
                out.write(all, 0, all.length / 2);
                out.flush();
            } catch (IOException ignored) {
            }
            ex.close();
        });
        server.start();
        try {
            CurtainPacks p = packs(manifest(zip, "medium", 4, 2, sha(zip)),
                "http://127.0.0.1:" + server.getAddress().getPort() + "/");
            CurtainPacks.Result r = p.download(CurtainQuality.MEDIUM, b -> { });
            assertFalse(r.ok());
            assertFalse(p.installed(CurtainQuality.MEDIUM));
            assertEquals(CurtainQuality.LOW, p.active().quality());
        } finally {
            server.stop(0);
        }
    }

    @Test
    void 오프라인이면_실패를_알리고_저화질로_동작한다() throws Exception {
        Path zip = packZip("curtain-medium.zip", 4, 2);
        CurtainPacks p = packs(manifest(zip, "medium", 4, 2, sha(zip)), "http://127.0.0.1:9/");
        CurtainPacks.Result r = p.download(CurtainQuality.MEDIUM, b -> { });
        assertFalse(r.ok());
        assertTrue(r.message().contains("연결"), r.message());
        assertEquals(CurtainQuality.LOW, p.active().quality());
    }

    @Test
    void 설치된_폴더가_손상되면_저화질로_대체한다() throws Exception {
        Path zip = packZip("curtain-medium.zip", 4, 2);
        CurtainPacks p = packs(manifest(zip, "medium", 4, 2, sha(zip)), dirUrl());
        assertTrue(p.download(CurtainQuality.MEDIUM, b -> { }).ok());
        Files.delete(tmp.resolve("home/curtain/medium/left/curtain_002.png"));
        CurtainPacks.Active a = p.active();
        assertEquals(CurtainQuality.LOW, a.quality());
        assertEquals(tmp.resolve("bundled-low"), a.root());
        assertTrue(a.note().contains("손상"), a.note());
    }

    @Test
    void 설치된_파일이_잘려도_손상으로_본다() throws Exception {
        Path zip = packZip("curtain-medium.zip", 4, 2);
        CurtainPacks p = packs(manifest(zip, "medium", 4, 2, sha(zip)), dirUrl());
        assertTrue(p.download(CurtainQuality.MEDIUM, b -> { }).ok());
        Files.write(tmp.resolve("home/curtain/medium/right/curtain_001.png"), new byte[] {1});
        assertEquals(CurtainQuality.LOW, p.active().quality());
    }

    @Test
    void zip_경로_탈출은_막는다() throws Exception {
        Path zip = packZip("evil.zip", 1, 1, "../../escaped.png");
        Path dest = tmp.resolve("dest/inner");
        IOException e = assertThrows(IOException.class, () -> SafeUnzip.extract(zip, dest));
        assertTrue(e.getMessage().contains("경로"), e.getMessage());
        assertFalse(Files.exists(tmp.resolve("escaped.png")));
    }

    @Test
    void 경로_탈출_팩은_받아도_설치하지_않는다() throws Exception {
        Path zip = packZip("curtain-medium.zip", 4, 2, "../outside.png");
        CurtainPacks p = packs(manifest(zip, "medium", 4, 2, sha(zip)), dirUrl());
        assertFalse(p.download(CurtainQuality.MEDIUM, b -> { }).ok());
        assertFalse(Files.exists(tmp.resolve("home/curtain/outside.png")));
        assertFalse(p.installed(CurtainQuality.MEDIUM));
    }

    @Test
    void 저화질은_받지_않고_앱에_든_것을_쓴다() {
        CurtainPacks p = packs(PackManifest.parse("{\"packs\": []}"), dirUrl());
        p.choose(CurtainQuality.LOW);
        assertTrue(p.download(CurtainQuality.LOW, b -> { }).ok());
        assertEquals(tmp.resolve("bundled-low"), p.active().root());
    }

    @Test
    void 고른_화질이_설치_안_됐으면_저화질로_동작한다() {
        CurtainPacks p = packs(PackManifest.parse("{\"packs\": []}"), dirUrl());
        p.choose(CurtainQuality.HIGH);
        assertEquals(CurtainQuality.LOW, p.active().quality());
    }
}
