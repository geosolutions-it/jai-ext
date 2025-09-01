package it.geosolutions.jaiext.utilities;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import javax.media.jai.RenderedOp;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;

public class TestImageDumper {

    private TestImageDumper() {}

    public static void saveAsDeflateTiff(Path path, RenderedImage image) {

        try {

            // Pick a TIFF writer
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
            if (!writers.hasNext()) {
                throw new IllegalStateException(
                        "No TIFF ImageWriter found. Add a TIFF plugin (e.g., jai-imageio or TwelveMonkeys).");
            }
            ImageWriter writer = writers.next();

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(path.toFile())) {
                writer.setOutput(ios);

                ImageWriteParam param = writer.getDefaultWriteParam();
                // Explicitly request Deflate compression if supported
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                // Common names: "Deflate", "ZLib". We try to pick Deflate if available.
                String deflate = findCompressionTypeIgnoreCase(param, "Deflate", "ZLib", "ZIP");
                if (deflate != null) {
                    param.setCompressionType(deflate);
                } else {
                    // Fall back to writer default if Deflate isn't present
                    param.setCompressionMode(ImageWriteParam.MODE_COPY_FROM_METADATA);
                }

                // If your RenderedImage already has metadata (possibly GeoTIFF), pass it through
                IIOMetadata metadata = writer.getDefaultImageMetadata(ImageTypeSpecifier.createFromRenderedImage(image), param);
                // If you carry custom/GeoTIFF metadata from upstream, you could merge it here.

                writer.write(null, new IIOImage(image, null, metadata), param);
            } finally {
                writer.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to save image: " + e.getMessage(), e);
        }
        if (image instanceof RenderedOp) {
            ((RenderedOp) image).dispose();
        }
    }

    private static String findCompressionTypeIgnoreCase(ImageWriteParam p, String... wanted) {
        String[] types = p.getCompressionTypes();
        if (types == null) return null;
        for (String w : wanted) {
            for (String t : types) {
                if (t.equalsIgnoreCase(w)) return t;
            }
        }
        return null;
    }
}
