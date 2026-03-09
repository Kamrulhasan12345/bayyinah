package com.ks.bayyinah.bayyinah_server.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.model.Bookmark;
import com.ks.bayyinah.bayyinah_server.repository.BookmarkRepository;

import jakarta.transaction.Transactional;

@Service
public class BookmarkService {

  @Autowired
  private BookmarkRepository bookmarkRepository;

  public List<Bookmark> getBookmarksByUserId(Long userId) {
    return bookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
  }

  public Optional<Bookmark> getBookmarkByIdAndUserId(Long id, Long userId) {
    return bookmarkRepository.findByIdAndUserId(id, userId);
  }

  public List<Bookmark> getBookmarksBySurahNumber(Long userId, Integer surahNumber) {
    return bookmarkRepository.findByUserIdAndSurahNumber(userId, surahNumber);
  }

  @Transactional
  public void deleteBookmarkByIdAndUserId(Long id, Long userId) {
    bookmarkRepository.deleteByUserIdAndId(userId, id);
  }

  public Bookmark saveBookmark(Bookmark bookmark) {
    return bookmarkRepository.save(bookmark);
  }

  public boolean existsByUserIdAndSurahNumberAndAyahNumber(Long userId, Integer surahNumber, Integer ayahNumber) {
    return bookmarkRepository.existsByUserIdAndSurahNumberAndAyahNumber(userId, surahNumber, ayahNumber);
  }
}
