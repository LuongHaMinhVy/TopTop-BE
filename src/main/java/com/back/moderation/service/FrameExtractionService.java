package com.back.moderation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Extracts JPEG frames from a remote video URL using the system FFmpeg binary.
 */
@Slf4j
@Service
public class FrameExtractionService {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Value("${ffmpeg.max-frames:5}")
    private int maxFrames;

    /**
     * Downloads and extracts up to {@code maxFrames} frames from the given video URL.
     *
     * @param videoUrl public URL of the video file
     * @return list of JPEG frame bytes (may be empty if extraction fails)
     */
    public List<byte[]> extractFrames(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            log.warn("FrameExtractionService: videoUrl is blank, skipping extraction");
            return List.of();
        }

        Path tempDir = null;
        try {
            tempDir = Files.createTempDirectory("vid_mod_" + UUID.randomUUID());
            String outputPattern = tempDir.resolve("frame_%02d.jpg").toString();

            // fps=1/4 → one frame every 4 seconds, capped by -frames:v maxFrames
            List<String> command = List.of(
                    ffmpegPath,
                    "-y",                          // overwrite without asking
                    "-i", videoUrl,                // input URL (FFmpeg streams it directly)
                    "-vf", "fps=1/4,scale=480:-2", // sample rate + resize to 480 px wide
                    "-frames:v", String.valueOf(maxFrames),
                    "-q:v", "3",                   // JPEG quality (2 = best, 31 = worst)
                    "-f", "image2",
                    outputPattern
            );

            log.debug("FFmpeg command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true); // merge stderr into stdout
            Process process = pb.start();

            // Consume output to prevent blocking
            String ffmpegOutput = new String(process.getInputStream().readAllBytes());
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                log.warn("FFmpeg exited with code {} for video {}: {}", exitCode, videoUrl,
                        ffmpegOutput.length() > 500 ? ffmpegOutput.substring(0, 500) : ffmpegOutput);
                return List.of();
            }

            // Collect produced frames sorted by filename (frame_01, frame_02 …)
            File[] frameFiles = tempDir.toFile().listFiles(
                    f -> f.isFile() && f.getName().endsWith(".jpg"));

            if (frameFiles == null || frameFiles.length == 0) {
                log.warn("FFmpeg produced no frames for video {}", videoUrl);
                return List.of();
            }

            Arrays.sort(frameFiles, Comparator.comparing(File::getName));

            List<byte[]> frames = new ArrayList<>();
            for (File f : frameFiles) {
                frames.add(Files.readAllBytes(f.toPath()));
            }
            log.info("Extracted {} frames from {}", frames.size(), videoUrl);
            return frames;

        } catch (IOException | InterruptedException e) {
            log.error("Frame extraction failed for {}: {}", videoUrl, e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return List.of();
        } finally {
            // Clean up temp directory
            if (tempDir != null) {
                try {
                    Files.walk(tempDir)
                            .sorted(Comparator.reverseOrder())
                            .map(Path::toFile)
                            .forEach(File::delete);
                } catch (IOException ignored) {}
            }
        }
    }
}
