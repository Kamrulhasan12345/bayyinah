package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.local.repository.user.BookmarksRepository;
import com.ks.bayyinah.infra.hybrid.model.Bookmark;

import java.util.Optional;
import java.util.List;

public class BookmarkService {
  // This service will manage bookmarks for the user. It will interact with the
  // local database
  // to save and retrieve bookmarks, and also sync with the server when needed.

  // For simplicity, let's assume a bookmark has an ID, a title, a URL, and a
  // timestamp.

  // We will need methods to:
  // - Add a bookmark
  // - Remove a bookmark
  // - Get all bookmarks
  // - Sync bookmarks with the server

  // The implementation details will depend on the specific requirements and data
  // models of the application.
  private final BookmarksRepository repository;
  private final SyncQueueService syncQueueService;

  public BookmarkService(BookmarksRepository repository) {
    this(repository, null);
  }

  public BookmarkService(BookmarksRepository repository, SyncQueueService syncQueueService) {
    this.repository = repository;
    this.syncQueueService = syncQueueService;
  }

  public void addBookmark(int surahNumber, int ayahNumber) {
    Optional<Bookmark> existing = repository.findByVerse(surahNumber, ayahNumber);
    Bookmark bookmark = new Bookmark(surahNumber, ayahNumber);
    Long id = existing.map(Bookmark::getId).orElse(null);
    bookmark.setId(id);
    if (existing.isPresent()) {
      repository.update(bookmark);
    } else {
      repository.insert(bookmark);
    }

    if (syncQueueService != null && bookmark.getId() != null) {
      syncQueueService.enqueueUpsert("bookmarks", bookmark.getId(), buildBookmarkPayload(bookmark));
    }
  }

  public void removeBookmark(Long id) {
    repository.delete(id);
    if (syncQueueService != null && id != null) {
      syncQueueService.enqueueDelete("bookmarks", id, "{\"id\":" + id + "}");
    }
  }

  public void removeBookmark(int surahNumber, int ayahNumber) {
    Optional<Bookmark> existing = repository.findByVerse(surahNumber, ayahNumber);
    repository.deleteByVerse(surahNumber, ayahNumber);
    if (syncQueueService != null && existing.isPresent() && existing.get().getId() != null) {
      Bookmark bookmark = existing.get();
      syncQueueService.enqueueDelete("bookmarks", bookmark.getId(), buildBookmarkPayload(bookmark));
    }
  }

  public List<Bookmark> getAll() {
    return repository.findAll();
  }

  public List<Bookmark> getAll(int surahNumber) {
    return repository.findBySurah(surahNumber);
  }

  public Optional<Bookmark> getByVerse(int surahNumber, int ayahNumber) {
    return repository.findByVerse(surahNumber, ayahNumber);
  }

  public Optional<Bookmark> getById(Long id) {
    return repository.findById(id);
  }

  public List<Bookmark> getUnsynced() {
    return repository.findUnsynced();
  }

  private String buildBookmarkPayload(Bookmark bookmark) {
    String title = bookmark.getTitle() != null ? bookmark.getTitle().replace("\"", "\\\"") : "";
    String color = bookmark.getColor() != null ? bookmark.getColor().replace("\"", "\\\"") : "";
    return "{" +
        "\"surahNumber\":" + bookmark.getSurahNumber() + "," +
        "\"ayahNumber\":" + bookmark.getAyahNumber() + "," +
        "\"title\":\"" + title + "\"," +
        "\"color\":\"" + color + "\"" +
        "}";
  }
}
