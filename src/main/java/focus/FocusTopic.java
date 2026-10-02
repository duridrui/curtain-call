package focus;

import java.util.List;

// 집중 주제: 좌석 화면에서 고르는 기본 주제 다섯 개와 '기타'(직접 입력, 20자까지)
final class FocusTopic {
    static final List<String> PRESETS = List.of("공부", "과제", "코딩", "독서", "작업");
    static final String OTHER = "기타";
    static final int MAX_LENGTH = 20;

    private FocusTopic() {
    }

    // 글자 수 (이모지처럼 두 칸을 쓰는 글자도 한 글자로 셈)
    static int length(String text) {
        return text.codePointCount(0, text.length());
    }

    // 직접 입력한 주제 정리: 줄바꿈, 제어 문자는 공백으로, 공백은 하나로, 앞뒤 공백 제거
    // 비었거나 20자를 넘으면 null
    static String normalize(String raw) {
        if (raw == null)
            return null;
        String text = raw.replaceAll("\\p{Cntrl}", " ").replaceAll("\\s+", " ").strip();
        if (text.isEmpty() || length(text) > MAX_LENGTH)
            return null;
        return text;
    }

    // 고른 칩과 입력란으로 정한 주제. 기타에 입력이 비었으면 '기타', 고르지 않았거나 쓸 수 없는 입력이면 null
    static String choose(String chip, String typed) {
        if (chip == null)
            return null;
        if (PRESETS.contains(chip))
            return chip;
        if (!OTHER.equals(chip))
            return null;
        if (typed == null || typed.isBlank())
            return OTHER;
        return normalize(typed);
    }

    // 기록 파일에서 읽은 주제. 없거나 쓸 수 없는 값이면 '기타' (주제가 없던 예전 기록 호환)
    static String orOther(String stored) {
        String topic = normalize(stored);
        return topic == null ? OTHER : topic;
    }
}
