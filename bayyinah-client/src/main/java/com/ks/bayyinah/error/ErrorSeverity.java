package com.ks.bayyinah.error;

public enum ErrorSeverity {
  INFO, // Blue - Informational
  WARNING, // Yellow/Orange - Something went wrong but recoverable
  ERROR, // Red - Error but app can continue
  CRITICAL // Dark Red - Critical error, may need restart
}
