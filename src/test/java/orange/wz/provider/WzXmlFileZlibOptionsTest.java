package orange.wz.provider;

import orange.wz.provider.properties.WzCanvasProperty;
import orange.wz.provider.properties.WzPngZlibCompressMode;
import orange.wz.provider.tools.MediaExportType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.zip.Deflater;

import static org.junit.jupiter.api.Assertions.*;

class WzXmlFileZlibOptionsTest {
    @TempDir
    Path tempDir;

    @Test
    void xmlImportShouldUseConfiguredZlibOptionsWhenCreatingCanvasPngPayload() throws Exception {
        Path xmlPath = writeCanvasXml();
        WzXmlFile defaultCompression = parseXml(xmlPath, Deflater.DEFAULT_COMPRESSION, WzPngZlibCompressMode.DEFAULT);
        WzXmlFile noCompression = parseXml(xmlPath, Deflater.NO_COMPRESSION, WzPngZlibCompressMode.DEFAULT);

        WzCanvasProperty defaultCanvas = (WzCanvasProperty) defaultCompression.getChildren().getFirst();
        WzCanvasProperty noCompressionCanvas = (WzCanvasProperty) noCompression.getChildren().getFirst();

        assertTrue(defaultCanvas.getCompressedPngStorageLength() < noCompressionCanvas.getCompressedPngStorageLength());
        assertEquals(0xFF336699, defaultCanvas.getPngImage(false).getRGB(0, 0));
        assertEquals(0xFF336699, noCompressionCanvas.getPngImage(false).getRGB(0, 0));
    }

    private WzXmlFile parseXml(Path xmlPath, int level, WzPngZlibCompressMode mode) {
        WzXmlFile xml = new WzXmlFile(xmlPath.getFileName().toString(), xmlPath.toString(), "empty",
                WzAESConstant.WZ_EMPTY_IV, WzAESConstant.DEFAULT_KEY);
        xml.setMeType(MediaExportType.BASE64);
        xml.setPngWriteOptions(new orange.wz.provider.properties.WzPngWriteOptions(level, mode));
        assertTrue(xml.parse());
        return xml;
    }

    private Path writeCanvasXml() throws Exception {
        BufferedImage image = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                image.setRGB(x, y, 0xFF336699);
            }
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", out);
        String basedata = Base64.getEncoder().encodeToString(out.toByteArray());
        String xml = """
                <?xml version=\"1.0\" encoding=\"UTF-8\"?>
                <imgdir name=\"sample.img\" media=\"BASE64\">
                  <canvas name=\"c\" format=\"2\" scale=\"0\" basedata=\"%s\" />
                </imgdir>
                """.formatted(basedata);
        Path xmlPath = tempDir.resolve("sample.img.xml");
        Files.writeString(xmlPath, xml, StandardCharsets.UTF_8);
        return xmlPath;
    }
}
