## 2026-04-14 - Disable Insecure Android Backup

**Learning:** Leaving `android:allowBackup="true"` in the `AndroidManifest.xml` can allow unauthorized extraction of sensitive application data (like shared preferences or databases) via `adb backup`, even on non-rooted devices.

**Action:** Always set `android:allowBackup="false"` unless a backup mechanism is explicitly required and secured.
