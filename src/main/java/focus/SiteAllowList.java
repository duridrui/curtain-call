package focus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.net.URI;

// 허용 사이트 목록을 파일에서 읽고 저장함, 감지 스레드와 설정 창이 함께 읽음
public class SiteAllowList {
    // 파일이 없거나 못 읽을 때 쓰는 기본 허용 사이트
    static final List<String> DEFAULT = List.of("daegu.ac.kr", "kmooc.kr", "github.com");

    private final Path file;                // 목록을 저장하는 파일 위치
    private volatile List<String> current;  // 지금 목록

    public SiteAllowList(Path file) {
        this.file = file;
        this.current = load(file);          // 시작할 때 파일에서 목록을 읽어 current에 저장
    }

    // 지금 목록 꺼내기
    public List<String> get() {
        return current;
    }

    // 새 목록으로 교체. 정리 후 파일에 저장하고 current를 바꿈. 정리 후 비어 있으면 기본 목록 저장
    public void replace(List<String> sites) throws IOException {
        if (sites == null)
            return;
        List<String> cleaned = clean(sites);
        if (cleaned.isEmpty())
            cleaned = DEFAULT;

        Files.createDirectories(file.getParent());      // ~/.curtain-call 폴더가 없으면 만듦
        Files.write(file, cleaned);                     // 한 줄에 사이트 하나씩 파일에 씀

        current = cleaned;                              // 감지 스레드가 다음 판정부터 새 목록을 읽음
    }

    // 파일에서 목록 읽기, 파일이 없거나, 읽다 실패하거나, 정리 후 비어 있으면 기본 목록
    static List<String> load(Path file) {
        try {
            if (!Files.exists(file))
                return DEFAULT;
            List<String> cleaned = clean(Files.readAllLines(file)); // 파일의 모든 줄을 읽어 정리
            if (cleaned.isEmpty())                                  // 정리 후 남은 줄이 없으면
                return DEFAULT;
            return cleaned;
        } catch (IOException e) {
            return DEFAULT;
        }
    }

    // 목록 정리 : 앞뒤 공백 제거, 빈 줄 무시, 소문자로 통일, 중복 제거
    static List<String> clean(List<String> lines) {
        ArrayList<String> result = new ArrayList<>();
        for (String line : lines) {
            String site = line.trim().toLowerCase();    // 앞뒤 공백 제거 후 소문자로 변환
            if (site.isEmpty())
                continue;
            if (!result.contains(site))
                result.add(site);
        }
        return List.copyOf(result);                     // 수정할 수 없는 List로 만들어 반환
    }

    // 호스트가 목록의 사이트와 같거나 그 하위 주소면 그 사이트 이름을 반환, 아니면 null
    static String matchedSite(String host, List<String> allowed) {
        if (host == null)
            return null;
        for (String site : allowed) {
            if (host.equals(site) || host.endsWith("." + site))
                return site;
        }
        return null;
    }

    // 호스트가 허용 사이트인지
    static boolean isAllowedHost(String host, List<String> allowed) {
        return matchedSite(host, allowed) != null;
    }

    // 웹 주소에서 호스트를 소문자로 꺼냄. http, https 주소가 아니거나 잘못된 주소면 null
    static String hostOf(String url) {
        if (url == null)
            return null;
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();   // 주소 앞부분 (https 등)
            if (!"http".equals(scheme) && !"https".equals(scheme))
                return null;                   // 새 탭, 설정 화면, PDF 파일 등은 웹 주소가 아님
            String host = uri.getHost();
            return host == null ? null : host.toLowerCase();
        } catch (IllegalArgumentException e) {
            return null;                       // 주소 형식이 잘못됨
        }
    }

    // 통계에 적을 사이트 이름. 앞의 www. 또는 m. 을 떼서 같은 사이트를 하나로 셈
    static String siteKey(String host) {
        if (host == null)
            return null;
        if (host.startsWith("www."))
            return host.substring(4);       // "www." 네 글자 뒤부터
        if (host.startsWith("m."))
            return host.substring(2);       // "m." 두 글자 뒤부터
        return host;
    }

    // 브라우저 탭 판정. 주소를 모르면 딴짓 아님, 허용 사이트가 아니면 딴짓
    static boolean isDistractingSite(String host, List<String> allowed) {
        if (host == null)
            return false;
        return !isAllowedHost(host, allowed);
    }
}
