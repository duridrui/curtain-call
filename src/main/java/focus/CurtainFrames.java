package focus;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

// Blender로 렌더한 커튼 장(PNG) 파일 목록을 들고, 닫힌 정도에 맞는 장 번호를 고른다.
// 폴더에 curtain_000.png(닫힘 0%) ~ curtain_899.png(닫힘 100%) 투명 PNG를 두면 닫힌 정도에 맞는 장을 고른다.
// 막 오름은 open_00.png ~ open_89.png를 흐른 시간 순서로 고른다 (BlenderCurtain이 네 폴더를 묶음)
final class CurtainFrames {
    private final List<Path> files;     // 닫힘 0%부터 100%까지 순서

    private CurtainFrames(List<Path> files) {
        this.files = files;
    }

    // 폴더의 curtain_번호.png를 번호순으로 읽음. 폴더가 없거나 한 장도 없으면 null (사진 커튼을 씀)
    static CurtainFrames fromFolder(Path dir) {
        return fromFolder(dir, "curtain_");
    }

    // 폴더의 <prefix>번호.png를 번호순으로 읽음 (막 오름은 "open_"). 번호는 자릿수를 맞춘 이름이라 이름순 = 번호순
    static CurtainFrames fromFolder(Path dir, String prefix) {
        if (!Files.isDirectory(dir))
            return null;
        String pattern = java.util.regex.Pattern.quote(prefix) + "\\d+\\.png";
        try (Stream<Path> list = Files.list(dir)) {
            List<Path> files = list.filter(p -> p.getFileName().toString().matches(pattern))
                .sorted()
                .toList();
            return files.isEmpty() ? null : new CurtainFrames(files);
        } catch (IOException e) {
            return null;
        }
    }

    // 닫힌 정도(0~1)에 맞는 장 번호
    static int indexFor(double closeProgress, int count) {
        double p = Math.max(0, Math.min(1, closeProgress));
        return (int) Math.round(p * (count - 1));
    }

    // 닫힘 장 번호. 다 닫혀 머무는 동안(1 이상)은 마지막 앞 장
    // Blender 렌더의 마지막 장(899)은 가운데 밑단이 반투명하게 벌어져 있어 898을 붙잡아 둔다
    static int closedIndex(double closeProgress, int count) {
        return closeProgress >= 1 && count >= 2 ? count - 2 : indexFor(closeProgress, count);
    }

    // 막 오름 장 번호: 시작부터 흐른 초를 연출 시간으로 나눠 고름 (진행도 곡선 없이 렌더된 움직임 그대로 재생)
    static int openIndexAt(double seconds, double durationSeconds, int count) {
        if (seconds <= 0)
            return 0;
        return (int) Math.min(count - 1, Math.floor(seconds / durationSeconds * count));
    }

    // 다시 열림 일정: fromProgress(닫힌 정도)에서 다 열림까지 durationSeconds 동안 fps마다 보일 닫힘 장 번호.
    // curve는 다시 열림 곡선(0~1을 0~1로, 앱은 Motion.EASE_IN_OUT). 시작할 때 정해 두고 미리 풀어 두면 늦게 뜨는 장이 없다
    static int[] reopenSchedule(double fromProgress, double durationSeconds, int fps, int count,
                                java.util.function.DoubleUnaryOperator curve) {
        int[] out = new int[(int) Math.round(durationSeconds * fps) + 1];
        for (int j = 0; j < out.length; j++) {
            double t = Math.min(1, j / (double) fps / durationSeconds);
            out[j] = closedIndex(fromProgress * (1 - curve.applyAsDouble(t)), count);
        }
        return out;
    }

    int count() {
        return files.size();
    }

    Path path(int index) {
        return files.get(index);
    }
}
