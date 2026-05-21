package com.back.common.service;

import com.back.config.cloudflare.RCloudflareR2Properties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.transfer.s3.S3TransferManager;
import software.amazon.awssdk.transfer.s3.model.UploadFileRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class R2StorageService {
    
    private static final long MULTIPART_THRESHOLD = 10 * 1024 * 1024L;

    private final S3Client s3Client;
    private final S3TransferManager transferManager;
    private final S3Presigner s3Presigner;
    private final RCloudflareR2Properties props;

    public String uploadFile(MultipartFile file, String key) throws IOException {
        if (file.getSize() >= MULTIPART_THRESHOLD) {
            return uploadMultipart(file, key);
        }
        return uploadSimple(file, key);
    }

    private String uploadSimple(MultipartFile file, String key) throws IOException {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(props.bucketName())
                        .key(key)
                        .contentType(file.getContentType())
                        .contentLength(file.getSize())
                        .cacheControl("public, max-age=31536000, immutable")
                        .build(),
                RequestBody.fromInputStream(file.getInputStream(), file.getSize())
        );
        log.debug("Simple upload OK: {}", key);
        return buildPublicUrl(key);
    }

    private String uploadMultipart(MultipartFile file, String key) throws IOException {
        Path tmp = Files.createTempFile("r2-", getExtension(file.getOriginalFilename()));
        try {
            file.transferTo(tmp);
            transferManager.uploadFile(UploadFileRequest.builder()
                            .putObjectRequest(r -> r
                                    .bucket(props.bucketName())
                                    .key(key)
                                    .contentType(file.getContentType())
                                    .cacheControl("public, max-age=31536000, immutable"))
                            .source(tmp)
                            .build())
                    .completionFuture()
                    .join();
            log.debug("Multipart upload OK: {}", key);
            return buildPublicUrl(key);
        } finally {
            Files.deleteIfExists(tmp);
        }
    }

    public void deleteFile(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(props.bucketName())
                .key(key)
                .build());
        log.debug("Deleted: {}", key);
    }

    public String generatePresignedUrl(String key, Duration duration) {
        return s3Presigner.presignGetObject(
                GetObjectPresignRequest.builder()
                        .signatureDuration(duration)
                        .getObjectRequest(r -> r.bucket(props.bucketName()).key(key))
                        .build()
        ).url().toString();
    }

    public String buildPublicUrl(String key) {
        return props.publicUrl() + "/" + key;
    }


    public String extractKeyFromUrl(String fileUrl) {
        String base = props.publicUrl();
        if (fileUrl.startsWith(base)) {
            return fileUrl.substring(base.length() + 1);
        }
        throw new IllegalArgumentException("URL không thuộc bucket: " + fileUrl);
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".tmp";
        return filename.substring(filename.lastIndexOf('.'));
    }
}