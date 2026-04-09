package com.ks.bayyinah.config;

import com.ks.bayyinah.infra.hybrid.model.*;

import lombok.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

public class ConfigManager {
  private static MainConfig mainConfig;

  public static MainConfig getConfig() {
    if (mainConfig == null) {
      loadConfig();
    }
    return mainConfig;
  }

  public static void loadConfig() {
    ObjectMapper yamlMapper = new YAMLMapper();
    try {
      Path configPath = Path.of(System.getProperty("user.home"), ".bayyinah", "config.yaml");
      File configFile = new File(configPath.toString());

      // 1. Check if folder exists, if not, create it
      if (!Files.exists(configPath.getParent())) {
        Files.createDirectories(configPath.getParent());
      }

      // 2. Check if file exists
      if (!configFile.exists()) {
        System.err.println("Config file not found at: " + configFile.getAbsolutePath());
        String quranDbPath = Path.of(System.getProperty("user.home"), ".bayyinah", "quran.db").toString();
        String userDbPath = Path.of(System.getProperty("user.home"), ".bayyinah", "user.db").toString();
        String audioRootPath = Path.of(System.getProperty("user.home"), ".bayyinah", "audio").toString();

        mainConfig = new MainConfig(
            new QuranConfig(quranDbPath, "https://bayyinah-nvoz.onrender.com"),
            new UserConfig(userDbPath, "https://bayyinah-nvoz.onrender.com"),
            new ApiConfig(10, 30, 3, 5000),
          new AudioConfig(audioRootPath, 2),
            "https://bayyinah-nvoz.onrender.com",
            "https://kamrulhasan12345-bayyinah-ai.hf.space"
        );

        yamlMapper.writeValue(configFile, mainConfig);
      } else {
        mainConfig = yamlMapper.readValue(configFile, MainConfig.class);
        mainConfig.loadFallbacksAsNeeded();
      }

      System.out.println("Configuration loaded successfully from: " + configFile.getAbsolutePath());
      System.out.println("Whole config: " + mainConfig.toString());
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException("Failed to load configuration: " + e.getMessage());
    }
  }
}
