package com.back.config;

@ConfigurationProperties(prefix = "cloudflare.r2")
@Component
public record CloudflareR2Properties(
    String accountId,
    String accessKey,
    String secretKey,
    String bucketName,
    String endpoint,
    String publicUrl
) {}