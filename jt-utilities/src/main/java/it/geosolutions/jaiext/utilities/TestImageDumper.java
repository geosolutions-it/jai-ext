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

    // Root folder for all test outputs
    private static final Path ROOT_OUT_DIR = Paths.get("src/test", "resources");

    private TestImageDumper() {}

    public static Path saveAsDeflateTiff(String testName, String suffix, RenderedImage image) {

        // Find the calling test class from the stack trace
        String testClassName = findCallingTestClass();
        System.out.println(testClassName);
        String packagePath = "";
        if (testClassName != null && testClassName.contains(".")) {
            testClassName = testClassName.replace("it.geosolutions.jaiext", "org.eclipse.imagen.media")
                    .replace("testclasses", "");
            String pkg = testClassName.substring(0, testClassName.lastIndexOf('.'));
            packagePath = pkg.replace('.', '/');
        }

        // Build final output dir
        Path outDir = ROOT_OUT_DIR;
        if (!packagePath.isEmpty()) {
            outDir = outDir.resolve(packagePath).resolve("test-data");
        }
        Path out;

        try {
            Files.createDirectories(outDir);
            String safeName = testName.replaceAll("Old|New", "").replaceAll("[^a-zA-Z0-9_.-]", "_");
            safeName += (suffix == null || suffix.trim().isEmpty()) ? "" : suffix;
            out = outDir.resolve(safeName + ".tif");
            System.out.println("Saving image to: " + out.toAbsolutePath());

            // Pick a TIFF writer
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("tiff");
            if (!writers.hasNext()) {
                throw new IllegalStateException(
                        "No TIFF ImageWriter found. Add a TIFF plugin (e.g., jai-imageio or TwelveMonkeys).");
            }
            ImageWriter writer = writers.next();

            try (ImageOutputStream ios = ImageIO.createImageOutputStream(out.toFile())) {
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
        return out;
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

    private static String findCallingTestClass() {
        StackTraceElement[] stack = Thread.currentThread().getStackTrace();
        String candidate = null;

        for (StackTraceElement el : stack) {
            String cls = el.getClassName();
            if (cls.startsWith("java.") || cls.startsWith("sun.") ||
                    cls.equals(TestImageDumper.class.getName()) ||
                    cls.endsWith("TestBase")) {
                continue; // skip infra/base classes
            }
            candidate = cls;
            break;
        }
        return candidate;
    }
}
