package com.ks.bayyinah.ui;

public enum ToastSeverity {
  INFO, // Blue - Informational
  SUCCESS, // Green - Successful operation
  WARNING, // Yellow/Orange - Something went wrong but recoverable
  ERROR, // Red - Error but app can continue
  CRITICAL, // Dark Red - Critical error, may need restart
  DEBUG // Gray - Debug information (optional)
}
