package focus;

import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.util.Duration;

// 커튼콜: 공연이 끝나면 보이는 관람 완료(또는 중도 종료) 티켓과 도장, 숫자 격자, 직전 공연 비교
class CurtainCallView {
    private static final double PAPER_WIDTH = 470;      // 왼쪽 종이 티켓
    private static final double PAPER_TOP = 200;        // 절취선 위 높이
    private static final double PAPER_STUB = 200;       // 절취선 아래 높이
    private static final double RIGHT_LEFT = 590;       // 오른쪽 열 시작 x

    private final ShowRecord record;
    private final ShowRecord previous;      // 직전 공연, 없으면 null
    private final String exitLabel;         // "로비로" 또는 "퇴장"
    private final Runnable onExit;
    private final Runnable onContinue;      // 공연 이어보기 (중도 종료이고 남은 시간이 있을 때만), 없으면 null

    CurtainCallView(ShowRecord record, ShowRecord previous, String exitLabel, Runnable onExit, Runnable onContinue) {
        this.onContinue = onContinue;
        this.record = record;
        this.previous = previous;
        this.exitLabel = exitLabel;
        this.onExit = onExit;
    }

    Parent build() {
        AnchorPane root = new AnchorPane();
        root.getStyleClass().add("screen-dim");

        VBox head = new VBox(2, Ui.label("공연 결과 · " + CurtainCallReport.formatWhen(record.getId()), "t-greeting"),
            Ui.label("커튼콜", "t-title"));
        Ui.anchor(head, Ui.EDGE, null, null, Ui.EDGE);

        StackPane stamp = stamp(record.getEnding());
        Pane paper = paperTicket(stamp);
        paper.setRotate(-2);
        Ui.anchor(paper, 160.0, null, null, Ui.EDGE);

        Label headline = Ui.label(headline(previous, record), "t-serif-head");
        Label paragraph = Ui.label(paragraph(CurtainCallReport.compare(previous, record)), "t-serif-body");
        paragraph.setWrapText(true);
        VBox right = new VBox(0, statGrid(), headline, paragraph, Ui.rule(), Ui.label("자주 간 무대 순위", "t-label"),
            rankList(record.getCounts()));
        VBox.setMargin(headline, new Insets(26, 0, 8, 0));
        VBox.setMargin(right.getChildren().get(3), new Insets(18, 0, 12, 0));
        Ui.anchor(right, 150.0, Ui.EDGE, null, RIGHT_LEFT);

        Button exit = onContinue == null ? Ui.primary(exitLabel, 240) : Ui.ghost(exitLabel, 160);
        exit.setOnAction(e -> onExit.run());
        HBox buttons = new HBox(12, exit);
        if (onContinue != null) {       // 막이 다 닫혀 끝났을 때: 주제, 허용 목록, 남은 시간 그대로 다음 막
            Button again = Ui.primary("공연 이어보기", 240);
            again.setOnAction(e -> onContinue.run());
            buttons.getChildren().add(again);
        }
        Ui.anchor(buttons, null, Ui.EDGE, Ui.BOTTOM, null);

        root.getChildren().addAll(head, paper, right, buttons);
        Motion.itemsIn(List.of(head, right, buttons));
        Motion.rise(paper, 24, Motion.ISSUE, Duration.ZERO).play();
        Motion.stamp(stamp, Motion.ISSUE).play();
        return root;
    }

    // 크림 종이 입장권: 공연 이름과 숫자, 절취선 아래에 가장 자주 간 무대, 막이 내린 횟수, 바코드, 도장
    private Pane paperTicket(StackPane stamp) {
        Region strip = new Region();
        strip.getStyleClass().add("paper-strip");
        strip.setMinHeight(10);
        HBox badgeRow = new HBox(Ui.grow(), badge(record));
        int percent = CurtainCallReport.focusPercent(record.getShowMillis(), record.getDistractionMillis());
        HBox numbers = new HBox(28,
            Ui.field("공연 시간", CurtainCallReport.formatDuration(record.getShowMillis()), "paper-label", "paper-value", false),
            Ui.field("집중 시간", CurtainCallReport.formatDuration(record.getFocusMillis()) + " (" + percent + "%)", "paper-label", "paper-value", false),
            Ui.field("좌석", record.getSeat(), "paper-label", "paper-value", false));
        VBox name = new VBox(0, Ui.label("공연 길이", "paper-label"), Ui.label(lengthText(record.getPlannedMinutes()), "paper-title"),
            Ui.label("집중 주제 · " + record.getTopic(), "paper-label"));
        VBox top = new VBox(0, badgeRow, name, numbers);
        VBox.setMargin(numbers, new Insets(18, 0, 0, 0));
        top.setPadding(new Insets(12, 30, 0, 36));
        VBox topSide = new VBox(strip, top);

        String visited = record.getMostVisited() == null ? "없음" : record.getMostVisited();
        VBox facts = new VBox(12, Ui.field("가장 자주 간 무대", visited, "paper-label", "paper-name", false),
            Ui.field("막이 내린 횟수", record.getSwitchCount() + "번", "paper-label", "paper-value", false));
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        VBox stubSide = new VBox(0, facts, gap, Ui.barcode(record.getId(), 240, 40, Ui.INK));
        stubSide.setPadding(new Insets(26, 36, 24, 36));

        StackPane topPiece = piece(topSide, PAPER_TOP);
        topPiece.setClip(Ui.ticketTop(PAPER_WIDTH, PAPER_TOP, 24, 12, false));
        StackPane stubPiece = piece(stubSide, PAPER_STUB);
        stubPiece.setClip(Ui.ticketStub(PAPER_WIDTH, PAPER_STUB, 24, 12, false));
        stubPiece.setLayoutY(PAPER_TOP);
        Line perforation = new Line(24, PAPER_TOP, PAPER_WIDTH - 24, PAPER_TOP);
        perforation.setStroke(Color.rgb(43, 30, 30, 0.18));
        perforation.setStrokeWidth(2);
        perforation.getStrokeDashArray().addAll(6.0, 4.0);
        stamp.setLayoutX(PAPER_WIDTH - 36 - 116);
        stamp.setLayoutY(PAPER_TOP + 30);

        Pane paper = new Pane(topPiece, stubPiece, perforation, stamp);
        paper.setPrefSize(PAPER_WIDTH, PAPER_TOP + PAPER_STUB);
        paper.setMaxSize(PAPER_WIDTH, PAPER_TOP + PAPER_STUB);
        paper.setEffect(new DropShadow(32, 0, 14, Color.rgb(0, 0, 0, 0.45)));
        return paper;
    }

    private static StackPane piece(Node content, double height) {
        StackPane p = new StackPane(content);
        p.getStyleClass().add("paper");
        p.setMinSize(PAPER_WIDTH, height);
        p.setPrefSize(PAPER_WIDTH, height);
        p.setMaxSize(PAPER_WIDTH, height);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        return p;
    }

    // 종료 이유 배지 (이어본 공연은 막 수도). 중도 종료는 빨간 글자로 정상 종료와 구분
    static Label badge(ShowRecord record) {
        Label badge = Ui.label(badgeText(record), "badge");
        if (record.getEnding() == ShowEnding.CURTAIN_DOWN)
            badge.getStyleClass().add("abort");
        return badge;
    }

    // 도장: 지름 116 빨간 원, 세리프 글자. 중도 종료는 테두리를 점선으로
    private static StackPane stamp(ShowEnding ending) {
        Circle ring = new Circle(56);
        ring.setFill(Color.TRANSPARENT);
        ring.setStroke(Color.web("#B20209"));
        ring.setStrokeWidth(3);
        if (ending == ShowEnding.CURTAIN_DOWN)
            ring.getStrokeDashArray().addAll(10.0, 7.0);
        Label text = Ui.label(ending.getStamp(), "stamp-text");
        StackPane stamp = new StackPane(ring, text);
        stamp.setPrefSize(116, 116);
        stamp.setMouseTransparent(true);
        return stamp;
    }

    // 2x2 숫자 격자: 공연 시간, 집중 비율, 막이 내린 횟수, 인터미션 (칸 사이 1px 선)
    private GridPane statGrid() {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("stat-grid");
        grid.setHgap(1);
        grid.setVgap(1);
        grid.setPadding(new Insets(1));
        int percent = CurtainCallReport.focusPercent(record.getShowMillis(), record.getDistractionMillis());
        grid.add(statCell("공연 시간", CurtainCallReport.formatDuration(record.getShowMillis()), "top-left"), 0, 0);
        grid.add(statCell("집중 비율", percent + "%", "top-right"), 1, 0);
        grid.add(statCell("막이 내린 횟수", record.getSwitchCount() + "번", "bottom-left"), 0, 1);
        grid.add(statCell("인터미션(딴짓)", CurtainCallReport.formatDuration(record.getDistractionMillis()), "bottom-right"), 1, 1);
        ColumnConstraints half = new ColumnConstraints();
        half.setPercentWidth(50);
        grid.getColumnConstraints().addAll(half, half);
        return grid;
    }

    // 숫자 칸: 라벨 아래 큰 세리프 숫자 + 작은 단위. 바깥 모서리만 둥글게
    private static VBox statCell(String key, String value, String corner) {
        String[] parts = splitNumber(value);
        Label number = Ui.label(parts[0].isEmpty() ? parts[1] : parts[0], "t-serif-num");
        Label unit = Ui.label(parts[0].isEmpty() ? "" : parts[1], "t-unit");
        HBox line = new HBox(6, number, unit);
        line.setAlignment(Pos.BASELINE_LEFT);
        VBox cell = new VBox(6, Ui.label(key, "t-label"), line);
        cell.getStyleClass().add("stat-cell");
        cell.setMaxWidth(Double.MAX_VALUE);
        String radius = switch (corner) {
            case "top-left" -> "23 0 0 0";
            case "top-right" -> "0 23 0 0";
            case "bottom-left" -> "0 0 0 23";
            default -> "0 0 23 0";
        };
        cell.setStyle("-fx-background-radius: " + radius + ";");
        return cell;
    }

    // 곳별 횟수 상위 2개 (행 높이 40)
    static VBox rankList(Map<String, Integer> counts) {
        VBox box = new VBox(0);
        List<Map.Entry<String, Integer>> top = CurtainCallReport.topEntries(counts, 2);
        if (top.isEmpty()) {
            HBox none = new HBox(Ui.label("오늘은 다른 무대에 한 번도 가지 않았습니다.", "t-body"));
            none.getStyleClass().add("rank-row");
            box.getChildren().add(none);
            return box;
        }
        int rank = 1;
        for (Map.Entry<String, Integer> e : top) {
            HBox row = new HBox(Ui.label(rank + ". " + e.getKey(), "t-body"), Ui.grow(), Ui.label(e.getValue() + "번", "t-body"));
            row.getStyleClass().add("rank-row");
            box.getChildren().add(row);
            rank++;
        }
        return box;
    }

    // 공연 길이(분)를 글로: "50분", "1시간 30분", "2시간", 0이면 "무제한"
    private static String lengthText(int minutes) {
        if (minutes <= 0)
            return "무제한";
        int h = minutes / 60, m = minutes % 60;
        if (h == 0)
            return m + "분";
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }

    // 기록 배지: "관람 완료" / "중도 종료", 이어본 공연은 막 수(2막)를 붙임
    private static String badgeText(ShowRecord r) {
        return r.getActs() > 1 ? r.getEnding().getBadge() + " · " + r.getActs() + "막" : r.getEnding().getBadge();
    }

    // "4분 5초"를 {"4", "분 5초"}로: 앞의 숫자는 크게, 나머지는 단위로 작게 쓰기 위해 나눔
    private static String[] splitNumber(String text) {
        int i = 0;
        while (i < text.length() && Character.isDigit(text.charAt(i)))
            i++;
        return new String[] {text.substring(0, i), text.substring(i).trim()};
    }

    // 커튼콜 소제목: 직전 공연과 집중 비율을 비교
    private static String headline(ShowRecord previous, ShowRecord current) {
        if (current.getEnding() == ShowEnding.CURTAIN_DOWN)
            return "막이 내려 공연이 끝났어요";
        if (previous == null)
            return "첫 공연을 마쳤어요";
        int before = CurtainCallReport.focusPercent(previous.getShowMillis(), previous.getDistractionMillis());
        int now = CurtainCallReport.focusPercent(current.getShowMillis(), current.getDistractionMillis());
        if (now > before)
            return "직전 공연보다 나아졌어요";
        if (now == before)
            return "직전 공연과 같아요";
        return "다음 공연에서 다시 올려 봐요";
    }

    // 비교 문장들을 마침표로 이어 한 문단으로
    private static String paragraph(List<String> lines) {
        return String.join(". ", lines) + ".";
    }
}
