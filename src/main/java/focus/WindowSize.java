package focus;

// 창 크기와 위치 계산: 기본 크기로 화면 가운데에 두고, 화면이 작으면 사용 영역의 85% 안으로 같은 비율로 줄임
final class WindowSize {
    static final double SCREEN_LIMIT = 0.85;     // 화면 사용 영역 대비 최대 비율
    static final double MIN_RATIO = 0.75;        // 따로 정하지 않은 창의 최소 크기 = 기본 크기의 75%

    private final double x;
    private final double y;
    private final double width;
    private final double height;
    private final double minWidth;
    private final double minHeight;

    private WindowSize(double x, double y, double width, double height, double minWidth, double minHeight) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.minWidth = minWidth;
        this.minHeight = minHeight;
    }

    // 극장 창 (로비·예매·커튼콜·관람 기록): 1440x900 목업 비율에 맞춘 1200x760.
    // 최소 1100x720은 좌석 홀(폭 760)과 좌우 여백이 들어가는 크기
    static WindowSize theater(double screenX, double screenY, double screenWidth, double screenHeight) {
        return fit(screenX, screenY, screenWidth, screenHeight, 1200, 760, 1100, 720);
    }

    // 허용 목록 설정 창: 두 목록 칸과 아래 버튼 줄이 들어가는 760x620
    static WindowSize settings(double screenX, double screenY, double screenWidth, double screenHeight) {
        return fit(screenX, screenY, screenWidth, screenHeight, 760, 620,
            Math.floor(760 * MIN_RATIO), Math.floor(620 * MIN_RATIO));
    }

    // 기본 크기를 화면 사용 영역의 85% 안에 들어오게 같은 비율로 줄이고, 화면 가운데에 놓을 위치를 계산함
    static WindowSize fit(double screenX, double screenY, double screenWidth, double screenHeight,
            double preferredWidth, double preferredHeight, double minimumWidth, double minimumHeight) {
        double scale = 1;
        if (screenWidth > 0 && screenHeight > 0)                    // 화면 크기를 모르면 줄이지 않음
            scale = Math.min(1, Math.min(screenWidth * SCREEN_LIMIT / preferredWidth,
                screenHeight * SCREEN_LIMIT / preferredHeight));
        double width = Math.floor(preferredWidth * scale);
        double height = Math.floor(preferredHeight * scale);
        double minWidth = Math.min(width, minimumWidth);            // 최소 크기가 줄인 창 크기보다 크지 않게 함
        double minHeight = Math.min(height, minimumHeight);
        double x = screenX + (screenWidth > 0 ? (screenWidth - width) / 2 : 0);
        double y = screenY + (screenHeight > 0 ? (screenHeight - height) / 2 : 0);
        return new WindowSize(x, y, width, height, minWidth, minHeight);
    }

    double getX() { return x; }
    double getY() { return y; }
    double getWidth() { return width; }
    double getHeight() { return height; }
    double getMinWidth() { return minWidth; }
    double getMinHeight() { return minHeight; }
}
