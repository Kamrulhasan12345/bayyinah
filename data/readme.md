# Bayyinah Data Directory

## To complete the setup, put quran.db in this directory

## Generated audio assets

When audio generation is enabled in the scripts project, files are downloaded under:

- `data/audio/recitation_<id>/surah_<chapter>/<surahayah>.mp3`

Example:

- `data/audio/recitation_2/surah_001/001001.mp3`

Fatihah test runs can target `data/quran-audio-test.db` to avoid modifying the main `data/quran.db`.
