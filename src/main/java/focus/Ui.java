package focus;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.ImagePattern;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.RadialGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Ellipse;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.Shape;

// 여러 화면이 함께 사용하는 글자,버튼,조명,티켓,바코드 부품을 모은 클래스
final class Ui {
    static final double EDGE = 72;      // 화면의 위쪽과 좌우에서 공통으로 사용하는 기본 여백
    static final double BOTTOM = 56;    // 화면 아래쪽에 사용하는 공통 여백
    static final Color PAPER = Color.web("#F3E9DC");
    static final Color INK = Color.web("#2B1E1E");
    private static Image curtain;   // 커튼 이미지 최초 한 번 읽은 뒤 보관

    private Ui() {
    }
    static Label label(String text, String... styleClasses) {
        Label l = new Label(text);
        l.getStyleClass().addAll(styleClasses);
        return l;
    }

    static Button primary(String text, double width) {
        return button(text, width, "btn-primary");
    }
    static Button ghost(String text, double width) {
        return button(text, width, "btn-ghost");
    }

    private static Button button(String text, double width, String styleClass) {
        Button b = new Button(text);
        b.getStyleClass().add(styleClass);
        if (width > 0) {
            b.setMinWidth(width);
            b.setPrefWidth(width);
            b.setMaxWidth(width);
        }
        return Motion.pressable(b);
    }
    // 이전 화면으로 돌아가는 원형 버튼, 클릭하면 전달받은 동작을 실행
    static Button back(Runnable onBack) {
        Button b = new Button("‹");
        b.getStyleClass().add("btn-round");
        b.setOnAction(e -> onBack.run());
        return Motion.pressable(b);
    }
    static Button chip(String text) {
        Button b = new Button(text);
        b.getStyleClass().add("chip");
        return b;
    }
    static Region grow() {
        Region r = new Region();
        HBox.setHgrow(r, Priority.ALWAYS);
        return r;
    }
    static Region rule() {
        Region r = new Region();
        r.getStyleClass().add("rule");
        return r;
    }
    // 화면 부품을 네 방향에 고정, null인 방향은 고정하지 않음
    static <T extends Node> T anchor(T node, Double top, Double right, Double bottom, Double left) {
        AnchorPane.setTopAnchor(node, top);
        AnchorPane.setRightAnchor(node, right);
        AnchorPane.setBottomAnchor(node, bottom);
        AnchorPane.setLeftAnchor(node, left);
        return node;
    }
    // 사다리꼴 빛줄기와 바닥의 타원형 빛을 합쳐 무대 조명 만듦
    static Pane spotlight(double width, double height, double poolHeight) {
        double topWidth = width * 0.22;
        Polygon beam = new Polygon(
            (width - topWidth) / 2, 0, (width + topWidth) / 2, 0, width, height, 0, height);
        beam.setFill(new LinearGradient(0, 0, 0, 1, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(245, 243, 241, 0.26)), new Stop(1, Color.rgb(245, 243, 241, 0.03))));

        Ellipse pool = new Ellipse(width / 2, height, width / 2, poolHeight / 2);
        pool.setFill(new RadialGradient(0, 0, 0.5, 0.5, 0.5, true, CycleMethod.NO_CYCLE,
            new Stop(0, Color.rgb(245, 243, 241, 0.30)), new Stop(0.6, Color.rgb(245, 243, 241, 0.12)),
            new Stop(1, Color.rgb(245, 243, 241, 0))));
        Pane light = new Pane(beam, pool);
        light.setPrefSize(width, height + poolHeight / 2);
        light.setMaxSize(width, height + poolHeight / 2);
        light.setMouseTransparent(true);
        return light;
    }
    static Image curtainImage() {
        if (curtain == null) {
            java.net.URL url = Ui.class.getResource("/images/curtain.jpg");
            curtain = url == null ? null : new Image(url.toExternalForm());
        }
        return curtain == null || curtain.isError() ? null : curtain;
    }
    // 커튼 사진을 원본 비율 높이에 맞춰 사용, 사진 없으면 어두운 붉은색 사용
    static javafx.scene.paint.Paint velvet(double offsetX, double height) {
        Image image = curtainImage();
        if (image == null)
            return Color.web("#8E0710");
        double width = height * image.getWidth() / image.getHeight();
        return new ImagePattern(image, offsetX, 0, width, height, false);
    }
    // 커튼 사진을 지정한 높이로 표시했을 때 원본 비율에 맞는 너비 계산
    static double velvetWidth(double height) {
        Image image = curtainImage();
        return image == null ? height : height * image.getWidth() / image.getHeight();
    }
    // 위 모서리는 둥글고 아래 양쪽에는 반원 홈, 선택에 따라 아래쪽을 찢어진 모양으로 만드는 티켓 윗부분
    static Shape ticketTop(double width, double height, double radius, double notch, boolean torn) {
        Shape body = Shape.union(roundRect(0, 0, width, height, radius), new Rectangle(0, radius, width, height - radius));
        if (torn) {
            body = Shape.subtract(body, new Rectangle(0, height - 8, width, 16));
            body = Shape.union(body, edgePolygon(width, height, true));
        }
        return Shape.subtract(Shape.subtract(body, new Circle(0, height, notch)), new Circle(width, height, notch));
    }
    // 아래 모서리는 둥글고 위 양쪽에는 반원 홈, 선택에 따라 위쪽을 찢어진 모양으로 만드는 티켓 아래 조각
    static Shape ticketStub(double width, double height, double radius, double notch, boolean torn) {
        Shape body = Shape.union(new Rectangle(0, 0, width, height - radius), roundRect(0, 0, width, height, radius));
        if (torn) {
            body = Shape.subtract(body, new Rectangle(0, -8, width, 16));
            body = Shape.union(body, edgePolygon(width, 0, false));
        }
        return Shape.subtract(Shape.subtract(body, new Circle(0, 0, notch)), new Circle(width, 0, notch));
    }
    // TicketArt의 톱니 좌표 연결, 위쪽 또는 아래쪽을 채워 찢어진 가장자리 띠를 만듦
    private static Polygon edgePolygon(double width, double y, boolean above) {
        double[] edge = TicketArt.tornEdge(width, 16, 5);
        Polygon p = new Polygon();
        double far = above ? y - 10 : y + 10;
        p.getPoints().addAll(0.0, far);
        for (int i = 0; i < edge.length; i += 2)
            p.getPoints().addAll(edge[i], y + edge[i + 1]);
        p.getPoints().addAll(width, far);
        return p;
    }
    // 둥근 사각형의 양옆 가운데를 반원으로 파낸 기록 카드 모양
    static Shape notchedCard(double width, double height, double radius, double notch) {
        Shape card = roundRect(0, 0, width, height, radius);
        return Shape.subtract(Shape.subtract(card, new Circle(0, height / 2, notch)), new Circle(width, height / 2, notch));
    }
    static Rectangle roundRect(double x, double y, double w, double h, double radius) {
        Rectangle r = new Rectangle(x, y, w, h);
        r.setArcWidth(radius * 2);
        r.setArcHeight(radius * 2);
        return r;
    }
    // 티켓 번호로 계산한 막대 위치와 굵기를 사각형으로 그려 장식용 바코드를 만듦
    static Pane barcode(String number, double width, double height, Color color) {
        Pane bars = new Pane();
        for (double[] bar : TicketArt.barcode(number, width)) {
            Rectangle r = new Rectangle(bar[0], 0, bar[1], height);
            r.setFill(color);
            bars.getChildren().add(r);
        }
        bars.setPrefSize(width, height);
        bars.setMaxSize(width, height);
        bars.setMinSize(width, height);
        return bars;
    }
    // 작은 항목 이름은 위에, 값은 아래에 배치, 필요하면 오른쪽으로 정렬하는 정보 한 칸
    static javafx.scene.layout.VBox field(String key, String value, String keyClass, String valueClass, boolean right) {
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(4, label(key, keyClass), label(value, valueClass));
        box.setAlignment(right ? Pos.TOP_RIGHT : Pos.TOP_LEFT);
        return box;
    }
}