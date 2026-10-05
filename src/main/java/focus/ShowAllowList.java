package focus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

final class ShowAllowList {
    private ShowAllowList() {
    }

    // 지난 공연 앱을 먼저 두고, 실행 중인 앱을 정렬해 중복 없이 추가하는 규칙
    static List<String> appChoices(List<String> last, List<String> running) {
        List<String> result = new ArrayList<>(AllowList.clean(last));
        List<String> rest = new ArrayList<>(AllowList.clean(running));
        rest.sort(String.CASE_INSENSITIVE_ORDER);
        for (String app : rest)
            if (!result.contains(app) && !app.equals("java"))
                result.add(app);
        return result;
    }

    // 지난 목록을 읽을 수 없거나 비어 있으면 기본 목록으로 돌아가는 규칙
    static List<String> precheck(Path lastFile, List<String> defaults) {
        if (!Files.exists(lastFile))
            return defaults;
        try {
            List<String> last = AllowList.clean(Files.readAllLines(lastFile));
            return last.isEmpty() ? defaults : last;
        } catch (IOException e) {
            return defaults;
        }
    }

    // 이번 공연 목록을 다음 공연을 위해 저장
    static void saveLast(Path lastFile, List<String> items) throws IOException {
        Files.createDirectories(lastFile.getParent());
        Files.write(lastFile, AllowList.clean(items));
    }

    static boolean canIssue(List<String> checkedApps) {
        return !checkedApps.isEmpty();
    }

    static List<String> parseProcessList(String output) {
        if (output == null || output.isBlank())
            return List.of();
        return AllowList.clean(Arrays.asList(output.strip().split(",")));
    }

    // 현재 앱을 조회하고, 실패하거나 2초가 넘으면 빈 목록을 반환
    static List<String> runningApps() {
        try {
            ProcessBuilder pb = new ProcessBuilder("osascript", "-e",
                "tell application \"System Events\" to get name of every application process whose background only is false");
            pb.redirectError(ProcessBuilder.Redirect.DISCARD);
            Process p = pb.start();
            if (!p.waitFor(2, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                return List.of();
            }
            String out = new String(p.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
            return p.exitValue() == 0 ? parseProcessList(out) : List.of();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return List.of();
        } catch (IOException e) {
            return List.of();
        }
    }
}
