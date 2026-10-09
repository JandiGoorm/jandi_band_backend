package com.jandi.band_backend.image;

import com.jandi.band_backend.global.exception.BadRequestException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.Arguments;
import org.springframework.mock.web.MockMultipartFile;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageUploadValidatorTest {
    static Stream<Arguments> invalidFiles() throws Exception {
        return Stream.of(
                Arguments.of("empty.png", "image/png", new byte[0]),
                Arguments.of("plain.txt", "text/plain", "text".getBytes()),
                Arguments.of("fake.png", "image/png", "not an image".getBytes()),
                Arguments.of("renamed.jpg", "image/jpeg", ImageTestFiles.bytes("png", 0)),
                Arguments.of("wrong-mime.png", "image/jpeg", ImageTestFiles.bytes("png", 0)),
                Arguments.of("large.png", "image/png", ImageTestFiles.bytes("png", 10 * 1024 * 1024 + 1)),
                Arguments.of("truncated.png", "image/png", new byte[]{(byte) 137, 80, 78, 71, 13, 10, 26, 10}));
    }

    @ParameterizedTest
    @MethodSource("invalidFiles")
    void rejectsEmptyForgedMismatchedOversizedAndTruncatedImages(String name, String type, byte[] bytes) {
        assertThatThrownBy(() -> ImageUploadValidator.validate(new MockMultipartFile("image", name, type, bytes)))
                .isInstanceOf(BadRequestException.class);
    }
}
