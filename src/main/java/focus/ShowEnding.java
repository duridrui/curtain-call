package focus;

// 공연이 끝난 이유: 정상 종료(공연 길이를 다 채우거나, 공연 종료 버튼을 눌렀을 때, 메뉴바로 끝냈을 때) 또는 막이 다 닫혀 중도 종료
enum ShowEnding {
    COMPLETED("관람 완료", "관람\n완료"),
    CURTAIN_DOWN("중도 종료", "중도\n종료");

    private final String badge;     // 커튼콜, 관람 기록 카드의 배지
    private final String stamp;     // 커튼콜 티켓 도장 (두 줄)

    ShowEnding(String badge, String stamp) {
        this.badge = badge;
        this.stamp = stamp;
    }

    String getBadge() {
        return badge;
    }

    String getStamp() {
        return stamp;
    }

    // 기록 파일의 값을 읽음. 없거나 모르는 값이면 정상 종료 (예전 기록 호환)
    static ShowEnding parse(String stored) {
        for (ShowEnding e : values())
            if (e.name().equals(stored))
                return e;
        return COMPLETED;
    }
}
