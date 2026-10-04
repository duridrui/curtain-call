package focus;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

final class PngDecoder {
    static {
        ImageIO.setUseCache(false);
    }
    // 스레드별 읽개와 그림 버퍼를 새로 만들지 않고 재사용하는 동작
    private static final class Slot {
        final ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
        ImageTypeSpecifier type;
        BufferedImage dest;
    }

    private static final ThreadLocal<Slot> SLOT = ThreadLocal.withInitial(Slot::new);

    private PngDecoder() {
    }

    static int[] decode(InputStream in, int width, int height, int[] reuse) {
        Slot slot = SLOT.get();
        try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            slot.reader.setInput(iis, true, true);
            if (slot.reader.getWidth(0) != width || slot.reader.getHeight(0) != height)
                throw new IllegalStateException("커튼 장 크기가 다름: " + slot.reader.getWidth(0) + "x" + slot.reader.getHeight(0));
            ImageTypeSpecifier type = slot.reader.getImageTypes(0).next();
            if (slot.dest == null || !type.equals(slot.type) || slot.dest.getWidth() != width || slot.dest.getHeight() != height) {
                slot.type = type;
                slot.dest = type.createBufferedImage(width, height);
            }

            ImageReadParam param = slot.reader.getDefaultReadParam();
            param.setDestination(slot.dest);
            slot.reader.read(0, param);
            int[] out = reuse != null && reuse.length == width * height ? reuse : new int[width * height];
            toArgbPre(slot.dest, out);
            return out;
        } catch (IOException | RuntimeException e) {
            throw e instanceof IllegalStateException ise ? ise : new IllegalStateException("커튼 장을 풀지 못함", e);
        } finally {
            slot.reader.setInput(null);
        }
    }

    static List<Integer> size(InputStream in) {
        Slot slot = SLOT.get();
        try (ImageInputStream iis = ImageIO.createImageInputStream(in)) {
            slot.reader.setInput(iis, true, true);
            return List.of(slot.reader.getWidth(0), slot.reader.getHeight(0));
        } catch (IOException | RuntimeException e) {
            throw new IllegalStateException("커튼 장 크기를 읽지 못함", e);
        } finally {
            slot.reader.setInput(null);
        }
    }

    private static void toArgbPre(BufferedImage img, int[] out) {
        if (img.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            byte[] d = ((DataBufferByte) img.getRaster().getDataBuffer()).getData();
            for (int i = 0, j = 0; i < out.length; i++, j += 4)
                out[i] = pre(d[j] & 255, d[j + 3] & 255, d[j + 2] & 255, d[j + 1] & 255);
            return;
        }
        img.getRGB(0, 0, img.getWidth(), img.getHeight(), out, 0, img.getWidth());
        for (int i = 0; i < out.length; i++) {
            int p = out[i];
            out[i] = pre(p >>> 24, (p >> 16) & 255, (p >> 8) & 255, p & 255);
        }
    }
    // RGB에 알파를 곱해 미리 곱해진 ARGB 값으로 바꾸는 계산
    private static int pre(int a, int r, int g, int b) {
        if (a == 255)
            return 0xFF000000 | r << 16 | g << 8 | b;
        if (a == 0)
            return 0;
        return a << 24 | (r * a + 127) / 255 << 16 | (g * a + 127) / 255 << 8 | (b * a + 127) / 255;
    }
}
