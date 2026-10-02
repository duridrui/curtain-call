package focus;

import java.util.List;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;

// 로비, 예매, 관람 기록, 커튼콜을 번갈아 보여 주는 극장 창 하나
public class TheaterWindow {

    // 화면에서 누른 버튼을 Main에 넘기는 통로
    interface Actions {
        void enter(Ticket ticket);      // 티켓으로 입장 -> 공연 시작
        void openSettings();            // 허용 목록 설정 창
        void quit();                    // 앱 종료
        List<ShowRecord> history();     // 관람 기록 (최근 순)
    }

    private final Stage stage = new Stage();
    private final StackPane content = new StackPane();     // 지금 화면이 들어가는 칸
    private final Actions actions;
    private boolean placed;                                // 화면 가운데에 한 번 놓았는지 (그 뒤엔 사용자가 옮긴 자리 유지)

    // 창 만들기: 화면이 들어갈 칸을 놓고, 주 모니터에 맞춘 크기로 창을 준비함 (띄우는 일은 bringToFront)
    public TheaterWindow(Actions actions) {
        this.actions = actions;
        StackPane root = new StackPane(content);
        WindowSize size = sizeForPrimaryScreen();
        Scene scene = new Scene(root, size.getWidth(), size.getHeight());
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
