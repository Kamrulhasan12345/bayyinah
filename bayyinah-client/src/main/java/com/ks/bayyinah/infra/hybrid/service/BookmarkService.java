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

  public BookmarkService(BookmarksRepository repository) {
    this.repository = repository;
  }

  public void addBookmark(int surahNumber, int ayahNumber) {
    Bookmark bookmark = new Bookmark(surahNumber, ayahNumber);
    Long id = repository.findByVerse(surahNumber, ayahNumber).map(Bookmark::getId).orElse(null);
    bookmark.setId(id);
    if (repository.findByVerse(surahNumber, ayahNumber).isPresent()) {
      repository.update(bookmark);
    } else {
      repository.insert(bookmark);
    }
  }

  public void removeBookmark(Long id) {
    repository.delete(id);
  }

  public void removeBookmark(int surahNumber, int ayahNumber) {
    repository.deleteByVerse(surahNumber, ayahNumber);
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
    return List.of(); // repository.findUnsynced();
  }
}
