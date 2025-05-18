package com.jandi.band_backend.image.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.jandi.band_backend.image.service.S3Service;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
public class ImageController {

    private final S3Service s3Service;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam("dirName") String dirName) throws IOException {
        String imageUrl = s3Service.uploadImage(file, dirName);
        return ResponseEntity.ok(imageUrl);
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteImage(@RequestParam("fileUrl") String fileUrl) {
        s3Service.deleteImage(fileUrl);
        return ResponseEntity.ok().build();
    }
} 