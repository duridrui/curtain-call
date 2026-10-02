package focus;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// 티켓 화면에 사용할 바코드, 찢어진 가장자리, 눈금자 모양의 값 계산
final class TicketArt {
    static final int RULER_MAX_MINUTES = 90;    // 장식용 마지막 값 90분
    static final int RULER_STEP_MINUTES = 2;    // 작은 눈금 2분 간격으로 표시
    static final int MAJOR_STEP_MINUTES = 10;   // 긴 눈금 10분 간격으로 표시
    private static final double[] BAR_WIDTHS = { 2, 4, 6 };

    private TicketArt() {
    }

    // 티켓 번호를 기준으로 같은 모양의 바코드 막대 만들기
    static List<double[]> barcode(String number, double width) {
        Random random = new Random(number == null ? 0 : number.hashCode());     // 같은 티켓 번호는 같은 바코드 모양을 생성
        List<double[]> bars = new ArrayList<>();
        double x = 0;
        while (true) {
            double w = BAR_WIDTHS[random.nextInt(BAR_WIDTHS.length)];   // 세가지 후보 중 현재 막대 폭을 무작위로 선택
            if (x + w > width)      // 다음 막대가 전체 폭을 넘으면 생성 중단
                break;
            bars.add(new double[] { x, w });    // 막대의 시작 위치와 폭을 한 쌍으로 저장
            x += w + 2 + random.nextInt(3);     // 현재 막대 폭과 2~4 간격만큼 다음 위치로 이동
        }
        return bars;
    }

    // 찢어진 가장자리를 만들 x,y 좌표쌍 배열을 반환
    static double[] tornEdge(double width, int teeth, double amplitude) {
        int points = teeth * 2 + 1;
        double[] edge = new double[points * 2];     // 각 점의 x와 y를 저장하므로 점 개수의 두배 크기
        for (int i = 0; i < points; i++) {
            edge[i * 2] = width * i / (points - 1);     // 전체 폭의 좌표의 x의 위치를 같은 간격으로 배치
            edge[i * 2 + 1] = i % 2 == 0 ? -amplitude : amplitude;      // y값의 음수, 양수를 번갈아 넣어 골과 마루 생성
        }
        return edge;
    }
    // 공연 시간을 제한한 뒤 눈금자 전체 폭에서 해당하는 가로 위치 변경
    static double rulerX(int minutes, double width) {
        int clamped = Math.max(0, Math.min(RULER_MAX_MINUTES, minutes));    // 입력한 분을 0~90분 범위로 제한
        return width * clamped / RULER_MAX_MINUTES;     // 제한된 분을 눈금자 폭에 비례한 x 위치로 변환
    }
    // 눈금자에 표시할 값을 0분부터 90분까지 2분 간격의 값으로 설정
    static List<Integer> rulerTicks() {
        List<Integer> ticks = new ArrayList<>();
        for (int m = 0; m <= RULER_MAX_MINUTES; m += RULER_STEP_MINUTES)    // 0분부터 90분까지 2분씩 증가
            ticks.add(m);
        return ticks;
    }
    // 현재 분이 10분 단위인지 확인해 눈금 표시 결정
    static boolean isMajorTick(int minutes) {
        return minutes % MAJOR_STEP_MINUTES == 0;   // 10으로 나눈 나머지가 0이면 긴 눈금으로 판정
    }
}
