# Dissolved
Compressed archive of released memory bank entries.
Each seed is a one-sentence pointer to content that was
dissolved from the active temporal bank. Full originals are
recoverable via git history using the commit hash.

This file is a table of contents, not a backup.
It is never loaded automatically. It is retrieved on explicit
request only.

## Index
- 2026-09-24: `hybrid-file-system-vault` (decision-log.md | a791283)
- 2026-09-26: `cwe-359-sensitive-clipboard-masking` (systemPatterns.md | c6e75cd)
- 2026-09-26: `cleartext-lan-and-tailscale-transport` (techContext.md | c6e75cd)
- 2026-09-26: `raw-reflection-log-consolidation-pass-1` (raw_reflection_log.md | 4ceec02)

## 2026-09-26 — Dissolution Pass

### From systemPatterns.md
- **cwe-359-sensitive-clipboard-masking** | systemPatterns.md | 2026-09-26
  Enforced ClipDescription.EXTRA_IS_SENSITIVE on Android 13+ with 30s auto-scrubbing to prevent clipboard thumbnail leaks.
  git: c6e75cd

### From techContext.md
- **cleartext-lan-and-tailscale-transport** | techContext.md | 2026-09-26
  Permitted cleartext HTTP traffic across local subnets and Tailscale CGNAT addresses where domain TLS is absent.
  git: c6e75cd

### From decision-log.md
- **hybrid-file-system-vault** | decision-log.md | 2026-09-24
  Decoupled attachments from Room SQLite rows to prevent 2MB CursorWindow crashes, streaming ciphertext to internal disk.
  git: a791283

### From raw_reflection_log.md
- **raw-reflection-log-consolidation-pass-1** | raw_reflection_log.md | 2026-09-26
  Transferred Stage 0 through Phase 2 raw architectural and deployment reflections into consolidated_learnings.md and project rules.
  git: 4ceec02
