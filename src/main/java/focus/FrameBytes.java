package focus;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceArray;

final class FrameBytes {
    private static final byte[] EMPTY = new byte[0];
    private final List<Path> files;
    private final AtomicReferenceArray<ByteBuffer> loaded;
    private final AtomicLong loadedBytes = new AtomicLong();

    FrameBytes(List<Path> files) {
        this.files = List.copyOf(files);
        this.loaded = new AtomicReferenceArray<>(files.size());
    }
    // 동시에 읽어도 한 스레드만 같은 장을 저장하게 하는 동작
    void loadAll() {
        for (int i = 0; i < files.size(); i++)
            if (loaded.get(i) == null && loaded.compareAndSet(i, null, offHeap(read(files.get(i)))))
                loadedBytes.addAndGet(loaded.get(i).capacity());
    }

    byte[] get(int i) {
        ByteBuffer b = loaded.get(i);
        if (b == null)
            return read(files.get(i));
        byte[] out = new byte[b.capacity()];
        b.duplicate().clear().get(out);
        return out;
    }

    java.io.InputStream stream(int i) {
        ByteBuffer b = loaded.get(i);
        if (b == null)
            return new java.io.ByteArrayInputStream(read(files.get(i)));
        ByteBuffer d = b.duplicate().clear();
        return new java.io.InputStream() {
            @Override
            public int read() {
                return d.hasRemaining() ? d.get() & 255 : -1;
            }

            @Override
            public int read(byte[] dst, int off, int len) {
                if (len == 0)
                    return 0;
                if (!d.hasRemaining())
                    return -1;
                int n = Math.min(len, d.remaining());
                d.get(dst, off, n);
                return n;
            }
        };
    }
    // 힙 밖 버퍼로 GC에 의한 멈춤을 줄임
    private static ByteBuffer offHeap(byte[] bytes) {
        ByteBuffer b = ByteBuffer.allocateDirect(bytes.length);
        b.put(bytes).flip();
        return b;
    }

    int loadedCount() {
        int n = 0;
        for (int i = 0; i < loaded.length(); i++)
            if (loaded.get(i) != null)
                n++;
        return n;
    }

    long loadedBytes() {
        return loadedBytes.get();
    }

    private static byte[] read(Path file) {
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            return EMPTY;
        }
    }
}
