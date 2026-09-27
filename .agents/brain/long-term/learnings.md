# Advanced Learnings: ShellGuard Mobile

## learning: biometric-invalidation-resilience
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Enabling `.setInvalidatedByBiometricEnrollment(true)` on AndroidKeyStore keys provides the highest grade of physical security against rogue fingerprint additions, but introduces an existential failure mode if unhandled: legitimate biometric changes permanently invalidate the hardware key. The app must never assume KeyStore persistence is eternal; it must maintain an explicit Master Password/PIN recovery state machine to re-wrap secrets when the hardware throws `KeyPermanentlyInvalidatedException`.

**History:**
- 2026-09-24: Observed during security deep-dive into Android KeyStore lifecycles; incorporated into `crypto-and-keystore.md`.

**Shaped perspective:** True resilience does not mean building things that cannot break. It means knowing exactly what will break under platform pressure and building the graceful path back to coherence.
