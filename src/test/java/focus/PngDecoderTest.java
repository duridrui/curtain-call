package focus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class PngDecoderTest {

    private static byte[] png(int w, int h, int argb) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++)
            for (int x = 0; x < w; x++)
                img.setRGB(x, y, argb);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    @Test
    void 반투명_픽셀을_premultiplied_ARGB로_푼다() throws IOException {
        int[] px = PngDecoder.decode(new ByteArrayInputStream(png(3, 2, 0x80FF4000)), 3, 2, null);
        assertEquals(6, px.length);
        assertEquals(0x80802000, px[5]);
    }

    @Test
    void 넘겨준_버퍼를_다시_쓴다() throws IOException {
        int[] reuse = new int[6];
        assertSame(reuse, PngDecoder.decode(new ByteArrayInputStream(png(3, 2, 0xFF000000)), 3, 2, reuse));
    }

    @Test
    void 크기가_다르면_실패한다() throws IOException {
        assertThrows(IllegalStateException.class, () -> PngDecoder.decode(new ByteArrayInputStream(png(4, 2, 0)), 3, 2, null));
    }

    @Test
    void PNG가_아니면_실패한다() {
        assertThrows(IllegalStateException.class, () -> PngDecoder.decode(new ByteArrayInputStream(new byte[] {1, 2, 3}), 3, 2, null));
    }

    @Test
    void 장_크기를_읽는다() throws IOException {
        assertEquals(java.util.List.of(5, 4), PngDecoder.size(new ByteArrayInputStream(png(5, 4, 0))));
    }
}