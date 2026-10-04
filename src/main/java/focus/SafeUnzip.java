
package focus;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

final class SafeUnzip {
    private SafeUnzip() {
    }

    static void extract(Path zip, Path dest) throws IOException {
        Path root = dest.toAbsolutePath().normalize();
        Files.createDirectories(root);
        try (InputStream in = Files.newInputStream(zip); ZipInputStream z = new ZipInputStream(in)) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                Path target = root.resolve(e.getName()).normalize();
                if (!target.startsWith(root) || e.getName().startsWith("/") || e.getName().contains("\\"))
                    throw new IOException("zip 항목이 풀 폴더 밖을 가리킴(경로 탈출): " + e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(target);
                    continue;
                }
                Files.createDirectories(target.getParent());
                Files.copy(z, target, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
