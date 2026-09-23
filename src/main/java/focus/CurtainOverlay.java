package focus;

import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.layout.StackPane;
import javafx.geometry.Pos;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.application.Platform;

public class CurtainOverlay {
    private Rectangle leftCurtain = new Rectangle();                               // 왼쪽 커튼 객체를 저장
    private Rectangle rightCurtain = new Rectangle();                              // 오른쪽 커튼 객체를 저장
    private StackPane view = new StackPane();                                      // 커튼 두 장을 담을 화면 판을 저장
    private TranslateTransition leftTransition =
            new TranslateTransition(Duration.seconds(1), leftCurtain);          // 왼쪽 커튼을 1초 동안 이동시키는 장치
    private TranslateTransition rightTransition =
            new TranslateTransition(Duration.seconds(1), rightCurtain);         // 오른쪽 커튼을 1초 동안 이동시키는 장치

    public CurtainOverlay(double screenWidth, double screenHeight) {
        leftCurtain.setWidth(screenWidth / 2);                                    // 왼쪽 커튼 가로 길이 설정
        leftCurtain.setHeight(screenHeight);                                      // 왼쪽 커튼 세로 길이 설정
        rightCurtain.setWidth(screenWidth / 2);                                   // 오른쪽 커튼 가로 길이 설정
        rightCurtain.setHeight(screenHeight);                                     // 오른쪽 커튼 세로 길이 설정

        Color curtainColor = Color.rgb(0, 0, 0, 0.8);    // 검은색 커튼 불투명도 80%
        leftCurtain.setFill(curtainColor);
        rightCurtain.setFill(curtainColor);

        StackPane.setAlignment(leftCurtain, Pos.CENTER_LEFT);                     // 커튼을 각 화면 끝에 정렬
        StackPane.setAlignment(rightCurtain, Pos.CENTER_RIGHT);
        view.getChildren().addAll(leftCurtain, rightCurtain);                     // 두 커튼을 view 안에 추가

        leftCurtain.setTranslateX(-(screenWidth / 2));                            // 커튼을 화면 바깥으로 이동해 열린 상태로
        rightCurtain.setTranslateX(screenWidth / 2);
    }

    public StackPane getView() {                                                  // Main이 커튼 판을 가져갈 수 있게 반환
        return view;
    }

    public void updateCurtain(long elapsedSeconds, boolean isDistracting) {
        double closeProgress;                                                     // 닫힌 정도를 소수로 저장

        if (isDistracting) {                                                      // 딴짓 중인지 확인
            closeProgress = Math.min(1.0, elapsedSeconds / 10.0);                 // 결과가 1.0이 넘지 않도록 제한
        } else {
            closeProgress = 0.0;
        }
        Platform.runLater(() -> {                                                 // 화면 변경 작업을 FX 스레드에서 실행
            double curtainOffset = leftCurtain.getWidth() * (1.0 - closeProgress);    // 닫히지 않은 비율을 실제 커튼 이동 거리로 변환
            leftTransition.setToX(-curtainOffset);
            rightTransition.setToX(curtainOffset);
            leftTransition.playFromStart();
            rightTransition.playFromStart();
        });
    }
}
