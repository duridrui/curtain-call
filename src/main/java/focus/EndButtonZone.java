package focus;

// 커튼 창에서 클릭을 받는 유일한 사각형: 왼쪽 아래 공연 종료 버튼(지름 44)과 사방 4px.
// 커튼 창 전체는 클릭이 아래 앱으로 통과하고, 이 사각형만 작은 창을 따로 띄워 클릭을 받는다
final class EndButtonZone {
    static final double MARGIN = 56;       // 화면 왼쪽, 아래에서 버튼까지
    static final double DIAMETER = 44;     // 버튼 지름
    static final double PAD = 4;           // 버튼 둘레 여유 (누를 때 바깥 링이 들어가는 자리)
    static final double SIZE = DIAMETER + PAD * 2;

    private final double x;
    private final double y;

    private EndButtonZone(double x, double y) {
        this.x = x;
        this.y = y;
    }

    // 화면 사용 영역(왼쪽 위 좌표와 크기) 기준
    static EndButtonZone of(double screenX, double screenY, double screenWidth, double screenHeight) {
        return new EndButtonZone(screenX + MARGIN - PAD, screenY + screenHeight - MARGIN - DIAMETER - PAD);
    }

    boolean contains(double px, double py) {
        return px >= x && px < x + SIZE && py >= y && py < y + SIZE;
    }

    double getX() { return x; }
    double getY() { return y; }
    double getWidth() { return SIZE; }
    double getHeight() { return SIZE; }
}
