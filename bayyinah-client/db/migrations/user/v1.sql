-- ═══════════════════════════════════════════════════════════
-- BOOKMARKS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL,
    title TEXT, -- User's custom label
    color TEXT DEFAULT '#FFD700', -- Hex color for UI
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- Cloud sync tracking
    synced BOOLEAN DEFAULT 0,
    server_id INTEGER, -- ID from PostgreSQL after sync
    
    UNIQUE(surah_number, ayah_number) -- Prevent duplicate bookmarks
);
CREATE INDEX idx_bookmarks_verse ON bookmarks(surah_number, ayah_number);
CREATE INDEX idx_bookmarks_synced ON bookmarks(synced);

-- ═══════════════════════════════════════════════════════════
-- NOTES
-- ═══════════════════════════════════════════════════════════
CREATE TABLE notes (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    -- Cloud sync tracking
    synced BOOLEAN DEFAULT 0,
    server_id INTEGER,
    deleted BOOLEAN DEFAULT 0 -- Soft delete for sync
);
CREATE INDEX idx_notes_verse ON notes(surah_number, ayah_number);
CREATE INDEX idx_notes_synced ON notes(synced);

-- ═══════════════════════════════════════════════════════════
-- READING PROGRESS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE reading_progress (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL,
    last_read_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    time_spent_seconds INTEGER DEFAULT 0, -- Optional: track reading time
    
    -- Cloud sync tracking
    synced BOOLEAN DEFAULT 0,
    server_id INTEGER,
    
    UNIQUE(surah_number, ayah_number)
);
CREATE INDEX idx_progress_time ON reading_progress(last_read_at DESC);
CREATE INDEX idx_progress_synced ON reading_progress(synced);

-- ═══════════════════════════════════════════════════════════
-- USER PREFERENCES
-- ═══════════════════════════════════════════════════════════
CREATE TABLE preferences (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- Insert defaults
INSERT INTO preferences (key, value) VALUES
    ('theme', 'light'),
    ('font_size', '16'),
    ('default_translation', '20'),
    ('show_arabic', 'true'),
    ('show_transliteration', 'false'),
    ('last_read_surah', '1'),
    ('last_read_ayah', '1');

    -- ═══════════════════════════════════════════════════════════
-- SYNC QUEUE (For Offline Changes)
-- ═══════════════════════════════════════════════════════════
CREATE TABLE sync_queue (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    operation TEXT NOT NULL, -- 'CREATE', 'UPDATE', 'DELETE'
    table_name TEXT NOT NULL, -- 'bookmarks', 'notes', etc.
    record_id INTEGER NOT NULL,
    payload TEXT, -- JSON data to sync
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER DEFAULT 0,
    last_error TEXT
);
CREATE INDEX idx_sync_queue_pending ON sync_queue(created_at);

-- ═══════════════════════════════════════════════════════════
-- STATISTICS (Optional - for gamification)
-- ═══════════════════════════════════════════════════════════
CREATE TABLE daily_stats (
    date DATE PRIMARY KEY,
    verses_read INTEGER DEFAULT 0,
    time_spent_seconds INTEGER DEFAULT 0,
    bookmarks_created INTEGER DEFAULT 0,
    notes_written INTEGER DEFAULT 0
);

