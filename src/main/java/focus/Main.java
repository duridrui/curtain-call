package focus;

import javafx.application.Application;
import javafx.application.Platform;
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
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Function;
import com.sun.jna.Pointer;
import com.sun.jna.NativeLong;
import java.nio.file.Path;

public class Main extends Application {
    private static final String OVERLAY_TITLE = "Curtain Call Overlay";
    // 탭 주소로 사이트 판정을 하는 브라우저, 이 밖의 브라우저는 앱 이름으로만 판정
    static final List<String> BROWSERS = List.of("Google Chrome", "Safari");
    // 실행 중인 앱 읽기 (예매의 허용 목록 단계가 고를 앱 목록)
    static java.util.function.Supplier<List<String>> runningAppsReader = ShowAllowList::runningApps;

    // 감지 스레드와 화면이 함께 쓰는 공연 정보
    private final DetectionState detectionState = new DetectionState();         // 감지 결과를 담을 객체
    private final ShowSession show = new ShowSession();                         // 공연 상태와 이번 공연 통계
    private volatile Ticket ticket;                                             // 지금 공연의 티켓
    private final CurtainDownRule curtainDownRule = new CurtainDownRule();      // 막이 다 닫히면 중도 종료 (화면 스레드만)
    private javafx.animation.PauseTransition pendingCurtainCall;                // 중도 종료 뒤 커튼콜까지 기다리는 중
    private ShowRecord earlierActs;                                             // 공연 이어보기 중이면 앞 막까지의 기록, 아니면 null
    private final DoNotDisturb dnd = new DoNotDisturb();                        // 방해금지 연동 (단축어 실행)
    // 관람 기록 저장소: ~/.curtain-call/history (로비의 마지막 공연, 관람 기록 화면)
    private final HistoryStore history = new HistoryStore(
        Path.of(System.getProperty("user.home"), ".curtain-call", "history"));
    private CurtainOverlay curtainOverlay;                                      // 커튼 판 (공연 시작, 종료 때 막을 올리고 붙잡음)
    private TheaterWindow theaterWindow;                                        // 로비, 예매 화면을 띄우는 극장 창
    private EndButton endButton;                                                // 막이 내려오는 동안 화면 왼쪽 아래 공연 종료 버튼
    private java.awt.TrayIcon trayIcon;                                         // 메뉴바 아이콘 (남은 시간 툴팁)
    private java.awt.MenuItem endItem;                                          // 메뉴바 '공연 종료' (공연 중에만 켬)
    private volatile boolean quitAfterCurtainCall;                              // 공연 중 '종료'를 눌렀으면 커튼콜 뒤에 앱을 끝냄

    @Override
    public void start(Stage stage) {
        Platform.setImplicitExit(false);    // 극장 창을 숨겨도 앱은 메뉴바에 남게
        stage.setAlwaysOnTop(true);    // 항상 위에오게
        stage.setTitle(OVERLAY_TITLE);
        stage.initStyle(StageStyle.TRANSPARENT);    // 배경 안 칠하게
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();    // 주 모니터의 사용 가능한 화면 크기 저장

        // 커튼을 담을 화면 판과 투명한 Scene을 준비
        StackPane root = new StackPane();
        root.setStyle("-fx-background-color: transparent;");     // 커튼 배경을 투명하게
        Scene scene = new Scene(root, screenBounds.getWidth(), screenBounds.getHeight());    // 실제 화면 크기로 Scene 생성
        scene.setFill(Color.TRANSPARENT);    // Scene 배경을 투명하게 설정
        scene.getStylesheets().add(Main.class.getResource("/css/theater.css").toExternalForm());   // 커튼 안내 문구 모양
        stage.setScene(scene);
        curtainOverlay = new CurtainOverlay(scene.getWidth(), scene.getHeight());    // 현재 화면 크기로 커튼 객체 생성
        root.getChildren().add(curtainOverlay.getView());    // 커튼 판을 root 화면에 추가

        Duration interval = Duration.seconds(1);    // 실행 간격 설정
        KeyFrame keyFrame = new KeyFrame(interval, event -> {    // 1초 간격과 실행할 작업을 받는 KeyFrame 생성 시작
            boolean distracting = detectionState.isDistracting();
            boolean running = show.isRunning();
            if (distracting) {
                detectionState.setDistractionElapsedSeconds(detectionState.getDistractionElapsedSeconds() + 1);    // 경과 시간 증가
            } else {
                detectionState.setDistractionElapsedSeconds(0);    // 집중 상태일 때 경과 시간 초기화
            }
            long elapsed = detectionState.getDistractionElapsedSeconds();
            curtainOverlay.updateCurtain(elapsed, distracting);
            curtainOverlay.showNotice(CurtainOverlay.isFullyClosed(elapsed, distracting));    // 다 닫히면 안내 문구
            double progress = CurtainOverlay.closeProgress(elapsed, distracting);
            long tickNow = System.currentTimeMillis();
            curtainOverlay.updateHud(show.isRunning(), show.isUnlimited(), show.remainingMillis(tickNow),
                show.elapsedMillis(tickNow), progress);    // 남은(무제한이면 지난) 시간, 닫힌 정도 표시
            if (show.isRunning() && progress > 0)
                endButton.show();       // 공연 종료 버튼은 막이 내려오기 시작할 때만 (평소 종료는 메뉴바)
            else
                endButton.hide();

            boolean curtainDown = curtainDownRule.onTick(running, elapsed, distracting);    // 막이 다 닫힌 첫 1초
            if (running) {
                long now = System.currentTimeMillis();
                if (show.isTimeUp(now))
                    endShow(now);       // 공연 길이를 다 채우면 종료 (같은 1초에 막도 다 닫혔다면 끝까지 본 것으로)
                else if (curtainDown)
                    endShow(now, ShowEnding.CURTAIN_DOWN);     // 막이 다 닫히면 그 자리에서 중도 종료
            }
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
        endButton = new EndButton(() -> endShow(System.currentTimeMillis()));    // 길게 누르면 메뉴바 '공연 종료'와 같은 동작

        // 기본 허용 목록: ~/.curtain-call/allowed-apps.txt, allowed-sites.txt (예매할 때 미리 체크되는 목록)
        Path home = Path.of(System.getProperty("user.home"), ".curtain-call");
        AllowList allowList = new AllowList(home.resolve("allowed-apps.txt"));
        SiteAllowList siteAllowList = new SiteAllowList(home.resolve("allowed-sites.txt"));
        // 지난 공연에서 고른 목록 (다음 예매에서 미리 체크)
        BookingView.Lists lists = new BookingView.Lists(allowList, siteAllowList,
            home.resolve("last-show-apps.txt"), home.resolve("last-show-sites.txt"));

        AllowListWindow settingsWindow = new AllowListWindow(allowList, siteAllowList);
        theaterWindow = new TheaterWindow(new TheaterWindow.Actions() {
            public void enter(Ticket t) { startShow(t); }
            public void openSettings() { settingsWindow.show(); }
            public void quit() { quitApp(); }
            public List<ShowRecord> history() { return history.list(); }
            public BookingView.Lists lists() { return lists; }
            public List<String> runningApps() { return runningAppsReader.get(); }
        });
        installQuitMenu(settingsWindow);
        theaterWindow.showLobby();      // 앱을 켜면 로비부터 (감지는 공연 중에만)

        Thread checker = new Thread(() -> {     // 방해금지 연동 단축어가 있는지 한 번 확인 (최대 2초)
            boolean ready = DoNotDisturb.isReady();
            Platform.runLater(() -> {
                theaterWindow.setDndReady(ready);
                if (!show.isRunning())
                    theaterWindow.refreshLobby();     // 로비를 보고 있을 때만 단축어 상태를 새로 그림
            });
        });
        checker.setDaemon(true);
        checker.start();

        // 감지 스레드: 1초마다 맨 앞 앱을 확인해 허용 목록과 비교
        Thread watcher = new Thread(() -> {
            while (true) {  // 프로그램이 꺼질 때 까지 반복
                // 공연 전과 마감이 끝난 뒤에는 osascript를 돌리지 않고 1초 쉼
                if (!show.needsDetection()) {
                    detectionState.setDistracting(false);
                    if (!sleepOneSecond())
                        break;
                    continue;
                }

                String app = frontApp(); // 앱 이름을 한 번만 구해서 재사용 (osascript 두 번 실행 방지)

                // 딴짓인지 판정 결과
                boolean distracting;
                String statsName = app;                             // 통계에 적을 이름, 기본은 앱 이름

                if (BROWSERS.contains(app)) {
                    // 브라우저면 탭 주소에서 호스트만 꺼내, 허용 사이트가 아니면 딴짓
                    String host = SiteAllowList.hostOf(activeTabUrl(app));
                    distracting = SiteAllowList.isDistractingSite(host, ticket.getAllowedSites());          // 이번 공연에서 고른 사이트

                    if (distracting)
                        statsName = SiteAllowList.siteKey(host);    // 딴짓 사이트면 www. 또는 m.을 뗀 사이트 이름으로 적음
                } else {
                    // 브라우저가 아니면 앱 이름으로 판정
                    distracting = isDistracting(app, ticket.getAllowedApps());                              // 이번 공연에서 고른 앱
                }

                boolean shown = show.tick(statsName, distracting, System.currentTimeMillis());              // 공연 중이면 통계에 적고 판정 그대로, 아니면 false
                System.out.println("front: " + app + " / 통계 이름 : " + statsName + " / 딴짓 : " + shown);    // 앱, 통계 이름, 커튼에 알릴 딴짓 여부
                detectionState.setCurrentAppName(app);
                detectionState.setDistracting(shown);
                if (!sleepOneSecond())                                                                     // 1초 대기, 종료 신호면 반복 끝
                    break;
            }
        });
        watcher.setDaemon(true);    // 창 닫으면 이 스레드도 같이 종료되게
        watcher.start();    // 스레드 시작
    }

    // 티켓으로 입장: 공연 시작 + 막 오름 + 방해금지 켜기 (FX 스레드)
    private void startShow(Ticket t) {
        if (pendingCurtainCall != null)
            pendingCurtainCall.stop();      // 직전 공연의 커튼콜 대기가 남아 있으면 버림
        curtainOverlay.releaseHold();
        ticket = t;
        quitAfterCurtainCall = false;
        detectionState.setDistracting(false);
        detectionState.setDistractionElapsedSeconds(0);
        show.start(System.currentTimeMillis(), t.getLength(), t.getTopic());
        curtainOverlay.setTopic(t.getTopic());
        curtainOverlay.raiseCurtain();
        runInBackground(dnd::turnOn);
        setEndEnabled(true);
        setTrayTip("공연 중 · " + t.getTopic() + " · " + lengthText(t.getLength()) + " " + t.getSeat());
    }

    // 공연 종료 요청(정상 종료). 실제 마감은 다음 1초에 감지 스레드가 함
    private void endShow(long nowMillis) {
        endShow(nowMillis, ShowEnding.COMPLETED);
    }

    // 막이 다 닫혀 끝나면 막을 닫은 채 종료 문구를 보여 줌. 어떻게 끝나든 방해금지를 끄고 종료 버튼을 숨김
    private void endShow(long nowMillis, ShowEnding ending) {
        if (!show.isRunning())
            return;
        show.requestEnd(nowMillis, ending);
        if (show.getEnding() == ShowEnding.CURTAIN_DOWN)
            curtainOverlay.holdClosed();
        runInBackground(dnd::turnOff);
        setEndEnabled(false);
        endButton.hide();
        setTrayTip("Curtain Call");
    }

    // 공연 이어보기: 주제, 허용 목록, 좌석은 그대로, 남은 시간만큼 다음 막. 끝나면 같은 기록에 합침
    private void continueShow(ShowRecord record, ShowLength rest) {
        earlierActs = record;
        theaterWindow.hide();
        startShow(ticket.withLength(rest));
    }

    // 종료: 공연 중이면 공연을 먼저 끝내고(방해금지 끄기, 기록) 커튼콜 뒤에 앱을 끝냄
    private void quitApp() {
        if (show.isRunning()) {
            quitAfterCurtainCall = true;
            endShow(System.currentTimeMillis());
            return;
        }
        exitNow();
    }

    private void exitNow() {
        Platform.exit();
        System.exit(0);
    }

    // 오래 걸릴 수 있는 일(단축어 실행)을 화면 스레드 밖에서 돌림
    private static void runInBackground(Runnable work) {
        Thread t = new Thread(work);
        t.setDaemon(true);
        t.start();
    }

    // 메뉴바 '공연 종료'를 공연 중에만 누를 수 있게 (메뉴바 스레드에서 바꿈)
    private void setEndEnabled(boolean enabled) {
        if (endItem != null)
            java.awt.EventQueue.invokeLater(() -> endItem.setEnabled(enabled));
    }

    // 메뉴바 아이콘에 마우스를 올리면 보이는 글
    private void setTrayTip(String text) {
        if (trayIcon != null)
            java.awt.EventQueue.invokeLater(() -> trayIcon.setToolTip(text));
    }

    // 1초 대기, 스레드 종료 신호를 받으면 false
    private static boolean sleepOneSecond() {
        try {
            Thread.sleep(1000);                 // 1초 대기
            return true;
        } catch (InterruptedException e) {      // 스레드 종료 신호를 받으면 루프 탈출
            return false;
        }
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

            Pointer setSel = sel.invokePointer(new Object[]{"setIgnoresMouseEvents:"});     // 클릭 무시 설정과 창 제목 확인에 사용할 메서드 준비
            Pointer titleSel = sel.invokePointer(new Object[]{"title"});
            Pointer utf8Sel = sel.invokePointer(new Object[]{"UTF8String"});
            for (long i = 0; i < count; i++) {
                Pointer w = msg.invokePointer(new Object[]{
                    windows, sel.invokePointer(new Object[]{"objectAtIndex:"}), new NativeLong(i)});
                Pointer nsTitle = msg.invokePointer(new Object[]{w, titleSel});     // 현재 창의 제목을 읽어 Java 문자열로 변환
                if (nsTitle == null) continue;
                Pointer cTitle = msg.invokePointer(new Object[]{nsTitle, utf8Sel});
                String title = (cTitle == null) ? null : cTitle.getString(0);
                if (!OVERLAY_TITLE.equals(title)) continue;     // 커튼 창이 아니면 건너뜀
                msg.invokeVoid(new Object[]{w, setSel, (byte) 1});  // 커튼 창의 클릭을 뒤 화면으로 통과
            }
        } catch (Throwable t) {
            // 클릭 통과 실패해도 앱은 계속 뜨게
            System.err.println("클릭 통과 설정 실패(무시하고 계속): " + t.getMessage());
        }
    }

    // 메뉴바에 아이콘 추가 (커튼에 갇혔을 때 비상구)
    private void installQuitMenu(AllowListWindow settingsWindow) {
        try {
            // 공연 종료, 설정, 종료 메뉴 준비
            java.awt.PopupMenu trayMenu = new java.awt.PopupMenu();
            endItem = new java.awt.MenuItem("공연 종료");
            endItem.setEnabled(false);          // 앱을 켰을 땐 공연 전
            java.awt.MenuItem quitItem = new java.awt.MenuItem("종료");
            java.awt.MenuItem settingsItem = new java.awt.MenuItem("기본 허용 목록…");
            trayMenu.add(endItem);
            trayMenu.addSeparator();
            trayMenu.add(settingsItem);
            trayMenu.add(quitItem);
            settingsItem.addActionListener(e -> Platform.runLater(() -> settingsWindow.show()));    // 메뉴 클릭을 화면 작업으로 넘김
            endItem.addActionListener(e -> Platform.runLater(() -> endShow(System.currentTimeMillis())));
            // "종료"를 누르면 할 일 (공연 중이면 공연을 먼저 끝냄)
            quitItem.addActionListener(e -> Platform.runLater(this::quitApp));

            // 메뉴바 아이콘에 올리기
            java.awt.Image img = java.awt.Toolkit.getDefaultToolkit()
                    .getImage(getClass().getResource("/images/tray.png"));
            trayIcon = new java.awt.TrayIcon(img, "Curtain Call", trayMenu);
            trayIcon.setImageAutoSize(true);
            java.awt.SystemTray.getSystemTray().add(trayIcon);
        } catch (Exception e) {
            System.err.println("종료 메뉴 설치 실패(무시하고 계속): " + e.getMessage());
        }
    }

    // 딴짓 판정 / 모르면 딴짓 아님(커튼에 갇히지 않게)
    static boolean isDistracting(String app, List<String> allowed) {
        if (app == null || app.equals("(unknown)") || app.isBlank()) return false;   // 모르거나 비었을 때 -> 딴짓 아님
        if (app.equals("java")) return false;                       // 자기 자신 -> 딴짓 아님
        return !allowed.contains(app);                              // 목록에 없으면 딴짓
    }

    // 제일 앞에 떠 있는 앱 이름을 알아내는 메서드 / 실패하면 (unknown)을 돌려줌
    private String frontApp() {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "osascript", "-e",
                    "tell application \"System Events\" to get name of first process whose frontmost is true");
            pb.redirectError(ProcessBuilder.Redirect.DISCARD); // 에러 출력은 버려서 오류 문장이 앱 이름으로 섞이지 않게
            Process p = pb.start(); // osascript 실행
            boolean finished = p.waitFor(2, TimeUnit.SECONDS); // 최대 2초만 기다리고 넘으면 멈춘 osascript를 끝냄
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) { // 블록이 끝나면 자동으로 스트림 닫힘
                if (!finished) {
                    p.destroyForcibly(); // 2초 넘게 멈춘 osascript를 강제로 끝냄
                    return "(unknown)";
                }
                String name = r.readLine(); // 실행 결과에서 앱 이름 한 줄 읽어오기
                int code = p.exitValue(); // 끝난 명령의 종료코드(0=성공)를 꺼냄
                if (code != 0 || name == null)
                    return "(unknown)";
                return name;
            }
        } catch (InterruptedException e) { // 인터럽트 상태 복원
            Thread.currentThread().interrupt();
            return "(unknown)";
        } catch (Exception e) { // 에러 원인 콘솔에 출력
            System.err.println("frontApp 실패 : " + e.getMessage());
            return "(unknown)";
        }
    }

    // 브라우저의 지금 탭 주소를 알아내는 메서드 / 실패하거나 2초 넘게 걸리면 null
    private String activeTabUrl(String browser) {
        String script = browser.equals("Safari")                                // BROWSERS에 있는 두 브라우저만 들어옴
            ? "tell application \"Safari\" to get URL of front document"
            : "tell application \"Google Chrome\" to get URL of active tab of front window";

        try {
            ProcessBuilder pb = new ProcessBuilder("osascript", "-e", script);
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);                  // 에러 출력은 버려서 오류 문장이 주소로 섞이지 않게
            Process p = pb.start();                                             // osascript 실행
            boolean finished = p.waitFor(2, TimeUnit.SECONDS);                  // 최대 2초만 기다림

            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()))) {    // 블록이 끝나면 자동으로 스트림 닫힘
                if (!finished) {
                    p.destroyForcibly();                                        // 2초 넘게 멈춘 osascript를 강제로 끝냄
                    return null;
                }
                String url = r.readLine();                                      // 실행 결과에서 주소 한 줄 읽어오기
                if (p.exitValue() != 0)                                         // 종료코드가 0이 아니면 실패
                    return null;
                return url;
            }
        } catch (InterruptedException e) {                                      // 인터럽트 상태 복원
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {                                                 // 에러 원인 콘솔에 출력
            System.err.println("activeTabUrl 실패 : " + e.getMessage());
            return null;
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

    // 공연 길이 글: 무제한, 이어보기처럼 초가 남은 길이는 초까지, 나머지는 시간과 분
    private static String lengthText(ShowLength length) {
        if (length.isUnlimited())
            return "무제한";
        int h = length.getMinutes() / 60, m = length.getMinutes() % 60;
        if (h == 0)
            return m + "분";
        return m == 0 ? h + "시간" : h + "시간 " + m + "분";
    }
}
