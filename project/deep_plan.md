# Deep Implementation Plan — Context-Aware Locked Autofill & Add Item Flow
**Date**: 2026-10-04 09:40
**Scope**: Refines Autofill inline UX. When locked, instead of hiding items, it displays matched URIs without leaking titles/usernames. If there are no matches (locked or unlocked), it presents an "Add Item" chip that deep-links directly into ShellGuard's `ItemFormScreen` with the URI pre-filled.
**Irreversibility**: medium (modifies navigation state handling)

## Ruled Out First Steps
- **[Approach A] Show `pearl.title` when locked.** — Ruled out. Violates zero-knowledge privacy; titles can be sensitive. Displaying the domain string is safer and answers "Do I have an account for this site?".
- **[Approach B] Add a dedicated `AutofillAddItemActivity` to handle the Add Item flow.** — Ruled out. We want the user to land inside the full ShellGuard app (`MainActivity`) so they have the complete form UI, tag selection, and generator tools. Deep-linking is better.

## The Plan

### Step 1: Context-Aware Inline Presentation in `ShellGuardAutofillService`
Update `onFillRequest` to iterate over `matchedPearls` even when locked:
- If `isVaultLocked == true`:
  - Title: "Unlock to Fill"
  - Subtitle: The matched URI (extracted from the current context `parsedFields.webDomain` or `parsedFields.packageName`).
  - Action: Route to `AutofillAuthActivity` which launches `MainActivity` to authenticate. (Wait, if they authenticate via `MainActivity`, the autofill session is lost, but the user is fine with this for cold logins).
- If `isVaultLocked == false`:
  - Title: `pearl.title`, Subtitle: `pearl.username`. (Normal behavior).
  
If `matchedPearls.isEmpty()`:
- Display an "Add Item" chip.
- Title: "Add Item"
- Subtitle: The current domain/package.
- Action: Launch `MainActivity` with a deep link intent `shellguard://app/form/NEW/PASSWORD/new?url=[encoded_domain]`.

**Justification: Privacy-Preserving Matched Context**
**Chosen**: Display the target domain as the subtitle when locked.
**Alternatives considered**:
- **[Display "1 account found"]** — Less informative. The user might want to verify *which* domain it matched against, especially for subdomains.
**Why this one**: It proves ShellGuard is context-aware without leaking the user's stored plaintext metadata.

### Step 2: Handle Deep Links in `MainActivity` NavGraph
Modify `MainActivity.kt`:
- Add a `deepLinks` parameter to the `form/{mode}/{domain}/{id}` composable route:
  `deepLinks = listOf(navDeepLink { uriPattern = "shellguard://app/form/{mode}/{domain}/{id}?url={url}" })`
- Modify `ItemFormViewModel.kt` to extract the `url` from `SavedStateHandle` and pre-fill the form's `url` list if it's not null.

### Step 3: Preserve Deep Links Across Biometric Lock Screen
Modify `MainActivity.kt` and `LockScreen`:
- Currently, unlocking hardcodes `navController.navigate("dashboard")`.
- If the app was launched via a deep link, the NavController handles the deep link immediately. But `MainActivity` overrides the start destination to `"lock"` if `appContainer.deviceVault.isVaultLocked()`.
- Wait, if `MainActivity` is started with a deep link, Jetpack Compose Navigation automatically handles the intent. If we override `startDestination` to `"lock"`, does it drop the deep link?
- Yes, Compose Navigation might lose the intent if we don't handle it carefully.
- Instead of overriding `startDestination`, we should let the NavGraph load normally, but overlay a full-screen `LockScreen` on top of everything if `vaultLockManager.isLocked` is true!
- Actually, the current `NavHost` has `startDestination = startRoute`. If it's `"lock"`, the deep link is ignored.
- We need to pass the `Intent` data to the `MainActivity` so it can navigate *after* unlock, or change the architecture to overlay the lock screen globally.

**Justification: Lock Screen Overlay vs Navigation Node**
**Chosen**: Global Lock Screen Overlay in `MainActivity`.
**Alternatives considered**:
- **[Pass Intent to LockViewModel]** — Complicated to serialize/deserialize Intents across ViewModels.
**Why this one**: If we lift the `LockScreen` OUTSIDE the `NavHost` (e.g. `if (isLocked) { LockScreen(...) } else { NavHost(...) }`), deep links will automatically process once `isLocked` becomes false, because the `NavHost` will be instantiated with the original intent.

## Premarket Postmortem

> "This plan failed. Here's how."

| # | Failure Mode | Step that created it | Severity |
|---|---|---|---|
| 1 | The global LockScreen overlay flashes the underlying app content for 1 frame before realizing it's locked. | Step 3 | cosmetic |
| 2 | `ItemFormViewModel` pre-fills the URL, but the field requires a specific format (e.g. `List<String>`) causing a crash. | Step 2 | moderate |
| 3 | The "Unlock to Fill" chip uses `pearl.id` for its PendingIntent, but since the vault is zeroized, unlocking drops the autofill session, and the user has to tap the field again anyway. | Step 1 | cosmetic |

**Critical fixes applied to plan:**
- Step 3: Ensure `isLocked` state is read synchronously from `DeviceVault` before the first composition of `NavHost`.
- Step 2: `ItemFormViewModel` stores `urls` as a `List<String>`. We must wrap the deep-linked URL in a `listOf(url)`.

## Adversarial Review

| # | Finding | Severity |
|---|---|---|
| 1 | If `isVaultLocked` is true, clicking "Add Item" will prompt for unlock and then go to Add Item, which is good, but requires the Global Lock Screen refactor to work reliably. | moderate |
| 2 | `parsedFields.webDomain` can be null if the app isn't a browser. We need to gracefully fall back to `parsedFields.packageName`. | cosmetic |
| 3 | Deep linking via `shellguard://` might be intercepted by other apps if they declare the same scheme. A custom `clawstack-shellguard://` or App Links would be safer. | moderate |

## Task List

- [x] Step 1: Update `ShellGuardAutofillService` logic (Matched URIs when locked, Add Item when empty)
- [x] Step 2: Implement Deep Link parsing in `MainActivity` NavGraph
- [x] Step 3: Update `ItemFormViewModel` to consume the `url` deep link argument
- [x] Step 4: Refactor `MainActivity` to use a Global Lock Screen overlay
- [x] Verification: Build succeeds
