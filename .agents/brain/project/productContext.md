# Product Context: ShellGuard Mobile

## Why This Project Exists
Self-hosters and privacy-conscious users running ShellGuard web servers need a native mobile companion that provides full secrets management on the go. While ShellGuard-TOTP provides 2FA codes, users require:
1. Access to their entire vault (logins, notes, SSH keys) in their pocket.
2. System-wide Autofill so they don't have to switch apps and copy-paste passwords.
3. 100% offline vault accessibility (Bitwarden Read-Only model) with automatic delta synchronization upon reconnection.
4. Seamless cryptographic parity with the web vault.

## Problems It Solves
- **Cloud Dependency**: Eliminates reliance on proprietary, closed-source password manager clouds.
- **Offline Friction**: Keeps the entire encrypted vault on-device; operations never fail due to intermittent cellular service.
- **Credential Shuffling Vulnerabilities**: AAD namespace binding prevents attackers from swapping encrypted ciphertext between records.
- **Copy-Paste Clipboard Leaking**: Native Autofill reduces clipboard exposure and prevents malicious apps from reading copied secrets.

## User Experience Goals
- **Reef Modernist Aesthetic**: Seamless visual consistency with ShellGuard Web and ShellGuard-TOTP across 6 theme accents (`REEF_DEFAULT`, `CYAN_VENT`, `PURPLE_SHELL`, `EMERALD_TRENCH`, `AMBER_FLARE`, `MONOCHROME`).
- **Sub-16ms Fluidity**: Instant search filtering, smooth 60fps TOTP countdown arcs, spring touch bounces.
- **ClawStack Gateway Parity**: Familiar protocol/host/port gateway login that connects directly to local or remote servers.
