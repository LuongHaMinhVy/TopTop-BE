-- Migration: Add edit_instructions_json column to videos table
-- This column stores the serialized VideoEditInstructionsRequestDTO as JSON text
-- for the CapCut-lite video editor feature.

ALTER TABLE videos ADD COLUMN edit_instructions_json TEXT DEFAULT NULL;
