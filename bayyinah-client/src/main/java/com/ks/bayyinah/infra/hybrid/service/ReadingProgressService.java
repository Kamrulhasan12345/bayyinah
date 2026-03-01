package com.ks.bayyinah.infra.hybrid.service;

import com.ks.bayyinah.infra.local.repository.user.ReadingProgressRepository;

public class ReadingProgressService {
  // This service will manage the reading progress of the user. It will interact
  // with the local database to save and retrieve the reading progress, and also
  // sync with the server when needed.

  // For simplicity, let's assume the reading progress is represented by a
  // chapter number and a verse number.

  // We will need methods to:
  // - Update reading progress
  // - Get current reading progress
  // - Sync reading progress with the server

  // The implementation details will depend on the specific requirements and data
  // models of the application.

  private final ReadingProgressRepository repository;

  public ReadingProgressService(ReadingProgressRepository repository) {
    this.repository = repository;
  }


}
