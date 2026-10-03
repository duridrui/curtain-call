package focus;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.WritableValue;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.TextFormatter;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.transform.Rotate;
import javafx.util.Duration;

class BookingView {
    record Lists(AllowList apps, SiteAllowList sites, Path lastApps, Path lastSites) {
    }

    private static final int ROWS = 8;
    private static final int COLS = 12;
    private static final double HALL_WIDTH = 760;
    private static final double HALL_HEIGHT = 430;
    private static final double TICKET_WIDTH = 560;
    private static final double TOP_HEIGHT = 260;
    private static final double STUB_HEIGHT = 220;
    private static final double TICKET_RADIUS = 24;
    private static final double NOTCH = 12;
    private static final String ISSUED = "티켓이 나왔습니다";
    private static final String ENTERING = "입장합니다";

    private final Consumer<Ticket> onEnter;
    private final Runnable onBack;
    private final StackPane root = new StackPane();
    private final Lists lists;
    private final java.util.function.Supplier<List<String>> runningApps;
    private ShowLength chosen;
    private List<String> pickedApps;
    private List<String> pickedSites;
    private List<String> appChoices;
    private List<String> siteChoices;
    private List<String> running = List.of();
    private String topicChip;
    private String typedTopic = "";

    private StackPane topPiece;
    private StackPane stubPiece;
    private Rotate stubRotate;
    private BooleanProperty torn = new SimpleBooleanProperty(false);
    private Label ticketTitle;
    private List<Button> ticketButtons = List.of();
    private boolean tearing;

    BookingView(Consumer<Ticket> onEnter, Runnable onBack, Lists lists,
            java.util.function.Supplier<List<String>> runningApps) {
        this.lists = lists;
        this.runningApps = runningApps;
        this.onEnter = onEnter;
        this.onBack = onBack;
    }

    Parent build() {
        showLengths();
        return root;
    }
    // 이전 화면을 새 화면으로 바꾸고 전환 효과를 적용
    private AnchorPane screen(String background) {
        AnchorPane screen = new AnchorPane();
        screen.getStyleClass().add(background);
        if (!root.getChildren().isEmpty())
            Motion.screenIn(screen);
        root.getChildren().setAll(screen);
        return screen;
    }

    private void showLengths() {
        AnchorPane s = screen("screen-dim");
        Button back = Ui.anchor(Ui.back(onBack), Ui.EDGE, null, null, Ui.EDGE);
        Label title = Ui.anchor(Ui.label("얼마나 공연할까요?", "t-title-sm"), Ui.EDGE + 7, null, null, Ui.EDGE + 64);
        Label desc = Ui.anchor(Ui.label("빠른 선택을 누르거나 눈금자를 끌어 1분부터 24시간까지 정합니다.", "t-body"),
            Ui.EDGE + 60,  null, null, Ui.EDGE);
        Label readout = Ui.label("", "length-readout");
        LengthRuler ruler = new LengthRuler();
        Button next = Ui.primary("좌석 고르기", 520);
        next.setOnAction(e -> { });
        HBox cards = new HBox(14);
        cards.setAlignment(Pos.BOTTOM_CENTER);
        List<VBox> cells = new ArrayList<>();
        Runnable refresh = () -> {      // 선택한 공연 길이에 맞춰 표시 문구와 카드, 눈금자 상태를 갱신
            next.setDisable(chosen == null);
            readout.setText(chosen == null ? "길이를 고르세요" : LengthRuler.lengthText(chosen.getMinutes()));
            ruler.show(chosen);
            for (VBox cell : cells) {
                boolean on = cell.getUserData() != null && sameQuick(chosen, (ShowLength) cell.getUserData());
                cell.getChildren().get(0).setVisible(on);
                cell.getChildren().get(1).getStyleClass().remove("selected");
                if (on)
                    cell.getChildren().get(1).getStyleClass().add("selected");
            }
        };
        List<ShowLength> quick = new ArrayList<>();
        for (int m : ShowLength.QUICK_MINUTES)
            quick.add(ShowLength.minutes(m));
        quick.add(ShowLength.unlimited());
        for (ShowLength length : quick) {
            Pane light = Ui.spotlight(196, 150, 46);
            light.setVisible(false);
            VBox card = card(length);
            VBox cell = new VBox(-34, light, card);
            cell.setAlignment(Pos.BOTTOM_CENTER);
            cell.setUserData(length);
            card.setOnMouseClicked(e -> {
                chosen = length;
                refresh.run();
            });
            cells.add(cell);
            cards.getChildren().add(cell);
        }
        ruler.setOnPick(minutes -> {
            chosen = ShowLength.minutes(minutes);
            refresh.run();
        });
        refresh.run();

        VBox bottom = new VBox(0, readout, ruler, cards, next);
        VBox.setMargin(ruler, new Insets(6, 0, 0, 0));
        VBox.setMargin(cards, new Insets(0, 0, 18, 0));
        bottom.setAlignment(Pos.TOP_CENTER);
        Ui.anchor(bottom, null, Ui.EDGE, Ui.BOTTOM, Ui.EDGE);
        s.getChildren().addAll(back, title, desc, bottom);
        Motion.itemsIn(cards.getChildren());
    }
    private static boolean sameQuick(ShowLength a, ShowLength b) {
        if (a == null)
            return false;
        return a.isUnlimited() == b.isUnlimited() && a.getMillis() == b.getMillis();
    }
    private static VBox card(ShowLength length) {
        Label tag = Ui.label(length.isUnlimited() ? "∞" : length.getMinutes() + "", "tag");
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        String sub = length.isUnlimited() ? "직접 끝낼 때까지" : length.getMinutes() + "분";
        VBox card = new VBox(0, tag, gap, Ui.label(LengthRuler.lengthText(length.getMinutes()), "card-title"), Ui.label(sub, "card-sub"));
        card.getStyleClass().add("show-card");
        card.setMinSize(132, 112);
        card.setPrefSize(132, 112);
        card.setMaxSize(132, 112);
        return Motion.liftOnHover(card, 2);
    }
}
