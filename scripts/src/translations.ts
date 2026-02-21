import type { QuranClient } from '@quranjs/api';
import type { Db } from './db.js';
import { withTransaction } from './db.js';
import { sanitizeTranslationText } from './utils.js';

type TranslationResource = {
  id: number;
  authorName?: string;
  languageName?: string;
};

type VerseWithTranslations = {
  id: number;
  translations?: Array<{ resourceId: number; text: string }>;
};

export async function populateTranslationMetadata(db: Db, client: QuranClient) {
  const resources = (await client.resources.findAllTranslations()) as TranslationResource[];
  const stmt = db.prepare(
    `INSERT INTO translations(id, author_name, language)
		 VALUES(?, ?, ?)
		 ON CONFLICT(id) DO UPDATE SET author_name=excluded.author_name, language=excluded.language`
  );

  for (const t of resources) {
    stmt.run(t.id, t.authorName ?? null, t.languageName ?? null);
  }

  return { count: resources.length };
}

async function fetchAllChapterTranslationVerses(
  client: QuranClient,
  chapterId: number,
  translationId: number,
  perPage: number
) {
  const verses: VerseWithTranslations[] = [];
  let page = 1;
  for (; ;) {
    const batch = (await client.verses.findByChapter(String(chapterId), {
      page,
      perPage,
      translations: [translationId],
      translationFields: {
        resourceId: true,
        text: true,
      },
    })) as VerseWithTranslations[];

    verses.push(...batch);
    if (batch.length < perPage) break;
    page++;
  }
  return verses;
}

export async function populateTranslationText(
  db: Db,
  client: QuranClient,
  opts: { translationIds: number[]; perPage: number }
) {
  const upsert = db.prepare(
    `INSERT INTO translation_text(verse_id, translation_id, text)
		 VALUES(?, ?, ?)
		 ON CONFLICT(verse_id, translation_id) DO UPDATE SET text=excluded.text`
  );

  let total = 0;
  for (const translationId of opts.translationIds) {
    for (let chapterId = 1; chapterId <= 114; chapterId++) {
      const verses = await fetchAllChapterTranslationVerses(client, chapterId, translationId, opts.perPage);
      const inserted = withTransaction(db, () => {
        let count = 0;
        for (const v of verses) {
          const tr = v.translations?.find((t) => t.resourceId === translationId);
          if (!tr) continue;
          upsert.run(v.id, translationId, sanitizeTranslationText(tr.text));
          count++;
        }
        return count;
      });
      total += inserted;
    }
  }

  return { count: total };
}
