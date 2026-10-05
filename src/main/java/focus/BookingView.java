package focus;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter CLOCK = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("yyyy/MM/dd");

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
    private SeatMap seats;
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
        next.setOnAction(e -> showSeats());
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

    void showSeatsFor(ShowLength p) {
        chosen = p;
        showSeats();
    }

    private void showSeats() {
        seats = new SeatMap(ROWS, COLS, seed());
        AnchorPane s = screen("screen-dim");
        Button back = Ui.anchor(Ui.back(this::showLengths), Ui.EDGE, null, null, Ui.EDGE);

        HBox legend = new HBox(24, legendItem("빈 좌석", ""), legendItem("팔린 좌석", "taken"), legendItem("내 좌석", "selected"));
        legend.setAlignment(Pos.CENTER);

        Label pickedSeat = Ui.label("—", "t-title-sm");
        Label pickedShow = Ui.label("", "t-body");
        HBox pickedLine = new HBox(10, pickedSeat, pickedShow);
        pickedLine.setAlignment(Pos.BASELINE_LEFT);
        VBox picked = new VBox(2, Ui.label("고른 좌석", "t-label"), pickedLine);
        Button again = Ui.ghost("길이 다시 고르기", 190);
        again.setOnAction(e -> showLengths());
        Button issue = Ui.primary("다음: 허용 목록", 240);
        issue.setDisable(true);
        issue.setOnAction(e -> showAllowList());
        HBox bottom = new HBox(12, picked, Ui.grow(), again, issue);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setMaxWidth(HALL_WIDTH);

        Runnable refresh = () -> {
            String topic = topic();
            pickedShow.setText(LengthRuler.lengthText(chosen.getMinutes()) + (topic == null ? "" : " · " + topic));
            issue.setDisable(seats.getSelectedName() == null || topic == null);
        };
        HBox topics = topicRow(refresh);
        refresh.run();

        GridPane grid = seatGrid(pickedSeat, refresh);
        VBox inside = new VBox(24, stageBar(), grid);
        inside.setAlignment(Pos.TOP_CENTER);
        inside.setPadding(new Insets(20, 0, 0, 0));
        Region shape = new Region();
        shape.getStyleClass().add("hall");
        shape.setStyle("-fx-shape: \"M0,140 A380,140 0 0 1 380,0 A380,140 0 0 1 760,140 L760,390 Q760,430 720,430 L40,430 Q0,430 0,390 Z\";");
        StackPane hall = new StackPane(shape, inside);
        hall.setMinSize(HALL_WIDTH, HALL_HEIGHT);
        hall.setMaxSize(HALL_WIDTH, HALL_HEIGHT);
        StackPane.setAlignment(inside, Pos.TOP_CENTER);

        VBox column = new VBox(16, legend, hall, topics, bottom);
        VBox.setMargin(bottom, new Insets(14, 0, 0, 0));
        column.setAlignment(Pos.CENTER);
        column.setPadding(new Insets(Ui.BOTTOM, 0, Ui.BOTTOM, 0));
        Ui.anchor(column, 0.0, Ui.EDGE, 0.0, Ui.EDGE);
        s.getChildren().addAll(column, back);
        Motion.rise(hall, 8, Motion.ITEM, Duration.ZERO).play();
    }

    private String topic() {
        return FocusTopic.choose(topicChip, typedTopic);
    }

    private HBox topicRow(Runnable refresh) {
        TextField input = new TextField(typedTopic);
        input.getStyleClass().add("topic-input");
        input.setPromptText("직접 입력");
        input.setPrefWidth(150);
        input.setTextFormatter(new TextFormatter<String>(change ->
            FocusTopic.length(change.getControlNewText()) <= FocusTopic.MAX_LENGTH ? change : null));
        Label count = Ui.label("", "topic-count");
        Runnable showInput = () -> {
            boolean other = FocusTopic.OTHER.equals(topicChip);
            input.setVisible(other);
            input.setManaged(other);
            count.setVisible(other);
            count.setManaged(other);
            count.setText(FocusTopic.length(input.getText()) + "/" + FocusTopic.MAX_LENGTH);
        };
        input.textProperty().addListener((obs, was, now) -> {
            typedTopic = now;
            showInput.run();
            refresh.run();
        });

        HBox chips = new HBox(6);
        chips.setAlignment(Pos.CENTER_LEFT);
        List<String> names = new ArrayList<>(FocusTopic.PRESETS);
        names.add(FocusTopic.OTHER);
        for (String name : names) {
            Button chip = Ui.chip(name);
            chip.getStyleClass().add("topic-chip");
            if (name.equals(topicChip))
                chip.getStyleClass().add("selected");
            chip.setOnAction(e -> {
                topicChip = name;
                chips.getChildren().forEach(c -> c.getStyleClass().remove("selected"));
                chip.getStyleClass().add("selected");
                showInput.run();
                refresh.run();
                if (FocusTopic.OTHER.equals(name))
                    input.requestFocus();
            });
            chips.getChildren().add(chip);
        }
        showInput.run();

        HBox row = new HBox(12, Ui.label("무엇에 집중할까요", "t-label"), chips, input, count);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setMaxWidth(HALL_WIDTH);
        row.setMinHeight(32);
        return row;
    }

    private static StackPane stageBar() {
        Rectangle velvet = Ui.roundRect(0, 0, 360, 30, 15);
        velvet.setFill(Ui.velvet(-400, 240));
        Rectangle shade = Ui.roundRect(0, 0, 360, 30, 15);
        shade.setFill(Color.rgb(0, 0, 0, 0.30));
        StackPane bar = new StackPane(velvet, shade, Ui.label("무    대", "stage-bar-label"));
        bar.setMaxSize(360, 30);
        return bar;
    }

    private GridPane seatGrid(Label pickedSeat, Runnable refresh) {
        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(9);
        grid.setAlignment(Pos.CENTER);
        Button[][] buttons = new Button[ROWS][COLS];
        for (int r = 0; r < ROWS; r++) {
            grid.add(rowLabel(r), 0, r);
            for (int c = 0; c < COLS; c++) {
                Button seat = new Button(String.valueOf(c + 1));
                seat.getStyleClass().add("seat");
                if (seats.isTaken(r, c)) {
                    seat.getStyleClass().add("taken");
                    seat.setText("");
                }
                int row = r, col = c;
                seat.setOnAction(e -> {
                    if (!seats.select(row, col))
                        return;
                    for (int rr = 0; rr < ROWS; rr++)
                        for (int cc = 0; cc < COLS; cc++) {
                            Button b = buttons[rr][cc];
                            if (b.getStyleClass().remove("selected")) {
                                b.setText(String.valueOf(cc + 1));
                                Motion.scaleTo(b, 1, Motion.SEAT);
                            }
                        }
                    seat.getStyleClass().add("selected");
                    seat.setText(SeatMap.seatName(row, col).replace("열 ", "").replace("번", ""));
                    Motion.scaleTo(seat, 1.12, Motion.SEAT);
                    pickedSeat.setText(seats.getSelectedName());
                    refresh.run();
                });
                buttons[r][c] = seat;
                grid.add(seat, c + 1 + (c >= COLS / 2 ? 1 : 0), r);
            }
            grid.add(rowLabel(r), COLS + 2, r);
        }
        Region aisle = new Region();
        aisle.setMinWidth(12);
        grid.add(aisle, COLS / 2 + 1, 0);
        return grid;
    }

    private static Label rowLabel(int row) {
        Label l = Ui.label(String.valueOf((char) ('A' + row)), "row-label");
        l.setMinWidth(28);
        l.setAlignment(Pos.CENTER);
        return l;
    }

    private static HBox legendItem(String text, String state) {
        Region box = new Region();
        box.getStyleClass().add("seat");
        if (!state.isEmpty())
            box.getStyleClass().add(state);
        box.setStyle("-fx-min-width: 14px; -fx-pref-width: 14px; -fx-max-width: 14px; -fx-min-height: 14px; -fx-pref-height: 14px; -fx-max-height: 14px; -fx-background-radius: 4; -fx-border-radius: 4;");
        HBox item = new HBox(6, box, Ui.label(text, "t-caption"));
        item.setAlignment(Pos.CENTER);
        return item;
    }

    private long seed() {
        LocalDate today = LocalDate.now();
        return today.getYear() * 10_000L + today.getMonthValue() * 100L + today.getDayOfMonth()
            + (chosen.isUnlimited() ? 0 : chosen.getMinutes()) * 7L + 1;
    }

    private void showAllowList() {
        AnchorPane s = screen("screen-dim");
        Button back = Ui.anchor(Ui.back(this::showSeats), Ui.EDGE, null, null, Ui.EDGE);
        Label title = Ui.anchor(Ui.label("이번 공연에서 쓸 앱과 사이트", "t-title-sm"), Ui.EDGE + 7, null, null, Ui.EDGE + 64);
        Label desc = Ui.anchor(Ui.label("체크한 앱과 사이트에 있으면 집중으로 봅니다. 지난 공연 목록을 미리 체크해 두었습니다.", "t-body"),
            Ui.EDGE + 60, null, null, Ui.EDGE);

        if (pickedApps == null) {
            pickedApps = new ArrayList<>(ShowAllowList.precheck(lists.lastApps(), lists.apps().get()));
            pickedSites = new ArrayList<>(ShowAllowList.precheck(lists.lastSites(), lists.sites().get()));
            appChoices = new ArrayList<>(ShowAllowList.appChoices(pickedApps, List.of()));
            siteChoices = new ArrayList<>(pickedSites);
        }
        Button issue = Ui.primary("발권하기", 240);
        Label status = Ui.label("", "t-caption");
        Runnable refresh = () -> {
            issue.setDisable(!ShowAllowList.canIssue(pickedApps));
            status.setText(pickedApps.isEmpty() ? "앱을 하나 이상 체크해야 발권할 수 있습니다." : "");
        };
        VBox appBox = new VBox(0);
        VBox siteBox = new VBox(0);
        Runnable fillApps = () -> fillChecks(appBox, appChoices, pickedApps, running, refresh);
        Runnable fillSites = () -> fillChecks(siteBox, siteChoices, pickedSites, List.of(), refresh);
        fillApps.run();
        fillSites.run();

        VBox apps = checkColumn("앱", "앱 이름 (예: Notion)", appBox, name -> {
            String app = name.trim();
            if (app.isEmpty())
                return;
            if (!appChoices.contains(app))
                appChoices.add(app);
            if (!pickedApps.contains(app))
                pickedApps.add(app);
            fillApps.run();
            refresh.run();
        });
        VBox sites = checkColumn("사이트", "사이트 주소 (예: daegu.ac.kr)", siteBox, name -> {
            List<String> cleaned = SiteAllowList.clean(List.of(name));
            if (cleaned.isEmpty())
                return;
            String site = cleaned.get(0);
            if (!siteChoices.contains(site))
                siteChoices.add(site);
            if (!pickedSites.contains(site))
                pickedSites.add(site);
            fillSites.run();
        });
        HBox columns = new HBox(28, apps, sites);
        columns.setMaxWidth(HALL_WIDTH + 140);

        Button saveDefault = Ui.ghost("기본값으로 저장", 190);
        saveDefault.setOnAction(e -> {
            try {
                lists.apps().replace(pickedApps);
                lists.sites().replace(pickedSites);
                status.setText("기본 허용 목록으로 저장했습니다.");
            } catch (java.io.IOException ex) {
                status.setText("기본 허용 목록 저장에 실패했습니다.");
            }
        });
        issue.setOnAction(e -> showTicket());
        HBox bottom = new HBox(12, status, Ui.grow(), saveDefault, issue);
        bottom.setAlignment(Pos.CENTER_LEFT);
        bottom.setMaxWidth(HALL_WIDTH + 140);
        refresh.run();

        VBox column = new VBox(20, columns, bottom);
        column.setAlignment(Pos.BOTTOM_CENTER);
        Ui.anchor(column, Ui.EDGE + 110, Ui.EDGE, Ui.BOTTOM, Ui.EDGE);
        s.getChildren().addAll(column, back, title, desc);
        Motion.rise(columns, 8, Motion.ITEM, Duration.ZERO).play();

        if (running.isEmpty()) {
            Thread reader = new Thread(() -> {
                List<String> found = runningApps.get();
                javafx.application.Platform.runLater(() -> {
                    running = found;
                    for (String app : ShowAllowList.appChoices(appChoices, found))
                        if (!appChoices.contains(app))
                            appChoices.add(app);
                    fillApps.run();
                });
            });
            reader.setDaemon(true);
            reader.start();
        }
    }

    private static void fillChecks(VBox box, List<String> choices, List<String> picked, List<String> running, Runnable changed) {
        box.getChildren().clear();
        for (String name : choices) {
            CheckBox check = new CheckBox(name);
            check.getStyleClass().add("allow-check");
            check.setSelected(picked.contains(name));
            check.selectedProperty().addListener((obs, was, now) -> {
                if (now && !picked.contains(name))
                    picked.add(name);
                if (!now)
                    picked.remove(name);
                changed.run();
            });
            HBox row = new HBox(8, check, Ui.grow());
            if (running.contains(name))
                row.getChildren().add(Ui.label("실행 중", "topic-tag"));
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("check-row");
            box.getChildren().add(row);
        }
    }

    private static VBox checkColumn(String title, String prompt, VBox checks, java.util.function.Consumer<String> onAdd) {
        ScrollPane scroll = new ScrollPane(checks);
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.getStyleClass().add("check-list");
        VBox.setVgrow(scroll, Priority.ALWAYS);
        TextField input = new TextField();
        input.setPromptText(prompt);
        input.getStyleClass().add("topic-input");
        HBox.setHgrow(input, Priority.ALWAYS);
        Button add = Ui.chip("추가");
        Runnable addNow = () -> {
            onAdd.accept(input.getText());
            input.clear();
        };
        add.setOnAction(e -> addNow.run());
        input.setOnAction(e -> addNow.run());
        HBox inputRow = new HBox(8, input, add);
        inputRow.setAlignment(Pos.CENTER_LEFT);
        VBox column = new VBox(10, Ui.label(title, "t-label"), scroll, inputRow);
        column.getStyleClass().add("panel");
        column.setPadding(new Insets(18, 20, 18, 20));
        HBox.setHgrow(column, Priority.ALWAYS);
        column.setPrefWidth(440);
        return column;
    }

    void showTicket() {
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        Ticket ticket = new Ticket(chosen, seats.getSelectedName(),
            Ticket.number(now, seats.getSelectedRow(), seats.getSelectedCol()), now, topic(), pickedApps, pickedSites);
        try {
            ShowAllowList.saveLast(lists.lastApps(), pickedApps);
            ShowAllowList.saveLast(lists.lastSites(), pickedSites);
        } catch (java.io.IOException e) {
            System.err.println("지난 공연 허용 목록 저장 실패 : " + e.getMessage());
        }
        tearing = false;

        AnchorPane s = screen("screen-dim");
        Button back = Ui.anchor(Ui.back(this::showAllowList), Ui.EDGE, null, null, Ui.EDGE);
        ticketTitle = Ui.label(ISSUED, "t-title");
        HBox titleBox = new HBox(ticketTitle);
        titleBox.setAlignment(Pos.CENTER);
        titleBox.setPadding(new Insets(0, 120, 0, 120));

        Pane ticketNode = ticketPieces(ticket);
        Button reseat = Ui.ghost("좌석 다시 고르기", 190);
        reseat.setOnAction(e -> showSeats());
        Button enter = Ui.primary("입장하기", TICKET_WIDTH - 190 - 12);
        enter.setOnAction(e -> startTear(false, ticket));
        ticketButtons = List.of(back, reseat, enter);
        HBox buttons = new HBox(12, reseat, enter);
        buttons.setMaxWidth(TICKET_WIDTH);
        VBox column = new VBox(20, titleBox, ticketNode, buttons);
        VBox.setMargin(ticketNode, new Insets(-6, 0, 0, 0));
        column.setAlignment(Pos.CENTER);
        column.setPadding(new Insets(Ui.BOTTOM, 0, Ui.BOTTOM, 0));
        Ui.anchor(column, 0.0, 0.0, 0.0, 0.0);
        stubPiece.setOnMouseReleased(e -> releaseStub(e.getSceneX(), ticket));
        s.getChildren().addAll(column, back);

        buttons.setOpacity(0);
        var out = Motion.issue(ticketNode);
        out.setOnFinished(e -> Motion.fadeTo(buttons, 1, Motion.ITEM));
        out.play();
    }

    private Pane ticketPieces(Ticket ticket) {
        topPiece = piece(topContent(ticket), TOP_HEIGHT);
        stubPiece = piece(stubContent(ticket), STUB_HEIGHT);
        stubPiece.setLayoutY(TOP_HEIGHT);
        stubPiece.setCursor(Cursor.OPEN_HAND);
        stubRotate = new Rotate(0, NOTCH, 0);
        stubPiece.getTransforms().add(stubRotate);
        Line perforation = new Line(24, TOP_HEIGHT, TICKET_WIDTH - 24, TOP_HEIGHT);
        perforation.setStroke(Color.rgb(43, 30, 30, 0.18));
        perforation.setStrokeWidth(2);
        perforation.getStrokeDashArray().addAll(6.0, 4.0);
        torn = new SimpleBooleanProperty(false);
        Runnable reshape = () -> {
            topPiece.setClip(Ui.ticketTop(TICKET_WIDTH, TOP_HEIGHT, TICKET_RADIUS, NOTCH, torn.get()));
            stubPiece.setClip(Ui.ticketStub(TICKET_WIDTH, STUB_HEIGHT, TICKET_RADIUS, NOTCH, torn.get()));
            perforation.setVisible(!torn.get());
        };
        torn.addListener((obs, was, now) -> reshape.run());
        reshape.run();

        TearGesture gesture = new TearGesture();
        stubPiece.setOnMousePressed(e -> {
            if (tearing)
                return;
            gesture.press(e.getSceneX(), nowMillis());
            stubPiece.setCursor(Cursor.CLOSED_HAND);
        });
        stubPiece.setOnMouseDragged(e -> {
            if (tearing)
                return;
            gesture.drag(e.getSceneX(), nowMillis());
            double distance = gesture.distance();
            if (distance > 2)
                torn.set(true);
            stubPiece.setTranslateX(TearGesture.shownOffset(distance));
            stubRotate.setAngle(2 * TearGesture.progress(distance));
        });
        stubPiece.setUserData(gesture);

        Pane holder = new Pane(topPiece, stubPiece, perforation);
        holder.setMinSize(TICKET_WIDTH, TOP_HEIGHT + STUB_HEIGHT);
        holder.setPrefSize(TICKET_WIDTH, TOP_HEIGHT + STUB_HEIGHT);
        holder.setMaxSize(TICKET_WIDTH, TOP_HEIGHT + STUB_HEIGHT);
        holder.setEffect(new DropShadow(32, 0, 14, Color.rgb(0, 0, 0, 0.45)));
        return holder;
    }

    private static StackPane piece(Node content, double height) {
        StackPane p = new StackPane(content);
        p.getStyleClass().add("paper");
        p.setMinSize(TICKET_WIDTH, height);
        p.setPrefSize(TICKET_WIDTH, height);
        p.setMaxSize(TICKET_WIDTH, height);
        StackPane.setAlignment(content, Pos.TOP_LEFT);
        return p;
    }

    private static VBox topContent(Ticket ticket) {
        Region strip = new Region();
        strip.getStyleClass().add("paper-strip");
        strip.setMinHeight(10);
        HBox admit = new HBox(Ui.label("A D M I T   O N E", "admit"));
        admit.setAlignment(Pos.CENTER_RIGHT);

        LocalDateTime start = ticket.getStartAt();
        ShowLength length = ticket.getLength();
        Region line = new Region();
        line.setStyle("-fx-background-color: rgba(43,30,30,0.22);");
        line.setMinSize(100, 1);
        line.setMaxSize(100, 1);
        VBox middle = new VBox(6, Ui.label(LengthRuler.lengthText(length.getMinutes()), "paper-caption"), line);
        middle.setAlignment(Pos.CENTER);
        HBox times = new HBox(
            timeField(clock(start), "입장", false), Ui.grow(), middle, Ui.grow(),
            timeField(length.isUnlimited() ? "--:--" : clock(start.plusSeconds(length.getMillis() / 1000)),
                length.isUnlimited() ? "직접 끝낼 때" : "막이 내림", true));
        times.setAlignment(Pos.CENTER);

        HBox row1 = new HBox(Ui.field("좌석", ticket.getSeat(), "paper-label", "paper-value", false), Ui.grow(),
            Ui.field("공연 길이", LengthRuler.lengthText(length.getMinutes()), "paper-label", "paper-value", true));
        HBox row2 = new HBox(Ui.field("집중 주제", ticket.getTopic(), "paper-label", "paper-name", false), Ui.grow(),
            Ui.field("날짜", date(start), "paper-label", "paper-value", true));
        VBox body = new VBox(0, admit, times, row1, row2);
        VBox.setMargin(row1, new Insets(34, 0, 0, 0));
        VBox.setMargin(row2, new Insets(14, 0, 0, 0));
        body.setPadding(new Insets(14, 36, 0, 36));
        return new VBox(strip, body);
    }

    private static VBox timeField(String time, String label, boolean right) {
        VBox box = new VBox(2, Ui.label(time, "paper-big"), Ui.label(label, "paper-label"));
        box.setAlignment(right ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        return box;
    }

    private static VBox stubContent(Ticket ticket) {
        Label tip = Ui.label("입장하면 이 창은 접힙니다. 공연은 메뉴바 아이콘 > 공연 종료로 끝냅니다. 막이 내려오기 시작하면 왼쪽 아래 ■ 버튼을 길게 눌러도 끝납니다.", "paper-caption");
        tip.setWrapText(true);
        Region gap = new Region();
        VBox.setVgrow(gap, Priority.ALWAYS);
        Pane bars = Ui.barcode(ticket.getNumber(), 400, 70, Ui.INK);
        VBox stub = new VBox(0, Ui.label("✂ 여기를 끌어 찢기", "tear-hint"), bars,
            Ui.label("No. " + ticket.getNumber(), "paper-mono-caption"), gap, tip);
        VBox.setMargin(bars, new Insets(12, 0, 8, 0));
        stub.setPadding(new Insets(24, 36, 22, 36));
        stub.setPrefHeight(STUB_HEIGHT);
        return stub;
    }

    private void releaseStub(double sceneX, Ticket ticket) {
        if (tearing)
            return;
        stubPiece.setCursor(Cursor.OPEN_HAND);
        TearGesture gesture = (TearGesture) stubPiece.getUserData();
        if (gesture.release(sceneX, nowMillis())) {
            startTear(true, ticket);
            return;
        }
        Timeline back = new Timeline(new KeyFrame(Motion.SETTLE,
            new KeyValue(stubPiece.translateXProperty(), 0, Motion.EASE_OUT),
            new KeyValue(stubRotate.angleProperty(), 0, Motion.EASE_OUT)));
        back.setOnFinished(e -> torn.set(false));
        back.play();
    }

    private void startTear(boolean fromDrag, Ticket ticket) {
        if (tearing)
            return;
        tearing = true;
        ticketButtons.forEach(b -> b.setDisable(true));
        Timeline tear = tearTimeline();
        tear.setOnFinished(e -> onEnter.accept(ticket));
        if (fromDrag)
            tear.playFrom(Duration.millis(120));
        else
            tear.play();
    }

    Timeline tearTimeline() {
        double fromX = stubPiece.getTranslateX();
        Interpolator out = Motion.EASE_OUT;
        return new Timeline(
            new KeyFrame(Duration.ZERO,
                kv(stubPiece.translateXProperty(), fromX, out), kv(stubPiece.translateYProperty(), 0.0, out),
                kv(stubRotate.angleProperty(), stubRotate.getAngle(), out), kv(stubPiece.opacityProperty(), 1.0, out),
                kv(topPiece.translateYProperty(), 0.0, out), kv(topPiece.opacityProperty(), 1.0, out),
                kv(torn, torn.get(), Interpolator.DISCRETE), kv(ticketTitle.textProperty(), ISSUED, Interpolator.DISCRETE)),
            new KeyFrame(Duration.millis(60), kv(torn, true, Interpolator.DISCRETE)),
            new KeyFrame(Duration.millis(120),
                kv(stubPiece.translateXProperty(), fromX, out), kv(stubPiece.translateYProperty(), 4.0, out),
                kv(stubRotate.angleProperty(), 2.0, out)),
            new KeyFrame(Duration.millis(240), kv(stubPiece.opacityProperty(), 1.0, out)),
            new KeyFrame(Duration.millis(420),
                kv(stubPiece.translateXProperty(), fromX + 60, out), kv(stubPiece.translateYProperty(), 90.0, out),
                kv(stubRotate.angleProperty(), -14.0, out), kv(stubPiece.opacityProperty(), 0.0, out),
                kv(topPiece.translateYProperty(), 0.0, out), kv(topPiece.opacityProperty(), 1.0, out),
                kv(ticketTitle.textProperty(), ENTERING, Interpolator.DISCRETE)),
            new KeyFrame(Duration.millis(720),
                kv(topPiece.translateYProperty(), -24.0, out), kv(topPiece.opacityProperty(), 0.0, out)));
    }

    private static <T> KeyValue kv(WritableValue<T> target, T value, Interpolator interpolator) {
        return new KeyValue(target, value, interpolator);
    }

    void previewTear(double millis) {
        ticketButtons.forEach(b -> b.setDisable(millis > 0));
        Timeline tear = tearTimeline();
        tear.play();
        tear.jumpTo(Duration.millis(millis));
        tear.pause();
    }

    private static long nowMillis() {
        return System.nanoTime() / 1_000_000;
    }

    private static String clock(LocalDateTime at) {
        return at.format(CLOCK);
    }

    private static String date(LocalDateTime at) {
        return at.format(DATE);
    }
}
