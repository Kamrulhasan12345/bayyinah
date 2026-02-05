import type { QuranClient } from '@quranjs/api';
import type { Db } from './db.js';

export async function populateChapters(db: Db, client: QuranClient) {
  const chapters = await client.chapters.findAll();
  const stmt = db.prepare(
    `INSERT INTO chapters(id, name_simple, name_arabic, verse_count, revelation_place)
		 VALUES(?, ?, ?, ?, ?)
		 ON CONFLICT(id) DO UPDATE SET
			 name_simple=excluded.name_simple,
			 name_arabic=excluded.name_arabic,
			 verse_count=excluded.verse_count,
			 revelation_place=excluded.revelation_place`
  );

  for (const ch of chapters) {
    stmt.run(
      ch.id,
      ch.nameSimple ?? null,
      ch.nameArabic ?? null,
      ch.versesCount ?? null,
      ch.revelationPlace ?? null
    );
  }

  return { count: chapters.length };
}
