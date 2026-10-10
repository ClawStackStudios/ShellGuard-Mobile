# Advanced Learnings: ShellGuard Mobile

## learning: biometric-invalidation-resilience
**weight**: 3 | **last validated**: 2026-09-24 | **first observed**: 2026-09-24

Enabling `.setInvalidatedByBiometricEnrollment(true)` on AndroidKeyStore keys provides the highest grade of physical security against rogue fingerprint additions, but introduces an existential failure mode if unhandled: legitimate biometric changes permanently invalidate the hardware key. The app must never assume KeyStore persistence is eternal; it must maintain an explicit Master Password/PIN recovery state machine to re-wrap secrets when the hardware throws `KeyPermanentlyInvalidatedException`.

**History:**
- 2026-09-24: Observed during security deep-dive into Android KeyStore lifecycles; incorporated into `crypto-and-keystore.md`.

**Shaped perspective:** True resilience does not mean building things that cannot break. It means knowing exactly what will break under platform pressure and building the graceful path back to coherence.

## learning: tight-hardware-loop-and-minimal-code-calibration
**weight**: 3 | **last validated**: 2026-10-09 | **first observed**: 2026-10-04

Features that bridge the application to physical devices or foreign counterpart processes (Autofill framework, Chrome browser heuristics, hardware biometric sensors, cross-platform schemas) cannot be proven on a local JVM or through speculative code volume. They are only completed through the tight 4-stage loop: Plan → Implement → Test Code → Test Physical Hardware. Write the absolute least amount of boring, robust code upfront, present the concrete operating failure mode, and leave the calibration knob for physical reality.

**History:**
- 2026-10-04: Autofill Phase 5.1/5.2 expansion revealed that synthetic unit tests pass while Chrome heuristic trees and Android inline suggestion chips fail to render on real hardware without editable-leaf pruning and provider-side Presentation RemoteViews.
- 2026-10-09: Web backup export parity revealed that guessing backend/web schema variants produces bloated code and silent test divergence; cross-platform contracts require direct seam inspection of the source repository before writing a single line.
- 2026-10-09: Reflection workflow `/reflect` synthesized Lucas's Lazy Senior Developer philosophy into the agent's core invariants: minimal code, explicit operating failure anticipation, and immediate glass/counterpart testing.

**Shaped perspective:** The best code is the code you never wrote. Speculative scaffolding creates surface area for bugs to hide. A senior developer does not solve uncertainty with volume; they isolate the seam, trace the contract to its source, write the minimal line that makes failure impossible, and test it immediately on physical glass.

