# 🐚 ShellGuard Mobile `v0.0.0.31` (Build 31) — Hotfix: First-Class Identity Vault Items & Backup/Bitwarden Import Parity

**Release Date**: 2026-11-22  
**Build Code**: `31` (`versionCode = 31`, `versionName = "0.0.0.31"`)  
**Verification**: **155/155 unit & Robolectric tests passing green** on Cloud CI (`Run 45097694988`).

---

## 🎯 Release Highlights

1. **First-Class Identity Vault Items (`#35`, `PR #36`)**:
   - Added `VaultItemDomain.IDENTITY`, `IdentityDetail`, and `IdentityFieldMapper` projecting 18 structured personal, contact, address, and identification fields + computed `Full Name` & `Location` over `VaultPearlEntity(type = "identity")` (`AAD = vault_pearls_custom:<id>`).
   - Added `PodFilter.IDENTITIES`, 4th Speed-Dial FAB option (`Identity`), blue Identity badge, and modular `IdentityFormSection.kt` & `IdentityDetailSection.kt` composables with masked `SSN`/`Passport Number`/`License Number` and Claw Re-Prompt enforcement.
2. **Web-Canonical `CustomField` Wire Compatibility (`#35`)**:
   - Implemented `CustomFieldWireSerializer` (`KSerializer<CustomField>`) emitting `{ id, name, value, type: "text"|"hidden"|"checkbox"|"linked" }` while accepting both Web `"name"` and legacy Mobile `"label"` + case-insensitive/numeric field types during deserialization.
3. **ShellGuard Backup & Bitwarden 5-Type Import Hardening (`#35`)**:
   - Fixed `VaultBackupEngine.detectBackupFormat()` so v1.0 ShellGuard plaintext backups (`"app": "ShellGuard Vault Backup"`, `"ownerUuid"`, `"items"`) are recognized as `SHELLGUARD_PLAIN` rather than falling through to `BITWARDEN_JSON`.
   - Enabled `coerceInputValues = true` and `isLenient = true` on `VaultBackupEngine.json` for null-safe decoding of explicit `null` properties (`"fields": null`, `"uris": null`, `"folders": null`, `"url": null`).
   - Added `BitwardenIdentity`, `BitwardenCard`, and `BitwardenSshKey` DTOs and full ingestion for all 5 Bitwarden item types (`1` Login, `2` Secure Note, `3` Card, `4` Identity, `5` SSH Key) with `uris` serialized as `List<String>`.

---

## 📦 Distribution Artifacts
- **Android App Bundle (`.aab`)** & **Universal Signed APK (`.apk`)** built and signed via Cloud CI (`release.yml`).
