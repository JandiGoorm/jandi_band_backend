package com.jandi.band_backend.image;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

public final class ImageUrls {
    private final URI publicBase;
    private final List<URI> managedBases;
    private final String defaultClubKey;

    public ImageUrls(String publicUrl, List<String> legacyPublicUrls, String defaultClubKey) {
        this.publicBase = parseBase(publicUrl);
        var bases = new ArrayList<URI>();
        bases.add(publicBase);
        legacyPublicUrls.stream().filter(url -> !url.isBlank()).map(ImageUrls::parseBase).forEach(bases::add);
        bases.sort(Comparator.comparingInt((URI base) -> base.getPath().length()).reversed());
        this.managedBases = List.copyOf(bases);
        validateKey(defaultClubKey);
        this.defaultClubKey = defaultClubKey;
    }

    public String publicUrl(String key) {
        validateKey(key);
        try {
            return publicBase.toASCIIString() + new URI(null, null, "/" + key, null).toASCIIString();
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid image key", e);
        }
    }

    public String defaultClubPhotoUrl() {
        return publicUrl(defaultClubKey);
    }

    public boolean isDefaultClubKey(String key) {
        return defaultClubKey.equals(key);
    }

    public Optional<String> managedKey(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            URI url = new URI(value);
            if (url.getHost() == null || url.getUserInfo() != null
                    || url.getRawQuery() != null || url.getRawFragment() != null) {
                return Optional.empty();
            }
            String path = decodedPath(url);
            for (URI base : managedBases) {
                String prefix = base.getPath() + "/";
                if (base.getScheme().equalsIgnoreCase(url.getScheme())
                        && base.getHost().equalsIgnoreCase(url.getHost())
                        && port(base) == port(url) && path.startsWith(prefix)) {
                    String key = path.substring(prefix.length());
                    validateKey(key);
                    return Optional.of(key);
                }
            }
        } catch (URISyntaxException | IllegalArgumentException | CharacterCodingException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    public static void validateKey(String key) {
        if (key == null || key.isBlank() || key.contains("\\")
                || key.codePoints().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("Invalid image key");
        }
        for (String segment : key.split("/", -1)) {
            if (segment.isEmpty() || segment.equals(".") || segment.equals("..")) {
                throw new IllegalArgumentException("Invalid image key path");
            }
        }
    }

    private static URI parseBase(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Image public URL is required");
        }
        URI uri = URI.create(value.replaceAll("/+$", ""));
        if (uri.getHost() == null || !("https".equalsIgnoreCase(uri.getScheme())
                || "http".equalsIgnoreCase(uri.getScheme())) || uri.getUserInfo() != null
                || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalArgumentException("Invalid image public URL");
        }
        try {
            String path = decodedPath(uri);
            if (!path.isEmpty()) {
                validateKey(path.substring(1));
            }
        } catch (CharacterCodingException e) {
            throw new IllegalArgumentException("Invalid image public URL encoding", e);
        }
        return uri;
    }

    private static String decodedPath(URI uri) throws CharacterCodingException {
        String raw = URI.create(uri.toASCIIString()).getRawPath();
        var bytes = new ByteArrayOutputStream();
        for (int i = 0; i < raw.length(); i++) {
            if (raw.charAt(i) == '%') {
                bytes.write(Integer.parseInt(raw.substring(i + 1, i + 3), 16));
                i += 2;
            } else {
                bytes.write(raw.charAt(i));
            }
        }
        return StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes.toByteArray())).toString();
    }

    private static int port(URI uri) {
        return uri.getPort() != -1 ? uri.getPort() : ("https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80);
    }
}
