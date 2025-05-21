package com.jandi.band_backend.image;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final AmazonS3Client amazonS3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    @Value("${cloud.aws.s3.url}")
    private String s3Url;

    public String uploadImage(MultipartFile file, String dirName) throws IOException {
        log.info("=== S3 Upload Debug Info ===");
        log.info("Bucket name: {}", bucket);
        log.info("S3 URL: {}", s3Url);
        log.info("File name: {}", file.getOriginalFilename());
        log.info("File size: {} bytes", file.getSize());
        log.info("Content type: {}", file.getContentType());
        log.info("Directory name: {}", dirName);

        String fileName = createFileName(file.getOriginalFilename(), dirName);
        log.info("Generated file key: {}", fileName);

        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentType(file.getContentType());
        objectMetadata.setContentLength(file.getSize());

        boolean bucketExists = amazonS3Client.doesBucketExistV2(bucket);
        log.info("Bucket exists: {}", bucketExists);

        try {
            AccessControlList acl = amazonS3Client.getBucketAcl(bucket);
            log.info("Bucket ACL: {}", acl.getGrantsAsList());
        } catch (Exception e) {
            log.error("Failed to get bucket ACL: {}", e.getMessage());
        }

        try (InputStream inputStream = file.getInputStream()) {
            PutObjectRequest putObjectRequest = new PutObjectRequest(bucket, fileName, inputStream, objectMetadata);
            
            log.info("Attempting to upload with PutObjectRequest: bucket={}, key={}", 
                    putObjectRequest.getBucketName(), 
                    putObjectRequest.getKey());
            
            amazonS3Client.putObject(putObjectRequest);
            log.info("Upload successful");
            
            return s3Url + "/" + fileName;
        } catch (Exception e) {
            log.error("Upload failed: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public void deleteImage(String fileUrl) {
        log.info("=== S3 Delete Debug Info ===");
        log.info("File URL to delete: {}", fileUrl);
        
        String fileName = fileUrl.replace(s3Url + "/", "");
        log.info("Extracted file key: {}", fileName);
        
        try {
            amazonS3Client.deleteObject(new DeleteObjectRequest(bucket, fileName));
            log.info("Delete successful");
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage(), e);
            throw e;
        }
    }

    public String getFileUrl(String fileName) {
        String url = amazonS3Client.getUrl(bucket, fileName).toString();
        log.info("Generated URL for file {}: {}", fileName, url);
        return url;
    }

    private String createFileName(String originalFileName, String dirName) {
        String fileName = dirName + "/" + UUID.randomUUID().toString() + getFileExtension(originalFileName);
        log.info("Created file name: {} from original: {}", fileName, originalFileName);
        return fileName;
    }

    private String getFileExtension(String fileName) {
        try {
            String extension = fileName.substring(fileName.lastIndexOf("."));
            log.info("File extension: {} from file: {}", extension, fileName);
            return extension;
        } catch (StringIndexOutOfBoundsException e) {
            log.error("Invalid file name format: {}", fileName);
            throw new IllegalArgumentException("잘못된 형식의 파일입니다.");
        }
    }
} 