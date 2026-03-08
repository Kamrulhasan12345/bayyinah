package com.ks.bayyinah.bayyinah_server.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ks.bayyinah.bayyinah_server.model.ReadingProgress;
import com.ks.bayyinah.bayyinah_server.repository.ReadingProgressRepository;

import jakarta.transaction.Transactional;

@Service
public class ReadingProgressService {

  @Autowired
  private ReadingProgressRepository readingProgressRepository;

  public List<ReadingProgress> getReadingProgressByUserId(Long userId) {
    return readingProgressRepository.findByUserId(userId);
  }

  public ReadingProgress saveReadingProgress(ReadingProgress readingProgress) {
    return readingProgressRepository.save(readingProgress);
  }

  public Optional<ReadingProgress> getLatestProgress(Long userId) {
    return readingProgressRepository.findByUserIdOrderByLastReadAtDesc(userId).stream().findFirst();
  }

  public Optional<ReadingProgress> getReadingProgressByUserIdAndSurahNumber(Long userId, Integer surahNumber) {
    return readingProgressRepository.findByUserIdAndSurahNumber(userId, surahNumber);
  }

  @Transactional
  public void deleteReadingProgressByIdAndUserId(Long id, Long userId) {
    readingProgressRepository.deleteByUserIdAndId(userId, id);
  }

  @Transactional
  public void deleteReadingProgressByUserIdAndSurahNumber(Long userId, Integer surahNumber) {
    readingProgressRepository.deleteByUserIdAndSurahNumber(userId, surahNumber);
  }
}
