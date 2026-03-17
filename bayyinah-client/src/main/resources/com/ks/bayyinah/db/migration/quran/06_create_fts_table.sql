-- FTS5 virtual table for Arabic verses
CREATE VIRTUAL TABLE IF NOT EXISTS verses_fts USING fts5(
    verse_id UNINDEXED,
    verse_key UNINDEXED,
    surah_id UNINDEXED,
    verse_number UNINDEXED,
    text_uthmani,
    text_indopak,
    content='verses',
    content_rowid='id',
    tokenize='unicode61'
);

-- Populate FTS table from verses
INSERT INTO verses_fts(verse_id, verse_key, surah_id, verse_number, text_uthmani, text_indopak)
SELECT id, verse_key, surah_id, verse_number, text_uthmani, text_indopak
FROM verses;

-- Triggers to keep FTS in sync
CREATE TRIGGER IF NOT EXISTS verses_ai AFTER INSERT ON verses BEGIN
    INSERT INTO verses_fts(verse_id, verse_key, surah_id, verse_number, text_uthmani, text_indopak)
    VALUES (new.id, new.verse_key, new.surah_id, new.verse_number, new.text_uthmani, new.text_indopak);
END;

CREATE TRIGGER IF NOT EXISTS verses_ad AFTER DELETE ON verses BEGIN
    DELETE FROM verses_fts WHERE verse_id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS verses_au AFTER UPDATE ON verses BEGIN
    DELETE FROM verses_fts WHERE verse_id = old.id;
    INSERT INTO verses_fts(verse_id, verse_key, surah_id, verse_number, text_uthmani, text_indopak)
    VALUES (new.id, new.verse_key, new.surah_id, new.verse_number, new.text_uthmani, new.text_indopak);
END;

-- FTS5 virtual table for translations
CREATE VIRTUAL TABLE IF NOT EXISTS translation_text_fts USING fts5(
    id UNINDEXED,
    verse_id UNINDEXED,
    translation_id UNINDEXED,
    text,
    content='translation_text',
    content_rowid='id',
    tokenize='porter unicode61'  -- Porter stemming for better English search
);

-- Populate FTS table from translation_text
INSERT INTO translation_text_fts(id, verse_id, translation_id, text)
SELECT id, verse_id, translation_id, text
FROM translation_text;

-- Triggers for translation_text FTS
CREATE TRIGGER IF NOT EXISTS translation_text_ai AFTER INSERT ON translation_text BEGIN
    INSERT INTO translation_text_fts(id, verse_id, translation_id, text)
    VALUES (new.id, new.verse_id, new.translation_id, new.text);
END;

CREATE TRIGGER IF NOT EXISTS translation_text_ad AFTER DELETE ON translation_text BEGIN
    DELETE FROM translation_text_fts WHERE id = old.id;
END;

CREATE TRIGGER IF NOT EXISTS translation_text_au AFTER UPDATE ON translation_text BEGIN
    DELETE FROM translation_text_fts WHERE id = old.id;
    INSERT INTO translation_text_fts(id, verse_id, translation_id, text)
    VALUES (new.id, new.verse_id, new.translation_id, new.text);
END;

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_verses_surah ON verses(surah_id, verse_number);
CREATE INDEX IF NOT EXISTS idx_verses_key ON verses(verse_key);
CREATE INDEX IF NOT EXISTS idx_translation_text_verse ON translation_text(verse_id);
CREATE INDEX IF NOT EXISTS idx_translation_text_translation ON translation_text(translation_id);
CREATE INDEX IF NOT EXISTS idx_chapters_i18n_chapter ON chapters_i18n(chapter_id, lang_code);
