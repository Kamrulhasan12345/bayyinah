import type { QuranClient } from '@quranjs/api';
import type { Db } from './db.js';
import { withTransaction } from './db.js';

type VerseRow = {
  id: number;
  verseNumber: number;
  verseKey: string;
  textUthmani?: string;
  textIndopak?: string;
};

async function fetchAllVersesByChapter(client: QuranClient, chapterId: number, perPage: number) {
  const verses: VerseRow[] = [];
  let page = 1;
  for (; ;) {
    const batch = (await client.verses.findByChapter(String(chapterId), {
      page,
      perPage,
      fields: {
        textUthmani: true,
        textIndopak: true,
      },
    })) as VerseRow[];

    console.log(`Fetched ${batch.length} verses for chapter ${chapterId} (page ${page})`);

    verses.push(...batch);
    if (batch.length === 0) break;
    if (batch.length < perPage) break;
    page++;
  }
  return verses;
}

export async function populateVerses(db: Db, client: QuranClient, opts: { perPage: number }) {
  const insert = db.prepare(
    `INSERT INTO verses(id, surah_id, verse_number, verse_key, text_uthmani, text_indopak)
		 VALUES(?, ?, ?, ?, ?, ?)
		 ON CONFLICT(id) DO UPDATE SET
			 surah_id=excluded.surah_id,
			 verse_number=excluded.verse_number,
			 verse_key=excluded.verse_key,
			 text_uthmani=excluded.text_uthmani,
			 text_indopak=excluded.text_indopak`
  );

  let total = 0;
  for (let chapterId = 1; chapterId <= 114; chapterId++) {
    const verses = await fetchAllVersesByChapter(client, chapterId, opts.perPage);
    const inserted = withTransaction(db, () => {
      let count = 0;
      for (const v of verses) {
        insert.run(
          v.id,
          chapterId,
          v.verseNumber,
          v.verseKey,
          v.textUthmani ?? null,
          v.textIndopak ?? null
        );
        count++;
      }
      return count;
    });
    total += inserted;
  }

  return { count: total };
}
