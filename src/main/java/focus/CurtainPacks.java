package focus;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.function.LongConsumer;
import java.util.stream.Stream;

final class CurtainPacks {
    static final String DEFAULT_URL = "https://github.com/duridrui/curtain-call/releases/download/curtain-packs-v1/";
    private static final String MARKER = ".pack";
    private static final String[] SIDES = {"left", "right", "open_left", "open_right"};

    record Result(boolean ok, String message) {
    }

    record Active(CurtainQuality quality, Path root, String note) {
    }

    private final Path home;
    private final String baseUrl;
    private final Path bundledLow;
    private final PackManifest manifest;

    CurtainPacks(Path home, String baseUrl, Path bundledLow, PackManifest manifest) {
        this.home = home;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        this.bundledLow = bundledLow;
        this.manifest = manifest;
    }

    static CurtainPacks forApp() {
        Path home = Path.of(System.getProperty("curtain.home", Path.of(System.getProperty("user.home"), ".curtain-call").toString()));
        String json = "{\"packs\": []}";
        try (InputStream in = CurtainPacks.class.getResourceAsStream("/curtain/manifest.json")) {
            if (in != null)
                json = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
        return new CurtainPacks(home, System.getProperty("curtain.packs.url", DEFAULT_URL), resourceDir("/curtain/low"),
            PackManifest.parse(json));
    }

    static Path resourceDir(String name) {
        try {
            URL url = CurtainPacks.class.getResource(name);
            if (url == null)
                return null;
            URI uri = url.toURI();
            if ("jar".equals(uri.getScheme())) {
                try {
                    FileSystems.getFileSystem(uri);
                } catch (FileSystemNotFoundException e) {
                    FileSystems.newFileSystem(uri, Map.of());
                }
            }
            return Path.of(uri);
        } catch (Exception e) {
            return null;
        }
    }

    PackManifest manifest() {
        return manifest;
    }

    private Path choiceFile() {
        return home.resolve("curtain-quality.txt");
    }

    private Path dir(CurtainQuality q) {
        return home.resolve("curtain").resolve(q.id);
    }

    CurtainQuality chosen() {
        try {
            return CurtainQuality.fromId(Files.readString(choiceFile()));
        } catch (IOException e) {
            return CurtainQuality.LOW;
        }
    }

    void choose(CurtainQuality q) {
        try {
            Files.createDirectories(home);
            Files.writeString(choiceFile(), q.id);
        } catch (IOException ignored) {
        }
    }

    boolean installed(CurtainQuality q) {
        return problem(q) == null;
    }

    private String problem(CurtainQuality q) {
        if (q.bundled)
            return bundledLow != null && Files.isDirectory(bundledLow) ? null : "앱에 든 저화질 그림이 없습니다";
        PackManifest.Pack p = manifest.pack(q);
        if (p == null)
            return "팩 목록에 " + q.label + "이 없습니다";
        Path d = dir(q);
        if (!Files.isDirectory(d))
            return q.label + " 팩이 설치되지 않았습니다";
        try {
            List<String> marker = Files.readAllLines(d.resolve(MARKER));
            if (marker.size() < 3 || !marker.get(0).equals(p.sha256()))
                return q.label + " 팩이 손상됐습니다 (설치 표시가 다름)";
            long[] counted = count(d);
            if (!valid(d, p) || counted[0] != Long.parseLong(marker.get(1)) || counted[1] != Long.parseLong(marker.get(2)))
                return q.label + " 팩이 손상됐습니다 (장 수·크기가 다름)";
            return null;
        } catch (IOException | NumberFormatException e) {
            return q.label + " 팩이 손상됐습니다 (" + e.getMessage() + ")";
        }
    }

    private static boolean valid(Path d, PackManifest.Pack p) {
        BlenderCurtain b = BlenderCurtain.load(d);
        return b != null && b.left().count() == p.closed() && b.openLeft().count() == p.open();
    }

    private static long[] count(Path d) throws IOException {
        long files = 0, bytes = 0;
        for (String side : SIDES) {
            Path s = d.resolve(side);
            if (!Files.isDirectory(s))
                continue;
            try (Stream<Path> list = Files.list(s)) {
                for (Path f : (Iterable<Path>) list::iterator) {
                    files++;
                    bytes += Files.size(f);
                }
            }
        }
        return new long[] {files, bytes};
    }

    Active active() {
        CurtainQuality q = chosen();
        if (q.bundled)
            return new Active(CurtainQuality.LOW, bundledLow, "");
        String problem = problem(q);
        if (problem == null)
            return new Active(q, dir(q), "");
        return new Active(CurtainQuality.LOW, bundledLow, problem + " — 저화질로 보입니다");
    }

    Result download(CurtainQuality q, LongConsumer progress) {
        if (q.bundled) {
            choose(q);
            return new Result(true, q.label + "은 앱에 들어 있어 바로 씁니다 — 다음 실행부터 적용");
        }
        PackManifest.Pack p = manifest.pack(q);
        if (p == null)
            return new Result(false, "팩 목록에 " + q.label + "이 없습니다");
        Path base = home.resolve("curtain");
        Path part = base.resolve(q.id + ".zip.part");
        Path unpacked = base.resolve(q.id + ".tmp");
        try {
            Files.createDirectories(base);
            String sha = fetch(new URL(baseUrl + p.file()), part, p.bytes(), progress);
            if (!sha.equals(p.sha256()))
                return new Result(false, "받은 파일 확인(sha256)이 맞지 않습니다 — 설치하지 않았습니다");
            deleteTree(unpacked);
            SafeUnzip.extract(part, unpacked);
            if (!valid(unpacked, p))
                return new Result(false, "팩 안의 장 수가 맞지 않습니다 — 설치하지 않았습니다");
            long[] counted = count(unpacked);
            Files.write(unpacked.resolve(MARKER), List.of(p.sha256(), Long.toString(counted[0]), Long.toString(counted[1])));
            Path target = dir(q);
            deleteTree(target);
            Files.move(unpacked, target, StandardCopyOption.ATOMIC_MOVE);
            choose(q);
            return new Result(true, q.label + "을 설치했습니다 — 다음 실행부터 적용");
        } catch (ConnectException | UnknownHostException | NoRouteToHostException | SocketTimeoutException e) {
            return new Result(false, "인터넷에 연결할 수 없습니다 — 저화질로 계속합니다 (" + e.getClass().getSimpleName() + ")");
        } catch (IOException e) {
            return new Result(false, "받기·설치에 실패했습니다: " + e.getMessage());
        } finally {
            try {
                Files.deleteIfExists(part);
                deleteTree(unpacked);
            } catch (IOException ignored) {
            }
        }
    }

    private static String fetch(URL url, Path part, long expected, LongConsumer progress) throws IOException {
        URLConnection c = url.openConnection();
        c.setConnectTimeout(10_000);
        c.setReadTimeout(30_000);
        if (c instanceof HttpURLConnection http && http.getResponseCode() != 200)
            throw new IOException("서버 응답 " + http.getResponseCode());
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new IOException(e);
        }
        long got = 0, reported = 0;
        try (InputStream in = c.getInputStream(); OutputStream out = Files.newOutputStream(part)) {
            byte[] buf = new byte[1 << 16];
            for (int n; (n = in.read(buf)) > 0; ) {
                out.write(buf, 0, n);
                digest.update(buf, 0, n);
                got += n;
                if (got - reported >= (1 << 20)) {
                    progress.accept(got);
                    reported = got;
                }
            }
        }
        progress.accept(got);
        if (got != expected)
            throw new IOException("내려받기가 중간에 끊겼습니다 (" + got + " / " + expected + " 바이트)");
        return HexFormat.of().formatHex(digest.digest());
    }

    private static void deleteTree(Path d) throws IOException {
        if (!Files.exists(d))
            return;
        try (Stream<Path> walk = Files.walk(d)) {
            for (Path p : (Iterable<Path>) walk.sorted(Comparator.reverseOrder())::iterator)
                Files.delete(p);
        }
    }
}