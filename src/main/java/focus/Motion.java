package focus;

import java.util.List;
import javafx.animation.FadeTransition;
import javafx.animation.Interpolator;
import javafx.animation.ParallelTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

// 화면 전환과 연출에 쓰는 짧은 애니메이션 모음.
// 위치,크기,투명도만 움직이고 레이아웃 값은 건드리지 않는다
final class Motion {
    static final Interpolator EASE_OUT = Interpolator.SPLINE(0.23, 1, 0.32, 1);       // 들어올 때,호버
    static final Interpolator EASE_IN_OUT = Interpolator.SPLINE(0.77, 0, 0.175, 1);   // 막이 움직일 때

    static final Duration SCREEN = Duration.millis(320);    // 화면 전환
    static final Duration ITEM = Duration.millis(280);      // 카드,행 등장
    static final Duration ITEM_GAP = Duration.millis(40);   // 항목 간 지연
    static final int ITEM_GAP_MAX = 6;                      // 지연을 늘리는 최대 항목 수
    static final Duration PRESS = Duration.millis(120);     // 버튼 눌림
    static final Duration RELEASE = Duration.millis(160);   // 버튼 뗌
    static final Duration HOVER = Duration.millis(150);     // 호버
    static final Duration SEAT = Duration.millis(140);      // 좌석 선택
    static final Duration ISSUE = Duration.millis(600);     // 티켓 발권
    static final Duration STAMP = Duration.millis(450);     // 관람 완료 도장
    static final Duration POP = Duration.millis(220);       // 설정 창 열림
    static final Duration SETTLE = Duration.millis(220);    // 끌다 놓은 조각이 제자리로

    private Motion() {
    }

    // 새 화면: 투명도 0 -> 1, 위로 12px -> 0
    static void screenIn(Node node) {
        rise(node, 12, SCREEN, Duration.ZERO).play();
    }

    // 여러 항목이 40ms 간격으로 아래 8px에서 올라오며 나타남
    static void itemsIn(List<? extends Node> nodes) {
        for (int i = 0; i < nodes.size(); i++)
            rise(nodes.get(i), 8, ITEM, ITEM_GAP.multiply(Math.min(i, ITEM_GAP_MAX))).play();
    }

    // 투명하게 distance px 아래에 둔 뒤, 투명도 1,제자리로 함께 움직이는 애니메이션을 만들어 돌려줌 (시작은 부르는 쪽에서 play)
    static ParallelTransition rise(Node node, double distance, Duration duration, Duration delay) {
        node.setOpacity(0);
        node.setTranslateY(distance);
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setToValue(1);
        TranslateTransition move = new TranslateTransition(duration, node);
        move.setToY(0);
        ParallelTransition both = new ParallelTransition(fade, move);
        both.setInterpolator(EASE_OUT);
        both.setDelay(delay);
        return both;
    }

    // 투명도를 지금 값에서 opacity까지 duration 동안 바꿈
    static void fadeTo(Node node, double opacity, Duration duration) {
        FadeTransition fade = new FadeTransition(duration, node);
        fade.setToValue(opacity);
        fade.setInterpolator(EASE_OUT);
        fade.play();
    }

    // 누르면 0.97배로 줄었다가 떼면 돌아옴
    static <T extends Node> T pressable(T node) {
        node.setOnMousePressed(e -> scaleTo(node, 0.97, PRESS));
        node.setOnMouseReleased(e -> scaleTo(node, 1, RELEASE));
        return node;
    }

    // 마우스를 올리면 위로 distance px 뜸
    static <T extends Node> T liftOnHover(T node, double distance) {
        node.hoverProperty().addListener((obs, was, now) -> {
            TranslateTransition move = new TranslateTransition(HOVER, node);
            move.setToY(now ? -distance : 0);
            move.setInterpolator(EASE_OUT);
            move.play();
        });
        return node;
    }

    // 크기를 지금 값에서 scale배까지 duration 동안 바꿈 (가로와 세로를 같은 비율로)
    static void scaleTo(Node node, double scale, Duration duration) {
        ScaleTransition s = new ScaleTransition(duration, node);
        s.setToX(scale);
        s.setToY(scale);
        s.setInterpolator(EASE_OUT);
        s.play();
    }

    // 티켓 발권: 아래 160px에서 투명하게 올라와 제자리
    static ParallelTransition issue(Node ticket) {
        return rise(ticket, 160, ISSUE, Duration.ZERO);
    }

    // 관람 완료 도장: 1.6배에서 찍히며 불투명도 .88, 각도 -14도는 고정
    static ParallelTransition stamp(Node stamp, Duration delay) {
        stamp.setOpacity(0);
        stamp.setScaleX(1.6);
        stamp.setScaleY(1.6);
        stamp.setRotate(-14);
        ScaleTransition scale = new ScaleTransition(STAMP, stamp);
        scale.setToX(1);
        scale.setToY(1);
        FadeTransition fade = new FadeTransition(STAMP, stamp);
        fade.setToValue(0.88);
        ParallelTransition both = new ParallelTransition(scale, fade);
        both.setInterpolator(EASE_OUT);
        both.setDelay(delay);
        return both;
    }

    // 설정 창 열림: 투명도와 크기 0.96 -> 1 (가운데 기준)
    static void popIn(Node node) {
        node.setOpacity(0);
        node.setScaleX(0.96);
        node.setScaleY(0.96);
        FadeTransition fade = new FadeTransition(POP, node);
        fade.setToValue(1);
        ScaleTransition scale = new ScaleTransition(POP, node);
        scale.setToX(1);
        scale.setToY(1);
        ParallelTransition both = new ParallelTransition(fade, scale);
        both.setInterpolator(EASE_OUT);
        both.play();
    }
}
