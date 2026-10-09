package com.jandi.band_backend.image;

import com.jandi.band_backend.config.ImageStorageProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;
    private final ImageStorageProperties properties;
    private final ImageUrls imageUrls;

    public String uploadImage(MultipartFile file, String dirName) throws IOException {
        ImageUrls.validateKey(dirName);
        String originalName = file.getOriginalFilename();
        int dot = originalName == null ? -1 : originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            throw new IllegalArgumentException("잘못된 형식의 파일입니다.");
        }
        String extension = originalName.substring(dot);
        if (!extension.matches("\\.[a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("잘못된 형식의 파일입니다.");
        }
        String key = dirName + "/" + UUID.randomUUID() + extension;
        String url = imageUrls.publicUrl(key);
        String contentType = file.getContentType() == null ? "application/octet-stream" : file.getContentType();
        var request = PutObjectRequest.builder().bucket(properties.getBucket()).key(key)
                .contentType(contentType).contentLength(file.getSize()).build();
        // Repeatable multipart streams for SDK signing and transfer
        var streams = ContentStreamProvider.fromInputStreamSupplier(() -> {
            try {
                return file.getInputStream();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        s3Client.putObject(request, RequestBody.fromContentProvider(streams, file.getSize(), contentType));
        return url;
    }

    public void deleteImage(String fileUrl) {
        imageUrls.managedKey(fileUrl).filter(key -> !imageUrls.isDefaultClubKey(key))
                .ifPresent(key -> s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(properties.getBucket()).key(key).build()));
    }
}
