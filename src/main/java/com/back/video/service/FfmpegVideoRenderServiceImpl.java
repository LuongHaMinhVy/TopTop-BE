package com.back.video.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * FFmpeg-based implementation of {@link IVideoRenderService}.
 * Handles video trimming, audio mixing, and thumbnail extraction.
 */
@Slf4j
@Service
public class FfmpegVideoRenderServiceImpl implements IVideoRenderService {

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    @Override
    public RenderedVideoResult renderEditedVideo(VideoRenderInput input) {
        try {
            renderVideo(input);
            extractThumbnail(input);

            File outputFile = input.outputVideoPath().toFile();
            if (!outputFile.exists() || outputFile.length() == 0) {
                throw new RuntimeException("FFmpeg produced no output file");
            }

            // Probe output for metadata
            double duration = probeMediaDuration(input.outputVideoPath());
            long fileSize = outputFile.length();

            return new RenderedVideoResult(
                    input.outputVideoPath(),
                    input.outputThumbnailPath(),
                    duration,
                    0, // width – can be probed if needed
                    0, // height – can be probed if needed
                    fileSize
            );
        } catch (IOException | InterruptedException e) {
            log.error("Video render failed: {}", e.getMessage(), e);
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            throw new RuntimeException("VIDEO_RENDER_FAILED", e);
        }
    }

    /**
     * Case 1: No selected sound – trim video, adjust original audio volume.
     * Case 2: Selected sound – trim video, trim sound, mix with volume/offset.
     */
    private void renderVideo(VideoRenderInput input) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add(ffmpegPath);
        command.add("-y");

        double trimStart = input.videoTrimStart();
        double trimEnd = input.videoTrimEnd();
        double trimDuration = trimEnd - trimStart;

        // Input 0: source video (with seek)
        if (trimStart > 0) {
            command.addAll(List.of("-ss", String.format("%.3f", trimStart)));
        }
        command.addAll(List.of("-i", input.sourceVideoPath().toString()));
        if (trimDuration > 0) {
            command.addAll(List.of("-t", String.format("%.3f", trimDuration)));
        }

        boolean hasSelectedSound = input.selectedSoundPath() != null
                && Files.exists(input.selectedSoundPath());

        if (!hasSelectedSound) {
            // Case 1: No selected sound – just adjust original audio volume
            double vol = Math.max(0, Math.min(1, input.originalAudioVolume()));
            if (vol < 1.0) {
                command.addAll(List.of("-af", String.format("volume=%.2f", vol)));
            }
            command.addAll(List.of(
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-c:a", "aac",
                    "-b:a", "128k"
            ));
        } else {
            // Case 2: Selected sound – complex filtergraph for audio mixing
            // Input 1: selected sound
            command.addAll(List.of("-i", input.selectedSoundPath().toString()));

            double origVol = Math.max(0, Math.min(1, input.originalAudioVolume()));
            double sndVol = Math.max(0, Math.min(1, input.soundVolume()));
            double sndTrimStart = input.soundTrimStart();
            double sndTrimEnd = input.soundTrimEnd();
            double soundOffset = input.soundStartAtVideoSeconds();

            // Build complex filter:
            // [0:a] -> volume -> original audio
            // [1:a] -> atrim -> adelay -> volume -> sound audio
            // mix both -> amix duration=first
            long delayMs = Math.round(soundOffset * 1000);
            String filterComplex = String.format(
                    "[0:a]volume=%.2f[a0];" +
                    "[1:a]atrim=%.3f:%.3f,asetpts=PTS-STARTPTS,adelay=%d|%d,volume=%.2f[a1];" +
                    "[a0][a1]amix=inputs=2:duration=first:dropout_transition=2[aout]",
                    origVol,
                    sndTrimStart, sndTrimEnd > sndTrimStart ? sndTrimEnd : 9999,
                    delayMs, delayMs,
                    sndVol
            );

            command.addAll(List.of(
                    "-filter_complex", filterComplex,
                    "-map", "0:v",
                    "-map", "[aout]",
                    "-c:v", "libx264",
                    "-preset", "veryfast",
                    "-c:a", "aac",
                    "-b:a", "128k",
                    "-shortest"
            ));
        }

        command.add(input.outputVideoPath().toString());

        log.info("FFmpeg render command: {}", String.join(" ", command));
        int exitCode = executeCommand(command);
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg render exited with code " + exitCode);
        }
    }

    /**
     * Extracts a single thumbnail frame from the rendered video.
     */
    private void extractThumbnail(VideoRenderInput input) throws IOException, InterruptedException {
        double frameTime = 0;
        if (input.coverFrameSeconds() != null && input.coverFrameSeconds() >= 0) {
            // Adjust relative to trim start since the output video starts at 0
            frameTime = Math.max(0, input.coverFrameSeconds() - input.videoTrimStart());
        }

        List<String> command = List.of(
                ffmpegPath,
                "-y",
                "-ss", String.format("%.3f", frameTime),
                "-i", input.outputVideoPath().toString(),
                "-frames:v", "1",
                "-q:v", "2",
                "-f", "image2",
                input.outputThumbnailPath().toString()
        );

        log.debug("FFmpeg thumbnail command: {}", String.join(" ", command));
        int exitCode = executeCommand(command);
        if (exitCode != 0) {
            log.warn("FFmpeg thumbnail extraction exited with code {}", exitCode);
        }
    }

    /**
     * Probes the duration of a media file using ffprobe (falls back to ffmpeg).
     */
    private double probeMediaDuration(Path mediaPath) {
        try {
            // Try ffprobe first (same directory as ffmpeg typically)
            String ffprobePath = ffmpegPath.replace("ffmpeg", "ffprobe");
            List<String> command = List.of(
                    ffprobePath,
                    "-v", "error",
                    "-show_entries", "format=duration",
                    "-of", "csv=p=0",
                    mediaPath.toString()
            );

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String output = new String(process.getInputStream().readAllBytes()).trim();
            process.waitFor();
            return Double.parseDouble(output);
        } catch (Exception e) {
            log.warn("Duration probe failed for {}: {}", mediaPath, e.getMessage());
            return 0;
        }
    }

    private int executeCommand(List<String> command) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        // Consume output to prevent blocking
        String output = new String(process.getInputStream().readAllBytes());
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("FFmpeg output (exit {}): {}", exitCode,
                    output.length() > 1000 ? output.substring(0, 1000) : output);
        }
        return exitCode;
    }
}
