package com.jandi.band_backend.image;

import com.jandi.band_backend.config.ImageStorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.ContentStreamProvider;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class R2Service {
    private final S3Client s3Client;
    private final ImageStorageProperties properties;
    private final ImageUrls imageUrls;

    public String uploadImage(MultipartFile file, String dirName) throws IOException {
        ImageUrls.validateKey(dirName);
        String extension = ImageUploadValidator.validate(file);
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
        var transaction = currentTransaction();
        if (transaction != null) transaction.uploads.add(key);
        s3Client.putObject(request, RequestBody.fromContentProvider(streams, file.getSize(), contentType));
        return url;
    }

    public void deleteImage(String fileUrl) {
        imageUrls.managedKey(fileUrl).filter(key -> !imageUrls.isDefaultClubKey(key))
                .ifPresent(key -> {
                    var transaction = currentTransaction();
                    if (transaction == null) deleteObject(key);
                    else transaction.deletions.add(key);
                });
    }

    private void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.getBucket()).key(key).build());
    }

    private StorageTransaction currentTransaction() {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
                || !TransactionSynchronizationManager.isSynchronizationActive()) return null;
        for (var synchronization : TransactionSynchronizationManager.getSynchronizations()) {
            if (synchronization instanceof StorageTransaction transaction) return transaction;
        }
        var transaction = new StorageTransaction();
        TransactionSynchronizationManager.registerSynchronization(transaction);
        return transaction;
    }

    private final class StorageTransaction implements TransactionSynchronization {
        private final Set<String> uploads = new LinkedHashSet<>();
        private final Set<String> deletions = new LinkedHashSet<>();

        @Override public void afterCommit() {
            var failure = new ImageCleanupException();
            for (String key : deletions) {
                try { deleteObject(key); }
                catch (RuntimeException e) {
                    log.error("IMAGE_CLEANUP_FAILED phase=commit bucket={} key={}", properties.getBucket(), key, e);
                    failure.addSuppressed(e);
                }
            }
            if (failure.getSuppressed().length > 0) throw failure;
        }

        @Override public void afterCompletion(int status) {
            if (status != STATUS_ROLLED_BACK) return;
            for (String key : uploads) {
                try { deleteObject(key); }
                catch (RuntimeException e) {
                    log.error("IMAGE_CLEANUP_FAILED phase=rollback bucket={} key={}", properties.getBucket(), key, e);
                }
            }
        }
    }
}
