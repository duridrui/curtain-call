package focus;

import java.nio.file.Path;

// Blender 커튼 프레임 폴더 하나: 닫힘 left/right curtain_NNN + 막 오름 open_left/open_right open_NN.
// 폴더가 없거나 장 수가 어긋나면 null을 돌려주고 사진 커튼(curtain.jpg)을 씀.
// right/, open_right/는 이미 오른쪽 그림(렌더 뒤 좌우 반전)이라 앱에서 뒤집지 않는다
record BlenderCurtain(CurtainFrames left, CurtainFrames right, CurtainFrames openLeft, CurtainFrames openRight) {

    // 개발용: -Dcurtain.frames=폴더 를 주면 그 폴더를 씀 (없으면 null). 그림은 창 반쪽 크기에 맞춰 늘린다
    static final Path ROOT = System.getProperty("curtain.frames") == null ? null : Path.of(System.getProperty("curtain.frames"));

    static BlenderCurtain load(Path root) {
        if (root == null)
            return null;
        CurtainFrames left = CurtainFrames.fromFolder(root.resolve("left"));
        CurtainFrames right = CurtainFrames.fromFolder(root.resolve("right"));
        CurtainFrames openLeft = CurtainFrames.fromFolder(root.resolve("open_left"), "open_");
        CurtainFrames openRight = CurtainFrames.fromFolder(root.resolve("open_right"), "open_");
        if (left == null || right == null || openLeft == null || openRight == null)
            return null;
        if (left.count() != right.count() || openLeft.count() != openRight.count())
            return null;    // 한쪽만 덜 복사됐으면 양쪽 장이 어긋나 보이므로 쓰지 않음
        return new BlenderCurtain(left, right, openLeft, openRight);
    }
}
