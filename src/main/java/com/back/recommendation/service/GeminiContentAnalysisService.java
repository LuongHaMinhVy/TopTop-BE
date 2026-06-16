package com.back.recommendation.service;

import com.back.video.model.entity.Video;
import com.back.video.model.entity.VideoCategory;
import com.back.video.repo.IVideoRepository;
import com.back.video.repo.IVideoCategoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.message.TextContent;
import dev.langchain4j.data.message.ImageContent;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiContentAnalysisService {

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final IVideoRepository videoRepository;
    private final IVideoCategoryRepository videoCategoryRepository;

    @Value("${ai.gemini.enabled:true}")
    private boolean enabled;

    public void analyseContent(Video video, List<byte[]> frameBytes) {
        if (!enabled) {
            log.info("Gemini content analysis is disabled, skipping for video {}", video.getId());
            return;
        }

        try {
            log.info("Starting Gemini content analysis for video {}", video.getId());

            List<VideoCategory> activeCategories = videoCategoryRepository.findByIsActiveTrue();
            String categoryListStr = activeCategories.isEmpty()
                    ? "Comedy, Dance, Music, Food, Education, Beauty, Sports, Gaming, Pets, Travel, DIY, News, Other"
                    : activeCategories.stream().map(VideoCategory::getName).collect(java.util.stream.Collectors.joining(", "));

            String systemPrompt = """
                    You are an expert AI content analyzer for a short-form video platform like TikTok.
                    Your task is to analyze the video's content based on its description/caption and some keyframes extracted from the video.

                    Available Categories: """ + categoryListStr + """

                    Available Moods: funny, educational, emotional, inspirational, dramatic, chill, exciting

                    Return ONLY valid JSON in this exact shape, without markdown fences or backticks:
                    {
                      "category": "one of the Available Categories",
                      "tags": ["tag1", "tag2"],
                      "language": "ISO 639-1 code, e.g. vi, en",
                      "mood": "one of the Available Moods",
                      "qualityScore": 0.85
                    }
                    """;

            String userPrompt = "Video Description/Caption: " + (video.getDescription() != null ? video.getDescription() : "(no description)") + "\n";
            if (frameBytes == null || frameBytes.isEmpty()) {
                userPrompt += "No frames available for this video.";
            } else {
                userPrompt += "Analyze the attached keyframes to extract content details.";
            }

            List<dev.langchain4j.data.message.Content> contentList = new ArrayList<>();
            contentList.add(TextContent.from(userPrompt));

            if (frameBytes != null) {
                for (byte[] bytes : frameBytes) {
                    String base64Image = Base64.getEncoder().encodeToString(bytes);
                    contentList.add(ImageContent.from(base64Image, "image/jpeg"));
                }
            }

            ChatRequest request = ChatRequest.builder()
                    .messages(
                            SystemMessage.from(systemPrompt),
                            UserMessage.from(contentList)
                    )
                    .temperature(0.2)
                    .build();

            ChatResponse response = chatModel.chat(request);
            String responseText = response.aiMessage().text().trim();
            log.debug("Gemini raw content analysis response: {}", responseText);

            // Clean markdown blocks if returned
            if (responseText.startsWith("```")) {
                responseText = responseText.replaceAll("```json", "").replaceAll("```", "").trim();
            }

            JsonNode root = objectMapper.readTree(responseText);
            String category = root.path("category").asText("Other");
            JsonNode tagsNode = root.path("tags");
            List<String> tags = new ArrayList<>();
            if (tagsNode.isArray()) {
                for (JsonNode t : tagsNode) {
                    tags.add(t.asText());
                }
            }
            String language = root.path("language").asText("vi");
            String mood = root.path("mood").asText("chill");
            double qualityScore = root.path("qualityScore").asDouble(0.5);

            video.setAiCategory(category);
            VideoCategory videoCategory = videoCategoryRepository.findByCodeIgnoreCaseOrNameIgnoreCase(category, category)
                    .orElseGet(() -> videoCategoryRepository.findByCodeIgnoreCase("OTHER").orElse(null));
            video.setVideoCategory(videoCategory);
            video.setCategory(videoCategory != null ? videoCategory.getName() : category);
            video.setAiTagsJson(objectMapper.writeValueAsString(tags));
            video.setDetectedLanguage(language);
            video.setContentMood(mood);
            video.setContentQualityScore(qualityScore);

            videoRepository.save(video);
            log.info("Gemini content analysis completed successfully for video {}", video.getId());

        } catch (Exception e) {
            log.error("Failed to analyze content using Gemini for video {}: {}", video.getId(), e.getMessage(), e);
        }
    }
}
