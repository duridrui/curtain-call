package focus;

import java.util.List;
import java.util.concurrent.TimeUnit;

// 방해금지 연동: 단축어 앱의 "커튼콜 방해금지 켜기", "커튼콜 방해금지 끄기"를 실행해 공연 중에만 집중 모드(방해금지)를 켬.
// 시스템 설정은 직접 바꾸지 않고 사용자가 만든 단축어만 실행한다. 단축어가 없거나 2초 안에 안 끝나면 조용히 넘어간다
public class DoNotDisturb {
    static final String ON_NAME = "커튼콜 방해금지 켜기";
    static final String OFF_NAME = "커튼콜 방해금지 끄기";
    static final long TIMEOUT_MILLIS = 2000;

    // 명령 실행기. 성공이면 true (테스트에서는 가짜를 넣음)
    interface Runner {
        boolean run(List<String> command, long timeoutMillis);
    }

    private final Runner runner;

    public DoNotDisturb() {
        this(DoNotDisturb::runProcess);
    }

    DoNotDisturb(Runner runner) {
        this.runner = runner;
    }

    public boolean turnOn() {
        return runShortcut(ON_NAME);
    }

    public boolean turnOff() {
        return runShortcut(OFF_NAME);
    }

    private boolean runShortcut(String name) {
        try {
            return runner.run(List.of("shortcuts", "run", name), TIMEOUT_MILLIS);
        } catch (RuntimeException e) {      // 실행기 자체가 실패해도 공연은 계속
            System.err.println("방해금지 연동 실패(무시하고 계속): " + e.getMessage());
            return false;
        }
    }

    // 로비에 보여 줄 준비 상태: 단축어 목록을 읽어 켜기, 끄기가 다 있는지 (2초 제한)
    public static boolean isReady() {
        try {
            ProcessBuilder pb = new ProcessBuilder("shortcuts", "list");
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            if (!p.waitFor(TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();
                return false;
            }
            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return p.exitValue() == 0 && hasShortcuts(out);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    // 목록 출력(한 줄에 단축어 하나)에 켜기, 끄기 이름이 정확히 있는지
    static boolean hasShortcuts(String listOutput) {
        if (listOutput == null)
            return false;
        List<String> names = listOutput.lines().map(String::trim).toList();
        return names.contains(ON_NAME) && names.contains(OFF_NAME);
    }

    // 실제 명령 실행: 시간 안에 끝나고 종료코드가 0이면 true
    static boolean runProcess(List<String> command, long timeoutMillis) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.DISCARD);     // 출력은 쓰지 않음
            Process p = pb.start();
            if (!p.waitFor(timeoutMillis, TimeUnit.MILLISECONDS)) {
                p.destroyForcibly();                                // 제한 시간 넘으면 끝냄
                return false;
            }
            return p.exitValue() == 0;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (Exception e) {     // shortcuts 명령이 없는 경우 등
            return false;
        }
    }
}
