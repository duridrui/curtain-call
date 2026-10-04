package focus;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class HistoryStore {
    private static final String SUFFIX = ".properties";
    private final Path dir;

    public HistoryStore(Path dir) {
        this.dir = dir;
    }

    public void save(ShowRecord record) throws IOException {
        if (!record.getId().matches("\\d{8}-\\d{6}"))
            throw new IllegalArgumentException("기록 이름 형식이 아님: " + record.getId());
        Properties p = new Properties();
        p.setProperty("title", record.getTitle());
        p.setProperty("plannedMinutes", Integer.toString(record.getPlannedMinutes()));
        p.setProperty("seat", record.getSeat());
        p.setProperty("showMillis", Long.toString(record.getShowMillis()));
        p.setProperty("distractionMillis", Long.toString(record.getDistractionMillis()));
        p.setProperty("switchCount", Integer.toString(record.getSwitchCount()));
        p.setProperty("topic", record.getTopic());
        p.setProperty("ending", record.getEnding().name());
        p.setProperty("acts", Integer.toString(record.getActs()));
        if (record.getMostVisited() != null)
            p.setProperty("mostVisited", record.getMostVisited());
        int i = 0;
        for (Map.Entry<String, Integer> e : record.getCounts().entrySet()) {
            p.setProperty("count." + i + ".name", e.getKey());
            p.setProperty("count." + i + ".value", Integer.toString(e.getValue()));
            i++;
        }
        Files.createDirectories(dir);
        Path tmp = dir.resolve(record.getId() + SUFFIX + ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            p.store(w, "Curtain Call 관람 기록");
        }
        Files.move(tmp, dir.resolve(record.getId() + SUFFIX), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    public List<ShowRecord> list() {
        List<ShowRecord> result = new ArrayList<>();
        if (!Files.isDirectory(dir))
            return result;
        try (DirectoryStream<Path> files = Files.newDirectoryStream(dir, "*" + SUFFIX)) {
            for (Path file : files) {
                ShowRecord record = read(file);
                if (record != null)
                    result.add(record);
            }
        } catch (IOException e) {
            System.err.println("관람 기록 읽기 실패: " + e.getMessage());
        }
        result.sort(Comparator.comparing(ShowRecord::getId).reversed());
        return result;
    }

    public ShowRecord previousOf(String id) {
        for (ShowRecord record : list()) {
            if (record.getId().compareTo(id) < 0)
                return record;
        }
        return null;
    }

    private static ShowRecord read(Path file) {
        String name = file.getFileName().toString();
        String id = name.substring(0, name.length() - SUFFIX.length());
        if (!id.matches("\\d{8}-\\d{6}"))
            return null;
        Properties p = new Properties();
        try (Reader r = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            p.load(r);
            Map<String, Integer> counts = new HashMap<>();
            for (int i = 0; p.containsKey("count." + i + ".name"); i++)
                counts.put(p.getProperty("count." + i + ".name"), Integer.parseInt(p.getProperty("count." + i + ".value")));
            return new ShowRecord(id,
                p.getProperty("title", "공연"),
                Integer.parseInt(p.getProperty("plannedMinutes", "0")),
                p.getProperty("seat", ""),
                Long.parseLong(p.getProperty("showMillis")),
                Long.parseLong(p.getProperty("distractionMillis")),
                Integer.parseInt(p.getProperty("switchCount")),
                p.getProperty("mostVisited"),
                counts,
                p.getProperty("topic"),
                ShowEnding.parse(p.getProperty("ending")),
                Integer.parseInt(p.getProperty("acts", "1")));
        } catch (IOException | RuntimeException e) {
            System.err.println("관람 기록 건너뜀 " + name + " : " + e.getMessage());
            return null;
        }
    }
}