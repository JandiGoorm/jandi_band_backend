package com.jandi.band_backend.image;

import com.jandi.band_backend.global.exception.BadRequestException;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;

final class ImageUploadValidator {
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final long MAX_PIXELS = 40_000_000;
    private static final Map<String, String> TYPES = Map.of(
            "jpg", "jpeg", "jpeg", "jpeg", "png", "png", "gif", "gif", "webp", "webp");

    static String validate(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_BYTES) {
            throw new BadRequestException("이미지는 1바이트 이상 10MB 이하여야 합니다.");
        }
        String name = file.getOriginalFilename();
        int dot = name == null ? -1 : name.lastIndexOf('.');
        String extension = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        String format = TYPES.get(extension);
        if (format == null || !("image/" + format).equalsIgnoreCase(file.getContentType())) {
            throw new BadRequestException("파일 확장자와 Content-Type이 일치하는 JPEG, PNG, GIF, WebP만 가능합니다.");
        }
        try (var stream = file.getInputStream(); var input = new MemoryCacheImageInputStream(stream)) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new BadRequestException("이미지 데이터를 읽을 수 없습니다.");
            var reader = readers.next();
            try {
                reader.setInput(input);
                if (!format.equalsIgnoreCase(reader.getFormatName())) {
                    throw new BadRequestException("실제 이미지 형식이 파일 형식과 다릅니다.");
                }
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || (long) width * height > MAX_PIXELS) {
                    throw new BadRequestException("이미지는 4천만 픽셀 이하여야 합니다.");
                }
                if (reader.read(0) == null) throw new BadRequestException("손상된 이미지입니다.");
            } finally {
                reader.dispose();
            }
        } catch (IOException | IndexOutOfBoundsException e) {
            throw new BadRequestException("손상된 이미지 데이터를 읽을 수 없습니다.");
        }
        return "." + extension;
    }
}
