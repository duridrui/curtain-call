package focus;

import java.util.ArrayList;
import java.util.List;
import java.nio.file.Path;
import java.nio.file.Files;
import java.io.IOException;

// 허용 목록을 파일에서 읽고 쓰고 두 스레드가 안전하게 보게 하는 곳
public class AllowList {
    // 파일이 없거나 못 읽을 때 쓰는 기본 목록
    static final List<String> DEFAULT = List.of("Code", "Terminal", "iTerm2");

    private final Path file; // 목록을 저장하는 파일 위치
    private volatile List<String> current; // 지금 목록

    public AllowList(Path file) {
        this.file = file;
        this.current = load(file); // 시작할 때 파일에서 목록을 읽어 칸에 걸기
    }

    // 지금 목록 꺼내기
    public List<String> get() {
        return current;
    }

    // 새 목록으로 교체(설정 창 저장 버튼이 부름) / 0개면 기본 목록
    public void replace(List<String> apps) throws IOException {
        if (apps == null)
            return;
        List<String> cleand = clean(apps);
        if (cleand.isEmpty())
            cleand = DEFAULT;

        // 파일에 쓰기
        Files.createDirectories(file.getParent());
        Files.write(file, cleand);

        // 감지 스레드가 쓰는 목록을 새 목록으로 교체
        current = cleand;
    }

    // 파일에서 목록 읽기 / 없거나 실패하거나 비어 있으면 기본 목록
    static List<String> load(Path file) {
        try {
            if (!Files.exists(file))
                return DEFAULT;
            List<String> lines = Files.readAllLines(file);
            List<String> cleaned = clean(lines); // 정리한 결과 받아두기
            if (cleaned.isEmpty())
                return DEFAULT; // 0개면 기본 목록
            return cleaned;
        } catch (IOException e) {
            return DEFAULT;
        }
    }

    // 정리 : 앞뒤 공백 제거, 빈 줄 무시, 중복은 한 번만 (대소문자 그대로)
    static List<String> clean(List<String> lines) {
        ArrayList<String> result = new ArrayList<>();
        for (String line : lines) {
            String name = line.trim();
            if (name.isEmpty())
                continue;
            if (!result.contains(name)) {
                result.add(name);
            }
        }
        return List.copyOf(result);
    }
}
