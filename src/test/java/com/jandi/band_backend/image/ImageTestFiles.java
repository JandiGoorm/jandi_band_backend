package com.jandi.band_backend.image;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Base64;

final class ImageTestFiles {
    static byte[] bytes(String format, int minimumSize) throws IOException {
        if (format.equals("webp")) {
            return Base64.getDecoder().decode("UklGRhwAAABXRUJQVlA4TA8AAAAvAAAAAAcQ/Y/+ByKi/wEA");
        }
        var output = new ByteArrayOutputStream();
        if (!ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), format, output)) {
            throw new IllegalArgumentException("Unsupported test format");
        }
        byte[] bytes = output.toByteArray();
        return Arrays.copyOf(bytes, Math.max(bytes.length, minimumSize));
    }
}
