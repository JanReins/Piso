# Piso - Personal Money Tracker

**Piso** is a private, lightweight personal finance and money tracking Android application built with Kotlin, Jetpack Compose, Material 3, and Room Database.

---

## Features & Screens

- **Home / Dashboard**: Overview of total net worth, cash flow summaries, budget progress, and quick transaction shortcuts.
- **Accounts**: Manage multiple accounts across Cash, Savings, Credit Cards, e-Wallets, and Investments. Supports transfers between accounts.
- **Activity**: Complete ledger of all transactions (Income, Expense, Transfer) with filtering, categorization, and search.
- **Invest**: Track investment assets across Stocks, Crypto, Real Estate, Mutual Funds, and custom assets with real-time portfolio allocation metrics.
- **More / Settings**: Profile customization, visual theme selection (Light, Dark, System), PIN security settings, and data backup/restore utilities.

---

## Security & Privacy

- **100% Local Storage**: All financial records, accounts, and profile data are stored strictly on-device using SQLite via Room Database (`piso_database`).
- **PIN Lock Security**: Secure app launch with a 4-digit PIN code. Supports customizable lock behaviors including instant lock on pause or temporary skip-lock options.
- **No Cloud Dependency**: Runs completely offline with no mandatory external server connections or cloud tracking.

---

## Data Backup & Restore

- **JSON Import / Export**: Safely export your entire financial history, categories, and account states into a portable JSON backup file. Easily restore data across device transfers.

---

## How to Build & Run

### Requirements
- **Android Studio** (Ladybug | 2024.2.1 or newer recommended)
- **JDK 17+**
- **Android SDK** API 35 (minimum supported SDK is 26 / Android 8.0)

### Build Commands

```bash
# Clone repository
git clone https://github.com/JanReins/Piso.git
cd Piso

# Run unit tests
./gradlew test

# Build Debug APK
./gradlew assembleDebug

# Build Production Release APK
./gradlew assembleRelease
```
