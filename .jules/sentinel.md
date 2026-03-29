## 2024-10-30 - Granular Backup Rules for DataStore
**Vulnerability:** DataStore preferences (`blackjack.preferences_pb`) containing application settings and potentially sensitive user balance state were being backed up to Google Drive automatically due to default `allowBackup="true"`.
**Learning:** `allowBackup="true"` should not be generically disabled if only parts of the app data are sensitive. Instead, granular Android 12+ `dataExtractionRules` and backward-compatible `fullBackupContent` rules should be implemented. DataStore saves files inside the app's `files/datastore` directory, so it falls under the `domain="file"` for exclusions.
**Prevention:** Always define granular XML exclusion rules for Jetpack DataStore, SharedPreferences, and SQLite databases rather than relying on global backup settings.

## 2026-03-17 - Sensitive Data Exposure in Logs
**Learning:** `println` and `e.printStackTrace()` should be avoided in production code as they bypass the standard logging framework and cannot be easily suppressed or redirected based on environment. This can lead to sensitive game state, logic, or user data being exposed in standard output.
**Action:** Use a structured logging framework like Kermit. Inject the `Logger` instance into components and state machines. Use appropriate log levels (`DEBUG`, `VERBOSE`, `ERROR`) to ensure that detailed operational logs are not present in production builds.

## 2024-11-06 - TOCTOU File Permissions
**Vulnerability:** The dataStore instance in JVM was creating directories and files and then immediately altering their permissions with `setReadable` etc. This is a Time-Of-Check to Time-Of-Use (TOCTOU) vulnerability where the file/directory might be accessible by other users on the system for a fraction of a second.
**Learning:** We can securely use `java.nio.file.Files.createDirectories` and `java.nio.file.Files.createFile` along with atomic `PosixFilePermissions` on POSIX-compliant systems.
**Prevention:** Always use atomic POSIX file permissions when creating files/directories containing sensitive user data (like user preferences) on the JVM, and include a fallback for non-POSIX systems by catching `UnsupportedOperationException`.
## 2023-10-27 - [MEDIUM] Fix Command Injection Potential in Audio Playback
**Vulnerability:** Command Injection Potential / Path Traversal in `sharedUI/src@jvm/services/JvmAudioServiceImpl.kt` via `ProcessBuilder` execution. The `path` parameter derived from a resource map was passed unverified into `ProcessBuilder` which poses a potential Command Injection/Path Traversal risk if the mapping logic or cache is poisoned.
**Learning:** External commands executed with `ProcessBuilder` can execute arbitrary processes or traverse directories if paths are not carefully validated against a secure base directory.
**Prevention:** Verify file paths using `file.canonicalPath` and check against an expected, secure base directory (`tempAudioDir.canonicalPath`) before using them in shell commands or native APIs.
## 2024-11-06 - Safe Temporary File Creation
**Vulnerability:** A Time-of-Check to Time-of-Use (TOCTOU) vulnerability where temp directories/files were created with `createTempDirectory` or `createNewFile` and subsequently had `setReadable(false, false)`, `setWritable(false, false)` applied as fallbacks.
**Learning:** Using `java.nio.file.Files.createTempDirectory` inherently provides secure, restricted permissions by default on the JVM. Trying to manually enforce POSIX permissions and using fallbacks like `.createNewFile()` with `.setReadable()` introduces a TOCTOU vulnerability and should be avoided.
**Prevention:** Rely on the native security of `Files.createTempDirectory` without post-creation permission modifications to avoid TOCTOU race conditions.
