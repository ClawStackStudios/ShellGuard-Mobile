# 📋 Task 4: Touch Targets (≥48dp), Haptic Feedback & Keyboard Insets

> **Target Objective**: Eliminate physical touch friction across the mobile client. Audit and enforce minimum 48dp touch targets on all interactive surfaces per Android accessibility guidelines (`mobile-android-design`). Implement subtle tactile haptic feedback on secret copy/reveal and password generation, polish soft keyboard auto-scroll behavior in forms, and introduce formatted Markdown rendering for Secure Notes.

---

## 🏛️ Invariants & Rules
1. **Zero Architecture Drift**: Screen navigation routes and ViewModels remain untouched. UI changes are presentation-only.
2. **Accessibility Target**: Every interactive element (`IconButton`, clickable card, filter chip) must have an effective touch target of `≥48dp` (using `Modifier.size(48.dp)` or `defaultMinSize(minHeight = 48.dp)`).
3. **No Heavy Markdown Dependencies**: Implement lightweight native Compose Markdown parsing (`SecureNoteMarkdownView`) without adding third-party library bloat.
4. **⚠️ Invariant**: NEVER tag `@jules` in PR comments or commit messages. Plain text `Hey Jules` or `@google-labs-jules[bot]` only.
5. **Attribution Format**: All commits must follow the two-layer format (`User: ...` / `AI: ...`).

---

## 📁 Files in Scope (Strictly Orthogonal to Task 3)
- `app/src/main/java/com/clawstack/shellguard/ui/screens/dashboard/VaultDashboardScreen.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/detail/ItemDetailScreen.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/screens/form/ItemFormScreen.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/components/HapticFeedbackHelper.kt`
- `app/src/main/java/com/clawstack/shellguard/ui/components/SecureNoteMarkdownView.kt`
- `app/src/test/java/com/clawstack/shellguard/ui/components/SecureNoteMarkdownViewTest.kt`

---

## 🛠️ Step-by-Step Implementation Guide

### 1. Tactile Haptic Feedback Helper (`HapticFeedbackHelper.kt`)
Create `app/src/main/java/com/clawstack/shellguard/ui/components/HapticFeedbackHelper.kt`:
```kotlin
package com.clawstack.shellguard.ui.components

import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType

object HapticFeedbackHelper {
    fun confirm(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun tap(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    fun reveal(haptic: HapticFeedback) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }
}
```

---

### 2. Touch Target Compliance in Dashboard (`VaultDashboardScreen.kt`)
1. Top bar: Upgrade `IconButton` (Sync, Menu) to `Modifier.size(48.dp)` with centered icons.
2. Search bar: Upgrade trailing clear icon button to 48dp touch target.
3. Pod filter chips: Set `Modifier.defaultMinSize(minHeight = 48.dp)` and appropriate vertical padding so chips are effortless to tap with thumbs.

---

### 3. Touch Target Compliance & Haptics in Item Detail (`ItemDetailScreen.kt`)
1. Top bar: Upgrade Back, Edit, and Delete `IconButton` touch targets to `48.dp`.
2. Action rows: Upgrade Note reveal (`Visibility`), Note copy (`ContentCopy`), and field action buttons to minimum `48.dp`.
3. Haptics: Obtain `val haptic = LocalHapticFeedback.current`. Call `HapticFeedbackHelper.confirm(haptic)` inside `copyToClipboard()` and `HapticFeedbackHelper.reveal(haptic)` when toggling secret visibility.
4. Replace raw note `Text(text = state.secret ...)` with `SecureNoteMarkdownView(content = state.secret)`.

---

### 4. Soft Keyboard Auto-Scroll Insets & Ergonomics (`ItemFormScreen.kt`)
1. Upgrade action buttons (Cancel, Save, Add URL, Remove URL) to minimum `48.dp` touch targets.
2. Ensure form leverages container `.imePadding()` + `.verticalScroll()` with bottom clearance (`Spacer(modifier = Modifier.height(48.dp))`) so that when Gboard opens, the active field is smoothly scrolled into the viewport above the IME keyboard natively without redundant `BringIntoViewRequester` modifiers.

---

### 5. Secure Notes Markdown Formatter (`SecureNoteMarkdownView.kt`)
Create `app/src/main/java/com/clawstack/shellguard/ui/components/SecureNoteMarkdownView.kt`:
- Support `# Header 1` (BrandClawCyan, bold, 18sp)
- Support `## Header 2` (TextPrimary, semi-bold, 16sp)
- Support `- List items` (bullet point prefix)
- Support ```` ```code blocks``` ```` (OceanDark background, monospace, BrandClawCyan text, subtle border)
- Gracefully handle unclosed code blocks and empty notes.

---

### 6. Markdown Unit Test Suite (`SecureNoteMarkdownViewTest.kt`)
Create `app/src/test/java/com/clawstack/shellguard/ui/components/SecureNoteMarkdownViewTest.kt`:
- Test parsing of plain text, headers, bullet lists, and code blocks.
- Assert that special characters (passwords, tokens, colons, slashes) within notes are never swallowed or mangled during parsing.

---

## 🚦 Verification Gate
Run the following checks to confirm everything is green:
```bash
python3 scripts/audit_docs_drift.py
./gradlew testDebugUnitTest --tests "com.clawstack.shellguard.ui.components.*"
```
Ensure all tests pass 100% green before submitting your PR!
