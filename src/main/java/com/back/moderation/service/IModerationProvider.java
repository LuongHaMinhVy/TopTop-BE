package com.back.moderation.service;

public interface IModerationProvider {
    ModerationProviderResult moderateText(TextModerationInput input);
    ModerationProviderResult moderateImage(ImageModerationInput input);
}
