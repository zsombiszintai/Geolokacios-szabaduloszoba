package com.cityscape.geoszabaduloszobabackend.service;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AvatarStorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucketName}")
    private String bucketName;

    @Value("${minio.url}")
    private String minioUrl;

    /**
     * Alkalmazás indulásakor ellenőrzi, hogy létezik-e a bucket, ha nem, létrehozza.
     */
    @PostConstruct
    public void init() {
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("MinIO bucket létrehozva: {}", bucketName);
            }
        } catch (Exception e) {
            log.error("Hiba a MinIO inicializálásakor: {}", e.getMessage());
        }
    }

    /**
     * Generál egy egyedi kulcsot a fájlnak az ID és kiterjesztés alapján.
     */
    public String newKey(Long id, String extension) {
        return "avatar_" + id + "_" + System.currentTimeMillis() + "." + extension;
    }

    /**
     * Feltölti a nyers fájlt a MinIO-ba.
     */
    public void uploadRaw(String key, MultipartFile file, String contentType) throws Exception {
        try (InputStream is = file.getInputStream()) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(is, file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );
            log.info("Sikeres feltöltés: {} (típus: {})", key, contentType);
        }
    }

    /**
     * Létrehoz egy elérhető URL-t.
     * Refaktorálási szempont: A Presigned URL biztonságosabb és
     * megoldja a "localhost" problémát is.
     */
    public String publicUrl(String key) {
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucketName)
                            .object(key)
                            .expiry(7, TimeUnit.DAYS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Nem sikerült URL-t generálni a kulcshoz: {}", key);
            return null;
        }
    }
}