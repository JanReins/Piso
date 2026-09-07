# AGENTS.md

## Project Overview
JanReins/Piso is a private, 100% on-device, offline-only personal finance and money tracking Android application built with Kotlin, Jetpack Compose, Material 3, and Room Database.

- **Package Name / Application ID**: `com.janreins.piso`
- **Database**: On-device SQLite via Room Database (`piso_database`).
- **Network**: 100% local storage and offline execution. No remote API calls or cloud dependencies.

## Key Build & Test Commands
- **Run Unit Tests**: `./gradlew test`
- **Build Debug APK**: `./gradlew assembleDebug`
- **Build Release APK**: `./gradlew assembleRelease`

## Codebase Conventions & Constraints
- **Offline Integrity**: Do not add network calls, remote API integrations, or cloud sync services.
- **Do Not Touch**:
  - `FinanceRepository` balance calculation, goal, and debt handling logic (unless explicitly instructed in a task brief).
  - `UserProfileManager` PIN hashing, salt generation, and lockout mechanisms (unless explicitly instructed).
  - Room entities, DAOs, and database migrations (`MIGRATION_1_2` / database version changes).
  - Compose UI screens, components, and navigation structures unless tasked specifically.
