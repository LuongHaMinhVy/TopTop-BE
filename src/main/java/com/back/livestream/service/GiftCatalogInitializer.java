package com.back.livestream.service;

import com.back.livestream.model.entity.GiftCatalog;
import com.back.livestream.repo.IGiftCatalogRepo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class GiftCatalogInitializer implements CommandLineRunner {

    private final IGiftCatalogRepo giftCatalogRepo;

    @Override
    public void run(String... args) throws Exception {
        log.info("Checking and seeding Gift Catalog...");
        
        // Fetch existing gifts to avoid duplicates
        Set<String> existingGiftNames = giftCatalogRepo.findAll().stream()
                .map(GiftCatalog::getName)
                .map(String::toLowerCase)
                .collect(Collectors.toSet());

        List<GiftCatalog> newGifts = new ArrayList<>();

        // Define our complete catalog of gifts (default and expanded list)
        addGiftIfMissing(newGifts, existingGiftNames, "Rose", 1, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/rose.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Finger Heart", 5, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/finger_heart.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Heart", 10, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/heart.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Star", 20, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/star.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Sunglasses", 99, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/glasses.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Crown", 299, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/crown.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Sports Car", 999, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/car.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Yacht", 2999, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/yacht.png");
        addGiftIfMissing(newGifts, existingGiftNames, "Diamond", 4999, "https://pub-79a8458d5e744b78b2266173ea787dd7.r2.dev/gifts/diamond.png");

        if (!newGifts.isEmpty()) {
            giftCatalogRepo.saveAll(newGifts);
            log.info("Successfully seeded {} new gifts to the catalog.", newGifts.size());
        } else {
            log.info("Gift Catalog is already up-to-date. No new gifts added.");
        }
    }

    private void addGiftIfMissing(List<GiftCatalog> list, Set<String> existingNames, String name, int price, String iconUrl) {
        if (!existingNames.contains(name.toLowerCase())) {
            list.add(GiftCatalog.builder()
                    .name(name)
                    .coinPrice(price)
                    .iconUrl(iconUrl)
                    .isActive(true)
                    .build());
        }
    }
}
