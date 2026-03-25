package com.ks.bayyinah.bayyinah_server.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import com.ks.bayyinah.bayyinah_server.dto.BookmarkCreationRequest;
import com.ks.bayyinah.bayyinah_server.dto.BookmarkDeleteResponse;
import com.ks.bayyinah.bayyinah_server.model.Bookmark;
import com.ks.bayyinah.bayyinah_server.model.User;
import com.ks.bayyinah.bayyinah_server.model.UserDetailsImpl;
import com.ks.bayyinah.bayyinah_server.service.BookmarkService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/bookmarks")
public class BookmarkController {
  private static final Logger logger = LoggerFactory.getLogger(BookmarkController.class);

  @Autowired
  private BookmarkService bookmarkService;

  @GetMapping("")
  public ResponseEntity<List<Bookmark>> getBookmarks() {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    List<Bookmark> bookmarks = bookmarkService.getBookmarksByUserId(currentUser.getId());
    logger.info("Bookmarks fetched. userId={} count={}", currentUser.getId(), bookmarks.size());

    return ResponseEntity.ok(bookmarks);
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getBookmarkById(@PathVariable("id") Long id) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    Optional<Bookmark> bookmark = bookmarkService.getBookmarkByIdAndUserId(id, currentUser.getId());

    if (bookmark.isPresent()) {
      return ResponseEntity.ok(bookmark.get());
    } else {
      return ResponseEntity.status(404).body(new BookmarkDeleteResponse("Bookmark not found"));
    }
  }

  @GetMapping("/chapters/{number}")
  public ResponseEntity<List<Bookmark>> getBookmarksByChapterNumber(@PathVariable("number") int chapterNumber) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    List<Bookmark> bookmarks = bookmarkService.getBookmarksBySurahNumber(currentUser.getId(), chapterNumber);
    return ResponseEntity.ok(bookmarks);
  }

  @PostMapping("")
  public ResponseEntity<?> createBookmark(@RequestBody BookmarkCreationRequest bookmark) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    logger.info("Bookmark create requested. userId={} surah={} ayah={}",
        currentUser.getId(), bookmark.surahNumber(), bookmark.ayahNumber());

    if (bookmarkService.existsByUserIdAndSurahNumberAndAyahNumber(currentUser.getId(), bookmark.surahNumber(),
        bookmark.ayahNumber())) {
      logger.warn("Bookmark create rejected as duplicate. userId={} surah={} ayah={}",
          currentUser.getId(), bookmark.surahNumber(), bookmark.ayahNumber());
      return ResponseEntity.status(400).body(new BookmarkDeleteResponse("Bookmark already exists for this verse"));
    }
    Bookmark newBookmark = Bookmark.builder().userId(currentUser.getId()).surahNumber(bookmark.surahNumber())
        .ayahNumber(bookmark.ayahNumber()).title(bookmark.title()).color(bookmark.color()).build();
    Bookmark createdBookmark = bookmarkService.saveBookmark(newBookmark);
    logger.info("Bookmark created. userId={} bookmarkId={} surah={} ayah={}",
        currentUser.getId(), createdBookmark.getId(), createdBookmark.getSurahNumber(), createdBookmark.getAyahNumber());
    return ResponseEntity.ok(createdBookmark);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<BookmarkDeleteResponse> deleteBookmarkById(@PathVariable("id") Long id) {
    Authentication authentication = (Authentication) SecurityContextHolder.getContext().getAuthentication();
    UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
    User currentUser = userDetails.getUser();

    logger.info("Bookmark delete requested. userId={} bookmarkId={}", currentUser.getId(), id);

    bookmarkService.deleteBookmarkByIdAndUserId(id, currentUser.getId());
    logger.info("Bookmark delete completed. userId={} bookmarkId={}", currentUser.getId(), id);
    return ResponseEntity.ok(new BookmarkDeleteResponse("Bookmark deleted successfully"));
  }
}
