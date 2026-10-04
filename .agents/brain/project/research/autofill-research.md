# Comprehensive Educational Research: Android Autofill & Credential Provider in Compose (2026)

## 1. Official Documentation & Release Notes

### Android Developers: Autofill in Compose
* **URL:** `https://developer.android.com/develop/ui/compose/text/autofill`
* **Last Updated:** Late 2025
* **Depth Level:** Foundational to Intermediate
* **Covers 2026 APIs:** Yes
* **Key Takeaways:** Explains the modern declarative approach introduced in Compose 1.8.0 using `Modifier.semantics { contentType = ... }`. Deprecates the manual `AutofillNode` tree management. Mentions `LocalAutofillManager.current.commit()` for explicit saves.
* **Gaps:** Lacks deep integration examples with the new Credential Manager API.

### Credential Manager API Documentation
* **URL:** `https://developer.android.com/identity/sign-in/credential-manager`
* **Last Updated:** 2026
* **Depth Level:** Advanced
* **Covers 2026 APIs:** Yes
* **Key Takeaways:** Unifies Passkeys, Passwords, and Federated Sign-In. Direct integration with the Android autofill framework as of 2026 using `androidx.credentials:credentials:1.7.0-alpha03` or later. Uses `GetCredentialRequest` wrapped in coroutines.
* **Gaps:** Compose-specific architecture patterns (e.g., hoisting Credential Manager state) are left to the developer to architect.

---

## 2. Courses & Video Content

### "Diving into Autofill Framework in Android with Jetpack Compose" (Gibson Ruitiari)
* **URL:** `https://academy.droidcon.com/course/diving-into-autofill`
* **Last Updated:** 2024
* **Depth Level:** Intermediate
* **Covers 2026 APIs:** Partial (Predates Compose 1.8.0 semantics)
* **Key Takeaways:** 26-minute coffee-break codelab covering foundations, architecture of Autofill services vs. clients, and testing techniques.
* **Gaps:** Relies on older Compose autofill implementations. Must be adapted to the `Modifier.semantics` approach for 2026 compliance.

---

## 3. Agent Skills & GitHub Repositories

### `chrisbanes/skills`
* **URL:** `https://github.com/chrisbanes/skills`
* **Last Updated:** 2026
* **Depth Level:** Expert / Architecture
* **Covers 2026 APIs:** N/A
* **Key Takeaways:** While an authoritative repository for Agent AI skills covering Compose state, effects, and performance (`compose-state-and-effects`, `compose-performance`), it does not have a dedicated Autofill or Credential Manager module yet. 
* **Gaps:** No explicit autofill guidance provided in this repository.

### `new-silvermoon/awesome-android-agent-skills`
* **URL:** `https://github.com/new-silvermoon/awesome-android-agent-skills`
* **Last Updated:** 2026
* **Depth Level:** Intermediate
* **Covers 2026 APIs:** N/A
* **Key Takeaways:** A curated hub for standardized Agent Skills. Points to modular UI and navigation skills, but currently lacks a dedicated skill codifying the Compose 1.8.0 Autofill API or Credential Manager passkeys.
* **Gaps:** Missing direct instructions for `androidx.autofill.inline.v1.InlineSuggestionUi` or Credential Provider services.

### `Drjacky/claude-android-ninja`
* **URL:** `https://github.com/Drjacky/claude-android-ninja`
* **Last Updated:** 2025/2026
* **Depth Level:** Advanced
* **Covers 2026 APIs:** Yes
* **Key Takeaways:** Contains explicit patterns for forms and user input in Jetpack Compose, including standardized approaches for keyboard configurations and form validation.
* **Gaps:** Autofill instructions are present but generalized; may require manual cross-referencing with `androidx.credentials` documentation for passkey-specific Autofill implementation.

---

## 4. Blogs & Articles (2024–2026)

### ProAndroidDev / Bryan Herbst: Jetpack Compose 1.8.0 Autofill
* **URL:** `https://bryanherbst.com` / Medium
* **Last Updated:** 2025
* **Depth Level:** Intermediate
* **Covers 2026 APIs:** Yes
* **Key Takeaways:** Highlights the transition to `Modifier.semantics { contentType = ContentType.Username + ContentType.EmailAddress }`. Demonstrates `LocalAutofillManager.current?.commit()` to save credentials manually.
* **Gaps:** 🚨 **LEGACY WARNING**: Specifically flags older tutorials using `LocalAutofillTree`, `AutofillNode`, and `requestAutofillForNode` as deprecated. Do not use these patterns in 2026.

---

## 5. Testing Autofill in Compose

### CTS & ADB Strategies
* **URL:** `https://source.android.com/compatibility/cts`
* **Last Updated:** 2026
* **Depth Level:** Advanced
* **Covers 2026 APIs:** Yes
* **Key Takeaways:** 
  * Testing locally requires setting a mock autofill service via ADB: `adb shell cmd autofill set bind_instant_service_allowed true`.
  * Compose UI Testing: Use `SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ContentType.Username)` to assert that the semantic tree correctly exposes the fields to the OS.
* **Gaps:** Mocking the Credential Manager bottom sheet in UI tests requires overriding the `androidx.credentials` dependencies with a fake testing provider.

---

## 🏁 Recommended Learning Path (2026)

1. **Start with the Core Paradigm Shift:** Read the Jetpack Compose 1.8.0 release notes focusing on `Modifier.semantics { contentType }`. Completely unlearn `AutofillNode`.
2. **Understand the Credential Manager Bridge:** Read the official Android Identity documentation for `androidx.credentials:credentials:1.7.0+`. Understand how `GetCredentialRequest` hooks into the Autofill inline keyboard UI.
3. **Architecture Context:** Watch Gibson Ruitiari's Droidcon course for the *architectural theory* of Autofill (Service vs. Client), but ignore the specific Compose code examples, mentally replacing them with the 2026 semantics approach.
4. **Implement & Test:** Apply the patterns to your UI, then use Compose UI semantics matchers and `adb shell cmd autofill` to verify the integration.

---

## ✅ 2026 Jetpack Compose Autofill & Credential Checklist

- [ ] **Deprecation Check:** Ensure zero usage of `LocalAutofillTree`, `AutofillNode`, or `OptIn(ExperimentalComposeUiApi::class)` for autofill.
- [ ] **Semantics Modifier:** Use `Modifier.semantics { contentType = ContentType.Username }` (or `Password`, `EmailAddress`, `NewPassword`) on all relevant `TextField` components.
- [ ] **Combined Types:** Use the plus operator for multi-type fields: `ContentType.Username + ContentType.EmailAddress`.
- [ ] **Explicit Commits:** Invoke `LocalAutofillManager.current?.commit()` when a form is successfully submitted (e.g., login success) to prompt the OS to save the credentials.
- [ ] **Credential Manager Integration:** Use `androidx.credentials:credentials:1.7.0+` for Passkeys.
- [ ] **Passkey Options:** Construct `GetCredentialRequest` with `GetPublicKeyCredentialOption` for passkey support.
- [ ] **Asset Links:** Ensure `assetlinks.json` is correctly deployed to your domain to bind passkeys to the application.
- [ ] **UI Testing:** Write `composeTestRule.onNodeWithTag(...).assert(SemanticsMatcher.expectValue(SemanticsProperties.ContentType, ...))` tests.
