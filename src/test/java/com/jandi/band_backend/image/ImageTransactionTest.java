package com.jandi.band_backend.image;

import com.jandi.band_backend.config.ImageStorageProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class ImageTransactionTest {
    private S3Client client;
    private R2Service service;

    @BeforeEach void setUp() {
        client = mock(S3Client.class);
        var properties = new ImageStorageProperties();
        properties.setBucket("test-bucket");
        service = new R2Service(client, properties, new ImageUrls("https://images.example.com", "default.png"));
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach void clearTransaction() {
        TransactionSynchronizationManager.clearSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test void rollbackRemovesNewUploadAndPreservesPreviousImage() throws Exception {
        String url = service.uploadImage(new MockMultipartFile("image", "image.png", "image/png",
                ImageTestFiles.bytes("png", 0)), "profile");
        service.deleteImage("https://images.example.com/profile/previous.png");
        verify(client, never()).deleteObject(any(DeleteObjectRequest.class));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        verify(client).deleteObject(argThat((DeleteObjectRequest r) -> url.endsWith(r.key())));
        verify(client, never()).deleteObject(argThat((DeleteObjectRequest r) -> r.key().equals("profile/previous.png")));
    }

    @Test void commitAttemptsEveryDeletionAndSurfacesCleanupFailures() {
        service.deleteImage("https://images.example.com/profile/first.png");
        service.deleteImage("https://images.example.com/profile/second.png");
        service.deleteImage("https://images.example.com/profile/first.png");
        verifyNoInteractions(client);
        when(client.deleteObject(any(DeleteObjectRequest.class)))
                .thenThrow(new IllegalStateException("injected deletion failure")).thenReturn(null);
        assertThatThrownBy(() -> TransactionSynchronizationManager.getSynchronizations().getFirst().afterCommit())
                .isInstanceOf(ImageCleanupException.class).satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));
        verify(client, times(2)).deleteObject(any(DeleteObjectRequest.class));
        TransactionSynchronizationManager.getSynchronizations().forEach(s -> s.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verifyNoMoreInteractions(client);
    }
}
