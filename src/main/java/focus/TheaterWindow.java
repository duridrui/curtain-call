package focus;

import java.util.List;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.geometry.Pos;
import javafx.scene.effect.DropShadow;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Screen;
import javafx.stage.Stage;

// 로비, 예매, 관람 기록, 커튼콜을 번갈아 보여 주는 극장 창 하나
public class TheaterWindow {
    static final double DRAPE_WIDTH = 56;       // 양옆 커튼 폭
    static final double VALANCE_HEIGHT = 26;    // 위 가로 휘장 높이

    // 화면에서 누른 버튼을 Main에 넘기는 통로
    interface Actions {
        void enter(Ticket ticket);      // 티켓으로 입장 -> 공연 시작
        void openSettings();            // 허용 목록 설정 창
        void quit();                    // 앱 종료
        List<ShowRecord> history();     // 관람 기록 (최근 순)
        BookingView.Lists lists();      // 예매의 허용 목록 단계가 쓰는 목록
        List<String> runningApps();     // 지금 실행 중인 앱 (허용 목록 단계)
    }

    private final Stage stage = new Stage();
    private final StackPane content = new StackPane();     // 지금 화면이 들어가는 칸
    private final Actions actions;
    private Boolean dndReady;                              // 방해금지 연동 단축어가 있는지, 확인 전이면 null
    private boolean placed;                                // 화면 가운데에 한 번 놓았는지 (그 뒤엔 사용자가 옮긴 자리 유지)
    private boolean onLobby;                               // 지금 로비 화면인지

    // 창 만들기: 화면이 들어갈 칸을 놓고, 주 모니터에 맞춘 크기로 창을 준비함 (띄우는 일은 bringToFront)
    public TheaterWindow(Actions actions) {
        this.actions = actions;
        StackPane root = new StackPane(content, drapes());
        WindowSize size = sizeForPrimaryScreen();
        Scene scene = new Scene(root, size.getWidth(), size.getHeight());
        scene.getStylesheets().add(TheaterWindow.class.getResource("/css/theater.css").toExternalForm());
        stage.setTitle("Curtain Call");
        stage.setScene(scene);
        stage.setResizable(true);
        stage.setMinWidth(size.getMinWidth());
        stage.setMinHeight(size.getMinHeight());
        stage.setOnShown(event -> {             // 최소 크기는 내용 기준: 제목 표시줄만큼 더함
            stage.setMinWidth(size.getMinWidth() + stage.getWidth() - scene.getWidth());
            stage.setMinHeight(size.getMinHeight() + stage.getHeight() - scene.getHeight());
        });
        stage.setOnCloseRequest(event -> {      // 창 닫기 버튼은 앱 종료가 아니라 숨기기 (메뉴바에서 다시 엶)
            event.consume();
            stage.hide();
        });
    }

    // 주 모니터 사용 영역 기준 크기와 가운데 위치
    private static WindowSize sizeForPrimaryScreen() {
        javafx.geometry.Rectangle2D area = Screen.getPrimary().getVisualBounds();
        return WindowSize.theater(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());
    }

    // 창 양쪽 끝에 걸린 벨벳 커튼(폭 56)과 위 가로 휘장(높이 26). 화면 내용 위에 얹고 클릭은 통과.
    // 이미지는 높이 112%로 맞춰 아래 바닥 줄무늬가 안 보이게 하고 안쪽으로 그림자를 떨어뜨려 입체감을 냄
    static StackPane drapes() {
        StackPane layer = new StackPane();
        layer.setMouseTransparent(true);
        layer.setMinSize(0, 0);                         // 장식 크기가 창 크기를 따라가므로 장식이 창 크기를 붙잡지 않게 최소 크기는 0
        if (Ui.curtainImage() == null)
            return layer;
        Rectangle left = new Rectangle(DRAPE_WIDTH, 0);
        Rectangle right = new Rectangle(DRAPE_WIDTH, 0);
        left.heightProperty().bind(layer.heightProperty());
        right.heightProperty().bind(layer.heightProperty());
        double imageHeight = 900 * 1.12;                                    // 창 높이 900 기준 112%
        left.setFill(Ui.velvet(0, imageHeight));
        right.setFill(Ui.velvet(DRAPE_WIDTH - Ui.velvetWidth(imageHeight), imageHeight));
        left.setEffect(new DropShadow(28, 8, 0, Color.rgb(0, 0, 0, 0.65)));
        right.setEffect(new DropShadow(28, -8, 0, Color.rgb(0, 0, 0, 0.65)));
        StackPane.setAlignment(left, Pos.CENTER_LEFT);
        StackPane.setAlignment(right, Pos.CENTER_RIGHT);
        Rectangle valance = new Rectangle(0, VALANCE_HEIGHT);
        valance.widthProperty().bind(layer.widthProperty());
        valance.setFill(Ui.velvet(0, imageHeight));
        valance.setEffect(new DropShadow(24, 0, 10, Color.rgb(0, 0, 0, 0.65)));
        StackPane.setAlignment(valance, Pos.TOP_CENTER);
        layer.getChildren().addAll(left, right, valance);
        return layer;
    }

    // 로비에 보여 줄 방해금지 준비 상태 (Main이 단축어를 확인한 뒤 알려 줌)
    public void setDndReady(boolean ready) {
        dndReady = ready;
    }

    // 로비 화면: 방해금지 준비 상태, 마지막 공연, 예매, 설정, 종료
    public void showLobby() {
        onLobby = true;
        List<ShowRecord> history = actions.history();
        setContent(new LobbyView(dndReady, history.isEmpty() ? null : history.get(0), history.size(),
            this::showBooking, () -> { }, actions::openSettings, actions::quit).build());   // 관람 기록 화면(HistoryView)이 생기면 this::showHistory
        bringToFront(false);
    }

    // 로비를 보고 있을 때만 새로 그림 (방해금지 연동 확인처럼 늦게 끝나는 작업이 예매 중인 화면을 로비로 되돌리지 않게)
    public void refreshLobby() {
        if (onLobby && stage.isShowing())
            showLobby();
    }

    // 예매 화면: 티켓을 찢으면 극장 창을 숨기고 공연 시작
    public void showBooking() {
        onLobby = false;
        setContent(new BookingView(ticket -> {
            stage.hide();
            actions.enter(ticket);
        }, this::showLobby, actions.lists(), actions::runningApps).build());
    }

    // 창을 숨김 (공연이 시작되면 극장 창을 치우고 커튼 창만 남김)
    public void hide() {
        stage.hide();
    }

    // 창이 지금 화면에 떠 있는지
    public boolean isShowing() {
        return stage.isShowing();
    }

    // 창을 앞으로. 커튼 창이 항상 위라 커튼콜 때는 이 창도 항상 위로 둠
    private void bringToFront(boolean onTop) {
        stage.setAlwaysOnTop(onTop);
        if (!placed) {      // 처음 띄울 때만 가운데로
            WindowSize size = sizeForPrimaryScreen();
            stage.setX(size.getX());
            stage.setY(size.getY());
            placed = true;
        }
        if (!stage.isShowing())
            stage.show();
        stage.toFront();
        stage.requestFocus();
    }

    // 지금 화면을 view로 바꾸고, 새 화면이 아래에서 올라오며 나타나게 함
    void setContent(Parent view) {
        content.getChildren().setAll(view);     // 이전 화면은 바로 사라지고 새 화면만 들어옴
        Motion.screenIn(view);
    }

    // 창 객체 (같은 패키지에서 창 상태를 직접 확인할 때)
    Stage getStage() {
        return stage;
    }
}
