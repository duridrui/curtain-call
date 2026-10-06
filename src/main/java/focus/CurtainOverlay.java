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
import javafx.animation.AnimationTimer;
import javafx.animation.FadeTransition;
import javafx.scene.control.Label;
import javafx.scene.effect.InnerShadow;
import javafx.scene.layout.VBox;
import javafx.animation.AnimationTimer;
import javafx.scene.image.ImageView;

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

    // Blender 프레임 커튼. frames가 null이면 사진 커튼만 쓰고 아래 필드는 쓰지 않는다.
    // 사진 커튼의 두 Rectangle은 숨긴 채 그대로 움직이고(닫힘, 다시 열림, 붙잡기 규칙을 그대로 씀), 그 위치를 닫힘 장 번호로 바꿔 보인다
    private final BlenderCurtain frames;
    private final boolean blend;                // 닫힘 장 사이 두 장 섞기 (저화질)
    private final ImageView leftFrame = new ImageView();
    private final ImageView rightFrame = new ImageView();
    private javafx.scene.image.WritableImage leftPixels, rightPixels;     // 장마다 픽셀만 바꿔 쓰는 그림 두 장
    private javafx.scene.image.PixelBuffer<java.nio.IntBuffer> leftBuffer, rightBuffer;   // 위 그림의 픽셀 (풀린 장과 같은 형식)
    private int[] reopenPlan;                   // 다시 열림 동안 1/30초마다 보일 닫힘 장 (시작할 때 정함, 아니면 null)
    private long reopenStart;
    private int frameWidth, frameHeight;
    private FrameSides closedSides, openSides;  // 닫힘 / 막 오름 장 (좌우 반쪽씩 디코드 창)
    private int targetFrame = -1;               // 보이려는 장 번호 (지금 재생 중인 쪽: 닫힘 또는 막 오름)
    private int shownFrame = -1;                // 화면에 보이는 장 번호
    private int closedStep = 1;                 // 닫힘 장이 움직이는 방향 (미리 디코드할 쪽)
    private boolean raisingFrames;              // 막 오름 장을 재생하는 중 (닫힘 장 갱신을 멈춤)
    private AnimationTimer raiseTimer;

    public CurtainOverlay(double screenWidth, double screenHeight) {
        this(screenWidth, screenHeight, appCurtain());
    }

    // 이번 실행에서 쓸 커튼: 개발용 폴더(-Dcurtain.frames)가 있으면 그것, 아니면 고른 화질 등급(받아 둔 팩이 온전치 않으면 저화질)
    record Choice(BlenderCurtain frames, boolean blend, String label) {
    }

    static Choice appCurtain() {
        if (BlenderCurtain.ROOT != null)
            return new Choice(BlenderCurtain.load(BlenderCurtain.ROOT), Boolean.getBoolean("curtain.blend"), "개발용 폴더 " + BlenderCurtain.ROOT);
        CurtainPacks.Active active = CurtainPacks.forApp().active();
        return new Choice(BlenderCurtain.load(active.root()), active.quality().blend,
            active.quality().label + (active.note().isEmpty() ? "" : " (" + active.note() + ")"));
    }

    private CurtainOverlay(double screenWidth, double screenHeight, Choice choice) {
        this(screenWidth, screenHeight, choice.frames(), choice.blend(), choice.label());
    }

    // frames: Blender 프레임 (null이면 사진 커튼), blend: 닫힘 장 사이를 두 장 섞어 보임 (장 수를 줄인 저화질), label: 시작 로그에 남길 커튼 이름
    CurtainOverlay(double screenWidth, double screenHeight, BlenderCurtain frames, boolean blend, String label) {
        long setUpStart = System.nanoTime();
        this.frames = frames;
        this.blend = blend;
        leftCurtain.setWidth(screenWidth / 2);    // 왼쪽 커튼 가로 길이 설정
        leftCurtain.setHeight(screenHeight);    // 왼쪽 커튼 세로 길이 설정
        rightCurtain.setWidth(screenWidth / 2);    // 오른쪽 커튼 가로 길이 설정
        rightCurtain.setHeight(screenHeight);    // 오른쪽 커튼 세로 길이 설정
        leftTransition.setInterpolator(Interpolator.LINEAR);
        rightTransition.setInterpolator(Interpolator.LINEAR);       // 양쪽 커튼 일정한 속도로 설정

        java.net.URL imageUrl = frames != null ? null    // Blender 커튼이면 사진을 읽지도 굽지도 않음
            : CurtainOverlay.class.getResource("/images/curtain.jpg");    // 커튼 이미지 주소 찾기
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
        if (frames == null) {
            bake(leftCurtain);      // 사진 채우기와 그림자를 그림 한 장으로 미리 그려 두고, 움직일 때는 그 그림만 옮김
            bake(rightCurtain);
            leftCurtain.translateXProperty().addListener((obs, was, now) -> roundInnerCorner(leftCurtain, true));
            rightCurtain.translateXProperty().addListener((obs, was, now) -> roundInnerCorner(rightCurtain, false));
        }
        StackPane.setAlignment(leftCurtain, Pos.CENTER_LEFT);    // 커튼을 각 화면 끝에 정렬
        StackPane.setAlignment(rightCurtain, Pos.CENTER_RIGHT);
        view.getChildren().addAll(leftCurtain, rightCurtain);    // 두 커튼을 view 안에 추가
        if (frames != null)
            setUpFrames(screenWidth / 2, screenHeight);
        System.out.println(frames != null
            ? "커튼: " + label + " (닫힘 " + frames.left().count() + "장, 막 오름 " + frames.openLeft().count() + "장"
                + (blend ? ", 두 장 섞기" : "") + ", 준비 " + (System.nanoTime() - setUpStart) / 1_000_000 + "ms, 장 바이트는 백그라운드로 올림)"
            : "커튼: 사진 커튼 (Blender 프레임을 못 찾았거나 끔: " + label + ")");

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

    // Blender 프레임 커튼
    // 디스크: 앱 시작 때 압축 PNG 바이트를 백그라운드로 메모리에 올림 (FrameBytes).
    // 디코드: 그릴 장보다 AHEAD장 앞서 다른 스레드에서 픽셀 배열로 풀어 둠 (FrameWindow, PngDecoder). 풀린 장은 몇십 장만 들고 있고,
    // 놓은 배열은 pixelPool로 돌려 다음 디코드에 다시 씀 (버리는 큰 배열이 없어야 GC 멈춤이 없음).
    // 그리기: 그림(WritableImage) 두 장의 픽셀만 바꿔 씀. 장마다 새 Image를 끼우면 그래픽 메모리를 새로 잡다가 전체 GC로 멈춘다.
    // 그림은 풀린 장과 같은 형식(PixelBuffer, premultiplied ARGB)이라 바꿀 때 형식 변환 없이 복사만 함
    // 다시 열림(1.2초에 수백 장): 시작할 때 1/30초 일정을 정해 그 장들을 미리 풀고 일정대로 보임
    // 고정: 닫힘 시작 장, 다 닫힌 장, 막 오름 시작 장은 늘 풀어 두어 막 오름이 첫 장 대기 없이 시작함

    private static final int AHEAD = 12;        // 앞서 풀어 둘 장 수 (30fps로 0.4초, 다른 일로 CPU가 바쁠 때 버틸 여유)
    private static final int SPARE = 4;         // 지난 장을 남겨 둘 수 (빨리 움직일 때 가까운 장으로 대신 보임)
    private static final int POOL_LIMIT = 24;   // 다시 쓸 픽셀 배열을 모아 둘 수 (한 장 960x1080 약 4MB)
    private final java.util.concurrent.ConcurrentLinkedQueue<int[]> pixelPool = new java.util.concurrent.ConcurrentLinkedQueue<>();
    private static final java.util.concurrent.ExecutorService DECODER =
        java.util.concurrent.Executors.newFixedThreadPool(4, r -> {
            Thread t = new Thread(r, "curtain-decode");
            t.setDaemon(true);
            return t;
        });

    // 왼쪽, 오른쪽 반쪽 장을 같은 번호로 함께 다룸
    private final class FrameSides {
        final FrameBytes leftBytes, rightBytes;
        final FrameWindow<int[]> left, right;

        FrameSides(CurtainFrames l, CurtainFrames r, int width, int height) {
            leftBytes = new FrameBytes(paths(l));
            rightBytes = new FrameBytes(paths(r));
            left = new FrameWindow<>(l.count(), AHEAD, SPARE, i -> PngDecoder.decode(leftBytes.stream(i), width, height, pixelPool.poll()),
                DECODER, i -> decoded(), this::recycle);
            right = new FrameWindow<>(r.count(), AHEAD, SPARE, i -> PngDecoder.decode(rightBytes.stream(i), width, height, pixelPool.poll()),
                DECODER, i -> decoded(), this::recycle);
        }

        private void recycle(int[] pixels) {
            if (pixelPool.size() < POOL_LIMIT)
                pixelPool.offer(pixels);
        }

        void pin(int... indices) {
            left.pin(indices);
            right.pin(indices);
        }

        void aim(int index, int step) {
            left.aim(index, step);
            right.aim(index, step);
        }

        void aimList(java.util.List<Integer> want) {
            left.aimList(want);
            right.aimList(want);
        }

        boolean ready(int i) {
            return left.get(i) != null && right.get(i) != null;
        }

        // target에서 toward 쪽으로 가며 두 반쪽이 다 풀린 첫 장, 없으면 -1
        int nearestReady(int target, int toward) {
            int dir = toward >= target ? 1 : -1;
            for (int i = target; i != toward + dir; i += dir)
                if (ready(i))
                    return i;
            return -1;
        }

        void release() {
            left.release();
            right.release();
        }

        void loadAllBytes() {
            leftBytes.loadAll();
            rightBytes.loadAll();
        }

        long loadedBytes() {
            return leftBytes.loadedBytes() + rightBytes.loadedBytes();
        }
    }

    private static java.util.List<java.nio.file.Path> paths(CurtainFrames frames) {
        java.util.List<java.nio.file.Path> out = new java.util.ArrayList<>();
        for (int i = 0; i < frames.count(); i++)
            out.add(frames.path(i));
        return out;
    }

    // 반쪽 그림 두 장을 각 화면 끝에 두고, 장 번호에 맞는 그림을 끼워 보임
    private void setUpFrames(double halfWidth, double height) {
        leftCurtain.setVisible(false);      // 위치만 쓰고 그리지는 않음
        rightCurtain.setVisible(false);
        for (ImageView v : new ImageView[] {leftFrame, rightFrame}) {
            v.setFitWidth(halfWidth);
            v.setFitHeight(height);
            v.setVisible(false);
            v.setMouseTransparent(true);
        }
        StackPane.setAlignment(leftFrame, Pos.CENTER_LEFT);
        StackPane.setAlignment(rightFrame, Pos.CENTER_RIGHT);
        view.getChildren().addAll(leftFrame, rightFrame);
        java.util.List<Integer> size = PngDecoder.size(new FrameBytes(java.util.List.of(frames.left().path(0))).stream(0));
        frameWidth = size.get(0);       // 렌더 크기(960x1080) 그대로 풀고, 창 반쪽 크기로 늘리는 건 ImageView(그래픽 카드)가 함
        frameHeight = size.get(1);
        leftBuffer = pixelBuffer(frameWidth, frameHeight);
        rightBuffer = pixelBuffer(frameWidth, frameHeight);
        leftPixels = new javafx.scene.image.WritableImage(leftBuffer);
        rightPixels = new javafx.scene.image.WritableImage(rightBuffer);
        leftFrame.setImage(leftPixels);
        rightFrame.setImage(rightPixels);
        closedSides = new FrameSides(frames.left(), frames.right(), frameWidth, frameHeight);
        openSides = new FrameSides(frames.openLeft(), frames.openRight(), frameWidth, frameHeight);
        int[] raiseStart = new int[AHEAD + 1];      // 막 오름 시작 13장: 앞서 풀기가 따라붙을 때까지 늘 풀어 둠
        for (int i = 0; i <= AHEAD; i++)
            raiseStart[i] = i;
        openSides.pin(raiseStart);
        closedSides.pin(0, 1, 2, 3);                                                // 닫힘 시작 (뒤는 창이 따라잡음)
        closedSides.pin(CurtainFrames.closedIndex(1, frames.left().count()));     // 다 닫혀 머무는 장
        preloadBytes();
        leftCurtain.translateXProperty().addListener((obs, was, now) -> showClosedFrame());
    }

    private static javafx.scene.image.PixelBuffer<java.nio.IntBuffer> pixelBuffer(int width, int height) {
        java.nio.IntBuffer pixels = java.nio.ByteBuffer.allocateDirect(width * height * 4)
            .order(java.nio.ByteOrder.nativeOrder()).asIntBuffer();
        return new javafx.scene.image.PixelBuffer<>(width, height, pixels, javafx.scene.image.PixelFormat.getIntArgbPreInstance());
    }

    // 그림 한 장의 픽셀을 풀린 장으로 바꿈 (FX 스레드)
    private static void copy(javafx.scene.image.PixelBuffer<java.nio.IntBuffer> target, int[] pixels) {
        target.updateBuffer(b -> {
            java.nio.IntBuffer buffer = b.getBuffer();
            buffer.clear();
            buffer.put(pixels);
            buffer.rewind();    // 그래픽 카드로 올릴 때 처음부터 읽음 (안 되돌리면 "0 elements remain"으로 올리기 실패)
            return null;        // 그림 전체가 바뀜
        });
    }

    // 압축 PNG 바이트를 백그라운드로 다 올림 (막 오름 먼저). 힙이 모자라면 올리지 않고 장마다 파일에서 읽음(디코드는 그대로 앞서 함)
    private void preloadBytes() {
        long need = 0;
        for (CurtainFrames f : new CurtainFrames[] {frames.left(), frames.right(), frames.openLeft(), frames.openRight()})
            for (int i = 0; i < f.count(); i++)
                need += f.path(i).toFile().length();
        long headroom = 512L << 20;
        if (Runtime.getRuntime().maxMemory() < need + headroom) {
            System.out.printf("커튼: 힙 %dMB가 장 바이트 %dMB + 여유 512MB보다 작아 미리 올리지 않음 (장마다 파일에서 읽음)%n",
                Runtime.getRuntime().maxMemory() >> 20, need >> 20);
            return;
        }
        Thread t = new Thread(() -> {
            long t0 = System.nanoTime();
            openSides.loadAllBytes();
            closedSides.loadAllBytes();
            System.out.printf("커튼: 장 바이트 %dMB 올림 (%.1f초)%n",
                (openSides.loadedBytes() + closedSides.loadedBytes()) >> 20, (System.nanoTime() - t0) / 1e9);
        }, "curtain-bytes");
        t.setDaemon(true);
        t.setPriority(Thread.MIN_PRIORITY);
        t.start();
    }

    // 디코드 스레드에서 한 장이 풀림: 보이려던 장이 늦게 풀렸으면 그때 보임
    private void decoded() {
        Platform.runLater(() -> {
            if (targetFrame >= 0 && shownFrame != targetFrame)
                show(raisingFrames ? openSides : closedSides, targetFrame);
        });
    }

    // 숨긴 왼쪽 Rectangle 위치(-폭 = 열림, 0 = 닫힘)를 닫힘 장으로 바꿔 보임
    private void showClosedFrame() {
        if (raisingFrames)
            return;
        int count = frames.left().count();
        if (reopenPlan != null && showReopenFrame())
            return;
        double progress = 1 + leftCurtain.getTranslateX() / leftCurtain.getWidth();
        int index = CurtainFrames.closedIndex(progress, count);
        if (index == 0) {       // 다 열림: 그리지 않고 지난 장도 놓음 (0번 장은 가장자리 9px뿐)
            hideFrames();
            closedSides.release();
            return;
        }
        if (targetFrame >= 0 && index != targetFrame)
            closedStep = index - targetFrame;       // 다음 펄스에 움직일 만큼 (빨리 열릴 때는 건너뛸 간격)
        closedSides.aim(index, closedStep);
        if (blend && progress < 1 && showBlended(progress, count))
            return;
        show(closedSides, index);
    }

    // 닫힘 장 사이를 두 장 섞어 보임 (저화질은 닫힘 450장이라 그대로 두면 초당 15번만 바뀜, 섞으면 900장만큼 고름).
    // 두 장이 다 풀렸을 때만 섞고, 아니면 false (보통대로 한 장)
    private int[] blendLeft, blendRight;
    private int blendedBase = -1, blendedWeight = -1;      // 마지막으로 섞은 단계
    private boolean blendedOnScreen;                         // 화면 그림이 섞은 장인지 (한 장을 보일 땐 꼭 다시 그리게)

    // 두 장 사이 위치(0~1)에 맞는 섞는 비율. 가운데 한 단계만 써서 450장을 900단계로 (0 = 앞 장, 128 = 가운데, 256 = 다음 장)
    static int blendWeight(double fraction) {
        return (int) Math.round(fraction * 2) * 128;
    }

    private boolean showBlended(double progress, int count) {
        double f = Math.max(0, progress) * (count - 1);
        int base = (int) Math.floor(f);
        int next = Math.min(count - 1, base + 1);
        if (!closedSides.ready(base) || !closedSides.ready(next))
            return false;
        int w = blendWeight(f - base);
        if (w == 0 || w == 256 || base == next)
            return false;       // 섞을 것 없이 한 장 그대로 (보통 길로)
        if (blendedOnScreen && base == blendedBase && w == blendedWeight)
            return true;        // 이미 그 단계를 보이는 중
        if (blendLeft == null) {
            blendLeft = new int[frameWidth * frameHeight];
            blendRight = new int[frameWidth * frameHeight];
        }
        mix(closedSides.left.get(base), closedSides.left.get(next), w, blendLeft);
        mix(closedSides.right.get(base), closedSides.right.get(next), w, blendRight);
        copy(leftBuffer, blendLeft);
        copy(rightBuffer, blendRight);
        blendedBase = base;
        blendedWeight = w;
        blendedOnScreen = true;
        targetFrame = -1;       // 한 장 번호로는 없는 그림 (다음에 한 장을 보일 때 꼭 다시 그림)
        shownFrame = -1;
        leftFrame.setVisible(true);
        rightFrame.setVisible(true);
        return true;
    }

    // premultiplied ARGB 두 장을 w/256 비율로 섞음 (채널마다 선형)
    static void mix(int[] a, int[] b, int w, int[] out) {
        int v = 256 - w;
        for (int i = 0; i < out.length; i++) {
            int p = a[i], q = b[i];
            if (p == q) {
                out[i] = p;
                continue;
            }
            out[i] = (((p >>> 24) * v + (q >>> 24) * w) >> 8) << 24
                | ((((p >> 16) & 255) * v + ((q >> 16) & 255) * w) >> 8) << 16
                | ((((p >> 8) & 255) * v + ((q >> 8) & 255) * w) >> 8) << 8
                | (((p & 255) * v + (q & 255) * w) >> 8);
        }
    }

    // 다시 열림 시작 (updateCurtain이 FX 스레드에서 부름): 지금 닫힌 정도에서 1.2초 일정을 정하고 앞쪽 장부터 풀기 시작
    private void startReopenPlan() {
        double from = 1 + leftCurtain.getTranslateX() / leftCurtain.getWidth();
        if (from <= 0) {
            reopenPlan = null;
            return;
        }
        reopenPlan = CurtainFrames.reopenSchedule(from, OPEN_SECONDS, 30, frames.left().count(),
            t -> Motion.EASE_IN_OUT.interpolate(0.0, 1.0, t));
        reopenStart = System.nanoTime();
        closedSides.aimList(planFrom(0));
    }

    // 일정의 j번째부터 AHEAD장 (먼저 보일 장부터 풂)
    private java.util.List<Integer> planFrom(int j) {
        java.util.List<Integer> want = new java.util.ArrayList<>();
        for (int k = j; k < reopenPlan.length && want.size() <= AHEAD; k++)
            if (!want.contains(reopenPlan[k]))
                want.add(reopenPlan[k]);
        return want;
    }

    // 다시 열림 중이면 일정의 장을 보이고 true. 일정이 끝났으면 false (그 뒤는 숨긴 Rectangle 위치대로)
    private boolean showReopenFrame() {
        int j = (int) ((System.nanoTime() - reopenStart) / 1e9 * 30);
        if (j >= reopenPlan.length - 1) {
            reopenPlan = null;
            return false;
        }
        int index = reopenPlan[j];
        if (index == 0) {
            hideFrames();
            closedSides.release();
            return true;
        }
        closedSides.aimList(planFrom(j));
        show(closedSides, index);
        return true;
    }

    // index 장을 보임. 아직 안 풀렸으면 지금 보이는 장 쪽으로 가장 가까운 풀린 장을 대신 보이고, 늦게 풀리면 decoded()가 다시 부름
    private void show(FrameSides sides, int index) {
        targetFrame = index;
        if (index == shownFrame)
            return;
        int pick = sides.ready(index) ? index : sides.nearestReady(index, shownFrame >= 0 ? shownFrame : index);
        if (pick < 0 || pick == shownFrame)
            return;     // 대신 보일 장도 없음: 앞 장을 그대로 둠 (숨겨 둔 그림이면 지난 장이 비치지 않게 계속 숨김)
        copy(leftBuffer, sides.left.get(pick));
        copy(rightBuffer, sides.right.get(pick));
        blendedOnScreen = false;
        shownFrame = pick;
        leftFrame.setVisible(true);
        rightFrame.setVisible(true);
    }

    private void hideFrames() {
        leftFrame.setVisible(false);       // 그림은 그대로 두고 숨김 (다음에 보일 때 첫 장으로 덮어씀)
        rightFrame.setVisible(false);
        forgetFrames();
    }

    // 닫힘과 막 오름이 서로 바뀔 때: 장 번호를 잊음 (화면의 그림은 다음 장을 보일 때까지 그대로)
    private void forgetFrames() {
        blendedOnScreen = false;
        targetFrame = -1;
        shownFrame = -1;
        closedStep = 1;
    }

    // 막 오름: open_00~ 를 흐른 시간 순서로 RAISE_SECONDS 동안 재생. appear초가 있으면 그 동안 첫 장이 나타남
    private void raiseFrames(double appear) {
        stopFrameRaise();
        reopenPlan = null;
        raisingFrames = true;
        forgetFrames();
        closedSides.release();
        int count = frames.openLeft().count();
        raiseTimer = new AnimationTimer() {
            private long start = -1;

            @Override
            public void handle(long now) {
                if (start < 0)
                    start = now;
                double t = (now - start) / 1e9;
                double opacity = appear > 0 ? Math.min(1, t / appear) : 1;
                leftFrame.setOpacity(opacity);
                rightFrame.setOpacity(opacity);
                int index = CurtainFrames.openIndexAt(t - appear, RAISE_SECONDS, count);
                openSides.aim(index, 1);
                show(openSides, index);
                if (t >= appear + RAISE_SECONDS)
                    stopFrameRaise();
            }
        };
        raiseTimer.start();
    }

    // 막 오름 재생을 멈추고 숨긴 Rectangle 위치대로 닫힘 장(다 열렸으면 숨김)으로 돌아감
    private void stopFrameRaise() {
        if (raiseTimer == null)
            return;
        raiseTimer.stop();
        raiseTimer = null;
        raisingFrames = false;
        forgetFrames();
        leftFrame.setOpacity(1);
        rightFrame.setOpacity(1);
        openSides.release();
        showClosedFrame();
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
            if (frames != null) {
                if (opening)
                    startReopenPlan();      // Blender 커튼: 다시 열림은 정해 둔 일정대로 장을 보임
                else
                    reopenPlan = null;
            }
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
        if (frames != null) {
            reopenPlan = null;
            stopFrameRaise();       // 막 오름 중이었으면 멈추고 지금 위치의 닫힘 장으로
            showClosedFrame();      // 이미 닫힌 자리였으면 위치 변화 알림이 없으므로 직접
        }
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
        if (frames != null) {
            open.play();            // 숨긴 Rectangle은 같은 시간에 열린 자리로 (다음 닫힘, 다시 열림 규칙이 이 위치를 씀)
            raiseFrames(appear);    // 보이는 건 렌더한 막 오름 장
            return;
        }
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
