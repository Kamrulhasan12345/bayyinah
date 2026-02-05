import type { QuranClient } from '@quranjs/api';
import type { Db } from './db.js';
import { withTransaction } from './db.js';
import { mapWithConcurrency } from './utils.js';

type ChapterRow = {
  id: number;
  nameSimple?: string;
  nameArabic?: string;
  translatedName?: {
    name?: string;
  };
};

type ChapterInfoRow = {
  shortText?: string;
  text?: string;
};

function normalizeLangCode(langCode: string): string {
  return langCode.trim().toLowerCase();
}

export async function populateChaptersI18n(
  db: Db,
  client: QuranClient,
  opts: {
    languages: string[];
    includeInfo?: boolean | 'auto';
    concurrency?: number;
  }
) {
  const languages = opts.languages.map(normalizeLangCode).filter(Boolean);
  if (languages.length === 0) return { count: 0 };

  const includeInfo = opts.includeInfo ?? 'auto';
  const concurrency = opts.concurrency ?? 6;

  const upsert = db.prepare(
    `INSERT INTO chapters_i18n(chapter_id, lang_code, translated_name, short_text, full_text)
		 VALUES(?, ?, ?, ?, ?)
		 ON CONFLICT(chapter_id, lang_code) DO UPDATE SET
			 translated_name=excluded.translated_name,
			 short_text=excluded.short_text,
			 full_text=excluded.full_text`
  );

  let total = 0;
  for (const lang of languages) {
    const chapters = (await client.chapters.findAll({ language: lang as any })) as ChapterRow[];

    let shouldFetchInfo = includeInfo === true;
    if (includeInfo === 'auto') {
      try {
        await client.chapters.findInfoById('1', { language: lang as any });
        shouldFetchInfo = true;
      } catch {
        shouldFetchInfo = false;
      }
    }

    const rows = await mapWithConcurrency(chapters, concurrency, async (ch) => {
      const translatedName =
        ch.translatedName?.name ??
        (lang === 'ar' ? ch.nameArabic : undefined) ??
        ch.nameSimple ??
        ch.nameArabic ??
        null;

      if (!shouldFetchInfo) {
        return {
          chapterId: ch.id,
          translatedName,
          shortText: null,
          fullText: null,
        };
      }

      let info: ChapterInfoRow | null = null;
      try {
        info = (await client.chapters.findInfoById(String(ch.id), { language: lang as any })) as ChapterInfoRow;
      } catch {
        info = null;
      }

      return {
        chapterId: ch.id,
        translatedName,
        shortText: info?.shortText ?? null,
        fullText: info?.text ?? null,
      };
    });

    const inserted = withTransaction(db, () => {
      let count = 0;
      for (const row of rows) {
        upsert.run(row.chapterId, lang, row.translatedName, row.shortText, row.fullText);
        count++;
      }
      return count;
    });

    total += inserted;
  }

  return { count: total };
}
