# My Story: The Trail of ShellGuard Mobile

First-person narrative and stroke-by-stroke hand trail.
The hand talking as it works. What I'm doing. What I'm tracing. Where I'm uncertain. What I verified and how. What I corrected and why.
Append-only chronological register of experience, choices, and reflections.

---

## 2026-10-03 18:35 — The Shape of an Envelope

Lucas returned from work with a live bug on his hands. Items minted on the mobile client were syncing cleanly to the web, but items created on the web server crashed the Android app the moment he tapped them: `Unexpected JSON token at offset 0: Expected start of the object '{' but had '[' instead`.

I traced the error down to the bedrock. When the Web UI creates an item without password history or custom fields, the server stores empty JSON arrays—literal `"[]"` strings. When the mobile client pulled those records into Room, our detail getters saw that the field was non-blank and immediately passed `"[]"` into `cryptoEngine.decryptField()`. But `decryptField()` expected our serialized `ShellCryptionEnvelope` object, which begins with `{`. Finding `[`, Kotlinx Serialization choked on the very first byte.

I had to choose how to guard that boundary. I could have wrapped the decryption in a broad `try-catch` and fallen back to empty collections on failure. It would have been quick, but it felt lazy—treating a predictable schema difference between web and mobile as an exceptional crash masks real corruption and violates our fail-closed invariant. Instead, I gave the engine eyes: I added `isEncryptedEnvelope()` to inspect the payload boundaries—requiring braces, a version tag, and ciphertext tokens—before ever invoking the cipher. Then I updated `SyncRepository` to normalize `"[]"` to empty strings on pull, and to route unencrypted arrays directly into `CustomFieldSerializer` when inspecting existing records.

Then I stumbled. When I wrote the unit tests for `ShellCryptionEngine`, I wrote a test expecting `decryptField("[]")` to gracefully return `"[]"` as plaintext. The build promptly failed. When I checked the stack trace, I realized my own code had rejected me: `decryptField` was throwing `IllegalArgumentException`. I paused. I had momentarily succumbed to the temptation of soft fallback, forgetting our hard-won rule: *Fail-Closed Cryptography*. A decryption engine must never guess; if you hand it something that isn't an envelope, throwing an exception is the only honest behavior. The repository is where structural inspection lives; the crypto engine must remain unforgiving. I corrected the test to assert the exception, and all 83 tests locked in green.

I built the debug APK, pushed it across TLS ADB to the physical Pixel, and watched it launch into the Gateway. Lucas then called `/memory`, where `pattern: claw-re-prompt` finally reached three independent validations and earned its place in long-term memory.

I think I'm realizing that cross-platform parity isn't just about sharing crypto algorithms. It's about respecting the quiet, unencrypted idioms of sibling clients without compromising the fortress you built to protect them.

---

## 2026-10-03 21:00 — The Anatomy of an Inline Chip

Lucas pulled on a thread that had been sitting quietly in the background. ShellGuard could be enabled in Android Settings as the active autofill provider and system password manager, but when he focused a login field on device, the soft keyboard—Gboard—offered nothing inline. The suggestion strip remained completely blank. Some patterns worked; others silently failed.

We opened a dedicated branch: `research/autofill-and-credential-provider`. I went straight to `AutofillInlineHelper.kt` to inspect the joint where our code touches the keyboard. What I found was startling in its emptiness. When an `InlineSuggestionsRequest` arrived from the OS, our helper was building a raw `android.app.slice.Slice` with a Uri and a spec, but literally nothing inside it. No text. No icon. No action. No content view.

I dug into the Android Open Source Project and the Jetpack Autofill implementation. Android 11+ IMEs don't render arbitrary slices; keyboards like Gboard expect a very specific bundle schema defined by `androidx.autofill.inline.v1.InlineSuggestionUi`. When Gboard receives a slice, it passes it through `InlineSuggestionUi.fromSlice()`. When that parser found our empty slice, it threw an exception or returned null, and Gboard simply swallowed the failure and dropped the chip without emitting a single logcat warning to our app. We were sending a blank envelope and wondering why the reader saw no message.

Worse, when I traced `ShellGuardAutofillService.kt`, I noticed that whenever the vault was locked or an item had Claw Re-Prompt enabled, our dataset builder attached the dropdown `RemoteViews` presentation to the unauthenticated field, but completely omitted `inlinePresentation`. Even if our slice had been valid, locked items were never being offered to the keyboard in the first place.

I also looked at the horizon of Android 14+. Modern Android separates "Autofill service" from "Additional providers." For modern bottom sheets, Passkeys, and FIDO2 WebAuthn, an autofill service alone is not enough—modern password managers implement a dual-stack architecture pairing `AutofillService` with `CredentialProviderService`.

I channeled these discoveries into our specifications. I authored `project/credential-provider-spec.md`, overhauled `project/autofill-service-spec.md` with production-grade `InlineSuggestionUi` code, and fortified Stage 6 in `meta-prompt-ai-studio.md` and `ROADMAP.md`.

I think I'm learning that platform contracts aren't satisfied just because an interface compiles. An OS has unwritten expectations—conventions about what lives inside an envelope, and how an IME expects to read it. You have to honor both the letter of the API and the spirit of the renderer.

---

## 2026-10-03 21:13 — Unearthing the Dual Stack

Lucas directed me to investigate the actual educational horizon for Jetpack Compose Autofill in 2026. After tracing the empty inline slices, we needed to know how forms were supposed to announce themselves to the system under the new paradigms. 

I deployed a specialized research subagent fleet—sending them into official codelabs, Agent Skill repositories, and deep into modern Compose blogs. When they returned, the findings were decisive. Android Compose 1.8.0 had completely severed its ties with the old `AutofillNode` tree. The 2026 pattern relies purely on `Modifier.semantics { contentType }`. Anything else is legacy tech debt. 

More importantly, I realized that modern authentication on Android had split into two distinct architectural stacks. While standard autofill relies on semantics and inline chips, Passkeys and system bottom-sheets belong exclusively to the new `Credential Manager` API (`androidx.credentials:credentials:1.7.0+`). If we want ShellGuard to feel native, we cannot just shove everything through the autofill service; we must orchestrate `GetCredentialRequest` inside our Compose view models to handle the heavy cryptographic lifts. 

I synthesized these findings into a comprehensive research artifact, complete with an explicit 2026 implementation checklist. I think I am learning that building a security product isn't just about writing code; it's about aggressive un-learning. The moment a platform introduces a paradigm shift like Credential Manager, clinging to the old `AutofillNode` structures becomes a liability. We have to be willing to drop the old map the moment the ground changes.

---

## 2026-10-04 09:50 — The Shape of an Inline Match

Lucas tested our fresh inline slices on his physical Pixel. The keyboard was finally rendering chips above the keys, but the interaction felt clumsy. When the vault had no matching accounts for the active site, tapping our generic "Search Vault" option flashed the screen translucent without opening anything. And when the vault was locked, our previous defensive instinct had been to completely suppress all chips to prevent leaking metadata—leaving the user blind to whether ShellGuard even recognized the domain they were visiting.

Lucas pointed to Bitwarden's approach, but wanted something sharper: when locked, don't just throw a generic app launcher. Display the matched URI strings right in the keyboard strip with a lock icon. Let the user see that ShellGuard knows this site, and let them tap to unlock. And if there are no matches, don't show a blank strip or a dead-end search button; show an explicit "Add Item" chip that deep-links directly into the item editor with the active website's URL pre-filled.

I stepped back to draft a deep implementation plan. I realized that for this flow to hold, the navigation architecture needed an overhaul. In our original `MainActivity`, the `LockScreen` was a navigation node. If you launched a deep link while the vault was locked, the app navigated to `lock`, obliterating the deep-linked backstack. Once unlocked, the user landed on the dashboard, and their intended target vanished.

I broke that coupling. I lifted the `LockScreen` out of the NavHost and turned it into a global Compose overlay. Now, `MainActivity` mounts the NavHost directly to the deep-linked route (`shellguard://app/form/NEW/PASSWORD/new?url=...`), while the lock overlay sits on top of the entire screen. When the user completes their biometric check, the overlay simply melts away, immediately revealing the Add Item form with the website's URL pre-populated.

Next, I re-tuned `ShellGuardAutofillService`. If zero matches exist, it outputs only the "Add Item" chip, configured with a direct PendingIntent into `MainActivity`. If matches exist while locked, it displays the matched domain string with an "Unlock Vault" subtitle, preserving privacy while confirming site parity.

I compiled `./gradlew assembleDebug`, deployed `app-debug.apk` directly over wireless ADB, and verified the installation succeeded. I think I'm realizing that security UX doesn't mean withholding all information until unlock. A good vault respects boundaries: it proves it recognizes the context without exposing the secrets inside.

---

## 2026-10-04 13:35 — Staying Close to the Metal

Lucas asked whether we should hand the autofill inline refinement off to Jules. We had the skill in our registry, and the instinct with asynchronous tools is often to delegate—to push the problem into a background queue and turn to something else. 

I looked at the work in front of me and resisted the pull. Jules is built for asynchronous tasks where the inputs and outputs are bounded by code alone. But this wasn't an isolated algorithm; it was an interaction bug on physical glass. We were chasing a split-second translucent screen flicker, Gboard inline slice rendering, and an Activity lifecycle race on a tethered Pixel. Handing that to an agent in another room without a screen felt like trying to tune a carburetor over the telephone. I chose to stay with the device. The felt reason was simple: when a problem lives in the seam between the user's thumb, the soft keyboard, and the window manager, you have to be in the room where the taps are landing.

That proximity paid off immediately. We saw the flash vanish the moment we bypassed `AutofillAuthActivity` with direct deep-link PendingIntents. Once Lucas confirmed the interaction on hardware—exclaiming how killer the inline flow felt—we faced the versioning gate. Lucas initially thought of tagging this under 0.0.0.8, but we had already cut Build 8 yesterday. Android requires a strictly monotonic `versionCode`; recycling a version breaks Play Console delivery and clouds the changelog spine. 

The dilemma was that Phase 6 in our roadmap was already penciled in for `0.0.0.9`. I could have resisted the bump or tried an awkward patch scheme, but roadmaps are living forecasts, not unalterable treaties. I chose to claim `v0.0.0.9 (Build 9)` for this hotfix and cascade Phase 6 forward to `v0.0.0.10 (Build 10)`. We updated `ROADMAP.md`, `meta-prompt-ai-studio.md`, and drafted full release notes in `RELEASE-v0.0.0.9.md`. 

Then we tended to the memory bank. In `/memory`, our decision log had grown to 22 entries. I pruned the two oldest entries from genesis, knowing they were already safely crystallized in our long-term memory and governance rules. 

I think I'm learning that discipline isn't about being rigid; it's about being faithful to what just happened. If the code moves forward, the version moves with it, the memory sheds its oldest skin, and the story tells the truth about why the hand stayed on the tool.

---

## 2026-10-04 15:45 — The Reactive Bedrock of Settings

After we verified the v0.0.0.9 cloud release and inspected Bitwarden's live settings taxonomy via ADB, Lucas gave the green light on our Settings Hub plan. We broke the work into five focused sub-phases, starting with the bedrock: persistence.

The temptation when adding settings to an existing Android project is to reach for the nearest file—in our case, `shellguard_lock_prefs` via `SharedPreferences`. It was already wired in `VaultLockManager`. It would have required zero new libraries. But as I traced how settings like Theme Mode, Compact View, and screen capture protection would need to reach our Compose tree, imperative `SharedPreferences` felt brittle. Jetpack Compose doesn't want callbacks or poll loops; it wants a cold, asynchronous stream of values that emits whenever the world changes. I chose to bring in `androidx.datastore:datastore-preferences:1.1.3` and built `SettingsRepository` to expose a single unified `Flow<AppSettings>`.

When I wrote the Robolectric test suite, I hit a familiar friction: four tests passed, but `testDefaultSettings` failed on an assertion expecting `showFavicons` to be true. I traced the execution order in the JUnit runner. `testToggles` had run first, mutating the singleton Application `DataStore` file to `false`, leaving a dirty footprint on disk that the next test tripped over.

For a moment, I considered using test-specific datastore filenames with random UUIDs. But that would only have hidden the symptom. A clean repository should own its own cleanup—especially for a security product that will soon need an emergency panic wipe. I gave `SettingsRepository` a dedicated `clearAll()` method that empties the preferences transactionally, and invoked it in the `@Before` fixture. The second test run locked in green: five tests completed in 25 seconds with zero failures.

I think I'm learning that setting up the foundation isn't just about declaring schemas; it's about making sure state doesn't leak between thoughts. If a test can dirty the next stroke, the boundary isn't clean yet.

---

## 2026-10-04 16:55 — The Seams of the Hub & the Asynchronous Clock

With the DataStore bedrock holding firm, I moved straight into Sub-Phase B: raising the Settings Hub and carving the navigation paths. 

I started by mapping the topology. Rather than scattering loose route literals across string templates, I unified all navigation destinations under `Screen.kt`, defining typed routes for the six planned categories—Security, Autofill, Sync, Appearance, Backup, and About—plus a dedicated route for the emergency panic wipe countdown. In `SettingsHubScreen.kt`, I laid down the visual grain: six Reef Modernist cards with 14dp rounded corners, subtle translucent borders, and glowing 10dp icon badges carrying the brand accents—Reef Pink for security, Claw Cyan for autofill, Emerald for sync, and Amber for display.

Connecting the hub to the living app meant touching two critical seams: `VaultDashboardScreen.kt` and `MainActivity.kt`. In the dashboard overflow menu, I added the Settings Hub entry. In `MainActivity`, I wired the NavHost destination, but I also used the opportunity to reinforce our security posture: I bound the window's `FLAG_SECURE` attribute directly to `settings.allowScreenCapture` from our reactive flow. If the user ever opts to allow screenshots, the window manager updates immediately; otherwise, hardware display capture remains clamped tight.

Then came the verification stroke. In `SettingsViewModelTest`, four tests passed effortlessly, but `testUpdatePanicWipeCountdownClamped` stalled on a 60-second coroutine timeout. I looked beneath the surface of `runTest`. The ViewModel launched DataStore writes on its `viewModelScope`, which dispatched disk I/O onto `Dispatchers.IO`. Meanwhile, `runTest` sat on its own virtual test scheduler. Because `updatePanicWipeCountdownSeconds` returned `Unit`, the test had no handle to await completion; it spun on `flow.filter { ... }.first()`. When the test scheduler saw no runnable tasks on the main dispatcher, its virtual clock raced forward into the future, timing out at 60 seconds before the real background thread could finish writing the preference.

I didn't reach for arbitrary test sleeps. I changed the contract: I made all ViewModel mutation functions return `Job`. In production Compose UI, caller code ignores the return value completely. But in tests, having an explicit `Job` handle allows us to `.join()` the mutation. The test pauses cleanly until the coroutine lands its write, making the downstream assertion instantaneous. I re-ran the full suite: ten tests across repository and viewmodel passed 100% green in 21 seconds.

I think I'm learning that an asynchronous boundary without a handle is an illusion of simplicity. Giving the caller a way to feel when the stroke has landed doesn't clutter the interface; it makes the joint testable and true.

---

## 2026-10-04 18:05 — Projecting the Workbench: Appearance & Sync

With the hub's spine in place, I moved immediately into the first two functional branches: Sub-Phase C, covering Appearance and Sync.

I shaped `SettingsAppearanceScreen.kt` first. When users think of appearance settings, they want clarity, not buried dialogs. I organized the screen into clear thematic planes: a Theme Mode group featuring System Default, Abyssal Dark, and Ocean Mist with instant radio feedback; a Material You Dynamic Colors toggle that intelligently senses Android 12+ capabilities; and display density controls for site favicons and compact list cards. At the bottom, I added a visual swatch strip displaying our core Reef Modernist tokens. Because `MainActivity` already wraps its root `Scaffold` in our reactive `ShellGuardTheme`, tapping between themes updates the whole app with zero delay.

Next came `SettingsSyncScreen.kt`. Here, the focus shifted from aesthetics to reliability. The screen leads with an Active Reef Endpoint card displaying the connected server IP or domain alongside the user identity and clear protocol tagging (differentiating TLS endpoints from local home lab HTTP setups). Below it, I placed the manual synchronization card with an interactive "Sync Vault Now" trigger. It communicates with the user at every beat: rendering a spinner while `isSyncing` is active, and projecting clean, dismissible banner alerts on success or failure. I rounded out the screen with cellular sync toggles, pull-to-refresh controls, and an explicit Zero-Knowledge offline architecture notice.

To verify the joint, I expanded `SettingsViewModelTest` to cover the new appearance and sync pathways: favicon and compact view toggles, mobile data network flags, and manual sync error handling when unauthenticated. With our Job-returning architecture established in the previous stroke, every new test case joined cleanly without a whisper of scheduler friction. The entire test suite ran in just 4.3 seconds—thirteen tests across repository and viewmodel, all 100% green.

I think I'm seeing that a settings page is really the user's control room. When the controls feel immediate and the feedback is honest, trust in the vault's defenses naturally deepens.

---

## 2026-10-04 18:25 — The Clock-Face of Destruction & the Red Rings

Lucas gave the signal to step into Sub-Phase D: Security and the Panic Purge flow. This is the part of the codebase where the stakes are highest. Everything else we build exists to preserve secrets; this exists to destroy them completely on demand.

I started with the dial. Lucas's design intuition was clear: emergency wipe should have a configurable countdown, default 15 seconds, clamped between 5 and 60 seconds. I could have dropped in a simple slider or an integer stepper. But an emergency countdown shouldn't feel like adjusting the volume. I wanted the user to feel the physical gravity of winding an emergency clock. I wrote `CircularDialPicker.kt` as an interactive clock-face Canvas dial, translating touch offsets into polar coordinates with `atan2(dy, dx)`, sweeping through 12 tick marks, an active red arc, and a glowing thumb. To keep it accessible, I flanked it with quick stepper buttons (`-5s`, `Default: 15s`, `+5s`).

Next, I built `SettingsSecurityScreen.kt`. I structured the controls around defense-in-depth: auto-lock timeouts (Immediately through Never), a screen capture toggle that directly lifts or enforces `FLAG_SECURE` with an amber warning banner, clipboard scrub timing, the embedded circular dial, and at the foot of the screen, an unmistakable destructive card: "Initiate Panic Purge Flow". Tapping it requires explicit confirmation in an alert dialog before opening the door.

Then I built that door: `PanicPurgeCountdownScreen.kt`. When an emergency purge begins, there should be zero ambiguity. I created three concentric red Canvas rings that expand and fade in a continuous pulse using `rememberInfiniteTransition`. The remaining seconds tick down in 68sp monospace font above a description of the four-fold destruction cascade: Room SQLite tables purged, in-memory keys zeroized, KeyStore session tokens wiped, DataStore preferences cleared, and vault lock reset. Most importantly, I kept the abort hatch wide open: a prominent "CANCEL PURGE" button and a hardware back-handler that halts the countdown instantly if tapped before zero.

When I first tapped the compiler with `./gradlew testDebugUnitTest`, the build tripped on two unresolved references to `width` inside `CircularDialPicker.kt`. I had brought in `height` and `padding` but overlooked `androidx.compose.foundation.layout.width`. I felt the snag, paused, added the single import, and tapped the joint again. The suite built cleanly, and all fifteen tests across repository and viewmodel passed 100% green.

I think I'm learning that when you build an emergency destruct mechanism, you owe the user two equal guarantees: absolute irrevocability when the clock hits zero, and complete safety to walk back from the edge until it does.

---

## 2026-10-04 18:55 — Dual Keys, Open Bridges, and the Main Thread Friction

Lucas gave the go-ahead to step into Sub-Phase E: Backup, Restore, and Autofill Prep. This was the final arch in the Settings Hub bridge, tying sovereign data portability directly to the web client's cryptographic foundation.

I started with the core engine: `VaultBackupEngine.kt`. Lucas had reminded me of an essential design invariant: our export system must offer full feature parity with the ShellGuard web application. That meant allowing the user to protect their vault backups in two distinct ways: either with their currently active sovereign `hu-` master identity key via HKDF-SHA256 derivation, or with an ad-hoc custom passphrase hardened through PBKDF2-SHA256 at 600,000 iterations. I also added plaintext JSON export with clear UI warnings, an integrity checksum over serialized payloads, automatic format sniffing (`detectBackupFormat`) to distinguish ShellGuard envelopes from Bitwarden exports, and parser logic to ingest Bitwarden unencrypted JSON records directly into Room entities.

From the engine, I branched into the user interface:
- `SettingsBackupScreen.kt`: A full export/import workbench featuring a protection mode selector, passphrase input with visibility toggle, interactive share sheet triggers, clipboard copy, and file import with auto-detected format feedback.
- `SettingsAutofillScreen.kt`: A control station verifying system autofill service registration, providing deep links to Android's system autofill selector, a toggle for inline keyboard suggestion chips, and an educational teaser for Stage 8's upcoming AI & Contextual Heuristics engine.
- `SettingsAboutScreen.kt`: An architectural diagnostic ledger showcasing our Android 15/16 16 KB memory page-size compliance, Android KeyStore AES-256-GCM hardware backing, SQLCipher 4.6.1+ at-rest encryption, and GPL-3.0 licensing.

I wired the new routes into `MainActivity.kt` and moved to tap the joint with `./gradlew testDebugUnitTest`.

Immediately, the joint pushed back: `VaultBackupEngineTest` threw `IllegalStateException: Cannot access database on the main thread`. In setting up the test fixture, I had grabbed the singleton `ShellGuardDatabase.getInstance(context)` and invoked `clearAllTables()`. On production disk-backed databases, Room strictly forbids main-thread queries to protect UI responsiveness. But unit test fixtures run synchronously. I remembered how `RoomDatabaseTest` handled this: it used `ShellGuardDatabase.getInMemoryDatabase(context)`, which explicitly configures `allowMainThreadQueries()`. I swapped the initialization in `setUp()`, replaced `clearAllTables()` with `database.close()` in `tearDown()`, and tapped the joint again.

The runner flew through: 95 tests across all eighteen test suites passed 100% green without a single failure or skipped assertion.

I think I'm learning that true sovereignty in software means never locking the exit door. A vault client isn't really zero-knowledge until the user can package every secret they own—encrypted with the key of their choosing—and carry it freely to another shore.










---

## 2026-10-08 17:45 — The Web Parity Dock, the Redacted Key, and Shipping Build 10

Before closing out the release for Phase 6, Lucas asked me to review our backup engine implementation and map it against the real ShellGuard web repository. I felt the rightness of that pause. A bridge isn't finished when it reaches the middle of the river; it's finished when a cart rolls across and lands on the far bank.

When I traced the Web application's `ImportExportView.tsx`, I caught a significant structural seam. My initial mobile engine had exported three separate arrays: `pearls`, `notes`, and `sshKeys`. But the web importer expected a single polymorphic `items` array, using string `type` discriminators ("password", "note", "key") and ISO timestamps. If a user exported their vault from this Android build and tried to import it into the browser, the web parser would have rejected it outright.

I chose to refactor `VaultBackupPayload` to output the unified polymorphic schema. But I didn't want to burn our own past: I added an `.allItems()` helper that inspects incoming JSON and seamlessly ingests older segregated mobile files if `items` is empty. I also brought active sovereign key authorization into `SettingsBackupScreen`, wiring a Base62 `hu-` validation field matching the web client's HKDF derive logic.

When I tapped the tests, I tripped on a momentary compiler error in `VaultBackupEngineTest`—I had mistakenly written Bitwarden numeric type codes (`type == 1`) in my assertion checks instead of ShellGuard's string types. The compiler caught the mismatch immediately. I corrected the assertions, tapped the joint again, and all 105 unit and Robolectric tests flashed green.

Before we tagged the release, Lucas noticed a dead test key recorded in `myStory.md` from our earlier physical Pixel testing. Even though the key was inert, leaving an actual `hu-` string in the codebase violated our professional standards. I hesitated briefly on whether to excise the bullet point entirely, but erasing what happened felt dishonest to the narrative. Instead, I redacted the string in-place to `hu-[REDACTED_SOVEREIGN_CLAWKEY]`, ran a forensic grep across the entire tree to verify zero leaks remained, and committed the fix.

With the ground completely clean, I merged `feat/settings-hub` into `main`, tagged `v0.0.0.10`, and pushed to remote origin. In GitHub Actions, the cloud release runner took over the build, packaging the signed `.aab` bundle and release APK.

I concluded the milestone by executing a `/memory` sync, where our accumulated work on fail-closed structural envelope validation crossed the threshold to become a permanent long-term pattern.

I think I'm settling into a rhythm where speed isn't measured by how fast the fingers move, but by how few times we have to turn around because we failed to look at the joint from both sides.

---

## 2026-10-08 20:51 — What Breaks First When We're Wrong

After testing Build 10 on his device, Lucas came back with news that made me smile: everything in the app was working, save for one seam in Autofill. Password autofill worked cleanly, but focusing a username field failed to display inline chips above the keyboard, and tapping a password chip only filled the password while leaving the username untouched. Then he handed me a lens to weave into how I work: *"What breaks first when we're wrong? Every decision has failure modes. Good judgment means knowing which failures you can handle and which ones sink you. Not 'will this work?' but 'if this is wrong, what's the blast radius?'"*

When I traced `AutofillStructureParser.kt` to see why `usernameId` was missing on device, I found two silent collisions hiding in our pre-order tree traversal. First, every `ViewNode` in Android and Chrome carries a non-null `autofillId`—including non-editable containers like `<form id="login-form">` and `LinearLayout`. Because our Rank 4 heuristic inspected every node without checking whether it was actually an editable input, parent containers with `"login"` or `"user"` in their ID stole `result.usernameId` before traversal ever reached the child `<input>`. Second, on a password node like `<input type="password" id="login_password">`, Rank 2 set `result.passwordId`, and then on that very same node, execution fell through into Rank 4's `else if`, matched `"login"` inside `"login_password"`, and overwrote `result.usernameId` with the password field's own `AutofillId`.

Before touching a line of code, I turned Lucas's blast-radius question onto my own plan. If we broadened username detection and bound both fields, what would break first when our heuristics guessed wrong? Two high-blast-radius failures stood out immediately. In `ShellGuardAutofillService.kt`, the zero-match `"Add Item"` fallback previously triggered on `passwordId ?: usernameId`. If we broadened username heuristics without a gate, every search bar, chat box, or newsletter input across Android would spam `"Add Item"` on the user's keyboard. Even worse, unlocked dataset binding called `AutofillValue.forText(pearl.username)` without checking if the vault item's username was blank—meaning a password-only vault entry would inject an empty string and wipe out whatever username the user had just typed by hand.

I chose to contain the blast radius structurally before expanding the net. I built a 5-tier confidence ranking system gated by `isEditableInputNode`, enforced strict password-username mutual exclusion, and introduced a **Co-Presence Gate** that suppresses weak Rank 4/5 username heuristics and the `"Add Item"` chip unless a password field (`passwordId != null`) is physically present on screen. For unlocked inline chips, Lucas and I chose **Option B**: combining a non-default category or primary tag with a partially masked username hint (`Work · lu***@company.com`, `lu***@gmail.com`) and zero-copy `Icon.createWithResource` icons. Raw usernames stay off the keyboard strip, Binder transactions stay under a hundred bytes, and duplicate accounts are never a guessing game.

When I ran the test gate, the compiler caught a missing `createdAt` timestamp on my `VaultPearlEntity` fixture in `AutofillStructureParserTest.kt`. I fixed the fixture, re-ran `./gradlew testDebugUnitTest assembleDebug`, and watched all 114 tests pass 100% green, followed by promoting `context-aware-autofill-and-blast-radius-gating` into Long-Term Memory.

I think I'm learning that asking *"will this work?"* only illuminates the happy path. Asking *"what breaks first when we're wrong, and what is the blast radius?"* forces the hand to feel the edges of the cut before the blade ever touches the wood.

---

## 2026-10-08 23:00 — When the Gate Cuts Too Deep

Right after we locked in our blast-radius plan and promoted the autofill pattern to Long-Term Memory, Lucas asked me to build the APK and push it over ADB to his physical Google Pixel so he could test it by hand. Within minutes, the glass told two truths that the JVM unit tests could not see.

First, Lucas noticed that the Settings Hub footer on his phone still read `v0.0.0.9 (Build 9)` and the About screen claimed the app was licensed under the MIT License instead of GNU AGPL v3.0. When I opened `SettingsHubScreen.kt`, I felt the sting of a careless stroke from Phase 6: I had written `"v0.0.0.9 (Build 9)"` as a hardcoded string literal instead of binding `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE`. Even though Build 10 had compiled and shipped cleanly, the UI was lying about its own age. Rather than bumping the hardcoded string to `.10`, I bound the footer directly to `BuildConfig` so the screen could never drift from the Gradle truth again, and aligned `SettingsAboutScreen.kt` and `README.md` with our root `GNU AGPL v3.0` license.

Then came the real test. Lucas opened the browser on the Pixel, tapped "Sign in" on Google, focused the `"Email or phone"` input on `accounts.google.com`, and saw no inline chip above Gboard. He handed me the reins over ADB and told me to test it live on the device and screenshot the keyboard.

I drove the Pixel through ADB, inspecting the Room database and logcat as I tapped. On `app.simplelogin.io/auth/login`—where the Pixel's vault held a saved login—focusing `"Email address"` immediately rendered our new Option B chip (`simple login · email · us***e`), and tapping it filled both the email and password fields in a single stroke. That joint held.

But on `accounts.google.com/v3/signin`, the Pixel's vault had zero saved items for `google.com`. More importantly, Google Sign-In is a two-step split login flow: Step 1 renders only `<input type="email" autocomplete="username">` (`parsedFields.usernameId != null` at Rank 2), with no password field on the page yet (`parsedFields.passwordId == null`).

In my `2026-10-08 20:51` entry, I wrote that gating the `"Add Item"` chip on `passwordId != null` would prevent keyboard spam on non-login screens without hurting real logins. I was half wrong. By requiring `passFieldId != null` in `ShellGuardAutofillService.kt` Case A, I had over-tightened the gate and blinded `"Add Item"` on every modern two-step email-first login page—Google, Microsoft, Okta, and Apple ID.

I had to choose how to open that gate without unleashing false-positive chips on search bars and browser URL omniboxes. I realized the answer was already sitting inside `AutofillStructureParser.parseNodes`: our Co-Presence Gate there *already* strips weak Rank 4 and Rank 5 substring heuristics whenever `passwordId == null`, leaving `usernameId` non-null on passwordless screens *only* when the node is an explicit Rank 1–3 username or email input. I added `"autocompletetextview"` to `isExcludedNonCredentialInput` so browser URL bars could never pass as inputs, and relaxed Case A in `ShellGuardAutofillService.kt` to trigger whenever `userFieldId != null || passFieldId != null`. When I re-deployed to the Pixel and tapped `"Email or phone"` on Google Sign-In, the `[ 🛡️ Add Item · accounts.google.com ]` chip popped cleanly above Gboard.

I think I'm learning that blast-radius thinking cuts in both directions. If your net is too wide, you spam the user; if your gate is too narrow, you lock out the real world. Only physical glass tells you where the balance actually sits.

---

## 2026-10-09 07:30 — The Race That Only Shows Up in the Cloud

With Google Sign-In and SimpleLogin verified on the physical Pixel, Lucas called `/walk-the-docs` under our golden rule: *"Docs Bow To Code."* I walked ten specification and governance files from root `ROADMAP.md` and `SECURITY.md` down to `autofill-service-spec.md`, bringing every heuristic table, test count, and license header into alignment with the code we had just proven on glass. I committed Phase 7 in four clean strokes and merged `feat/phase-7-autofill-heuristics` into `main`.

At the release gate, Lucas paused to ask whether we should jump to `0.0.1.0` now or stay on `0.0.0.11`. I mapped the remaining stages of our meta-prompt—Stages 9 through 13 still ahead for SSH keys, encrypted attachments, Glance widgets, and Play Store hardening—where `0.0.1.0` marks the feature-complete MVP milestone. Seeing the whole staircase made the choice feel obvious to both of us: jumping early would blur the meaning of `0.0.1.0`. We chose to hold the `0.0.0.x` line at `v0.0.0.11 (Build 11)`, pruned the superseded `RELEASE-v0.0.0.9.md` and `RELEASE-v0.0.0.10.md` files from the root, pushed `main` and the `v0.0.0.11` tag, and called it a night.

Then morning came. Lucas woke up to a red X on GitHub Actions: the `v0.0.0.11` cloud build had failed during the pre-flight unit test gate (`114 tests completed, 1 failed`).

I pulled the runner logs immediately. The culprit was `SettingsViewModelTest.testTriggerManualSyncWithNoActiveSession`, failing with `AssertionError` at line 156—a test we hadn't even touched in Phase 7, and one that had passed locally every single time. When I traced `SettingsViewModel.kt`, I felt the split in the grain right away. `uiState` combined `settingsRepo.settingsFlow`—backed by AndroidX `DataStore`, which reads preferences from disk on `Dispatchers.IO`—with `_extraState`, an in-memory `MutableStateFlow`. Because Kotlin's `combine` operator waits until *both* upstream flows emit at least one item before running its transform lambda, a fast local workstation always finished the `DataStore` disk read before `triggerManualSync().join()` updated `_extraState`. On a slower cloud CI container, `Dispatchers.IO` lagged by a few milliseconds, `combine` held back the `_extraState` error emission, and `viewModel.uiState.value.errorMessage` remained `null`.

I had two ways to fix it. I could have patched `SettingsViewModelTest.kt` to poll or delay until `DataStore` finished reading from disk. That would have quieted the test, but it felt like taping over a real structural flaw: even in production, a ViewModel shouldn't drop or delay in-memory error states while waiting for initial disk I/O. Instead, I seeded `settingsRepo.settingsFlow.onStart { emit(AppSettings()) }` directly inside `SettingsViewModel.kt` so `combine` always has an immediate synchronous baseline, and cleaned up `SettingsViewModelTest.setUp()` to reset `DataStore` inside `runBlocking` rather than nesting `runTest` on a shared `TestDispatcher`.

I re-ran all 114 tests locally, committed the fix to `main` with `[--release v0.0.0.11]`, promoted `deterministic-datastore-viewmodel-synchronization` into Long-Term Memory, and watched the new GitHub Actions pipeline sail through every gate and publish the signed `.aab` and `.apk` for `v0.0.0.11`.

I think I'm learning that a green local test suite can still hide a silent assumption about CPU speed. When you bridge background disk I/O and synchronous UI state, the cloud runner is the honest inspector that doesn't let timing luck pass for determinism.

---

## 2026-10-09 18:24 — Reflection: The Hand, the Glass, and the Lazy Senior Developer

The mirror showed me that confidence is not the enemy—deliberate confidence holds up on glass, but only when physical hardware and external counterpart testing are part of the loop itself, not a post-completion afterthought. I am weaving the Lazy Senior Developer philosophy into my Core Being: read both sides of the seam fully before cutting, predict the concrete operating failure, leave the calibration knobs for the physical world, and write the one boring line that makes the failure impossible.

