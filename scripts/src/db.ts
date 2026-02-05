import { mkdirSync } from 'node:fs';
import path from 'node:path';
import { DatabaseSync } from 'node:sqlite';

export type Db = DatabaseSync;

export function openDb(dbPath: string): Db {
  if (dbPath !== ':memory:') {
    const dir = path.dirname(dbPath);
    mkdirSync(dir, { recursive: true });
  }

  const db = new DatabaseSync(dbPath);
  db.exec('PRAGMA journal_mode = WAL;');
  db.exec('PRAGMA synchronous = NORMAL;');
  db.exec('PRAGMA foreign_keys = ON;');
  db.exec('PRAGMA temp_store = MEMORY;');
  return db;
}

export function withTransaction<T>(db: Db, fn: () => T): T {
  db.exec('BEGIN;');
  try {
    const result = fn();
    db.exec('COMMIT;');
    return result;
  } catch (err) {
    try {
      db.exec('ROLLBACK;');
    } catch {
      // ignore
    }
    throw err;
  }
}

export function dropAll(db: Db) {
  db.exec(`
		DROP TABLE IF EXISTS chapters_i18n;
		DROP TABLE IF EXISTS translation_text;
		DROP TABLE IF EXISTS translations;
		DROP TABLE IF EXISTS verses;
		DROP TABLE IF EXISTS chapters;
	`);
}

export function createSchema(db: Db) {
  db.exec(`
		CREATE TABLE IF NOT EXISTS chapters (
			id INTEGER PRIMARY KEY,
			name_simple TEXT,
			name_arabic TEXT,
			verse_count INTEGER,
			revelation_place TEXT
		);

		CREATE TABLE IF NOT EXISTS chapters_i18n (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			chapter_id INTEGER,
			lang_code VARCHAR(5),
			translated_name TEXT,
			short_text TEXT,
			full_text TEXT,
			FOREIGN KEY(chapter_id) REFERENCES chapters(id)
		);

		CREATE UNIQUE INDEX IF NOT EXISTS uq_chapters_i18n ON chapters_i18n(chapter_id, lang_code);
		CREATE INDEX IF NOT EXISTS idx_chapters_i18n_lang ON chapters_i18n(lang_code);

		CREATE TABLE IF NOT EXISTS verses (
			id INTEGER PRIMARY KEY,
			surah_id INTEGER,
			verse_number INTEGER,
			verse_key TEXT UNIQUE,
			text_uthmani TEXT,
			text_indopak TEXT
		);

		CREATE INDEX IF NOT EXISTS idx_verses_surah_verse ON verses(surah_id, verse_number);

		CREATE TABLE IF NOT EXISTS translations (
			id INTEGER PRIMARY KEY,
			author_name TEXT,
			language TEXT
		);

		CREATE TABLE IF NOT EXISTS translation_text (
			id INTEGER PRIMARY KEY AUTOINCREMENT,
			verse_id INTEGER,
			translation_id INTEGER,
			text TEXT,
			FOREIGN KEY(verse_id) REFERENCES verses(id),
			FOREIGN KEY(translation_id) REFERENCES translations(id)
		);

		CREATE UNIQUE INDEX IF NOT EXISTS uq_translation_text ON translation_text(verse_id, translation_id);
		CREATE INDEX IF NOT EXISTS idx_translation_text_verse ON translation_text(verse_id);
		CREATE INDEX IF NOT EXISTS idx_translation_text_translation ON translation_text(translation_id);
	`);
}
