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
import java.util.ArrayList;
import java.util.List;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.NativeLong;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        stage.setAlwaysOnTop(true);    // 항상 위에오게
        stage.initStyle(StageStyle.TRANSPARENT);    // 배경 안 칠하게
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();    // 주 모니터의 사용 가능한 화면 크기 저장

        DetectionState detectionState = new DetectionState();    // 감지 결과를 담을 객체생성

        // 커튼을 담을 화면 판과 투명한 Scene을 준비
        StackPane root = new StackPane();
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());    // 실제 화면 크기로 Scene 생성
        scene.setFill(Color.TRANSPARENT);    // Scene 배경을 투명하게 설정
        stage.setScene(scene);
        CurtainOverlay curtainOverlay = new CurtainOverlay(scene.getWidth(), scene.getHeight());    // 현재 화면 크기로 커튼 객체 생성
        root.getChildren().add(curtainOverlay.getView());    // 커튼 판을 root 화면에 추가

        Duration interval = Duration.seconds(1);    // 실행 간격 설정
        KeyFrame keyFrame = new KeyFrame(interval, event -> {    // 1초 간격과 실행할 작업을 받는 KeyFrame 생성 시작
            if (detectionState.isDistracting()) {
                detectionState.setDistractionElapsedSeconds(detectionState.getDistractionElapsedSeconds() + 1);    // 경과 시간 증가
            } else {
                detectionState.setDistractionElapsedSeconds(0);    // 집중 상태일 때 경과 시간 초기화
            }
            curtainOverlay.updateCurtain(detectionState.getDistractionElapsedSeconds(), detectionState.isDistracting());
            System.out.println(detectionState.getDistractionElapsedSeconds());    // 시간이 되면 터미널에 증가 여부 출력
        });
        Timeline timeline = new Timeline();    // 반복 실행의 일정표 객체생성
        timeline.getKeyFrames().add(keyFrame);    // timeline안에 keyFrame 추가
        timeline.setCycleCount(Timeline.INDEFINITE);    // Timeline 반복 횟수 설정
        timeline.play();    // Timeline 실행 시작
        root.setMouseTransparent(true); // 오버레이가 마우스 클릭을 가로채지 않게
        // setMouseTransparent는 javaFx 화면 안에서만 통함 - macOS 레벨 클릭 통과는 아래 enableMacClickThrough()에서 해결함

        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        stage.show();   // 창 띄우기
        enableMacClickThrough();

        ArrayList<String> allowedApps = new ArrayList<>();  // 허용 목록: 작업용으로 인정할 앱 이름
        allowedApps.add("Code");
        allowedApps.add("Terminal");

        // 감지 스레드: 1초마다 맨 앞 앱을 확인해 허용 목록과 비교
        Thread watcher = new Thread(() -> {
            while (true) {  // 프로그램이 꺼질 때 까지 반복
                String app = frontApp(); // 앱 이름을 한 번만 구해서 재사용 (osascript 두 번 실행 방지)
                boolean distracting = isDistracting(app, allowedApps); // 딴짓인지 판정
                System.out.println("front: " + app + " / 딴짓 : " + distracting); // 딴짓 여부
                detectionState.setCurrentAppName(app);
                detectionState.setDistracting(distracting);
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

    /*
    클릭이 오버레이를 그냥 통과해서 뒤에 있는 다른 앱을 누를 수 있게 하는 코드
    setMouseTransparent(위에 있는 줄)는 JavaFx 창 안에서만 통하고 진짜로 macOS한테
    "이 창은 클릭 무시해도 돼"라고 알려주려면 이 메서드가 필요해서 넣음
    JNA라는 도구로 macOS 쪽 코드에 직접 말을 걸어서 처리함
    */
    private void enableMacClickThrough() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("mac")) return;
        try {
            NativeLibrary objc = NativeLibrary.getInstance("objc");
            Function getClass = objc.getFunction("objc_getClass");
            Function sel = objc.getFunction("sel_registerName");
            Function msg = objc.getFunction("objc_msgSend");

            Pointer nsAppCls = getClass.invokePointer(new Object[]{"NSApplication"});
            Pointer nsApp = msg.invokePointer(new Object[]{nsAppCls, sel.invokePointer(new Object[]{"sharedApplication"})});
            Pointer windows = msg.invokePointer(new Object[]{nsApp, sel.invokePointer(new Object[]{"windows"})});
            long count = msg.invokeLong(new Object[]{windows, sel.invokePointer(new Object[]{"count"})});

            Pointer setSel = sel.invokePointer(new Object[]{"setIgnoresMouseEvents:"});
            for (long i = 0; i < count; i++) {
                Pointer w = msg.invokePointer(new Object[]{
                    windows, sel.invokePointer(new Object[]{"objectAtIndex:"}), new NativeLong(i)});
                msg.invokeVoid(new Object[]{w, setSel, (byte) 1});  // YES
            }
        } catch (Throwable t) {
            // 클릭 통과 실패해도 앱은 계속 뜨게
            System.err.println("클릭 통과 설정 실패(무시하고 계속): " + t.getMessage());
        }
    }

    // 딴짓 판정 / 모르면 딴짓 아님(커튼에 갇히지 않게)
    static boolean isDistracting(String app, List<String> allowed) {
        if (app == null || app.equals("(unknown)")) return false;   // 모름 -> 딴짓 아님
        if (app.equals("java")) return false;                       // 자기 자신 -> 딴짓 아님
        return !allowed.contains(app);                              // 목록에 없으면 딴짓
    }

    // 제일 앞에 떠 있는 앱 이름을 알아내는 메서드 / 실패하면 (unknown)을 돌려줌
    private String frontApp() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                "osascript", "-e",
                "tell application \"System Events\" to get name of first process whose frontmost is true");
                pb.redirectError(ProcessBuilder.Redirect.DISCARD);   // 에러 출력은 버려서 오류 문장이 앱 이름으로 섞이지 않게
                Process p = pb.start(); // osascript 실행
                try (BufferedReader r = new BufferedReader(new InputStreamReader (p.getInputStream()))) { // 블록이 끝나면 자동으로 스트림 닫힘
                String name = r.readLine();    // 실행 결과에서 앱 이름 한 줄 읽어오기
                int code = p.waitFor(); // 실행이 끝날 때까지 기다리고 종료코드(0=성공)를 받아둠
                if (code != 0 || name == null) return "(unknown)";
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
