package com.ks.bayyinah.infra.local.query;

import com.ks.bayyinah.core.dto.*;
import com.ks.bayyinah.core.repository.SearchRepository;
import com.ks.bayyinah.infra.local.repository.quran.*;

public class LocalQuranSearchQueryService {
  private final SearchRepository searchRepo;

  private static class Holder {
    private static final LocalQuranSearchQueryService INSTANCE = new LocalQuranSearchQueryService();
  }

  public static LocalQuranSearchQueryService getInstance() {
    return Holder.INSTANCE;
  }

  private LocalQuranSearchQueryService() {
    this.searchRepo = new LocalSearchRepository(); // Initialize local search repository
  }

  public SearchResponse search(String query, Integer translationId, PageRequest pageRequest) {
    return searchRepo.smartSearch(query, translationId, pageRequest);
  }

  public SearchResponse nextPage(String query, Integer translationId, SearchResponse currentResults) {
    if (currentResults.getVerseResults() != null && currentResults.getVerseResults().hasNext()) { // Assuming page size
                                                                                                  // of 20
      PageRequest nextPageRequest = PageRequest.of(currentResults.getVerseResults().getPage() + 1,
          currentResults.getVerseResults().getPageSize());
      return searchRepo.smartSearch(query, translationId, nextPageRequest);
    }
    return currentResults;
  }

  public Page<VerseView> getChapterVerses(int chapterId, Integer translationId, int page) {
    PageRequest pageRequest = PageRequest.of(page, 10);
    return searchRepo.getVersesByChapter(chapterId, translationId, pageRequest);
  }
}
