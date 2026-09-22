package focus;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import javafx.geometry.Rectangle2D;
import javafx.stage.Screen;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setAlwaysOnTop(true);
        stage.initStyle(StageStyle.TRANSPARENT);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();   // 화면 크기 정보를 screenBounds에 저장

        // 반투명 검은 오버레이 창 만들기
        StackPane root = new StackPane();
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());
        scene.setFill(Color.rgb(0, 0, 0, 0.5)); // 검은색, 불투명도 0.5
        stage.setScene(scene);

        root.setMouseTransparent(true); // 오버레이가 마우스 클릭을 가로채지 않게
        // setMouseTransparent는 javaFx 화면 안에서만 통함 - macOs 레벨 클릭 통과는 별도 네이티브 연동 조사 필요

        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.show();   // 창 띄우기

        ArrayList<String> allowedApps = new ArrayList<>();  // 허용 목록: 작업용으로 인정할 앱 이름
        allowedApps.add("Code");
        allowedApps.add("Terminal");

        DetectionState state = new DetectionState();

        // 감지 스레드: 1초마다 맨 앞 앱을 확인해 허용 목록과 비교
        Thread watcher = new Thread(() -> {
            while (true) {  // 프로그램이 꺼질 때 까지 반복
                String app = frontApp(); // 앱 이름을 한 번만 구해서 재사용 (osascript 두 번 실행 방지)
                boolean allowed = allowedApps.contains(app); // 허용 목록에 있는지 판정
                System.out.println("front: " + app + " / 허용 : " + allowed); // 앱 이름과 허용 여부 출력
                state.setCurrentAppName(app);
                state.setDistracting(!allowed);
                // 오버레이가 이 state를 읽어갈 자리 (검토 2차 때 연결)
                try {
                    Thread.sleep(1000); // 1초 대기
                } catch (InterruptedException e) {  // 스레드 종료 신호를 받으면 루프 탈출
                    break;
                }
            }
        });
        watcher.setDaemon(true);    // 창 닫으면 이 스레드도 같이 종료되게
        watcher.start();    // 스레드 시작
    }   

    // 제일 앞에 떠 있는 앱 이름을 알아내는 메서드 / 실패하면 (unknown)을 돌려줌
    private String frontApp() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "osascript", "-e",
                "tell application \"System Events\" to get name of first process whose frontmost is true");
                pb.redirectErrorStream(true);   // 정상 출력과 에러 출력을 합쳐서 에러도 놓치지 않게
                Process p = pb.start(); // osascript 실행
                try (BufferedReader r = new BufferedReader(new InputStreamReader (p.getInputStream()))) { // 블록이 끝나면 자동으로 스트림 닫힘
                String name = r.readLine();    // 실행 결과에서 앱 이름 한 줄 읽어오기
                p.waitFor();    // 명령어 실행이 끝날 때 까지 대기
                return name;
                }
            } catch (InterruptedException e) {  // 인터럽트 상태 복원
                Thread.currentThread().interrupt();
                return "(unknown)";
            } catch (Exception e) { // 에러 원인 콘솔에 출력
                System.err.println("frontApp 실패 : " + e.getMessage());
                return "(unknown)"; 
            }
        }
    public static void main(String[] args) {
        launch(args);
    }
}
