package focus;

import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Arc;
import javafx.scene.shape.ArcType;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.StrokeLineCap;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;

// 막이 내려오는 동안 화면 왼쪽 아래 '공연 종료' 버튼 (지름 44). 600ms 길게 누르면 메뉴바 '공연 종료'와 같은 동작.
// 커튼 창은 클릭이 아래 앱으로 통과하므로, 이 버튼만 52x52 작은 창으로 따로 띄워 클릭을 받는다
final class EndButton {
    private static final double REST_OPACITY = 0.62;    // 평소 불투명도, 마우스를 올리면 1
    private final Stage stage = new Stage();
    private final LongPress press = new LongPress();
    private final StackPane button;
    private final Arc ring = new Arc();                 // 누르는 동안 시계 방향으로 차는 링
    private final AnimationTimer timer;
    private final Pane root;

    EndButton(Runnable onEnd) {
        double size = EndButtonZone.SIZE;
        Circle base = new Circle(EndButtonZone.DIAMETER / 2, Color.rgb(11, 8, 8, 0.62));
        base.setStroke(Color.rgb(245, 243, 241, 0.22));
        Rectangle square = new Rectangle(14, 14, Color.WHITE);
        button = new StackPane(base, square);
        button.setLayoutX(EndButtonZone.PAD);
        button.setLayoutY(EndButtonZone.PAD);
        button.setPrefSize(EndButtonZone.DIAMETER, EndButtonZone.DIAMETER);
        button.setOpacity(REST_OPACITY);

        ring.setCenterX(size / 2);
        ring.setCenterY(size / 2);
        ring.setRadiusX(size / 2 - 1.5);
        ring.setRadiusY(size / 2 - 1.5);
        ring.setStartAngle(90);
        ring.setType(ArcType.OPEN);
        ring.setFill(null);
        ring.setStroke(Color.web("#E3222B"));
        ring.setStrokeWidth(3);
        ring.setStrokeLineCap(StrokeLineCap.ROUND);
        ring.setMouseTransparent(true);

        root = new Pane(button, ring);
        root.setStyle("-fx-background-color: transparent;");
        root.setOnMouseEntered(e -> button.setOpacity(1));
        root.setOnMouseExited(e -> button.setOpacity(REST_OPACITY));

        timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                long millis = now / 1_000_000;
                ring.setLength(-360 * press.progress(millis));      // 음수 = 시계 방향
                if (press.poll(millis)) {
                    release();
                    onEnd.run();
                }
            }
        };
        root.setOnMousePressed(e -> {
            press.press(System.nanoTime() / 1_000_000);
            ring.setOpacity(1);
            Motion.scaleTo(button, 0.97, Motion.PRESS);
            timer.start();
        });
        root.setOnMouseReleased(e -> release());

        Scene scene = new Scene(root, size, size, Color.TRANSPARENT);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.setAlwaysOnTop(true);
        stage.setTitle("Curtain Call End");
        stage.setScene(scene);
    }

    // 손을 떼면 링이 200ms 동안 사라짐
    private void release() {
        press.release();
        timer.stop();
        Motion.scaleTo(button, 1, Motion.RELEASE);
        FadeTransition fade = new FadeTransition(Duration.millis(200), ring);
        fade.setToValue(0);
        fade.setInterpolator(Motion.EASE_OUT);
        fade.setOnFinished(e -> ring.setLength(0));
        fade.play();
    }

    // 막이 내려오기 시작하면 주 모니터 왼쪽 아래에 띄움 (1초마다 불려도 이미 떠 있으면 그대로)
    void show() {
        if (stage.isShowing())
            return;
        Rectangle2D area = Screen.getPrimary().getVisualBounds();
        EndButtonZone zone = EndButtonZone.of(area.getMinX(), area.getMinY(), area.getWidth(), area.getHeight());
        stage.setX(zone.getX());
        stage.setY(zone.getY());
        ring.setLength(0);
        button.setOpacity(REST_OPACITY);
        stage.show();
    }

    void hide() {
        if (!stage.isShowing())
            return;
        press.release();
        timer.stop();
        stage.hide();
    }
}
