# Piso - Personal Money Tracker

**Piso** is a private, lightweight personal finance and money tracking Android application built with Kotlin, Jetpack Compose, Material 3, and Room Database.

---

## Features & Screens

- **Home / Dashboard**: Overview of total net worth, cash flow summaries, budget progress, and quick transaction shortcuts.
- **Accounts**: Manage multiple accounts (Cash, Bank, Savings, e-Wallets such as GCash/Maya). Supports transfers between accounts.
- **Activity**: Month-by-month ledger of all transactions (Income, Expense, Transfer) with type filters, search, monthly income/spent/net totals, and back-dating via a date picker.
- **Goals**: Savings goals with progress tracking and contributions from an account.
- **Budgets**: Monthly category budgets with over/near-limit warnings; copy last month's budgets in one tap.
- **Debts**: Track what you owe and record payments from an account.
- **Invest**: Track the current value of Stocks, Mutual Funds / ETFs, Gold, Silver, Crypto and other assets (values are entered manually).
- **More / Settings**: Profile, theme (Light, Dark, System), PIN security, JSON backup/restore, and CSV export of transactions.

---

## Security & Privacy

- **100% Local Storage**: All financial records, accounts, and profile data are stored strictly on-device using SQLite via Room Database (`piso_database`).
- **PIN Lock Security**: Optional 6-digit PIN. The app locks whenever it goes to the background (not on screen rotation), with a lockout after repeated wrong PINs. Screenshots and the recent-apps preview are blocked.
- **No Cloud Dependency**: Runs completely offline with no mandatory external server connections or cloud tracking.

---

## Data Backup & Restore

- **JSON Import / Export**: Export your entire financial history, categories, and account states into a portable JSON backup file, and restore it on another device. Restoring replaces all current data.
- **CSV Export**: Export all transactions to a spreadsheet-friendly CSV (date, type, category, amount, signed amount, account, note). This is for analysis only; use the JSON backup to restore.
- Backups are plain, unencrypted files. Keep them somewhere private.

---

## How to Build & Run

### Option A: Build the APK on GitHub (no Android Studio needed)

Every push to any branch runs the **Android CI** workflow, which runs the unit tests and builds a
debug APK.

1. Push your changes to GitHub (or open the **Actions** tab and run **Android CI** manually with
   **Run workflow**).
2. Open the **Actions** tab and click the latest **Android CI** run.
3. When it finishes (about 5–10 minutes), scroll to **Artifacts** and download **piso-debug-apk**.
   GitHub delivers it as a `.zip`; unzip it to get `app-debug.apk`.
4. Copy the APK to your phone and open it. Android will ask you to allow installing apps from that
   source (Files / Chrome) the first time.

#### Publishing a release APK

The **Android Release** workflow builds a minified release APK:

- **From a tag** – creates a GitHub Release with the APK attached:
  ```bash
  git tag v1.0.0
  git push origin v1.0.0
  ```
  The APK then appears under **Releases** on the repository page as `Piso-1.0.0.apk`.
- **Manually** – Actions → **Android Release** → **Run workflow**. The APK is in the run's
  **Artifacts** as **piso-release-apk**.

#### Signing (important if you will keep real data in the app)

Android only lets you install an update over an existing install if both APKs are signed with the
**same key**. Without your own key, each GitHub build is signed with a temporary debug key, so
installing a newer build means uninstalling first, **which deletes all data in the app** (export a
JSON backup first). To avoid this, create a keystore once and store it as repository secrets:

```bash
# 1. Create a keystore (keep this file and its passwords safe - you can't recover them)
keytool -genkeypair -v -keystore piso-release.jks -alias piso \
  -keyalg RSA -keysize 2048 -validity 10000

# 2. Base64-encode it so it can be stored as a secret
base64 -w 0 piso-release.jks > piso-release.jks.b64     # macOS: base64 -i piso-release.jks -o piso-release.jks.b64
```

3. On GitHub: **Settings → Secrets and variables → Actions → New repository secret**, add:

   | Secret | Value |
   |---|---|
   | `PISO_KEYSTORE_BASE64` | contents of `piso-release.jks.b64` |
   | `PISO_KEYSTORE_PASSWORD` | the keystore password |
   | `PISO_KEY_ALIAS` | `piso` (or the alias you chose) |
   | `PISO_KEY_PASSWORD` | the key password (same as keystore password if you weren't asked separately) |

4. Run the **Android Release** workflow again. Never commit the `.jks` file to the repository.

Release builds get `versionCode` from the workflow run number, so each new release installs as an
update over the previous one.

### Option B: Build locally

#### Requirements
- **Android Studio** – a current stable version that supports Android Gradle Plugin 9.1
- **JDK 17+**
- **Android SDK** API 35 (minimum supported SDK is 26 / Android 8.0)

#### Build Commands

```bash
# Clone repository
git clone https://github.com/JanReins/Piso.git
cd Piso

# Run unit tests
./gradlew test

# Build Debug APK  -> app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# Build Release APK -> app/build/outputs/apk/release/app-release.apk
./gradlew assembleRelease
```

To sign a local release build with your own key, set these environment variables before running
`assembleRelease`: `PISO_KEYSTORE_FILE` (path to the `.jks`), `PISO_KEYSTORE_PASSWORD`,
`PISO_KEY_ALIAS`, `PISO_KEY_PASSWORD`. Without them the release APK is signed with the local debug key.

Install on a connected device with `adb install -r app/build/outputs/apk/debug/app-debug.apk`.
