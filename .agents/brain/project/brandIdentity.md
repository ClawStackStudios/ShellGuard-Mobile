# Brand Identity: ShellGuard Mobile

## Brand Name & Tagline
- **Product Name**: ShellGuard Mobile
- **Studio**: ClawStack Studios
- **Tagline**: *"Your reef. Your keys. Your vault. In your pocket."*
- **Trademark Conventions**: ClawKey©™, ShellCryption™

## Design Language: Reef Modernist Mobile
A dark-first, privacy-forward aesthetic inspired by deep ocean bioluminescence and marine exoskeletal geometry. Every surface is flat, every border is deliberate, every glow is earned.

**Core Principles:**
1. **Flat Carapace**: Zero elevation shadows. Identity comes from borders and color, not depth illusion.
2. **Bioluminescent Accents**: Color is used sparingly and intentionally — it signals interactive elements, status, and identity.
3. **Exoskeletal Structure**: Visible 1dp borders define component boundaries. The skeleton is part of the aesthetic, not hidden beneath it.

## Color Palette

### Base Surface Tokens
| Token | Hex | Role |
| :--- | :--- | :--- |
| Surface / Background | `#0D1117` | Primary dark background |
| Surface Variant | `#161B22` | Cards, dialogs, elevated containers |
| Border / Outline | `#3D484E` | 1dp structural borders on all cards and inputs |
| On Surface | `#F0F6FC` | Primary text |
| On Surface Variant | `#8B949E` | Secondary / muted text |

### 6 Curated Theme Accents
| Accent Name | Primary Hex | Identity |
| :--- | :--- | :--- |
| `REEF_DEFAULT` | `#E4048A` | Reef Pink — canonical brand default |
| `CYAN_VENT` | `#00D8F6` | Bioluminescent Cyan |
| `PURPLE_SHELL` | `#A371F7` | Deep Purple |
| `EMERALD_TRENCH` | `#2EA043` | Marine Green |
| `AMBER_FLARE` | `#D29922` | Warning Amber |
| `MONOCHROME` | `#8B949E` | Desaturated Neutral |

### Semantic Colors
| Token | Hex | Role |
| :--- | :--- | :--- |
| Error | `#FF5252` | Destructive actions, validation errors |
| Success | `#2EA043` | Positive confirmations |
| Warning | `#D29922` | Cautionary states, offline banners |

## Typography
- **Font Family**: System default sans-serif (Roboto on Android, SF Pro on iOS, Inter on Web).
- **Weight Convention**: Regular (400) for body, Medium (500) for labels and buttons, Bold (700) for headings and emphasis only.
- **Scale Principle**: Material 3 type scale. Never hardcode pixel sizes in composables — bind to `MaterialTheme.typography` tokens.

## Component DNA (The Blind Lineup Test)
These are the traits that make ShellGuard recognizable in a screenshot with no logo visible:

1. **Cards**: `elevation = 0.dp`, `1dp #3D484E border`, `12.dp corner radius`, `#161B22` surface fill.
2. **Input Fields**: Outlined variant, `1dp #3D484E` border at rest, accent color border on focus, no filled background.
3. **Buttons (Primary)**: Filled with current theme accent, `8.dp corner radius`, white text.
4. **Buttons (Secondary)**: Outlined with `1dp` accent border, transparent fill, accent-colored text.
5. **Icons**: Outlined style from Material Icons. Filled icons used only for active/selected states.
6. **Dividers**: `#3D484E` at `0.5dp` — subtle, never heavy.
7. **Touch Targets**: Minimum `48.dp` for all interactive elements (accessibility invariant).

## Voice & Tone
- **Error Messages**: Direct and actionable. No jargon, no blame. *"Connection failed. Check your server address and try again."*
- **Success States**: Brief and confident. *"Saved."* / *"Copied to clipboard."*
- **Empty States**: Warm and instructive. *"No passwords yet. Tap + to add your first."*
- **Security Prompts**: Firm but not alarming. *"Verify your identity to continue."*
- **Offline Banners**: Informative, not panicky. *"Offline — viewing cached vault. Editing disabled until reconnected."*

## Iconography & Motion
- **Icon Style**: Material Symbols Outlined, weight 400, optical size 24.
- **Animation Philosophy**: Spring physics (`dampingRatio = 0.7f`, `stiffness = Spring.StiffnessLow`) for entrances and reveals. No linear animations except progress indicators.
- **Transitions**: Shared element transitions between list items and detail views. Crossfade for screen-level navigation.
- **TOTP Countdown**: Depleting Canvas arc with smooth color interpolation (Cyan `#00D8F6` → Amber `#D29922` → Red `#FF5252`).

## Cross-Platform Parity Anchors
- **Canonical Reference**: ShellGuard Web Client (Express 5 + vanilla JS) is the design source of truth.
- **Parity Rule**: ShellGuard Mobile must achieve **blind side-by-side visual parity** with the web client on identical screen densities.
- **Shared Brand Siblings**: ShellGuard-TOTP (companion app) shares the same Reef Modernist tokens and accent palette.
- **1:1 Layout Parity**: Tablets and foldables (≥ 840dp) must render a 3-pane layout (`SidebarFolderTree` 240dp, `ItemListPane` 340dp, `ItemDetailPane` weight 1f) matching the web client's desktop column structure.
