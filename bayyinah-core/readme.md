# bayyinah-core

Shared domain and query contract module for Bayyinah.

## What this module provides

- Core Quran domain models (chapter, verse, translation, audio metadata)
- Query and repository interfaces consumed by client/server implementations
- DTOs for chapter/verse/translation views and pagination
- Shared exceptions for repository and query operations

## Key source anchors

- Module exports: `src/main/java/module-info.java`
- Query contract: `src/main/java/com/ks/bayyinah/core/query/QuranQueryService.java`
- Domain model examples:
  - `src/main/java/com/ks/bayyinah/core/model/Chapter.java`
  - `src/main/java/com/ks/bayyinah/core/model/Verse.java`

## Package map

| Package | Responsibility |
| --- | --- |
| `com.ks.bayyinah.core.model` | Domain models |
| `com.ks.bayyinah.core.dto` | Read models and pagination DTOs |
| `com.ks.bayyinah.core.repository` | Repository contracts |
| `com.ks.bayyinah.core.query` | Query service interfaces |
| `com.ks.bayyinah.core.exception` | Shared exceptions |

## Build and test

From module directory:

```bash
mvn clean install
mvn test
```

From repository root:

```bash
mvn -pl bayyinah-core clean install
```

## Notes

- Java 21 target.
- Lombok is used for model boilerplate.
- This module intentionally avoids JavaFX and Spring runtime concerns.

## Related docs

- [Repository root guide](../readme.md)
- [Client module guide](../bayyinah-client/readme.md)
- [Server module guide](../bayyinah-server/readme.md)