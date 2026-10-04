package focus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

// 그릴 장보다 몇 장 앞서 디코드해 두는 창. 장 하나 디코드에 30~80ms 걸려서 그릴 때 디코드하면 늦게 뜬다.
// 움직이는 방향으로 ahead장(빨리 움직이면 건너뛸 간격으로)을 미리 디코드하고, 고정한 장(닫힘 시작, 다 닫힘, 막 오름 시작)은 늘 들고 있음.
// 지난 장은 가까운 순서로 spare장까지만 남겨 두고 놓는다 (풀린 RGBA 전체 적재 금지: 반쪽 1장 약 3.4MB).
// FX 스레드에서만 부른다. decode는 executor 스레드에서 돌고, 끝나면 onReady(장 번호)를 그 스레드에서 부름
final class FrameWindow<T> {
    private final int count;
    private final int ahead;
    private final int spare;
    private final IntFunction<T> decode;
    private final Executor executor;
    private final IntConsumer onReady;
    private final Consumer<T> recycle;
    private final TreeMap<Integer, CompletableFuture<T>> held = new TreeMap<>();
    private final Set<Integer> pinned = new HashSet<>();

    FrameWindow(int count, int ahead, int spare, IntFunction<T> decode, Executor executor, IntConsumer onReady) {
        this(count, ahead, spare, decode, executor, onReady, value -> { });
    }

    // recycle: 다 풀린 장을 놓을 때 그 값(픽셀 배열)을 돌려받아 다음 디코드에 다시 씀
    FrameWindow(int count, int ahead, int spare, IntFunction<T> decode, Executor executor, IntConsumer onReady, Consumer<T> recycle) {
        this.recycle = recycle;
        this.count = count;
        this.ahead = ahead;
        this.spare = spare;
        this.decode = decode;
        this.executor = executor;
        this.onReady = onReady;
    }

    // 늘 들고 있을 장 (바로 디코드 시작)
    void pin(int... indices) {
        for (int i : indices) {
            pinned.add(i);
            start(i);
        }
    }

    // index 장을 그릴 차례. step은 다음 펄스에 움직일 장 수(부호 = 방향, 0이면 1로 봄)
    void aim(int index, int step) {
        int s = step == 0 ? 1 : step;
        List<Integer> want = new ArrayList<>();
        for (int k = 0; k <= ahead; k++) {
            int i = index + k * s;
            if (i >= 0 && i < count)
                want.add(i);
        }
        aimList(want);
    }

    // 정해 둔 장 목록(첫 장 = 지금 그릴 장)을 순서대로 풀어 둠 (다시 열림처럼 간격이 고르지 않을 때)
    void aimList(List<Integer> want) {
        int index = want.get(0);
        List<Integer> others = new ArrayList<>();
        for (int i : held.keySet())
            if (!want.contains(i) && !pinned.contains(i))
                others.add(i);
        others.sort(Comparator.comparingInt(i -> Math.abs(i - index)));
        for (int k = spare; k < others.size(); k++)
            drop(others.get(k));
        for (int i : want)
            start(i);
    }

    private void start(int i) {
        if (held.containsKey(i))
            return;
        // 디코드 future 자체를 들고 있어야 놓을 때 cancel이 대기열의 디코드를 건너뛰게 함
        // (whenComplete가 돌려준 뒤 future를 cancel하면 디코드는 그대로 돌아 대기열이 밀렸다)
        CompletableFuture<T> f = CompletableFuture.supplyAsync(() -> decode.apply(i), executor);
        f.thenRun(() -> onReady.accept(i));
        held.put(i, f);
    }

    // 디코드가 끝난 장, 아직이거나 실패했으면 null
    T get(int i) {
        CompletableFuture<T> f = held.get(i);
        return f == null || !f.isDone() ? null : get(f);
    }

    // 움직임이 끝남: 고정한 장만 남김
    void release() {
        for (int i : held())
            if (!pinned.contains(i))
                drop(i);
    }

    // 장을 놓음: 다 풀렸으면 값을 돌려주고, 아직이면 디코드를 건너뛰게 함
    private void drop(int i) {
        CompletableFuture<T> f = held.remove(i);
        T value = f.isDone() ? get(f) : null;
        if (value != null)
            recycle.accept(value);
        else
            f.cancel(false);
    }

    private static <T> T get(CompletableFuture<T> f) {
        return f.isCompletedExceptionally() || f.isCancelled() ? null : f.join();
    }

    List<Integer> held() {
        return new ArrayList<>(held.keySet());
    }
}
