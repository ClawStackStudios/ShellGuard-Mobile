# 🦞 ClawStack Studios — Standard Release Template

> **Location:** `.agents/templates/release-template.md`  
> **Use When:** Drafting `RELEASE-vX.Y.Z.N.md` in the repo root during the `/draft-release` and `/version-update` workflows.  
> **Automation Trigger:** Pushing a commit to `main` with `--release vX.Y.Z.N` (or pushing tag `vX.Y.Z.N`) triggers `.github/workflows/release.yml`, which automatically consumes this file as the official GitHub Release body and triggers Docker container publication.

---

# 🦞 [Project Name] — Release v[X.Y.Z.N]

## *[Release Catchphrase or Structural Theme]*

```text
███████╗██╗   ██╗███████╗██╗     ██╗              ██████╗   ██╗   ██╗   █████╗    ██████╗     ██████╗ 
██╔════╝██║   ██║██╔════╝██║     ██║              ██╔═══╝   ██║   ██║  ██╔══██╗  ██╔══██╗    ██╔══██╗
███████╗███████║█████╗   ██║     ██║              ██║ ███╗  ██║   ██║  ███████║  ██████╔╝    ██║   ██║
╚════██║██╔══██║██╔══╝   ██║     ██║              ██║   ██║  ██║   ██║  ██╔══██║  ██╔══██╗    ██║   ██║
███████║██║   ██║███████╗███████╗███████╗  ╚██████╔╝╚██████╝  ██║   ██║  ██║   ██║   ██████╔╝
╚══════╝╚═╝  ╚═╝╚══════╝╚══════╝╚══════╝    ╚═════╝   ╚═════╝   ╚═╝  ╚═╝  ╚═╝   ╚═╝   ╚═════╝
                                                  ~ **ClawStack Mobile Studios©™** ~
```

---

## 🚀 The Core Summary

Welcome to **v[X.Y.Z.N]** of **[Project Name]**! This release introduces **[1-2 sentence high-level summary of the primary achievement, e.g., zero-knowledge custom fields, layout ergonomics, or LAN WebCrypto fallback]**. We have streamlined **[Core Feature A]**, unlocked enhanced capabilities for **[Core Feature B]**, finalized architectural alignments for **[Core Feature C]**, and fortified our zero-knowledge cryptographic boundaries to ensure total sovereign privacy.

---

## 💎 Key Themes & Highlights

### 🛡️ 1. [Theme/Domain Title 1, e.g., Zero-Knowledge Cryptography & Storage]

[Brief 1-sentence context on why this theme matters for this release cycle.]

* **[Feature/Fix Name]:** [Action verb-focused description]. Added/Refactored `[Component/Function]` to handle `[Specific Use Case]`, ensuring `[Desired System Outcome]`.
* **[Feature/Fix Name]:** [Action verb-focused description]. Interacting with `[UI Element]` now triggers `[Expected Behavior]` for enhanced ergonomics.
* **[Feature/Fix Name]:** [Action verb-focused description]. Calibrated `[System Logic]` to resolve `[Bug/Edge Case]` without causing regressions.

### 🎨 2. [Theme/Domain Title 2, e.g., Master-Detail UI & Ergonomics]

[Brief 1-sentence context on interface, styling, or modal layout improvements.]

* **Standardized [Component Type]:** Refactored `[Specific View]` to consume the unified `[Global Component]`, establishing consistent behavior and aesthetics.
* **[Brand Style] Alignment:** Standardized focus rings, borders, and active states to match the Reef Modernist design palette.
* **Integrated [Feature] Module:** Introduced an interactive `[Sub-component]` nested directly inside `[Parent Container]`.

### 🔌 3. [Theme/Domain Title 3, e.g., Network & API Topology]

[Brief 1-sentence context on infrastructural or connectivity improvements.]

* **[Feature/Fix Name]:** Swapped `[Legacy Mechanism]` for `[Dynamic Mechanism]`. The system now automatically detects `[Context]`, allowing users to `[Specific Capability]`.

### 🧪 4. [Theme/Domain Title 4, e.g., Verification & Test Oracle Parity]

[Brief 1-sentence context on testing, build validation, or infrastructure.]

* **100% Passing Test Oracle:** [Test results, suite count, and pass rates].
* **Clean Production Bundle:** [Vite build or container image validation details].

---

## 🏗️ Architectural Topology Map

```text
┌─────────────────────────────────────────────────────────────┐
│             🌐 [Client / UI Layer — Reef Modernist]         │
│  ┌────────────────────────┐     ┌────────────────────────┐  │
│  │   SidebarFolderTree    │     │     ItemListPane       │  │
│  │  (User-Governed Pods)  │     │ (Search & Filter Feed) │  │
│  └───────────┬────────────┘     └───────────┬────────────┘  │
│              │                              │               │
│              ▼                              ▼               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │                   ItemDetailPane                      │  │
│  │     (Client AES-256-GCM ShellCryption + TOTP Ring)    │  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               │ (Opaque Ciphertext REST API)
                               ▼
┌─────────────────────────────────────────────────────────────┐
│          🔌 [Server & API Layer — Express + Helmet]         │
│  ┌───────────────────────┐     ┌─────────────────────────┐  │
│  │  requireAuth() Guard  │ ──> │ requirePermission() Claws│  │
│  └───────────────────────┘     └────────────┬────────────┘  │
│                                             │               │
│                                             ▼               │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Tenant Isolation Filter (WHERE user_uuid = ?)        │  │
│  └───────────────────────────┬───────────────────────────┘  │
└──────────────────────────────┼──────────────────────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│         🖥️ [Bedrock Storage — SQLite + SQLCipher]           │
│        (Encrypted At-Rest Database / Volume-Mounted)        │
└─────────────────────────────────────────────────────────────┘
```

---

## 📦 Changes by Layer

| Component | Files | Description |
|:---|:---|:---|
| **Types & Schemas** | `src/types.ts`, `src/server/validation/schemas.ts` | [Type definitions and schema validation updates] |
| **Migrations** | `migrations/XXXX_name.up.sql` | [Database schema migration changes] |
| **Server / API** | `src/server/routes/...` | [Backend routes and permission middleware] |
| **Frontend / UI** | `src/components/...` | [UI components, modal forms, and interaction hooks] |
| **Design System** | `src/index.css`, `DESIGN.md` | [CSS variables, custom scrollbars, theme tokens] |
| **Tests** | `tests/...` | [Unit, integration, and roundtrip test suites] |

---

## 📋 Commit Ledger (Since `v[Previous.Version]`)

* `[hash]` — **feat:** [description of newly introduced capability]
* `[hash]` — **fix:** [description of bug resolution]
* `[hash]` — **refactor:** [description of internal code structure improvements]
* `[hash]` — **docs:** [description of documentation changes]
* `[hash]` — **chore:** [description of maintenance or version bump]

---

## 🚀 Upgrade & Verification Instructions

### Upgrading via Docker / Unraid
```bash
docker pull ghcr.io/clawstackstudios/shellguard:latest
docker restart shellguard
```

### Upgrading from Source
```bash
git fetch --tags
git checkout v[X.Y.Z.N]
npm install
npm run build
npm run scuttle:prod
```
