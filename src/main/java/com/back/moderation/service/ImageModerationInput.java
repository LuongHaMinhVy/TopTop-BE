package com.back.moderation.service;

public record ImageModerationInput(byte[] imageBytes, String mimeType) {}
