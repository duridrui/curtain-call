package focus;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.scene.paint.Color;
import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setAlwaysOnTop(true);     // 항상 위에오게
        stage.initStyle(StageStyle.TRANSPARENT);    // 배경 안 칠하게

        // 검은색(불투명도 0.5) 창 만들기
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 400, 300);
        scene.setFill(Color.rgb(0, 0, 0, 0.5));
        stage.setScene(scene);

        root.setMouseTransparent(true); // 마우스 클릭 무시

        stage.show();   // 창 띄우기

        // 감지 스레드
        Thread watcher = new Thread(() -> {
            while (true) {  // 프로그램이 꺼질 때 까지 반복
                System.out.println("front: " + frontApp()); // 어떤 앱을 보고 있는지 터미널에 
                // 예외 상황 대비
                try {
                    Thread.sleep(1000); // 1초마다
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        watcher.setDaemon(true);    // 창 닫으면 이 작업도 꺼지게
        watcher.start();    // Thread 시작
    }   

    // 제일 앞에 떠 있는 앱 이름을 알아내는 메서드 / 실패하면 (unknown)을 돌려줌
    private String frontApp() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "osascript", "-e",
                "tell application \"System Events\" to get name of first process whose frontmost is true"); // 터미널에 칠 명령어 (제일 앞에 있는 프로세스 이름이 뭔지)
                pb.redirectErrorStream(true);   // 정상출력과 에러 출력을 합쳐 에러 상황 대비
                Process p = pb.start(); // 명령어 실행
                try (BufferedReader r = new BufferedReader(new InputStreamReader (p.getInputStream()))) { // 실행결과를 읽을 수 있는 글자로 바꾸고 한 줄 씩 포장 + try 형태로 자동 닫힘
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
