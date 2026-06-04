ALTER TABLE reports
    ADD COLUMN review_note VARCHAR(1000) NULL AFTER reviewed_by;
