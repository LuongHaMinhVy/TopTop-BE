package com.back.common.service;

import com.back.config.cloudflare.CloudflareR2Properties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class R2StorageService {

    private final S3Client s3Client;
    private final CloudflareR2Properties props;

    public String uploadFile(MultipartFile file, String key) throws IOException{
        PutObjectRequest request = PutObjectRequest.builder()
            .bucket(props.bucketName())
            .key(key)
            .contentType(file.getContentType())
            .contentLength(file.getSize())
            .build();

        s3Client.putObject(request, RequestBody.fromInputStream(
            file.getInputStream(), file.getSize()
        ));

        return props.publicUrl() + "/" + key;
    }
    
    public byte[] downloadFile(String key) {
        GetObjectRequest request = GetObjectRequest.builder()
            .bucket(props.bucketName())
            .key(key)
            .build();

        return s3Client.getObjectAsBytes(request).asByteArray();
    }

    public void deleteFile(String key) {
        DeleteObjectRequest request = DeleteObjectRequest.builder()
            .bucket(props.bucketName())
            .key(key)
            .build();

        s3Client.deleteObject(request);
    }

    public String generatePresignedUrl(String key, Duration duration) {
        try (S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(URI.create(props.endpoint()))
                .region(Region.of("auto"))
                .credentialsProvider(StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(props.accessKey(), props.secretKey())
                ))
                .build()) {

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(duration)
                .getObjectRequest(r -> r.bucket(props.bucketName()).key(key))
                .build();

            return presigner.presignGetObject(presignRequest).url().toString();
        }
    }
}