package com.back.config.cloudflare;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "cloudflare.r2")
public record CloudflareR2Properties(
    String accountId,
    String accessKey,
    String secretKey,
    String bucketName,
    String endpoint,
    String publicUrl
) {}