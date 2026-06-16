-- Map historical string-based categories (category or ai_category) to the new video_category_id
-- We look up the IDs from the video_categories table
UPDATE videos 
SET video_category_id = (
    SELECT id 
    FROM video_categories 
    WHERE code = UPPER(TRIM(COALESCE(videos.ai_category, videos.category)))
)
WHERE video_category_id IS NULL 
  AND EXISTS (
    SELECT 1 
    FROM video_categories 
    WHERE code = UPPER(TRIM(COALESCE(videos.ai_category, videos.category)))
  );

-- Fallback for remaining videos to 'OTHER'
UPDATE videos 
SET video_category_id = (
    SELECT id 
    FROM video_categories 
    WHERE code = 'OTHER'
)
WHERE video_category_id IS NULL;
