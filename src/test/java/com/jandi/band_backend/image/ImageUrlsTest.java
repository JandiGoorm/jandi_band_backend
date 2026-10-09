package com.jandi.band_backend.image;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ImageUrlsTest {
    private final ImageUrls urls = new ImageUrls("https://cdn.example.com/assets/",
            List.of("https://old.example.com/bucket"), "club-photo/rhythmeet.webp");

    @ParameterizedTest
    @ValueSource(strings = {"photo/a.jpg", "한글/공백 + 더하기.jpg", "photo/100%real.jpg", "photo/literal%2F.jpg"})
    void encodesAndDecodesKeysExactlyOnce(String key) {
        String url = urls.publicUrl(key);
        assertThat(url).startsWith("https://cdn.example.com/assets/").doesNotContain(" ", "+");
        if (key.contains("+")) {
            assertThat(url).contains("%2B");
        }
        assertThat(urls.managedKey(url)).contains(key);
        assertThat(urls.managedKey(url.replace("https://cdn.example.com/assets", "https://old.example.com/bucket")))
                .contains(key);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://cdn.example.com/assets-other/photo.jpg", "https://cdn.example.com.evil/assets/photo.jpg",
            "https://cdn.example.com:444/assets/photo.jpg", "https://user@cdn.example.com/assets/photo.jpg",
            "http://cdn.example.com/assets/photo.jpg", "https://cdn.example.com/assets/%2e%2e/photo.jpg",
            "https://cdn.example.com/assets//photo.jpg", "https://cdn.example.com/assets/%5cphoto.jpg",
            "https://cdn.example.com/assets/photo.jpg#fragment", "https://cdn.example.com/assets/%00photo.jpg",
            "https://cdn.example.com/assets/bad%ZZ.jpg", "https://cdn.example.com/assets/%FF.jpg", "not a URL"})
    void rejectsUrlsOutsideConfiguredOriginAndPath(String url) {
        assertThat(urls.managedKey(url)).isEmpty();
    }
}
