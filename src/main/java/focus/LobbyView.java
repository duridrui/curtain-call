package focus;

import java.util.List;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.layout.AnchorPane;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;

class LobbyView {
    private static final double BLOCK_WIDTH = 380;

    private final Boolean dndReady;
    private final ShowRecord last;
    private final int historyCount;
    private final Runnable onBook;
    private final Runnable onHistory;
    private final Runnable onSettings;
    private final Runnable onQuit;

    LobbyView(Boolean dndReady, ShowRecord last, int historyCount, Runnable onBook, Runnable onHistory,
            Runnable onSettings, Runnable onQuit) {
        this.dndReady = dndReady;
        this.last = last;
        this.historyCount = historyCount;
        this.onBook = onBook;
        this.onHistory = onHistory;
        this.onSettings = onSettings;
        this.onQuit = onQuit;
    }
    // 로비의 상태 안내와 예매,기록,설정 메뉴를 한 화면에 배치
    Parent build() {
        AnchorPane root = new AnchorPane();
        root.getStyleClass().add("screen-dim");

        Label desc = Ui.label("딴짓하면 막이 내리고, 돌아오면 막이 오릅니다.", "t-body");
        VBox head = new VBox(0, Ui.label("오늘의 공연", "t-greeting"), Ui.label("커튼콜", "t-display"), desc);
        VBox.setMargin(desc, new Insets(14, 0, 0, 0));
        Ui.anchor(head, Ui.EDGE, null, null, Ui.EDGE);

        HBox chips = new HBox(8, statusChip("쉬는 중 · 입장해야 딴짓을 살펴요"), statusChip(dndText()));
        Ui.anchor(chips, Ui.EDGE + 4, Ui.EDGE, null, null);

        Button book = Ui.primary("공연 예매", BLOCK_WIDTH);
        book.setOnAction(e -> onBook.run());

        VBox menu = new VBox(
            row("관람 기록", historyCount + "편", onHistory, true),
            row("기본 허용 목록", "›", onSettings, false),
            row("종료", "", onQuit, false));
        menu.getStyleClass().add("panel");
        menu.setPrefWidth(BLOCK_WIDTH);
        Label lastLine = Ui.label(lastText(), "t-caption");
        VBox block = new VBox(0, lastLine, book, menu);
        VBox.setMargin(book, new Insets(14, 0, 12, 0));
        block.setPrefWidth(BLOCK_WIDTH);
        Ui.anchor(block, null, null, Ui.BOTTOM, Ui.EDGE);

        Label hint = Ui.label(hintText(), "t-caption");
        hint.setStyle("-fx-text-alignment : right;");
        Ui.anchor(hint, null, Ui.EDGE, Ui.BOTTOM, null);

        root.getChildren().addAll(head, chips, block, hint);
        Motion.itemsIn(List.of(head, chips, block, hint));
        return root;
    }

    private static Label statusChip(String text) {
        Label chip = Ui.label(text, "chip");
        chip.setAlignment(Pos.CENTER);
        return chip;
    }

    private static Button row(String name, String value, Runnable action, boolean first) {
        HBox content = new HBox(Ui.label(name, "t-row"), Ui.grow(), Ui.label(value, "t-caption"));
        content.setAlignment(Pos.CENTER_LEFT);
        Button b = new Button();
        b.setGraphic(content);
        content.prefWidthProperty().bind(b.widthProperty().subtract(40));
        b.getStyleClass().add("list-row");
        if (first)
            b.getStyleClass().add("first");
        b.setMaxWidth(Double.MAX_VALUE);
        b.setOnAction(e -> action.run());
        return b;
    }

    private String dndText() {
        if (dndReady == null)
            return "방해금지 연동 확인 중";
        return dndReady ? "공연 중 방해금지 자동으로 켜짐" : "방해금지 연동 안 됨 · 아래 안내";
    }

    private String hintText() {
        if (Boolean.FALSE.equals(dndReady))
            return "방해금지 연동: 단축어 앱에서 '" + DoNotDisturb.ON_NAME + "'(집중 모드 켜기)와\n'"
                + DoNotDisturb.OFF_NAME + "'(집중 모드 끄기) 단축어를 만들면\n공연 중에만 방해금지가 켜집니다.";
        return "앱을 켜도 딴짓 감지는 시작하지 않습니다.\n예매하고 입장하면 공연이 시작됩니다.";
    }

    private String lastText() {
        if (last == null)
            return "아직 관람 기록이 없습니다.";
        return "지난 공연 " + formatWhen(last.getId()) + " · " + lengthText(last.getPlannedMinutes())
            + " · 집중 " + focusPercent(last.getShowMillis(), last.getDistractionMillis()) + "%";
    }

    private static String lengthText(int minutes) {
        if (minutes <= 0)
            return "무제한";
        int h = minutes / 60, m = minutes % 60;
        if (h == 0)
            return m + "분";
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }
    // 전체 공연 시간에서 딴짓한 시간을 빼 집중 비율을 구함
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
}