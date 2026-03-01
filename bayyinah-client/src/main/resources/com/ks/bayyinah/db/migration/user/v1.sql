-- ═══════════════════════════════════════════════════════════
-- BOOKMARKS
-- ═══════════════════════════════════════════════════════════
CREATE TABLE bookmarks (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    surah_number INTEGER NOT NULL,
    ayah_number INTEGER NOT NULL,
    title TEXT, -- User's custom label
    color TEXT DEFAULT '#FFD700', -- Hex color for UI
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Cloud sync tracking
    synced BOOLEAN DEFAULT 0,
    server_id INTEGER, -- ID from PostgreSQL after sync
    deleted BOOLEAN DEFAULT 0,

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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

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
    last_read_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    time_spent_seconds INTEGER DEFAULT 0, -- Optional: track reading time

    -- Cloud sync tracking
    synced BOOLEAN DEFAULT 0,
    server_id INTEGER,
    deleted BOOLEAN DEFAULT 0, -- Soft delete for sync

    UNIQUE(surah_number, ayah_number)
);
CREATE INDEX idx_progress_time ON reading_progress(last_read_at DESC);
CREATE INDEX idx_progress_synced ON reading_progress(synced);

-- ═══════════════════════════════════════════════════════════
-- USER PREFERENCES
-- ═══════════════════════════════════════════════════════════
CREATE TABLE user_preferences (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Insert defaults
INSERT INTO user_preferences (key, value) VALUES
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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
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

-- ═══════════════════════════════════════════════════════════
-- USERS (Single User Mode with Guest Support)
-- ═══════════════════════════════════════════════════════════
CREATE TABLE users (
    id INTEGER PRIMARY KEY CHECK (id = 1),  -- Enforce single row
    username TEXT,              -- NULL for guests
    email TEXT,                 -- NULL for guests
    first_name TEXT,            -- NULL for guests
    last_name TEXT,             -- NULL for guests
    device_id TEXT NOT NULL,    -- Always set (unique device identifier)
    is_guest BOOLEAN NOT NULL DEFAULT 1,  -- TRUE = guest mode
    server_id INTEGER,          -- NULL for guests
    last_sync_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE auth_tokens (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    token_type TEXT DEFAULT 'Bearer',
    expires_at DATETIME NOT NULL,
    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    encrypted BOOLEAN DEFAULT 0
);

CREATE INDEX idx_auth_expires_at ON auth_tokens(expires_at);
