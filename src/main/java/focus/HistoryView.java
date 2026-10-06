package focus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

class HistoryView {
    private static final double LIST_WIDTH = 450;
    private static final double CARD_HEIGHT = 142;
    private static final double LIST_TOP = 166;

    private final List<ShowRecord> records;
    private final Runnable onBack;
    private final VBox cards = new VBox(14);
    private final VBox detail = new VBox(0);
    private HistoryFilter filter = HistoryFilter.ALL;
    private Label sub;

    HistoryView(List<ShowRecord> records, Runnable onBack) {
        this.records = records;
        this.onBack = onBack;
    }

    Parent build() {
        AnchorPane root = new AnchorPane();
        root.getStyleClass().add("screen-dim");
        Button back = Ui.anchor(Ui.back(onBack), Ui.EDGE, null, null, Ui.EDGE);
        Label title = Ui.anchor(Ui.label("관람 기록", "t-title"), Ui.EDGE + 2, null, null, Ui.EDGE + 64);
        sub = Ui.anchor(Ui.label("아직 관람 기록이 없습니다. 첫 공연을 예매해 보세요.", "t-caption"), Ui.EDGE + 58, null, null, Ui.EDGE);

        HBox chips = new HBox(8);
        for (HistoryFilter f : HistoryFilter.values()) {
            Button chip = Ui.chip(f.getLabel());
            if (f == filter)
                chip.getStyleClass().add("selected");
            chip.setOnAction(e -> {
                filter = f;
                chips.getChildren().forEach(c -> c.getStyleClass().remove("selected"));
                chip.getStyleClass().add("selected");
                fillList();
            });
            chips.getChildren().add(chip);
        }
        Ui.anchor(chips, Ui.EDGE + 6, Ui.EDGE, null, null);

        ScrollPane scroll = new ScrollPane(cards);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setPrefWidth(LIST_WIDTH + 12);
        cards.setPadding(new Insets(0, 12, 0, 0));
        Ui.anchor(scroll, LIST_TOP, null, Ui.BOTTOM, Ui.EDGE);

        detail.getStyleClass().add("detail-panel");
        VBox detailBox = new VBox(detail);
        Ui.anchor(detailBox, LIST_TOP, Ui.EDGE, null, Ui.EDGE + LIST_WIDTH + 40);

        Button lobby = Ui.ghost("로비로", 120);
        lobby.setOnAction(e -> onBack.run());
        Ui.anchor(lobby, null, Ui.EDGE, Ui.BOTTOM, null);

        root.getChildren().addAll(scroll, detailBox, back, title, sub, chips, lobby);
        fillList();
        return root;
    }

    private void fillList() {
        List<ShowRecord> shown = filter.apply(records, LocalDateTime.now());
        if (!records.isEmpty())
            sub.setText(shown.isEmpty() ? "공연 0편" : "공연 " + shown.size() + "편 · " + topicSummary(shown));
        cards.getChildren().clear();
        List<VBox> made = new ArrayList<>();
        for (ShowRecord r : shown) {
            VBox card = card(r);
            card.setOnMouseClicked(e -> select(card, r));
            made.add(card);
        }
        cards.getChildren().addAll(made);
        if (made.isEmpty()) {
            detail.getChildren().setAll(Ui.label(records.isEmpty() ? "공연이 끝나면 여기에 결과가 쌓입니다."
                : "이 기간에는 관람 기록이 없습니다.", "t-body"));
            return;
        }
        select(made.get(0), shown.get(0));
        Motion.itemsIn(made);
    }

    private void select(VBox card, ShowRecord r) {
        cards.getChildren().forEach(c -> c.getStyleClass().remove("selected"));
        card.getStyleClass().add("selected");
        int i = records.indexOf(r);
        fillDetail(r, i >= 0 && i + 1 < records.size() ? records.get(i + 1) : null);
    }

    private static VBox card(ShowRecord r) {
        HBox top = new HBox(8, Ui.label(dateOf(r), "t-caption"), Ui.label(r.getTopic(), "topic-tag"), Ui.grow(),
            CurtainCallView.badge(r));
        top.setAlignment(Pos.CENTER_LEFT);
        Region line = new Region();
        line.getStyleClass().add("time-line");
        HBox.setHgrow(line, Priority.ALWAYS);
        HBox times = new HBox(10, Ui.label(startTime(r), "t-mono-big"), dot(), line, dot(),
            Ui.label(endTime(r), "t-mono-big"));
        times.setAlignment(Pos.CENTER);
        int percent = focusPercent(r.getShowMillis(), r.getDistractionMillis());
        HBox facts = new HBox(Ui.field("공연 길이", lengthText(r.getPlannedMinutes()), "t-label", "t-mono", false),
            Ui.grow(), Ui.field("집중", percent + "%", "t-label", "t-mono", false), Ui.grow(),
            Ui.field("막이 내린 횟수", r.getSwitchCount() + "번", "t-label", "t-mono", true));
        VBox card = new VBox(8, top, times, facts);
        card.getStyleClass().add("record-card");
        card.setMinHeight(CARD_HEIGHT);
        card.setPrefHeight(CARD_HEIGHT);
        card.setMaxHeight(CARD_HEIGHT);
        card.setClip(Ui.notchedCard(LIST_WIDTH, CARD_HEIGHT, 20, 9));
        return card;
    }

    private static Circle dot() {
        return new Circle(3, Color.web("#C9A227"));
    }

    private static String dateOf(ShowRecord r) {
        String when = formatWhen(r.getId());
        int space = when == null ? -1 : when.lastIndexOf(' ');
        return space > 0 ? when.substring(0, space) : String.valueOf(when);
    }

    private void fillDetail(ShowRecord r, ShowRecord previous) {
        GridPane grid = new GridPane();
        grid.setHgap(24);
        grid.setVgap(16);
        int percent = focusPercent(r.getShowMillis(), r.getDistractionMillis());
        grid.add(Ui.field("공연 시간", formatDuration(r.getShowMillis()), "t-label", "t-mono", false), 0, 0);
        grid.add(Ui.field("집중 시간", formatDuration(r.getFocusMillis()) + " (" + percent + "%)", "t-label", "t-mono", false), 1, 0);
        grid.add(Ui.field("인터미션", formatDuration(r.getDistractionMillis()), "t-label", "t-mono", false), 2, 0);
        grid.add(Ui.field("가장 자주 간 무대", r.getMostVisited() == null ? "없음" : r.getMostVisited(), "t-label", "t-row", false), 0, 1);
        grid.add(Ui.field("좌석", r.getSeat(), "t-label", "t-mono", false), 1, 1);
        grid.add(Ui.field("집중 주제", r.getTopic(), "t-label", "t-row", false), 2, 1);

        VBox compare = new VBox(0);
        for (String[] row : compareRows(previous, r)) {
            Label value = Ui.label(row[1], "t-caption");
            value.setWrapText(true);
            HBox line = new HBox(16, Ui.label(row[0], "t-row"), Ui.grow(), value);
            line.getStyleClass().add("compare-row");
            compare.getChildren().add(line);
        }
        Region rule = Ui.rule();
        Label compareTitle = Ui.label("직전 공연과 비교", "t-label");
        String when = formatWhen(r.getId())
            + (r.getEnding() == ShowEnding.CURTAIN_DOWN ? " · 막이 다 닫혀 중도 종료" : "");
        detail.getChildren().setAll(Ui.label(when, "t-caption"),
            Ui.label(lengthText(r.getPlannedMinutes()) + " 공연", "t-title"), grid, rule, compareTitle, compare);
        VBox.setMargin(grid, new Insets(20, 0, 0, 0));
        VBox.setMargin(rule, new Insets(22, 0, 16, 0));
        VBox.setMargin(compareTitle, new Insets(0, 0, 4, 0));
        Motion.fadeTo(detail, 1, Motion.ITEM);
    }

    private static final java.time.format.DateTimeFormatter CLOCK = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
    private static final java.time.format.DateTimeFormatter DATE = java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final java.time.format.DateTimeFormatter RECORD_ID = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final String NO_TIME = "--:--";

    private static String lengthText(int minutes) {
        if (minutes <= 0)
            return "무제한";
        int h = minutes / 60, m = minutes % 60;
        if (h == 0)
            return m + "분";
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }

    private static List<String[]> compareRows(ShowRecord previous, ShowRecord current) {
        List<String[]> rows = new ArrayList<>();
        if (previous == null) {
            rows.add(new String[] {"비교", "첫 공연이라 비교할 기록이 없습니다."});
            return rows;
        }
        int before = focusPercent(previous.getShowMillis(), previous.getDistractionMillis());
        int now = focusPercent(current.getShowMillis(), current.getDistractionMillis());
        rows.add(new String[] {"집중 비율",
            now + "% (직전 " + before + "%" + change(now - before, "%p", "올랐어요", "내려갔어요") + ")"});
        rows.add(new String[] {"막이 내린 횟수", current.getSwitchCount() + "번 (직전"
            + countChange(current.getSwitchCount() - previous.getSwitchCount()) + ")"});
        rows.add(new String[] {"인터미션", formatDuration(current.getDistractionMillis())
            + " (직전 " + formatDuration(previous.getDistractionMillis()) + ")"});
        return rows;
    }

    private static String startTime(ShowRecord r) {
        LocalDateTime start = startOf(r);
        return start == null ? NO_TIME : start.format(CLOCK);
    }

    private static String endTime(ShowRecord r) {
        LocalDateTime start = startOf(r);
        return start == null ? NO_TIME : start.plusNanos(r.getShowMillis() * 1_000_000L).format(CLOCK);
    }

    private static String topicSummary(List<ShowRecord> records) {
        if (records.isEmpty())
            return "";
        java.util.Map<String, Long> focus = new java.util.LinkedHashMap<>();
        for (ShowRecord r : records)
            focus.merge(r.getTopic(), r.getFocusMillis(), Long::sum);
        List<java.util.Map.Entry<String, Long>> sorted = new ArrayList<>(focus.entrySet());
        sorted.sort(java.util.Comparator.comparing((java.util.Map.Entry<String, Long> e) -> e.getValue()).reversed()
            .thenComparing(java.util.Map.Entry::getKey));
        StringBuilder line = new StringBuilder("주제별 집중");
        for (int i = 0; i < Math.min(3, sorted.size()); i++)
            line.append(" · ").append(sorted.get(i).getKey()).append(' ').append(shortDuration(sorted.get(i).getValue()));
        if (sorted.size() > 3)
            line.append(" 외 ").append(sorted.size() - 3).append("개");
        return line.toString();
    }

    private static String shortDuration(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        if (hours > 0)
            return minutes > 0 ? hours + "시간 " + minutes + "분" : hours + "시간";
        if (minutes > 0)
            return minutes + "분";
        return seconds + "초";
    }

    private static String clock(LocalDateTime at) {
        return at.format(CLOCK);
    }

    private static String date(LocalDateTime at) {
        return at.format(DATE);
    }

    private static LocalDateTime startOf(ShowRecord r) {
        String id = r.getId();
        if (id == null || !id.matches("\\d{8}-\\d{6}"))
            return null;
        return LocalDateTime.parse(id, RECORD_ID);
    }

    private static String formatDuration(long millis) {
        long seconds = Math.max(0, millis) / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long rest = seconds % 60;
        if (hours > 0)
            return hours + "시간 " + minutes + "분";
        if (minutes > 0)
            return minutes + "분 " + rest + "초";
        return rest + "초";
    }

    private static int focusPercent(long showMillis, long distractionMillis) {
        if (showMillis <= 0)
            return 0;
        long focus = Math.max(0, showMillis - distractionMillis);
        return (int) Math.min(100, Math.round(focus * 100.0 / showMillis));
    }

    private static String formatWhen(String id) {
        if (id == null || !id.matches("\\d{8}-\\d{6}"))
            return id;
        int month = Integer.parseInt(id.substring(4, 6));
        int day = Integer.parseInt(id.substring(6, 8));
        return month + "월 " + day + "일 " + id.substring(9, 11) + ":" + id.substring(11, 13);
    }

    private static String change(int diff, String unit, String up, String down) {
        if (diff == 0)
            return "와 같아요";
        return "보다 " + Math.abs(diff) + unit + " " + (diff > 0 ? up : down);
    }

    private static String countChange(int diff) {
        if (diff == 0)
            return "과 같아요";
        return "보다 " + Math.abs(diff) + "번 " + (diff > 0 ? "늘었어요" : "줄었어요");
    }
}
