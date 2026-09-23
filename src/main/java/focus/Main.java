package focus;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javafx.animation.Timeline;
import javafx.animation.KeyFrame;
import javafx.util.Duration;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setAlwaysOnTop(true);                          // 항상 위에오게
        stage.initStyle(StageStyle.TRANSPARENT);                   // 배경 안 칠하게
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();    // 주 모니터의 사용 가능한 화면 크기 저장

        DetectionState detectionState = new DetectionState();      // 감지 결과를 담을 객체생성
        detectionState.setDistracting(true);
        detectionState.setDistractionElapsedSeconds(0);

        // 커튼을 담을 화면 판과 투명한 Scene을 준비
        StackPane root = new StackPane();
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());                 // 실제 화면 크기로 Scene 생성
        scene.setFill(Color.TRANSPARENT);                          // Scene 배경을 투명하게 설정
        stage.setScene(scene);
        CurtainOverlay curtainOverlay = new CurtainOverlay(scene.getWidth(), scene.getHeight());          // 현재 화면 크기로 커튼 객체 생성
        root.getChildren().add(curtainOverlay.getView());                                                 // 커튼 판을 root 화면에 추가

        root.setMouseTransparent(true);                                                            // 마우스 클릭 무시

        stage.setX(screenBounds.getMinX());     // 창을 화면의 왼쪽 시작점에 배치
        stage.setY(screenBounds.getMinY());     // 창을 화면의 위쪽 시작점에 배치
        stage.show();                                                                                     // 창 띄우기

        Duration interval = Duration.seconds(1);                                                       // 실행 간격 설정
        KeyFrame keyFrame = new KeyFrame(interval, event -> {                                            // 1초 간격과 실행할 작업을 받는 KeyFrame 생성 시작
            if (detectionState.isDistracting()) {
                detectionState.setDistractionElapsedSeconds(detectionState.getDistractionElapsedSeconds() + 1);                 // 경과 시간 증가
                if (detectionState.getDistractionElapsedSeconds() > 10) {
                    detectionState.setDistracting(false);                                                           // 경과 시간이 10초를 넘으면 false 전환
                }
            } else {
                detectionState.setDistractionElapsedSeconds(Math.max(0, detectionState.getDistractionElapsedSeconds() - 1));    // 집중 상태일 때 경과 시간을 1씩 감소
            }
            curtainOverlay.updateCurtain(detectionState.getDistractionElapsedSeconds(), detectionState.isDistracting());
            System.out.println(detectionState.getDistractionElapsedSeconds());                                                  // 시간이 되면 터미널에 증가 여부 출력
        });
        Timeline timeline = new Timeline();                         // 반복 실행의 일정표 객체생성
        timeline.getKeyFrames().add(keyFrame);                      // timeline안에 keyFrame 추가
        timeline.setCycleCount(Timeline.INDEFINITE);                // Timeline 반복 횟수 설정
        timeline.play();                                            // Timeline 실행 시작
        // 감지 스레드
        Thread watcher = new Thread(() -> {
            while (true) {                                          // 프로그램이 꺼질 때 까지 반복
                System.out.println("front: " + frontApp());         // 어떤 앱을 보고 있는지 터미널에
                // 예외 상황 대비
                try {
                    Thread.sleep(1000); // 1초마다
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        watcher.setDaemon(true);                                     // 창 닫으면 이 작업도 꺼지게
        watcher.start();                                             // Thread 시작
    }   

    // 제일 앞에 떠 있는 앱 이름을 알아내는 메서드 / 실패하면 (unknown)을 돌려줌
    private String frontApp() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "osascript", "-e",
                "tell application \"System Events\" to get name of first process whose frontmost is true");     // 터미널에 칠 명령어 (제일 앞에 있는 프로세스 이름이 뭔지)
                Process p = pb.start(); // 명령어 실행
                BufferedReader reader = new BufferedReader(new InputStreamReader (p.getInputStream()));         // 실행결과를 읽을 수 있는 글자로 바꾸고 한 줄 씩 포장
                String name = reader.readLine();    // 실행 결과에서 앱 이름 한 줄 읽어오기
                p.waitFor();                        // 명령어 실행이 끝날 때 까지 대기
                return name;
            } catch (Exception e) {                 // 문제 생기면 unknown으로 처리
                return "(unknown)"; 
            }
        }
    public static void main(String[] args) {
        launch(args);
    }
}
