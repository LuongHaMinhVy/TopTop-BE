package com.back.moderation.service;

import com.back.moderation.model.enums.MusicCopyrightStatus;
import com.back.sound.model.entity.Sound;
import com.back.sound.model.enums.SoundType;
import com.back.video.model.entity.Video;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "acrcloud.enabled", havingValue = "true")
public class AcrCloudMusicCopyrightServiceImpl implements IMusicCopyrightService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${acrcloud.access-key}")
    private String accessKey;

    @Value("${acrcloud.access-secret}")
    private String accessSecret;

    @Value("${acrcloud.host}")
    private String host;

    @Override
    public MusicCopyrightResult check(Video video) {
        Sound sound = video.getSound();

        // 1. If it's a platform-licensed public music, approve immediately
        if (sound != null && sound.getType() != SoundType.ORIGINAL
                && Boolean.TRUE.equals(sound.getIsActive())
                && Boolean.TRUE.equals(sound.getIsPublic())) {
            return new MusicCopyrightResult(
                    MusicCopyrightStatus.APPROVED,
                    "PLATFORM_SOUND_LICENSED",
                    "Âm thanh được chọn từ thư viện công khai của hệ thống."
            );
        }

        // 2. Perform ACRCloud check for original sound or embedded audio
        log.info("Starting ACRCloud copyright check for video ID: {}", video.getId());

        if (!isConfigured()) {
            log.warn("ACRCloud is enabled but missing access key, secret, or host. Marking video {} for review.", video.getId());
            return new MusicCopyrightResult(
                    MusicCopyrightStatus.NEED_REVIEW,
                    "ACRCLOUD_NOT_CONFIGURED",
                    "Cấu hình ACRCloud chưa đầy đủ. Cần kiểm tra bản quyền thủ công."
            );
        }

        byte[] sampleBytes;
        try {
            sampleBytes = extractAudioSample(video.getFileUrl());
        } catch (Exception e) {
            log.warn("Failed to extract audio sample using FFmpeg: {}. Falling back to downloading raw video file.", e.getMessage());
            try {
                sampleBytes = restTemplate.getForObject(video.getFileUrl(), byte[].class);
                if (sampleBytes == null || sampleBytes.length == 0) {
                    throw new IOException("Downloaded file is empty");
                }
            } catch (Exception ex) {
                log.error("Failed to download video file: {}", ex.getMessage());
                return new MusicCopyrightResult(
                        MusicCopyrightStatus.NEED_REVIEW,
                        "ACRCLOUD_DOWNLOAD_ERROR",
                        "Không thể tải file video để kiểm tra bản quyền: " + ex.getMessage()
                );
            }
        }

        // Send to ACRCloud
        try {
            String responseJson = callAcrCloud(sampleBytes);
            return parseAcrCloudResponse(responseJson);
        } catch (Exception e) {
            log.error("Error during ACRCloud recognition: {}", e.getMessage(), e);
            return new MusicCopyrightResult(
                    MusicCopyrightStatus.NEED_REVIEW,
                    "ACRCLOUD_CHECK_ERROR",
                    "Lỗi kết nối hoặc nhận diện âm thanh qua ACRCloud: " + e.getMessage()
            );
        }
    }

    private byte[] extractAudioSample(String videoUrl) throws IOException, InterruptedException {
        Path tempVideo = Files.createTempFile("acr-video-", ".tmp");
        Path tempAudio = Files.createTempFile("acr-audio-", ".mp3");

        try {
            // Download video
            byte[] videoBytes = restTemplate.getForObject(videoUrl, byte[].class);
            if (videoBytes == null || videoBytes.length == 0) {
                throw new IOException("Failed to download video file from URL: " + videoUrl);
            }
            Files.write(tempVideo, videoBytes);

            // Execute FFmpeg: ffmpeg -y -i tempVideo -ss 0 -t 15 -vn -ar 16000 -ac 1 -ab 128k -f mp3 tempAudio
            ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg",
                    "-y",
                    "-i", tempVideo.toString(),
                    "-ss", "0",
                    "-t", "15",
                    "-vn",
                    "-ar", "16000",
                    "-ac", "1",
                    "-ab", "128k",
                    "-f", "mp3",
                    tempAudio.toString()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IOException("FFmpeg process timed out");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                throw new IOException("FFmpeg process failed with exit code: " + exitCode);
            }

            return Files.readAllBytes(tempAudio);
        } finally {
            try {
                Files.deleteIfExists(tempVideo);
            } catch (IOException e) {
                log.warn("Failed to delete temp video file: {}", e.getMessage());
            }
            try {
                Files.deleteIfExists(tempAudio);
            } catch (IOException e) {
                log.warn("Failed to delete temp audio file: {}", e.getMessage());
            }
        }
    }

    private String callAcrCloud(byte[] sampleBytes) throws Exception {
        String httpMethod = "POST";
        String httpUri = "/v1/identify";
        String dataType = "audio";
        String signatureVersion = "1";
        String timestamp = String.valueOf(System.currentTimeMillis() / 1000);

        String stringToSign = httpMethod + "\n" +
                httpUri + "\n" +
                accessKey + "\n" +
                dataType + "\n" +
                signatureVersion + "\n" +
                timestamp;

        String signature = generateSignature(stringToSign, accessSecret);

        // Normalize host URL
        String url = host;
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (!url.endsWith("/v1/identify")) {
            if (url.endsWith("/")) {
                url = url + "v1/identify";
            } else {
                url = url + "/v1/identify";
            }
        }

        String boundary = "----TopTopAcrCloudBoundary" + UUID.randomUUID().toString().replace("-", "");
        byte[] multipartBody = buildAcrCloudMultipartBody(
                boundary,
                sampleBytes,
                accessKey,
                dataType,
                signatureVersion,
                timestamp,
                signature
        );

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(30))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(multipartBody))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString());

        return response.body();
    }

    private byte[] buildAcrCloudMultipartBody(
            String boundary,
            byte[] sampleBytes,
            String accessKey,
            String dataType,
            String signatureVersion,
            String timestamp,
            String signature
    ) throws IOException {
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();

        writeFormField(out, boundary, "access_key", accessKey);
        writeFormField(out, boundary, "sample_bytes", String.valueOf(sampleBytes.length));
        writeFormField(out, boundary, "timestamp", timestamp);
        writeFormField(out, boundary, "signature", signature);
        writeFormField(out, boundary, "data_type", dataType);
        writeFormField(out, boundary, "signature_version", signatureVersion);
        writeFileField(out, boundary, "sample", "sample.mp3", "audio/mpeg", sampleBytes);
        out.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));

        return out.toByteArray();
    }

    private void writeFormField(java.io.ByteArrayOutputStream out, String boundary, String name, String value) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private void writeFileField(
            java.io.ByteArrayOutputStream out,
            String boundary,
            String name,
            String filename,
            String contentType,
            byte[] data
    ) throws IOException {
        out.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + filename + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(("Content-Type: " + contentType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        out.write(data);
        out.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private boolean isConfigured() {
        return hasText(accessKey) && hasText(accessSecret) && hasText(host);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String generateSignature(String stringToSign, String accessSecret) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA1");
        SecretKeySpec secretKey = new SecretKeySpec(accessSecret.getBytes("UTF-8"), "HmacSHA1");
        mac.init(secretKey);
        byte[] rawHmac = mac.doFinal(stringToSign.getBytes("UTF-8"));
        return Base64.getEncoder().encodeToString(rawHmac);
    }

    private MusicCopyrightResult parseAcrCloudResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode statusNode = root.path("status");
            int code = statusNode.path("code").asInt(-1);
            String msg = statusNode.path("msg").asText("Unknown");

            if (code == 1001) {
                // No result matched
                return new MusicCopyrightResult(
                        MusicCopyrightStatus.APPROVED,
                        "NO_COPYRIGHT_MATCHED",
                        "Không phát hiện âm thanh vi phạm bản quyền."
                );
            }

            if (code != 0) {
                log.warn("ACRCloud returned error code {}: {}", code, msg);
                return new MusicCopyrightResult(
                        MusicCopyrightStatus.NEED_REVIEW,
                        "ACRCLOUD_API_ERROR",
                        "ACRCloud trả về mã lỗi: " + code + " - " + msg
                );
            }

            JsonNode musicArray = root.path("metadata").path("music");
            if (musicArray.isArray() && musicArray.size() > 0) {
                JsonNode firstMusic = musicArray.get(0);
                String title = firstMusic.path("title").asText("Unknown Title");
                int score = firstMusic.path("score").asInt(0);

                StringBuilder artists = new StringBuilder();
                JsonNode artistsArray = firstMusic.path("artists");
                if (artistsArray.isArray()) {
                    for (int i = 0; i < artistsArray.size(); i++) {
                        if (i > 0) artists.append(", ");
                        artists.append(artistsArray.get(i).path("name").asText(""));
                    }
                }

                String artistStr = artists.toString().trim();
                if (artistStr.isEmpty()) {
                    artistStr = "Unknown Artist";
                }

                log.info("ACRCloud matched music: {} - {} (score={})", title, artistStr, score);

                if (score >= 70) {
                    return new MusicCopyrightResult(
                            MusicCopyrightStatus.REJECTED,
                            "COPYRIGHT_MATCHED",
                            "Phát hiện âm thanh bản quyền: " + title + " - " + artistStr + " (Độ khớp: " + score + "%)"
                    );
                } else {
                    return new MusicCopyrightResult(
                            MusicCopyrightStatus.NEED_REVIEW,
                            "COPYRIGHT_MATCHED_LOW_SCORE",
                            "Phát hiện âm thanh nghi vấn bản quyền: " + title + " - " + artistStr + " (Độ khớp: " + score + "%)"
                    );
                }
            }

            // Code is 0 but music list is empty
            return new MusicCopyrightResult(
                    MusicCopyrightStatus.APPROVED,
                    "NO_COPYRIGHT_MATCHED",
                    "Không phát hiện âm thanh vi phạm bản quyền."
            );

        } catch (Exception e) {
            log.error("Failed to parse ACRCloud response JSON: {}", e.getMessage(), e);
            return new MusicCopyrightResult(
                    MusicCopyrightStatus.NEED_REVIEW,
                    "ACRCLOUD_PARSE_ERROR",
                    "Lỗi phân tích kết quả bản quyền âm thanh."
            );
        }
    }
}
