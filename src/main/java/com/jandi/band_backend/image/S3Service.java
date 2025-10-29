package com.jandi.band_backend.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

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

        try {
            // 버킷 존재 여부 확인
            HeadBucketRequest headBucketRequest = HeadBucketRequest.builder()
                    .bucket(bucket)
                    .build();
            s3Client.headBucket(headBucketRequest);
            log.info("Bucket exists: {}", bucket);
        } catch (NoSuchBucketException e) {
            log.error("Bucket does not exist: {}", bucket);
            throw new RuntimeException("S3 bucket does not exist: " + bucket);
        }

        try {
            // PutObject 요청 생성
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            log.info("Attempting to upload with PutObjectRequest: bucket={}, key={}", 
                    bucket, fileName);

            // 파일 업로드
            s3Client.putObject(putObjectRequest, 
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
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
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(fileName)
                    .build();
            
            s3Client.deleteObject(deleteObjectRequest);
            log.info("Delete successful");
        } catch (Exception e) {
            log.error("Delete failed: {}", e.getMessage(), e);
            throw e;
        }
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