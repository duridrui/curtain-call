package focus;

import javafx.scene.shape.Rectangle;
import javafx.scene.paint.Color;
import javafx.scene.image.Image;
import javafx.scene.paint.ImagePattern;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.animation.TranslateTransition;
import javafx.animation.Interpolator;
import javafx.util.Duration;
import javafx.application.Platform;
import javafx.scene.shape.LineTo;
import javafx.scene.shape.MoveTo;
import javafx.scene.shape.Path;
import javafx.scene.shape.QuadCurveTo;
import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.VBox;

public class CurtainOverlay {
    private static final double CLOSE_DURATION_SECONDS = 30.0;      // 커튼 닫히는 시간 30초 설정
    static final String NOTICE_TITLE = "막이 내렸습니다";              // 커튼이 다 닫혔을 때 보이는 안내 문구
    static final String NOTICE_BODY = "허용한 무대로 돌아오면 막이 다시 오릅니다. 다 닫히면 공연이 끝납니다.";
    static final String ENDED_TITLE = "막이 내려 공연이 끝났습니다";   // 막이 다 닫혀 중도 종료됐을 때 커튼콜 전까지 보이는 문구
    static final String ENDED_BODY = "커튼콜로 이동합니다.";
    private static final double RAISE_SECONDS = 3.0;                // 막이 오르는 연출 시간
    private static final double APPEAR_SECONDS = 0.3;               // 입장 때 닫힌 막이 나타나는 시간 (한 번에 튀어나오지 않게)
    private static final double OPEN_SECONDS = 1.2;                 // 딴짓에서 돌아왔을 때 막이 다시 열리는 시간
    private static final double CORNER_RADIUS = 90;    // 막 아래 안쪽 모서리 둥글기
    private Rectangle leftCurtain = new Rectangle();    // 왼쪽 커튼 객체를 저장
    private Rectangle rightCurtain = new Rectangle();    // 오른쪽 커튼 객체를 저장
    private StackPane view = new StackPane();    // 커튼 두 장을 담을 화면 판을 저장
    private TranslateTransition leftTransition =
            new TranslateTransition(Duration.seconds(1), leftCurtain);    // 왼쪽 커튼을 1초 동안 이동시키는 장치
    private TranslateTransition rightTransition =
            new TranslateTransition(Duration.seconds(1), rightCurtain);    // 오른쪽 커튼을 1초 동안 이동시키는 장치
    private long raiseEndsAtNanos; // 막 오름 연출이 끝나는 시각
    private boolean held;                                     // 중도 종료 뒤 커튼콜 전까지 막을 닫힌 채로 붙잡아 둠
    private double lastTarget = Double.NaN;                   // 마지막으로 보낸 커튼 목표 위치 (같으면 다시 걸지 않음)
    private VBox notice = new VBox(10);    // 커튼 가운데 안내 문구 판
    private FadeTransition noticeFade = new FadeTransition(Duration.seconds(0.4), notice);
    private boolean noticeShown;
    private final Label noticeTitle = new Label(NOTICE_TITLE);
    private static final double HUD_MARGIN = 56;       // HUD 화면 가장자리 여백
    private static final double HUD_LEFT = 124;        // 왼쪽 HUD는 공연 종료 버튼 오른쪽에
    private final Label remainingNumber = new Label("0");     // HUD 남은 시간(분)
    private final Label closedNumber = new Label("0");        // HUD 막이 닫힌 정도(%)
    private final Label topicLabel = new Label("");           // HUD 집중 주제 (남은 시간 위)
    private final VBox leftHud;
    private final VBox rightHud;
    private final Label leftTitle = new Label("남은 시간");    // HUD 왼쪽 제목 (무제한 공연이면 지난 시간)
    private final Label noticeBody = new Label(NOTICE_BODY);

    public CurtainOverlay(double screenWidth, double screenHeight) {
        leftCurtain.setWidth(screenWidth / 2);    // 왼쪽 커튼 가로 길이 설정
        leftCurtain.setHeight(screenHeight);    // 왼쪽 커튼 세로 길이 설정
        rightCurtain.setWidth(screenWidth / 2);    // 오른쪽 커튼 가로 길이 설정
        rightCurtain.setHeight(screenHeight);    // 오른쪽 커튼 세로 길이 설정
        leftTransition.setInterpolator(Interpolator.LINEAR);
        rightTransition.setInterpolator(Interpolator.LINEAR);       // 양쪽 커튼 일정한 속도로 설정

        java.net.URL imageUrl = CurtainOverlay.class.getResource("/images/curtain.jpg");    // 커튼 이미지 주소 찾기
        Color curtainColor = Color.rgb(0, 0, 0, 0.8);    // 커튼 이미지 로딩 실패 시 검은색 커튼 불투명도 80%
        leftCurtain.setFill(curtainColor);
        rightCurtain.setFill(curtainColor);     // 양쪽 커튼에 기본 검은색을 먼저 적용
        if (imageUrl != null) {
            Image curtainImage = new Image(imageUrl.toExternalForm());      // 이미지 주소를 실제 사진 객체로 읽음
            if (!curtainImage.isError()) {
                leftCurtain.setFill(new ImagePattern(curtainImage, 0, 0, 2, 1.18, true));
                rightCurtain.setFill(new ImagePattern(curtainImage, -1, 0, 2, 1.18, true));    // 사진 한 장을 절반으로 나눠 적용, 높이 118%로 아래 바닥 줄무늬는 잘라냄
            }
        }

        leftCurtain.setEffect(new InnerShadow(34, -26, 0, Color.rgb(0, 0, 0, 0.42)));     // 안쪽 가장자리 그림자
        rightCurtain.setEffect(new InnerShadow(34, 26, 0, Color.rgb(0, 0, 0, 0.42)));
        bake(leftCurtain);      // 사진 채우기와 그림자를 그림 한 장으로 미리 그려 두고, 움직일 때는 그 그림만 옮김
        bake(rightCurtain);
        leftCurtain.translateXProperty().addListener((obs, was, now) -> roundInnerCorner(leftCurtain, true));
        rightCurtain.translateXProperty().addListener((obs, was, now) -> roundInnerCorner(rightCurtain, false));

        StackPane.setAlignment(leftCurtain, Pos.CENTER_LEFT);    // 커튼을 각 화면 끝에 정렬
        StackPane.setAlignment(rightCurtain, Pos.CENTER_RIGHT);
        view.getChildren().addAll(leftCurtain, rightCurtain);    // 두 커튼을 view 안에 추가

        noticeTitle.getStyleClass().add("notice-title");
        noticeBody.getStyleClass().add("notice-body");
        notice.getChildren().addAll(noticeTitle, noticeBody);
        notice.getStyleClass().add("curtain-notice");
        notice.setAlignment(Pos.CENTER);
        notice.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);    // 글자 크기만큼만 차지
        notice.setOpacity(0);
        noticeFade.setInterpolator(Motion.EASE_OUT);
        view.getChildren().add(notice);    // 커튼 위에 안내 문구

        // HUD: 왼쪽 아래 남은 시간, 오른쪽 아래 막이 닫힌 정도 (딴짓 중에만 보임)
        leftHud = hud("남은 시간", remainingNumber, "분", Pos.BOTTOM_LEFT);
        topicLabel.getStyleClass().add("hud-topic");
        leftHud.getChildren().add(0, topicLabel);
        rightHud = hud("막이 닫힌 정도", closedNumber, "%", Pos.BOTTOM_RIGHT);
        StackPane.setMargin(leftHud, new Insets(0, 0, HUD_MARGIN, HUD_LEFT));
        StackPane.setMargin(rightHud, new Insets(0, HUD_MARGIN, HUD_MARGIN, 0));
        view.getChildren().addAll(leftHud, rightHud);

        leftCurtain.setTranslateX(-(screenWidth / 2));    // 커튼을 화면 바깥으로 이동해 열린 상태로
        rightCurtain.setTranslateX(screenWidth / 2);
    }

    public StackPane getView() {    // Main이 커튼 판을 가져갈 수 있게 반환
        return view;
    }

    // 닫힌 정도(0.0~1.0): 딴짓 경과 초를 닫힘 시간으로 나눈 값, 집중 중이면 0
    static double closeProgress(long elapsedSeconds, boolean isDistracting) {
        if (isDistracting) {
            return Math.min(1.0, elapsedSeconds / CLOSE_DURATION_SECONDS);
        }
        return 0.0;
    }

    // 커튼이 다 닫혔는지 (안내 문구를 띄울 때)
    static boolean isFullyClosed(long elapsedSeconds, boolean isDistracting) {
        return closeProgress(elapsedSeconds, isDistracting) >= 1.0;
    }

    private VBox hud(String title, Label number, String unit, Pos corner) {
        Label titleLabel = corner == Pos.BOTTOM_LEFT ? leftTitle : new Label(title);
        titleLabel.setText(title);
        titleLabel.getStyleClass().add("hud-label");
        number.getStyleClass().add("hud-num");
        Label unitLabel = new Label(unit);
        unitLabel.getStyleClass().add("hud-unit");
        HBox line = new HBox(4, number, unitLabel);
        line.setAlignment(Pos.BASELINE_LEFT);
        VBox box = new VBox(0, titleLabel, line);
        box.getStyleClass().add("hud");
        box.setAlignment(corner == Pos.BOTTOM_LEFT ? Pos.BOTTOM_LEFT : Pos.BOTTOM_RIGHT);
        box.setMaxSize(VBox.USE_PREF_SIZE, VBox.USE_PREF_SIZE);
        box.setOpacity(0);
        StackPane.setAlignment(box, corner);
        return box;
    }

    // 공연 시작 때 HUD에 집중 주제를 적음
    public void setTopic(String topic) {
        topicLabel.setText(topic == null ? "" : "집중 주제 · " + topic);
    }

    // HUD 갱신 (FX 스레드에서 부름). 남은 시간은 막이 내려오기 시작하면, 닫힌 정도는 다 닫히기 전까지만 보임
    public void updateHud(boolean running, boolean unlimited, long remainingMillis, long elapsedMillis, double closeProgress) {
        String[] time = hudTime(unlimited, remainingMillis, elapsedMillis);
        leftTitle.setText(time[0]);
        remainingNumber.setText(time[1]);
        closedNumber.setText(closedPercent(closeProgress));
        fadeHud(leftHud, running && closeProgress > 0);
        fadeHud(rightHud, running && closeProgress > 0 && closeProgress < 1);
    }

    private static void fadeHud(VBox box, boolean show) {
        if (Boolean.valueOf(show).equals(box.getUserData()))
            return;     // 이미 그 상태로 바뀌는 중
        box.setUserData(show);
        Motion.fadeTo(box, show ? 1 : 0, Motion.HOVER);
    }
    // 막 아래 안쪽 모서리를 둥글게. 다 닫힌 쪽으로 갈수록 반지름이 줄어 가운데에서 빈틈 없이 맞닿음.
    // 반지름이 바뀔 때만(가운데 90px 안쪽) 모양을 새로 만든다
    private static void roundInnerCorner(Rectangle curtain, boolean left) {
        double w = curtain.getWidth();
        double h = curtain.getHeight();
        double r = Math.round(Math.min(CORNER_RADIUS, Math.abs(curtain.getTranslateX())));
        if (Double.valueOf(r).equals(curtain.getProperties().get("cornerRadius")))
            return;
        curtain.getProperties().put("cornerRadius", r);
        Path clip = new Path();
        if (left) {
            clip.getElements().addAll(new MoveTo(0, 0), new LineTo(w, 0), new LineTo(w, h - r),
                    new QuadCurveTo(w, h, w - r, h), new LineTo(0, h));
        } else {
            clip.getElements().addAll(new MoveTo(0, 0), new LineTo(w, 0), new LineTo(w, h), new LineTo(r, h),
                    new QuadCurveTo(0, h, 0, h - r));
        }
        clip.setFill(Color.BLACK);
        curtain.setClip(clip);
    }

    public void updateCurtain(long elapsedSeconds, boolean isDistracting) {
        double closeProgress = closeProgress(elapsedSeconds, isDistracting);    // 닫힌 정도를 소수로 저장
        Platform.runLater(() -> {    // 화면 변경 작업을 FX 스레드에서 실행
            if (held)
                return;    // 중도 종료 뒤 커튼콜 전까지는 막을 닫힌 채로 둠
            if (closeProgress == 0.0 && System.nanoTime() < raiseEndsAtNanos)
                return;    // 막이 오르는 중이면 같은 방향 이동을 겹쳐 걸지 않음
            double curtainOffset = leftCurtain.getWidth() * (1.0 - closeProgress);    // 닫히지 않은 비율을 실제 커튼 이동 거리로 변환
            if (curtainOffset == lastTarget)
                return;    // 같은 자리로 가는 중이면 다시 시작하지 않음 (1초마다 다시 걸면 움직임이 끊김)
            lastTarget = curtainOffset;
            boolean opening = closeProgress == 0.0;     // 다시 열 때는 부드러운 곡선, 닫힐 때는 1초마다 일정한 속도
            Duration time = opening ? Duration.seconds(OPEN_SECONDS) : Duration.seconds(1);
            Interpolator curve = opening ? Motion.EASE_IN_OUT : Interpolator.LINEAR;
            leftTransition.setDuration(time);
            rightTransition.setDuration(time);
            leftTransition.setInterpolator(curve);
            rightTransition.setInterpolator(curve);
            leftTransition.setToX(-curtainOffset);
            rightTransition.setToX(curtainOffset);
            leftTransition.playFromStart();
            rightTransition.playFromStart();
        });
    }

    // 커튼이 다 닫혔을 때만 안내 문구를 보임 (FX 스레드에서 부름)
    public void showNotice(boolean show) {
        if (held)
            return;     // 중도 종료 문구를 커튼콜 전까지 유지
        if (show == noticeShown)
            return;
        noticeShown = show;
        noticeFade.stop();
        noticeFade.setToValue(show ? 1.0 : 0.0);
        noticeFade.playFromStart();
    }

    // 막이 다 닫혀 공연이 끝났을 때: 막을 닫힌 채로 붙잡고 안내 문구를 종료 문구로 바꿈 (FX 스레드에서 부름)
    public void holdClosed() {
        leftTransition.stop();
        rightTransition.stop();
        lastTarget = 0;
        leftTransition.setDuration(Duration.seconds(1));      // 마지막 1초도 끊지 않고 지금 자리에서 닫힌 자리까지 이어서 닫음
        rightTransition.setDuration(Duration.seconds(1));
        leftTransition.setInterpolator(Interpolator.LINEAR);
        rightTransition.setInterpolator(Interpolator.LINEAR);
        leftTransition.setToX(0);
        rightTransition.setToX(0);
        leftTransition.playFromStart();
        rightTransition.playFromStart();
        noticeTitle.setText(ENDED_TITLE);
        noticeBody.setText(ENDED_BODY);
        showNotice(true);
        held = true;
    }

    // 커튼콜이 뜨면 붙잡은 막을 풀고 문구를 원래대로 돌린 뒤 막을 올림 (FX 스레드에서 부름)
    public void releaseHold() {
        if (!held)
            return;
        held = false;
        showNotice(false);
        noticeFade.setOnFinished(e -> {
            noticeTitle.setText(NOTICE_TITLE);
            noticeBody.setText(NOTICE_BODY);
            noticeFade.setOnFinished(null);
        });
        raiseCurtain();
    }

    boolean isHeld() {
        return held;
    }

    // 막 오름 연출 (공연 시작, 이어보기, 중도 종료 뒤 커튼콜에 씀, FX 스레드에서 부름)
    // 막이 열려 있었으면 닫힌 자리에서 0.3초 동안 나타난 뒤, 3초 동안 천천히 시작해 천천히 멈추며 양쪽으로 열림
    public void raiseCurtain() {
        leftTransition.stop();
        rightTransition.stop();
        boolean wasClosed = Math.abs(leftCurtain.getTranslateX()) < 1;
        double appear = wasClosed ? 0 : APPEAR_SECONDS;
        raiseEndsAtNanos = System.nanoTime() + (long) ((appear + RAISE_SECONDS) * 1_000_000_000L);
        lastTarget = leftCurtain.getWidth();
        leftCurtain.setTranslateX(0);
        rightCurtain.setTranslateX(0);
        TranslateTransition left = new TranslateTransition(Duration.seconds(RAISE_SECONDS), leftCurtain);
        TranslateTransition right = new TranslateTransition(Duration.seconds(RAISE_SECONDS), rightCurtain);
        left.setInterpolator(Motion.EASE_IN_OUT);      // 천천히 시작해 천천히 멈춤
        right.setInterpolator(Motion.EASE_IN_OUT);
        left.setToX(-leftCurtain.getWidth());
        right.setToX(rightCurtain.getWidth());
        javafx.animation.ParallelTransition open = new javafx.animation.ParallelTransition(left, right);
        if (wasClosed) {
            open.play();
            return;
        }
        leftCurtain.setOpacity(0);
        rightCurtain.setOpacity(0);
        FadeTransition showLeft = new FadeTransition(Duration.seconds(APPEAR_SECONDS), leftCurtain);
        FadeTransition showRight = new FadeTransition(Duration.seconds(APPEAR_SECONDS), rightCurtain);
        showLeft.setToValue(1);
        showRight.setToValue(1);
        new javafx.animation.SequentialTransition(new javafx.animation.ParallelTransition(showLeft, showRight), open).play();
    }

    // 커튼 한 장을 지금 모양(사진 채우기 + 안쪽 그림자) 그대로 그림을 굳힘
    // 큰 사진 채우기와 그림자를 매 프레임 다시 그리면 막이 움직일 때 프레임이 밀려 끊겨 보이므로 한 번만 그림
    private static void bake(Rectangle curtain) {
        javafx.scene.SnapshotParameters params = new javafx.scene.SnapshotParameters();
        params.setFill(Color.TRANSPARENT);
        javafx.scene.image.WritableImage baked = curtain.snapshot(params, null);
        curtain.setEffect(null);
        curtain.setFill(new ImagePattern(baked));
        curtain.setCache(true);
        curtain.setCacheHint(javafx.scene.CacheHint.SPEED);
    }

    // 커튼 HUD 남은 시간: 분 단위 올림 (5초 남아도 "1")
    private static String remainingMinutes(long millis) {
        if (millis <= 0)
            return "0";
        return String.valueOf((millis + 59_999) / 60_000);
    }

    // 커튼 HUD 시간 {제목, 숫자(분)}: 길이가 있으면 남은 시간(올림), 무제한이면 지난 시간(버림)
    private static String[] hudTime(boolean unlimited, long remainingMillis, long elapsedMillis) {
        if (unlimited)
            return new String[] {"지난 시간", String.valueOf(Math.max(0, elapsedMillis) / 60_000)};
        return new String[] {"남은 시간", remainingMinutes(remainingMillis)};
    }

    // 커튼 HUD 막이 닫힌 정도: 0~100 정수
    private static String closedPercent(double progress) {
        return String.valueOf(Math.round(Math.max(0, Math.min(1, progress)) * 100));
    }
}
