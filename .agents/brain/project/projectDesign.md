# Project Design Vision

> The crystallized mental model of how this product looks, feels, and behaves. Not implementation details — the *design intent* that lives in the developer's head and guides every layout, interaction, and screen they build.

## Canonical Design Reference
**Full Design Specification**: [`DESIGN.md`](file:///config/Local-Storage/workspace-lucas/projects/Agents/ShellGuard-Mobile/DESIGN.md) (1232 lines — Reef Modernist Mobile)

When making any UI or interaction decision, consult `DESIGN.md` as the definitive source. This file (`projectDesign.md`) holds the compressed working model — the parts a developer carries in their head while building.

## Design Identity
- **Design System**: Reef Modernist Mobile — Bioluminescent Defense aesthetic adapted for Material 3 Jetpack Compose.
- **Core Metaphor**: The interface is a living carapace — flat, luminous, structurally visible. Authentication is not an opaque wall; it is an active, glowing shell.
- **Surface Philosophy**: Dark-first. Zero elevation shadows. Structure is communicated through borders and color contrast, not depth illusion.

## Screen Topology & Layout Model

### Phone (Compact, < 600dp)
Single-column fluid navigation. List → Detail → Form as sequential full-screen destinations.

### Tablet & Foldable (≥ 840dp)
3-pane master-detail achieving 1:1 desktop web parity:
```
┌──────────────┬──────────────────┬─────────────────────────┐
│ Sidebar Tree │   Item List      │     Item Detail         │
│   (240dp)    │    (340dp)       │      (weight 1f)        │
│              │                  │                         │
│ [All]        │ ┌──────────────┐ │  Title: ...             │
│ [Passwords]  │ │ Item Card    │ │  Username: ...          │
│ [Notes]      │ ├──────────────┤ │  Password: ••••••••     │
│ [SSH Keys]   │ │ Item Card    │ │  TOTP: 123 456  ◔ 24s  │
│              │ ├──────────────┤ │  Custom Fields: ...     │
│              │ │ Item Card    │ │                         │
└──────────────┴──────────────────┴─────────────────────────┘
```

## Interaction Patterns (The Developer's Mental Shortcuts)

### Vault Item Cards
- Flat `0.dp` elevation, `1dp #3D484E` border, `12.dp` corner radius.
- Spring press feedback (`dampingRatio = 0.7f`, `stiffness = Spring.StiffnessLow`).
- Domain-typed leading icon: 🔑 Password, 📝 Note, 🔐 SSH Key.

### TOTP Countdown Ring
- Depleting Canvas arc (counter-clockwise).
- Color interpolation: Cyan `#00D8F6` (full) → Amber `#D29922` (halfway) → Red `#FF5252` (expiring).
- Large `3x3` grouped monospace digits (`123 456`) for glanceability.

### Forms & Editors
- Pinned header (title + domain selector) and pinned footer (Save/Cancel) remain visible during scroll.
- `.imePadding().verticalScroll()` on all form bodies — soft keyboard never obscures inputs.
- Action menus expand **upward** (dropup) to avoid keyboard occlusion.

### Autofill Suggestion Popup
- Flat `#161B22` background, `1dp #3D484E` border.
- Leading ShellGuard icon, domain-matched title, dimmed username subtitle.
- Android 11+ inline keyboard chips via `InlinePresentation`.

### Security Gates
- **Claw Re-Prompt**: Biometric or PIN challenge before revealing passwords or copying secrets on items marked `reprompt = true`.
- **Clipboard Masking**: `ClipDescription.EXTRA_IS_SENSITIVE = true` on Android 13+, with 30s auto-scrub timer.
- **FLAG_SECURE**: Window capture blocking on release builds (disabled in debug for ADB inspection).

## Offline State Visual Language
- **Amber status banner** at top: *"Offline — viewing cached vault. Editing disabled until reconnected."*
- FAB disabled with tooltip explanation.
- Edit/delete actions dimmed and non-interactive.
- Automatic reconnection via `ConnectivityManager.NetworkCallback` + health probe → banner clears, delta sync fires.

## Empty States & Onboarding
- Warm, illustrative empty states: *"No passwords yet. Tap + to add your first."*
- First-launch "Hatch New Vault" wizard (planned Phase 6+).
- Interactive spotlight guided tour overlay (planned Phase 6+).
