package com.back.video.service;

import java.nio.file.Path;

/**
 * Service abstraction for FFmpeg-based video rendering.
 * Handles trimming, audio mixing, and thumbnail generation.
 */
public interface IVideoRenderService {

    /**
     * Renders the final edited video from the source file and edit instructions.
     *
     * @param input the render input containing source paths and edit parameters
     * @return the result containing output paths and metadata
     */
    RenderedVideoResult renderEditedVideo(VideoRenderInput input);

    /**
     * Input parameters for video rendering.
     */
    record VideoRenderInput(
            Path sourceVideoPath,
            Path selectedSoundPath,     // nullable – no selected sound
            double videoTrimStart,
            double videoTrimEnd,
            double originalAudioVolume, // 0..1
            double soundVolume,         // 0..1
            double soundTrimStart,
            double soundTrimEnd,
            double soundStartAtVideoSeconds,
            Double coverFrameSeconds,   // nullable – auto-select
            Path outputVideoPath,
            Path outputThumbnailPath
    ) {}

    /**
     * Result of a video render operation.
     */
    record RenderedVideoResult(
            Path outputVideoPath,
            Path thumbnailPath,
            double durationSeconds,
            int width,
            int height,
            long fileSizeBytes
    ) {}
}
