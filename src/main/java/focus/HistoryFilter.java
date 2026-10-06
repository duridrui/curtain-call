package focus;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

enum HistoryFilter {
    ALL("전체", 0),
    WEEK("주", 7),
    MONTH("월", 30);

    private final String label;
    private final int days;

    HistoryFilter(String label, int days) {
        this.label = label;
        this.days = days;
    }

    String getLabel() {
        return label;
    }

    List<ShowRecord> apply(List<ShowRecord> records, LocalDateTime now) {
        if (days == 0)
            return records;
        LocalDateTime from = now.minusDays(days);
        return records.stream().filter(r -> {
            LocalDateTime start = startOf(r);
            return start != null && !start.isBefore(from);
        }).toList();
    }

    private static final DateTimeFormatter RECORD_ID = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private static LocalDateTime startOf(ShowRecord r) {
        String id = r.getId();
        if (id == null || !id.matches("\\d{8}-\\d{6}"))
            return null;
        return LocalDateTime.parse(id, RECORD_ID);
    }
}
